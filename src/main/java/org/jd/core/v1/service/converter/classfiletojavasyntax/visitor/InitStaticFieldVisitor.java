/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileStaticInitializerDeclaration;
import org.jd.core.v1.util.DefaultList;

import java.util.Iterator;

public class InitStaticFieldVisitor extends AbstractJavaSyntaxVisitor {
    protected SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
    protected String internalTypeName;
    protected DefaultList<FieldDeclarator> fields = new DefaultList<>();
    protected ClassFileStaticInitializerDeclaration staticDeclaration;

    public void setInternalTypeName(String internalTypeName) {
        this.internalTypeName = internalTypeName;
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        this.internalTypeName = declaration.getInternalName();
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        this.internalTypeName = declaration.getInternalName();
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        this.internalTypeName = declaration.getInternalName();
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;
        // Store field declarations
        fields.clear();
        staticDeclaration = null;
        safeAcceptListDeclaration(bodyDeclaration.getFieldDeclarations());

        if (!fields.isEmpty()) {
            // Visit methods
            safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        }

        if ((staticDeclaration != null) && (staticDeclaration.getStatements() == null)) {
            bodyDeclaration.getMethodDeclarations().remove(staticDeclaration);
        }
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {}

    @Override
    public void visit(MethodDeclaration declaration) {}

    @Override
    @SuppressWarnings("unchecked")
    public void visit(StaticInitializerDeclaration declaration) {
        staticDeclaration = (ClassFileStaticInitializerDeclaration) declaration;

        if (!staticDeclaration.getStatements().isList()) {
            return;
        }

        DefaultList<Statement> statements = staticDeclaration.getStatements().getList();

        if ((statements != null) && !statements.isEmpty()) {
            Statement statement = statements.getFirst();

            if (statement.getClass() == ExpressionStatement.class) {
                ExpressionStatement cdes = (ExpressionStatement) statement;

                if (cdes.getExpression().getClass() == BinaryOperatorExpression.class) {
                    BinaryOperatorExpression cfboe = (BinaryOperatorExpression) cdes.getExpression();

                    if (cfboe.getLeftExpression().getClass() == FieldReferenceExpression.class) {
                        FieldReferenceExpression fre = (FieldReferenceExpression) cfboe.getLeftExpression();

                        if ((fre.getType() == PrimitiveType.TYPE_BOOLEAN) && fre.getInternalTypeName().equals(internalTypeName) && fre.getName().equals("$assertionsDisabled")) {
                            // Remove assert initialization statement
                            statements.remove(0);
                        }
                    }
                }
            }

            Iterator<Statement> statementIterator = statements.iterator();
            Iterator<FieldDeclarator> fieldDeclaratorIterator = fields.iterator();

            while (statementIterator.hasNext()) {
                statement = statementIterator.next();

                if (statement.getClass() != ExpressionStatement.class) {
                    break;
                }

                ExpressionStatement cdes = (ExpressionStatement) statement;

                if (cdes.getExpression().getClass() != BinaryOperatorExpression.class) {
                    break;
                }

                BinaryOperatorExpression cfboe = (BinaryOperatorExpression) cdes.getExpression();

                if (cfboe.getLeftExpression().getClass() != FieldReferenceExpression.class) {
                    break;
                }

                FieldReferenceExpression fre = (FieldReferenceExpression) cfboe.getLeftExpression();

                if (!fre.getInternalTypeName().equals(internalTypeName)) {
                    break;
                }

                FieldDeclarator fieldDeclarator = null;

                while (fieldDeclaratorIterator.hasNext()) {
                    FieldDeclarator fdr = fieldDeclaratorIterator.next();
                    FieldDeclaration fdn = fdr.getFieldDeclaration();

                    if (((fdn.getFlags() & Declaration.FLAG_STATIC) != 0) && fdr.getName().equals(fre.getName()) && fdn.getType().getDescriptor().equals(fre.getDescriptor())) {
                        fieldDeclarator = fdr;
                        break;
                    }
                }

                if (fieldDeclarator == null) {
                    break;
                } else {
                    Expression expression = cfboe.getRightExpression();

                    fieldDeclarator.setVariableInitializer(new ExpressionVariableInitializer(expression));
                    ((ClassFileFieldDeclaration)fieldDeclarator.getFieldDeclaration()).setFirstLineNumber(expression.getLineNumber());
                    statementIterator.remove();
                }
            }

            if (statements.isEmpty()) {
                staticDeclaration.setStatements(null);
                staticDeclaration.setFirstLineNumber(0);
            } else {
                searchFirstLineNumberVisitor.init();
                staticDeclaration.getStatements().accept(searchFirstLineNumberVisitor);
                int firstLineNumber = searchFirstLineNumberVisitor.getLineNumber();

                staticDeclaration.setFirstLineNumber((firstLineNumber==-1) ? 0 : firstLineNumber);
            }
        }
    }

    @Override
    public void visit(FieldDeclarator declaration) {
        fields.add(declaration);
    }
}
