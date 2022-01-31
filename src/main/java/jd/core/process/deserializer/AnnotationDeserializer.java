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

public final class AnnotationDeserializer
{
    private AnnotationDeserializer() {
        super();
    }

    public static Annotation[] deserialize(DataInput di)
        throws IOException
    {
        int numAnnotations = di.readUnsignedShort();
        if (numAnnotations == 0) {
            return null;
        }

        Annotation[] annotations = new Annotation[numAnnotations];

        for (int i=0; i<numAnnotations; i++) {
            annotations[i] = new Annotation(
                    di.readUnsignedShort(),
                    deserializeElementValuePairs(di));
        }

        return annotations;
    }

    private static ElementValuePair[] deserializeElementValuePairs(DataInput di)
        throws IOException
    {
        int numElementValuePairs = di.readUnsignedShort();
        if (numElementValuePairs == 0) {
            return null;
        }

        ElementValuePair[] pairs = new ElementValuePair[numElementValuePairs];

        for(int i=0; i < numElementValuePairs; i++) {
            pairs[i] = new ElementValuePair(
                                di.readUnsignedShort(),
                                deserializeElementValue(di));
        }

        return pairs;
    }

    public static ElementValue deserializeElementValue(DataInput di)
        throws IOException
    {
        byte type = di.readByte();

        switch (type)
        {
        case 'B', 'D', 'F', 'I', 'J', 'S', 'Z', 'C', 's':
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
                               deserializeElementValuePairs(di)));
        case '[':
            return new ElementValueArrayValue(
                    ElementValueContants.EV_ARRAY_VALUE,
                    deserializeElementValues(di));
        default:
            throw new ClassFormatException("Invalid element value type: " + type);
        }
    }

    private static ElementValue[] deserializeElementValues(DataInput di)
        throws IOException
    {
        int numValues = di.readUnsignedShort();
        if (numValues == 0) {
            return null;
        }

        ElementValue[] values = new ElementValue[numValues];

        for (int i=0; i<numValues; i++) {
            values[i] = deserializeElementValue(di);
        }

        return values;
    }
}
