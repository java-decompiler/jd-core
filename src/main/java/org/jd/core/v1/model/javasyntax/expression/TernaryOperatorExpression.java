/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.type.Type;

public class TernaryOperatorExpression extends AbstractLineNumberTypeExpression {
    protected Expression condition;
    protected Expression expressionTrue;
    protected Expression expressionFalse;

    public TernaryOperatorExpression(Expression condition, Expression expressionTrue, Expression expressionFalse) {
        super(getType(expressionTrue, expressionFalse));
        this.condition = condition;
        this.expressionTrue = expressionTrue;
        this.expressionFalse = expressionFalse;
    }

    public TernaryOperatorExpression(Type type, Expression condition, Expression expressionTrue, Expression expressionFalse) {
        super(type);
        this.condition = condition;
        this.expressionTrue = expressionTrue;
        this.expressionFalse = expressionFalse;
    }

    public TernaryOperatorExpression(int lineNumber, Expression condition, Expression expressionTrue, Expression expressionFalse) {
        super(lineNumber, getType(expressionTrue, expressionFalse));
        this.condition = condition;
        this.expressionTrue = expressionTrue;
        this.expressionFalse = expressionFalse;
    }

    public TernaryOperatorExpression(int lineNumber, Type type, Expression condition, Expression expressionTrue, Expression expressionFalse) {
        super(lineNumber, type);
        this.condition = condition;
        this.expressionTrue = expressionTrue;
        this.expressionFalse = expressionFalse;
    }

    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    public Expression getExpressionTrue() {
        return expressionTrue;
    }

    public void setExpressionTrue(Expression expressionTrue) {
        this.expressionTrue = expressionTrue;
    }

    public Expression getExpressionFalse() {
        return expressionFalse;
    }

    public void setExpressionFalse(Expression expressionFalse) {
        this.expressionFalse = expressionFalse;
    }

    @Override
    public int getPriority() {
        return 15;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "TernaryOperatorExpression{" + condition + " ? " + expressionTrue + " : " + expressionFalse + "}";
    }

    protected static Type getType(Expression expressionTrue, Expression expressionFalse) {
        if (expressionTrue instanceof NullExpression) {
            return expressionFalse.getType();
        } else {
            return expressionTrue.getType();
        }
    }
}
