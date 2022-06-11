/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantUtf8;
import org.jd.core.v1.model.classfile.attribute.Annotation;
import org.jd.core.v1.model.classfile.attribute.Annotations;
import org.jd.core.v1.model.classfile.attribute.AttributeElementValue;
import org.jd.core.v1.model.classfile.attribute.ElementValueAnnotationValue;
import org.jd.core.v1.model.classfile.attribute.ElementValueArrayValue;
import org.jd.core.v1.model.classfile.attribute.ElementValueClassInfo;
import org.jd.core.v1.model.classfile.attribute.ElementValueEnumConstValue;
import org.jd.core.v1.model.classfile.attribute.ElementValuePrimitiveType;
import org.jd.core.v1.model.classfile.attribute.ElementValueVisitor;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.TypeReferenceDotClassExpression;
import org.jd.core.v1.model.javasyntax.reference.AnnotationElementValue;
import org.jd.core.v1.model.javasyntax.reference.AnnotationReference;
import org.jd.core.v1.model.javasyntax.reference.AnnotationReferences;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.reference.BaseElementValue;
import org.jd.core.v1.model.javasyntax.reference.ElementValueArrayInitializerElementValue;
import org.jd.core.v1.model.javasyntax.reference.ElementValuePair;
import org.jd.core.v1.model.javasyntax.reference.ElementValuePairs;
import org.jd.core.v1.model.javasyntax.reference.ElementValues;
import org.jd.core.v1.model.javasyntax.reference.ExpressionElementValue;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;

import java.util.List;
import java.util.Map.Entry;

public class AnnotationConverter implements ElementValueVisitor {
    private final TypeMaker typeMaker;
    private BaseElementValue elementValue;

