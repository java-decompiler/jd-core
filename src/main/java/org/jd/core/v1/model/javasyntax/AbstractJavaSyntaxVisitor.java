/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax;

import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ArrayVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.Declaration;
import org.jd.core.v1.model.javasyntax.declaration.DeclarationVisitor;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarators;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameters;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarators;
import org.jd.core.v1.model.javasyntax.declaration.MemberDeclarations;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ModuleDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.StaticInitializerDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.TypeDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.TypeDeclarations;
import org.jd.core.v1.model.javasyntax.expression.ArrayExpression;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.CommentExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.EnumConstantReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.ExpressionVisitor;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaFormalParametersExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LengthExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NewArray;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.NewInitializedArray;
import org.jd.core.v1.model.javasyntax.expression.NoExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.ParenthesesExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.expression.TypeReferenceDotClassExpression;
import org.jd.core.v1.model.javasyntax.reference.AnnotationElementValue;
import org.jd.core.v1.model.javasyntax.reference.AnnotationReference;
import org.jd.core.v1.model.javasyntax.reference.AnnotationReferences;
import org.jd.core.v1.model.javasyntax.reference.ElementValueArrayInitializerElementValue;
import org.jd.core.v1.model.javasyntax.reference.ElementValuePair;
import org.jd.core.v1.model.javasyntax.reference.ElementValuePairs;
import org.jd.core.v1.model.javasyntax.reference.ElementValues;
import org.jd.core.v1.model.javasyntax.reference.ExpressionElementValue;
import org.jd.core.v1.model.javasyntax.reference.Reference;
import org.jd.core.v1.model.javasyntax.reference.ReferenceVisitor;
import org.jd.core.v1.model.javasyntax.statement.AssertStatement;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.CommentStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LabelStatement;
import org.jd.core.v1.model.javasyntax.statement.LambdaExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.NoStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.StatementVisitor;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.model.javasyntax.statement.SynchronizedStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.TypeDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.model.javasyntax.type.AbstractTypeArgumentVisitor;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeParameter;
import org.jd.core.v1.model.javasyntax.type.TypeParameterVisitor;
import org.jd.core.v1.model.javasyntax.type.TypeParameterWithTypeBounds;
import org.jd.core.v1.model.javasyntax.type.TypeParameters;
import org.jd.core.v1.model.javasyntax.type.TypeVisitor;
import org.jd.core.v1.model.javasyntax.type.Types;

import java.util.Iterator;
import java.util.List;

