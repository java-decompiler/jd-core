/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.type.DiamondTypeArgument;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeArgumentVisitor;
import org.jd.core.v1.model.javasyntax.type.TypeArguments;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardSuperTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;

public class CreateTypeFromTypeArgumentVisitor implements TypeArgumentVisitor {
    private Type type;

    public CreateTypeFromTypeArgumentVisitor() {
        type = null;
    }

    public Type getType() {
        return type;
    }

    @Override public void visit(TypeArguments arguments) { this.type = null; }
    @Override public void visit(DiamondTypeArgument argument) { this.type = null; }
    @Override public void visit(WildcardTypeArgument type) { this.type = null; }
    @Override public void visit(WildcardExtendsTypeArgument type) { this.type = type.type(); }
    @Override public void visit(WildcardSuperTypeArgument type) { this.type = type.type(); }
    @Override public void visit(PrimitiveType type) { this.type = type; }
    @Override public void visit(ObjectType type) { this.type = type; }
    @Override public void visit(InnerObjectType type) { this.type = type; }
    @Override public void visit(GenericType type) { this.type = type; }
}
