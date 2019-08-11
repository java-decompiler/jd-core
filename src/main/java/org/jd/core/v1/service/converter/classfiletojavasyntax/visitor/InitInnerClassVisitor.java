/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Constants;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.statement.*;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.util.DefaultList;

import java.util.*;

public class InitInnerClassVisitor extends AbstractJavaSyntaxVisitor {
    protected UpdateReferencesVisitor updateReferencesVisitor = new UpdateReferencesVisitor();
    protected UpdateNewExpressionVisitor updateNewExpressionVisitor = new UpdateNewExpressionVisitor();
    protected ObjectType outerType;
    protected DefaultList<String> outerLocalVariableNames = new DefaultList<>();

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
        outerType = null;
        outerLocalVariableNames.clear();
        // Visit methods
        safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        // Init values
        bodyDeclaration.setOuterType(outerType);
        bodyDeclaration.setOuterLocalVariableNames(outerLocalVariableNames.isEmpty() ? null : new DefaultList<>(outerLocalVariableNames));

        if ((outerType != null) || !outerLocalVariableNames.isEmpty()) {
            updateReferencesVisitor.visit(bodyDeclaration);
        }

        if (bodyDeclaration.getInnerTypeDeclarations() != null) {
            updateNewExpressionVisitor.visit(bodyDeclaration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(ConstructorDeclaration declaration) {
        ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration)declaration;

        outerLocalVariableNames.clear();

        // Search synthetic field initialization
        if (cfcd.getStatements().isList()) {
            DefaultList<Statement> statements = cfcd.getStatements().getList();
            Iterator<Statement> iterator = statements.iterator();

            while (iterator.hasNext()) {
                Statement statement = iterator.next();

                if (statement.getClass() == ExpressionStatement.class) {
                    Expression expression = ((ExpressionStatement) statement).getExpression();
                    Class clazz = expression.getClass();

                    if (clazz == SuperConstructorInvocationExpression.class) {
                        break;
                    }

                    if (clazz == ConstructorInvocationExpression.class) {
                        break;
                    }

                    if (clazz == BinaryOperatorExpression.class) {
                        BinaryOperatorExpression boe = (BinaryOperatorExpression) expression;
                        Expression e = boe.getLeftExpression();

                        if (e.getClass() == FieldReferenceExpression.class) {
                            String name = ((FieldReferenceExpression)e).getName();

                            if (name.startsWith("this$")) {
                                outerType = (ObjectType) boe.getRightExpression().getType();
                            } else if (name.startsWith("val$")) {
                                outerLocalVariableNames.add(name);
                            }
                        }
                    }
                }

                iterator.remove();
            }
        }

        // Remove synthetic parameters
        BaseFormalParameter parameters = cfcd.getFormalParameters();

        if (parameters != null) {
            if (parameters.isList()) {
                List<FormalParameter> list = parameters.getList();

                if (outerType != null) {
                    // Remove outer this
                    list.remove(0);
                }

                int count = outerLocalVariableNames.size();

                if (count > 0) {
                    // Remove outer local variable reference
                    int size = list.size();
                    list.subList(size - count, size).clear();
                }
            } else if (outerType != null) {
                cfcd.setFormalParameters(null);
            }
        }

        ClassFile outerClassFile = cfcd.getClassFile().getOuterClassFile();

        if (outerClassFile != null) {
            String outerTypeName = outerClassFile.getInternalTypeName();
            String internalTypeName = cfcd.getClassFile().getInternalTypeName();
            int min;

            if (internalTypeName.startsWith(outerTypeName + '$')) {
                min = outerTypeName.length() + 1;
            } else {
                min = internalTypeName.lastIndexOf('$') + 1;
            }

            if (Character.isDigit(internalTypeName.charAt(min))) {
                int i = internalTypeName.length();
                boolean anonymousFlag = true;

                while (--i > min) {
                    if (!Character.isDigit(internalTypeName.charAt(i))) {
                        anonymousFlag = false;
                        break;
                    }
                }

                if (anonymousFlag) {
                    // Hide anonymous class constructor
                    cfcd.setFlags(cfcd.getFlags() | Constants.ACC_SYNTHETIC);
                }
            }
        }
    }

    @Override
    public void visit(MethodDeclaration declaration) {}

    @Override
    public void visit(StaticInitializerDeclaration declaration) {}

    protected class UpdateReferencesVisitor extends AbstractJavaSyntaxVisitor {
        @Override
        public void visit(BodyDeclaration declaration) {
            ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;
            safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        }

        @Override
        public void visit(MethodDeclaration declaration) {
            safeAccept(declaration.getStatements());
        }

        @Override
        public void visit(StaticInitializerDeclaration declaration) {
            safeAccept(declaration.getStatements());
        }

        @Override
        public void visit(SuperConstructorInvocationExpression expression) {
            SuperConstructorInvocationExpression cfscie = expression;

            if (cfscie.getParameters() != null) {
                if (cfscie.getParameters().isList()) {
                    visitParameters(cfscie.getParameters().getList());
                } else {
                    cfscie.setParameters(visitParameter(cfscie.getParameters().getFirst()));
                }
            }
        }

        @Override
        public void visit(ConstructorInvocationExpression expression) {
            ConstructorInvocationExpression cie = expression;

            assert cie.getParameters() != null;

            if (cie.getParameters().isList()) {
                DefaultList<Expression> parameters = cie.getParameters().getList();

                parameters.remove(0);
                assert parameters.size() > 0;

                if (parameters.size() == 1) {
                    cie.setParameters(visitParameter(parameters.getFirst()));
                } else {
                    visitParameters(parameters);
                }
            } else {
                cie.setParameters(null);
            }
        }

        @SuppressWarnings("unchecked")
        protected void visitParameters(DefaultList<Expression> list) {
            ListIterator<Expression> iterator = list.listIterator();

            while (iterator.hasNext()) {
                iterator.set(visitParameter(iterator.next()));
            }
        }

        protected Expression visitParameter(Expression expression) {
            if (expression.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                ClassFileLocalVariableReferenceExpression cflvre = (ClassFileLocalVariableReferenceExpression)expression;

                if (outerLocalVariableNames.contains(cflvre.getName())) {
                    return new FieldReferenceExpression(cflvre.getType(), new ObjectTypeReferenceExpression(cflvre.getLineNumber(), outerType), outerType.getInternalName(), cflvre.getName().substring(4), cflvre.getType().getDescriptor());
                } else if ((cflvre.getName() != null) && cflvre.getName().startsWith("this$") && cflvre.getType().getDescriptor().equals(outerType.getDescriptor())) {
                    return new FieldReferenceExpression(outerType, new ObjectTypeReferenceExpression(cflvre.getLineNumber(), outerType), outerType.getInternalName(), "this", outerType.getDescriptor());
                }
            } else if (expression.getClass() == FieldReferenceExpression.class) {
                expression.accept(this);
            }

            return expression;
        }

        @Override
        public void visit(FieldReferenceExpression expression) {
            FieldReferenceExpression cffre = expression;

            if (outerLocalVariableNames.contains(expression.getName())) {
                cffre.setName(cffre.getName().substring(4));
                cffre.setExpression(null);
            } else if (cffre.getExpression() != null) {
                Class clazz = cffre.getExpression().getClass();

                if (clazz == FieldReferenceExpression.class) {
                    FieldReferenceExpression cffre2 = (FieldReferenceExpression) cffre.getExpression();

                    if (cffre2.getName().startsWith("this$") && cffre2.getDescriptor().equals(outerType.getDescriptor())) {
                        cffre.setExpression(new FieldReferenceExpression(outerType, new ObjectTypeReferenceExpression(cffre2.getLineNumber(), outerType), outerType.getInternalName(), "this", outerType.getDescriptor()));
                    }
                } else if (clazz == ClassFileLocalVariableReferenceExpression.class) {
                    ClassFileLocalVariableReferenceExpression cdlvre = (ClassFileLocalVariableReferenceExpression) cffre.getExpression();

                    if ((cdlvre.getName() != null) && cdlvre.getName().startsWith("this$") && cdlvre.getType().getDescriptor().equals(outerType.getDescriptor())) {
                        cffre.setExpression(new FieldReferenceExpression(outerType, new ObjectTypeReferenceExpression(cdlvre.getLineNumber(), outerType), outerType.getInternalName(), "this", outerType.getDescriptor()));
                    }
                } else if (clazz == ThisExpression.class) {
                    if (cffre.getName().startsWith("this$") && cffre.getType().getDescriptor().equals(outerType.getDescriptor())) {
                        cffre.setExpression(new ObjectTypeReferenceExpression(cffre.getExpression().getLineNumber(), outerType));
                        cffre.setName("this");
                    }
                }
            }
        }

        @Override
        public void visit(NewExpression expression) {
            safeAccept(expression.getParameters());
            if (expression.getBodyDeclaration() != null) {
                ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)expression.getBodyDeclaration();

                for (ClassFileConstructorOrMethodDeclaration comd : bodyDeclaration.getMethodDeclarations()) {
                    safeAccept(comd.getStatements());
                }
            }
        }
    }

