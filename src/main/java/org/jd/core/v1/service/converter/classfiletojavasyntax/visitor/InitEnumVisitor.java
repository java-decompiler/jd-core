/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.*;
import org.jd.core.v1.util.DefaultList;

import java.util.Comparator;

import static org.jd.core.v1.model.javasyntax.declaration.Declaration.*;


public class InitEnumVisitor extends AbstractJavaSyntaxVisitor {
    protected DefaultList<ClassFileEnumDeclaration.ClassFileConstant> constants = new DefaultList<>();
    protected int lineNumber;
    protected int index;
    protected BaseExpression arguments;

    @SuppressWarnings("unchecked")
    public DefaultList<EnumDeclaration.Constant> getConstants() {
        if (!constants.isEmpty()) {
            constants.sort(new EnumConstantComparator());
        }

        return new DefaultList(constants);
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;
        constants.clear();
        safeAcceptListDeclaration(bodyDeclaration.getFieldDeclarations());
        safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {
        ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration)declaration;

        if (cfcd.getStatements().getClass() == Statements.class) {
            Statements statements = (Statements)cfcd.getStatements();

            if (statements.size() == 1) {
                cfcd.setFlags(FLAG_SYNTHETIC);
            } else {
                FormalParameters parameters = (FormalParameters)cfcd.getFormalParameters();
                // Remove name & index parameters
                parameters.subList(0, 2).clear();
                // Remove super constructor call
                statements.remove(0);
                // Fix flags
                cfcd.setFlags(0);
            }
        } else {
            cfcd.setFlags(FLAG_SYNTHETIC);
        }
    }

    @Override
    public void visit(StaticInitializerDeclaration declaration) {}

    @Override
    public void visit(MethodDeclaration declaration) {
        if ((declaration.getFlags() & (FLAG_STATIC|FLAG_PUBLIC)) != 0) {
            if (declaration.getName().equals("values") || declaration.getName().equals("valueOf")) {
                ClassFileMethodDeclaration cfmd = (ClassFileMethodDeclaration)declaration;
                cfmd.setFlags(cfmd.getFlags() | FLAG_SYNTHETIC);
            }
        }
    }

    @Override
    public void visit(FieldDeclaration declaration) {
        if ((declaration.getFlags() & FLAG_ENUM) != 0) {
            ClassFileFieldDeclaration cffd = (ClassFileFieldDeclaration)declaration;
            cffd.getFieldDeclarators().accept(this);
            cffd.setFlags(cffd.getFlags() | FLAG_SYNTHETIC);
        }
    }

    @Override
    public void visit(FieldDeclarator declaration) {
        safeAccept(declaration.getVariableInitializer());
        constants.add(new ClassFileEnumDeclaration.ClassFileConstant(lineNumber, declaration.getName(), index, arguments));
    }

    @Override
    public void visit(NewExpression expression) {
        Expressions parameters = (Expressions)expression.getParameters();
        IntegerConstantExpression ice = (IntegerConstantExpression)parameters.get(1);

        lineNumber = expression.getLineNumber();
        index = ice.getValue();

        // Remove name & index
        if (parameters.size() == 2) {
            arguments = null;
        } else {
            parameters.subList(0, 2).clear();
            arguments = parameters;
        }
    }

    protected static class EnumConstantComparator implements Comparator<ClassFileEnumDeclaration.ClassFileConstant> {
        public int compare(ClassFileEnumDeclaration.ClassFileConstant ec1, ClassFileEnumDeclaration.ClassFileConstant ec2) {
            return ec1.getIndex() - ec2.getIndex();
        }
    }
}
