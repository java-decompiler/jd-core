/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.apache.bcel.Const;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.TypeDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileEnumDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

public class UpdateJavaSyntaxTreeStep2Visitor extends AbstractJavaSyntaxVisitor {
    protected static final AggregateFieldsVisitor AGGREGATE_FIELDS_VISITOR = new AggregateFieldsVisitor();
    protected static final SortMembersVisitor SORT_MEMBERS_VISITOR = new SortMembersVisitor();
    protected static final AutoboxingVisitor AUTOBOXING_VISITOR = new AutoboxingVisitor();

    private final InitStaticFieldVisitor initStaticFieldVisitor = new InitStaticFieldVisitor();
    private final InitInstanceFieldVisitor initInstanceFieldVisitor = new InitInstanceFieldVisitor();
    private final InitEnumVisitor initEnumVisitor = new InitEnumVisitor();
    private final RemoveDefaultConstructorVisitor removeDefaultConstructorVisitor = new RemoveDefaultConstructorVisitor();

    private final UpdateBridgeMethodVisitor replaceBridgeMethodVisitor;
    private final InitInnerClassVisitor.UpdateNewExpressionVisitor initInnerClassStep2Visitor;
    private final AddCastExpressionVisitor addCastExpressionVisitor;

    private TypeDeclaration typeDeclaration;

    public UpdateJavaSyntaxTreeStep2Visitor(TypeMaker typeMaker) {
        this.replaceBridgeMethodVisitor = new UpdateBridgeMethodVisitor(typeMaker);
        this.initInnerClassStep2Visitor = new InitInnerClassVisitor.UpdateNewExpressionVisitor(typeMaker);
        this.addCastExpressionVisitor = new AddCastExpressionVisitor(typeMaker);
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        // Visit inner types
        if (bodyDeclaration.hasInnerTypeDeclarations()) {
            TypeDeclaration td = typeDeclaration;
            acceptListDeclaration(bodyDeclaration.getInnerTypeDeclarations());
            typeDeclaration = td;
        }

        // Init bindTypeArgumentVisitor
        initStaticFieldVisitor.setInternalTypeName(typeDeclaration.getInternalTypeName());

        // Visit declaration
        initInnerClassStep2Visitor.visit(declaration);
        initStaticFieldVisitor.visit(declaration);
        initInstanceFieldVisitor.visit(declaration);
        removeDefaultConstructorVisitor.visit(declaration);
        AGGREGATE_FIELDS_VISITOR.visit(declaration);
        SORT_MEMBERS_VISITOR.visit(declaration);

        if (bodyDeclaration.isMainBodyDeclaration()) {
            if (bodyDeclaration.hasInnerTypeDeclarations() && replaceBridgeMethodVisitor.init(bodyDeclaration)) {
                // Replace bridge method invocation
                replaceBridgeMethodVisitor.visit(bodyDeclaration);
            }
            // Add cast expressions
            addCastExpressionVisitor.visit(declaration);
            // Autoboxing
            AUTOBOXING_VISITOR.visit(declaration);
        }
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        this.typeDeclaration = declaration;
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        this.typeDeclaration = declaration;
        addCastExpressionVisitor.pushContext(declaration);
        safeAccept(declaration.getBodyDeclaration());
        addCastExpressionVisitor.popContext(declaration);
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        this.typeDeclaration = declaration;
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        this.typeDeclaration = declaration;

        // Remove 'static', 'final' and 'abstract' flags
        ClassFileEnumDeclaration cfed = (ClassFileEnumDeclaration)declaration;

        cfed.setFlags(cfed.getFlags() & ~(Const.ACC_STATIC|Const.ACC_FINAL|Const.ACC_ABSTRACT));
        cfed.getBodyDeclaration().accept(this);
        initEnumVisitor.visit(cfed.getBodyDeclaration());
        cfed.setConstants(initEnumVisitor.getConstants());
    }
    
    @Override
    public void visit(CompilationUnit compilationUnit) {
        SORT_MEMBERS_VISITOR.init();
        super.visit(compilationUnit);
    }
}
