/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.AttributeCode;
import org.jd.core.v1.model.classfile.attribute.AttributeLineNumberTable;
import org.jd.core.v1.model.classfile.attribute.LineNumber;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;

public class InitInstanceFieldVisitor extends AbstractJavaSyntaxVisitor {
    protected SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
    protected HashMap<String, FieldDeclarator> fieldDeclarators = new HashMap<>();
    protected DefaultList<Data> datas = new DefaultList<>();
    protected DefaultList<BinaryOperatorExpression> putFields = new DefaultList<>();
    protected boolean containsLocalVariableReference;

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
        if ((declaration.getFlags() & FieldDeclaration.FLAG_STATIC) == 0) {
            declaration.getFieldDeclarators().accept(this);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(ConstructorDeclaration declaration) {
        ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration)declaration;

        if (cfcd.getStatements().getClass() == Statements.class) {
            Statements statements = (Statements) cfcd.getStatements();
            ListIterator<Statement> iterator = statements.listIterator();
            Expression superConstructorCall = searchSuperConstructorCall(iterator);

            if (superConstructorCall != null) {
                String internalTypeName = cfcd.getClassFile().getInternalTypeName();

                datas.add(new Data(cfcd, statements, iterator.nextIndex()));

                if (datas.size() == 1) {
                    initPutFields(internalTypeName, cfcd, iterator);
                } else {
                    filterPutFields(internalTypeName, iterator);
                }
            }
        }
    }

    @Override
    public void visit(MethodDeclaration declaration) {}

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

    protected Expression searchSuperConstructorCall(ListIterator<Statement> iterator) {
        while (iterator.hasNext()) {
            Statement statement = iterator.next();

            if (statement.getClass() == ExpressionStatement.class) {
                Expression expression = ((ExpressionStatement)statement).getExpression();
                Class clazz = expression.getClass();

                if (clazz == SuperConstructorInvocationExpression.class) {
                    return expression;
                }

                if (clazz == ConstructorInvocationExpression.class) {
                    break;
                }
            }
        }

        return null;
    }

    protected void initPutFields(String internalTypeName, ClassFileConstructorDeclaration cfcd, ListIterator<Statement> iterator) {
        Method method = cfcd.getMethod();
        HashSet<String> fieldNames = new HashSet<>();
        int lineNumberBefore = 0;
        int lineNumberAfter = 0;
        Expression expression = null;
        AttributeCode attributeCode = method.getAttribute("Code");

        if (attributeCode != null) {
            AttributeLineNumberTable lineNumberTable = attributeCode.getAttribute("LineNumberTable");

            if (lineNumberTable != null) {
                LineNumber[] lineNumbers = lineNumberTable.getLineNumberTable();
                lineNumberBefore = lineNumbers[0].getLineNumber();
                lineNumberAfter = lineNumbers[lineNumbers.length - 1].getLineNumber();

                for (ClassFileConstructorOrMethodDeclaration cfcomd : cfcd.getBodyDeclaration().getMethodDeclarations()) {
                    if ((cfcomd.getFirstLineNumber() > lineNumberBefore) && (cfcomd.getFirstLineNumber() < lineNumberAfter)) {
                        lineNumberAfter = Printer.UNKNOWN_LINE_NUMBER;
                        break;
                    }
                }
            }
        }

        while (iterator.hasNext()) {
            Statement statement = iterator.next();

            if (statement.getClass() != ExpressionStatement.class) {
                break;
            }

            expression = ((ExpressionStatement)statement).getExpression();

            if (expression.getClass() != BinaryOperatorExpression.class) {
                break;
            }

            BinaryOperatorExpression cfboe = (BinaryOperatorExpression)expression;

            if (!cfboe.getOperator().equals("=") || (cfboe.getLeftExpression().getClass() != FieldReferenceExpression.class)) {
                break;
            }

            FieldReferenceExpression fre = (FieldReferenceExpression)cfboe.getLeftExpression();

            if (!fre.getInternalTypeName().equals(internalTypeName) || (fre.getExpression().getClass() != ThisExpression.class)) {
                break;
            }

            String fieldName = fre.getName();

            if (fieldNames.contains(fieldName)) {
                break;
            }

            containsLocalVariableReference = false;
            cfboe.getRightExpression().accept(this);

            if (containsLocalVariableReference) {
                break;
            }

            putFields.add(cfboe);
            fieldNames.add(fieldName);
        }

        if ((lineNumberAfter != Printer.UNKNOWN_LINE_NUMBER) && (expression != null) && (expression.getLineNumber() != lineNumberAfter)) {
            Iterator<BinaryOperatorExpression> ite = putFields.iterator();

            while (ite.hasNext()) {
                int lineNumber = ite.next().getLineNumber();

                if ((lineNumberBefore <= lineNumber) && (lineNumber <= lineNumberAfter)) {
                    ite.remove();
                }
            }
        }
    }

    protected void filterPutFields(String internalTypeName, ListIterator<Statement> iterator) {
        Iterator<BinaryOperatorExpression> putFieldIterator = putFields.iterator();
        int index = 0;

        while (iterator.hasNext() && putFieldIterator.hasNext()) {
            Statement statement = iterator.next();

            if (statement.getClass() != ExpressionStatement.class) {
                break;
            }

            Expression expression = ((ExpressionStatement)statement).getExpression();

            if (expression.getClass() != BinaryOperatorExpression.class) {
                break;
            }

            BinaryOperatorExpression cfboe = (BinaryOperatorExpression)expression;

            if (!cfboe.getOperator().equals("=") || (cfboe.getLeftExpression().getClass() != FieldReferenceExpression.class)) {
                break;
            }

            FieldReferenceExpression fre = (FieldReferenceExpression)cfboe.getLeftExpression();

            if (!fre.getInternalTypeName().equals(internalTypeName)) {
                break;
            }

            BinaryOperatorExpression putField = putFieldIterator.next();

            if (cfboe.getLineNumber() != putField.getLineNumber()) {
                break;
            }

            FieldReferenceExpression putFieldFre = (FieldReferenceExpression)putField.getLeftExpression();

            if (!fre.getName().equals(putFieldFre.getName())) {
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
            // Init values
            for (BinaryOperatorExpression putField : putFields) {
                FieldReferenceExpression fre = (FieldReferenceExpression) putField.getLeftExpression();
                FieldDeclarator declaration = fieldDeclarators.get(fre.getName());

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

                    data.declaration.setFirstLineNumber((firstLineNumber==-1) ? 0 : firstLineNumber);
                }
            }
        }
    }

    protected static final class Data {
        public ClassFileConstructorDeclaration declaration;
        public Statements statements;
        public int index;

        public Data(ClassFileConstructorDeclaration declaration, Statements statements, int index) {
            this.declaration = declaration;
            this.statements = statements;
            this.index = index;
        }
    }
}
