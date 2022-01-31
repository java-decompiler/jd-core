/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.process.analyzer.classfile.reconstructor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayList;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.AAStore;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AStore;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;

/*
 * Les valeurs des Enum des classes produites par "javac" sont correctement
 * reconnues par "InitStaticFieldsReconstructor.Reconstruct(...)".
 *
 * Cette classe a ete creee car les enums produits par "dex2jar" contients un
 * motif particulier (Extrait de: gr\androiddev\FuelPrices\StaticTools.class):
 *
 * private static final synthetic LocationProvider ANY;
 * private static synthetic LocationProvider BESTOFBOTH;
 * private static synthetic LocationProvider ENUM$VALUES[];
 * private static synthetic LocationProvider GPS;
 * private static synthetic LocationProvider NETWORK;
 *
 * static
 * {
 *   BESTOFBOTH = new LocationProvider("BESTOFBOTH", 0);
 *   ANY = new LocationProvider("ANY", 1);
 *   GPS = new LocationProvider("GPS", 2);
 *   NETWORK = new LocationProvider("NETWORK", 3);
 *   LocationProvider alocationprovider[] = new LocationProvider[4];
 *   alocationprovider[0] = BESTOFBOTH;
 *   alocationprovider[1] = ANY;
 *   alocationprovider[2] = GPS;
 *   alocationprovider[3] = NETWORK;
 *   ENUM$VALUES = alocationprovider;
 * }
 *
 * --> Les instructions d'initialisation et les champs ne sont pas classes dans le même ordre.
 * --> Un tableau local est utilise.
 */
public final class InitDexEnumFieldsReconstructor
{
    private InitDexEnumFieldsReconstructor() {
        super();
    }

    public static void reconstruct(ClassFile classFile)
    {
        Method method = classFile.getStaticMethod();
        if (method == null) {
            return;
        }

        Field[] fields = classFile.getFields();
        if (fields == null) {
            return;
        }

        List<Instruction> list = method.getFastNodes();
        if (list == null) {
            return;
        }

        ConstantPool constants = classFile.getConstantPool();

        // Search field initialisation from the end

        // Search PutStatic("ENUM$VALUES", ALoad(...))
        int indexInstruction = list.size();

        if (indexInstruction > 0)
        {
            // Saute la derniere instruction 'return'
            indexInstruction--;

            while (indexInstruction-- > 0)
            {
                Instruction instruction = list.get(indexInstruction);
                if (instruction.getOpcode() != Const.PUTSTATIC) {
                    break;
                }

                PutStatic putStatic = (PutStatic)instruction;
                if (putStatic.getValueref().getOpcode() != Const.ALOAD) {
                    break;
                }

                ConstantFieldref cfr = constants.getConstantFieldref(putStatic.getIndex());
                if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
                    break;
                }

                ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());

                String name = constants.getConstantUtf8(cnat.getNameIndex());
                if (! StringConstants.ENUM_VALUES_ARRAY_NAME_ECLIPSE.equals(name)) {
                    break;
                }

                int indexField = fields.length;

                while (indexField-- > 0)
                {
                    Field field = fields[indexField];

                    if ((field.getAccessFlags() & (Const.ACC_STATIC|Const.ACC_SYNTHETIC|Const.ACC_FINAL|Const.ACC_PRIVATE)) ==
                            (Const.ACC_STATIC|Const.ACC_SYNTHETIC|Const.ACC_FINAL|Const.ACC_PRIVATE) &&
                        cnat.getSignatureIndex() == field.getDescriptorIndex() &&
                        cnat.getNameIndex() == field.getNameIndex())
                    {
                        // "ENUM$VALUES = ..." found.
                        ALoad aload = (ALoad)putStatic.getValueref();
                        int localEnumArrayIndex = aload.getIndex();
                        int index = indexInstruction;

                        // Middle instructions of pattern : AAStore(...)
                        List<Instruction> values = new ArrayList<>();

                        while (index-- > 0)
                        {
                            instruction = list.get(index);
                            if (instruction.getOpcode() != Const.AASTORE) {
                                break;
                            }
                            AAStore aastore = (AAStore)instruction;
                            if (aastore.getArrayref().getOpcode() != Const.ALOAD ||
                                aastore.getValueref().getOpcode() != Const.GETSTATIC ||
                                ((ALoad)aastore.getArrayref()).getIndex() != localEnumArrayIndex) {
                                break;
                            }
                            values.add(aastore.getValueref());
                        }

                        // FastDeclaration(AStore(...))
                        if (instruction.getOpcode() != FastConstants.DECLARE) {
                            break;
                        }
                        FastDeclaration declaration = (FastDeclaration)instruction;
                        if (declaration.getInstruction().getOpcode() != Const.ASTORE) {
                            break;
                        }
                        AStore astore = (AStore)declaration.getInstruction();
                        if (astore.getIndex() != localEnumArrayIndex) {
                            break;
                        }

                        int valuesLength = values.size();

                        if (valuesLength > 0)
                        {
                            // Pattern found.

                            // Construct new pattern
                            InitArrayInstruction iai =
                                new InitArrayInstruction(
                                    ByteCodeConstants.INITARRAY,
                                    putStatic.getOffset(),
                                    declaration.getLineNumber(),
                                    new ANewArray(
                                        Const.ANEWARRAY,
                                        putStatic.getOffset(),
                                        declaration.getLineNumber(),
                                        classFile.getThisClassIndex(),
                                        new IConst(
                                            ByteCodeConstants.ICONST,
                                            putStatic.getOffset(),
                                            declaration.getLineNumber(),
                                            valuesLength)),
                                    values);
                            field.setValueAndMethod(iai, method);

                            // Remove PutStatic
                            list.remove(indexInstruction);
                            // Remove AAStores
                            while (--indexInstruction > index) {
                                list.remove(indexInstruction);
                            }
                            // Remove FastDeclaration
                            list.remove(indexInstruction);
                        }

                        break;
                    }
                }
            }
        }
    }
}
