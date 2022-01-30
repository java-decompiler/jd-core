/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import static org.apache.bcel.Const.MAJOR_1_5;

public class UpdateJavaSyntaxTreeStep0Visitor extends AbstractJavaSyntaxVisitor {
    private final UpdateOuterFieldTypeVisitor updateOuterFieldTypeVisitor;
    private final UpdateBridgeMethodTypeVisitor updateBridgeMethodTypeVisitor;

    public UpdateJavaSyntaxTreeStep0Visitor(TypeMaker typeMaker) {
        this.updateOuterFieldTypeVisitor = new UpdateOuterFieldTypeVisitor(typeMaker);
        this.updateBridgeMethodTypeVisitor = new UpdateBridgeMethodTypeVisitor(typeMaker);
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;
        boolean genericTypesSupported = bodyDeclaration.getClassFile().getMajorVersion() >= MAJOR_1_5;

        if (genericTypesSupported) {
            updateOuterFieldTypeVisitor.safeAcceptListDeclaration(bodyDeclaration.getInnerTypeDeclarations());
            updateBridgeMethodTypeVisitor.visit(declaration);
        }
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override public void visit(AnnotationDeclaration declaration) {}
    @Override public void visit(EnumDeclaration declaration) {}
}
