package jd.core.model.layout.block;


public class StatementsBlockStartLayoutBlock extends BlockLayoutBlock 
{
	public StatementsBlockStartLayoutBlock() 
	{
		this(2);
	}
	
	public StatementsBlockStartLayoutBlock(int preferedLineCount) 
	{
		super(
			LayoutBlockConstants.STATEMENTS_BLOCK_START, 0, 
			LayoutBlockConstants.UNLIMITED_LINE_COUNT, preferedLineCount);
	}
	
	public void transformToStartEndBlock(int preferedLineCount)
	{
		this.tag = LayoutBlockConstants.STATEMENTS_BLOCK_START_END;
		this.preferedLineCount = this.lineCount = preferedLineCount;
	}
}
