package jd.instruction.fast.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;
import jd.instruction.bytecode.instruction.Instruction;

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
