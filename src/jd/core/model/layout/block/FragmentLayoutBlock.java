package jd.core.model.layout.block;

import jd.core.model.instruction.bytecode.instruction.Instruction;

public class FragmentLayoutBlock extends LayoutBlock 
{
	public FragmentLayoutBlock(byte tag) 
	{
		super(
			tag, 
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 0, 0);
	}
}
