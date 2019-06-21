/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SynchronizedStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileMonitorEnterStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileMonitorExitStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

import java.util.Iterator;


public class SynchronizedStatementMaker {

    public static Statement make(LocalVariableMaker localVariableMaker, Statements<Statement> statements, Statements<Statement> tryStatements) {
        // Remove monitor enter
        ClassFileMonitorEnterStatement monitorEnterStatement = (ClassFileMonitorEnterStatement) statements.removeLast();
        Expression monitor = monitorEnterStatement.getMonitor();
        Class monitorClass = monitor.getClass();
        AbstractLocalVariable localVariable = null;

        if (monitorClass == ClassFileLocalVariableReferenceExpression.class) {
            if (!statements.isEmpty()) {
                Statement statement = statements.removeLast();

                if (statement.getClass() == ExpressionStatement.class) {
                    Expression expression = ((ExpressionStatement)statement).getExpression();

                    if (expression.getClass() == BinaryOperatorExpression.class) {
                        BinaryOperatorExpression boe = (BinaryOperatorExpression)expression;

                        if ((boe != null) && (boe.getLeftExpression().getClass() == ClassFileLocalVariableReferenceExpression.class)) {
                            ClassFileLocalVariableReferenceExpression m = (ClassFileLocalVariableReferenceExpression) monitor;
                            ClassFileLocalVariableReferenceExpression l = boe.getGenericLeftExpression();
                            assert l.getLocalVariable() == m.getLocalVariable();
                            // Update monitor
                            monitor = boe.getRightExpression();
                            // Store synthetic local variable
                            localVariable = l.getLocalVariable();
                        }
                    }
                }
            }
        } else if (monitorClass == BinaryOperatorExpression.class) {
            BinaryOperatorExpression boe = (BinaryOperatorExpression)monitor;

            if (boe.getLeftExpression().getClass() == ClassFileLocalVariableReferenceExpression.class) {
                ClassFileLocalVariableReferenceExpression l = boe.getGenericLeftExpression();
                // Update monitor
                monitor = boe.getRightExpression();
                // Store synthetic local variable
                localVariable = l.getLocalVariable();
            }
        }

        new RemoveMonitorExitVisitor(localVariable).visit(tryStatements);

        // Remove synthetic local variable
        localVariableMaker.removeLocalVariable(localVariable);

        return new SynchronizedStatement(monitor, tryStatements);
    }

    protected static class RemoveMonitorExitVisitor extends AbstractJavaSyntaxVisitor {
        protected AbstractLocalVariable localVariable;

        public RemoveMonitorExitVisitor(AbstractLocalVariable localVariable) {
            this.localVariable = localVariable;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void visit(Statements list) {
            if (! list.isEmpty()) {
                Iterator<Statement> iterator = list.iterator();

                while (iterator.hasNext()) {
                    Statement statement = iterator.next();

                    if (statement.getClass() == ClassFileMonitorExitStatement.class) {
                        ClassFileMonitorExitStatement cfmes = (ClassFileMonitorExitStatement)statement;
                        if (cfmes.getMonitor().getClass() == ClassFileLocalVariableReferenceExpression.class) {
                            ClassFileLocalVariableReferenceExpression cflvre = (ClassFileLocalVariableReferenceExpression)cfmes.getMonitor();
                            if (cflvre.getLocalVariable() == localVariable) {
                                iterator.remove();
                            }
                        }
                    } else {
                        statement.accept(this);
                    }
                }
            }
        }
    }
}
