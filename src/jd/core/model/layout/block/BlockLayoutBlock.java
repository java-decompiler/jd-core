package jd.core.model.layout.block;

import jd.core.model.instruction.bytecode.instruction.Instruction;


public class BlockLayoutBlock extends LayoutBlock 
{
	public BlockLayoutBlock other;
	
	public BlockLayoutBlock(byte tag, int lineCount) 
	{
		super(
			tag, Instruction.UNKNOWN_LINE_NUMBER, 
			Instruction.UNKNOWN_LINE_NUMBER, lineCount);
		this.other = null;
	}

	public BlockLayoutBlock(
		byte tag, int minimalLineCount, 
		int maximalLineCount, int preferedLineCount) 
	{
		super(
			tag, 
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER,
			minimalLineCount, maximalLineCount, preferedLineCount);
		this.other = null;
	}
}
