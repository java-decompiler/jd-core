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
package jd.core.process.analyzer.instruction.fast.reconstructor;

import org.apache.bcel.Const;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.process.analyzer.instruction.bytecode.ComparisonInstructionAnalyzer;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;

/*
 * Recontruction de l'instruction 'return (b1 == 1);' depuis la sequence :
 * 46: if (b1 == 1)
 *   46: return true;
 * 48: return false;
 */
public final class TernaryOpInReturnReconstructor
{
    private TernaryOpInReturnReconstructor() {
        super();
    }

    public static void reconstruct(List<Instruction> list)
    {
        for (int index=list.size()-1; index>=0; --index)
        {
            if (list.get(index).getOpcode() != ByteCodeConstants.XRETURN) {
                continue;
            }

            ReturnInstruction ri1 = (ReturnInstruction)list.get(index);
            int opcode = ri1.getValueref().getOpcode();

            if (opcode != Const.SIPUSH &&
                opcode != Const.BIPUSH &&
                opcode != ByteCodeConstants.ICONST) {
                continue;
            }

            IConst iConst1 = (IConst)ri1.getValueref();

            if (!"Z".equals(iConst1.getSignature())) {
                continue;
            }

            if (index <= 0) {
                continue;
            }

            int index2 = index - 1;

            if (list.get(index2).getOpcode() != ByteCodeConstants.XRETURN) {
                continue;
            }

            ReturnInstruction ri2 = (ReturnInstruction)list.get(index2);

            if (ri1.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER &&
                ri1.getLineNumber() > ri2.getLineNumber()) {
                continue;
            }

            opcode = ri2.getValueref().getOpcode();

            if (opcode != Const.SIPUSH &&
                opcode != Const.BIPUSH &&
                opcode != ByteCodeConstants.ICONST) {
                continue;
            }

            IConst iConst2 = (IConst)ri2.getValueref();

            if (!"Z".equals(iConst2.getSignature())) {
                continue;
            }

            if (index2 <= 0) {
                continue;
            }

            Instruction instruction = list.get(--index2);

            opcode = instruction.getOpcode();

            if (!ByteCodeUtil.isIfInstruction(opcode, true)) {
                continue;
            }

            BranchInstruction bi = (BranchInstruction)instruction;
            int offset = bi.getJumpOffset();

            if (ri2.getOffset() >= offset || offset > ri1.getOffset()) {
                continue;
            }

            // Verification qu'aucune instruction saute sur 'ri1'
            boolean found = false;
            int i = index2;

            while (i-- > 0)
            {
                instruction = list.get(i);
                opcode = instruction.getOpcode();

                if (opcode == Const.GOTO)
                {
                    int jumpOffset = ((Goto)instruction).getJumpOffset();
                    if (ri2.getOffset() < jumpOffset && jumpOffset <= ri1.getOffset())
                    {
                        found = true;
                        break;
                    }
                }
                else if (ByteCodeUtil.isIfInstruction(opcode, true))
                {
                    int jumpOffset =
                        ((BranchInstruction)instruction).getJumpOffset();
                    if (ri2.getOffset() < jumpOffset && jumpOffset <= ri1.getOffset())
                    {
                        found = true;
                        break;
                    }
                }
            }

            if (found) {
                continue;
            }

            if (iConst2.getValue() == 1) {
                ComparisonInstructionAnalyzer.inverseComparison( bi );
            }

            list.remove(index);
            list.remove(index2);

            ri2.setValueref(bi);

            index -= 3;
        }
    }
}
