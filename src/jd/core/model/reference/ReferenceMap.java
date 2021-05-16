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
