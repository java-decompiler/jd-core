/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.classfile.AnnotationElementValue;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.ArrayElementValue;
import org.apache.bcel.classfile.ClassElementValue;
import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.EnumElementValue;
import org.apache.bcel.classfile.SimpleElementValue;
import org.jd.core.v1.model.classfile.attribute.ElementValueVisitor;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.TypeReferenceDotClassExpression;
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

public class AnnotationConverter implements ElementValueVisitor {
    private final TypeMaker typeMaker;
    private BaseElementValue elementValue;

    public AnnotationConverter(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

    public BaseAnnotationReference convert(AnnotationEntry[] visibles, AnnotationEntry[] invisibles) {
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

        for (AnnotationEntry a : visibles) {
            aral.add(convert(a));
        }
        for (AnnotationEntry a : invisibles) {
            aral.add(convert(a));
        }

        return aral;
    }

    protected BaseAnnotationReference convert(AnnotationEntry[] as) {

        if (as.length == 1) {
            return convert(as[0]);
        }
        AnnotationReferences<AnnotationReference> aral = new AnnotationReferences<>(as.length);

        for (AnnotationEntry a : as) {
            aral.add(convert(a));
        }

        return aral;
    }

    protected AnnotationReference convert(AnnotationEntry annotation) {
        String descriptor = annotation.getAnnotationType();

        assert descriptor != null && descriptor.length() > 2 && descriptor.charAt(0) == 'L' && descriptor.charAt(descriptor.length()-1) == ';';

        ObjectType ot = typeMaker.makeFromDescriptor(descriptor);
        org.apache.bcel.classfile.ElementValuePair[] elementValuePairs = annotation.getElementValuePairs();

        if (elementValuePairs.length == 1) {
            org.apache.bcel.classfile.ElementValuePair elementValuePair = elementValuePairs[0];
            String elementName = elementValuePair.getNameString();
            ElementValue elemValue = elementValuePair.getValue();

            if ("value".equals(elementName)) {
                return new AnnotationReference(ot, convert(elemValue));
            }
            return new AnnotationReference(
                    ot,
                    new ElementValuePair(elementName, convert(elemValue)));
        }
        ElementValuePairs list = new ElementValuePairs(elementValuePairs.length);
        String elementName;
        ElementValue elemValue;
        for (org.apache.bcel.classfile.ElementValuePair elementValuePair : elementValuePairs) {
            elementName = elementValuePair.getNameString();
            elemValue = elementValuePair.getValue();
            list.add(new ElementValuePair(elementName, convert(elemValue)));
        }
        return new AnnotationReference(ot, list);
    }

    public BaseElementValue convert(ElementValue ev) {
        visit(ev);
        return elementValue;
    }

    public void visit(ElementValue ev) {
        if (ev instanceof SimpleElementValue) {
            visit((SimpleElementValue) ev);
        }
        if (ev instanceof ClassElementValue) {
            visit((ClassElementValue) ev);
        }
        if (ev instanceof AnnotationElementValue) {
            visit((AnnotationElementValue) ev);
        }
        if (ev instanceof EnumElementValue) {
            visit((EnumElementValue) ev);
        }
        if (ev instanceof ArrayElementValue) {
            visit((ArrayElementValue) ev);
        }
    }

    /** --- ElementValueVisitor --- */
    @Override
    public void visit(SimpleElementValue elementValuePrimitiveType) {
        elementValue = switch (elementValuePrimitiveType.getElementValueType()) {
            case 'B' -> new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_BYTE, elementValuePrimitiveType.getValueByte()));
            case 'D' -> new ExpressionElementValue(new DoubleConstantExpression(elementValuePrimitiveType.getValueDouble()));
            case 'F' -> new ExpressionElementValue(new FloatConstantExpression(elementValuePrimitiveType.getValueFloat()));
            case 'I' -> new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_INT, elementValuePrimitiveType.getValueInt()));
            case 'J' -> new ExpressionElementValue(new LongConstantExpression(elementValuePrimitiveType.getValueLong()));
            case 'S' -> new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_SHORT, elementValuePrimitiveType.getValueShort()));
            case 'Z' -> new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_BOOLEAN, elementValuePrimitiveType.getValueBoolean() ? 1 : 0));
            case 'C' -> new ExpressionElementValue(new IntegerConstantExpression(PrimitiveType.TYPE_CHAR, elementValuePrimitiveType.getValueChar()));
            case 's' -> new ExpressionElementValue(new StringConstantExpression(elementValuePrimitiveType.getValueString()));
            default -> elementValue;
        };
    }

    @Override
    public void visit(ClassElementValue elementValueClassInfo) {
        String classInfo = elementValueClassInfo.getClassString();
        ObjectType ot = typeMaker.makeFromDescriptor(classInfo);
        elementValue = new ExpressionElementValue(new TypeReferenceDotClassExpression(ot));
    }

    @Override
    public void visit(AnnotationElementValue elementValueAnnotationValue) {
        AnnotationEntry annotationValue = elementValueAnnotationValue.getAnnotationEntry();
        AnnotationReference annotationReference = convert(annotationValue);
        elementValue = new org.jd.core.v1.model.javasyntax.reference.AnnotationElementValue(annotationReference);
    }

    @Override
    public void visit(EnumElementValue elementValueEnumConstValue) {
        String descriptor = elementValueEnumConstValue.getEnumTypeString();

        if (descriptor == null || descriptor.length() <= 2 || descriptor.charAt(0) != 'L' || descriptor.charAt(descriptor.length()-1) != ';') {
            throw new IllegalArgumentException("AnnotationConverter.visit(elementValueEnumConstValue)");
        }

        ObjectType ot = typeMaker.makeFromDescriptor(descriptor);
        String constName = elementValueEnumConstValue.getEnumValueString();
        String internalTypeName = descriptor.substring(1, descriptor.length()-1);
        elementValue = new ExpressionElementValue(new FieldReferenceExpression(ot, new ObjectTypeReferenceExpression(ot), internalTypeName, constName, descriptor));
    }

    @Override
    public void visit(ArrayElementValue elementValueArrayValue) {
        ElementValue[] values = elementValueArrayValue.getElementValuesArray();

        if (values.length == 1) {
            visit(values[0]);
            elementValue = new ElementValueArrayInitializerElementValue(elementValue);
        } else {
            ElementValues list = new ElementValues(values.length);

            for (ElementValue value : values) {
                visit(value);
                list.add(elementValue);
            }

            elementValue = new ElementValueArrayInitializerElementValue(list);
        }
    }
}
