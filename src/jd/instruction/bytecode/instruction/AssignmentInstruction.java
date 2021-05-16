package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;


public class AssignmentInstruction extends BinaryOperatorInstruction 
{
	public AssignmentInstruction(
		int opcode, int offset, int lineNumber, int priority, String operator, 
		Instruction value1, Instruction value2)
	{
		super(
			opcode, offset, lineNumber, priority, 
			null, operator, value1, value2);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		return this.value2.getReturnedSignature(constants, localVariables);
	}
}
