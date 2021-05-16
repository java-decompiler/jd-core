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
