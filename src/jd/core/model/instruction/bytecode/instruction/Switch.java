package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;


public class Switch extends Instruction
{
	public Instruction key;
	public int         defaultOffset;
	public int[]       offsets;
	
	public Switch(
			int opcode, int offset, int lineNumber, 
			Instruction key, int defaultOffset, int[] offsets)
	{
		super(opcode, offset, lineNumber);
		this.key = key;
		this.defaultOffset = defaultOffset;
		this.offsets = offsets;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return null;
	}
}
