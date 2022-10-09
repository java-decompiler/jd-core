/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement.CatchClause;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.util.DefaultList;

import java.util.Iterator;
import java.util.List;

public final class TryWithResourcesStatementMaker {

    private TryWithResourcesStatementMaker() {
        super();
    }

    public static Statement makeLegacy(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements,
            DefaultList<TryStatement.CatchClause> catchClauses, Statements finallyStatements) {
        int size = statements.size();

        if (size < 2 || finallyStatements == null || finallyStatements.size() != 1 || !checkThrowable(catchClauses)) {
            return null;
        }

        Statement statement = statements.get(size - 2);

        if (!statement.isExpressionStatement()) {
            return null;
        }

        Expression expression = statement.getExpression();

        if (!expression.isBinaryOperatorExpression()) {
            return null;
        }

        Expression boe = expression;

        expression = boe.getLeftExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable lv1 = ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable();

        statement = statements.get(size - 1);

        if (!statement.isExpressionStatement()) {
            return null;
        }

        expression = statement.getExpression();

        if (!expression.isBinaryOperatorExpression()) {
            return null;
        }

        expression = expression.getLeftExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable lv2 = ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable();

        statement = finallyStatements.getFirst();

        if (statement.isIfStatement() && lv1 == getLocalVariable(statement.getCondition())) {
            statement = statement.getStatements().getFirst();

            if (statement.isIfElseStatement()) {
                return parsePatternAddSuppressed(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2, statement);
            }
            if (statement.isExpressionStatement()) {
                return parsePatternCloseResource(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2, statement);
            }
        }

        if (statement.isExpressionStatement()) {
            return parsePatternCloseResource(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2, statement);
        }

        return null;
    }

