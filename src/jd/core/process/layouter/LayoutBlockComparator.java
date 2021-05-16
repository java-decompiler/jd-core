package jd.core.process.layouter;

import java.util.Comparator;

import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.layout.block.LayoutBlock;

/*
 * Tri par ordre croissant, les blocs sans numero de ligne sont places a la fin.
 */
public class LayoutBlockComparator implements Comparator<LayoutBlock>
{
	public LayoutBlockComparator() {}
	
	public int compare(LayoutBlock lb1, LayoutBlock lb2) {
		if (lb1.lastLineNumber == Instruction.UNKNOWN_LINE_NUMBER)
		{
			if (lb2.lastLineNumber == Instruction.UNKNOWN_LINE_NUMBER)
			{
				// Tri par l'index pour eviter les ecarts de tri entre la 
				// version Java et C++ 
				return lb1.index - lb2.index;
			}
			else
			{
				// lb1 > lb2
				return 1;
			}
		}
		else
		{
			if (lb2.lastLineNumber == Instruction.UNKNOWN_LINE_NUMBER)
			{
				// lb1 < lb2
				return -1;
			}
			else
			{
				return lb1.lastLineNumber - lb2.lastLineNumber;
			}
		}
	}
}
