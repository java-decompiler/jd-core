/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

public class GenericType implements Type {
    protected String name;
    protected int  dimension;

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
        assert dimension >= 0;
        if (this.dimension == dimension)
            return this;
        else
            return new GenericType(name, dimension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericType that = (GenericType) o;

        if (dimension != that.dimension) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 991890290 + name.hashCode();
        result = 31 * result + dimension;
        return result;
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
    public boolean isTypeArgumentAssignableFrom(BaseTypeArgument typeArgument) {
        Class typeArgumentClass = typeArgument.getClass();

        if (typeArgumentClass == GenericType.class) {
            return true;
        } else if (typeArgument instanceof Type) {
            return false;
        } else if (typeArgumentClass == WildcardTypeArgument.class) {
            return false;
        }

        return false;
    }

    @Override
    public boolean isGeneric() {
        return true;
    }

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
