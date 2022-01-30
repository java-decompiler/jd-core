/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.declaration;

import org.jd.core.v1.util.DefaultList;

public class FieldDeclarators extends DefaultList<FieldDeclarator> implements BaseFieldDeclarator {
    private static final long serialVersionUID = 1L;

    public FieldDeclarators(int capacity) {
        super(capacity);
    }

    public FieldDeclarators(FieldDeclarator declarator, FieldDeclarator... declarators) {
        super(declarator, declarators);
        if (declarators.length <= 0) {
            throw new IllegalArgumentException("Use 'FieldDeclarator' instead");
        }
    }

    @Override
    public void setFieldDeclaration(FieldDeclaration fieldDeclaration) {
        for (FieldDeclarator fieldDeclarator : this) {
            fieldDeclarator.setFieldDeclaration(fieldDeclaration);
        }
    }

    @Override
    public void accept(DeclarationVisitor visitor) {
        visitor.visit(this);
    }
}
