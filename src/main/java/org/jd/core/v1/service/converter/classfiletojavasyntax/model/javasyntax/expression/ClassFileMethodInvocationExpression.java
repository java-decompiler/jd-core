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
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.AbstractTypeParametersToTypeArgumentsBinder;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker.MethodTypes;

public class ClassFileMethodInvocationExpression extends MethodInvocationExpression {
    private BaseTypeParameter typeParameters;
    private BaseType parameterTypes;
    private BaseType unboundParameterTypes;
    private Type unboundType;
    private boolean bound;

    public ClassFileMethodInvocationExpression(
            int lineNumber, Type type, Expression expression,
            String internalTypeName, String name, String descriptor, BaseExpression parameters, MethodTypes methodTypes) {
        super(lineNumber, type, expression, internalTypeName, name, descriptor, parameters, methodTypes);
        this.typeParameters = methodTypes == null ? null : methodTypes.getTypeParameters();
        this.parameterTypes = methodTypes == null ? null : AbstractTypeParametersToTypeArgumentsBinder.clone(methodTypes.getParameterTypes());
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
