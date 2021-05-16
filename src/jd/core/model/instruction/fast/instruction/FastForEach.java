package jd.core.model.instruction.fast.instruction;

import java.util.List;

import jd.core.model.instruction.bytecode.instruction.Instruction;

public class FastForEach extends FastList
{
	public Instruction variable;
	public Instruction values;
	
	public FastForEach(
		int opcode, int offset, int lineNumber, int branch, 
		Instruction declaration, Instruction values,
		List<Instruction> instructions)
	{
		super(opcode, offset, lineNumber, branch, instructions);
		this.variable = declaration;
		this.values = values;
	}
}
