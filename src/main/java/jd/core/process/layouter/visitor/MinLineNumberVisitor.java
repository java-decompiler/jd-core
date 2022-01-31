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
package jd.core.process.layouter.visitor;

import org.apache.bcel.Const;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;

public final class MinLineNumberVisitor
{
    private MinLineNumberVisitor() {
        super();
    }

    public static int visit(Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case ByteCodeConstants.ARRAYLOAD:
            return visit(((ArrayLoadInstruction)instruction).getArrayref());
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            return visit(((ArrayStoreInstruction)instruction).getArrayref());
        case ByteCodeConstants.ASSIGNMENT:
            return visit(((AssignmentInstruction)instruction).getValue1());
        case ByteCodeConstants.BINARYOP:
            return visit(((BinaryOperatorInstruction)instruction).getValue1());
        case ByteCodeConstants.PREINC:
            {
                IncInstruction ii = (IncInstruction)instruction;

                if (ii.isSingleStep())
                {
                    // Operator '++' or '--'
                    return instruction.getLineNumber();
                }
                return visit(ii.getValue());
            }
        case ByteCodeConstants.POSTINC:
            {
                IncInstruction ii = (IncInstruction)instruction;

                if (ii.isSingleStep())
                {
                    // Operator '++' or '--'
                    return visit(ii.getValue());
                }
                return instruction.getLineNumber();
            }
        case Const.INSTANCEOF:
            return visit(((InstanceOf)instruction).getObjectref());
        case Const.INVOKEINTERFACE,
             Const.INVOKEVIRTUAL,
             Const.INVOKESPECIAL:
            return visit(((InvokeNoStaticInstruction)instruction).getObjectref());
        case Const.POP:
            return visit(((Pop)instruction).getObjectref());
        case Const.PUTFIELD:
            return visit(((PutField)instruction).getObjectref());
        case ByteCodeConstants.TERNARYOP:
            return visit(((TernaryOperator)instruction).getTest());
        }

        return instruction.getLineNumber();
    }
}
