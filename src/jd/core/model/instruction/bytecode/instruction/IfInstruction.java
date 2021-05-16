package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.instruction.bytecode.ByteCodeConstants;


public class IfInstruction extends ConditionalBranchInstruction 
{
	public Instruction value;
	
	public IfInstruction(
		int opcode, int offset, int lineNumber, 
		int cmp, Instruction value, int branch)
	{
		super(opcode, offset, lineNumber, cmp, branch);
		this.value = value;
	}

	public int getPriority()
	{
		switch (this.cmp)
		{
		case ByteCodeConstants.CMP_EQ:
		case ByteCodeConstants.CMP_NE:
			return 7;
		default:
			return 6;
		}
	}
}
