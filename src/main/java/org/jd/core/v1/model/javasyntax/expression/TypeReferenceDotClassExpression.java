/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;

public class TypeReferenceDotClassExpression implements Expression {
    private final int lineNumber;
    private final Type typeDotClass;
    private final Type type;

    public TypeReferenceDotClassExpression(Type typeDotClass) {
        this(0, typeDotClass);
    }

    public TypeReferenceDotClassExpression(int lineNumber, Type typeDotClass) {
        this.lineNumber = lineNumber;
        this.typeDotClass = typeDotClass;
        this.type = ObjectType.TYPE_CLASS.createType(typeDotClass);
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    public Type getTypeDotClass() {
        return typeDotClass;
    }

    @Override
    public Type getType() {
        return type;
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
        return "TypeReferenceDotClassExpression{" + typeDotClass + "}";
    }

    @Override
    public Expression copyTo(int lineNumber) {
        return new TypeReferenceDotClassExpression(lineNumber, typeDotClass);
    }
}