    protected class UpdateNewExpressionVisitor extends AbstractJavaSyntaxVisitor {
        protected ClassFileBodyDeclaration bodyDeclaration;
        protected HashMap<String, String> finalLocalVariableNameMap = new HashMap<>();
        protected DefaultList<ClassFileClassDeclaration> localClassDeclarations = new DefaultList<>();
        protected HashSet<NewExpression> newExpressions = new HashSet<>();
        protected int lineNumber;

        @Override
        public void visit(BodyDeclaration declaration) {
            bodyDeclaration = (ClassFileBodyDeclaration)declaration;
            safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        }

        @Override
        public void visit(ConstructorDeclaration declaration) {
            finalLocalVariableNameMap.clear();
            localClassDeclarations.clear();
            safeAccept(declaration.getStatements());

            if (! finalLocalVariableNameMap.isEmpty()) {
                UpdateFinalFieldReferenceVisitor visitor = new UpdateFinalFieldReferenceVisitor();

                declaration.getStatements().accept(visitor);

                if (declaration.getFormalParameters() != null) {
                    declaration.getFormalParameters().accept(visitor);
                }
            }

            if (! localClassDeclarations.isEmpty()) {
                localClassDeclarations.sort(new MemberDeclarationComparator());
                declaration.accept(new AddLocalClassDeclarationVisitor());
            }
        }

