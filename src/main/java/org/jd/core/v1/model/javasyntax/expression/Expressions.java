/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.util.DefaultList;

import java.util.Collection;

public class Expressions<E extends Expression> extends DefaultList<E> implements BaseExpression {

    public Expressions() {}

    public Expressions(int capacity) {
        super(capacity);
    }

    public Expressions(Collection<E> collection) {
        super(collection);
        assert (collection != null) && (collection.size() > 1) : "Uses 'Expression' or sub class instead";
    }

    @SuppressWarnings("unchecked")
    public Expressions(E expression, E... expressions) {
        super(expression, expressions);
        assert (expressions != null) && (expressions.length > 0) : "Uses 'Expression' or sub class instead";
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
