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
package jd.core.model.classfile.constant;

public class ConstantConstant 
{
	public static final byte CONSTANT_Unknown            = 0;
	public static final byte CONSTANT_Utf8               = 1;
	public static final byte CONSTANT_Integer            = 3;
	public static final byte CONSTANT_Float              = 4;
	public static final byte CONSTANT_Long               = 5;
	public static final byte CONSTANT_Double             = 6;
	public static final byte CONSTANT_Class              = 7;
	public static final byte CONSTANT_String             = 8;
	public static final byte CONSTANT_Fieldref           = 9;
	public static final byte CONSTANT_Methodref          = 10;
	public static final byte CONSTANT_InterfaceMethodref = 11;
	public static final byte CONSTANT_NameAndType        = 12;
}
