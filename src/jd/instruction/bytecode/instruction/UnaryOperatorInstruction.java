package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;



public class UnaryOperatorInstruction extends Instruction 
{
	private int priority;
	public String signature;
	public String operator;
	public Instruction value;

	public UnaryOperatorInstruction(
			int opcode, int offset, int lineNumber, int priority, 
			String signature, String operator, Instruction value)
	{
		super(opcode, offset, lineNumber);
		this.priority = priority;
		this.signature = signature;
		this.operator = operator;
		this.value = value;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		return this.signature;
	}

	public int getPriority()
	{
		return this.priority;
	}
}
