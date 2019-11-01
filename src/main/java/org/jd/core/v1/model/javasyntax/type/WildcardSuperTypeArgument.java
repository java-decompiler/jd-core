/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

public class WildcardSuperTypeArgument implements TypeArgument {
    protected Type type;

    public WildcardSuperTypeArgument(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public void accept(TypeArgumentVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean isTypeArgumentAssignableFrom(BaseTypeArgument typeArgument) {
        if (typeArgument.getClass() == WildcardSuperTypeArgument.class) {
            return type.isTypeArgumentAssignableFrom(((WildcardSuperTypeArgument)typeArgument).getType());
        } else if (typeArgument instanceof Type) {
            return type.isTypeArgumentAssignableFrom(typeArgument);
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WildcardSuperTypeArgument that = (WildcardSuperTypeArgument) o;

        return type != null ? type.equals(that.type) : that.type == null;
    }

    @Override
    public int hashCode() {
        return 979510081 + (type != null ? type.hashCode() : 0);
    }

    @Override
    public String toString() {
        return "WildcardSuperTypeArgument{? super " + type + "}";
    }
}
