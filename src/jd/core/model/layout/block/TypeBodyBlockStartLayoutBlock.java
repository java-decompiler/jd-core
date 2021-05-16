package jd.core.model.layout.block;


public class TypeBodyBlockStartLayoutBlock extends BlockLayoutBlock 
{
	public TypeBodyBlockStartLayoutBlock() 
	{
		this(2);
	}
	
	public TypeBodyBlockStartLayoutBlock(int preferedLineCount) 
	{
		super(
			LayoutBlockConstants.TYPE_BODY_BLOCK_START, 
			0, 2, preferedLineCount);
	}
	
	public void transformToStartEndBlock(int preferedLineCount)
	{
		this.tag = LayoutBlockConstants.TYPE_BODY_BLOCK_START_END;
		this.preferedLineCount = this.lineCount = preferedLineCount;
	}
}
