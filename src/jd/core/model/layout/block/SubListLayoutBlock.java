package jd.core.model.layout.block;

import java.util.List;

public class SubListLayoutBlock extends LayoutBlock 
{
	public List<LayoutBlock> subList;
	
	public SubListLayoutBlock(
		byte tag, List<LayoutBlock> subList, 
		int firstLineNumber, int lastLineNumber, int preferedLineCount) 
	{
		super(
			tag, firstLineNumber, lastLineNumber, 
			0, preferedLineCount, preferedLineCount);
		this.subList = subList;
	}
}
