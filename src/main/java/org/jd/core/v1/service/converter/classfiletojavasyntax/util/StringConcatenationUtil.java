/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.util.DefaultList;

import java.util.StringTokenizer;

public class StringConcatenationUtil {

    public static Expression create(Expression expression, int lineNumber, String typeName) {
        if (expression.getClass() == MethodInvocationExpression.class) {
            MethodInvocationExpression mie = (MethodInvocationExpression) expression;

            if ("append".equals(mie.getName()) && (mie.getParameters() != null) && !mie.getParameters().isList()) {
                Expression concatenatedStringExpression = mie.getParameters().getFirst();
                Expression expr = mie.getExpression();

                while (expr.getClass() == MethodInvocationExpression.class) {
                    mie = (MethodInvocationExpression) expr;

                    if (("append".equals(mie.getName()) == false) || (mie.getParameters() == null) || mie.getParameters().isList()) {
                        break;
                    }

                    concatenatedStringExpression = new BinaryOperatorExpression(mie.getLineNumber(), ObjectType.TYPE_STRING, (Expression) mie.getParameters(), "+", concatenatedStringExpression, 4);
                    expr = mie.getExpression();
                }

                if (expr.getClass() == NewExpression.class) {
                    NewExpression ne = (NewExpression) expr;
                    String internalTypeName = ne.getType().getDescriptor();

                    if ("Ljava/lang/StringBuilder;".equals(internalTypeName) || "Ljava/lang/StringBuffer;".equals(internalTypeName)) {
                        if (ne.getParameters() == null) {
                            return concatenatedStringExpression;
                        }
                        if (!ne.getParameters().isList()) {
                            expression = ne.getParameters().getFirst();

                            if (expression.getType() == ObjectType.TYPE_STRING) {
                                return new BinaryOperatorExpression(ne.getLineNumber(), ObjectType.TYPE_STRING, expression, "+", concatenatedStringExpression, 4);
                            }
                        }
                    }
                }
            }
        }

        return new MethodInvocationExpression(lineNumber, ObjectType.TYPE_STRING, expression, typeName, "toString", "()Ljava/lang/String;");
    }

    public static Expression create(String recipe, BaseExpression parameters) {
        StringTokenizer st = new StringTokenizer(recipe, "\u0001", true);

        if (st.hasMoreTokens()) {
            String token = st.nextToken();
            Expression expression = token.equals("\u0001") ? createFirstStringConcatenationItem(parameters.getFirst()) : new StringConstantExpression(token);

            if (parameters.isList()) {
                DefaultList<Expression> list = parameters.getList();
                int index = 0;

                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    Expression e = token.equals("\u0001") ? list.get(index++) : new StringConstantExpression(token);
                    expression = new BinaryOperatorExpression(expression.getLineNumber(), ObjectType.TYPE_STRING, expression, "+", e, 6);
                }
            } else {
                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    Expression e = token.equals("\u0001") ? parameters.getFirst() : new StringConstantExpression(token);
                    expression = new BinaryOperatorExpression(expression.getLineNumber(), ObjectType.TYPE_STRING, expression, "+", e, 6);
                }
            }

            return expression;
        } else {
            return StringConstantExpression.EMPTY_STRING;
        }
    }

    public static Expression create(BaseExpression parameters) {
        if (parameters.isList()) {
            DefaultList<Expression> list = parameters.getList();

            switch (list.size()) {
                case 0:
                    return StringConstantExpression.EMPTY_STRING;
                case 1:
                    return createFirstStringConcatenationItem(parameters.getFirst());
                default:
                    Expression expression = createFirstStringConcatenationItem(parameters.getFirst());

                    for (int i = 1, len = list.size(); i < len; i++) {
                        expression = new BinaryOperatorExpression(expression.getLineNumber(), ObjectType.TYPE_STRING, expression, "+", list.get(i), 6);
                    }

                    return expression;
            }
        } else {
            return createFirstStringConcatenationItem(parameters.getFirst());
        }
    }

    private static Expression createFirstStringConcatenationItem(Expression expression) {
        if (!expression.getType().equals(ObjectType.TYPE_STRING)) {
            expression = new BinaryOperatorExpression(expression.getLineNumber(), ObjectType.TYPE_STRING, StringConstantExpression.EMPTY_STRING, "+", expression, 6);
        }

        return expression;
    }
}