    public static Statement make(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements,
            DefaultList<TryStatement.CatchClause> catchClauses, Statements finallyStatements) {
        int size = statements.size();

        if (size < 1 || !checkThrowable(catchClauses)) {
            return null;
        }

        Statement statement = statements.getLast();
        
        if (!statement.isExpressionStatement()) {
            return null;
        }

        Expression expression = statement.getExpression();

        if (!expression.isBinaryOperatorExpression()) {
            return null;
        }

        Expression boe = expression;

        expression = boe.getLeftExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable lv1 = ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable();
        
        BaseStatement catchStatements = catchClauses.getFirst().getStatements();
        
        if (catchStatements == null || catchStatements.size() != 2) {
            return null;
        }

        ClassFileTryStatement tryStatement = getClassFileTryStatement(catchStatements, lv1);
        if (tryStatement == null || !catchStatements.getLast().isThrowStatement()) {
            return null;
        }
        
        ThrowStatement throwStatement = (ThrowStatement) catchStatements.getLast();
        
        expression = throwStatement.getExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable lv2 = ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable(); 
        
        if (tryStatement.getTryStatements() == null || tryStatement.getTryStatements().size() != 1) {
            return null;
        }

        if (!(tryStatement.getTryStatements().getFirst() instanceof ExpressionStatement)) {
            return null;
        }
        
        ExpressionStatement expressionStatement = (ExpressionStatement) tryStatement.getTryStatements().getFirst();
            
        if (!(expressionStatement.getExpression() instanceof MethodInvocationExpression)) {
            return null;
        }

        MethodInvocationExpression mie = (MethodInvocationExpression) expressionStatement.getExpression();
        if (!checkCloseInvocation(mie, lv1)) {
            return null;
        }

        if (tryStatement.getCatchClauses() == null || tryStatement.getCatchClauses().size() != 1) {
            return null;
        }

        CatchClause catchClause = tryStatement.getCatchClauses().getFirst();
        
        if (catchClause.getStatements() == null || catchClause.getStatements().size() != 1) {
            return null;
        }

        if (!(catchClause.getStatements().getFirst() instanceof ExpressionStatement)) {
            return null;
        }
        
        expressionStatement = (ExpressionStatement) catchClause.getStatements().getFirst();
            
        if (!(expressionStatement.getExpression() instanceof MethodInvocationExpression)) {
            return null;
        }

        mie = (MethodInvocationExpression) expressionStatement.getExpression();

        if (!"addSuppressed".equals(mie.getName()) || !"(Ljava/lang/Throwable;)V".equals(mie.getDescriptor())) {
            return null;
        }

        expression = mie.getExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        if (((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable() != lv2) {
            return null;
        }

        return newTryStatement(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2);
    }

    private static ClassFileTryStatement getClassFileTryStatement(BaseStatement catchStatements, AbstractLocalVariable lv1) {
        Statement firstStatement = catchStatements.getFirst();
        if (firstStatement instanceof ClassFileTryStatement) {
            return (ClassFileTryStatement) firstStatement;
        }
        if (firstStatement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) firstStatement;
            Expression condition = ifStatement.getCondition();
            BaseStatement thenStatements = ifStatement.getStatements();
            if (condition instanceof BinaryOperatorExpression && thenStatements.size() == 1) {
                BinaryOperatorExpression boe = (BinaryOperatorExpression) condition;
                if ("!=".equals(boe.getOperator())
                        && boe.getRightExpression() instanceof NullExpression
                        && boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression
                        && ((ClassFileLocalVariableReferenceExpression) boe.getLeftExpression()).getLocalVariable() == lv1
                        && thenStatements.getFirst() instanceof ClassFileTryStatement) {
                    ClassFileTryStatement tryStatement = (ClassFileTryStatement) thenStatements.getFirst();
                    BaseStatement tryStatements = ((ClassFileTryStatement)thenStatements.getFirst()).getTryStatements();
                    if (tryStatements.size() == 1 && tryStatements.getFirst() instanceof ExpressionStatement) {
                        MethodInvocationExpression mie = (MethodInvocationExpression) tryStatements.getFirst().getExpression();
                        if (checkCloseInvocation(mie, lv1)) {
                            return tryStatement;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private static Statement parsePatternAddSuppressed(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements,
            Statements finallyStatements, Expression boe, AbstractLocalVariable lv1, AbstractLocalVariable lv2,
            Statement statement) {
        if (!statement.isIfElseStatement()) {
            return null;
        }

        Statement ies = statement;

        statement = ies.getStatements().getFirst();

        if (!statement.isTryStatement()) {
            return null;
        }

        Statement ts = statement;

        statement = ies.getElseStatements().getFirst();

        if (!statement.isExpressionStatement()) {
            return null;
        }

        Expression expression = statement.getExpression();

        if (!expression.isMethodInvocationExpression()) {
            return null;
        }

        MethodInvocationExpression mie = (MethodInvocationExpression) expression;

        if (ts.getFinallyStatements() != null || lv2 != getLocalVariable(ies.getCondition()) ||
                !checkThrowable(ts.getCatchClauses()) || !checkCloseInvocation(mie, lv1)) {
            return null;
        }

        statement = ts.getTryStatements().getFirst();

        if (!statement.isExpressionStatement()) {
            return null;
        }

        expression = statement.getExpression();

        if (!expression.isMethodInvocationExpression()) {
            return null;
        }

        mie = (MethodInvocationExpression) expression;

        if (!checkCloseInvocation(mie, lv1)) {
            return null;
        }

        statement = ts.getCatchClauses().getFirst().getStatements().getFirst();

        if (!statement.isExpressionStatement()) {
            return null;
        }

        expression = statement.getExpression();

        if (!expression.isMethodInvocationExpression()) {
            return null;
        }

        mie = (MethodInvocationExpression) expression;

        if (!"addSuppressed".equals(mie.getName()) || !"(Ljava/lang/Throwable;)V".equals(mie.getDescriptor())) {
            return null;
        }

        expression = mie.getExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        if (((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable() != lv2) {
            return null;
        }

        return newTryStatement(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2);
    }

    private static boolean checkThrowable(List<? extends TryStatement.CatchClause> catchClauses) {
        return catchClauses.size() == 1 && catchClauses.get(0).getType().equals(ObjectType.TYPE_THROWABLE);
    }

    private static AbstractLocalVariable getLocalVariable(Expression condition) {
        if (!condition.isBinaryOperatorExpression()) {
            return null;
        }

        if (!"!=".equals(condition.getOperator()) || !condition.getRightExpression().isNullExpression() || !condition.getLeftExpression().isLocalVariableReferenceExpression()) {
            return null;
        }

        return ((ClassFileLocalVariableReferenceExpression) condition.getLeftExpression()).getLocalVariable();
    }

    private static boolean checkCloseInvocation(MethodInvocationExpression mie, AbstractLocalVariable lv) {
        if ("close".equals(mie.getName()) && "()V".equals(mie.getDescriptor())) {
            Expression expression = mie.getExpression();

            if (expression.isLocalVariableReferenceExpression()) {
                return ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable() == lv;
            }
        }

        return false;
    }

    private static Statement parsePatternCloseResource(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements, Statements finallyStatements,
            Expression boe, AbstractLocalVariable lv1, AbstractLocalVariable lv2, Statement statement) {
        Expression expression = statement.getExpression();

        if (!expression.isMethodInvocationExpression()) {
            return null;
        }

        MethodInvocationExpression mie = (MethodInvocationExpression) expression;

        if (!"$closeResource".equals(mie.getName()) || !"(Ljava/lang/Throwable;Ljava/lang/AutoCloseable;)V".equals(mie.getDescriptor())) {
            return null;
        }

        DefaultList<Expression> parameters = mie.getParameters().getList();
        Expression parameter0 = parameters.getFirst();

        if (!parameter0.isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)parameter0).getLocalVariable() != lv2) {
            return null;
        }

        Expression parameter1 = parameters.get(1);

        if (!parameter1.isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)parameter1).getLocalVariable() != lv1) {
            return null;
        }

        return newTryStatement(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2);
    }

    private static ClassFileTryStatement newTryStatement(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements,
            Statements finallyStatements, Expression boe, AbstractLocalVariable lv1, AbstractLocalVariable lv2) {

        // Remove resource & synthetic local variables
        if (checkLocalVariable(statements, lv2)) {
            statements.removeLast();
        }
        if (checkLocalVariable(statements, lv1)) {
            statements.removeLast();
        }
        lv1.setDeclared(true);
        localVariableMaker.removeLocalVariable(lv2);

        // Remove close statements
        tryStatements.accept(new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(Statements statements) {
                if (statements.isList()) {
                    for (Iterator<Statement> iterator = statements.getList().iterator(); iterator.hasNext();) {
                        Statement statement = iterator.next();
                        if (statement instanceof IfStatement) {
                            IfStatement ifStatement = (IfStatement) statement;
                            Expression condition = ifStatement.getCondition();
                            BaseStatement thenStatements = ifStatement.getStatements();
                            if (condition instanceof BinaryOperatorExpression && thenStatements.size() == 1) {
                                Statement singleStatement = thenStatements.getFirst();
                                BinaryOperatorExpression boe = (BinaryOperatorExpression) condition;
                                if ("!=".equals(boe.getOperator())
                                        && boe.getRightExpression() instanceof NullExpression
                                        && boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression
                                        && ((ClassFileLocalVariableReferenceExpression) boe.getLeftExpression()).getLocalVariable() == lv1
                                        && singleStatement.getExpression() instanceof MethodInvocationExpression) {
                                    MethodInvocationExpression mie = (MethodInvocationExpression) singleStatement.getExpression();
                                    if (checkCloseInvocation(mie, lv1)) {
                                        iterator.remove();
                                    }
                                }
                            }
                        }
                        Expression expression = statement.getExpression();
                        if (expression instanceof MethodInvocationExpression) {
                            MethodInvocationExpression mie = (MethodInvocationExpression) expression;
                            if (checkCloseInvocation(mie, lv1)) {
                                iterator.remove();
                            }
                        }
                    }
                }
                super.visit(statements);
            }
        });

        // Create try-with-resources statement
        DefaultList<TryStatement.Resource> resources = new DefaultList<>();

        resources.add(new TryStatement.Resource((ObjectType) lv1.getType(), lv1.getName(), boe.getRightExpression()));

        return new ClassFileTryStatement(resources, tryStatements, null, finallyStatements, false, false);
    }

    private static boolean checkLocalVariable(Statements statements, AbstractLocalVariable lv) {
        if (!statements.isEmpty()) {
            Statement lastStatement = statements.getLast();
            if (lastStatement instanceof ExpressionStatement) {
                ExpressionStatement expressionStatement = (ExpressionStatement) lastStatement;
                Expression expression = expressionStatement.getExpression();
                if (expression instanceof BinaryOperatorExpression) {
                    BinaryOperatorExpression boe = (BinaryOperatorExpression) expression;
                    Expression leftExpression = boe.getLeftExpression();
                    if (leftExpression instanceof ClassFileLocalVariableReferenceExpression) {
                        ClassFileLocalVariableReferenceExpression ref = (ClassFileLocalVariableReferenceExpression) leftExpression;
                        return lv == ref.getLocalVariable();
                    }
                }
            }
        }
        return false;
    }
}
