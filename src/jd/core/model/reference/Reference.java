package jd.core.model.reference;


public class Reference
{
	private String internalName;
	private int counter;
	
	Reference(String internalName)
	{
		this.internalName = internalName;
		this.counter = 1;
	}
	
	public String getInternalName()
	{
		return this.internalName;
	}
	
	public int getCounter()
	{
		return this.counter;
	}
	
	public void incCounter()
	{
		this.counter++;
	}
	
//	public String toString()
//	{
//		return this.internalName + '#' + this.counter;
//	}
}
