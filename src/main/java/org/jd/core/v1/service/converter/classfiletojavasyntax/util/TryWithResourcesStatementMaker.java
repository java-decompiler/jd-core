/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.statement.*;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.util.DefaultList;

import java.util.List;


public class TryWithResourcesStatementMaker {

    public static Statement make(LocalVariableMaker localVariableMaker, Statements<Statement> statements, Statements tryStatements, DefaultList<TryStatement.CatchClause> catchClauses, Statements<Statement> finallyStatements) {
        int size = statements.size();

        if ((size < 2) || (finallyStatements == null) || (finallyStatements.size() != 1) || !checkThrowable(catchClauses)) {
            return null;
        }

        Statement statement = finallyStatements.getFirst();

        if (statement.getClass() != IfStatement.class) {
            return null;
        }

        IfStatement is = (IfStatement)statement;

        statement = statements.get(size-2);

        if (statement.getClass() != ExpressionStatement.class) {
            return null;
        }

        Expression expression = ((ExpressionStatement)statement).getExpression();

        if (expression.getClass() != BinaryOperatorExpression.class) {
            return null;
        }

        BinaryOperatorExpression boe = (BinaryOperatorExpression)expression;

        expression = boe.getLeftExpression();

        if (expression.getClass() != ClassFileLocalVariableReferenceExpression.class) {
            return null;
        }

        AbstractLocalVariable lv1 = ((ClassFileLocalVariableReferenceExpression)expression).getLocalVariable();

        statement = statements.get(size-1);

        if (statement.getClass() != ExpressionStatement.class) {
            return null;
        }

        expression = ((ExpressionStatement)statement).getExpression();

        if (expression.getClass() != BinaryOperatorExpression.class) {
            return null;
        }

        expression = ((BinaryOperatorExpression)expression).getLeftExpression();

        if (expression.getClass() != ClassFileLocalVariableReferenceExpression.class) {
            return null;
        }

        AbstractLocalVariable lv2 = ((ClassFileLocalVariableReferenceExpression)expression).getLocalVariable();

        statement = is.getStatements().getFirst();

        if (statement.getClass() != IfElseStatement.class) {
            return null;
        }

        IfElseStatement ies = (IfElseStatement)statement;

        if (lv1 != getLocalVariable(is.getCondition())) {
            return null;
        }

        statement = ies.getStatements().getFirst();

        if (statement.getClass() != ClassFileTryStatement.class) {
            return null;
        }

        ClassFileTryStatement ts = (ClassFileTryStatement)statement;

        statement = ies.getElseStatements().getFirst();

        if (statement.getClass() != ExpressionStatement.class) {
            return null;
        }

        expression = ((ExpressionStatement)statement).getExpression();

        if (expression.getClass() != MethodInvocationExpression.class) {
            return null;
        }

        MethodInvocationExpression mie = (MethodInvocationExpression)expression;

        if ((is.getCondition().getLineNumber() != mie.getLineNumber()) || (ts.getFinallyStatements() != null) ||
            (lv2 != getLocalVariable(ies.getCondition())) || !checkThrowable(ts.getCatchClauses()) || !checkCloseInvocation(mie, lv1)) {
            return null;
        }

        statement = ts.getTryStatements().getFirst();

        if (statement.getClass() != ExpressionStatement.class) {
            return null;
        }

        expression = ((ExpressionStatement)statement).getExpression();

        if (expression.getClass() != MethodInvocationExpression.class) {
            return null;
        }

        mie = (MethodInvocationExpression)expression;

        if (!checkCloseInvocation(mie, lv1)) {
            return null;
        }

        statement = ts.getCatchClauses().get(0).getStatements().getFirst();

        if (statement.getClass() != ExpressionStatement.class) {
            return null;
        }

        expression = ((ExpressionStatement)statement).getExpression();

        if (expression.getClass() != MethodInvocationExpression.class) {
            return null;
        }

        mie = (MethodInvocationExpression)expression;

        if (!mie.getName().equals("addSuppressed") || !mie.getDescriptor().equals("(Ljava/lang/Throwable;)V")) {
            return null;
        }

        expression = mie.getExpression();

        if (expression.getClass() != ClassFileLocalVariableReferenceExpression.class) {
            return null;
        }

        if (((ClassFileLocalVariableReferenceExpression)expression).getLocalVariable() != lv2) {
            return null;
        }

        // Remove resource & synthetic local variables
        statements.removeLast();
        statements.removeLast();
        lv1.setDeclared(true);
        localVariableMaker.removeLocalVariable(lv2);

        // Create try-with-resources statement
        DefaultList<TryStatement.Resource> resources = new DefaultList<>();

        resources.add(new TryStatement.Resource((ObjectType)lv1.getType(), lv1.getName(), boe.getRightExpression()));

        return new ClassFileTryStatement(resources, tryStatements, null, finallyStatements, false, false);
    }

    protected static boolean checkThrowable(List<? extends TryStatement.CatchClause> catchClauses) {
        return (catchClauses.size() == 1) && catchClauses.get(0).getType().equals(ObjectType.TYPE_THROWABLE);
    }

    protected static AbstractLocalVariable getLocalVariable(Expression condition) {
        if (condition.getClass() != BinaryOperatorExpression.class) {
            return null;
        }

        BinaryOperatorExpression boe = (BinaryOperatorExpression)condition;

        if (!boe.getOperator().equals("!=") || (boe.getRightExpression().getClass() != NullExpression.class) || (boe.getLeftExpression().getClass() != ClassFileLocalVariableReferenceExpression.class)) {
            return null;
        }

        return ((ClassFileLocalVariableReferenceExpression) boe.getLeftExpression()).getLocalVariable();
    }

    protected static boolean checkCloseInvocation(MethodInvocationExpression mie, AbstractLocalVariable lv) {
        if (mie.getName().equals("close") && mie.getDescriptor().equals("()V")) {
            Expression expression = mie.getExpression();

            if (expression.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                return ((ClassFileLocalVariableReferenceExpression)expression).getLocalVariable() == lv;
            }
        }

        return false;
    }
}
