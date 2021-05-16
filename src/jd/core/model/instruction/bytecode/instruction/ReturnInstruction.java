package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

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
