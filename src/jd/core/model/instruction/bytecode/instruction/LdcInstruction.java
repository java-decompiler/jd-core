package jd.core.model.instruction.bytecode.instruction;


public abstract class LdcInstruction extends IndexInstruction 
{
	public LdcInstruction(int opcode, int offset, int lineNumber, int index)
	{
		super(opcode, offset, lineNumber, index);
	}
}
