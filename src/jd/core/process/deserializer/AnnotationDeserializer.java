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

import jd.core.model.classfile.attribute.Annotation;
import jd.core.model.classfile.attribute.ElementValue;
import jd.core.model.classfile.attribute.ElementValueAnnotationValue;
import jd.core.model.classfile.attribute.ElementValueArrayValue;
import jd.core.model.classfile.attribute.ElementValueClassInfo;
import jd.core.model.classfile.attribute.ElementValueContants;
import jd.core.model.classfile.attribute.ElementValueEnumConstValue;
import jd.core.model.classfile.attribute.ElementValuePair;
import jd.core.model.classfile.attribute.ElementValuePrimitiveType;


public class AnnotationDeserializer 
{
	public static Annotation[] Deserialize(DataInput di)
		throws IOException
	{
		int num_annotations = di.readUnsignedShort();
		if (num_annotations == 0)
			return null;
		
		Annotation[] annotations = new Annotation[num_annotations];
		
		for (int i=0; i<num_annotations; i++)
			annotations[i] = new Annotation(
					di.readUnsignedShort(), 
					DeserializeElementValuePairs(di));
		
		return annotations;
	}
	
	private static ElementValuePair[] DeserializeElementValuePairs(DataInput di)
		throws IOException
	{
		int num_element_value_pairs = di.readUnsignedShort();
		if (num_element_value_pairs == 0)
			return null;
		
		ElementValuePair[] pairs = new ElementValuePair[num_element_value_pairs];
		
		for(int i=0; i < num_element_value_pairs; i++)
			pairs[i] = new ElementValuePair(
								di.readUnsignedShort(), 
								DeserializeElementValue(di));
		
		return pairs;
	}
	
	public static ElementValue DeserializeElementValue(DataInput di)
		throws IOException
	{
		byte type = di.readByte();

		switch (type)
		{
		case 'B': case 'D': case 'F': 
		case 'I': case 'J': case 'S': 
		case 'Z': case 'C': case 's':
			return new ElementValuePrimitiveType(
					ElementValueContants.EV_PRIMITIVE_TYPE, type,
					di.readUnsignedShort());
		case 'e':
			return new ElementValueEnumConstValue(
					ElementValueContants.EV_ENUM_CONST_VALUE, 
					di.readUnsignedShort(), 
					di.readUnsignedShort());
		case 'c':
			return new ElementValueClassInfo(
					ElementValueContants.EV_CLASS_INFO, 
					di.readUnsignedShort());
		case '@':
			return new ElementValueAnnotationValue(
					ElementValueContants.EV_ANNOTATION_VALUE, 
					new Annotation(di.readUnsignedShort(), 
							   DeserializeElementValuePairs(di)));
		case '[':
			return new ElementValueArrayValue(
					ElementValueContants.EV_ARRAY_VALUE, 
					DeserializeElementValues(di));
		default:
			throw new ClassFormatException("Invalid element value type: " + type);
		}
	}
	
	private static ElementValue[] DeserializeElementValues(DataInput di)
		throws IOException
	{
		int num_values = di.readUnsignedShort();
		if (num_values == 0)
			return null;

		ElementValue[] values = new ElementValue[num_values];
		
		for (int i=0; i<num_values; i++)
			values[i] = DeserializeElementValue(di);

		return values;
	}
}
