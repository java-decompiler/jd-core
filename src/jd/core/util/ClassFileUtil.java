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

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;


/*
 * internalPath:		basic/data/Test.class
 * internalClassName:	basic/data/Test
 * qualifiedClassName:	basic.data.Test
 * internalPackageName:	basic/data
 * packageName:			basic.data
 */
public class ClassFileUtil 
{
	public static boolean ContainsMultipleConstructor(ClassFile classFile)
	{
		ConstantPool constants = classFile.getConstantPool();
		Method[] methods = classFile.getMethods();
		boolean flag = false;
		
		for (int i=0; i<methods.length; i++)
		{
	    	Method method = methods[i];
	    	
	    	if ((method.access_flags & 
	    		 (ClassFileConstants.ACC_SYNTHETIC|ClassFileConstants.ACC_BRIDGE)) != 0)
	    		continue;

    		if (method.name_index == constants.instanceConstructorIndex)
	    	{
    			if (flag)
    				// A other constructor has been found
    				return true;
    			// A first constructor has been found
    			flag = true;
	    	}
		}
		
		return false;
	}
	
	public static boolean IsAMethodOfEnum(
		ClassFile classFile, Method method, String signature)
	{
		ConstantPool constants = classFile.getConstantPool();

		if ((method.access_flags & (ClassFileConstants.ACC_PUBLIC|ClassFileConstants.ACC_STATIC)) == 
			(ClassFileConstants.ACC_PUBLIC|ClassFileConstants.ACC_STATIC))
		{
			String methodName = constants.getConstantUtf8(method.name_index);

			if (methodName.equals(StringConstants.ENUM_VALUEOF_METHOD_NAME))
			{
				String s = "(Ljava/lang/String;)" + classFile.getInternalClassName();	    				
				if (s.equals(signature)) 
				{
	    			// Ne pas afficher la methode 
					// "public static enumXXX valueOf(String paramString)".
					return true;
				}
			}
	
			if (methodName.equals(StringConstants.ENUM_VALUES_METHOD_NAME))
			{
				String s = "()[" + classFile.getInternalClassName();	    				
				if (s.equals(signature)) 
				{
	    			// Ne pas afficher la methode 
					// "public static enumXXX[] values()".
					return true;
				}
			}
		}
		
		return false;
	}
}
