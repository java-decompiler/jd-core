package jd.core.process.writer;

import jd.core.loader.Loader;
import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.attribute.Annotation;
import jd.core.model.classfile.attribute.ElementValuePair;
import jd.core.model.classfile.attribute.ParameterAnnotations;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.Printer;


public class AnnotationWriter 
{
	public static void WriteParameterAnnotation(
		Loader loader, Printer printer, ReferenceMap referenceMap, 
		ClassFile classFile, ParameterAnnotations parameterAnnotation)
	{
		if (parameterAnnotation == null)
			return;
		
		Annotation[] annotations = parameterAnnotation.annotations;

		if (annotations == null)
			return;
		
		for (int i=0; i<annotations.length; i++)
		{
			WriteAnnotation(
				loader, printer, referenceMap, classFile, annotations[i]);
			printer.print(' ');
		}
	}
	
	public static void WriteAnnotation(
		Loader loader, Printer printer, ReferenceMap referenceMap, 
		ClassFile classFile, Annotation annotation)
	{
		printer.startOfAnnotationName();
		printer.print('@');
		String annotationName = 
			classFile.getConstantPool().getConstantUtf8(annotation.type_index);
		SignatureWriter.WriteSignature(
			loader, printer, referenceMap, classFile, annotationName);
		printer.endOfAnnotationName();
		
		ElementValuePair[] evps = annotation.elementValuePairs;
		if (evps != null)
		{
			if (evps.length > 0)
			{			
				printer.print('(');
				
				ConstantPool constants = classFile.getConstantPool();
				String name = constants.getConstantUtf8(evps[0].element_name_index);
				
				if ((evps.length > 1) || !"value".equals(name))
				{
					printer.print(name);
					printer.print('=');
				}
				ElementValueWriter.WriteElementValue(
					loader, printer, referenceMap, 
					classFile, evps[0].element_value);
				
				for (int j=1; j<evps.length; j++)
				{
					name = constants.getConstantUtf8(evps[j].element_name_index);

					printer.print(", ");
					printer.print(name);
					printer.print('=');
					ElementValueWriter.WriteElementValue(
						loader, printer, referenceMap, 
						classFile, evps[j].element_value);
				}
				
				printer.print(')');
			}
		}
	}
}