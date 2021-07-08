/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.declaration;

import org.jd.core.v1.util.DefaultList;

import java.util.Collection;

public class MemberDeclarations extends DefaultList<MemberDeclaration> implements BaseMemberDeclaration {
    private static final long serialVersionUID = 1L;

    public MemberDeclarations(int capacity) {
        super(capacity);
    }

    public MemberDeclarations(Collection<MemberDeclaration> collection) {
        super(collection);
        if (collection.size() <= 1) {
            throw new IllegalArgumentException("Use 'MemberDeclaration' implementation instead");
        }
    }

    public MemberDeclarations(MemberDeclaration declaration, MemberDeclaration... declarations) {
        super(declaration, declarations);
        if (declarations.length <= 0) {
            throw new IllegalArgumentException("Use 'MemberDeclaration' implementation instead");
        }
    }

    @Override
    public void accept(DeclarationVisitor visitor) {
        visitor.visit(this);
    }
}
