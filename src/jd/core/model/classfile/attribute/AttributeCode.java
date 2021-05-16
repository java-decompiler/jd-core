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
package jd.core.model.classfile.attribute;



public class AttributeCode extends Attribute
{
	//private int max_stack;
	//private int max_locals;
	public final byte[] code;	
	public final CodeException[] exception_table;
	public final Attribute[] attributes;
	
	public AttributeCode(byte tag, 
			             int attribute_name_index, int max_stack, 
			             int max_locals, byte[] code, 
			             CodeException[] exception_table, 
			             Attribute[] attributes) 
	{
		super(tag, attribute_name_index);
		//this.max_stack = max_stack;
		//this.max_locals = max_locals;
		this.code = code;
		this.exception_table = exception_table;
		this.attributes = attributes;
	}
	
	public AttributeNumberTable getAttributeLineNumberTable()
	{
		if (this.attributes != null)
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == AttributeConstants.ATTR_NUMBER_TABLE) 
					return (AttributeNumberTable)this.attributes[i];
		
		return null;
	}
	
	public AttributeLocalVariableTable getAttributeLocalVariableTable()
	{
		if (this.attributes != null)
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == AttributeConstants.ATTR_LOCAL_VARIABLE_TABLE) 
					return (AttributeLocalVariableTable)this.attributes[i];
		
		return null;
	}
	
	public AttributeLocalVariableTable getAttributeLocalVariableTypeTable()
	{
		if (this.attributes != null)
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == AttributeConstants.ATTR_LOCAL_VARIABLE_TYPE_TABLE) 
					return (AttributeLocalVariableTable)this.attributes[i];
		
		return null;
	}
}
