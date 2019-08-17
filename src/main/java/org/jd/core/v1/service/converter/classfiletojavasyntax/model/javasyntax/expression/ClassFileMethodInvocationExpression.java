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
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.util.DefaultList;

public class ClassFileMethodInvocationExpression extends MethodInvocationExpression {
    protected DefaultList<Type> parameterTypes;

    public ClassFileMethodInvocationExpression(int lineNumber, Type type, Expression expression, String internalTypeName, String name, String descriptor, DefaultList<Type> parameterTypes, BaseExpression parameters) {
        super(lineNumber, type, expression, internalTypeName, name, descriptor, parameters);
        this.parameterTypes = parameterTypes;
    }

    public DefaultList<Type> getParameterTypes() {
        return parameterTypes;
    }
}
