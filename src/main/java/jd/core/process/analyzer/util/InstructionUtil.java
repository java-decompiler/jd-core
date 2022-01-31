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
package jd.core.process.analyzer.util;

import java.util.List;

import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;

public final class InstructionUtil
{
    private InstructionUtil() {
        super();
    }

    public static Instruction getInstructionAt(
            List<Instruction> list, int offset)
    {
        if (list == null || list.isEmpty()) {
            return null;
        }

        if (list.get(0).getOffset() >= offset) {
            return list.get(0);
        }

        int length = list.size();

        if (length == 1 || list.get(length-1).getOffset() < offset) {
            return null;
        }

        int firstIndex = 0;
        int lastIndex = length-1;

        while (true)
        {
            int medIndex = (lastIndex + firstIndex) / 2;
            Instruction i = list.get(medIndex);

            if (i.getOffset() < offset) {
                firstIndex = medIndex+1;
            } else if (list.get(medIndex-1).getOffset() >= offset) {
                lastIndex = medIndex-1;
            } else {
                return i;
            }
        }
    }

    public static int getIndexForOffset(
            List<Instruction> list, int offset)
    {
        if (offset < 0) {
            throw new IllegalStateException("offset=" + offset);
        }

        if (list == null || list.isEmpty()) {
            return -1;
        }

        if (list.get(0).getOffset() >= offset) {
            return 0;
        }

        int length = list.size();

        if (length == 1 || list.get(length-1).getOffset() < offset) {
            return -1;
        }

        int firstIndex = 0;
        int lastIndex = length-1;

        while (true)
        {
            int medIndex = (lastIndex + firstIndex) / 2;
            Instruction i = list.get(medIndex);

            if (i.getOffset() < offset) {
                firstIndex = medIndex+1;
            } else if (list.get(medIndex-1).getOffset() >= offset) {
                lastIndex = medIndex-1;
            } else {
                return medIndex;
            }
        }
    }

    public static boolean checkNoJumpToInterval(
            List<Instruction> list, int firstIndex, int afterIndex,
            int firstOffset, int lastOffset)
        {
            for (int index=firstIndex; index<afterIndex; index++)
            {
                Instruction i = list.get(index);

                if (ByteCodeUtil.isIfOrGotoInstruction(i.getOpcode(), false))
                {
                    int jumpOffset = ((BranchInstruction)i).getJumpOffset();
                    if (firstOffset < jumpOffset && jumpOffset <= lastOffset) {
                        return false;
                    }
                }
            }

            return true;
        }
}
