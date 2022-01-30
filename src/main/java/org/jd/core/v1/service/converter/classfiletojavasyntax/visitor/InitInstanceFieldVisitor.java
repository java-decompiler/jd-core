/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.apache.bcel.Const;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.StaticInitializerDeclaration;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static org.jd.core.v1.api.printer.Printer.UNKNOWN_LINE_NUMBER;

public class InitInstanceFieldVisitor extends AbstractJavaSyntaxVisitor {
    private final SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
    private final Map<String, FieldDeclarator> fieldDeclarators = new HashMap<>();
    private final DefaultList<Data> datas = new DefaultList<>();
    private final DefaultList<Expression> putFields = new DefaultList<>();
    private int lineNumber = UNKNOWN_LINE_NUMBER;
    private boolean containsLocalVariableReference;

    @Override
    public void visit(AnnotationDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

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

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        // Init attributes
        fieldDeclarators.clear();
        datas.clear();
        putFields.clear();
        // Visit fields
        safeAcceptListDeclaration(bodyDeclaration.getFieldDeclarations());
        // Visit methods
        safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        // Init values
        updateFieldsAndConstructors();
    }

    @Override
    public void visit(FieldDeclaration declaration) {
        if ((declaration.getFlags() & Const.ACC_STATIC) == 0) {
            declaration.getFieldDeclarators().accept(this);
        }
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {
        ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration)declaration;

        if (cfcd.getStatements() != null && cfcd.getStatements().isStatements()) {
            Statements statements = (Statements) cfcd.getStatements();
            ListIterator<Statement> iterator = statements.listIterator();
            SuperConstructorInvocationExpression superConstructorCall = searchSuperConstructorCall(iterator);

            if (superConstructorCall != null) {
                String internalTypeName = cfcd.getClassFile().getInternalTypeName();

                datas.add(new Data(cfcd, statements, iterator.nextIndex()));

                if (datas.size() == 1) {
                    int firstLineNumber;

                    if ((cfcd.getFlags() & Const.ACC_SYNTHETIC) != 0) {
                        firstLineNumber = UNKNOWN_LINE_NUMBER;
                    } else {
                        firstLineNumber = superConstructorCall.getLineNumber();

                        if ("()V".equals(superConstructorCall.getDescriptor()) && firstLineNumber != UNKNOWN_LINE_NUMBER && iterator.hasNext() && (lineNumber == UNKNOWN_LINE_NUMBER || lineNumber >= firstLineNumber)) {
                            searchFirstLineNumberVisitor.init();
                            iterator.next().accept(searchFirstLineNumberVisitor);
                            iterator.previous();

                            int ln = searchFirstLineNumberVisitor.getLineNumber();

                            if (ln != UNKNOWN_LINE_NUMBER && ln >= firstLineNumber) {
                                firstLineNumber = UNKNOWN_LINE_NUMBER;
                            }
                        }
                    }

                    initPutFields(internalTypeName, firstLineNumber, iterator);
                } else {
                    filterPutFields(internalTypeName, iterator);
                }
            }
        }
    }

    @Override
    public void visit(MethodDeclaration declaration) {
        lineNumber = ((ClassFileMethodDeclaration)declaration).getFirstLineNumber();
    }

    @Override
    public void visit(NewExpression expression) {
        safeAccept(expression.getParameters());
    }

    @Override
    public void visit(StaticInitializerDeclaration declaration) {}

    @Override
    public void visit(FieldDeclarator declaration) {
        fieldDeclarators.put(declaration.getName(), declaration);
    }

    @Override
    public void visit(LocalVariableReferenceExpression expression) {
        containsLocalVariableReference = true;
    }

    protected SuperConstructorInvocationExpression searchSuperConstructorCall(ListIterator<Statement> iterator) {
        Expression expression;
        while (iterator.hasNext()) {
            expression = iterator.next().getExpression();

            if (expression.isSuperConstructorInvocationExpression()) {
                return (SuperConstructorInvocationExpression)expression;
            }

            if (expression.isConstructorInvocationExpression()) {
                break;
            }
        }

        return null;
    }

