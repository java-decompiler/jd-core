/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable;

import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.Type;

public class GenericLocalVariable extends AbstractLocalVariable {
    protected GenericType type;

    public GenericLocalVariable(int index, int offset, GenericType type) {
        super(index, offset, null);
        this.type = type;
    }

    public GenericLocalVariable(int index, int offset, GenericType type, String name) {
        super(index, offset, name);
        this.type = type;
    }

    @Override
    public GenericType getType() {
        return type;
    }

    @Override
    public int getDimension() {
        return type.getDimension();
    }

    @Override
    public void accept(LocalVariableVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "GenericLocalVariable{" + type + ", index=" + index + "}";
    }

    @Override public boolean isAssignableFrom(Type otherType) { return true; }
    @Override public void typeOnRight(Type type) {}
    @Override public void typeOnLeft(Type type) {}

    @Override public boolean isAssignableFrom(AbstractLocalVariable variable) { return true; }
    @Override public void variableOnRight(AbstractLocalVariable variable) {}
    @Override public void variableOnLeft(AbstractLocalVariable variable) {}
}
