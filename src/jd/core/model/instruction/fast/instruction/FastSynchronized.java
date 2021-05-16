package jd.core.model.instruction.fast.instruction;

import java.util.List;

import jd.core.model.instruction.bytecode.instruction.Instruction;


/**
 * enter exit monitor
 */
public class FastSynchronized extends FastList
{
	public Instruction monitor;

	public FastSynchronized(
		int opcode, int offset, int lineNumber, 
		int branch, List<Instruction> instructions)
	{
		super(opcode, offset, lineNumber, branch, instructions);
		this.monitor = null;
	}
}
