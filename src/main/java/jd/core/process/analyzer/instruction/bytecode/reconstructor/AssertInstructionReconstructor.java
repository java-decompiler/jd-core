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
package jd.core.process.analyzer.instruction.bytecode.reconstructor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.util.StringConstants;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;

/*
 * Recontruction des des instructions 'assert' depuis le motif :
 * ...
 * complexif( (!($assertionsDisabled)) && (test) )
 *  athrow( newinvoke( classindex="AssertionError", args=["msg"] ));
 * ...
 */
public final class AssertInstructionReconstructor
{
    private AssertInstructionReconstructor() {
        super();
    }

    public static void reconstruct(ClassFile classFile, List<Instruction> list)
    {
        int index = list.size();
        if (index-- == 0) {
            return;
        }

        while (index-- > 1)
        {
            Instruction instruction = list.get(index);

            if (instruction.getOpcode() != Const.ATHROW) {
                continue;
            }

            // AThrow trouve
            AThrow athrow = (AThrow)instruction;
            if (athrow.getValue().getOpcode() != ByteCodeConstants.INVOKENEW) {
                continue;
            }

            instruction = list.get(index-1);
            if (instruction.getOpcode() != ByteCodeConstants.COMPLEXIF) {
                continue;
            }

            // ComplexConditionalBranchInstruction trouve
            ComplexConditionalBranchInstruction cbl =
                (ComplexConditionalBranchInstruction)instruction;
            int jumpOffset = cbl.getJumpOffset();
            int lastOffset = list.get(index+1).getOffset();

            if (athrow.getOffset() >= jumpOffset || jumpOffset > lastOffset) {
                continue;
            }

            if (cbl.getCmp() != 2 || cbl.getInstructions().isEmpty()) {
                continue;
            }

            instruction = cbl.getInstructions().get(0);
            if (instruction.getOpcode() != ByteCodeConstants.IF) {
                continue;
            }

            IfInstruction if1 = (IfInstruction)instruction;
            if (if1.getCmp() != 7 || if1.getValue().getOpcode() != Const.GETSTATIC) {
                continue;
            }

            GetStatic gs = (GetStatic)if1.getValue();
            ConstantPool constants = classFile.getConstantPool();
            ConstantFieldref cfr = constants.getConstantFieldref(gs.getIndex());

            if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
                continue;
            }

            ConstantNameAndType cnat =
                constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
            String fieldName = constants.getConstantUtf8(cnat.getNameIndex());

            if (! "$assertionsDisabled".equals(fieldName)) {
                continue;
            }

            InvokeNew in = (InvokeNew)athrow.getValue();
            ConstantMethodref cmr =
                constants.getConstantMethodref(in.getIndex());
            String className = constants.getConstantClassName(cmr.getClassIndex());

            if (! StringConstants.JAVA_LANG_ASSERTION_ERROR.equals(className)) {
                continue;
            }

            // Remove first condition "!($assertionsDisabled)"
            cbl.getInstructions().remove(0);

            Instruction msg = in.getArgs().isEmpty() ? null : in.getArgs().get(0);
            list.remove(index--);

            list.set(index, new AssertInstruction(
                ByteCodeConstants.ASSERT, athrow.getOffset(),
                cbl.getLineNumber(), cbl, msg));
        }
    }
}
