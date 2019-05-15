/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.classfile.attribute.LocalVariableType;
import org.jd.core.v1.model.javasyntax.type.AbstractNopTypeVisitor;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.LocalVariableSet;

public class UpdateTypeVisitor extends AbstractNopTypeVisitor {
    protected LocalVariableSet localVariableSet;
    protected LocalVariableType localVariableType;

    public UpdateTypeVisitor(LocalVariableSet localVariableSet) {
        this.localVariableSet = localVariableSet;
    }

    public void setLocalVariableType(LocalVariableType localVariableType) {
        this.localVariableType = localVariableType;
    }

    @Override
    public void visit(ObjectType type) {
        localVariableSet.update(localVariableType.getIndex(), localVariableType.getStartPc(), type);
    }

    @Override
    public void visit(InnerObjectType type) {
        localVariableSet.update(localVariableType.getIndex(), localVariableType.getStartPc(), type);
    }

    @Override
    public void visit(GenericType type) {
        localVariableSet.update(localVariableType.getIndex(), localVariableType.getStartPc(), type);
    }
}
