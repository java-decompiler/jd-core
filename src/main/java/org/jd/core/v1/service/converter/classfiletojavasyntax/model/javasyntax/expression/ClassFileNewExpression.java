/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.util.DefaultList;

public class ClassFileNewExpression extends NewExpression {
    protected DefaultList<Type> parameterTypes;

    public ClassFileNewExpression(int lineNumber, ObjectType type) {
        super(lineNumber, type);
    }

    public ClassFileNewExpression(int lineNumber, ObjectType type, BodyDeclaration bodyDeclaration) {
        super(lineNumber, type, bodyDeclaration);
    }

    public DefaultList<Type> getParameterTypes() {
        return parameterTypes;
    }

    public void set(String descriptor, DefaultList<Type> parameterTypes, BaseExpression parameters) {
        this.descriptor = descriptor;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }
}