        @Override
        public void visit(MethodDeclaration declaration) {
            finalLocalVariableNameMap.clear();
            localClassDeclarations.clear();
            safeAccept(declaration.getStatements());

            if (! finalLocalVariableNameMap.isEmpty()) {
                UpdateFinalFieldReferenceVisitor visitor = new UpdateFinalFieldReferenceVisitor();

                declaration.getStatements().accept(visitor);

                if (declaration.getFormalParameters() != null) {
                    declaration.getFormalParameters().accept(visitor);
                }
            }

            if (! localClassDeclarations.isEmpty()) {
                localClassDeclarations.sort(new MemberDeclarationComparator());
                declaration.accept(new AddLocalClassDeclarationVisitor());
            }
        }

        @Override
        public void visit(StaticInitializerDeclaration declaration) {
            finalLocalVariableNameMap.clear();
            localClassDeclarations.clear();
            safeAccept(declaration.getStatements());

            if (! finalLocalVariableNameMap.isEmpty()) {
                declaration.getStatements().accept(new UpdateFinalFieldReferenceVisitor());
            }

            if (! localClassDeclarations.isEmpty()) {
                localClassDeclarations.sort(new MemberDeclarationComparator());
                declaration.accept(new AddLocalClassDeclarationVisitor());
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void visit(Statements list) {
            if (!list.isEmpty()) {
                ListIterator<Statement> iterator = list.listIterator();

                while (iterator.hasNext()) {
                    //iterator.next().accept(this);
                    Statement s = iterator.next();
                    s.accept(this);

                    if ((lineNumber == Expression.UNKNOWN_LINE_NUMBER) && !localClassDeclarations.isEmpty()) {
                        iterator.previous();

                        for (TypeDeclaration typeDeclaration : localClassDeclarations) {
                            iterator.add(new TypeDeclarationStatement(typeDeclaration));
                        }

                        localClassDeclarations.clear();
                        iterator.next();
                    }
                }
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void visit(NewExpression expression) {
            if (!newExpressions.contains(expression)) {
                newExpressions.add(expression);

                ClassFileBodyDeclaration cfbd = null;

                if (expression.getBodyDeclaration() == null) {
                    ObjectType type = expression.getObjectType();
                    String internalName = type.getInternalName();
                    ClassFileMemberDeclaration memberDeclaration = bodyDeclaration.getInnerTypeDeclaration(internalName);

                    if ((memberDeclaration != null) && (memberDeclaration.getClass() == ClassFileClassDeclaration.class)) {
                        ClassFileClassDeclaration cfcd = (ClassFileClassDeclaration) memberDeclaration;
                        cfbd = (ClassFileBodyDeclaration)cfcd.getBodyDeclaration();

                        if ((type.getQualifiedName() == null) && (type.getName() != null)) {
                            // Local class
                            cfcd.setFlags(cfcd.getFlags() & (~Declaration.FLAG_SYNTHETIC));
                            localClassDeclarations.add(cfcd);
                            bodyDeclaration.removeInnerType(internalName);
                            lineNumber = expression.getLineNumber();
                        }
                    }
                } else {
                    // Anonymous class
                    cfbd = (ClassFileBodyDeclaration)expression.getBodyDeclaration();
                }

                if (cfbd != null) {
                    BaseExpression parameters = expression.getParameters();

                    if (parameters != null) {
                        // Remove synthetic parameters
                        DefaultList<String> outerParameterNames = cfbd.getOuterLocalVariableNames();

                        if (parameters.isList()) {
                            DefaultList<Expression> list = parameters.getList();

                            if (cfbd.getOuterType() != null) {
                                // Remove outer this
                                list.remove(0);
                            }

                            // Remove outer local variable reference
                            if (outerParameterNames != null) {
                                int size = list.size();
                                int count = outerParameterNames.size();
                                List<Expression> lastParameters = list.subList(size - count, size);
                                Iterator<Expression> parameterIterator = lastParameters.iterator();
                                Iterator<String> outerParameterNameIterator = outerParameterNames.iterator();

                                while (parameterIterator.hasNext()) {
                                    Expression param = parameterIterator.next();
                                    String outerParameterName = outerParameterNameIterator.next();

                                    if (param.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                                        String localVariableName = ((ClassFileLocalVariableReferenceExpression) param).getLocalVariable().getName();
                                        finalLocalVariableNameMap.put(localVariableName, outerParameterName.substring(4));
                                    }
                                }

                                lastParameters.clear();
                            }
                        } else if (parameters.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                            if (outerParameterNames != null) {
                                expression.setParameters(null);
                                String localVariableName = ((ClassFileLocalVariableReferenceExpression) parameters).getLocalVariable().getName();
                                String outerParameterName = outerParameterNames.get(0);
                                finalLocalVariableNameMap.put(localVariableName, outerParameterName.substring(4));
                            }
                        } else if (parameters.getClass() == ThisExpression.class) {
                            if (cfbd.getOuterType() != null) {
                                expression.setParameters(null);
                            }
                        }
                    }
                }
            }
        }

        protected class UpdateFinalFieldReferenceVisitor extends AbstractJavaSyntaxVisitor {
            protected boolean fina1;

            @Override
            public void visit(FormalParameter declaration) {
                if (finalLocalVariableNameMap.containsKey(declaration.getName())) {
                    declaration.setFinal(true);
                    declaration.setName(finalLocalVariableNameMap.get(declaration.getName()));
                }
            }

            @Override
            public void visit(LocalVariableDeclarationStatement statement) {
                fina1 = false;
                statement.getLocalVariableDeclarators().accept(this);
                if (fina1) {
                    statement.setFinal(true);
                }
            }

            @Override
            public void visit(LocalVariableDeclaration declaration) {
                fina1 = false;
                declaration.getLocalVariableDeclarators().accept(this);
                if (fina1) {
                    declaration.setFinal(true);
                }
            }

            @Override
            public void visit(LocalVariableDeclarator declarator) {
                if (finalLocalVariableNameMap.containsKey(declarator.getName())) {
                    fina1 = true;
                    declarator.setName(finalLocalVariableNameMap.get(declarator.getName()));
                }
            }
        }

        protected class AddLocalClassDeclarationVisitor extends AbstractJavaSyntaxVisitor {
            protected SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
            protected int lineNumber = Expression.UNKNOWN_LINE_NUMBER;

            @Override
            public void visit(ConstructorDeclaration declaration) {
                ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration)declaration;
                cfcd.setStatements(addLocalClassDeclarations(cfcd.getStatements()));
            }

            @Override
            public void visit(MethodDeclaration declaration) {
                ClassFileMethodDeclaration cfmd = (ClassFileMethodDeclaration)declaration;
                cfmd.setStatements(addLocalClassDeclarations(cfmd.getStatements()));
            }

            @Override
            public void visit(StaticInitializerDeclaration declaration) {
                ClassFileStaticInitializerDeclaration cfsid = (ClassFileStaticInitializerDeclaration)declaration;
                cfsid.setStatements(addLocalClassDeclarations(cfsid.getStatements()));
            }

            @SuppressWarnings("unchecked")
            protected BaseStatement addLocalClassDeclarations(BaseStatement statements) {
                if (!localClassDeclarations.isEmpty()) {
                    if (statements.getClass() == Statements.class) {
                        statements.accept(this);
                    } else {
                        ClassFileClassDeclaration declaration = localClassDeclarations.get(0);

                        searchFirstLineNumberVisitor.init();
                        statements.accept(searchFirstLineNumberVisitor);

                        if (searchFirstLineNumberVisitor.getLineNumber() != -1) {
                            lineNumber = searchFirstLineNumberVisitor.getLineNumber();
                        }

                        if (declaration.getFirstLineNumber() <= lineNumber) {
                            Statements list = new Statements();
                            Iterator<ClassFileClassDeclaration> declarationIterator = localClassDeclarations.iterator();

                            list.add(new TypeDeclarationStatement(declaration));
                            declarationIterator.next();
                            declarationIterator.remove();

                            while (declarationIterator.hasNext() && ((declaration = declarationIterator.next()).getFirstLineNumber() <= lineNumber)) {
                                list.add(new TypeDeclarationStatement(declaration));
                                declarationIterator.remove();
                            }

                            list.add(statements);
                            statements = list;
                        } else {
                            statements.accept(this);
                        }
                    }
                }

                return statements;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void visit(Statements list) {
                if (!localClassDeclarations.isEmpty() && !list.isEmpty()) {
                    ListIterator<Statement> statementIterator = list.listIterator();
                    Iterator<ClassFileClassDeclaration> declarationIterator = localClassDeclarations.iterator();
                    ClassFileClassDeclaration declaration = declarationIterator.next();

                    while (statementIterator.hasNext()) {
                        Statement statement = statementIterator.next();

                        searchFirstLineNumberVisitor.init();
                        statement.accept(searchFirstLineNumberVisitor);

                        if (searchFirstLineNumberVisitor.getLineNumber() != -1) {
                            lineNumber = searchFirstLineNumberVisitor.getLineNumber();
                        }

                        while (declaration.getFirstLineNumber() <= lineNumber) {
                            statementIterator.previous();
                            statementIterator.add(new TypeDeclarationStatement(declaration));
                            statementIterator.next();
                            declarationIterator.remove();

                            if (!declarationIterator.hasNext()) {
                                return;
                            }

                            declaration = declarationIterator.next();
                        }
                    }
                }
            }
        }

        protected class MemberDeclarationComparator implements Comparator<ClassFileMemberDeclaration> {
            public int compare(ClassFileMemberDeclaration md1, ClassFileMemberDeclaration md2) {
                return md1.getFirstLineNumber() - md2.getFirstLineNumber();
            }
        }
    }
}
