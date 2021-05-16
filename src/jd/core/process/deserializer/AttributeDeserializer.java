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
package jd.core.process.deserializer;

import java.io.DataInput;
import java.io.IOException;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeAnnotationDefault;
import jd.core.model.classfile.attribute.AttributeCode;
import jd.core.model.classfile.attribute.AttributeConstantValue;
import jd.core.model.classfile.attribute.AttributeConstants;
import jd.core.model.classfile.attribute.AttributeDeprecated;
import jd.core.model.classfile.attribute.AttributeEnclosingMethod;
import jd.core.model.classfile.attribute.AttributeExceptions;
import jd.core.model.classfile.attribute.AttributeInnerClasses;
import jd.core.model.classfile.attribute.AttributeLocalVariableTable;
import jd.core.model.classfile.attribute.AttributeNumberTable;
import jd.core.model.classfile.attribute.AttributeRuntimeAnnotations;
import jd.core.model.classfile.attribute.AttributeRuntimeParameterAnnotations;
import jd.core.model.classfile.attribute.AttributeSignature;
import jd.core.model.classfile.attribute.AttributeSourceFile;
import jd.core.model.classfile.attribute.AttributeSynthetic;
import jd.core.model.classfile.attribute.CodeException;
import jd.core.model.classfile.attribute.InnerClass;
import jd.core.model.classfile.attribute.LineNumber;
import jd.core.model.classfile.attribute.ParameterAnnotations;
import jd.core.model.classfile.attribute.UnknowAttribute;



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
			    		AttributeConstants.ATTR_ANNOTATION_DEFAULT,
			    		attribute_name_index,
			    		AnnotationDeserializer.DeserializeElementValue(di));
			}
			else if (attribute_name_index == constants.codeAttributeNameIndex)
			{
			    attributes[i] = new AttributeCode(
			    		AttributeConstants.ATTR_CODE,
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
						AttributeConstants.ATTR_CONSTANT_VALUE,
	                    attribute_name_index, 
	                    di.readUnsignedShort());
			} 
			else if (attribute_name_index == constants.deprecatedAttributeNameIndex)
			{
				if (attribute_length != 0) 
					throw new ClassFormatException("Invalid attribute length");
			    attributes[i] = new AttributeDeprecated(
			    		AttributeConstants.ATTR_DEPRECATED,
						attribute_name_index);
			}
			else if (attribute_name_index == constants.enclosingMethodAttributeNameIndex)
			{
				if (attribute_length != 4)
					throw new ClassFormatException("Invalid attribute length");
			    attributes[i] = new AttributeEnclosingMethod(
			    		AttributeConstants.ATTR_ENCLOSING_METHOD,
		    		    attribute_name_index,
		    		    di.readUnsignedShort(),
		    		    di.readUnsignedShort());
			}
			else if (attribute_name_index == constants.exceptionsAttributeNameIndex)
			{
			    attributes[i] = new AttributeExceptions(
			    		AttributeConstants.ATTR_EXCEPTIONS,
    		            attribute_name_index,
    		            DeserializeExceptionIndexTable(di));
			}
			else if (attribute_name_index == constants.innerClassesAttributeNameIndex)
			{
			    attributes[i] = new AttributeInnerClasses(
			    		AttributeConstants.ATTR_INNER_CLASSES,
    		            attribute_name_index,
    		            DeserializeInnerClasses(di));
			}
			else if (attribute_name_index == constants.lineNumberTableAttributeNameIndex)
			{
			    attributes[i] = new AttributeNumberTable(
			    		AttributeConstants.ATTR_NUMBER_TABLE,
    		            attribute_name_index,
	                    DeserializeLineNumbers(di));
			}
			else if (attribute_name_index == constants.localVariableTableAttributeNameIndex)
			{
			    attributes[i] = new AttributeLocalVariableTable(
			    		AttributeConstants.ATTR_LOCAL_VARIABLE_TABLE,
    		            attribute_name_index,
    		            DeserializeLocalVariable(di));
			}
			else if (attribute_name_index == constants.localVariableTypeTableAttributeNameIndex)
			{
			    attributes[i] = new AttributeLocalVariableTable(
			    		AttributeConstants.ATTR_LOCAL_VARIABLE_TYPE_TABLE,
    		            attribute_name_index,
    		            DeserializeLocalVariable(di));
			}
			else if (attribute_name_index == constants.runtimeInvisibleAnnotationsAttributeNameIndex)
			{		
			    attributes[i] = new AttributeRuntimeAnnotations(
			    		AttributeConstants.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS,
    		            attribute_name_index,
    		            AnnotationDeserializer.Deserialize(di));
			}
			else if (attribute_name_index == constants.runtimeVisibleAnnotationsAttributeNameIndex)
			{
			    attributes[i] = new AttributeRuntimeAnnotations(
			    		AttributeConstants.ATTR_RUNTIME_VISIBLE_ANNOTATIONS,
				        attribute_name_index,
    		            AnnotationDeserializer.Deserialize(di));
			}
			else if (attribute_name_index == constants.runtimeInvisibleParameterAnnotationsAttributeNameIndex)
			{
			    attributes[i] = new AttributeRuntimeParameterAnnotations(
			    		AttributeConstants.ATTR_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS,
				        attribute_name_index,
				        DeserializeParameterAnnotations(di));
			}
			else if (attribute_name_index == constants.runtimeVisibleParameterAnnotationsAttributeNameIndex)
			{
			    attributes[i] = new AttributeRuntimeParameterAnnotations(
			    		AttributeConstants.ATTR_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS,
				        attribute_name_index,
				        DeserializeParameterAnnotations(di));
			}
			else if (attribute_name_index == constants.signatureAttributeNameIndex)
			{
				if (attribute_length != 2)
					throw new ClassFormatException("Invalid attribute length");
				attributes[i] = new AttributeSignature(
						AttributeConstants.ATTR_SIGNATURE,
	                    attribute_name_index, 
	                    di.readUnsignedShort());
			}
			else if (attribute_name_index == constants.sourceFileAttributeNameIndex)
			{
				if (attribute_length != 2)
					throw new ClassFormatException("Invalid attribute length");
				attributes[i] = new AttributeSourceFile(
						AttributeConstants.ATTR_SOURCE_FILE,
	                    attribute_name_index, 
	                    di.readUnsignedShort());
			}
			else if (attribute_name_index == constants.syntheticAttributeNameIndex)
			{
				if (attribute_length != 0)
					throw new ClassFormatException("Invalid attribute length");
			    attributes[i] = new AttributeSynthetic(
			    		AttributeConstants.ATTR_SYNTHETIC,
			    		attribute_name_index);
			}
			else
			{
				attributes[i] = new UnknowAttribute(
						AttributeConstants.ATTR_UNKNOWN,
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
			codeExceptions[i] = new CodeException(i,
					                              di.readUnsignedShort(), 
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
