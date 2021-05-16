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


public class ElementValueEnumConstValue extends ElementValue
{
	final public int type_name_index;
	final public int const_name_index;
	
	public ElementValueEnumConstValue(byte tag, 
			                          int type_name_index, 
			                          int const_name_index) 
	{
		super(tag);
		this.type_name_index = type_name_index;
		this.const_name_index = const_name_index;
	}
}
