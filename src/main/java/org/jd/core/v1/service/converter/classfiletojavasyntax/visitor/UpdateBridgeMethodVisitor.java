/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.statement.*;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;
import java.util.List;

import static org.jd.core.v1.model.javasyntax.declaration.Declaration.*;

public class UpdateBridgeMethodVisitor extends AbstractUpdateExpressionVisitor {
    protected BodyDeclarationsVisitor bodyDeclarationsVisitor = new BodyDeclarationsVisitor();
    protected HashMap<String, HashMap<String, ClassFileMethodDeclaration>> bridgeMethodDeclarations = new HashMap<>();

    public boolean init(ClassFileBodyDeclaration bodyDeclaration) {
        bridgeMethodDeclarations.clear();
        bodyDeclarationsVisitor.visit(bodyDeclaration);
        return !bridgeMethodDeclarations.isEmpty();
    }

    @SuppressWarnings("unchecked")
    protected Expression updateExpression(Expression expression) {
        if (expression.getClass() != MethodInvocationExpression.class) {
            return expression;
        }

        MethodInvocationExpression mie1 = (MethodInvocationExpression)expression;
        HashMap<String, ClassFileMethodDeclaration> map = bridgeMethodDeclarations.get(mie1.getExpression().getType().getDescriptor());

        if (map == null) {
            return expression;
        }

        ClassFileMethodDeclaration methodBridgeDeclaration = map.get(mie1.getName() + mie1.getDescriptor());

        if (methodBridgeDeclaration == null) {
            return expression;
        }

        Statement statement = methodBridgeDeclaration.getStatements().getFirst();
        Class statementClass = statement.getClass();
        Expression exp;

        if (statementClass == ReturnExpressionStatement.class) {
            exp = ((ReturnExpressionStatement) statement).getExpression();
        } else if (statement.getClass() == ExpressionStatement.class) {
            exp = ((ExpressionStatement) statement).getExpression();
        } else {
            return expression;
        }

        Class expClass = exp.getClass();
        int parameterTypesCount = methodBridgeDeclaration.getParameterTypes().size();

        if (expClass == FieldReferenceExpression.class) {
            FieldReferenceExpression fre = (FieldReferenceExpression) exp;

            expression = (parameterTypesCount == 0) ? fre.getExpression() : mie1.getParameters().getFirst();

            return new FieldReferenceExpression(mie1.getLineNumber(), mie1.getType(), expression, fre.getInternalTypeName(), fre.getName(), fre.getDescriptor());
        } else if (expClass == MethodInvocationExpression.class) {
            MethodInvocationExpression mie2 = (MethodInvocationExpression) exp;

            if ((mie2.getExpression().getClass() == ObjectTypeReferenceExpression.class)) {
                return new MethodInvocationExpression(mie1.getLineNumber(), mie2.getType(), mie2.getExpression(), mie2.getInternalTypeName(), mie2.getName(), mie2.getDescriptor(), mie1.getParameters());
            } else {
                BaseExpression newParameters = null;
                BaseExpression mie1Parameters = mie1.getParameters();

                if (mie1Parameters != null) {
                    switch (mie1Parameters.size()) {
                        case 0:
                        case 1:
                            break;
                        case 2:
                            newParameters = mie1Parameters.getList().get(1);
                            break;
                        default:
                            DefaultList<Expression> list = mie1Parameters.getList();
                            newParameters = new Expressions(list.subList(1, list.size()));
                            break;
                    }
                }

                return new MethodInvocationExpression(mie1.getLineNumber(), mie2.getType(), mie1Parameters.getFirst(), mie2.getInternalTypeName(), mie2.getName(), mie2.getDescriptor(), newParameters);
            }
        } else if (expClass == BinaryOperatorExpression.class) {
            BinaryOperatorExpression boe = (BinaryOperatorExpression) exp;
            FieldReferenceExpression fre = (FieldReferenceExpression) boe.getLeftExpression();

            if (parameterTypesCount == 1) {
                return new BinaryOperatorExpression(
                        mie1.getLineNumber(), mie1.getType(),
                        new FieldReferenceExpression(fre.getType(), fre.getExpression(), fre.getInternalTypeName(), fre.getName(), fre.getDescriptor()),
                        boe.getOperator(),
                        mie1.getParameters().getFirst(),
                        boe.getPriority());
            } else if (parameterTypesCount == 2) {
                DefaultList<Expression> parameters = mie1.getParameters().getList();

                return new BinaryOperatorExpression(
                        mie1.getLineNumber(), mie1.getType(),
                        new FieldReferenceExpression(fre.getType(), parameters.get(0), fre.getInternalTypeName(), fre.getName(), fre.getDescriptor()),
                        boe.getOperator(),
                        parameters.get(1),
                        boe.getPriority());
            }
        } else if (expClass == PostOperatorExpression.class) {
            PostOperatorExpression poe = (PostOperatorExpression)exp;
            FieldReferenceExpression fre = (FieldReferenceExpression) poe.getExpression();

            expression = (parameterTypesCount == 0) ? fre.getExpression() : mie1.getParameters().getFirst();

            return new PostOperatorExpression(
                    mie1.getLineNumber(),
                    new FieldReferenceExpression(mie1.getType(), expression, fre.getInternalTypeName(), fre.getName(), fre.getDescriptor()),
                    poe.getOperator());
        } else if (expClass == PreOperatorExpression.class) {
            PreOperatorExpression poe = (PreOperatorExpression)exp;
            FieldReferenceExpression fre = (FieldReferenceExpression) poe.getExpression();

            expression = (parameterTypesCount == 0) ? fre.getExpression() : mie1.getParameters().getFirst();

            return new PreOperatorExpression(
                    mie1.getLineNumber(),
                    poe.getOperator(),
                    new FieldReferenceExpression(mie1.getType(), expression, fre.getInternalTypeName(), fre.getName(), fre.getDescriptor()));
        } else if (expClass == IntegerConstantExpression.class) {
            return exp;
        }

        return expression;
    }

