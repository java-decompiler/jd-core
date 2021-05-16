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
package jd.core.model.classfile;

import jd.core.model.classfile.attribute.Annotation;
import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeConstants;
import jd.core.model.classfile.attribute.AttributeRuntimeAnnotations;
import jd.core.model.classfile.attribute.AttributeSignature;


public class Base 
{
	public int access_flags;
	final public Attribute[] attributes;

	public Base(int access_flags, Attribute[] attributes)
	{
		this.access_flags = access_flags;
		this.attributes = attributes;
	}
	
	public AttributeSignature getAttributeSignature()
	{
		if (this.attributes != null)
		{
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == AttributeConstants.ATTR_SIGNATURE)
					return (AttributeSignature)this.attributes[i];
		}
		
		return null;
	}
	
	public boolean containsAttributeDeprecated()
	{
		if (this.attributes != null)
		{
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == AttributeConstants.ATTR_DEPRECATED)
					return true;
		}
		
		return false;
	}
	
	public boolean containsAnnotationDeprecated(ClassFile classFile)
	{
		if (this.attributes != null)
		{
			for (int i=this.attributes.length-1; i>=0; --i)
			{
				switch (this.attributes[i].tag)
				{
				case AttributeConstants.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS:
				case AttributeConstants.ATTR_RUNTIME_VISIBLE_ANNOTATIONS:
					{
						Annotation[]annotations = 
							((AttributeRuntimeAnnotations)attributes[i]).annotations;
						if (containsAnnotationDeprecated(classFile, annotations))
							return true;
					}
					break;
				}
			}
		}
		
		return false;
	}
	
	private boolean containsAnnotationDeprecated(
			ClassFile classFile, Annotation[] annotations)
	{
		if (annotations != null)
		{
			int idsIndex = 
				classFile.getConstantPool().internalDeprecatedSignatureIndex;
			
			for (int i=annotations.length-1; i>=0; --i)
				if (idsIndex == annotations[i].type_index)
					return true;
		}
		
		return false;
	}
}