public abstract class AbstractJavaSyntaxVisitor extends AbstractTypeArgumentVisitor implements DeclarationVisitor, ExpressionVisitor, ReferenceVisitor, StatementVisitor, TypeVisitor, TypeParameterVisitor {
    public void visit(CompilationUnit compilationUnit) {
        compilationUnit.typeDeclarations().accept(this);
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        safeAccept(declaration.getAnnotationDeclarators());
        safeAccept(declaration.getBodyDeclaration());
        safeAccept(declaration.getAnnotationReferences());
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
        BaseType superType = declaration.getSuperType();

        if (superType != null) {
            superType.accept(this);
        }

        safeAccept(declaration.getTypeParameters());
        safeAccept(declaration.getInterfaces());
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(CommentStatement statement) {}

    @Override
    public void visit(CommentExpression expression) {}

    @Override
    public void visit(ConstructorInvocationExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
        safeAccept(expression.getParameters());
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getFormalParameters());
        safeAccept(declaration.getExceptionTypes());
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
        safeAccept(declaration.getArguments());
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(Expressions list) {
        acceptListExpression(list);
    }

    @Override
    public void visit(ExpressionVariableInitializer declaration) {
        declaration.getExpression().accept(this);
    }

    @Override
    public void visit(FieldDeclaration declaration) {
        BaseType type = declaration.getType();

        type.accept(this);
        safeAccept(declaration.getAnnotationReferences());
        declaration.getFieldDeclarators().accept(this);
    }

    @Override
    public void visit(FieldDeclarator declarator) {
        safeAccept(declarator.getVariableInitializer());
    }

    @Override
    public void visit(FieldDeclarators list) {
        acceptListDeclaration(list);
    }

    @Override
    public void visit(FormalParameter declaration) {
        BaseType type = declaration.getType();

        type.accept(this);
        safeAccept(declaration.getAnnotationReferences());
    }

    @Override
    public void visit(FormalParameters list) {
        acceptListDeclaration(list);
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        safeAccept(declaration.getInterfaces());
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(LocalVariableDeclaration declaration) {
        BaseType type = declaration.getType();

        type.accept(this);
        declaration.getLocalVariableDeclarators().accept(this);
    }

    @Override
    public void visit(LocalVariableDeclarator declarator) {
        safeAccept(declarator.getVariableInitializer());
    }

    @Override
    public void visit(LocalVariableDeclarators declarators) {
        acceptListDeclaration(declarators);
    }

    @Override
    public void visit(MethodDeclaration declaration) {
        BaseType returnedType = declaration.getReturnedType();

        returnedType.accept(this);
        safeAccept(declaration.getAnnotationReferences());
        safeAccept(declaration.getFormalParameters());
        safeAccept(declaration.getExceptionTypes());
        safeAccept(declaration.getStatements());
    }

    @Override
    public void visit(MemberDeclarations declarations) {
        acceptListDeclaration(declarations);
    }

    @Override
    public void visit(ModuleDeclaration declarations) {}

    @Override
    public void visit(TypeDeclarations list) {
        acceptListDeclaration(list);
    }

    @Override
    public void visit(ArrayExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
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
        BaseType type = expression.getType();

        type.accept(this);
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(ConstructorReferenceExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(DoubleConstantExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(EnumConstantReferenceExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(FieldReferenceExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
        safeAccept(expression.getExpression());
    }

    @Override
    public void visit(FloatConstantExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(IntegerConstantExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(InstanceOfExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(LambdaFormalParametersExpression expression) {
        safeAccept(expression.getFormalParameters());
        expression.getStatements().accept(this);
    }

    @Override
    public void visit(LambdaIdentifiersExpression expression) {
        safeAccept(expression.getStatements());
    }

    @Override
    public void visit(LengthExpression expression) {
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(LocalVariableReferenceExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(LongConstantExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(MethodInvocationExpression expression) {
        expression.getExpression().accept(this);
        safeAccept(expression.getNonWildcardTypeArguments());
        safeAccept(expression.getParameters());
    }

    @Override
    public void visit(MethodReferenceExpression expression) {
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(NewArray expression) {
        BaseType type = expression.getType();

        type.accept(this);
        safeAccept(expression.getDimensionExpressionList());
    }

    @Override
    public void visit(NewExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
        safeAccept(expression.getParameters());
        // safeAccept(expression.getBodyDeclaration());
    }

    @Override
    public void visit(NewInitializedArray expression) {
        BaseType type = expression.getType();

        type.accept(this);
        safeAccept(expression.getArrayInitializer());
    }

    @Override
    public void visit(NoExpression expression) {}

    @Override
    public void visit(NullExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(TypeReferenceDotClassExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(ObjectTypeReferenceExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
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
        BaseType type = expression.getType();

        type.accept(this);
        safeAccept(expression.getParameters());
    }

    @Override
    public void visit(SuperExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(TernaryOperatorExpression expression) {
        expression.getCondition().accept(this);
        expression.getTrueExpression().accept(this);
        expression.getFalseExpression().accept(this);
    }

    @Override
    public void visit(ThisExpression expression) {
        BaseType type = expression.getType();

        type.accept(this);
    }

    @Override
    public void visit(AnnotationReference reference) {
        safeAccept(reference.getElementValue());
        safeAccept(reference.getElementValuePairs());
    }

    @Override
    public void visit(AnnotationReferences<? extends AnnotationReference> list) {
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
    public void visit(ElementValues list) {
        acceptListReference(list);
    }

    @Override
    public void visit(ElementValuePair reference) {
        reference.elementValue().accept(this);
    }

    @Override
    public void visit(ElementValuePairs list) {
        acceptListReference(list);
    }

    @Override
    public void visit(AssertStatement statement) {
        statement.getCondition().accept(this);
        safeAccept(statement.getMessage());
    }

    @Override
    public void visit(BreakStatement statement) {}


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
        BaseType type = statement.getType();

        type.accept(this);
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
        safeAccept(statement.statement());
    }

    @Override
    public void visit(LambdaExpressionStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(LocalVariableDeclarationStatement statement) {
        visit((LocalVariableDeclaration) statement);
    }

    @Override
    public void visit(NoStatement statement) {}

    @Override
    public void visit(ReturnExpressionStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(ReturnStatement statement) {}

    @Override
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
        BaseType type = statement.getType();

        type.accept(this);
        safeAccept(statement.getStatements());
    }

    @Override
    public void visit(TryStatement.Resource statement) {
        BaseType type = statement.getType();

        type.accept(this);
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(StaticInitializerDeclaration declaration) {
        safeAccept(declaration.getStatements());
    }

    @Override
    public void visit(TypeDeclarationStatement statement) {
        statement.typeDeclaration().accept(this);
    }

    @Override
    public void visit(WhileStatement statement) {
        statement.getCondition().accept(this);
        safeAccept(statement.getStatements());
    }

    @Override
    public void visit(TypeParameter parameter) {}

    @Override
    public void visit(TypeParameterWithTypeBounds parameter) {
        parameter.getTypeBounds().accept(this);
    }

    @Override
    public void visit(TypeParameters parameters) {
        Iterator<TypeParameter> iterator = parameters.iterator();

        while (iterator.hasNext()) {
            iterator.next().accept(this);
        }
    }

    public void visit(TypeDeclaration declaration) {
        safeAccept(declaration.getAnnotationReferences());
    }

    @Override
    public void visit(Types types) {
        Iterator<Type> iterator = types.iterator();

        while (iterator.hasNext()) {
            BaseType type = iterator.next();

            type.accept(this);
        }
    }

    public void acceptListDeclaration(List<? extends Declaration> list) {
        for (Declaration declaration : list) {
            declaration.accept(this);
        }
    }

    public void acceptListExpression(List<? extends Expression> list) {
        for (Expression expression : list) {
            expression.accept(this);
        }
    }

    public void acceptListReference(List<? extends Reference> list) {
        for (Reference reference : list) {
            reference.accept(this);
        }
    }

    public void acceptListStatement(List<? extends Statement> list) {
        for (Statement statement : list) {
            statement.accept(this);
        }
    }

    public void safeAccept(Declaration declaration) {
        if (declaration != null) {
            declaration.accept(this);
        }
    }

    public void safeAccept(BaseExpression expression) {
        if (expression != null) {
            expression.accept(this);
        }
    }

    public void safeAccept(Reference reference) {
        if (reference != null) {
            reference.accept(this);
        }
    }

    public void safeAccept(BaseStatement list) {
        if (list != null) {
            list.accept(this);
        }
    }

    public void safeAccept(BaseType list) {
        if (list != null) {
            list.accept(this);
        }
    }

    public void safeAccept(BaseTypeParameter list) {
        if (list != null) {
            list.accept(this);
        }
    }

    public void safeAcceptListDeclaration(List<? extends Declaration> list) {
        if (list != null) {
            for (Declaration declaration : list) {
                declaration.accept(this);
            }
        }
    }

    public void safeAcceptListStatement(List<? extends Statement> list) {
        if (list != null) {
            for (Statement statement : list) {
                statement.accept(this);
            }
        }
    }
}
