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
package jd.core.model.layout.block;

import jd.core.model.layout.section.LayoutSection;

/**
 * bloc(i).lastLineNumber = bloc(i).firstLineNumber + bloc(i).minimalLineCount
 * 
 * bloc(i).firstLineNumber + bloc(i).minimalLineCount <= 
 *   bloc(i+1).firstLineNumber <= 
 *     bloc(i).firstLineNumber + bloc(i).maximalLineCount
 */
public class LayoutBlock {

	public byte tag;
	
	public int firstLineNumber;
	public int lastLineNumber;
	
	public int minimalLineCount;
	public int maximalLineCount;
	public int preferedLineCount;

	public int lineCount;

	public int index;
	public LayoutSection section;

	public LayoutBlock(
		byte tag, int firstLineNumber, int lastLineNumber, int lineCount) 
	{
		this.tag = tag;
		this.firstLineNumber = firstLineNumber;
		this.lastLineNumber = lastLineNumber;
		this.minimalLineCount = lineCount;
		this.maximalLineCount = lineCount;
		this.preferedLineCount = lineCount;
		this.lineCount = lineCount;
		this.index = 0;
		this.section = null;
	}

	public LayoutBlock(
		byte tag, int firstLineNumber, int lastLineNumber,
		int minimalLineCount, int maximalLineCount, int preferedLineCount) 
	{
		this.tag = tag;
		this.firstLineNumber = firstLineNumber;
		this.lastLineNumber = lastLineNumber;
		this.minimalLineCount = minimalLineCount;
		this.maximalLineCount = maximalLineCount;
		this.preferedLineCount = preferedLineCount;
		this.lineCount = preferedLineCount;
		this.index = 0;
		this.section = null;
	}
}