    public AnnotationConverter(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

    public BaseAnnotationReference convert(Annotations visibles, Annotations invisibles) {
        if (visibles == null) {
            if (invisibles == null) {
                return null;
            }
            return convert(invisibles);
        }
        if (invisibles == null) {
            return convert(visibles);
        }
        AnnotationReferences<AnnotationReference> aral = new AnnotationReferences<>();

        for (Annotation a : visibles.getAnnotations()) {
            aral.add(convert(a));
        }
        for (Annotation a : invisibles.getAnnotations()) {
            aral.add(convert(a));
        }

        return aral;
    }

    protected BaseAnnotationReference convert(Annotations annotations) {
        Annotation[] as = annotations.getAnnotations();

        if (as.length == 1) {
            return convert(as[0]);
        }
        AnnotationReferences<AnnotationReference> aral = new AnnotationReferences<>(as.length);

        for (Annotation a : as) {
            aral.add(convert(a));
        }

        return aral;
    }

    protected AnnotationReference convert(Annotation annotation) {
        String descriptor = annotation.descriptor();

        assert descriptor != null && descriptor.length() > 2 && descriptor.charAt(0) == 'L' && descriptor.charAt(descriptor.length()-1) == ';';

        ObjectType ot = typeMaker.makeFromDescriptor(descriptor);
        List<Entry<String, AttributeElementValue>> elementValuePairs = annotation.elementValuePairs();

        if (elementValuePairs == null) {
            return new AnnotationReference(ot);
        }
        if (elementValuePairs.size() == 1) {
            Entry<String, AttributeElementValue> elementValuePair = elementValuePairs.get(0);
            String elementName = elementValuePair.getKey();
            AttributeElementValue elemValue = elementValuePair.getValue();

            if ("value".equals(elementName)) {
                return new AnnotationReference(ot, convert(elemValue));
            }
            return new AnnotationReference(
                    ot,
                    new ElementValuePair(elementName, convert(elemValue)));
        }
        ElementValuePairs list = new ElementValuePairs(elementValuePairs.size());
        String elementName;
        AttributeElementValue elemValue;
        for (Entry<String, AttributeElementValue> elementValuePair : elementValuePairs) {
            elementName = elementValuePair.getKey();
            elemValue = elementValuePair.getValue();
            list.add(new ElementValuePair(elementName, convert(elemValue)));
        }
        return new AnnotationReference(ot, list);
    }

    public BaseElementValue convert(AttributeElementValue ev) {
        ev.accept(this);
        return elementValue;
    }

    /** --- ElementValueVisitor --- */
    @Override
    public void visit(ElementValuePrimitiveType elementValuePrimitiveType) {
        elementValue = switch (elementValuePrimitiveType.getType()) {
            case 'B' -> new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_BYTE, elementValuePrimitiveType.<ConstantInteger>getConstValue().getBytes()));
            case 'D' -> new ExpressionElementValue(new DoubleConstantExpression(elementValuePrimitiveType.<ConstantDouble>getConstValue().getBytes()));
            case 'F' -> new ExpressionElementValue(new FloatConstantExpression(elementValuePrimitiveType.<ConstantFloat>getConstValue().getBytes()));
            case 'I' -> new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_INT, elementValuePrimitiveType.<ConstantInteger>getConstValue().getBytes()));
            case 'J' -> new ExpressionElementValue(new LongConstantExpression(elementValuePrimitiveType.<ConstantLong>getConstValue().getBytes()));
            case 'S' -> new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_SHORT, elementValuePrimitiveType.<ConstantInteger>getConstValue().getBytes()));
            case 'Z' -> new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_BOOLEAN, elementValuePrimitiveType.<ConstantInteger>getConstValue().getBytes()));
            case 'C' -> new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_CHAR, elementValuePrimitiveType.<ConstantInteger>getConstValue().getBytes()));
            case 's' -> new ExpressionElementValue(new StringConstantExpression(elementValuePrimitiveType.<ConstantUtf8>getConstValue().getBytes()));
            default -> elementValue;
        };
    }

    @Override
    public void visit(ElementValueClassInfo elementValueClassInfo) {
        String classInfo = elementValueClassInfo.classInfo();
        ObjectType ot = typeMaker.makeFromDescriptor(classInfo);
        elementValue = new ExpressionElementValue(new TypeReferenceDotClassExpression(ot));
    }

    @Override
    public void visit(ElementValueAnnotationValue elementValueAnnotationValue) {
        Annotation annotationValue = elementValueAnnotationValue.annotationValue();
        AnnotationReference annotationReference = convert(annotationValue);
        elementValue = new AnnotationElementValue(annotationReference);
    }

    @Override
    public void visit(ElementValueEnumConstValue elementValueEnumConstValue) {
        String descriptor = elementValueEnumConstValue.descriptor();

        if (descriptor == null || descriptor.length() <= 2 || descriptor.charAt(0) != 'L' || descriptor.charAt(descriptor.length()-1) != ';') {
            throw new IllegalArgumentException("AnnotationConverter.visit(elementValueEnumConstValue)");
        }

        ObjectType ot = typeMaker.makeFromDescriptor(descriptor);
        String constName = elementValueEnumConstValue.constName();
        String internalTypeName = descriptor.substring(1, descriptor.length()-1);
        elementValue = new ExpressionElementValue(new FieldReferenceExpression(ot, new ObjectTypeReferenceExpression(ot), internalTypeName, constName, descriptor));
    }

    @Override
    public void visit(ElementValueArrayValue elementValueArrayValue) {
        AttributeElementValue[] values = elementValueArrayValue.values();

        if (values == null) {
            elementValue = new ElementValueArrayInitializerElementValue();
        } else if (values.length == 1) {
            values[0].accept(this);
            elementValue = new ElementValueArrayInitializerElementValue(elementValue);
        } else {
            ElementValues list = new ElementValues(values.length);

            for (AttributeElementValue value : values) {
                value.accept(this);
                list.add(elementValue);
            }

            elementValue = new ElementValueArrayInitializerElementValue(list);
        }
    }
}
