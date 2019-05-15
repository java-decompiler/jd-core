/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.PrimitiveTypeUtil;

import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.*;


public class IntegerConstantExpression extends AbstractLineNumberTypeExpression {
    protected int value;

    public IntegerConstantExpression(Type type, int value) {
        super(type);
        this.value = value;
        assert type.isPrimitive();
    }

    public IntegerConstantExpression(int lineNumber, Type type, int value) {
        super(lineNumber, type);
        this.value = value;
        assert type.isPrimitive();
    }

    public int getValue() {
        return value;
    }

    @Override
    public void setType(Type type) {
        assert type.isPrimitive() && checkType(type) : "setType : incompatible types";
        super.setType(type);
    }

    protected boolean checkType(Type type) {
        if (type.isPrimitive()) {
            switch (PrimitiveTypeUtil.getStandardPrimitiveTypeFlags(((PrimitiveType)type).getFlags())) {
                case FLAG_BYTE:
                    return (Byte.MIN_VALUE <= value) && (value <= Byte.MAX_VALUE);
                case FLAG_CHAR:
                    return (Character.MIN_VALUE <= (char)value) && ((char)value <= Character.MAX_VALUE);
                case FLAG_SHORT:
                    return (Short.MIN_VALUE <= value) && (value <= Short.MAX_VALUE);
                case FLAG_BOOLEAN:
                    return (0 <= value) && (value <= 1);
                default:
                    return (Integer.MIN_VALUE <= value) && (value <= Integer.MAX_VALUE);
            }
        }

        return false;
    }
    
    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "IntegerConstantExpression{type=" + type + ", value=" + value + "}";
    }
}
