package jd.core.model.layout.block;

import jd.core.model.instruction.bytecode.instruction.Instruction;


public class CaseBlockEndLayoutBlock extends LayoutBlock 
{
	public CaseBlockEndLayoutBlock() 
	{
		super(
			LayoutBlockConstants.CASE_BLOCK_END, 
			Instruction.UNKNOWN_LINE_NUMBER,
			Instruction.UNKNOWN_LINE_NUMBER,
			0, LayoutBlockConstants.UNLIMITED_LINE_COUNT, 1);
	}
}
