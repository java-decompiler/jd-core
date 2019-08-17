/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.util.DefaultList;

public class ClassFileConstructorInvocationExpression extends ConstructorInvocationExpression {
    protected DefaultList<Type> parameterTypes;

    public ClassFileConstructorInvocationExpression(int lineNumber, ObjectType type, String descriptor, DefaultList<Type> parameterTypes, BaseExpression parameters) {
        super(lineNumber, type, descriptor, parameters);
        this.parameterTypes = parameterTypes;
    }

    public DefaultList<Type> getParameterTypes() {
        return parameterTypes;
    }
}
