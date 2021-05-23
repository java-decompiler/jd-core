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

public class IndexToIndexMap 
{
    private static final int INITIAL_CAPACITY = 128;
    
	private MapEntry[] entries;
	
	public IndexToIndexMap()
	{
		this.entries = new MapEntry[INITIAL_CAPACITY];
	}
	
	public void put(int key, int value)
	{
		int index = hashCodeToIndex(key, this.entries.length);
		MapEntry entry = this.entries[index];
		
		while (entry != null) 
		{
            if (entry.key == key)
            {
                entry.value = value;
                return;
            }
            entry = entry.next;
        }
		
		this.entries[index] = new MapEntry(key, value, this.entries[index]);
	}
	
	public int get(int key)
	{
		int index = hashCodeToIndex(key, this.entries.length);
		MapEntry entry = this.entries[index];
		
		 while (entry != null) 
		 {
			 if (entry.key == key)
				 return entry.value;
            entry = entry.next;
		 }
		 
		 return -1;
	}
	
    private int hashCodeToIndex(int hashCode, int size) 
    {
        return hashCode & (size - 1);
    }
    
    private static class MapEntry
    {
    	public int key;
    	public int value;
    	public MapEntry next;
    	
    	public MapEntry(int key, int value, MapEntry next)
    	{
    		this.key = key;
    		this.value = value;
    		this.next = next;
    	}
    }
}
