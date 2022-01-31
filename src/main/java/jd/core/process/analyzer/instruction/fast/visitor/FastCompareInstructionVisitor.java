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
package jd.core.process.analyzer.instruction.fast.visitor;

import java.util.List;

import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastSynchronized;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;
import jd.core.process.analyzer.classfile.visitor.CompareInstructionVisitor;

public class FastCompareInstructionVisitor extends CompareInstructionVisitor
{
    public boolean visit(
        List<Instruction> list1, List<Instruction> list2,
        int index1, int index2, int length)
    {
        if (index1+length <= list1.size() && index2+length <= list2.size())
        {
            while (length-- > 0)
            {
                if (!visit(list1.get(index1++), list2.get(index2++))) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean visit(Instruction i1, Instruction i2)
    {
        if (i1.getOpcode() != i2.getOpcode()) {
            return false;
        }
        switch (i1.getOpcode())
        {
        case FastConstants.TRY:
            {
                FastTry ft1 = (FastTry)i1;
                FastTry ft2 = (FastTry)i2;

                int i = ft1.getCatches().size();

                if (i != ft2.getCatches().size()) {
                    return false;
                }

                if (ft1.getFinallyInstructions() == null)
                {
                    if (ft2.getFinallyInstructions() != null) {
                        return false;
                    }
                }
                else if (ft2.getFinallyInstructions() == null)
                {
                    if (ft1.getFinallyInstructions() != null) {
                        return false;
                    }
                } else if (! visit(
                        ft1.getFinallyInstructions(),
                        ft2.getFinallyInstructions())) {
                    return false;
                }

                while (i-- > 0)
                {
                    FastCatch fc1 = ft1.getCatches().get(i);
                    FastCatch fc2 = ft2.getCatches().get(i);

                    if (fc1.exceptionTypeIndex() != fc2.exceptionTypeIndex() ||
                        ! visit(fc1.instructions(), fc2.instructions()) ||
                        ! compareExceptionTypeIndexes(
                            fc1.otherExceptionTypeIndexes(), fc2.otherExceptionTypeIndexes())) {
                        return false;
                    }
                }

                return visit(ft1.getInstructions(), ft2.getInstructions());
            }
        case FastConstants.SYNCHRONIZED:
            {
                FastSynchronized fs1 = (FastSynchronized)i1;
                FastSynchronized fs2 = (FastSynchronized)i2;

                if (! visit(fs1.getMonitor(), fs2.getMonitor())) {
                    return false;
                }

                return visit(fs1.getInstructions(), fs2.getInstructions());
            }
        default:
            return super.visit(i1, i2);
        }
    }

    private static boolean compareExceptionTypeIndexes(
        int[] otherExceptionTypeIndexes1, int[] otherExceptionTypeIndexes2)
    {
        if (otherExceptionTypeIndexes1 == null)
        {
            return otherExceptionTypeIndexes2 == null;
        }
        if (otherExceptionTypeIndexes2 == null) {
            return false;
        }

        int i = otherExceptionTypeIndexes1.length;

        if (i != otherExceptionTypeIndexes2.length) {
            return false;
        }

        while (i-- > 0)
        {
            if (otherExceptionTypeIndexes1[i] != otherExceptionTypeIndexes2[i]) {
                return false;
            }
        }

        return true;
    }
}
