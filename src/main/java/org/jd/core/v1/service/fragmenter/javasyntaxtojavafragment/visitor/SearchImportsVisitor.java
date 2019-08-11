/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.visitor;

import org.jd.core.v1.model.javafragment.ImportsFragment;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.reference.AnnotationElementValue;
import org.jd.core.v1.model.javasyntax.reference.AnnotationReference;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.util.JavaFragmentFactory;

import java.util.HashSet;

public class SearchImportsVisitor extends AbstractJavaSyntaxVisitor {
    protected String internalPackagePrefix;
    protected ImportsFragment importsFragment = JavaFragmentFactory.newImportsFragment();
    protected int maxLineNumber = 0;
    protected HashSet<String> internalTypeNames = new HashSet<>();

    public SearchImportsVisitor(String mainInternalName) {
        int index = mainInternalName.lastIndexOf('/');
        this.internalPackagePrefix = (index == -1) ? "" : mainInternalName.substring(0, index + 1);
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getAnnotationDeclarators());
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(AnnotationReference reference) {
        super.visit(reference);
        add(reference.getType());
    }

    @Override
    public void visit(AnnotationElementValue reference) {
        super.visit(reference);
        add(reference.getType());
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        safeAccept(declaration.getSuperType());
        safeAccept(declaration.getTypeParameters());
        safeAccept(declaration.getInterfaces());
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        if (!internalTypeNames.contains(declaration.getInternalTypeName())) {
            internalTypeNames.add(declaration.getInternalTypeName());
            safeAccept(declaration.getMemberDeclarations());
        }
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getFormalParameters());
        safeAccept(declaration.getExceptions());
        safeAccept(declaration.getStatements());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        safeAccept(declaration.getTypeParameters());
        safeAccept(declaration.getInterfaces());
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getBodyDeclaration());
    }

    public ImportsFragment getImportsFragment() {
        importsFragment.initLineCounts();
        return importsFragment;
    }

    public int getMaxLineNumber() {
        return maxLineNumber;
    }

    @Override
    public void visit(FieldDeclaration declaration) {
        safeAccept(declaration.getAnnotationReferences());
        declaration.getType().accept(this);
        declaration.getFieldDeclarators().accept(this);
    }

    @Override
    public void visit(ObjectType type) {
        add(type);
        safeAccept(type.getTypeArguments());
    }

    @Override
    public void visit(ArrayExpression expression) {
        if (maxLineNumber < expression.getLineNumber())
            maxLineNumber = expression.getLineNumber();
        expression.getExpression().accept(this);
        expression.getIndex().accept(this);
    }

    @Override
    public void visit(BinaryOperatorExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(BooleanExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(CastExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(ConstructorInvocationExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(ConstructorReferenceExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(DoubleConstantExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        safeAccept(declaration.getInterfaces());
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration.Constant declaration) {
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getArguments());
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(FieldReferenceExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        safeAccept(expression.getExpression());
    }

    @Override
    public void visit(FloatConstantExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
    }

    @Override
    public void visit(IntegerConstantExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
    }

    @Override
    public void visit(InstanceOfExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(LambdaFormalParametersExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(LambdaIdentifiersExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(LengthExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(LocalVariableReferenceExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(LongConstantExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
    }

    @Override
    public void visit(MethodInvocationExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(MethodReferenceExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(NewArray expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        safeAccept(expression.getDimensionExpressionList());
    }

    @Override
    public void visit(NewExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        safeAccept(expression.getNonWildcardTypeArguments());
        expression.getType().accept(this);
        safeAccept(expression.getParameters());
        safeAccept(expression.getBodyDeclaration());
    }

    @Override
    public void visit(NewInitializedArray expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(NewInnerExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        expression.getExpression().accept(this);
        safeAccept(expression.getNonWildcardTypeArguments());
        expression.getType().accept(this);
        safeAccept(expression.getParameters());
        safeAccept(expression.getBodyDeclaration());
    }

    @Override
    public void visit(NullExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
    }

    @Override
    public void visit(ObjectTypeReferenceExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(ParenthesesExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(PostOperatorExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(PreOperatorExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(StringConstantExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(SuperExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(TernaryOperatorExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override
    public void visit(ThisExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    @Override public void visit(TypeReferenceDotClassExpression expression) {
        if (maxLineNumber < expression.getLineNumber()) maxLineNumber = expression.getLineNumber();
        super.visit(expression);
    }

    protected void add(ObjectType type) {
        String descriptor = type.getDescriptor();

        if (descriptor.charAt(descriptor.length()-1) == ';') {
            String internalName = type.getInternalName();
            String qualifiedName = type.getQualifiedName();

            if (internalName.startsWith("java/lang/")) {
                if (internalName.indexOf('/', 10) != -1) { // 10 = "java/lang/".length()
                    importsFragment.addImport(internalName, qualifiedName);
                }
            } else if (internalName.startsWith(internalPackagePrefix)) {
                if (internalName.indexOf('/', internalPackagePrefix.length()) != -1) {
                    importsFragment.addImport(internalName, qualifiedName);
                }
            } else {
                importsFragment.addImport(internalName, qualifiedName);
            }
        }
    }
}
