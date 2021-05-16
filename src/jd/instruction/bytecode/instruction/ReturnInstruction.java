package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;

public class ReturnInstruction extends Instruction 
{
	public Instruction valueref;

	public ReturnInstruction(
		int opcode, int offset, int lineNumber, Instruction valueref)
	{
		super(opcode, offset, lineNumber);
		this.valueref = valueref;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return null;
	}
}
