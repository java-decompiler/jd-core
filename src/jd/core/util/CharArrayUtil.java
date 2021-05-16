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
