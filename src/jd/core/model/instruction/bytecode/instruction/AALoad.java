package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;


public class AALoad extends ArrayLoadInstruction 
{
	public AALoad(
		int opcode, int offset, int lineNumber, 
		Instruction arrayref, Instruction indexref)
	{
		super(opcode, offset, lineNumber, arrayref, indexref, null);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		String signature = 
			this.arrayref.getReturnedSignature(constants, localVariables);
		
		if ((signature == null) || (signature.length() == 0) || 
			(signature.charAt(0) != '['))
			return null;
		
		return signature.substring(1);
	}
}
