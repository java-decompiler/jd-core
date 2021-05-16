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
package jd.core.process.analyzer.classfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.Annotation;
import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeConstants;
import jd.core.model.classfile.attribute.AttributeRuntimeAnnotations;
import jd.core.model.classfile.attribute.AttributeRuntimeParameterAnnotations;
import jd.core.model.classfile.attribute.AttributeSignature;
import jd.core.model.classfile.attribute.CodeException;
import jd.core.model.classfile.attribute.ElementValue;
import jd.core.model.classfile.attribute.ElementValueAnnotationValue;
import jd.core.model.classfile.attribute.ElementValueArrayValue;
import jd.core.model.classfile.attribute.ElementValueClassInfo;
import jd.core.model.classfile.attribute.ElementValueContants;
import jd.core.model.classfile.attribute.ElementValueEnumConstValue;
import jd.core.model.classfile.attribute.ElementValuePair;
import jd.core.model.classfile.attribute.ParameterAnnotations;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.reference.Reference;
import jd.core.model.reference.ReferenceMap;
import jd.core.process.analyzer.classfile.visitor.ReferenceVisitor;
import jd.core.util.StringConstants;



public class ReferenceAnalyzer 
{
	public static void Analyze(
		ReferenceMap referenceMap, ClassFile classFile)
	{
		CountReferences(referenceMap, classFile);		
		ReduceReferences(referenceMap, classFile);
	}
	
	private static void CountReferences(
			ReferenceMap referenceMap, ClassFile classFile)
	{
		// Class
		referenceMap.add(classFile.getThisClassName());
		
		AttributeSignature as = classFile.getAttributeSignature();
		if (as == null)
		{
			// Super class
			if (classFile.getSuperClassIndex() != 0)
				referenceMap.add(classFile.getSuperClassName());
			// Interfaces
			int[] interfaces = classFile.getInterfaces();
			if (interfaces != null)
			{
				for(int i=interfaces.length-1; i>=0; --i) 
				{
					String internalInterfaceName = 
						classFile.getConstantPool().getConstantClassName(interfaces[i]);
					referenceMap.add(internalInterfaceName);
				}
			}
		}
		else
		{
			String signature = 
				classFile.getConstantPool().getConstantUtf8(as.signature_index);
			SignatureAnalyzer.AnalyzeClassSignature(referenceMap, signature);
		}
		
		// Class annotations
		CountReferencesInAttributes(
			referenceMap, classFile.getConstantPool(), classFile.getAttributes());
		
		// Inner classes
		ArrayList<ClassFile> innerClassFiles = classFile.getInnerClassFiles();	
		if (innerClassFiles != null)
			for (int i=innerClassFiles.size()-1; i>=0; --i)
				CountReferences(referenceMap, innerClassFiles.get(i));
		
		ReferenceVisitor visitor = 
			new ReferenceVisitor(classFile.getConstantPool(), referenceMap);
		
		// Fields
		CountReferencesInFields(referenceMap, visitor, classFile);
		
		// Methods
		CountReferencesInMethods(referenceMap, visitor, classFile);
	}
	
	private static void CountReferencesInAttributes(
			ReferenceMap referenceMap, ConstantPool constants, 
			Attribute[] attributes)
	{
		if (attributes != null)
		{
			for (int i=attributes.length-1; i>=0; --i)
				switch (attributes[i].tag)
				{
				case AttributeConstants.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS:
				case AttributeConstants.ATTR_RUNTIME_VISIBLE_ANNOTATIONS:
					{
						Annotation[] annotations = 
							((AttributeRuntimeAnnotations)attributes[i])
							.annotations;
						for (int j=annotations.length-1; j>=0; --j)
							CountAnnotationReference(referenceMap, constants, annotations[j]);
					}
					break;

				case AttributeConstants.ATTR_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS:
				case AttributeConstants.ATTR_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS:
					{
						ParameterAnnotations[] parameterAnnotations = 
							((AttributeRuntimeParameterAnnotations)
									attributes[i]).parameter_annotations;
						CountParameterAnnotationsReference(
								referenceMap, constants, parameterAnnotations);
					}
					break;
				}
		}
	}

