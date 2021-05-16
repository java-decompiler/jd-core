package jd.core.model.layout.block;


public class MethodBodyBlockStartLayoutBlock extends BlockLayoutBlock 
{
	public MethodBodyBlockStartLayoutBlock() 
	{
		this(2);
	}
	
	public MethodBodyBlockStartLayoutBlock(int preferedLineCount) 
	{
		super(
			LayoutBlockConstants.METHOD_BODY_BLOCK_START, 0, 
			LayoutBlockConstants.UNLIMITED_LINE_COUNT, preferedLineCount);
	}
	
	public void transformToStartEndBlock(int preferedLineCount)
	{
		this.tag = LayoutBlockConstants.METHOD_BODY_BLOCK_START_END;
		this.preferedLineCount = this.lineCount = preferedLineCount;
	}
	
	public void transformToSingleLineBlock()
	{
		this.tag = LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START;
	}
}
