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

import org.apache.bcel.Const;
import org.apache.bcel.classfile.InnerClass;
import org.jd.core.v1.service.deserializer.classfile.attribute.InvalidAttributeLengthException;

import java.io.DataInput;
import java.io.IOException;

import static org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer.loadBootstrapMethods;
import static org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer.loadCode;
import static org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer.loadCodeExceptions;
import static org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer.loadLineNumbers;
import static org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer.loadParameters;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeAnnotationDefault;
import jd.core.model.classfile.attribute.AttributeBootstrapMethods;
import jd.core.model.classfile.attribute.AttributeCode;
import jd.core.model.classfile.attribute.AttributeConstantValue;
import jd.core.model.classfile.attribute.AttributeDeprecated;
import jd.core.model.classfile.attribute.AttributeEnclosingMethod;
import jd.core.model.classfile.attribute.AttributeExceptions;
import jd.core.model.classfile.attribute.AttributeInnerClasses;
import jd.core.model.classfile.attribute.AttributeLocalVariableTable;
import jd.core.model.classfile.attribute.AttributeMethodParameters;
import jd.core.model.classfile.attribute.AttributeNumberTable;
import jd.core.model.classfile.attribute.AttributeRuntimeAnnotations;
import jd.core.model.classfile.attribute.AttributeRuntimeParameterAnnotations;
import jd.core.model.classfile.attribute.AttributeSignature;
import jd.core.model.classfile.attribute.AttributeSourceFile;
import jd.core.model.classfile.attribute.AttributeSynthetic;
import jd.core.model.classfile.attribute.ParameterAnnotations;
import jd.core.model.classfile.attribute.UnknownAttribute;

public final class AttributeDeserializer
{
    private AttributeDeserializer() {
        super();
    }

    public static Attribute[] deserialize(
            DataInput di, ConstantPool constants)
        throws IOException
    {
        int count = di.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        Attribute[] attributes = new Attribute[count];

        for (int i=0; i<count; i++)
        {
            int attributeNameIndex = di.readUnsignedShort();
            int attributeLength = di.readInt();

            if (attributeNameIndex == constants.getAnnotationDefaultAttributeNameIndex())
            {
                attributes[i] = new AttributeAnnotationDefault(
                        Const.ATTR_ANNOTATION_DEFAULT,
                        AnnotationDeserializer.deserializeElementValue(di));
            }
            else if (attributeNameIndex == constants.getCodeAttributeNameIndex())
            {
                // skip max stack (1 short) and max locals (1 short) => skip 2 shorts = 4 bytes
                di.skipBytes(4);
                attributes[i] = new AttributeCode(
                        Const.ATTR_CODE,
                        loadCode(di),
                        loadCodeExceptions(di),
                        deserialize(di, constants));
            }
            else if (attributeNameIndex == constants.getConstantValueAttributeNameIndex())
            {
                if (attributeLength != 2) {
                    throw new InvalidAttributeLengthException();
                }
                attributes[i] = new AttributeConstantValue(
                        Const.ATTR_CONSTANT_VALUE,
                        di.readUnsignedShort());
            }
            else if (attributeNameIndex == constants.getDeprecatedAttributeNameIndex())
            {
                if (attributeLength != 0) {
                    throw new InvalidAttributeLengthException();
                }
                attributes[i] = new AttributeDeprecated(
                        Const.ATTR_DEPRECATED);
            }
            else if (attributeNameIndex == constants.getEnclosingMethodAttributeNameIndex())
            {
                if (attributeLength != 4) {
                    throw new InvalidAttributeLengthException();
                }
                di.skipBytes(attributeLength);
                attributes[i] = new AttributeEnclosingMethod(
                        Const.ATTR_ENCLOSING_METHOD);
            }
            else if (attributeNameIndex == constants.getExceptionsAttributeNameIndex())
            {
                attributes[i] = new AttributeExceptions(
                        Const.ATTR_EXCEPTIONS,
                        deserializeExceptionIndexTable(di));
            }
            else if (attributeNameIndex == constants.getInnerClassesAttributeNameIndex())
            {
                attributes[i] = new AttributeInnerClasses(
                        Const.ATTR_INNER_CLASSES,
                        deserializeInnerClasses(di));
            }
            else if (attributeNameIndex == constants.getLineNumberTableAttributeNameIndex())
            {
                attributes[i] = new AttributeNumberTable(
                        Const.ATTR_LINE_NUMBER_TABLE,
                        loadLineNumbers(di));
            }
            else if (attributeNameIndex == constants.getLocalVariableTableAttributeNameIndex())
            {
                attributes[i] = new AttributeLocalVariableTable(
                        Const.ATTR_LOCAL_VARIABLE_TABLE,
                        deserializeLocalVariable(di));
            }
            else if (attributeNameIndex == constants.getLocalVariableTypeTableAttributeNameIndex())
            {
                attributes[i] = new AttributeLocalVariableTable(
                        Const.ATTR_LOCAL_VARIABLE_TYPE_TABLE,
                        deserializeLocalVariable(di));
            }
            else if (attributeNameIndex == constants.getRuntimeInvisibleAnnotationsAttributeNameIndex())
            {
                attributes[i] = new AttributeRuntimeAnnotations(
                        Const.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS,
                        AnnotationDeserializer.deserialize(di));
            }
            else if (attributeNameIndex == constants.getRuntimeVisibleAnnotationsAttributeNameIndex())
            {
                attributes[i] = new AttributeRuntimeAnnotations(
                        Const.ATTR_RUNTIME_VISIBLE_ANNOTATIONS,
                        AnnotationDeserializer.deserialize(di));
            }
            else if (attributeNameIndex == constants.getRuntimeInvisibleParameterAnnotationsAttributeNameIndex())
            {
                attributes[i] = new AttributeRuntimeParameterAnnotations(
                        Const.ATTR_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS,
                        deserializeParameterAnnotations(di));
            }
            else if (attributeNameIndex == constants.getRuntimeVisibleParameterAnnotationsAttributeNameIndex())
            {
                attributes[i] = new AttributeRuntimeParameterAnnotations(
                        Const.ATTR_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS,
                        deserializeParameterAnnotations(di));
            }
            else if (attributeNameIndex == constants.getBootstrapMethodsAttributeNameIndex())
            {
                attributes[i] = new AttributeBootstrapMethods(
                        Const.ATTR_BOOTSTRAP_METHODS,
                        loadBootstrapMethods(di));
            }
            else if (attributeNameIndex == constants.getMethodParametersAttributeNameIndex())
            {
                attributes[i] = new AttributeMethodParameters(
                        Const.ATTR_METHOD_PARAMETERS,
                        loadParameters(di));
            }
            else if (attributeNameIndex == constants.getSignatureAttributeNameIndex())
            {
                if (attributeLength != 2) {
                    throw new InvalidAttributeLengthException();
                }
                attributes[i] = new AttributeSignature(
                        Const.ATTR_SIGNATURE,
                        di.readUnsignedShort());
            }
            else if (attributeNameIndex == constants.getSourceFileAttributeNameIndex())
            {
                if (attributeLength != 2) {
                    throw new InvalidAttributeLengthException();
                }
                di.skipBytes(attributeLength);
                attributes[i] = new AttributeSourceFile(
                        Const.ATTR_SOURCE_FILE);
            }
            else if (attributeNameIndex == constants.getSyntheticAttributeNameIndex())
            {
                if (attributeLength != 0) {
                    throw new InvalidAttributeLengthException();
                }
                attributes[i] = new AttributeSynthetic(
                        Const.ATTR_SYNTHETIC);
            }
            else
            {
                attributes[i] = new UnknownAttribute(
                        Const.ATTR_UNKNOWN);
                for (int j=0; j<attributeLength; j++) {
                    di.readByte();
                }
            }
        }

        return attributes;
    }