	private static void CountAnnotationReference(
			ReferenceMap referenceMap, ConstantPool constants, 
			Annotation annotation)
	{
		String typeName = constants.getConstantUtf8(annotation.type_index);
		SignatureAnalyzer.AnalyzeSimpleSignature(referenceMap, typeName);
		
		ElementValuePair[] elementValuePairs = 
			annotation.elementValuePairs;
		if (elementValuePairs != null)
		{
			for (int j=elementValuePairs.length-1; j>=0; --j)
				CountElementValue(
					referenceMap, constants, elementValuePairs[j].element_value);
		}
	}
	
	private static void CountParameterAnnotationsReference(
			ReferenceMap referenceMap, ConstantPool constants, 
			ParameterAnnotations[] parameterAnnotations)
	{
		if (parameterAnnotations != null)
		{
			for (int i=parameterAnnotations.length-1; i>=0; --i)
			{
				Annotation[] annotations = parameterAnnotations[i].annotations;
				if (annotations != null)
				{
					for (int j=annotations.length-1; j>=0; --j)
						CountAnnotationReference(
							referenceMap, constants, annotations[j]);
				}
			}
		}
	}
	
	private static void CountElementValue(
			ReferenceMap referenceMap, ConstantPool constants, ElementValue ev)
	{
		String signature;
		ElementValueClassInfo evci;
		
		switch (ev.tag)
		{
		case ElementValueContants.EV_CLASS_INFO:
			{
				evci = (ElementValueClassInfo)ev;
				signature = constants.getConstantUtf8(evci.class_info_index);
				SignatureAnalyzer.AnalyzeSimpleSignature(referenceMap, signature);
			}
			break;
		case ElementValueContants.EV_ANNOTATION_VALUE:
			{
				ElementValueAnnotationValue evanv = (ElementValueAnnotationValue)ev;
				CountAnnotationReference(
						referenceMap, constants, evanv.annotation_value);
			}
			break;
		case ElementValueContants.EV_ARRAY_VALUE:
			{
				ElementValueArrayValue evarv = (ElementValueArrayValue)ev;
				ElementValue[] values = evarv.values;
	
				if (values != null)
				{
					for (int i=values.length-1; i>=0; --i)
						if (values[i].tag == ElementValueContants.EV_CLASS_INFO)
						{
							evci = (ElementValueClassInfo)values[i];
							signature = 
								constants.getConstantUtf8(evci.class_info_index);			
							SignatureAnalyzer.AnalyzeSimpleSignature(referenceMap, signature);
						}
				}
			}
			break;
		case ElementValueContants.EV_ENUM_CONST_VALUE:
			{
				ElementValueEnumConstValue evecv = (ElementValueEnumConstValue)ev;
				signature = constants.getConstantUtf8(evecv.type_name_index);
				SignatureAnalyzer.AnalyzeSimpleSignature(referenceMap, signature);
			}
			break;
		}
	}

	private static void CountReferencesInFields(
			ReferenceMap referenceMap, ReferenceVisitor visitor, 
			ClassFile classFile)
	{
		Field[] fields = classFile.getFields();
		
		if (fields == null)
			return;
		
	    for (int i=fields.length-1; i>=0; --i)
		{
			Field field = fields[i];
			
	    	if ((field.access_flags & ClassFileConstants.ACC_SYNTHETIC) != 0)
	    		continue;
	    	
	    	CountReferencesInAttributes(
	    			referenceMap, classFile.getConstantPool(), field.getAttributes());
	    	
			AttributeSignature as = field.getAttributeSignature();
			String signature = classFile.getConstantPool().getConstantUtf8(
				(as==null) ? field.descriptor_index : as.signature_index);
			SignatureAnalyzer.AnalyzeSimpleSignature(referenceMap, signature);
			
			if (field.getValueAndMethod() != null)
				visitor.visit(field.getValueAndMethod().getValue());
	    }
	}
	
