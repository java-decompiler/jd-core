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

import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.process.analyzer.instruction.bytecode.ComparisonInstructionAnalyzer;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;

/**
 * Transformation de l'ensemble 'if-break' en simple 'if':
 *
 * 35: ifge +6 -> 41
 * 38: goto +56 -> 94
 * 41: ...
 *
 *  to
 *
 * 35: ifLT +59 -> 94
 * 41: ...
 *
 */
public final class IfGotoToIfReconstructor
{
    private IfGotoToIfReconstructor() {
        super();
    }

    public static void reconstruct(List<Instruction> list)
    {
        int length = list.size();
        if (length < 3) {
            return;
        }

        int index = length-2;

        while (index-- > 0)
        {
            Instruction i = list.get(index);
            if (ByteCodeUtil.isIfInstruction(i.getOpcode(), true))
            {
                BranchInstruction bi = (BranchInstruction)i;

                i = list.get(index+1);
                if (i.getOpcode() != Const.GOTO) {
                    continue;
                }

                Goto g = (Goto)i;

                i = list.get(index+2);
                int jumpOffset = bi.getJumpOffset();

                if (jumpOffset <= g.getOffset() || i.getOffset() < jumpOffset) {
                    continue;
                }

                // Setup BranchInstruction
                bi.setBranch(g.getJumpOffset() - bi.getOffset());
                ComparisonInstructionAnalyzer.inverseComparison(bi);
                // Delete Goto
                list.remove(index+1);
            }
        }
    }
}
