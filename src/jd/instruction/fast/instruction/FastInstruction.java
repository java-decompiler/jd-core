package jd.instruction.fast.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;
import jd.instruction.bytecode.instruction.Instruction;

public class FastInstruction extends Instruction
{
	public Instruction instruction;

	public FastInstruction(
		int opcode, int offset, int lineNumber, Instruction instruction)
	{
		super(opcode, offset, lineNumber);
		this.instruction = instruction;
	}
	
	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		return null;
	}
}
