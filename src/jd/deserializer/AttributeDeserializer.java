package jd.deserializer;

import java.io.DataInput;
import java.io.IOException;

import jd.Constants;
import jd.classfile.ConstantPool;
import jd.classfile.LocalVariable;
import jd.classfile.attribute.Attribute;
import jd.classfile.attribute.AttributeAnnotationDefault;
import jd.classfile.attribute.AttributeCode;
import jd.classfile.attribute.AttributeConstantValue;
import jd.classfile.attribute.AttributeDeprecated;
import jd.classfile.attribute.AttributeEnclosingMethod;
import jd.classfile.attribute.AttributeExceptions;
import jd.classfile.attribute.AttributeInnerClasses;
import jd.classfile.attribute.AttributeLocalVariableTable;
import jd.classfile.attribute.AttributeNumberTable;
import jd.classfile.attribute.AttributeRuntimeInvisibleAnnotations;
import jd.classfile.attribute.AttributeRuntimeInvisibleParameterAnnotations;
import jd.classfile.attribute.AttributeRuntimeVisibleAnnotations;
import jd.classfile.attribute.AttributeRuntimeVisibleParameterAnnotations;
import jd.classfile.attribute.AttributeSignature;
import jd.classfile.attribute.AttributeSourceFile;
import jd.classfile.attribute.AttributeSynthetic;
import jd.classfile.attribute.InnerClass;
import jd.classfile.attribute.CodeException;
import jd.classfile.attribute.LineNumber;
import jd.classfile.attribute.ParameterAnnotations;
import jd.classfile.attribute.UnknowAttribute;
import jd.exception.ClassFormatException;



public class AttributeDeserializer 
{
	public static Attribute[] Deserialize(
			DataInput di, ConstantPool constants)
		throws IOException
	{
		int count = di.readUnsignedShort();
		if (count == 0)
			return null;
			
		Attribute[] attributes = new Attribute[count];
		
		for (int i=0; i<count; i++)
		{
			int attribute_name_index = di.readUnsignedShort();			
			int attribute_length = di.readInt();

			if (attribute_name_index == constants.annotationDefaultAttributeNameIndex)
			{
			    attributes[i] = new AttributeAnnotationDefault(
			    		Constants.ATTR_DEFAULT,
			    		attribute_name_index,
			    		AnnotationDeserializer.DeserializeElementValue(di));
			}
			else if (attribute_name_index == constants.codeAttributeNameIndex)
			{
			    attributes[i] = new AttributeCode(
			    		Constants.ATTR_CODE,
    		            attribute_name_index,
		                di.readUnsignedShort(), 
		                di.readUnsignedShort(), 
		                DeserializeCode(di), 
		                DeserializeCodeExceptions(di),
		                Deserialize(di, constants));
			}
			else if (attribute_name_index == constants.constantValueAttributeNameIndex)
			{
				if (attribute_length != 2)
					throw new ClassFormatException("Invalid attribute length");
				attributes[i] = new AttributeConstantValue(
    					Constants.ATTR_CONSTANT_VALUE,
	                    attribute_name_index, 
	                    di.readUnsignedShort());
			} 
			else if (attribute_name_index == constants.deprecatedAttributeNameIndex)
			{
				if (attribute_length != 0) 
					throw new ClassFormatException("Invalid attribute length");
			    attributes[i] = new AttributeDeprecated(
						Constants.ATTR_DEPRECATED,
						attribute_name_index);
			}
			else if (attribute_name_index == constants.enclosingMethodAttributeNameIndex)
			{
				if (attribute_length != 4)
					throw new ClassFormatException("Invalid attribute length");
			    attributes[i] = new AttributeEnclosingMethod(
						Constants.ATTR_ENCLOSING_METHOD,
		    		    attribute_name_index,
		    		    di.readUnsignedShort(),
		    		    di.readUnsignedShort());
			}
			else if (attribute_name_index == constants.exceptionsAttributeNameIndex)
			{
			    attributes[i] = new AttributeExceptions(
						Constants.ATTR_EXCEPTIONS,
    		            attribute_name_index,
    		            DeserializeExceptionIndexTable(di));
			}
			else if (attribute_name_index == constants.innerClassesAttributeNameIndex)
			{
			    attributes[i] = new AttributeInnerClasses(
						Constants.ATTR_INNER_CLASSES,
    		            attribute_name_index,
    		            DeserializeInnerClasses(di));
			}
			else if (attribute_name_index == constants.lineNumberTableAttributeNameIndex)
			{
			    attributes[i] = new AttributeNumberTable(
						Constants.ATTR_NUMBER_TABLE,
    		            attribute_name_index,
	                    DeserializeLineNumbers(di));
			}
			else if (attribute_name_index == constants.localVariableTableAttributeNameIndex)
			{
			    attributes[i] = new AttributeLocalVariableTable(
						Constants.ATTR_LOCAL_VARIABLE_TABLE,
    		            attribute_name_index,
    		            DeserializeLocalVariable(di));
			}
			else if (attribute_name_index == constants.runtimeInvisibleAnnotationsAttributeNameIndex)
			{		
			    attributes[i] = new AttributeRuntimeInvisibleAnnotations(
						Constants.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS,
    		            attribute_name_index,
    		            AnnotationDeserializer.Deserialize(di));
			}
			else if (attribute_name_index == constants.runtimeVisibleAnnotationsAttributeNameIndex)
			{
			    attributes[i] = new AttributeRuntimeVisibleAnnotations(
						Constants.ATTR_RUNTIME_VISIBLE_ANNOTATIONS,
				        attribute_name_index,
    		            AnnotationDeserializer.Deserialize(di));
			}
			else if (attribute_name_index == constants.runtimeInvisibleParameterAnnotationsAttributeNameIndex)
			{
			    attributes[i] = new AttributeRuntimeInvisibleParameterAnnotations(
			    		Constants.ATTR_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS,
				        attribute_name_index,
				        DeserializeParameterAnnotations(di));
			}
			else if (attribute_name_index == constants.runtimeVisibleParameterAnnotationsAttributeNameIndex)
			{
			    attributes[i] = new AttributeRuntimeVisibleParameterAnnotations(
			    		Constants.ATTR_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS,
				        attribute_name_index,
				        DeserializeParameterAnnotations(di));
			}
			else if (attribute_name_index == constants.signatureAttributeNameIndex)
			{
				if (attribute_length != 2)
					throw new ClassFormatException("Invalid attribute length");
				attributes[i] = new AttributeSignature(
			    		Constants.ATTR_SIGNATURE,
	                    attribute_name_index, 
	                    di.readUnsignedShort());
			}
			else if (attribute_name_index == constants.sourceFileAttributeNameIndex)
			{
				if (attribute_length != 2)
					throw new ClassFormatException("Invalid attribute length");
				attributes[i] = new AttributeSourceFile(
			    		Constants.ATTR_SOURCE_FILE,
	                    attribute_name_index, 
	                    di.readUnsignedShort());
			}
			else if (attribute_name_index == constants.syntheticAttributeNameIndex)
			{
				if (attribute_length != 0)
					throw new ClassFormatException("Invalid attribute length");
			    attributes[i] = new AttributeSynthetic(
			    		Constants.ATTR_SYNTHETIC,
			    		attribute_name_index);
			}
			else
			{
				attributes[i] = new UnknowAttribute(
			    		Constants.ATTR_UNKNOWN,
						attribute_name_index);
				for (int j=0; j<attribute_length; j++)
					di.readByte();
			}
		}
		
		return attributes;
	}
	
