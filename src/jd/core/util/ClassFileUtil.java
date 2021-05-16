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
