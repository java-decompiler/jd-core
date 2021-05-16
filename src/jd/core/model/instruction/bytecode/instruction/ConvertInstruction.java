package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;


public class ConvertInstruction extends Instruction 
{
	final public String signature;
	public Instruction value;

	public ConvertInstruction(
			int opcode, int offset, int lineNumber, Instruction value, String signature)
	{
		super(opcode, offset, lineNumber);
		this.value = value;
		this.signature = signature;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		return this.signature;
	}
	
	public int getPriority()
	{
		return 2;
	}
}
