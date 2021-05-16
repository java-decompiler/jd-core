package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

public abstract class Instruction 
{
	public static int UNKNOWN_LINE_NUMBER = 0;
	
	public int opcode;
	public int offset;
	public int lineNumber;

	public Instruction(int opcode, int offset, int lineNumber)
	{
		this.opcode = opcode;
		this.offset = offset;
		this.lineNumber = lineNumber;
	}

	public abstract String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables);
	
	public int getPriority()
	{
		return 0;
	}
}
