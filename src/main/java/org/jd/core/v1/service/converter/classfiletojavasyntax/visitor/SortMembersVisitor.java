/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BaseMemberDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileTypeDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.MergeMembersUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SortMembersVisitor extends AbstractJavaSyntaxVisitor {

    private Set<String> declaredTypes = new HashSet<>();

    private boolean isDeclared(ClassFileTypeDeclaration classFileTypeDeclaration) {
        return declaredTypes.contains(classFileTypeDeclaration.getInternalTypeName());
    }

    private void addDeclaredType(ClassFileTypeDeclaration classFileTypeDeclaration) {
        declaredTypes.add(classFileTypeDeclaration.getInternalTypeName());
    }

    public void init() {
        declaredTypes.clear();
    }
    
    @Override
    public void visit(AnnotationDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;
        List<ClassFileTypeDeclaration> innerTypes = bodyDeclaration.getInnerTypeDeclarations();
        if (innerTypes != null) {
            innerTypes.removeIf(this::isDeclared);
            innerTypes.forEach(this::addDeclaredType);
        }
        // Merge fields, getters & inner types
        BaseMemberDeclaration members = MergeMembersUtil.merge(bodyDeclaration.getFieldDeclarations(), bodyDeclaration.getMethodDeclarations(), innerTypes);
        bodyDeclaration.setMemberDeclarations(members);
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }
}
