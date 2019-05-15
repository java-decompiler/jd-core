/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

import java.util.Iterator;

public abstract class AbstractTypeVisitor implements TypeVisitor {
    @Override
    public void visit(ArrayTypeArguments type) {
        for (TypeArgument typeArgument : type)
            typeArgument.accept(this);
    }

    @Override
    public void visit(DiamondTypeArgument type) {}

    @Override
    public void visit(WildcardExtendsTypeArgument type) {
        type.getType().accept(this);
    }

    @Override public void visit(PrimitiveType type) {}

    @Override
    public void visit(ObjectType type) {
        safeAccept(type.getTypeArguments());
    }

    @Override
    public void visit(InnerObjectType type) {
        type.getOuterType().accept(this);
        safeAccept(type.getTypeArguments());
    }

    @Override
    public void visit(WildcardSuperTypeArgument type) {
        type.getType().accept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(Types list) {
        Iterator<Type> iterator = list.iterator();

        while (iterator.hasNext())
            iterator.next().accept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(TypeBounds list) {
        Iterator<Type> iterator = list.iterator();

        while (iterator.hasNext())
            iterator.next().accept(this);
    }

    @Override
    public void visit(TypeParameter type) {}

    @Override
    public void visit(TypeParameterWithTypeBounds type) {
        type.getTypeBounds().accept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(TypeParameters list) {
        Iterator<TypeParameter> iterator = list.iterator();

        while (iterator.hasNext())
            iterator.next().accept(this);
    }

    @Override
    public void visit(GenericType type) {}

    @Override
    public void visit(UnknownTypeArgument type) {}

    protected void safeAccept(TypeVisitable type) {
        if (type != null)
            type.accept(this);
    }
}
