/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

import java.util.ArrayDeque;
import java.util.Deque;

public class SearchLocalVariableReferenceVisitor extends AbstractJavaSyntaxVisitor {
    private int index;
    private boolean found;
    private String name;
    private Deque<LambdaIdentifiersExpression> lambdas = new ArrayDeque<>();

    public void init(int index, String name) {
        this.index = index;
        this.name = name;
        this.found = false;
    }

    public void init(AbstractLocalVariable localVariable) {
        init(localVariable.getIndex(), localVariable.getName());
    }

    public boolean containsReference() {
        return found;
    }

    @Override
    public void visit(LocalVariableReferenceExpression expression) {
        if (index < 0) {
            found = !isLambdaParameter(expression.getName());
        } else {
            ClassFileLocalVariableReferenceExpression referenceExpression = (ClassFileLocalVariableReferenceExpression) expression;
            if (lambdas.isEmpty()) {
                found |= referenceExpression.getLocalVariable().getIndex() == index;
            } else {
                found |= referenceExpression.getLocalVariable().getName().equals(name);
            }
        }
    }
    
    @Override
    public void visit(LambdaIdentifiersExpression expression) {
        lambdas.push(expression);
        super.visit(expression);
        lambdas.pop();
    }
    
    private boolean isLambdaParameter(String name) {
        for (LambdaIdentifiersExpression lambda : lambdas) {
            if (lambda.getParameterNames().contains(name)) {
                return true;
            }
        }
        return false;
    }
}