    protected void initPutFields(String internalTypeName, int firstLineNumber, ListIterator<Statement> iterator) {
        Set<String> fieldNames = new HashSet<>();
        Expression expression = null;

        Statement statement;
        FieldReferenceExpression fre;
        String fieldName;
        while (iterator.hasNext()) {
            statement = iterator.next();

            if (!statement.isExpressionStatement()) {
                break;
            }

            expression = statement.getExpression();

            if (!expression.isBinaryOperatorExpression() || !"=".equals(expression.getOperator()) || !expression.getLeftExpression().isFieldReferenceExpression()) {
                break;
            }

            fre = (FieldReferenceExpression)expression.getLeftExpression();

            if (!fre.getInternalTypeName().equals(internalTypeName) || !fre.getExpression().isThisExpression()) {
                break;
            }

            fieldName = fre.getName();

            if (fieldNames.contains(fieldName)) {
                break;
            }

            containsLocalVariableReference = false;
            expression.getRightExpression().accept(this);

            if (containsLocalVariableReference) {
                break;
            }

            putFields.add(expression);
            fieldNames.add(fieldName);
            expression = null;
        }

        int lastLineNumber;

        if (expression == null) {
            lastLineNumber = firstLineNumber == UNKNOWN_LINE_NUMBER ? UNKNOWN_LINE_NUMBER : firstLineNumber+1;
        } else {
            lastLineNumber = expression.getLineNumber();
        }

        if (firstLineNumber < lastLineNumber) {
            Iterator<Expression> ite = putFields.iterator();

            int localLineNumber;
            while (ite.hasNext()) {
                localLineNumber = ite.next().getLineNumber();

                if (firstLineNumber <= localLineNumber && localLineNumber <= lastLineNumber) {
                    if (localLineNumber == lastLineNumber) {
                        lastLineNumber++;
                    }
                    ite.remove();
                }
            }
        }
    }

    protected void filterPutFields(String internalTypeName, ListIterator<Statement> iterator) {
        Iterator<Expression> putFieldIterator = putFields.iterator();
        int index = 0;

        Expression expression;
        FieldReferenceExpression fre;
        Expression putField;
        while (iterator.hasNext() && putFieldIterator.hasNext()) {
            expression = iterator.next().getExpression();

            if (!expression.isBinaryOperatorExpression() || !"=".equals(expression.getOperator()) || !expression.getLeftExpression().isFieldReferenceExpression()) {
                break;
            }

            fre = (FieldReferenceExpression) expression.getLeftExpression();

            if (!fre.getInternalTypeName().equals(internalTypeName)) {
                break;
            }

            putField = putFieldIterator.next();

            if (expression.getLineNumber() != putField.getLineNumber() || !fre.getName().equals(putField.getLeftExpression().getName())) {
                break;
            }

            index++;
        }

        if (index < putFields.size()) {
            // Cut extra putFields
            putFields.subList(index, putFields.size()).clear();
        }
    }

    protected void updateFieldsAndConstructors() {
        int count = putFields.size();

        if (count > 0) {
            FieldDeclarator declaration;
            // Init values
            for (Expression putField : putFields) {
                declaration = fieldDeclarators.get(putField.getLeftExpression().getName());

                if (declaration != null) {
                    Expression expression = putField.getRightExpression();
                    declaration.setVariableInitializer(new ExpressionVariableInitializer(expression));
                    ((ClassFileFieldDeclaration) declaration.getFieldDeclaration()).setFirstLineNumber(expression.getLineNumber());
                }
            }

            // Update data : remove init field statements
            for (Data data : datas) {
                data.statements.subList(data.index, data.index + count).clear();

                if (data.statements.isEmpty()) {
                    data.declaration.setStatements(null);
                    data.declaration.setFirstLineNumber(0);
                } else {
                    searchFirstLineNumberVisitor.init();
                    searchFirstLineNumberVisitor.visit(data.statements);

                    int firstLineNumber = searchFirstLineNumberVisitor.getLineNumber();

                    data.declaration.setFirstLineNumber(firstLineNumber==-1 ? 0 : firstLineNumber);
                }
            }
        }
    }

    protected static final class Data {
        private final ClassFileConstructorDeclaration declaration;
        private final Statements statements;
        private final int index;

        public Data(ClassFileConstructorDeclaration declaration, Statements statements, int index) {
            this.declaration = declaration;
            this.statements = statements;
            this.index = index;
        }
    }
}
