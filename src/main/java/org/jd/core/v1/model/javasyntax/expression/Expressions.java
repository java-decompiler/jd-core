/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.util.DefaultList;

import java.util.Collection;

public class Expressions extends DefaultList<Expression> implements BaseExpression {
    private static final long serialVersionUID = 1L;

    public Expressions() {}

    public Expressions(int capacity) {
        super(capacity);
    }

    public Expressions(Collection<Expression> collection) {
        super(collection);
        if (collection.size() <= 1) {
            throw new IllegalArgumentException("Use 'Expression' or sub class instead");
        }
    }

    public Expressions(Expression expression, Expression... expressions) {
        super(expression, expressions);
        if (expressions.length <= 0) {
            throw new IllegalArgumentException("Use 'Expression' or sub class instead");
        }
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
