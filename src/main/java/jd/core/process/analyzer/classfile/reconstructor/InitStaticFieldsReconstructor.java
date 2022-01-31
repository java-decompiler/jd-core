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

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.process.analyzer.classfile.visitor.SearchInstructionByOpcodeVisitor;

public final class InitStaticFieldsReconstructor
{
    private InitStaticFieldsReconstructor() {
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

        // Search field initialisation from the begining
        int indexInstruction = 0;
        int length = list.size();
        int indexField = 0;

        while (indexInstruction < length)
        {
            Instruction instruction = list.get(indexInstruction);

            if (instruction.getOpcode() != Const.PUTSTATIC) {
                break;
            }

            PutStatic putStatic = (PutStatic)instruction;
            ConstantFieldref cfr = constants.getConstantFieldref(putStatic.getIndex());

            if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
                break;
            }

            ConstantNameAndType cnat =
                constants.getConstantNameAndType(cfr.getNameAndTypeIndex());

            int lengthBeforeSubstitution = list.size();

            while (indexField < fields.length)
            {
                Field field = fields[indexField++];

                if ((field.getAccessFlags() & Const.ACC_STATIC) != 0 &&
                    cnat.getSignatureIndex() == field.getDescriptorIndex() &&
                    cnat.getNameIndex() == field.getNameIndex())
                {
                    Instruction valueref = putStatic.getValueref();

                    if (SearchInstructionByOpcodeVisitor.visit(
                            valueref, Const.ALOAD) != null) {
                        break;
                    }
                    if (SearchInstructionByOpcodeVisitor.visit(
                            valueref, ByteCodeConstants.LOAD) != null) {
                        break;
                    }
                    if (SearchInstructionByOpcodeVisitor.visit(
                            valueref, Const.ILOAD) != null) {
                        break;
                    }

                    field.setValueAndMethod(valueref, method);
                    if (valueref.getOpcode() == ByteCodeConstants.NEWANDINITARRAY) {
                        valueref.setOpcode(ByteCodeConstants.INITARRAY);
                    }
                    list.remove(indexInstruction--);
                    break;
                }
            }

            // La substitution a-t-elle ete faite ?
            if (lengthBeforeSubstitution == list.size())
            {
                // Non -> On arrête.
                break;
            }

            indexInstruction++;
        }

        // Search field initialisation from the end
        indexInstruction = list.size();

        if (indexInstruction > 0)
        {
            // Saute la derniere instruction 'return'
            indexInstruction--;
            indexField = fields.length;

            while (indexInstruction-- > 0)
            {
                Instruction instruction = list.get(indexInstruction);

                if (instruction.getOpcode() != Const.PUTSTATIC) {
                    break;
                }

                PutStatic putStatic = (PutStatic)instruction;
                ConstantFieldref cfr = constants.getConstantFieldref(putStatic.getIndex());

                if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
                    break;
                }

                ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());

                int lengthBeforeSubstitution = list.size();

                while (indexField-- > 0)
                {
                    Field field = fields[indexField];

                    if ((field.getAccessFlags() & Const.ACC_STATIC) != 0 &&
                        cnat.getSignatureIndex() == field.getDescriptorIndex() &&
                        cnat.getNameIndex() == field.getNameIndex())
                    {
                        Instruction valueref = putStatic.getValueref();

                        if (SearchInstructionByOpcodeVisitor.visit(
                                valueref, Const.ALOAD) != null) {
                            break;
                        }
                        if (SearchInstructionByOpcodeVisitor.visit(
                                valueref, ByteCodeConstants.LOAD) != null) {
                            break;
                        }
                        if (SearchInstructionByOpcodeVisitor.visit(
                                valueref, Const.ILOAD) != null) {
                            break;
                        }

                        field.setValueAndMethod(valueref, method);
                        if (valueref.getOpcode() == ByteCodeConstants.NEWANDINITARRAY) {
                            valueref.setOpcode(ByteCodeConstants.INITARRAY);
                        }
                        list.remove(indexInstruction);
                        break;
                    }
                }

                // La substitution a-t-elle ete faite ?
                if (lengthBeforeSubstitution == list.size())
                {
                    // Non -> On arrête.
                    break;
                }
            }
        }
    }
}
