package jd;


public class Constants 
{
	public static final int MAGIC_NUMBER                 = 0xCafeBabe;

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

	// Access flag for Class, Field, Method, Nested class
	public final static short ACC_PUBLIC       = 0x0001; // C F M N
	public final static short ACC_PRIVATE      = 0x0002; //   F M N
	public final static short ACC_PROTECTED    = 0x0004; //   F M N
	public final static short ACC_STATIC       = 0x0008; //   F M N
	public final static short ACC_FINAL        = 0x0010; // C F M N
	public final static short ACC_SYNCHRONIZED = 0x0020; //     M
	public final static short ACC_SUPER        = 0x0020; // C
	public final static short ACC_VOLATILE     = 0x0040; //   F
	public final static short ACC_BRIDGE       = 0x0040; //     M
	public final static short ACC_TRANSIENT    = 0x0080; //   F
	public final static short ACC_VARARGS      = 0x0080; //     M
	public final static short ACC_NATIVE       = 0x0100; //     M
	public final static short ACC_INTERFACE    = 0x0200; // C     N
	public final static short ACC_ABSTRACT     = 0x0400; // C   M N
	public final static short ACC_STRICT       = 0x0800; //     M
	public final static short ACC_SYNTHETIC    = 0x1000; // C F M N
	public final static short ACC_ANNOTATION   = 0x2000; // C     N
	public final static short ACC_ENUM         = 0x4000; // C F   N

	public final static String[] ACCESS_FIELD_NAMES = {
		"public", "private", "protected", "static", "final", null, "volatile", 
		"transient"
	};

	public final static String[] ACCESS_METHOD_NAMES = {
		"public", "private", "protected", "static", "final", "synchronized", 
		null, null, "native", null, "abstract", "strictfp"
	};

	public final static String[] ACCESS_NESTED_CLASS_NAMES = {
		"public", "private", "protected", "static", "final"
	};

	public final static String[] ACCESS_NESTED_ENUM_NAMES = {
		"public", "private", "protected", "static"
	};

	public static final byte ATTR_UNKNOWN= 0;
	public static final byte ATTR_SOURCE_FILE= 1;
	public static final byte ATTR_CONSTANT_VALUE = 2;
	public static final byte ATTR_CODE = 3;
	public static final byte ATTR_EXCEPTIONS = 4;
	public static final byte ATTR_LINE_NUMBER_TABLE= 5;
	public static final byte ATTR_LOCAL_VARIABLE_TABLE = 6;
	public static final byte ATTR_INNER_CLASSES= 7;
	public static final byte ATTR_SYNTHETIC= 8;
	public static final byte ATTR_DEPRECATED = 9;
	public static final byte ATTR_PMG= 10;
	public static final byte ATTR_SIGNATURE= 11;
	public static final byte ATTR_STACK_MAP= 12;
	public static final byte ATTR_ENCLOSING_METHOD= 13;
	public static final byte ATTR_NUMBER_TABLE= 14;
	public static final byte ATTR_RUNTIME_VISIBLE_ANNOTATIONS= 15;
	public static final byte ATTR_RUNTIME_INVISIBLE_ANNOTATIONS= 16;
	public static final byte ATTR_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS= 17;
	public static final byte ATTR_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS= 18;
	public static final byte ATTR_DEFAULT= 19;

	
	public static final byte EV_UNKNOWN= 0;
	public static final byte EV_PRIMITIVE_TYPE= 1;
	public static final byte EV_ENUM_CONST_VALUE= 2;	
	public static final byte EV_CLASS_INFO= 3;	
	public static final byte EV_ANNOTATION_VALUE= 4;	
	public static final byte EV_ARRAY_VALUE= 5;		

	
	public static final byte ACCESSOR_GETSTATIC= 1;
	public static final byte ACCESSOR_PUTSTATIC= 2;
	public static final byte ACCESSOR_GETFIELD= 3;
	public static final byte ACCESSOR_PUTFIELD= 4;
	public static final byte ACCESSOR_INVOKEMETHOD= 5;


	public static final byte INVALID_INDEX = -1;		

	
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

	public static final String ANNOTATIONDEFAULT_ATTRIBUTE_NAME = "AnnotationDefault";
	public static final String CODE_ATTRIBUTE_NAME = "Code";
	public static final String CONSTANTVALUE_ATTRIBUTE_NAME = "ConstantValue";
	public static final String DEPRECATED_ATTRIBUTE_NAME = "Deprecated";
	public static final String ENCLOSINGMETHOD_ATTRIBUTE_NAME = "EnclosingMethod";
	public static final String EXCEPTIONS_ATTRIBUTE_NAME = "Exceptions";
	public static final String INNERCLASSES_ATTRIBUTE_NAME = "InnerClasses";
	public static final String LINENUMBERTABLE_ATTRIBUTE_NAME = "LineNumberTable";
	public static final String LOCALVARIABLETABLE_ATTRIBUTE_NAME = "LocalVariableTable";
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
