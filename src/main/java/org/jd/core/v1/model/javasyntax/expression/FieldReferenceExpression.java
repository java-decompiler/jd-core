/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.type.Type;

public class FieldReferenceExpression extends AbstractLineNumberTypeExpression {
    private Expression expression;
    private final String internalTypeName;
    private String name;
    private final String descriptor;

    public FieldReferenceExpression(Type type, Expression expression, String internalTypeName, String name, String descriptor) {
        super(type);
        this.expression = expression;
        this.internalTypeName = internalTypeName;
        this.name = name;
        this.descriptor = descriptor;
    }

    public FieldReferenceExpression(int lineNumber, Type type, Expression expression, String internalTypeName, String name, String descriptor) {
        super(lineNumber, type);
        this.expression = expression;
        this.internalTypeName = internalTypeName;
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String getInternalTypeName() {
        return internalTypeName;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean isFieldReferenceExpression() { return true; }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "FieldReferenceExpression{type=" + type + ", expression=" + expression + ", name=" + name + ", descriptor=" + descriptor + "}";
    }

    @Override
    public Expression copyTo(int lineNumber) {
        return new FieldReferenceExpression(lineNumber, type, expression, internalTypeName, name, descriptor);
    }
}
