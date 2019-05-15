/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

import java.util.Collection;

public class TypeBounds<T extends Type> extends Types implements TypeBoundList {
    public TypeBounds() {}

    public TypeBounds(int capacity) {
        super(capacity);
    }

    @SuppressWarnings("unchecked")
    public TypeBounds(Collection<T> collection) {
        super(collection);
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }
}
