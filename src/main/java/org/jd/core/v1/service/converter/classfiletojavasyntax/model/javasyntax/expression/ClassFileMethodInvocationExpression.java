/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.Type;

public class ClassFileMethodInvocationExpression extends MethodInvocationExpression {
    private final BaseTypeParameter typeParameters;
    private BaseType parameterTypes;
    private BaseType unboundParameterTypes;
    private Type unboundType;
    private boolean bound;

    public ClassFileMethodInvocationExpression(
            int lineNumber, BaseTypeParameter typeParameters, Type type, Expression expression,
            String internalTypeName, String name, String descriptor, BaseType parameterTypes, BaseExpression parameters, boolean varArgs) {
        super(lineNumber, type, expression, internalTypeName, name, descriptor, parameters, varArgs);
        this.typeParameters = typeParameters;
        this.parameterTypes = parameterTypes;
    }

    public BaseTypeParameter getTypeParameters() {
        return typeParameters;
    }

    public BaseType getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(BaseType parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public boolean isBound() {
        return bound;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }

    public BaseType getUnboundParameterTypes() {
        return unboundParameterTypes;
    }

    public void setUnboundParameterTypes(BaseType unboundParameterTypes) {
        this.unboundParameterTypes = unboundParameterTypes;
    }

    public Type getUnboundType() {
        return unboundType;
    }

    public void setUnboundType(Type unboundType) {
        this.unboundType = unboundType;
    }
}
