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
