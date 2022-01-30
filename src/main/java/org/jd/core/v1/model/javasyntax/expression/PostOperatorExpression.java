/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.type.Type;

public class PostOperatorExpression extends AbstractLineNumberExpression {
    private final String operator;
    private Expression expression;

    public PostOperatorExpression(int lineNumber, Expression expression, String operator) {
        super(lineNumber);
        this.operator = operator;
        this.expression = expression;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public Type getType() {
        return expression.getType();
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public boolean isPostOperatorExpression() { return true; }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "PostOperatorExpression{" + expression + " " + operator + "}";
    }

    @Override
    public Expression copyTo(int lineNumber) {
        return new PostOperatorExpression(lineNumber, expression, operator);
    }
}
