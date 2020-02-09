/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.Expression;

public class AutoboxingVisitor extends AbstractUpdateExpressionVisitor {

    protected Expression updateExpression(Expression expression) {
        if (expression.isMethodInvocationExpression()) {
            switch (expression.getInternalTypeName()) {
                case "java/lang/Boolean":
                case "java/lang/Byte":
                case "java/lang/Character":
                case "java/lang/Float":
                case "java/lang/Integer":
                case "java/lang/Long":
                case "java/lang/Short":
                case "java/lang/Double":
                    if (expression.getExpression().isObjectTypeReferenceExpression()) {
                        // static method invocation
                        if (expression.getName().equals("valueOf") && (expression.getParameters().size() == 1)) {
                            return expression.getParameters().getFirst();
                        }
                    } else {
                        // non-static method invocation
                        if (expression.getName().endsWith("Value") && (expression.getParameters() == null)) {
                            return expression.getExpression();
                        }
                    }
            }
        }

        return expression;
    }
}
