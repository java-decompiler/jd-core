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
package jd.core.model.instruction.fast.instruction;

import java.util.List;

import jd.core.model.instruction.bytecode.instruction.Instruction;

/**
 * try-catch-finally
 */
public class FastTry extends FastList
{
    private final List<FastCatch>   catches;
    private final List<Instruction> finallyInstructions;

    public FastTry(
            int opcode, int offset, int lineNumber, int branch,
            List<Instruction> instructions, List<FastCatch> catches,
            List<Instruction> finallyInstructions)
    {
        super(opcode, offset, lineNumber, branch, instructions);
        this.catches = catches;
        this.finallyInstructions = finallyInstructions;
    }

    public List<FastCatch> getCatches() {
        return catches;
    }

    public List<Instruction> getFinallyInstructions() {
        return finallyInstructions;
    }

    public record FastCatch(int exceptionOffset, int exceptionTypeIndex,
                int[] otherExceptionTypeIndexes, int localVarIndex,
                List<Instruction> instructions)
    {
    }
}
