/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.type.AbstractTypeArgumentVisitor;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;

public class SearchInTypeArgumentVisitor extends AbstractTypeArgumentVisitor {
    private boolean genericFound;

    public SearchInTypeArgumentVisitor() {
        init();
    }

    public void init() {
        genericFound = false;
    }

    public boolean containsGeneric() {
        return genericFound;
    }

    @Override
    public void visit(WildcardTypeArgument type) {
    }

    @Override
    public void visit(GenericType type) {
        genericFound = true;
    }
}
