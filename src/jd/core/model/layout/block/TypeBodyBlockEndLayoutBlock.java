package jd.core.model.layout.block;


public class TypeBodyBlockEndLayoutBlock extends BlockLayoutBlock 
{
	public TypeBodyBlockEndLayoutBlock() 
	{
		this(1);
	}
	
	public TypeBodyBlockEndLayoutBlock(int preferedLineCount) 
	{
		super(
			LayoutBlockConstants.TYPE_BODY_BLOCK_END, 
			0, 1, preferedLineCount);
	}
}
