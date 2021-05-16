package jd.core.model.instruction.fast.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.bytecode.instruction.Instruction;

/**
 * list & while(true)
 */
public class FastDeclaration extends Instruction
{
	public int index;
	public Instruction instruction;
	
	public FastDeclaration(
		int opcode, int offset, int lineNumber, 
		int index, Instruction instruction)
	{
		super(opcode, offset, lineNumber);
		this.index = index;
		this.instruction = instruction;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		return null;
	}
}
