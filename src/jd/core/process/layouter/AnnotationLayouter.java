package jd.core.process.layouter;

import java.util.ArrayList;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.attribute.Annotation;
import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeConstants;
import jd.core.model.classfile.attribute.AttributeRuntimeAnnotations;
import jd.core.model.layout.block.AnnotationsLayoutBlock;
import jd.core.model.layout.block.LayoutBlock;

public class AnnotationLayouter 
{
	public static void CreateBlocksForAnnotations(
		ClassFile classFile, Attribute[] attributes,
		List<LayoutBlock> layoutBlockList)
	{
		if (attributes == null)
			return;
		
		int attributesLength = attributes.length;
		ArrayList<Annotation> annotations = 
				new ArrayList<Annotation>(attributesLength);

		for (int i=0; i<attributesLength; i++)
		{
			Attribute attribute = attributes[i];
			
			switch(attribute.tag)
			{
			case AttributeConstants.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS:
			case AttributeConstants.ATTR_RUNTIME_VISIBLE_ANNOTATIONS:
				Annotation[] array = 
					((AttributeRuntimeAnnotations)attribute).annotations;
				
				if (array != null)
				{
					for (Annotation annotation : array)
						annotations.add(annotation);
				}
				break;
			}
		}
	
		if (annotations.size() > 0)
		{
			layoutBlockList.add(new AnnotationsLayoutBlock(
				classFile, annotations));
		}
	}
}
