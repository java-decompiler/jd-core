package jd.core.model.instruction.fast.instruction;

import java.util.List;

import jd.core.model.instruction.bytecode.instruction.Instruction;


/**
 * if-else
 */
public class FastTest2Lists extends FastTestList
{
	public List<Instruction> instructions2;

	public FastTest2Lists(
			int opcode, int offset, int lineNumber, int branch, 
			Instruction test, List<Instruction> instructions1, 
			List<Instruction> instructions2)
	{
		super(opcode, offset, lineNumber, branch, test, instructions1);
		this.instructions2 = instructions2;
	}
}
