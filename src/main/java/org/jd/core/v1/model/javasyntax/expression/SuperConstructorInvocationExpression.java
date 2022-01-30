/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;

public class SuperConstructorInvocationExpression extends ConstructorReferenceExpression {
    private BaseExpression parameters;

    public SuperConstructorInvocationExpression(int lineNumber, ObjectType type, String descriptor, BaseExpression parameters) {
        super(lineNumber, PrimitiveType.TYPE_VOID, type, descriptor);
        this.parameters = parameters;
    }

    @Override
    public BaseExpression getParameters() {
        return parameters;
    }

    public void setParameters(BaseExpression parameters) {
        this.parameters = parameters;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public boolean isSuperConstructorInvocationExpression() { return true; }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "SuperConstructorInvocationExpression{call super(" + descriptor + ")}";
    }
}
