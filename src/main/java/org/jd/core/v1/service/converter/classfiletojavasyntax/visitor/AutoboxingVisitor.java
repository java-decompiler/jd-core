/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.util.StringConstants;

import java.util.HashMap;
import java.util.Map;

public class AutoboxingVisitor extends AbstractUpdateExpressionVisitor {
    protected static final Map<String, String> VALUEOF_DESCRIPTOR_MAP = new HashMap<>();

    protected static final Map<String, String> VALUE_DESCRIPTOR_MAP = new HashMap<>();
    protected static final Map<String, String> VALUE_METHODNAME_MAP = new HashMap<>();

    static {
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_BYTE, "(B)Ljava/lang/Byte;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_CHARACTER, "(C)Ljava/lang/Character;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_FLOAT, "(F)Ljava/lang/Float;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_INTEGER, "(I)Ljava/lang/Integer;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_LONG, "(J)Ljava/lang/Long;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_SHORT, "(S)Ljava/lang/Short;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_DOUBLE, "(D)Ljava/lang/Double;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_BOOLEAN, "(Z)Ljava/lang/Boolean;");

        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_BYTE, "()B");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_CHARACTER, "()C");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_FLOAT, "()F");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_INTEGER, "()I");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_LONG, "()J");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_SHORT, "()S");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_DOUBLE, "()D");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_BOOLEAN, "()Z");

        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_BYTE, "byteValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_CHARACTER, "charValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_FLOAT, "floatValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_INTEGER, "intValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_LONG, "longValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_SHORT, "shortValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_DOUBLE, "doubleValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_BOOLEAN, "booleanValue");
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration cfbd = (ClassFileBodyDeclaration)declaration;
        boolean autoBoxingSupported = (cfbd.getClassFile().getMajorVersion() >= 49); // (majorVersion >= Java 5)

        if (autoBoxingSupported) {
            safeAccept(declaration.getMemberDeclarations());
        }
    }

    @Override
    protected Expression updateExpression(Expression expression) {
        if (expression.isMethodInvocationExpression() && expression.getInternalTypeName().startsWith("java/lang/")) {
            int parameterSize = (expression.getParameters() == null) ? 0 : expression.getParameters().size();

            if (expression.getExpression().isObjectTypeReferenceExpression()) {
                // static method invocation
                if ((parameterSize == 1) &&
                        expression.getName().equals("valueOf") &&
                        expression.getDescriptor().equals(VALUEOF_DESCRIPTOR_MAP.get(expression.getInternalTypeName())))
                {
                    return expression.getParameters().getFirst();
                }
            } else {
                // non-static method invocation
                if ((parameterSize == 0) &&
                        expression.getName().equals(VALUE_METHODNAME_MAP.get(expression.getInternalTypeName())) &&
                        expression.getDescriptor().equals(VALUE_DESCRIPTOR_MAP.get(expression.getInternalTypeName())))
                {
                    return expression.getExpression();
                }
            }
        }

        return expression;
    }
}
