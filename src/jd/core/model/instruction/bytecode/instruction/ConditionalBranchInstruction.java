package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;


public class ConditionalBranchInstruction extends BranchInstruction 
{
	public int cmp;
	
	public ConditionalBranchInstruction(
		int opcode, int offset, int lineNumber, int cmp, int branch)
	{
		super(opcode, offset, lineNumber, branch);
		this.cmp = cmp;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return "Z";
	}
}