	private static byte[] DeserializeCode(DataInput di)
		throws IOException
	{
		int code_length = di.readInt();
		if (code_length == 0)
			return null;
		
		byte[] code = new byte[code_length];
		di.readFully(code);
		
		return code;
	}
	
	private static CodeException[] DeserializeCodeExceptions(DataInput di)
		throws IOException
	{
		int count = di.readUnsignedShort();
		if (count == 0)
			return null;
		
		CodeException[] codeExceptions = new CodeException[count];
		
		for (int i=0; i<count; i++)
			codeExceptions[i] = new CodeException(di.readUnsignedShort(), 
												  di.readUnsignedShort(), 
												  di.readUnsignedShort(), 
												  di.readUnsignedShort());
		return codeExceptions;
	}
	
	private static LineNumber[] DeserializeLineNumbers(DataInput di)
		throws IOException
	{
		int count = di.readUnsignedShort();
		if (count == 0)
			return null;

		LineNumber[] lineNumbers = new LineNumber[count];
		
		for (int i=0; i<count; i++)
			lineNumbers[i] = new LineNumber(di.readUnsignedShort(), 
											di.readUnsignedShort());
		return lineNumbers;
	}
	
	private static LocalVariable[] DeserializeLocalVariable(DataInput di)
		throws IOException
	{
		int count = di.readUnsignedShort();
		if (count == 0)
			return null;
		
		LocalVariable[] localVariables = new LocalVariable[count];
		
		for (int i=0; i<count; i++)
			localVariables[i] = new LocalVariable(di.readUnsignedShort(), 
											      di.readUnsignedShort(),
											      di.readUnsignedShort(),
											      di.readUnsignedShort(),
											      di.readUnsignedShort());
		
		return localVariables;
	}
	
	private static int[] DeserializeExceptionIndexTable(DataInput di)
		throws IOException
	{
		int number_of_exceptions = di.readUnsignedShort();
		if (number_of_exceptions == 0)
			return null;

		int[] exception_index_table = new int[number_of_exceptions];
		
	    for(int i=0; i < number_of_exceptions; i++)
	        exception_index_table[i] = di.readUnsignedShort();
		
		return exception_index_table;
	}
	
	private static InnerClass[] DeserializeInnerClasses(DataInput di)
		throws IOException
	{
		int number_of_classes = di.readUnsignedShort();
		if (number_of_classes == 0)
			return null;

		InnerClass[] classes = new InnerClass[number_of_classes];
		
	    for(int i=0; i < number_of_classes; i++)
	    	classes[i] = new InnerClass(di.readUnsignedShort(),
						    		 di.readUnsignedShort(),
						    		 di.readUnsignedShort(),
						    		 di.readUnsignedShort());
		
		return classes;
	}
	
	private static ParameterAnnotations[] DeserializeParameterAnnotations(
			                               DataInput di)
		throws IOException
	{
		int num_parameters = di.readUnsignedByte();
		if (num_parameters == 0)
			return null;

		ParameterAnnotations[] parameterAnnotations = 
			new ParameterAnnotations[num_parameters];
		
		for(int i=0; i < num_parameters; i++)
			parameterAnnotations[i] = new ParameterAnnotations(
					AnnotationDeserializer.Deserialize(di));
		
		return parameterAnnotations;
	}
}
