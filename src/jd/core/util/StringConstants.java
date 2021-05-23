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


public class StringConstants 
{
	public static final String CLASS_CONSTRUCTOR = "<clinit>";
	public static final String INSTANCE_CONSTRUCTOR = "<init>";
	
	public static final String INTERNAL_CLASS_CLASS_NAME = "java/lang/Class";
	public static final String INTERNAL_OBJECT_CLASS_NAME = "java/lang/Object";
	public static final String INTERNAL_STRING_CLASS_NAME = "java/lang/String";
	public static final String INTERNAL_STRINGBUFFER_CLASS_NAME = "java/lang/StringBuffer";
	public static final String INTERNAL_STRINGBUILDER_CLASS_NAME = "java/lang/StringBuilder";
	public static final String INTERNAL_THROWABLE_CLASS_NAME = "java/lang/Throwable";
	public static final String INTERNAL_JAVA_LANG_PACKAGE_NAME = "java/lang";
	public static final char   INTERNAL_PACKAGE_SEPARATOR = '/';
	public static final char   INTERNAL_INNER_SEPARATOR = '$';
	public static final char   INTERNAL_BEGIN_TEMPLATE = '<';
	public static final char   INTERNAL_END_TEMPLATE = '>';
	
	public static final char   PACKAGE_SEPARATOR = '.';
	public static final char   INNER_SEPARATOR = '.';
	public static final String CLASS_FILE_SUFFIX = ".class";

	public static final String INTERNAL_CLASS_SIGNATURE = "Ljava/lang/Class;";
	public static final String INTERNAL_OBJECT_SIGNATURE = "Ljava/lang/Object;";
	public static final String INTERNAL_STRING_SIGNATURE = "Ljava/lang/String;";
	public static final String INTERNAL_DEPRECATED_SIGNATURE = "Ljava/lang/Deprecated;";
	public static final String INTERNAL_CLASSNOTFOUNDEXCEPTION_SIGNATURE = 
		"Ljava/lang/ClassNotFoundException;";
			
	public static final String THIS_LOCAL_VARIABLE_NAME       = "this";
	public static final String OUTER_THIS_LOCAL_VARIABLE_NAME = "this$1";
	public static final String TMP_LOCAL_VARIABLE_NAME        = "tmp";
	
	public static final String INDENT = "  ";
	
	public static final String ENUM_VALUES_ARRAY_NAME	= "$VALUES";
	public static final String ENUM_VALUES_ARRAY_NAME_ECLIPSE = "ENUM$VALUES";
	public static final String ENUM_VALUES_METHOD_NAME	= "values";
	public static final String ENUM_VALUEOF_METHOD_NAME	= "valueOf";
	public static final String TOSTRING_METHOD_NAME		= "toString";
	public static final String VALUEOF_METHOD_NAME		= "valueOf";
	public static final String APPEND_METHOD_NAME		= "append";
	public static final String FORNAME_METHOD_NAME		= "forName";
	public static final String ORDINAL_METHOD_NAME		= "ordinal";	

	public static final String ANNOTATIONDEFAULT_ATTRIBUTE_NAME = "AnnotationDefault";
	public static final String CODE_ATTRIBUTE_NAME = "Code";
	public static final String CONSTANTVALUE_ATTRIBUTE_NAME = "ConstantValue";
	public static final String DEPRECATED_ATTRIBUTE_NAME = "Deprecated";
	public static final String ENCLOSINGMETHOD_ATTRIBUTE_NAME = "EnclosingMethod";
	public static final String EXCEPTIONS_ATTRIBUTE_NAME = "Exceptions";
	public static final String INNERCLASSES_ATTRIBUTE_NAME = "InnerClasses";
	public static final String LINENUMBERTABLE_ATTRIBUTE_NAME = "LineNumberTable";
	public static final String LOCALVARIABLETABLE_ATTRIBUTE_NAME = "LocalVariableTable";
	public static final String LOCALVARIABLETYPETABLE_ATTRIBUTE_NAME = "LocalVariableTypeTable";
	public static final String RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_NAME = "RuntimeInvisibleAnnotations";
	public static final String RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME = "RuntimeVisibleAnnotations";
	public static final String RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_NAME = "RuntimeInvisibleParameterAnnotations";
	public static final String RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_NAME = "RuntimeVisibleParameterAnnotations";
	public static final String SIGNATURE_ATTRIBUTE_NAME = "Signature";
	public static final String SOURCEFILE_ATTRIBUTE_NAME = "SourceFile";
	public static final String SYNTHETIC_ATTRIBUTE_NAME = "Synthetic";
	
	public static final String CLASS_DOLLAR = "class$"; 
	public static final String ARRAY_DOLLAR = "array$"; 
	public static final String JD_METHOD_PREFIX = "jdMethod_"; 
	public static final String JD_FIELD_PREFIX = "jdField_"; 
}
