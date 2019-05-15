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
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.type.ObjectType;

public class CreateConcatStringUtil {

    public static Expression create(Expression expression, int lineNumber, String typeName) {
        if (expression.getClass() == MethodInvocationExpression.class) {
            MethodInvocationExpression mie = (MethodInvocationExpression)expression;

            if ("append".equals(mie.getName()) && (mie.getParameters() != null) && !mie.getParameters().isList()) {
                Expression concatenatedStringExpression = mie.getParameters().getFirst();

                Expression expr = mie.getExpression();

                while (expr.getClass() == MethodInvocationExpression.class) {
                    mie = (MethodInvocationExpression)expr;

                    if (("append".equals(mie.getName()) == false) || (mie.getParameters() == null) || mie.getParameters().isList())
                        break;

                    concatenatedStringExpression = new BinaryOperatorExpression(mie.getLineNumber(), ObjectType.TYPE_STRING, (Expression)mie.getParameters(), "+", concatenatedStringExpression, 4);
                    expr = mie.getExpression();
                }

                if (expr.getClass() == NewExpression.class) {
                    NewExpression ne = (NewExpression)expr;
                    String internalTypeName = ne.getType().getDescriptor();

                    if ("Ljava/lang/StringBuilder;".equals(internalTypeName) || "Ljava/lang/StringBuffer;".equals(internalTypeName)) {
                        if (ne.getParameters() == null) {
                            return concatenatedStringExpression;
                        } else if (!ne.getParameters().isList()) {
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
}
