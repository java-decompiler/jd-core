/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable;

import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.Type;

public class GenericLocalVariable extends AbstractLocalVariable {
    protected GenericType type;

    public GenericLocalVariable(int index, int offset, GenericType type, String name) {
        super(index, offset, name, type.getDimension());
        this.type = type;
    }

    public GenericLocalVariable(int index, int offset, GenericType type) {
        super(index, offset, type.getDimension());
        this.type = type;
    }

    @Override
    public GenericType getType() {
        return type;
    }

    @Override public void rightReduce(AbstractLocalVariable other) {}
    @Override public void rightReduce(Type otherType) {}
    @Override public void leftReduce(AbstractLocalVariable other) {}
    @Override public void leftReduce(Type otherType) {}

    @Override
    public boolean isAssignable(AbstractLocalVariable other) {
        return true;
    }

    @Override
    public boolean isAssignable(Type otherType) {
        return true;
    }

    @Override
    public void accept(LocalVariableVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "GenericLocalVariable{" + type + ", index=" + index + "}";
    }
}
