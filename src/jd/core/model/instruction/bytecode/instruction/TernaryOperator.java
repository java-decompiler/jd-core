package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

/*
 * La construction de cette instruction ne suit pas les régles générales !
 * C'est la seule exception. Cette instruction, appartement au package 
 * 'bytecode', ne peut etre construite qu'apres avoir aglomérée les instructions
 * 'if'. Cette instruction est affichée par une classe du package 'bytecode' et 
 * est construite par une classe du package 'fast'. 
 */
public class TernaryOperator extends Instruction
{
	public Instruction test;
	public Instruction value1;
	public Instruction value2;
	
	public TernaryOperator(
			int opcode, int offset, int lineNumber, 
			Instruction test, Instruction value1, Instruction value2)
	{
		super(opcode, offset, lineNumber);
		this.test = test;
		this.value1 = value1;
		this.value2 = value2;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		if (this.value1 != null)
			return this.value1.getReturnedSignature(constants, localVariables);
		else
			return this.value2.getReturnedSignature(constants, localVariables);
	}

	public int getPriority()
	{
		return 13;
	}
}
