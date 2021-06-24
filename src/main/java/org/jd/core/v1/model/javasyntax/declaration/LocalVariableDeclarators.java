/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.declaration;

import org.jd.core.v1.util.DefaultList;

import java.util.Collection;

public class LocalVariableDeclarators extends DefaultList<LocalVariableDeclarator> implements BaseLocalVariableDeclarator {
    private static final long serialVersionUID = 1L;

    public LocalVariableDeclarators(int capacity) {
        super(capacity);
    }

    public LocalVariableDeclarators(Collection<LocalVariableDeclarator> collection) {
        super(collection);
        if (collection.size() <= 1) {
            throw new IllegalArgumentException("Use 'LocalVariableDeclarator' instead");
        }
    }

    @Override
    public int getLineNumber() {
        return isEmpty() ? 0 : get(0).getLineNumber();
    }

    @Override
    public void accept(DeclarationVisitor visitor) {
        visitor.visit(this);
    }
}
