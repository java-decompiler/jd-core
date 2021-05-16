
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
