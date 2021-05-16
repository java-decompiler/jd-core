package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;


public class ArrayLength extends ArrayInstruction 
{
	public ArrayLength(
		int opcode, int offset, int lineNumber, Instruction arrayref)
	{
		super(opcode, offset, lineNumber, arrayref);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		return "I";
	}
}
