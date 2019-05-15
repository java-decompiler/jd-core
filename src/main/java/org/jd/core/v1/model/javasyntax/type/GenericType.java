/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

public class GenericType implements Type {
    protected String identifier;
    protected int  dimension;

    public GenericType(String identifier, int dimension) {
        this.identifier = identifier;
        this.dimension = dimension;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getDescriptor() {
        return identifier;
    }

    @Override
    public Type createType(int dimension) {
        assert dimension >= 0;
        if (this.dimension == dimension)
            return this;
        else
            return new GenericType(identifier, dimension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GenericType that = (GenericType) o;

        if (dimension != that.dimension) return false;
        if (!identifier.equals(that.identifier)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = identifier.hashCode();
        result = 31 * result + dimension;
        return result;
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean isGeneric() {
        return true;
    }

    @Override
    public String toString() {
        return "GenericType{" + identifier + "}";
    }
}
