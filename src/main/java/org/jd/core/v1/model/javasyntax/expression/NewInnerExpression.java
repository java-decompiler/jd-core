/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.ObjectType;

public class NewInnerExpression extends NewExpression {
    protected Expression expression;

    public NewInnerExpression(int lineNumber, BaseTypeArgument nonWildcardTypeArguments, ObjectType type, String descriptor, BaseExpression parameters, BodyDeclaration bodyDeclaration, Expression expression) {
        super(lineNumber, nonWildcardTypeArguments, type, descriptor, parameters, bodyDeclaration);
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
