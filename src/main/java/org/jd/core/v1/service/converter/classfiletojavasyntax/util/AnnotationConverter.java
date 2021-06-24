/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.classfile.attribute.*;
import org.jd.core.v1.model.classfile.attribute.ElementValuePair;
import org.jd.core.v1.model.classfile.constant.*;
import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.reference.*;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;

public class AnnotationConverter implements ElementValueVisitor {
    protected TypeMaker typeMaker;
    protected BaseElementValue elementValue;

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
        String descriptor = annotation.getDescriptor();

        assert descriptor != null && descriptor.length() > 2 && descriptor.charAt(0) == 'L' && descriptor.charAt(descriptor.length()-1) == ';';

        ObjectType ot = typeMaker.makeFromDescriptor(descriptor);
        ElementValuePair[] elementValuePairs = annotation.getElementValuePairs();

        if (elementValuePairs == null) {
            return new AnnotationReference(ot);
        }
		if (elementValuePairs.length == 1) {
            ElementValuePair elementValuePair = elementValuePairs[0];
            String elementName = elementValuePair.getElementName();
            AttributeElementValue elementValue = elementValuePair.getElementValue();

            if ("value".equals(elementName)) {
                return new AnnotationReference(ot, convert(elementValue));
            }
            return new AnnotationReference(
                    ot,
                    new org.jd.core.v1.model.javasyntax.reference.ElementValuePair(elementName, convert(elementValue)));
        }
		ElementValuePairs list = new ElementValuePairs(elementValuePairs.length);
		String elementName;
		AttributeElementValue elementValue;
		for (ElementValuePair elementValuePair : elementValuePairs) {
		    elementName = elementValuePair.getElementName();
		    elementValue = elementValuePair.getElementValue();
		    list.add(new org.jd.core.v1.model.javasyntax.reference.ElementValuePair(elementName, convert(elementValue)));
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
        switch (elementValuePrimitiveType.getType()) {
            case 'B':
                elementValue = new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_BYTE, elementValuePrimitiveType.<ConstantInteger>getConstValue().getValue()));
                break;
            case 'D':
                elementValue = new ExpressionElementValue(new DoubleConstantExpression(elementValuePrimitiveType.<ConstantDouble>getConstValue().getValue()));
                break;
            case 'F':
                elementValue = new ExpressionElementValue(new FloatConstantExpression(elementValuePrimitiveType.<ConstantFloat>getConstValue().getValue()));
                break;
            case 'I':
                elementValue = new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_INT, elementValuePrimitiveType.<ConstantInteger>getConstValue().getValue()));
                break;
            case 'J':
                elementValue = new ExpressionElementValue(new LongConstantExpression(elementValuePrimitiveType.<ConstantLong>getConstValue().getValue()));
                break;
            case 'S':
                elementValue = new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_SHORT, elementValuePrimitiveType.<ConstantInteger>getConstValue().getValue()));
                break;
            case 'Z':
                elementValue = new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_BOOLEAN, elementValuePrimitiveType.<ConstantInteger>getConstValue().getValue()));
                break;
            case 'C':
                elementValue = new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_CHAR, elementValuePrimitiveType.<ConstantInteger>getConstValue().getValue()));
                break;
            case 's':
                elementValue = new ExpressionElementValue(new StringConstantExpression(elementValuePrimitiveType.<ConstantUtf8>getConstValue().getValue()));
                break;
        }
    }

    @Override
    public void visit(ElementValueClassInfo elementValueClassInfo) {
        String classInfo = elementValueClassInfo.getClassInfo();
        ObjectType ot = typeMaker.makeFromDescriptor(classInfo);
        elementValue = new ExpressionElementValue(new TypeReferenceDotClassExpression(ot));
    }

    @Override
    public void visit(ElementValueAnnotationValue elementValueAnnotationValue) {
        Annotation annotationValue = elementValueAnnotationValue.getAnnotationValue();
        AnnotationReference annotationReference = convert(annotationValue);
        elementValue = new AnnotationElementValue(annotationReference);
    }

    @Override
    public void visit(ElementValueEnumConstValue elementValueEnumConstValue) {
        String descriptor = elementValueEnumConstValue.getDescriptor();

        if (descriptor == null || descriptor.length() <= 2 || descriptor.charAt(0) != 'L' || descriptor.charAt(descriptor.length()-1) != ';') {
            throw new IllegalArgumentException("AnnotationConverter.visit(elementValueEnumConstValue)");
        }

        ObjectType ot = typeMaker.makeFromDescriptor(descriptor);
        String constName = elementValueEnumConstValue.getConstName();
        String internalTypeName = descriptor.substring(1, descriptor.length()-1);
        elementValue = new ExpressionElementValue(new FieldReferenceExpression(ot, new ObjectTypeReferenceExpression(ot), internalTypeName, constName, descriptor));
    }

    @Override
    public void visit(ElementValueArrayValue elementValueArrayValue) {
        AttributeElementValue[] values = elementValueArrayValue.getValues();

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
