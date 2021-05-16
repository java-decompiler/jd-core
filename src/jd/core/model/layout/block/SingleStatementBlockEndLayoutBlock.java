package jd.core.model.layout.block;


public class SingleStatementBlockEndLayoutBlock extends BlockLayoutBlock 
{
	public SingleStatementBlockEndLayoutBlock(int preferedLineCount) 
	{
		super(
			LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_END, 0, 
			LayoutBlockConstants.UNLIMITED_LINE_COUNT, preferedLineCount);
	}
}
