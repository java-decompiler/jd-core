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
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantString;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayList;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;
import jd.core.model.instruction.bytecode.instruction.Ldc;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.reference.ReferenceMap;
import jd.core.process.analyzer.classfile.visitor.ReplaceDupLoadVisitor;
import jd.core.util.SignatureUtil;

/*
 * Recontruction du mot cle '.class' depuis les instructions generees par le
 * JDK 1.1.8 de SUN :
 * ...
 * ifnotnull( getstatic( current class, 'class$...', Class ) )
 *  ternaryopstore( getstatic( class, 'class$...' ) )
 *  goto
 *  dupstore( invokestatic( current class, 'class$', nom de la classe ) )
 *  putstatic( current class, 'class$...', Class, dupload ) )
 *  ??? ( dupload )
 * ...
 */
public final class DotClass118AReconstructor
{
    private DotClass118AReconstructor() {
        super();
    }

    public static void reconstruct(
        ReferenceMap referenceMap, ClassFile classFile, List<Instruction> list)
    {
        int i = list.size();

        if  (i < 6) {
            return;
        }

        i -= 5;
        ConstantPool constants = classFile.getConstantPool();

        while (i-- > 0)
        {
            Instruction instruction = list.get(i);

            if (instruction.getOpcode() != ByteCodeConstants.IFXNULL) {
                continue;
            }

            IfInstruction ii = (IfInstruction)instruction;

            if (ii.getValue().getOpcode() != Const.GETSTATIC) {
                continue;
            }

            GetStatic gs = (GetStatic)ii.getValue();

            int jumpOffset = ii.getJumpOffset();

            instruction = list.get(i+1);

            if (instruction.getOpcode() != ByteCodeConstants.TERNARYOPSTORE) {
                continue;
            }

            TernaryOpStore tos = (TernaryOpStore)instruction;

            if (tos.getObjectref().getOpcode() != Const.GETSTATIC ||
                gs.getIndex() != ((GetStatic)tos.getObjectref()).getIndex()) {
                continue;
            }

            instruction = list.get(i+2);

            if (instruction.getOpcode() != Const.GOTO) {
                continue;
            }

            Goto g = (Goto)instruction;

            instruction = list.get(i+3);

            if (instruction.getOpcode() != ByteCodeConstants.DUPSTORE) {
                continue;
            }

            if (g.getOffset() >= jumpOffset || jumpOffset > instruction.getOffset()) {
                continue;
            }

            DupStore ds = (DupStore)instruction;

            if (ds.getObjectref().getOpcode() != Const.INVOKESTATIC) {
                continue;
            }

            Invokestatic is = (Invokestatic)ds.getObjectref();

            if (is.getArgs().size() != 1) {
                continue;
            }

            instruction = is.getArgs().get(0);

            if (instruction.getOpcode() != Const.LDC) {
                continue;
            }

            ConstantMethodref cmr =
                constants.getConstantMethodref(is.getIndex());
            ConstantNameAndType cnatMethod =
                constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
            String nameMethod = constants.getConstantUtf8(cnatMethod.getNameIndex());

            if (! StringConstants.CLASS_DOLLAR.equals(nameMethod)) {
                continue;
            }

            Ldc ldc = (Ldc)instruction;
            Constant cv = constants.getConstantValue(ldc.getIndex());

            if (!(cv instanceof ConstantString)) {
                continue;
            }

            instruction = list.get(i+4);

            if (instruction.getOpcode() != Const.PUTSTATIC) {
                continue;
            }

            PutStatic ps = (PutStatic)instruction;

            if (ps.getValueref().getOpcode() != ByteCodeConstants.DUPLOAD ||
                ds.getOffset() != ps.getValueref().getOffset()) {
                continue;
            }

            ConstantFieldref cfr = constants.getConstantFieldref(gs.getIndex());
            ConstantNameAndType cnatField = constants.getConstantNameAndType(
                cfr.getNameAndTypeIndex());
            String signatureField =
                constants.getConstantUtf8(cnatField.getSignatureIndex());

            if (! StringConstants.INTERNAL_CLASS_SIGNATURE.equals(signatureField)) {
                continue;
            }

            String nameField = constants.getConstantUtf8(cnatField.getNameIndex());

            if (nameField.startsWith(StringConstants.CLASS_DOLLAR))
            {
                // motif 'x.class' classique trouvé !
                // Substitution par une constante de type 'ClassConstant'
                ConstantString cs = (ConstantString)cv;
                String signature = constants.getConstantUtf8(cs.getStringIndex());
                String internalName = signature.replace(
                    StringConstants.PACKAGE_SEPARATOR,
                    StringConstants.INTERNAL_PACKAGE_SEPARATOR);

                referenceMap.add(internalName);

                // Ajout du nom interne
                int index = constants.addConstantUtf8(internalName);
                // Ajout d'une nouvelle classe
                index = constants.addConstantClass(index);
                ldc = new Ldc(
                    Const.LDC, ii.getOffset(),
                    ii.getLineNumber(), index);

                // Remplacement de l'intruction GetStatic par l'instruction Ldc
                ReplaceDupLoadVisitor visitor = new ReplaceDupLoadVisitor(ds, ldc);

                for (int j=i+5; j<list.size(); j++)
                {
                    visitor.visit(list.get(j));
                    if (visitor.getParentFound() != null) {
                        break;
                    }
                }
            }
            else if (nameField.startsWith(StringConstants.ARRAY_DOLLAR))
            {
                // motif 'x[].class' trouvé !
                // Substitution par l'expression 'new x[0].getClass()'
                ConstantString cs = (ConstantString)cv;
                String signature = constants.getConstantUtf8(cs.getStringIndex());
                String signatureWithoutDimension =
                        SignatureUtil.cutArrayDimensionPrefix(signature);

                IConst iconst0 = new IConst(
                    ByteCodeConstants.ICONST, ii.getOffset(),
                    ii.getLineNumber(), 0);
                Instruction newArray;

                if (SignatureUtil.isObjectSignature(signatureWithoutDimension))
                {
                    //  8: iconst_0
                    //  9: anewarray 62    java/lang/String
                    //  12: invokevirtual 64    java/lang/Object:getClass    ()Ljava/lang/Class;
                    String tmp = signatureWithoutDimension.replace(
                            StringConstants.PACKAGE_SEPARATOR,
                            StringConstants.INTERNAL_PACKAGE_SEPARATOR);
                    String internalName = tmp.substring(1, tmp.length()-1);

                    // Ajout du nom de la classe pour generer la liste des imports
                    referenceMap.add(internalName);
                    // Ajout du nom interne
                    int index = constants.addConstantUtf8(internalName);
                    // Ajout d'une nouvelle classe
                    index = constants.addConstantClass(index);

                    newArray = new ANewArray(
                            Const.ANEWARRAY, ii.getOffset(),
                            ii.getLineNumber(), index, iconst0);
                }
                else
                {
                    //  8: iconst_0
                    //  9: newarray byte
                    //  11: invokevirtual 62    java/lang/Object:getClass    ()Ljava/lang/Class;
                    newArray = new NewArray(
                        Const.NEWARRAY, ii.getOffset(), ii.getLineNumber(),
                        SignatureUtil.getTypeFromSignature(signatureWithoutDimension),
                        iconst0);
                }

                // Ajout de la méthode 'getClass'
                int methodNameIndex = constants.addConstantUtf8("getClass");
                int methodDescriptorIndex =
                    constants.addConstantUtf8("()Ljava/lang/Class;");
                int nameAndTypeIndex = constants.addConstantNameAndType(
                    methodNameIndex, methodDescriptorIndex);
                int cmrIndex = constants.addConstantMethodref(
                    constants.getObjectClassIndex(), nameAndTypeIndex);

                Invokevirtual iv = new Invokevirtual(
                    Const.INVOKEVIRTUAL, ii.getOffset(),
                    ii.getLineNumber(), cmrIndex, newArray,
                    new ArrayList<>(0));

                // Remplacement de l'intruction
                ReplaceDupLoadVisitor visitor = new ReplaceDupLoadVisitor(ds, iv);

                for (int j=i+5; j<list.size(); j++)
                {
                    visitor.visit(list.get(j));
                    if (visitor.getParentFound() != null) {
                        break;
                    }
                }
            }
            else
            {
                continue;
            }

            // Retrait de l'intruction PutStatic
            list.remove(i+4);
            // Retrait de l'intruction DupStore
            list.remove(i+3);
            // Retrait de l'intruction Goto
            list.remove(i+2);
            // Retrait de l'intruction TernaryOpStore
            list.remove(i+1);
            // Retrait de l'intruction IfNotNull
            list.remove(i);

            // Recherche de l'attribut statique et ajout de l'attribut SYNTHETIC
            Field[] fields = classFile.getFields();
            int j = fields.length;

            while (j-- > 0)
            {
                Field field = fields[j];

                if (field.getNameIndex() == cnatField.getNameIndex())
                {
                    field.setAccessFlags(field.getAccessFlags() | Const.ACC_SYNTHETIC);
                    break;
                }
            }

            // Recherche de la méthode statique et ajout de l'attribut SYNTHETIC
            Method[] methods = classFile.getMethods();
            j = methods.length;

            while (j-- > 0)
            {
                Method method = methods[j];

                if (method.getNameIndex() == cnatMethod.getNameIndex())
                {
                    method.setAccessFlags(method.getAccessFlags() | Const.ACC_SYNTHETIC);
                    break;
                }
            }
        }
    }
}
