package jd.instruction.bytecode.instruction;


public class BIPush extends IConst 
{
	public BIPush(int opcode, int offset, int lineNumber, int value)
	{
		super(opcode, offset, lineNumber, value);
	}
}
