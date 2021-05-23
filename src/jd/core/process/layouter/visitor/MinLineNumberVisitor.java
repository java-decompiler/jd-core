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
import jd.core.model.instruction.fast.FastConstants;



public class MinLineNumberVisitor 
{
	public static int visit(Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLOAD:
			return visit(((ArrayLoadInstruction)instruction).arrayref);
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			return visit(((ArrayStoreInstruction)instruction).arrayref);
		case ByteCodeConstants.ASSIGNMENT:
			return visit(((AssignmentInstruction)instruction).value1);
		case ByteCodeConstants.BINARYOP:
			return visit(((BinaryOperatorInstruction)instruction).value1);
		case ByteCodeConstants.PREINC:		
			{	
				IncInstruction ii = (IncInstruction)instruction;
				
				switch (ii.count)
				{
				case -1:
				case 1:
					return instruction.lineNumber;
				default:
					return visit(ii.value);
				}
			}
		case ByteCodeConstants.POSTINC:			
			{
				IncInstruction ii = (IncInstruction)instruction;
				
				switch (ii.count)
				{
				case -1:
				case 1:
					return visit(ii.value);
				default:
					return instruction.lineNumber;
				}
			}
		case ByteCodeConstants.INSTANCEOF:
			return visit(((InstanceOf)instruction).objectref);
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKEVIRTUAL:
		case ByteCodeConstants.INVOKESPECIAL:
			return visit(((InvokeNoStaticInstruction)instruction).objectref);
		case ByteCodeConstants.POP:
			return visit(((Pop)instruction).objectref);
		case ByteCodeConstants.PUTFIELD:
			return visit(((PutField)instruction).objectref);
		case FastConstants.TERNARYOP:
			return visit(((TernaryOperator)instruction).test);
		}
		
		return instruction.lineNumber;
	}
}
