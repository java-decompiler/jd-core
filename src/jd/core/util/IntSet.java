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
package jd.core.util;

/*
 * Set of offset : contains sorted list of no duplicated elements.
 */
public class IntSet 
{
	private int values[];
	private int capacity;
	private int size;
	private int min;
	private int max;
	
	public IntSet()
	{
		this.values = null;
		this.capacity = 0;
		this.size = 0;
	}
	
	public int size()
	{
		return this.size;
	}
	
	public void add(int newValue)
	{
		if (this.capacity == 0)
		{
			// Init
			this.capacity = 5;
			this.values = new int[this.capacity];
			this.size = 1;
			this.min = this.max = this.values[0] = newValue;			
		}
		else 
		{
			if (this.capacity == this.size)
			{
				// Ensure capacity
				this.capacity *= 2;
				int[] tmp = new int[this.capacity];
				System.arraycopy(this.values, 0, tmp, 0, this.size);
				this.values = tmp;
			}
			
			if (this.max < newValue)
			{
				// Add last
				this.values[this.size++] = newValue;
				this.max = newValue;
			}
			else if (newValue < this.min)
			{
				// Add first
				System.arraycopy(this.values, 0, this.values, 1, this.size);
				this.min = this.values[0] = newValue;
				this.size++;
			}
			else
			{
				// Insert value
				int firstIndex = 0;
				int lastIndex = this.size-1;
				int medIndex;
				int value;
				
				while (firstIndex < lastIndex)
				{
					medIndex = (lastIndex + firstIndex) / 2;
					value = this.values[medIndex];
					
					if (value < newValue)
						firstIndex = medIndex+1;
					else if (value > newValue)
						lastIndex = medIndex-1;
					else
						break;
				}
				
				medIndex = (lastIndex + firstIndex) / 2;
				value = this.values[medIndex];
				
				if (value < newValue)
				{
					medIndex++;
					System.arraycopy(this.values, medIndex, 
					         this.values, medIndex+1, this.size-medIndex);
					this.values[medIndex] = newValue;
					this.size++;
				}
				else if (value > newValue)
				{
					System.arraycopy(this.values, medIndex, 
					         this.values, medIndex+1, this.size-medIndex);					
					this.values[medIndex] = newValue;
					this.size++;
				}				
			}
		}
	}
	
	public int[] toArray()
	{
		if (this.values == null)
			return null;
		
		int[] tmp = new int[this.size];
		System.arraycopy(this.values, 0, tmp, 0, this.size);
		return tmp;
	}
	
	public int get(int index)
	{
		if ((this.values == null) || (index >= this.size))
			throw new IndexOutOfBoundsException(
				"Index: "+index+", Size: "+size);
		
		return this.values[index];
	}
}