    private static LocalVariable[] deserializeLocalVariable(DataInput di)
        throws IOException
    {
        int count = di.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        LocalVariable[] localVariables = new LocalVariable[count];

        for (int i=0; i<count; i++) {
            localVariables[i] = new LocalVariable(di.readUnsignedShort(),
                                                  di.readUnsignedShort(),
                                                  di.readUnsignedShort(),
                                                  di.readUnsignedShort(),
                                                  di.readUnsignedShort());
        }

        return localVariables;
    }

    private static int[] deserializeExceptionIndexTable(DataInput di)
        throws IOException
    {
        int numberOfExceptions = di.readUnsignedShort();
        if (numberOfExceptions == 0) {
            return null;
        }

        int[] exceptionIndexTable = new int[numberOfExceptions];

        for(int i=0; i < numberOfExceptions; i++) {
            exceptionIndexTable[i] = di.readUnsignedShort();
        }

        return exceptionIndexTable;
    }

    private static InnerClass[] deserializeInnerClasses(DataInput di)
        throws IOException
    {
        int numberOfClasses = di.readUnsignedShort();
        if (numberOfClasses == 0) {
            return null;
        }

        InnerClass[] classes = new InnerClass[numberOfClasses];

        for(int i=0; i < numberOfClasses; i++) {
            classes[i] = new InnerClass(di.readUnsignedShort(),
                                     di.readUnsignedShort(),
                                     di.readUnsignedShort(),
                                     di.readUnsignedShort());
        }

        return classes;
    }

    private static ParameterAnnotations[] deserializeParameterAnnotations(
                                           DataInput di)
        throws IOException
    {
        int numParameters = di.readUnsignedByte();
        if (numParameters == 0) {
            return null;
        }

        ParameterAnnotations[] parameterAnnotations =
            new ParameterAnnotations[numParameters];

        for(int i=0; i < numParameters; i++) {
            parameterAnnotations[i] = new ParameterAnnotations(
                    AnnotationDeserializer.deserialize(di));
        }

        return parameterAnnotations;
    }
}
