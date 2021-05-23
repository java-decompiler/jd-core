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
package jd.core.util;


/*
 * internalPath:		basic/data/Test.class
 * internalClassName:	basic/data/Test
 * qualifiedClassName:	basic.data.Test
 * internalPackageName:	basic/data
 * packageName:			basic.data
 */
public class TypeNameUtil 
{
	public static String InternalTypeNameToInternalPackageName(String path)
	{
		int index = path.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);
		return (index == -1) ? "" : path.substring(0, index);
	}
	
	public static String InternalTypeNameToQualifiedTypeName(String path)
	{
		return path.replace(StringConstants.INTERNAL_PACKAGE_SEPARATOR, 
				            StringConstants.PACKAGE_SEPARATOR)
				   .replace(StringConstants.INTERNAL_INNER_SEPARATOR, 
				            StringConstants.INNER_SEPARATOR);
	}	
}
