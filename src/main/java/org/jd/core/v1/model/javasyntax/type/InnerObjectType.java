/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.type;

public class InnerObjectType extends ObjectType {
    private final ObjectType outerType;

    public InnerObjectType(String internalName, String qualifiedName, String name, ObjectType outerType) {
        super(internalName, qualifiedName, name);
        this.outerType = outerType;
        checkArguments(qualifiedName, name);
    }

    public InnerObjectType(String internalName, String qualifiedName, String name, BaseTypeArgument typeArguments, ObjectType outerType) {
        super(internalName, qualifiedName, name, typeArguments);
        this.outerType = outerType;
        checkArguments(qualifiedName, name);
    }

    public InnerObjectType(String internalName, String qualifiedName, String name, BaseTypeArgument typeArguments, int dimension, ObjectType outerType) {
        super(internalName, qualifiedName, name, typeArguments, dimension);
        this.outerType = outerType;
        checkArguments(qualifiedName, name);
    }

    protected void checkArguments(String qualifiedName, String name) {
        if (name != null && Character.isDigit(name.charAt(0)) && qualifiedName != null) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public ObjectType getOuterType() {
        return outerType;
    }

    @Override
    public Type createType(int dimension) {
        if (dimension < 0) {
            throw new IllegalArgumentException("InnerObjectType.createType(dim) : create type with negative dimension");
        }
        return new InnerObjectType(internalName, qualifiedName, name, typeArguments, dimension, outerType);
    }

    @Override
    public ObjectType createType(BaseTypeArgument typeArguments) {
        return new InnerObjectType(internalName, qualifiedName, name, typeArguments, dimension, outerType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }
        InnerObjectType that = (InnerObjectType) o;
        return outerType.equals(that.outerType);
    }

    @Override
    public int hashCode() {
        int result = 111_476_860 + super.hashCode();
        return 31 * result + outerType.hashCode();
    }

    @Override
    public boolean isInnerObjectType() {
        return true;
    }

    @Override
    public boolean isInnerObjectTypeArgument() {
        return true;
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
    public String toString() {
        if (typeArguments == null) {
            return "InnerObjectType{" + outerType + "." + descriptor + "}";
        }
        return "InnerObjectType{" + outerType + "." + descriptor + "<" + typeArguments + ">}";
    }
}
