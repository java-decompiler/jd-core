package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;


public class LoadInstruction extends IndexInstruction 
{
	private String signature;
	
	public LoadInstruction(
		int opcode, int offset, int lineNumber, int index, String signature)
	{
		super(opcode, offset, lineNumber, index);
		this.signature = signature;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		return this.signature;
	}
}
