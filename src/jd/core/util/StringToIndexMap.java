package jd.core.util;

public class StringToIndexMap 
{
    private static final int INITIAL_CAPACITY = 128*2;
    
	private HashEntry[] entries;
	
	public StringToIndexMap()
	{
		this.entries = new HashEntry[INITIAL_CAPACITY];
	}
	
	public void put(String key, int value)
	{
		int hashCode = key.hashCode();
		int index = hashCodeToIndex(hashCode, this.entries.length);
		HashEntry entry = this.entries[index];
		
		while (entry != null) 
		{
            if ((entry.hashCode == hashCode) && key.equals(entry.key)) 
            {
                entry.value = value;
                return;
            }
            entry = entry.next;
        }
		
		this.entries[index] = 
			new HashEntry(key, hashCode, value, this.entries[index]);
	}
	
	public int get(String key)
	{
		int hashCode = key.hashCode();
		int index = hashCodeToIndex(hashCode, this.entries.length);
		HashEntry entry = this.entries[index];
		
		 while (entry != null) 
		 {
			 if ((entry.hashCode == hashCode) && key.equals(entry.key))
				 return entry.value;
            entry = entry.next;
		 }
		 
		 return -1;
	}
	
    private int hashCodeToIndex(int hashCode, int size) 
    {
        return hashCode & (size - 1);
    }
    
    private static class HashEntry
    {
    	public String key;
    	public int hashCode;
    	public int value;
    	public HashEntry next;
    	
    	public HashEntry(String key, int hashCode, int value, HashEntry next)
    	{
    		this.key = key;
    		this.hashCode = hashCode;
    		this.value = value;
    		this.next = next;
    	}
    }
}
