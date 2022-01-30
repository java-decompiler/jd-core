/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.type;

import java.util.Map;

public class GenericType implements Type {
    private final String name;
    private final int  dimension;

    public GenericType(String name) {
        this.name = name;
        this.dimension = 0;
    }

    public GenericType(String name, int dimension) {
        this.name = name;
        this.dimension = dimension;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescriptor() {
        return name;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public Type createType(int dimension) {
        if (dimension < 0) {
            throw new IllegalArgumentException("GenericType.createType(dim) : create type with negative dimension");
        }
        if (this.dimension == dimension) {
            return this;
        }
        return new GenericType(name, dimension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GenericType that = (GenericType) o;

        return dimension == that.dimension && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = 991_890_290 + name.hashCode();
        return 31 * result + dimension;
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(TypeArgumentVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean isTypeArgumentAssignableFrom(Map<String, BaseType> typeBounds, BaseTypeArgument typeArgument) {
        return equals(typeArgument);
    }

    @Override
    public boolean isGenericType() {
        return true;
    }

    @Override
    public boolean isGenericTypeArgument() { return true; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GenericType{");

        sb.append(name);

        if (dimension > 0) {
            sb.append(", dimension=").append(dimension);
        }

        return sb.append('}').toString();
    }
}
