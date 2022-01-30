/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration;

import org.jd.core.v1.model.javasyntax.declaration.BaseFieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.type.Type;

public class ClassFileFieldDeclaration extends FieldDeclaration implements ClassFileMemberDeclaration {
    private int firstLineNumber;

    public ClassFileFieldDeclaration(int flags, Type type, BaseFieldDeclarator fieldDeclarators) {
        super(null, flags, type, fieldDeclarators);
    }

    public ClassFileFieldDeclaration(int flags, Type type, BaseFieldDeclarator fieldDeclarators, int firstLineNumber) {
        super(null, flags, type, fieldDeclarators);
        this.firstLineNumber = firstLineNumber;
    }

    public ClassFileFieldDeclaration(BaseAnnotationReference annotationReferences, int flags, Type type, BaseFieldDeclarator fieldDeclarators) {
        super(annotationReferences, flags, type, fieldDeclarators);
    }

    @Override
    public int getFirstLineNumber() {
        return firstLineNumber;
    }

    public void setFirstLineNumber(int firstLineNumber) {
        this.firstLineNumber = firstLineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }
        ClassFileFieldDeclaration that = (ClassFileFieldDeclaration) o;
        return firstLineNumber == that.firstLineNumber;
    }

    @Override
    public int hashCode() {
        int result = 65_247_265 + super.hashCode();
        return 31 * result + firstLineNumber;
    }

    @Override
    public String toString() {
        return "ClassFileFieldDeclaration{" + type + " " + fieldDeclarators + ", firstLineNumber=" + firstLineNumber + "}";
    }
}
