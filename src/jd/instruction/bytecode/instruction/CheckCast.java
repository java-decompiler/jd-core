package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;


public class CheckCast extends IndexInstruction
{
	public Instruction objectref;

	public CheckCast(
		int opcode, int offset, int lineNumber, 
		int index, Instruction objectref)
	{
		super(opcode, offset, lineNumber, index);
		this.objectref = objectref;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if (constants == null)
			return null;
				
		String signature = constants.getConstantClassName(this.index);
		
		if (signature.charAt(0) == '[')
			return signature;

		return 'L' + signature + ';';
	}
	
	public int getPriority()
	{
		return 2;
	}
}
