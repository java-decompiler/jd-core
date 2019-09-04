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
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileSuperConstructorInvocationExpression;
import org.jd.core.v1.util.DefaultList;

import java.util.*;

import static org.jd.core.v1.model.classfile.Constants.ACC_STATIC;

public class InitInnerClassVisitor extends AbstractJavaSyntaxVisitor {
    protected UpdateFieldReferencesVisitor updateFieldReferencesVisitor = new UpdateFieldReferencesVisitor();
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
            updateFieldReferencesVisitor.visit(bodyDeclaration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(ConstructorDeclaration declaration) {
        ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration)declaration;
        ClassFile classFile = cfcd.getClassFile();
        ClassFile outerClassFile = classFile.getOuterClassFile();

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

                    if (clazz == ClassFileSuperConstructorInvocationExpression.class) {
                        // 'super(...)'
                        break;
                    }

                    if (clazz == ClassFileConstructorInvocationExpression.class) {
                        // 'this(...)'
                        if ((outerClassFile != null) && ((classFile.getAccessFlags() & ACC_STATIC) == 0)) {
                            // Inner non-static class --> First parameter is the synthetic outer reference
                            outerType = (ObjectType) cfcd.getParameterTypes().getFirst();
                        }
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

        // Remove synthetic parameterTypes
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
            } else if ((outerType != null) || !outerLocalVariableNames.isEmpty()) {
                // Remove outer this and outer local variable reference
                cfcd.setFormalParameters(null);
            }
        }

        // Hide anonymous class constructor
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

    @Override public void visit(MethodDeclaration declaration) {}
    @Override public void visit(StaticInitializerDeclaration declaration) {}

    protected class UpdateFieldReferencesVisitor extends AbstractUpdateExpressionVisitor {
        protected ClassFileBodyDeclaration bodyDeclaration;

        @Override
        public void visit(BodyDeclaration declaration) {
            bodyDeclaration = (ClassFileBodyDeclaration)declaration;
            safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        }

        @Override public void visit(StaticInitializerDeclaration declaration) {}

        @Override
        public void visit(MethodDeclaration declaration) {
            safeAccept(declaration.getStatements());
        }

        @Override
        public void visit(NewExpression expression) {
            if (expression.getParameters() != null) {
                expression.setParameters(updateBaseExpression(expression.getParameters()));
                expression.getParameters().accept(this);
            }
            safeAccept(expression.getBodyDeclaration());
        }

        @Override
        public void visit(FieldReferenceExpression expression) {
            if (expression.getName().startsWith("this$")) {
                if (expression.getType().getDescriptor().equals(outerType.getDescriptor())) {
                    Expression exp = (expression.getExpression() == null) ? expression : expression.getExpression();
                    expression.setExpression(new ObjectTypeReferenceExpression(exp.getLineNumber(), outerType));
                    expression.setName("this");
                } else {
                    ClassFileMemberDeclaration memberDeclaration = bodyDeclaration.getInnerTypeDeclaration(expression.getInternalTypeName());

                    if ((memberDeclaration != null) && (memberDeclaration.getClass() == ClassFileClassDeclaration.class)) {
                        ClassFileClassDeclaration cfcd = (ClassFileClassDeclaration) memberDeclaration;

                        if (cfcd.getInternalTypeName().equals(expression.getInternalTypeName())) {
                            ClassFileBodyDeclaration cfbd = (ClassFileBodyDeclaration) cfcd.getBodyDeclaration();
                            String outerInternalTypeName = cfbd.getOuterBodyDeclaration().getInternalTypeName();
                            ObjectType objectType = (ObjectType)expression.getType();

                            if (outerInternalTypeName.equals(objectType.getInternalName())) {
                                Expression exp = (expression.getExpression() == null) ? expression : expression.getExpression();
                                expression.setExpression(new ObjectTypeReferenceExpression(exp.getLineNumber(), objectType));
                                expression.setName("this");
                            }
                        }
                    }
                }
            } else if (outerLocalVariableNames.contains(expression.getName())) {
                expression.setName(expression.getName().substring(4));
                expression.setExpression(null);
            } else {
                super.visit(expression);
            }
        }

        @Override
        protected Expression updateExpression(Expression expression) {
            if (expression.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                ClassFileLocalVariableReferenceExpression cdlvre = (ClassFileLocalVariableReferenceExpression) expression;

                if ((cdlvre.getName() != null) && cdlvre.getName().startsWith("this$") && cdlvre.getType().getDescriptor().equals(outerType.getDescriptor())) {
                    return new FieldReferenceExpression(outerType, new ObjectTypeReferenceExpression(cdlvre.getLineNumber(), outerType), outerType.getInternalName(), "this", outerType.getDescriptor());
                }
            }

            return expression;
        }
    }

    public static class UpdateNewExpressionVisitor extends AbstractJavaSyntaxVisitor {
        protected ClassFileBodyDeclaration bodyDeclaration;
        protected String superTypeName;
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
            superTypeName = ((ClassFileConstructorDeclaration)declaration).getClassFile().getSuperTypeName();
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
                        cfbd = (ClassFileBodyDeclaration) cfcd.getBodyDeclaration();

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
                    cfbd = (ClassFileBodyDeclaration) expression.getBodyDeclaration();
                }

