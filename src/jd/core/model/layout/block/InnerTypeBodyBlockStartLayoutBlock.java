package jd.core.model.layout.block;


public class InnerTypeBodyBlockStartLayoutBlock extends BlockLayoutBlock 
{
	public InnerTypeBodyBlockStartLayoutBlock() 
	{
		super(
			LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START, 0, 
			LayoutBlockConstants.UNLIMITED_LINE_COUNT, 2);
	}
	
	public void transformToStartEndBlock()
	{
		this.tag = LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START_END;
		this.preferedLineCount = this.lineCount = 0;
	}
}
