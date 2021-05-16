package jd.core.model.layout.section;


public class LayoutSection implements Comparable<LayoutSection>
{
	public int index;
	public int firstBlockIndex;
	public int lastBlockIndex;
	
	public final int originalLineCount;
	
	public boolean relayout;
	public int score;
	public boolean containsError;
	//
	public int debugFirstLineNumber;
	
	public LayoutSection(
		int index,
		int firstBlockIndex, int lastBlockIndex, 
		int firstLineNumber, int lastLineNumber,
		boolean containsError) 
	{
		this.index = index;
		this.firstBlockIndex = firstBlockIndex;
		this.lastBlockIndex = lastBlockIndex;
		this.originalLineCount = lastLineNumber - firstLineNumber;
		this.relayout = true;
		this.score = 0;
		this.containsError = containsError;
		//
		this.debugFirstLineNumber = firstLineNumber;
	}

	public int compareTo(LayoutSection o) 
	{
		return ((LayoutSection)o).score - this.score;
	}
}
