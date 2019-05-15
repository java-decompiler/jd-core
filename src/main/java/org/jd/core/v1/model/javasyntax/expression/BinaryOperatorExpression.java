/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.type.Type;

public class BinaryOperatorExpression extends AbstractLineNumberTypeExpression {
    protected Expression leftExpression;
    protected String operator;
    protected Expression rightExpression;
    protected int priority;

    public BinaryOperatorExpression(int lineNumber, Type type, Expression leftExpression, String operator, Expression rightExpression, int priority) {
        super(lineNumber, type);
        this.operator = operator;
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
        this.priority = priority;
    }

    public Expression getLeftExpression() {
        return leftExpression;
    }

    public void setLeftExpression(Expression leftExpression) {
        this.leftExpression = leftExpression;
    }

    @SuppressWarnings("unchecked")
    public <T extends Expression> T getGenericLeftExpression() {
        return (T) leftExpression;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Expression getRightExpression() {
        return rightExpression;
    }

    public void setRightExpression(Expression rightExpression) {
        this.rightExpression = rightExpression;
    }

    @SuppressWarnings("unchecked")
    public <T extends Expression> T getGenericRightExpression() {
        return (T) rightExpression;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "BinaryOperatorExpression{" + leftExpression.toString() + ' ' + operator + ' ' + rightExpression.toString() + "}";
    }
}
