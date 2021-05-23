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

public class CharArrayUtil 
{
	public static String Substring(char[] ca, int beginIndex, int endIndex) 
    {
    	return new String(ca, beginIndex, endIndex - beginIndex);
    }
    
	public static int IndexOf(char[] ca, char ch, int fromIndex) 
    {
    	int length = ca.length;

    	while (fromIndex < length)
    	{
    		if (ca[fromIndex] == ch)
    			return fromIndex;
    		fromIndex++;
    	}

    	return -1;
    }
}
