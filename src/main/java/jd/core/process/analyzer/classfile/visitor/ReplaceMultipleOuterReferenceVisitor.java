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
package jd.core.process.analyzer.classfile.visitor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.util.SignatureUtil;

/*
 * Replace 'this.this$3.this$2.this$1.this$0.xxx' by 'TestInnerClass.this.xxx'
 */
public class ReplaceMultipleOuterReferenceVisitor
    extends ReplaceOuterAccessorVisitor
{
    public ReplaceMultipleOuterReferenceVisitor(ClassFile classFile)
    {
        super(classFile);
    }

    @Override
    protected ClassFile match(Instruction instruction)
    {
        if (instruction.getOpcode() != Const.GETFIELD) {
            return null;
        }

        GetField gf = (GetField)instruction;

        switch (gf.getObjectref().getOpcode())
        {
        case Const.ALOAD:
            {
                ALoad aload = (ALoad)gf.getObjectref();
                if (aload.getIndex() != 0) {
                    return null;
                }
                Field field = this.classFile.getOuterThisField();
                if (field == null) {
                    return null;
                }
                ConstantPool constants = classFile.getConstantPool();
                ConstantFieldref cfr = constants.getConstantFieldref(gf.getIndex());
                if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
                    return null;
                }
                ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
                if (field.getNameIndex() != cnat.getNameIndex() ||
                    field.getDescriptorIndex() != cnat.getSignatureIndex()) {
                    return null;
                }
                return this.classFile.getOuterClass();
            }
        case ByteCodeConstants.OUTERTHIS:
            {
                ConstantPool constants = this.classFile.getConstantPool();
                GetStatic gs = (GetStatic)gf.getObjectref();
                ConstantFieldref cfr = constants.getConstantFieldref(gs.getIndex());
                String className =
                    constants.getConstantClassName(cfr.getClassIndex());
                ClassFile outerClass = this.classFile.getOuterClass();

                while (outerClass != null)
                {
                    if (outerClass.getThisClassName().equals(className))
                    {
                        Field outerField = outerClass.getOuterThisField();

                        if (outerField == null) {
                            return null;
                        }

                        cfr = constants.getConstantFieldref(gf.getIndex());
                        ConstantNameAndType cnat =
                            constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
                        String fieldName =
                            constants.getConstantUtf8(cnat.getNameIndex());

                        ConstantPool outerConstants =
                            outerClass.getConstantPool();
                        String outerFieldName =
                            outerConstants.getConstantUtf8(outerField.getNameIndex());

                        if (!fieldName.equals(outerFieldName)) {
                            return null;
                        }

                        String fieldDescriptor =
                            constants.getConstantUtf8(cnat.getSignatureIndex());
                        String outerFieldDescriptor =
                            outerConstants.getConstantUtf8(outerField.getDescriptorIndex());

                        if (!fieldDescriptor.equals(outerFieldDescriptor)) {
                            return null;
                        }

                        return outerClass.getOuterClass();
                    }

                    outerClass = outerClass.getOuterClass();
                }

                return null;
            }
        case Const.GETFIELD:
            {
                ConstantPool constants = this.classFile.getConstantPool();
                ConstantFieldref cfr = constants.getConstantFieldref(gf.getIndex());
                ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
                String descriptorName =
                    constants.getConstantUtf8(cnat.getSignatureIndex());
                if (!SignatureUtil.isObjectSignature(descriptorName)) {
                    return null;
                }

                ClassFile matchedClassFile = match(gf.getObjectref());
                if (matchedClassFile == null ||
                    !matchedClassFile.isAInnerClass()) {
                    return null;
                }

                Field matchedField = matchedClassFile.getOuterThisField();
                if (matchedField == null) {
                    return null;
                }

                String className =
                    constants.getConstantClassName(cfr.getClassIndex());

                if (!className.equals(matchedClassFile.getThisClassName())) {
                    return null;
                }

                String fieldName = constants.getConstantUtf8(cnat.getNameIndex());

                ConstantPool matchedConstants = matchedClassFile.getConstantPool();
                String matchedFieldName =
                    matchedConstants.getConstantUtf8(matchedField.getNameIndex());

                if (! fieldName.equals(matchedFieldName)) {
                    return null;
                }

                String matchedDescriptorName =
                    matchedConstants.getConstantUtf8(matchedField.getDescriptorIndex());

                if (! descriptorName.equals(matchedDescriptorName)) {
                    return null;
                }

                return matchedClassFile.getOuterClass();
            }
        default:
            return null;
        }
    }
}
