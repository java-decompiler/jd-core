/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration;

import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.ObjectType;

import java.util.Optional;

public class ClassFileClassDeclaration extends ClassDeclaration implements ClassFileTypeDeclaration {
    private final int firstLineNumber;

    public ClassFileClassDeclaration(BaseAnnotationReference annotationReferences, int flags, String internalName, String name, BaseTypeParameter typeParameters, ObjectType superType, BaseType interfaces, ClassFileBodyDeclaration bodyDeclaration) {
        super(annotationReferences, flags, internalName, name, typeParameters, superType, interfaces, bodyDeclaration);
        this.firstLineNumber = Optional.ofNullable(bodyDeclaration).map(ClassFileMemberDeclaration::getFirstLineNumber).orElse(0);
    }

    @Override
    public int getFirstLineNumber() {
        return firstLineNumber;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Override
    public String toString() {
        return "ClassFileClassDeclaration{" + internalTypeName + ", firstLineNumber=" + firstLineNumber + "}";
    }
}