                if (cfbd != null) {
                    BaseExpression parameters = expression.getParameters();

                    if (parameters != null) {
                        // Remove synthetic parameterTypes
                        DefaultList<String> outerParameterNames = cfbd.getOuterLocalVariableNames();

                        if (parameters.isList()) {
                            DefaultList<Expression> list = parameters.getList();

                            if (cfbd.getOuterType() != null) {
                                // Remove outer this
                                list.remove(0);
                            }

                            if (outerParameterNames != null) {
                                // Remove outer local variable reference
                                int size = list.size();
                                int count = outerParameterNames.size();
                                List<Expression> lastParameters = list.subList(size - count, size);
                                Iterator<Expression> parameterIterator = lastParameters.iterator();
                                Iterator<String> outerParameterNameIterator = outerParameterNames.iterator();

                                while (parameterIterator.hasNext()) {
                                    String outerParameterName = outerParameterNameIterator.next();
                                    Expression param = parameterIterator.next();

                                    if (param.getClass() == CastExpression.class) {
                                        param = ((CastExpression) param).getExpression();
                                    }

                                    if (param.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                                        String localVariableName = ((ClassFileLocalVariableReferenceExpression) param).getLocalVariable().getName();
                                        finalLocalVariableNameMap.put(localVariableName, outerParameterName.substring(4));
                                    }
                                }

                                lastParameters.clear();
                            }
                        } else if (cfbd.getOuterType() != null) {
                            // Remove outer this
                            expression.setParameters(null);
                        } else if (outerParameterNames != null) {
                            // Remove outer local variable reference
                            Expression param = parameters.getFirst();

                            if (param.getClass() == CastExpression.class) {
                                param = ((CastExpression) param).getExpression();
                            }

                            if (param.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                                String localVariableName = ((ClassFileLocalVariableReferenceExpression) param).getLocalVariable().getName();
                                String outerParameterName = outerParameterNames.get(0);
                                finalLocalVariableNameMap.put(localVariableName, outerParameterName.substring(4));
                                expression.setParameters(null);
                            }
                        }

                        // Is the last parameter synthetic ?
                        parameters = expression.getParameters();

                        if ((parameters != null) && (parameters.size() > 0) && (parameters.getLast().getClass() == NullExpression.class)) {
                            BaseType parameterTypes = ((ClassFileNewExpression) expression).getParameterTypes();

                            if (parameterTypes.getLast().getName() == null) {
                                // Yes. Remove it.
                                if (parameters.isList()) {
                                    parameters.getList().removeLast();
                                } else {
                                    expression.setParameters(null);
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void visit(SuperConstructorInvocationExpression expression) {
            BaseExpression parameters = expression.getParameters();

            if ((parameters != null) && (parameters.size() > 0)) {
                // Remove outer 'this' reference parameter
                ClassFileMemberDeclaration memberDeclaration = bodyDeclaration.getInnerTypeDeclaration(superTypeName);

                if ((memberDeclaration != null) && (memberDeclaration.getClass() == ClassFileClassDeclaration.class)) {
                    ClassFileClassDeclaration cfcd = (ClassFileClassDeclaration) memberDeclaration;
                    ClassFileBodyDeclaration cfbd = (ClassFileBodyDeclaration) cfcd.getBodyDeclaration();

                    if (parameters.getFirst().getType().equals(cfbd.getOuterType())) {
                        expression.setParameters(removeOuterThisParameter(parameters));
                    }
                }

                // Remove last synthetic parameter
                expression.setParameters(removeLastSyntheticParameter(
                        expression.getParameters(),
                        ((ClassFileSuperConstructorInvocationExpression)expression).getParameterTypes()));
            }
        }

        @Override
        public void visit(ConstructorInvocationExpression expression) {
            BaseExpression parameters = expression.getParameters();

            if ((parameters != null) && (parameters.size() > 0)) {
                // Remove outer this reference parameter
                if (parameters.getFirst().getType().equals(bodyDeclaration.getOuterType())) {
                    expression.setParameters(removeOuterThisParameter(parameters));
                }

                // Remove last synthetic parameter
                expression.setParameters(removeLastSyntheticParameter(
                        expression.getParameters(),
                        ((ClassFileConstructorInvocationExpression)expression).getParameterTypes()));
            }
        }

        protected BaseExpression removeOuterThisParameter(BaseExpression parameters) {
            // Remove outer 'this' reference parameter
            if (parameters.isList()) {
                parameters.getList().removeFirst();
            } else {
                parameters = null;
            }

            return parameters;
        }

        protected BaseExpression removeLastSyntheticParameter(BaseExpression parameters, BaseType parameterTypes) {
            // Is the last parameter synthetic ?
            if ((parameters != null) && (parameters.size() > 0) && (parameters.getLast().getClass() == NullExpression.class)) {
                if (parameterTypes.getLast().getName() == null) {
                    // Yes. Remove it.
                    if (parameters.isList()) {
                        parameters.getList().removeLast();
                    } else {
                        parameters = null;
                    }
                }
            }

            return parameters;
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
