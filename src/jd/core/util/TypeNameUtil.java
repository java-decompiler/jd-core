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
