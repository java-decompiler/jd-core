/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileEnumDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ObjectTypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.SignatureParser;


public class UpdateJavaSyntaxTreeVisitor extends AbstractJavaSyntaxVisitor {
    protected static final AggregateFieldsVisitor AGGREGATE_FIELDS_VISITOR = new AggregateFieldsVisitor();
    protected static final SortMembersVisitor SORT_MEMBERS_VISITOR = new SortMembersVisitor();

    protected InitInnerClassVisitor initInnerClassVisitor = new InitInnerClassVisitor();
    protected InitStaticFieldVisitor initStaticFieldVisitor = new InitStaticFieldVisitor();
    protected InitInstanceFieldVisitor initInstanceFieldVisitor = new InitInstanceFieldVisitor();
    protected InitEnumVisitor initEnumVisitor = new InitEnumVisitor();
    protected UpdateBridgeMethodVisitor replaceBridgeMethodVisitor = new UpdateBridgeMethodVisitor();

    protected CreateInstructionsVisitor createInstructionsVisitor;
    protected RemoveDefaultConstructorVisitor removeDefaultConstructorVisitor;

    protected TypeDeclaration typeDeclaration;

    public UpdateJavaSyntaxTreeVisitor(ObjectTypeMaker objectTypeMaker, SignatureParser signatureParser) {
        createInstructionsVisitor = new CreateInstructionsVisitor(objectTypeMaker, signatureParser);
        removeDefaultConstructorVisitor = new RemoveDefaultConstructorVisitor(signatureParser);
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        this.typeDeclaration = declaration;
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        // Visit inner types
        if (bodyDeclaration.getInnerTypeDeclarations() != null) {
            TypeDeclaration td = typeDeclaration;
            acceptListDeclaration(bodyDeclaration.getInnerTypeDeclarations());
            typeDeclaration = td;
        }

        // Init visitor
        initStaticFieldVisitor.setInternalTypeName(typeDeclaration.getInternalName());

        // Visit declaration
        createInstructionsVisitor.visit(declaration);
        initInnerClassVisitor.visit(declaration);
        initStaticFieldVisitor.visit(declaration);
        initInstanceFieldVisitor.visit(declaration);
        removeDefaultConstructorVisitor.visit(declaration);
        AGGREGATE_FIELDS_VISITOR.visit(declaration);
        SORT_MEMBERS_VISITOR.visit(declaration);

        if ((bodyDeclaration.getOuterBodyDeclaration() == null) && (bodyDeclaration.getInnerTypeDeclarations() != null) && replaceBridgeMethodVisitor.init(bodyDeclaration)) {
            // Replace bridge method invocation
            replaceBridgeMethodVisitor.visit(bodyDeclaration);
        }
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        this.typeDeclaration = declaration;
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        ClassFileEnumDeclaration cfed = (ClassFileEnumDeclaration)declaration;

        this.typeDeclaration = declaration;
        // Remove 'static' and 'final' flags
        cfed.setFlags(cfed.getFlags() ^ (Declaration.FLAG_STATIC|Declaration.FLAG_FINAL));
        cfed.getBodyDeclaration().accept(this);
        initEnumVisitor.visit(cfed.getBodyDeclaration());
        cfed.setConstants(initEnumVisitor.getConstants());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        this.typeDeclaration = declaration;
        safeAccept(declaration.getBodyDeclaration());
    }
}
