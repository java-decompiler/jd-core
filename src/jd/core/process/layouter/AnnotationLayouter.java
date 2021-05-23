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