    protected class BodyDeclarationsVisitor extends AbstractJavaSyntaxVisitor {
        protected HashMap<String, ClassFileMethodDeclaration> map = null;

        @Override
        public void visit(BodyDeclaration declaration) {
            ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;
            List<ClassFileConstructorOrMethodDeclaration> methodDeclarations = bodyDeclaration.getMethodDeclarations();

            if ((methodDeclarations != null) && !methodDeclarations.isEmpty()) {
                HashMap<String, ClassFileMethodDeclaration> backup = map;

                map = new HashMap<>();

                acceptListDeclaration(methodDeclarations);

                if (!map.isEmpty()) {
                    bridgeMethodDeclarations.put('L' + bodyDeclaration.getInternalTypeName() + ';', map);
                }

                map = backup;
            }

            safeAcceptListDeclaration(bodyDeclaration.getInnerTypeDeclarations());
        }

        @Override
        public void visit(MethodDeclaration declaration) {
            if ((declaration.getFlags() & FLAG_STATIC) == 0) {
                return;
            }

            BaseStatement statements = declaration.getStatements();

            if ((statements == null) || (statements.size() != 1)) {
                return;
            }

            String name = declaration.getName();

            if (!name.startsWith("access$")) {
                return;
            }

            ClassFileMethodDeclaration bridgeMethodDeclaration = (ClassFileMethodDeclaration)declaration;

            if (!checkBridgeMethodDeclaration(bridgeMethodDeclaration)) {
                return;
            }

            map.put(name + declaration.getDescriptor(), bridgeMethodDeclaration);
        }

        @Override public void visit(ClassDeclaration declaration) { safeAccept(declaration.getBodyDeclaration()); }
        @Override public void visit(EnumDeclaration declaration) { safeAccept(declaration.getBodyDeclaration()); }
        @Override public void visit(InterfaceDeclaration declaration) {}
        @Override public void visit(AnnotationDeclaration declaration) {}

