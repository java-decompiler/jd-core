package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;


public class AssertInstruction extends Instruction 
{
	public Instruction test;
	public Instruction msg;

	public AssertInstruction(
		int opcode, int offset, int lineNumber, 
		Instruction test, Instruction msg)
	{
		super(opcode, offset, lineNumber);
		this.test = test;
		this.msg = msg;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		return null;
	}
}
