/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

public class ClassFileLocalVariableReferenceExpression extends LocalVariableReferenceExpression {
    protected AbstractLocalVariable localVariable;

    public ClassFileLocalVariableReferenceExpression(int lineNumber, AbstractLocalVariable localVariable) {
        super(lineNumber, null, null);
        this.localVariable = localVariable;
        localVariable.addReference(this);
    }

    @Override
    public Type getType() {
        return localVariable.getType();
    }

    @Override
    public String getName() {
        return localVariable.getName();
    }

    public AbstractLocalVariable getLocalVariable() {
        return localVariable;
    }

    public void setLocalVariable(AbstractLocalVariable localVariable) {
        this.localVariable = localVariable;
    }

    @Override
    public String toString() {
        return "ClassFileLocalVariableReferenceExpression{type=" + localVariable.getType() + ", name=" + localVariable.getName() + "}";
    }
}
