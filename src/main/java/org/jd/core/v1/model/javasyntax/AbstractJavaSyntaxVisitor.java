/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax;

import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.reference.*;
import org.jd.core.v1.model.javasyntax.statement.*;
import org.jd.core.v1.model.javasyntax.type.AbstractTypeVisitor;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;

import java.util.List;

public abstract class AbstractJavaSyntaxVisitor extends AbstractTypeVisitor implements DeclarationVisitor, ExpressionVisitor, ReferenceVisitor, StatementVisitor {
    public void visit(CompilationUnit compilationUnit) {
        compilationUnit.getTypeDeclarations().accept(this);
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        safeAccept(declaration.getAnnotationDeclarators());
        safeAccept(declaration.getBodyDeclaration());
        visit((TypeDeclaration) declaration);
    }

    @Override
    public void visit(ArrayVariableInitializer declaration) {
        acceptListDeclaration(declaration);
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        safeAccept(declaration.getMemberDeclarations());
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        safeAccept(declaration.getSuperType());
        visit((InterfaceDeclaration) declaration);
    }

    @Override public void visit(CommentStatement statement) {}

    @Override public void visit(CommentExpression expression) {}

    @Override
    public void visit(ConstructorInvocationExpression expression) {
        safeAccept(expression.getParameters());
        expression.getType().accept(this);
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getFormalParameters());
        safeAccept(declaration.getExceptions());
        safeAccept(declaration.getStatements());
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        visit((TypeDeclaration) declaration);
        safeAccept(declaration.getInterfaces());
        safeAcceptListDeclaration(declaration.getConstants());
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration.Constant declaration) {
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getArguments());
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(Expressions list) {
        acceptListExpression(list);
    }

    @Override
    public void visit(ExpressionVariableInitializer declaration) {
        safeAccept(declaration.getExpression());
    }

    @Override
    public void visit(FieldDeclaration declaration) {
        safeAccept(declaration.getAnnotationReferences());
        declaration.getType().accept(this);
        declaration.getFieldDeclarators().accept(this);
    }

    @Override
    public void visit(FieldDeclarator declaration) {
        safeAccept(declaration.getVariableInitializer());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(FieldDeclarators list) {
        acceptListDeclaration((List<? extends Declaration>)list);
    }

    @Override
    public void visit(FormalParameter declaration) {
        safeAccept(declaration.getAnnotationReferences());
        declaration.getType().accept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(FormalParameters list) {
        acceptListDeclaration(list);
    }

    @Override
    public void visit(InstanceInitializerDeclaration declaration) {
        safeAccept(declaration.getStatements());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        safeAccept(declaration.getInterfaces());
        safeAccept(declaration.getBodyDeclaration());
        visit((TypeDeclaration) declaration);
    }

    @Override
    public void visit(LocalVariableDeclaration declaration) {
        declaration.getType().accept(this);
        declaration.getLocalVariableDeclarators().accept(this);
    }

    @Override
    public void visit(LocalVariableDeclarator declarator) {
        safeAccept(declarator.getVariableInitializer());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(LocalVariableDeclarators declarators) {
        acceptListDeclaration(declarators);
    }

    @Override
    public void visit(MethodDeclaration declaration) {
        safeAccept(declaration.getAnnotationReferences());
        declaration.getReturnedType().accept(this);
        safeAccept(declaration.getFormalParameters());
        safeAccept(declaration.getExceptionTypes());
        safeAccept(declaration.getStatements());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(MemberDeclarations declarations) {
        acceptListDeclaration(declarations);
    }

    @Override
    public void visit(ModuleDeclaration declarations) {}

    @Override
    @SuppressWarnings("unchecked")
    public void visit(TypeDeclarations list) {
        acceptListDeclaration(list);
    }

    @Override
    public void visit(ArrayExpression expression) {
        expression.getType().accept(this);
        expression.getExpression().accept(this);
        expression.getIndex().accept(this);
    }

    @Override
    public void visit(BinaryOperatorExpression expression) {
        expression.getLeftExpression().accept(this);
        expression.getRightExpression().accept(this);
    }

    @Override
    public void visit(BooleanExpression expression) {}

    @Override
    public void visit(CastExpression expression) {
        expression.getType().accept(this);
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(ConstructorReferenceExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(DoubleConstantExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(EnumConstantReferenceExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(FieldReferenceExpression expression) {
        safeAccept(expression.getExpression());
        expression.getType().accept(this);
    }

    @Override
    public void visit(FloatConstantExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(IntegerConstantExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(InstanceOfExpression expression) {
        expression.getExpression().accept(this);
        expression.getType().accept(this);
    }

    @Override
    public void visit(LambdaFormalParametersExpression expression) {
        safeAccept(expression.getParameters());
        expression.getStatements().accept(this);
    }

    @Override
    public void visit(LambdaIdentifiersExpression expression) {
        safeAccept(expression.getStatements());
    }

    @Override public void visit(LengthExpression expression) {
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(LocalVariableReferenceExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(LongConstantExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(MethodInvocationExpression expression) {
        safeAccept(expression.getParameters());
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(MethodReferenceExpression expression) {
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(NewArray expression) {
        expression.getType().accept(this);
        safeAccept(expression.getDimensionExpressionList());
    }

    @Override
    public void visit(NewExpression expression) {
        safeAccept(expression.getNonWildcardTypeArguments());
        expression.getType().accept(this);
        safeAccept(expression.getParameters());
        // safeAccept(expression.getBodyDeclaration());
    }

    @Override
    public void visit(NewInitializedArray expression) {
        expression.getType().accept(this);
        safeAccept(expression.getArrayInitializer());
    }

    @Override
    public void visit(NewInnerExpression expression) {
        expression.getExpression().accept(this);
        safeAccept(expression.getNonWildcardTypeArguments());
        expression.getType().accept(this);
        safeAccept(expression.getParameters());
        //safeAccept(expression.getBodyDeclaration());
    }

    @Override
    public void visit(NullExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(TypeReferenceDotClassExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(ObjectTypeReferenceExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(ParenthesesExpression expression) {
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(PostOperatorExpression expression) {
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(PreOperatorExpression expression) {
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(StringConstantExpression expression) {}

    @Override
    public void visit(SuperConstructorInvocationExpression expression) {
        safeAccept(expression.getParameters());
        expression.getType().accept(this);
    }

    @Override
    public void visit(SuperExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(TernaryOperatorExpression expression) {
        expression.getCondition().accept(this);
        expression.getExpressionTrue().accept(this);
        expression.getExpressionFalse().accept(this);
    }

    @Override
    public void visit(ThisExpression expression) {
        expression.getType().accept(this);
    }

    @Override
    public void visit(AnnotationReference reference) {
        safeAccept(reference.getElementValue());
        safeAccept(reference.getElementValuePairs());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(AnnotationReferences list) {
        acceptListReference(list);
    }

    @Override
    public void visit(ExpressionElementValue reference) {
        reference.getExpression().accept(this);
    }

    @Override
    public void visit(ElementValueArrayInitializerElementValue reference) {
        safeAccept(reference.getElementValueArrayInitializer());
    }

    @Override
    public void visit(AnnotationElementValue reference) {
        safeAccept(reference.getElementValue());
        safeAccept(reference.getElementValuePairs());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(ElementValues list) {
        acceptListReference(list);
    }

    @Override
    public void visit(ElementValuePair reference) {
        reference.getElementValue().accept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(ElementValuePairs list) {
        acceptListReference(list);
    }

    @Override
    public void visit(ObjectReference reference) {
        visit((ObjectType) reference);
    }

    @Override public void visit(InnerObjectReference reference) {
        visit((InnerObjectType) reference);
    }

    @Override
    public void visit(AssertStatement statement) {
        statement.getCondition().accept(this);
        safeAccept(statement.getMessage());
    }

    @Override
    public void visit(BreakStatement statement) {}

    @Override public void visit(ByteCodeStatement statement) {}

    @Override
    public void visit(ContinueStatement statement) {}

    @Override
    public void visit(DoWhileStatement statement) {
        safeAccept(statement.getCondition());
        safeAccept(statement.getStatements());
    }

    @Override
    public void visit(ExpressionStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(ForEachStatement statement) {
        statement.getType().accept(this);
        statement.getExpression().accept(this);
        safeAccept(statement.getStatements());
    }

    @Override
    public void visit(ForStatement statement) {
        safeAccept(statement.getDeclaration());
        safeAccept(statement.getInit());
        safeAccept(statement.getCondition());
        safeAccept(statement.getUpdate());
        safeAccept(statement.getStatements());
    }

    @Override
    public void visit(IfStatement statement) {
        statement.getCondition().accept(this);
        safeAccept(statement.getStatements());
    }

    @Override
    public void visit(IfElseStatement statement) {
        statement.getCondition().accept(this);
        safeAccept(statement.getStatements());
        statement.getElseStatements().accept(this);
    }

    @Override
    public void visit(LabelStatement statement) {
        safeAccept(statement.getStatement());
    }

    @Override
    public void visit(LambdaExpressionStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(LocalVariableDeclarationStatement statement) {
        visit((LocalVariableDeclaration) statement);
    }

    @Override public void visit(ReturnExpressionStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(ReturnStatement statement) {}

    @Override
    @SuppressWarnings("unchecked")
    public void visit(Statements list) {
        acceptListStatement(list);
    }

    @Override
    public void visit(SwitchStatement statement) {
        statement.getCondition().accept(this);
        acceptListStatement(statement.getBlocks());
    }

    @Override
    public void visit(SwitchStatement.DefaultLabel statement) {}

    @Override
    public void visit(SwitchStatement.ExpressionLabel statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(SwitchStatement.LabelBlock statement) {
        statement.getLabel().accept(this);
        statement.getStatements().accept(this);
    }

    @Override
    public void visit(SwitchStatement.MultiLabelsBlock statement) {
        safeAcceptListStatement(statement.getLabels());
        statement.getStatements().accept(this);
    }

    @Override
    public void visit(SynchronizedStatement statement) {
        statement.getMonitor().accept(this);
        safeAccept(statement.getStatements());
    }

    @Override
    public void visit(ThrowStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(TryStatement statement) {
        safeAcceptListStatement(statement.getResources());
        statement.getTryStatements().accept(this);
        safeAcceptListStatement(statement.getCatchClauses());
        safeAccept(statement.getFinallyStatements());
    }

    @Override
    public void visit(TryStatement.CatchClause statement) {
        statement.getType().accept(this);
        safeAccept(statement.getStatements());
    }

    @Override
    public void visit(TryStatement.Resource statement) {
        statement.getType().accept(this);
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(StaticInitializerDeclaration declaration) {
        safeAccept(declaration.getStatements());
    }

    @Override
    public void visit(TypeDeclarationStatement statement) {
        statement.getTypeDeclaration().accept(this);
    }

    @Override
    public void visit(WhileStatement statement) {
        statement.getCondition().accept(this);
        safeAccept(statement.getStatements());
    }

    protected void visit(TypeDeclaration declaration) {
        safeAccept(declaration.getAnnotationReferences());
    }

    protected void acceptListDeclaration(List<? extends Declaration> list) {
        for (Declaration declaration : list)
            declaration.accept(this);
    }

    protected void acceptListExpression(List<? extends Expression> list) {
        for (Expression expression : list)
            expression.accept(this);
    }

    protected void acceptListReference(List<? extends Reference> list) {
        for (Reference reference : list)
            reference.accept(this);
    }

    protected void acceptListStatement(List<? extends Statement> list) {
        for (Statement statement : list)
            statement.accept(this);
    }

    protected void safeAccept(Declaration declaration) {
        if (declaration != null)
            declaration.accept(this);
    }

    protected void safeAccept(BaseExpression expression) {
        if (expression != null)
            expression.accept(this);
    }

    protected void safeAccept(Reference reference) {
        if (reference != null)
            reference.accept(this);
    }

    protected void safeAccept(BaseStatement list) {
        if (list != null)
            list.accept(this);
    }

    protected void safeAccept(BaseType list) {
        if (list != null)
            list.accept(this);
    }

    protected void safeAcceptListDeclaration(List<? extends Declaration> list) {
        if (list != null) {
            for (Declaration declaration : list)
                declaration.accept(this);
        }
    }

    protected void safeAcceptListStatement(List<? extends Statement> list) {
        if (list != null) {
            for (Statement statement : list)
                statement.accept(this);
        }
    }
}
