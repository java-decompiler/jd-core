package jd.core.model.layout.block;


public class StatementsBlockEndLayoutBlock extends BlockLayoutBlock 
{
	public StatementsBlockEndLayoutBlock(int preferedLineCount) 
	{
		super(
			LayoutBlockConstants.STATEMENTS_BLOCK_END, 0, 
			LayoutBlockConstants.UNLIMITED_LINE_COUNT, preferedLineCount);
	}
}
