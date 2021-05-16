package jd.core.model.reference;

import java.util.Collection;
import java.util.HashMap;




public class ReferenceMap 
{
	private HashMap<String, Reference> references;
	
	public ReferenceMap()
	{
		this.references = new HashMap<String, Reference>();
	}
	
	public void add(String internalName)
	{
		if (internalName.indexOf(';') != -1)
		{
			System.err.println(
				"ReferenceMap.add: InvalidParameterException(" + 
				internalName + ")");
			// throw new InvalidParameterException(internalName);
		}
		else
		{
			Reference ref = this.references.get(internalName);
			
			if (ref == null)
				this.references.put(internalName, new Reference(internalName));
			else
				ref.incCounter();
		}
	}
	
	public Reference remove(String internalName) 
	{
		return this.references.remove(internalName);
	}

	public Collection<Reference> values()
	{
		return this.references.values();
	}
	
//	public String toString()
//	{
//		return this.references.values().toString();
//	}
	
	public boolean contains(String internalName)
	{
		return this.references.containsKey(internalName);
	}
}
