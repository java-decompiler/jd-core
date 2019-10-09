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
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;

public class ClassFileNewExpression extends NewExpression {
    protected BaseType parameterTypes;

    public ClassFileNewExpression(int lineNumber, ObjectType type) {
        super(lineNumber, type);
    }

    public ClassFileNewExpression(int lineNumber, ObjectType type, BodyDeclaration bodyDeclaration) {
        super(lineNumber, type, bodyDeclaration);
    }

    public BaseType getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(BaseType parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public void set(String descriptor, BaseType parameterTypes, BaseExpression parameters) {
        this.descriptor = descriptor;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "ClassFileNewExpression{new " + type + "}";
    }
}
