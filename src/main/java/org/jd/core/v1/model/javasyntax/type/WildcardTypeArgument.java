/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

public class WildcardTypeArgument implements TypeArgument {
    public static final WildcardTypeArgument WILDCARD_TYPE_ARGUMENT = new WildcardTypeArgument();

    private WildcardTypeArgument() {}

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "Wildcard{?}";
    }

    @Override
    public boolean isTypeArgumentAssignableFrom(BaseTypeArgument typeArgument) {
        return true;
    }
}
