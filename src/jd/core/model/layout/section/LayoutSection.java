/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
