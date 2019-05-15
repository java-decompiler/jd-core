/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.declaration.ArrayVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.VariableInitializer;
import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;

import java.util.Collections;
import java.util.ListIterator;

public class NewArrayMaker {
    protected static final ArrayVariableInitializer EMPTY_ARRAY = new ArrayVariableInitializer(PrimitiveType.TYPE_VOID);

    @SuppressWarnings("unchecked")
    public static Expression make(Statements<Statement> statements, NewArray newArray) {
        if (! statements.isEmpty()) {
            Statement statement = statements.getLast();

            if (statement.getClass() == ExpressionStatement.class) {
                ExpressionStatement es = (ExpressionStatement)statement;

                if (es.getExpression().getClass() == BinaryOperatorExpression.class) {
                    BinaryOperatorExpression boe = (BinaryOperatorExpression)es.getExpression();

                    if (boe.getLeftExpression().getClass() == ArrayExpression.class) {
                        ArrayExpression ae = (ArrayExpression)boe.getLeftExpression();

                        if ((ae.getExpression() == newArray) && (ae.getIndex().getClass() == IntegerConstantExpression.class)) {
                            return new NewInitializedArray(newArray.getLineNumber(), newArray.getType(), createVariableInitializer(statements.listIterator(statements.size()), newArray));
                        }
                    }
                }
            }
        }

        return newArray;
    }

    protected static ArrayVariableInitializer createVariableInitializer(ListIterator<Statement> li, NewArray newArray) {
        Statement statement = li.previous();

        li.remove();

        Type type = newArray.getType();
        BinaryOperatorExpression boe = (BinaryOperatorExpression)((ExpressionStatement)statement).getExpression();
        ArrayVariableInitializer array = new ArrayVariableInitializer(type.createType(type.getDimension()-1));
        int index = ((IntegerConstantExpression)((ArrayExpression)boe.getLeftExpression()).getIndex()).getValue();

        array.add(new ExpressionVariableInitializer(boe.getRightExpression()));

        while (li.hasPrevious()) {
            statement = li.previous();

            if (statement.getClass() == ExpressionStatement.class) {
                ExpressionStatement es = (ExpressionStatement)statement;

                if (es.getExpression().getClass() == BinaryOperatorExpression.class) {
                    boe = (BinaryOperatorExpression)es.getExpression();

                    if (boe.getLeftExpression().getClass() == ArrayExpression.class) {
                        ArrayExpression ae = (ArrayExpression) boe.getLeftExpression();

                        if (ae.getIndex().getClass() == IntegerConstantExpression.class) {
                            if (ae.getExpression() == newArray) {
                                index = ((IntegerConstantExpression)ae.getIndex()).getValue();
                                array.add(new ExpressionVariableInitializer(boe.getRightExpression()));
                                li.remove();
                                continue;
                            } else if (ae.getExpression().getClass() == NewArray.class) {
                                VariableInitializer lastVI = array.getLast();

                                if (lastVI.getClass() == ExpressionVariableInitializer.class) {
                                    Expression lastE = ((ExpressionVariableInitializer) lastVI).getExpression();

                                    if ((lastE.getClass() == NewArray.class) && (ae.getExpression() == lastE)) {
                                        array.removeLast();
                                        li.next();
                                        array.add(createVariableInitializer(li, (NewArray)lastE));
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            li.next();
            break;
        }

        // Replace 'new XXX[0][]' with '{}'
        ListIterator<VariableInitializer> vii = array.listIterator();

        while (vii.hasNext()) {
            VariableInitializer vi = vii.next();

            if (vi.getClass() == ExpressionVariableInitializer.class) {
                Expression expression = ((ExpressionVariableInitializer)vi).getExpression();

                if (expression.getClass() == NewArray.class) {
                    NewArray ne = (NewArray) expression;

                    if ((ne.getDimensionExpressionList().getClass() == IntegerConstantExpression.class) && (((IntegerConstantExpression)ne.getDimensionExpressionList()).getValue() == 0)) {
                        Type t = ne.getType();

                        if ((type.getDimension() == t.getDimension() + 1) && (type.getDescriptor().length() == t.getDescriptor().length() + 1) && type.getDescriptor().endsWith(t.getDescriptor())) {
                            vii.set(EMPTY_ARRAY);
                        }
                    }
                }
            }
        }

        // Padding
        if (index > 0) {
            ExpressionVariableInitializer evi;

            type = type.createType(type.getDimension()-1);

            if ((type.getDimension() == 0) && type.isPrimitive()) {
                evi = new ExpressionVariableInitializer(new IntegerConstantExpression(type, 0));
            } else {
                evi = new ExpressionVariableInitializer(new NullExpression(type));
            }

            while (index-- > 0) {
                array.add(evi);
            }
        }

        Collections.reverse(array);

        return array;
    }
}
