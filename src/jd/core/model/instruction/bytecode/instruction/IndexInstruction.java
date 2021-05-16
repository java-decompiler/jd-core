package jd.core.model.instruction.bytecode.instruction;



public abstract class IndexInstruction extends Instruction 
{
	public int index;

	public IndexInstruction(int opcode, int offset, int lineNumber, int index)
	{
		super(opcode, offset, lineNumber);
		this.index = index;
	}
}
