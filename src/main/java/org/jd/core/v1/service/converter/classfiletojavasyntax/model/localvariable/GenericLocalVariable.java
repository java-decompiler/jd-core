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
        StringBuilder sb = new StringBuilder();

        sb.append("GenericLocalVariable{");
        sb.append(type.getName());

        if (type.getDimension() > 0) {
            sb.append(new String(new char[type.getDimension()]).replaceAll("\0", "[]"));
        }

        sb.append(' ').append(name).append(", index=").append(index);

        if (next != null) {
            sb.append(", next=").append(next);
        }

        return sb.append("}").toString();
    }

    @Override public boolean isAssignableFrom(Type otherType) {
        return type.equals(otherType);
    }
    @Override public void typeOnRight(Type type) {}
    @Override public void typeOnLeft(Type type) {}

    @Override public boolean isAssignableFrom(AbstractLocalVariable variable) { return isAssignableFrom(variable.getType()); }
    @Override public void variableOnRight(AbstractLocalVariable variable) {}
    @Override public void variableOnLeft(AbstractLocalVariable variable) {}
}
