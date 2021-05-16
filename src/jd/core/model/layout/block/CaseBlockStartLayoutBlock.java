package jd.core.model.layout.block;

import jd.core.model.instruction.bytecode.instruction.Instruction;


public class CaseBlockStartLayoutBlock extends LayoutBlock 
{
	public CaseBlockStartLayoutBlock() 
	{
		super(
			LayoutBlockConstants.CASE_BLOCK_START, 
			Instruction.UNKNOWN_LINE_NUMBER,
			Instruction.UNKNOWN_LINE_NUMBER,
			0, 1, 1);
	}
}
