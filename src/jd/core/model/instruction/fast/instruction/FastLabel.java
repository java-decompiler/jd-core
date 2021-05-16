package jd.core.model.instruction.fast.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class FastLabel extends Instruction
{
	public Instruction instruction;
	
	public FastLabel(
		int opcode, int offset, int lineNumber, Instruction instruction)
	{
		super(opcode, offset, lineNumber);
		this.instruction = instruction;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		return this.instruction.getReturnedSignature(constants, localVariables);
	}
}
