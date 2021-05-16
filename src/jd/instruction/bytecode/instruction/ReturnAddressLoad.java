package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;

/*
 * Pseudo instruction placée en debut de sous procedure. Lors de l'execution, 
 * cette adresse est normalement placée sur la pile par l'instruction JSR.
 */
public class ReturnAddressLoad extends Instruction 
{
	public ReturnAddressLoad(int opcode, int offset, int lineNumber)
	{
		super(opcode, offset, lineNumber);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return null;
	}
}
