/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration;

import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.LocalVariableReference;

public class ClassFileFormalParameter extends FormalParameter implements LocalVariableReference {
    private AbstractLocalVariable localVariable;

    public ClassFileFormalParameter(AbstractLocalVariable localVariable, boolean varargs) {
        super(null, varargs, null);
        this.localVariable = localVariable;
    }

    public ClassFileFormalParameter(BaseAnnotationReference annotationReferences, AbstractLocalVariable localVariable, boolean varargs) {
        super(annotationReferences, null, varargs, null);
        this.localVariable = localVariable;
    }

    @Override
    public Type getType() {
        return localVariable.getType();
    }

    @Override
    public String getName() {
        return localVariable.getName();
    }

    @Override
    public void setName(String name) {
        localVariable.setName(name);
    }

    @Override
    public AbstractLocalVariable getLocalVariable() {
        return localVariable;
    }

    @Override
    public void setLocalVariable(AbstractLocalVariable localVariable) {
        this.localVariable = localVariable;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("ClassFileFormalParameter{");

        if (annotationReferences != null) {
            s.append(annotationReferences).append(" ");
        }

        Type type = localVariable.getType();

        if (varargs) {
            s.append(type.createType(type.getDimension()-1)).append("... ");
        } else {
            s.append(type).append(" ");
        }

        return s.append(localVariable.getName()).append("}").toString();
    }
}
