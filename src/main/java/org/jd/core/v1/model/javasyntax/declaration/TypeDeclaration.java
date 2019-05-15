/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.declaration;

import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;

public abstract class TypeDeclaration implements BaseTypeDeclaration, MemberDeclaration {
    protected BaseAnnotationReference annotationReferences;
    protected int flags;
    protected String internalName;
    protected String name;

    protected TypeDeclaration(BaseAnnotationReference annotationReferences, int flags, String internalName, String name) {
        this.annotationReferences = annotationReferences;
        this.flags = flags;
        this.internalName = internalName;
        this.name = name;
    }

    public BaseAnnotationReference getAnnotationReferences() {
        return annotationReferences;
    }

    public int getFlags() {
        return flags;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getName() {
        return name;
    }
}
