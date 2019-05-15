/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.statement.*;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileClassDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;
import java.util.ListIterator;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.util.ReflectionUtil.*;


public class SwitchStatementMaker {
    protected static final Integer MINUS_ONE = Integer.valueOf(-1);

    @SuppressWarnings("unchecked")
    public static void makeSwitchString(LocalVariableMaker localVariableMaker, Statements statements, SwitchStatement switchStatement) {
        int size = statements.size();
        SwitchStatement previousSwitchStatement = (SwitchStatement)statements.get(size - 2);

        if ((previousSwitchStatement.getCondition().getLineNumber() == switchStatement.getCondition().getLineNumber()) && (previousSwitchStatement.getCondition().getClass() == MethodInvocationExpression.class)) {
            AbstractLocalVariable syntheticLV1 = invokeGetters(previousSwitchStatement.getCondition(), getExpression_getLocalVariable, AbstractLocalVariable.class);

            if (ReflectionUtil.equals(syntheticLV1, invokeGetters(statements.get(size - 4), getExpression_getLeftExpression_getLocalVariable))) {
                BinaryOperatorExpression boe2 = invokeGetter(statements.get(size - 3), getExpression, BinaryOperatorExpression.class);

                if ((boe2 != null) && MINUS_ONE.equals(invokeGetters(boe2, getRightExpression_getValue))) {
                    AbstractLocalVariable syntheticLV2 = invokeGetter(switchStatement.getCondition(), getLocalVariable, AbstractLocalVariable.class);

                    if (ReflectionUtil.equals(syntheticLV2, invokeGetter(boe2.getLeftExpression(), getLocalVariable))) {
                        MethodInvocationExpression mie = (MethodInvocationExpression) previousSwitchStatement.getCondition();

                        if (mie.getName().equals("hashCode") && mie.getDescriptor().equals("()I")) {
                            // Pattern found ==> Parse cases of the synthetic switch statement 'previousSwitchStatement'
                            HashMap<Integer, String> map = new HashMap<>();

                            // Create map<synthetic index -> string>
                            for (SwitchStatement.Block block : previousSwitchStatement.getBlocks()) {
                                BaseStatement stmts = block.getStatements();

                                assert (stmts != null) && (stmts instanceof Statements) && !((Statements)stmts).isEmpty();

                                for (Statement stmt : (Statements<Statement>)stmts) {
                                    if (stmt.getClass() != IfStatement.class) {
                                        break;
                                    }

                                    IfStatement is = (IfStatement) stmt;
                                    String string = invokeGetters(is.getCondition(), getParameters_getString, String.class);
                                    Statements sal = (Statements) is.getStatements();
                                    Integer index = invokeGetters(sal.get(0), getExpression_getRightExpression_getValue, Integer.class);
                                    map.put(index, string);
                                }
                            }

                            // Replace synthetic index by string
                            for (SwitchStatement.Block block : switchStatement.getBlocks()) {
                                SwitchStatement.LabelBlock lb = (SwitchStatement.LabelBlock) block;

                                if (lb.getLabel() != SwitchStatement.DEFAULT_LABEL) {
                                    SwitchStatement.ExpressionLabel el = (SwitchStatement.ExpressionLabel) lb.getLabel();
                                    IntegerConstantExpression nce = (IntegerConstantExpression) el.getExpression();
                                    el.setExpression(new StringConstantExpression(nce.getLineNumber(), map.get(nce.getValue())));
                                }
                            }

                            // Replace synthetic key
                            switchStatement.setCondition(invokeGetters(statements.get(size - 4), getExpression_getRightExpression, Expression.class));

                            // Remove next synthetic statements
                            statements.subList(size - 4, size - 1).clear();

                            // Remove synthetic local variables
                            localVariableMaker.removeLocalVariable(syntheticLV1);
                            localVariableMaker.removeLocalVariable(syntheticLV2);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void makeSwitchEnum(ClassFileBodyDeclaration bodyDeclaration, SwitchStatement switchStatement) {
        ArrayExpression ae = (ArrayExpression)switchStatement.getCondition();
        Class expressionClass = ae.getExpression().getClass();

        if (expressionClass == FieldReferenceExpression.class) {
            FieldReferenceExpression fre = (FieldReferenceExpression)ae.getExpression();

            if (fre.getDescriptor().equals("[I") && fre.getName().startsWith("$SwitchMap$")) {
                // Javac switch-enum pattern
                ClassFileClassDeclaration syntheticClass =  (ClassFileClassDeclaration)bodyDeclaration.getInnerTypeDeclaration(fre.getInternalTypeName());

                bodyDeclaration = (ClassFileBodyDeclaration)syntheticClass.getBodyDeclaration();

                DefaultList<Statement> statements = (DefaultList)bodyDeclaration.getMethodDeclarations().get(0).getStatements();
                updateSwitchStatement(switchStatement, statements.listIterator());
            }
        } else if (expressionClass == MethodInvocationExpression.class) {
            MethodInvocationExpression mie = (MethodInvocationExpression)ae.getExpression();
            String methodName = mie.getName();

            if (mie.getDescriptor().equals("()[I") && methodName.startsWith("$SWITCH_TABLE$")) {
                // Eclipse compiler switch-enum pattern
                for (ClassFileConstructorOrMethodDeclaration declaration : bodyDeclaration.getMethodDeclarations()) {
                     if (declaration.getMethod().getName().equals(methodName)) {
                         DefaultList<Statement> statements = (DefaultList)declaration.getStatements();
                         updateSwitchStatement(switchStatement, statements.listIterator(3));
                         break;
                     }
                }
            }
        }
    }

    protected static void updateSwitchStatement(SwitchStatement switchStatement, ListIterator<Statement> iterator) {
        // Create map<synthetic index -> enum name>
        HashMap<Integer, String> map = new HashMap<>();

        while (iterator.hasNext()) {
            Statement statement = iterator.next();

            if (statement.getClass() != ClassFileTryStatement.class) {
                break;
            }

            BaseStatement statements = ((ClassFileTryStatement)statement).getTryStatements();

            if (!statements.isList()) {
                break;
            }

            statement = statements.getList().getFirst();

            if (statement.getClass() != ExpressionStatement.class) {
                break;
            }

            Expression expression = ((ExpressionStatement)statement).getExpression();

            if (expression.getClass() != BinaryOperatorExpression.class) {
                break;
            }

            BinaryOperatorExpression boe = (BinaryOperatorExpression)expression;
            Integer index = invokeGetters(boe, getRightExpression_getValue, Integer.class);
            String name = invokeGetters(boe, getLeftExpression_getIndex_getExpression_getName, String.class);

            map.put(index, name);
        }

        // Replace synthetic index by enum name
        ArrayExpression ae = (ArrayExpression)switchStatement.getCondition();
        Expression expression = ((MethodReferenceExpression)ae.getIndex()).getExpression();
        ObjectType type = (ObjectType)expression.getType();

        for (SwitchStatement.Block block : switchStatement.getBlocks()) {
            if (block.getClass() == SwitchStatement.LabelBlock.class) {
                SwitchStatement.LabelBlock lb = (SwitchStatement.LabelBlock) block;

                if (lb.getLabel() != SwitchStatement.DEFAULT_LABEL) {
                    SwitchStatement.ExpressionLabel el = (SwitchStatement.ExpressionLabel) lb.getLabel();
                    IntegerConstantExpression nce = (IntegerConstantExpression) el.getExpression();
                    el.setExpression(new EnumConstantReferenceExpression(nce.getLineNumber(), type, map.get(nce.getValue())));
                }
            } else if (block.getClass() == SwitchStatement.MultiLabelsBlock.class) {
                SwitchStatement.MultiLabelsBlock lmb = (SwitchStatement.MultiLabelsBlock) block;

                for (SwitchStatement.Label label : lmb.getLabels()) {
                    if (label != SwitchStatement.DEFAULT_LABEL) {
                        SwitchStatement.ExpressionLabel el = (SwitchStatement.ExpressionLabel) label;
                        IntegerConstantExpression nce = (IntegerConstantExpression) el.getExpression();
                        el.setExpression(new EnumConstantReferenceExpression(nce.getLineNumber(), type, map.get(nce.getValue())));
                    }
                }
            }
        }

        // Replace synthetic key
        switchStatement.setCondition(expression);
    }
}
