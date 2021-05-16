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


public class InnerClass 
{
	public final int inner_class_index;
	public final int outer_class_index;
	public final int inner_name_index;
	public final int inner_access_flags;
	  
	public InnerClass(int inner_class_index, int outer_class_index, 
			            int inner_name_index, int inner_access_flags) 
	{
		this.inner_class_index = inner_class_index;
		this.outer_class_index = outer_class_index;
		this.inner_name_index = inner_name_index;
		this.inner_access_flags = inner_access_flags;
	}
}
