/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.classfile.attribute.LocalVariableType;
import org.jd.core.v1.model.javasyntax.type.AbstractNopTypeArgumentVisitor;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.LocalVariableSet;

public class UpdateTypeVisitor extends AbstractNopTypeArgumentVisitor {
    private final UpdateClassTypeArgumentsVisitor updateClassTypeArgumentsVisitor = new UpdateClassTypeArgumentsVisitor();
    private final LocalVariableSet localVariableSet;
    private LocalVariableType localVariableType;

    public UpdateTypeVisitor(LocalVariableSet localVariableSet) {
        this.localVariableSet = localVariableSet;
    }

    public void setLocalVariableType(LocalVariableType localVariableType) {
        this.localVariableType = localVariableType;
    }

    @Override
    public void visit(ObjectType type) {
        localVariableSet.update(localVariableType.index(), localVariableType.startPc(), updateType(type));
    }

    @Override
    public void visit(InnerObjectType type) {
        localVariableSet.update(localVariableType.index(), localVariableType.startPc(), updateType(type));
    }

    @Override
    public void visit(GenericType type) {
        localVariableSet.update(localVariableType.index(), localVariableType.startPc(), type);
    }

    protected ObjectType updateType(ObjectType type) {
        BaseTypeArgument typeArguments = type.getTypeArguments();

        if (typeArguments != null) {
            updateClassTypeArgumentsVisitor.init();
            typeArguments.accept(updateClassTypeArgumentsVisitor);

            if (typeArguments != updateClassTypeArgumentsVisitor.getTypeArgument()) {
                type = type.createType(updateClassTypeArgumentsVisitor.getTypeArgument());
            }
        }

        return type;
    }
}
