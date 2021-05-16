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

import jd.core.model.classfile.attribute.Attribute;


public class FieldOrMethod extends Base
{
	public int name_index;
	final public int descriptor_index;

	public FieldOrMethod(int access_flags, int name_index, 
			             int descriptor_index, Attribute[] attributes)
	{
		super(access_flags, attributes);
		
		this.name_index = name_index;
		this.descriptor_index = descriptor_index;
	}

	public Attribute[] getAttributes()
	{
		return this.attributes;
	}
}
