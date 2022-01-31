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

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.process.analyzer.instruction.bytecode.ComparisonInstructionAnalyzer;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;
import jd.core.process.analyzer.instruction.fast.visitor.ReplaceInstructionVisitor;

/*
 * Reconstruction des operateurs ternaires.
 * Contenu de la liste d'instructions :
 *  offset-1) If instruction (IF || IFCMP || IFXNULL || COMPLEXIF)
 *    offset) TernaryOpStore
 *  offset+1) Goto
 */
public final class TernaryOpReconstructor
{
    private TernaryOpReconstructor() {
        super();
    }

    public static void reconstruct(List<Instruction> list)
    {
        int length = list.size();

        for (int index=1; index<length; ++index)
        {
            Instruction i = list.get(index);

            if (i.getOpcode() == ByteCodeConstants.TERNARYOPSTORE &&
                index+2<length)
            {
                // Search test
                Instruction gi = list.get(index+1);
                Instruction afterGi = list.get(index+2);
                Instruction test = null;
                int indexTest = index;

                while (indexTest-- > 0)
                {
                    Instruction instruction = list.get(indexTest);
                    int opcode = instruction.getOpcode();

                    if (ByteCodeUtil.isIfInstruction(opcode, true))
                    {
                        int jumpOffset =
                            ((BranchInstruction)instruction).getJumpOffset();
                        if (gi.getOffset() < jumpOffset &&
                            jumpOffset <= afterGi.getOffset())
                        {
                            test = instruction;
                            break;
                        }
                    }
                }

                if (test == null) {
                    continue;
                }

                TernaryOpStore value1 = (TernaryOpStore)i;

                ComparisonInstructionAnalyzer.inverseComparison(test);

                TernaryOperator fto = new TernaryOperator(
                    ByteCodeConstants.TERNARYOP,
                    value1.getTernaryOp2ndValueOffset(), test.getLineNumber(),
                    test, value1.getObjectref(), null);

                ReplaceInstructionVisitor visitor =
                    new ReplaceInstructionVisitor(
                            value1.getTernaryOp2ndValueOffset(), fto);

                int indexVisitor = index+2;
                while (indexVisitor<length && visitor.getOldInstruction()==null) {
                    visitor.visit(list.get(indexVisitor++));
                }

                fto.setValue2(visitor.getOldInstruction());

                if (isBooleanConstant(fto.getValue1()) &&
                    isBooleanConstant(fto.getValue2()))
                {
                    if (((IConst)fto.getValue1()).getValue() == 0) {
                        ComparisonInstructionAnalyzer.inverseComparison(fto.getTest());
                    }

                    visitor.init(fto.getOffset(), fto.getTest());

                    indexVisitor = index+2;
                    while (indexVisitor<length && visitor.getOldInstruction()==null) {
                        visitor.visit(list.get(indexVisitor++));
                    }
                }

                // Remove Goto
                list.remove(index+1);
                // Remove TernaryOpStore
                list.remove(index);
                // Remove test
                list.remove(indexTest);

                index -= 2;
                length -= 3;
            }
        }
    }

    private static boolean isBooleanConstant(Instruction instruction)
    {
        return instruction != null
            && ByteCodeUtil.isLoadIntValue(instruction.getOpcode())
            && "Z".equals(instruction.getReturnedSignature(null, null));
    }
}
