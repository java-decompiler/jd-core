package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

public class FConst extends ConstInstruction 
{
	public FConst(int opcode, int offset, int lineNumber, int value)
	{
		super(opcode, offset, lineNumber, value);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return "F";
	}
}
