/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;

public record CommentExpression(String text) implements Expression {

    @Override
    public int getLineNumber() {
        return UNKNOWN_LINE_NUMBER;
    }

    @Override
    public Type getType() {
        return PrimitiveType.TYPE_VOID;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "CommentExpression{" + text + "}";
    }

    @Override
    public Expression copyTo(int lineNumber) {
        return new CommentExpression(text);
    }
}
