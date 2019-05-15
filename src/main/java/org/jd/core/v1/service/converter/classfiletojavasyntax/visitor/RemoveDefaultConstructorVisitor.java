/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.SuperConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMemberDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.SignatureParser;

import java.util.Iterator;
import java.util.List;

public class RemoveDefaultConstructorVisitor extends AbstractJavaSyntaxVisitor {
    protected SignatureParser signatureParser;
    protected int constructorCounter;
    protected ClassFileMemberDeclaration constructor;

    public RemoveDefaultConstructorVisitor(SignatureParser signatureParser) {
        this.signatureParser = signatureParser;
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;
        List<ClassFileConstructorOrMethodDeclaration> methods = bodyDeclaration.getMethodDeclarations();

        constructor = null;
        constructorCounter = 0;
        safeAcceptListDeclaration(methods);

        if ((constructorCounter == 1) && (constructor != null)) {
            // Remove empty default constructor
            methods.remove(constructor);
        }
    }

    @Override
    public void visit(FieldDeclaration declaration) {}

    @Override
    @SuppressWarnings("unchecked")
    public void visit(ConstructorDeclaration declaration) {
        if ((declaration.getFlags() & ConstructorDeclaration.FLAG_ABSTRACT) == 0) {
            ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration)declaration;

            if (cfcd.getStatements().getClass() == Statements.class) {
                Statements statements = (Statements) cfcd.getStatements();

                // Remove 'super();'
                Iterator<Statement> iterator = statements.iterator();

                while (iterator.hasNext()) {
                    Statement statement = iterator.next();

                    if (statement.getClass() == ExpressionStatement.class) {
                        Expression es = ((ExpressionStatement) statement).getExpression();

                        if (es.getClass() == SuperConstructorInvocationExpression.class) {
                            SuperConstructorInvocationExpression scie = (SuperConstructorInvocationExpression) es;

                            if ("()V".equals(scie.getDescriptor())) {
                                iterator.remove();
                                break;
                            }
                        }
                    }
                }

                // Store empty default constructor
                if (statements.isEmpty()) {
                    ClassFileBodyDeclaration bodyDeclaration = cfcd.getBodyDeclaration();

                    if (bodyDeclaration.getOuterLocalVariableNames() == null) {
                        if (bodyDeclaration.getOuterType() == null) {
                            if (cfcd.getDescriptor().equals("()V")) {
                                constructor = cfcd;
                            }
                        } else {
                            if (cfcd.getDescriptor().equals("(L" + bodyDeclaration.getOuterType().getInternalName() + ";)V")) {
                                constructor = cfcd;
                            }
                        }
                    } else {
                        int syntheticParameterCount = bodyDeclaration.getOuterLocalVariableNames().size();

                        if (bodyDeclaration.getOuterType() != null) {
                            syntheticParameterCount++;
                        }

                        if (signatureParser.parseParameterTypes(cfcd.getDescriptor()).size() == syntheticParameterCount) {
                            constructor = cfcd;
                        }
                    }
                }
            }

            // Inc constructor counter
            constructorCounter++;
        }
    }

    @Override
    public void visit(MethodDeclaration declaration) {}

    @Override
    public void visit(StaticInitializerDeclaration declaration) {}

    @Override
    public void visit(ClassDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }
}
