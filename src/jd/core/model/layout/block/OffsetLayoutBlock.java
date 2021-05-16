package jd.core.model.layout.block;


public class OffsetLayoutBlock extends LayoutBlock 
{
	public int offset;
	
	public OffsetLayoutBlock(
		byte tag, int firstLineNumber, int lastLineNumber, 
		int minimalLineCount, int maximalLineCount, int preferedLineCount,
		int offset) 
	{
		super(
			tag, firstLineNumber, lastLineNumber, 
			minimalLineCount, maximalLineCount, preferedLineCount);
		this.offset = offset;
	}
}
