/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.type.*;

import java.util.Map;

public class PopulateBindingsWithTypeParameterVisitor implements TypeParameterVisitor {
    protected Map<String, TypeArgument> bindings = null;

    public void init(Map<String, TypeArgument> bindings) {
        this.bindings = bindings;
    }

    @Override
    public void visit(TypeParameter type) {
        bindings.put(type.getIdentifier(), null);
    }

    @Override
    public void visit(TypeParameterWithTypeBounds type) {
        bindings.put(type.getIdentifier(), null);
    }

    @Override
    public void visit(TypeParameters types) {
        for (TypeParameter typeParameter : types) {
            typeParameter.accept(this);
        }
    }
}
