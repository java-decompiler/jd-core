/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.declaration;

import org.jd.core.v1.util.DefaultList;

import java.util.Collection;

public class TypeDeclarations<T extends TypeDeclaration> extends DefaultList<T> implements BaseTypeDeclaration {
    public TypeDeclarations(Collection<T> collection) {
        super(collection);
        assert (collection != null) && (collection.size() > 1) : "Uses 'TypeDeclaration' or sub class instead";
    }

    @Override
    public void accept(DeclarationVisitor visitor) {
        visitor.visit(this);
    }
}
