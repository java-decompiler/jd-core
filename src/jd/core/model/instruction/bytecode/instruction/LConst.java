package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

public class LConst extends ConstInstruction 
{
	public LConst(int opcode, int offset, int lineNumber, int value)
	{
		super(opcode, offset, lineNumber, value);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return "J";
	}
}
