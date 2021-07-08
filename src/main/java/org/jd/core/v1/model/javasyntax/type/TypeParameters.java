/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.type;

import org.jd.core.v1.util.DefaultList;

import java.util.Collection;

public class TypeParameters extends DefaultList<TypeParameter> implements BaseTypeParameter {
    private static final long serialVersionUID = 1L;

    public TypeParameters() {}

    public TypeParameters(int capacity) {
        super(capacity);
    }

    public TypeParameters(Collection<TypeParameter> collection) {
        super(collection);
        if (collection.size() <= 1) {
            throw new IllegalArgumentException("Use 'TypeParameter' instead");
        }
    }

    public TypeParameters(TypeParameter type, TypeParameter... types) {
        super(types.length + 1);
        if (types.length <= 0) {
            throw new IllegalArgumentException("Use 'TypeParameter' instead");
        }

        add(type);

        for (TypeParameter t : types) {
            add(t);
        }
    }

    @Override
    public void accept(TypeParameterVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(get(0));

        for (int i=1; i<size(); i++) {
            sb.append(" & ");
            sb.append(get(i));
        }

        return sb.toString();
    }
}