        private boolean checkBridgeMethodDeclaration(ClassFileMethodDeclaration bridgeMethodDeclaration) {
            Statement statement = bridgeMethodDeclaration.getStatements().getFirst();
            Class statementClass = statement.getClass();
            Expression exp;

            if (statementClass == ReturnExpressionStatement.class) {
                exp = ((ReturnExpressionStatement) statement).getExpression();
            } else if (statement.getClass() == ExpressionStatement.class) {
                exp = ((ExpressionStatement) statement).getExpression();
            } else {
                return false;
            }

            Class expClass = exp.getClass();
            int parameterTypesCount = bridgeMethodDeclaration.getParameterTypes().size();

            if (expClass == FieldReferenceExpression.class) {
                FieldReferenceExpression fre = (FieldReferenceExpression) exp;

                if (parameterTypesCount == 0) {
                    return (fre.getExpression() != null) && (fre.getExpression().getClass() == ObjectTypeReferenceExpression.class);
                } else if (parameterTypesCount == 1) {
                    return (fre.getExpression() == null) || checkLocalVariableReference(fre.getExpression(), 0);
                }
            } else if (expClass == MethodInvocationExpression.class) {
                MethodInvocationExpression mie2 = (MethodInvocationExpression) exp;

                if ((mie2.getExpression().getClass() == ObjectTypeReferenceExpression.class)) {
                    BaseExpression mie2Parameters = mie2.getParameters();

                    if ((mie2Parameters == null) || (mie2Parameters.size() == 0)) {
                        return true;
                    }

                    if (mie2Parameters.isList()) {
                        int i = 0;
                        for (Expression parameter : mie2Parameters.getList()) {
                            if (!checkLocalVariableReference(parameter, i++)) {
                                return false;
                            }
                            Type type = parameter.getType();
                            if (type.equals(PrimitiveType.TYPE_LONG) || type.equals(PrimitiveType.TYPE_DOUBLE)) {
                                i++;
                            }
                        }
                        return true;
                    }

                    return checkLocalVariableReference(mie2Parameters, 0);
                } else if ((parameterTypesCount > 0) && checkLocalVariableReference(mie2.getExpression(), 0)) {
                    BaseExpression mie2Parameters = mie2.getParameters();

                    if ((mie2Parameters == null) || (mie2Parameters.size() == 0)) {
                        return true;
                    }

                    if (mie2Parameters.isList()) {
                        int i = 1;
                        for (Expression parameter : mie2Parameters.getList()) {
                            if (!checkLocalVariableReference(parameter, i++)) {
                                return false;
                            }
                            Type type = parameter.getType();
                            if (type.equals(PrimitiveType.TYPE_LONG) || type.equals(PrimitiveType.TYPE_DOUBLE)) {
                                i++;
                            }
                        }
                        return true;
                    }

                    return checkLocalVariableReference(mie2Parameters, 1);
                }
            } else if (expClass == BinaryOperatorExpression.class) {
                BinaryOperatorExpression boe = (BinaryOperatorExpression) exp;

                if (parameterTypesCount == 1) {
                    if ((boe.getLeftExpression().getClass() == FieldReferenceExpression.class) && checkLocalVariableReference(boe.getRightExpression(), 0)) {
                        FieldReferenceExpression fre = (FieldReferenceExpression) boe.getLeftExpression();

                        return (fre.getExpression().getClass() == ObjectTypeReferenceExpression.class);
                    }
                } else if (parameterTypesCount == 2) {
                    if ((boe.getLeftExpression().getClass() == FieldReferenceExpression.class) && checkLocalVariableReference(boe.getRightExpression(), 1)) {
                        FieldReferenceExpression fre = (FieldReferenceExpression) boe.getLeftExpression();

                        return checkLocalVariableReference(fre.getExpression(), 0);
                    }
                }
            } else if (expClass == PostOperatorExpression.class) {
                PostOperatorExpression poe = (PostOperatorExpression)exp;

                if (poe.getExpression().getClass() == FieldReferenceExpression.class) {
                    FieldReferenceExpression fre = (FieldReferenceExpression) poe.getExpression();

                    if ((parameterTypesCount == 0) && (fre.getExpression().getClass() == ObjectTypeReferenceExpression.class)) {
                        return true;
                    } else if ((parameterTypesCount == 1) && (fre.getExpression() != null) && checkLocalVariableReference(fre.getExpression(), 0)) {
                        return true;
                    }
                }
            } else if ((parameterTypesCount == 1) && (expClass == PreOperatorExpression.class)) {
                PreOperatorExpression poe = (PreOperatorExpression)exp;

                if (poe.getExpression().getClass() == FieldReferenceExpression.class) {
                    FieldReferenceExpression fre = (FieldReferenceExpression) poe.getExpression();

                    if ((parameterTypesCount == 0) && (fre.getExpression().getClass() == ObjectTypeReferenceExpression.class)) {
                        return true;
                    } else if ((parameterTypesCount == 1) && (fre.getExpression() != null) && checkLocalVariableReference(fre.getExpression(), 0)) {
                        return true;
                    }
                }
            } else if ((parameterTypesCount == 0) && (expClass == IntegerConstantExpression.class)) {
                return true;
            }

            return false;
        }

        private boolean checkLocalVariableReference(BaseExpression expression, int index) {
            if (expression.getClass() != ClassFileLocalVariableReferenceExpression.class) {
                return false;
            }

            ClassFileLocalVariableReferenceExpression var = (ClassFileLocalVariableReferenceExpression) expression;

            return (var.getLocalVariable().getIndex() == index);
        }
    }
}