	private static void CountReferencesInMethods(
			ReferenceMap referenceMap, ReferenceVisitor visitor, 
			ClassFile classFile)
	{
		Method[] methods = classFile.getMethods();
			
		if (methods == null)
			return;
		
		ConstantPool constants = classFile.getConstantPool();

		for (int i=methods.length-1; i>=0; --i)
		{
	    	Method method = methods[i];
	    	
	    	if (((method.access_flags & 
	    		 (ClassFileConstants.ACC_SYNTHETIC|ClassFileConstants.ACC_BRIDGE)) != 0) ||
	    		method.containsError())
	    		continue;
	    	
	    	CountReferencesInAttributes(
	    		referenceMap, classFile.getConstantPool(), method.getAttributes());
			
	    	// Signature
			AttributeSignature as = method.getAttributeSignature();
			String signature = constants.getConstantUtf8(
					(as==null) ? method.descriptor_index : as.signature_index);
			SignatureAnalyzer.AnalyzeMethodSignature(referenceMap, signature);
			
	    	// Exceptions
			int[] exceptionIndexes = method.getExceptionIndexes();
			if (exceptionIndexes != null)
				for (int j=exceptionIndexes.length-1; j>=0; --j)
					referenceMap.add(
						constants.getConstantClassName(exceptionIndexes[j]));
			
			// Default annotation method value
			ElementValue defaultAnnotationValue = method.getDefaultAnnotationValue();			
			if (defaultAnnotationValue != null)
				CountElementValue(
					referenceMap, constants, defaultAnnotationValue);
			
			// Local variables
			LocalVariables localVariables = method.getLocalVariables();
			if (localVariables != null)
				CountReferencesInLocalVariables(
					referenceMap, constants, localVariables);		
			
			// Code exceptions
			CodeException[] codeExceptions = method.getCodeExceptions();
			if (codeExceptions != null)
				CountReferencesInCodeExceptions(
					referenceMap, constants, codeExceptions);	
			
			// Code
			CountReferencesInCode(visitor, method);
		}
	}
	
	private static void CountReferencesInLocalVariables(
			ReferenceMap referenceMap, ConstantPool constants, 
			LocalVariables localVariables)
	{
		for (int i=localVariables.size()-1; i>=0; --i)
		{
			LocalVariable lv = localVariables.getLocalVariableAt(i);
			
			if ((lv != null) && (lv.signature_index > 0))
			{
				String signature = 
					constants.getConstantUtf8(lv.signature_index);
				SignatureAnalyzer.AnalyzeSimpleSignature(referenceMap, signature);
			}
		}
	}
	
	private static void CountReferencesInCodeExceptions(
			ReferenceMap referenceMap, ConstantPool constants, 
			CodeException[] codeExceptions)
	{
		for (int i=codeExceptions.length-1; i>=0; --i)
		{
			CodeException ce = codeExceptions[i];
			
			if (ce.catch_type != 0)
			{
				String internalClassName = 
					constants.getConstantClassName(ce.catch_type);
				referenceMap.add(internalClassName);
			}
		}
	}

	private static void CountReferencesInCode(
		ReferenceVisitor visitor, Method method)
	{
		List<Instruction> instructions = method.getFastNodes();
		
		if (instructions != null)
		{
			for (int i=instructions.size()-1; i>=0; --i)
				visitor.visit(instructions.get(i));
		}
	}

	private static void ReduceReferences(
		ReferenceMap referenceMap, ClassFile classFile)
	{		
		HashMap<String, Boolean> multipleInternalClassName = 
			new HashMap<String, Boolean>();
		
		Iterator<Reference> iterator = referenceMap.values().iterator();
		while (iterator.hasNext())
		{
			Reference reference = iterator.next();
			String internalName = reference.getInternalName();
			
			int index = 
				internalName.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);
			String internalClassName = 
				(index != -1) ? internalName.substring(index+1) : internalName;
				
			if (multipleInternalClassName.containsKey(internalClassName))
				multipleInternalClassName.put(internalClassName, Boolean.TRUE);
			else
				multipleInternalClassName.put(internalClassName, Boolean.FALSE);				
		}
		
		iterator = referenceMap.values().iterator();
		while (iterator.hasNext())
		{
			Reference reference = iterator.next();
			String internalName = reference.getInternalName();
			int index = 
				internalName.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);
			String internalPackageName;
			String internalClassName;
			
			if (index != -1)
			{
				internalPackageName = internalName.substring(0, index);
				internalClassName = internalName.substring(index+1);
			}
			else
			{
				internalPackageName = "";
				internalClassName = internalName;
			}
			
			String internalPackageName_className = 
				classFile.getInternalPackageName() + 
				StringConstants.INTERNAL_PACKAGE_SEPARATOR + internalClassName;
				
			if (!classFile.getInternalPackageName().equals(internalPackageName) && 
				multipleInternalClassName.get(internalClassName).booleanValue())
			{
				// Remove references with same name and different packages
				iterator.remove();
			}
			else if (referenceMap.contains(internalPackageName_className))
			{
				// Remove references with a name of same package of current class
				iterator.remove();
			}
		}		
	}
}
