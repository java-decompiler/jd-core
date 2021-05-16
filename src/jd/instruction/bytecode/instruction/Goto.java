package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;

public class Goto extends BranchInstruction 
{
	public Goto(int opcode, int offset, int lineNumber, int branch)
	{
		super(opcode, offset, lineNumber, branch);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return null;
	}
}
