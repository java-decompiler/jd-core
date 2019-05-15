/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

public abstract class AbstractNopTypeVisitor implements TypeVisitor {
    @Override public void visit(ArrayTypeArguments type) {}
    @Override public void visit(DiamondTypeArgument type) {}
    @Override public void visit(WildcardExtendsTypeArgument type) {}
    @Override public void visit(PrimitiveType type) {}
    @Override public void visit(ObjectType type) {}
    @Override public void visit(InnerObjectType type) {}
    @Override public void visit(WildcardSuperTypeArgument type) {}
    @Override public void visit(Types type) {}
    @Override public void visit(TypeBounds type) {}
    @Override public void visit(TypeParameter type) {}
    @Override public void visit(TypeParameterWithTypeBounds type) {}
    @Override public void visit(TypeParameters types) {}
    @Override public void visit(GenericType type) {}
    @Override public void visit(UnknownTypeArgument type) {}
}
