package jd.instruction.fast.instruction;

import java.util.List;

import jd.instruction.bytecode.instruction.Instruction;


/**
 * while, do-while & if
 */
public class FastTestList extends FastList
{
	public Instruction test;
	
	public FastTestList(
		int opcode, int offset, int lineNumber, int branch, Instruction test, 
		List<Instruction> instructions)
	{
		super(opcode, offset, lineNumber, branch, instructions);
		this.test = test;
	}
}
