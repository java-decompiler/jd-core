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
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;

import java.util.ArrayList;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokespecial;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.process.analyzer.classfile.visitor.CompareInstructionVisitor;
import jd.core.process.analyzer.classfile.visitor.SearchInstructionByOpcodeVisitor;

public final class InitInstanceFieldsReconstructor
{
    private InitInstanceFieldsReconstructor() {
        super();
    }

    public static void reconstruct(ClassFile classFile)
    {
        List<PutField> putFieldList = new ArrayList<>();
        ConstantPool constants = classFile.getConstantPool();
        Method[] methods = classFile.getMethods();

        if (methods == null) {
            return;
        }

        int methodIndex = methods.length;
        Method putFieldListMethod = null;

        // Recherche du dernier constructeur ne faisant pas appel a 'this(...)'
        while (methodIndex > 0)
        {
            final Method method = methods[--methodIndex];

            if ((method.getAccessFlags() & (Const.ACC_SYNTHETIC|Const.ACC_BRIDGE)) != 0 ||
                method.getCode() == null ||
                method.getFastNodes() == null ||
                method.containsError() ||
                method.getNameIndex() != constants.getInstanceConstructorIndex()) {
                continue;
            }

            List<Instruction> list = method.getFastNodes();
            if (list == null) {
                continue;
            }

            int length = list.size();

            if (length > 0)
            {
                int j = getSuperCallIndex(classFile, constants, list);

                if (j < 0) {
                    continue;
                }

                j++;

                int lineNumberBefore = j > 0 ?
                    list.get(j-1).getLineNumber() : Instruction.UNKNOWN_LINE_NUMBER;
                Instruction instruction = null;

                // Store init values
                while (j < length)
                {
                    instruction = list.get(j++);
                    if (instruction.getOpcode() != Const.PUTFIELD) {
                        break;
                    }

                    PutField putField = (PutField)instruction;
                    ConstantFieldref cfr = constants.getConstantFieldref(putField.getIndex());

                    if (cfr.getClassIndex() != classFile.getThisClassIndex() ||
                        putField.getObjectref().getOpcode() != Const.ALOAD) {
                        break;
                    }

                    ALoad aLaod = (ALoad)putField.getObjectref();
                    if (aLaod.getIndex() != 0) {
                        break;
                    }

                    Instruction valueInstruction =
                        SearchInstructionByOpcodeVisitor.visit(
                                putField.getValueref(), Const.ALOAD);
                    if (valueInstruction != null &&
                        ((ALoad)valueInstruction).getIndex() != 0) {
                        break;
                    }
                    if (SearchInstructionByOpcodeVisitor.visit(
                            putField.getValueref(), ByteCodeConstants.LOAD) != null) {
                        break;
                    }
                    if (SearchInstructionByOpcodeVisitor.visit(
                            putField.getValueref(), Const.ILOAD) != null) {
                        break;
                    }

                    putFieldList.add(putField);
                    putFieldListMethod = method;
                }

                // Filter list of 'PUTFIELD'
                if (lineNumberBefore != Instruction.UNKNOWN_LINE_NUMBER &&
                    instruction != null)
                {
                    int i = putFieldList.size();
                    int lineNumberAfter = instruction.getLineNumber();

                    // Si l'instruction qui suit la serie de 'PUTFIELD' est une
                    // 'RETURN' ayant le même numéro de ligne que le dernier
                    // 'PUTFIELD', le constructeur est synthetique et ne sera
                    // pas filtre.
                    if (instruction.getOpcode() != Const.RETURN ||
                        j != length || i == 0 ||
                        lineNumberAfter != putFieldList.get(i-1).getLineNumber())
                    {
                        while (i-- > 0)
                        {
                            int lineNumber = putFieldList.get(i).getLineNumber();

                            if (lineNumberBefore <= lineNumber &&
                                lineNumber <= lineNumberAfter)
                            {
                                // Remove 'PutField' instruction if it used in
                                // code block of constructor
                                putFieldList.remove(i);
                            }
                        }
                    }
                }
            }

            break;
        }

        // Filter list
        CompareInstructionVisitor visitor =    new CompareInstructionVisitor();

        while (methodIndex > 0)
        {
            final Method method = methods[--methodIndex];

            if ((method.getAccessFlags() &
                    (Const.ACC_SYNTHETIC|Const.ACC_BRIDGE)) != 0) {
                continue;
            }
            if (method.getCode() == null) {
                continue;
            }
            if (method.getNameIndex() != constants.getInstanceConstructorIndex()) {
                continue;
            }

            List<Instruction> list = method.getFastNodes();
            int length = list.size();

            if (length > 0)
            {
                // Filter init values
                int j = getSuperCallIndex(classFile, constants, list);

                if (j < 0) {
                    continue;
                }

                int firstPutFieldIndex = j + 1;
                int putFieldListLength = putFieldList.size();

                // If 'putFieldList' is longer than 'list',
                // remove extra 'putField'.
                while (firstPutFieldIndex+putFieldListLength > length) {
                    putFieldList.remove(--putFieldListLength);
                }

                for (int i=0; i<putFieldListLength; i++)
                {
                    Instruction initFieldInstruction = putFieldList.get(i);
                    Instruction instruction = list.get(firstPutFieldIndex+i);

                    if (initFieldInstruction.getLineNumber() != instruction.getLineNumber() ||
                        !visitor.visit(initFieldInstruction, instruction))
                    {
                        while (i < putFieldListLength) {
                            putFieldList.remove(--putFieldListLength);
                        }
                        break;
                    }
                }
            }
        }

        // Setup initial values
        int putFieldListLength = putFieldList.size();
        Field[] fields = classFile.getFields();

        if (putFieldListLength > 0 && fields != null)
        {
            int fieldLength = fields.length;
            int putFieldListIndex = putFieldListLength;

            while (putFieldListIndex-- > 0)
            {
                PutField putField = putFieldList.get(putFieldListIndex);
                ConstantFieldref cfr = constants.getConstantFieldref(putField.getIndex());
                ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
                int fieldIndex;

                for (fieldIndex=0; fieldIndex<fieldLength; fieldIndex++)
                {
                    Field field = fields[fieldIndex];

                    if (cnat.getNameIndex() == field.getNameIndex() &&
                        cnat.getSignatureIndex() == field.getDescriptorIndex() &&
                        (field.getAccessFlags() & Const.ACC_STATIC) == 0)
                    {
                        // Field found
                        Instruction valueref = putField.getValueref();
                        field.setValueAndMethod(valueref, putFieldListMethod);
                        if (valueref.getOpcode() == ByteCodeConstants.NEWANDINITARRAY) {
                            valueref.setOpcode(ByteCodeConstants.INITARRAY);
                        }
                        break;
                    }
                }

                if (fieldIndex == fieldLength)
                {
                    // Field not found
                    // Remove putField not used to initialize fields
                    putFieldList.remove(putFieldListIndex);
                    putFieldListLength--;
                }
            }

            if (putFieldListLength > 0)
            {
                // Remove instructions from constructors
                methodIndex = methods.length;

                while (methodIndex-- > 0)
                {
                    final Method method = methods[methodIndex];

                    if ((method.getAccessFlags() &
                            (Const.ACC_SYNTHETIC|Const.ACC_BRIDGE)) != 0) {
                        continue;
                    }
                    if (method.getCode() == null) {
                        continue;
                    }
                    if (method.getNameIndex() != constants.getInstanceConstructorIndex()) {
                        continue;
                    }

                    List<Instruction> list = method.getFastNodes();

                    if (!list.isEmpty())
                    {
                        // Remove instructions
                        putFieldListIndex = 0;
                        int putFieldIndex = putFieldList.get(putFieldListIndex).getIndex();

                        for (int index=0; index<list.size(); index++)
                        {
                            Instruction instruction = list.get(index);
                            if (instruction.getOpcode() != Const.PUTFIELD) {
                                continue;
                            }

                            PutField putField = (PutField)instruction;
                            if (putField.getIndex() != putFieldIndex) {
                                continue;
                            }

                            ConstantFieldref cfr = constants.getConstantFieldref(putField.getIndex());
                            if (cfr.getClassIndex() != classFile.getThisClassIndex() ||
                                putField.getObjectref().getOpcode() != Const.ALOAD) {
                                continue;
                            }

                            ALoad aLoad = (ALoad)putField.getObjectref();
                            if (aLoad.getIndex() != 0) {
                                continue;
                            }

                            /*
                             * Do not remove the PutField instruction if it loads a constructor parameter.
                             * If field is assigned to a constructor parameter, the instruction can only be
                             * inside the constructor.
                             */
                            Instruction putFieldValueref = putField.getValueref();
                            // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved    
                            if (putFieldValueref instanceof ALoad && ((ALoad) putFieldValueref).getIndex() != 0) {
                                continue;
                            }

                            list.remove(index--);

                            if (++putFieldListIndex >= putFieldListLength) {
                                break;
                            }
                            putFieldIndex =
                                putFieldList.get(putFieldListIndex).getIndex();

                        }
                    }
                }
            }
        }
    }

    private static int getSuperCallIndex(
        ClassFile classFile, ConstantPool constants, List<Instruction> list)
    {
        int length = list.size();

        for (int i=0; i<length; i++)
        {
            Instruction instruction = list.get(i);

            if (instruction.getOpcode() != Const.INVOKESPECIAL) {
                continue;
            }

            Invokespecial is = (Invokespecial)instruction;

            if (is.getObjectref().getOpcode() != Const.ALOAD ||
                ((ALoad)is.getObjectref()).getIndex() != 0) {
                continue;
            }

            ConstantMethodref cmr = constants.getConstantMethodref(is.getIndex());
            ConstantNameAndType cnat =
                constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

            if (cnat.getNameIndex() != constants.getInstanceConstructorIndex()) {
                continue;
            }

            if (cmr.getClassIndex() == classFile.getThisClassIndex()) {
                return -1;
            }

            return i;
        }

        return -1;
    }
}
