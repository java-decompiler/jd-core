/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.model.javasyntax.type.TypeVisitor;
import org.jd.core.v1.model.javasyntax.type.Types;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;

public class BaseTypeToTypeArgumentVisitor implements TypeVisitor {
    private TypeArgument typeArgument;

    public void init() {
        this.typeArgument = null;
    }

    public TypeArgument getTypeArgument() {
        return typeArgument;
    }

    @Override
    public void visit(PrimitiveType type) { typeArgument = type; }
    @Override
    public void visit(ObjectType type) { typeArgument = type; }
    @Override
    public void visit(InnerObjectType type) { typeArgument = type; }
    @Override
    public void visit(GenericType type) { typeArgument = type; }

    @Override
    public void visit(Types types) {
        if (types.isEmpty()) {
            typeArgument = WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT;
        } else {
            types.getFirst().accept(this);
        }
    }
}
