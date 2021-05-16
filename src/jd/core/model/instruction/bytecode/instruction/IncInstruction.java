package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

public class IncInstruction extends Instruction 
{
	public Instruction value;
	public int count;
	
	public IncInstruction(
		int opcode, int offset, int lineNumber, Instruction value, int count)
	{
		super(opcode, offset, lineNumber);
		this.value = value;
		this.count = count;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		return (this.value == null) ? null : 
			this.value.getReturnedSignature(constants, localVariables);
	}

	public int getPriority()
	{
		if ((this.count == 1) || (this.count == -1))
			// Operator '++' or '--'
			return 2;
		// Operator '+=' or '-='				
		return 14;
	}
}
