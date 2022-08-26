/*
 * Copyright (c) 2008, 2022 Emmanuel Dupuy and other contributors.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.DiamondTypeArgument;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.model.javasyntax.type.TypeArgumentVisitor;
import org.jd.core.v1.model.javasyntax.type.TypeArguments;
import org.jd.core.v1.model.javasyntax.type.TypeParameter;
import org.jd.core.v1.model.javasyntax.type.TypeParameterVisitor;
import org.jd.core.v1.model.javasyntax.type.TypeParameterWithTypeBounds;
import org.jd.core.v1.model.javasyntax.type.TypeParameters;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardSuperTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;

import java.util.HashMap;
import java.util.Map;

public class TypeWithBoundsToGenericVisitor implements TypeParameterVisitor, TypeArgumentVisitor {

    private Map<String, GenericType> internalNameToGenericType = new HashMap<>();

    // --- TypeParameterVisitor --- //
    @Override
    public void visit(TypeParameter parameter) {
    }

    @Override
    public void visit(TypeParameterWithTypeBounds parameter) {
        BaseType bounds = parameter.getTypeBounds();
        if (bounds instanceof ObjectType) {
            ObjectType ot = (ObjectType) bounds;
            internalNameToGenericType.put(ot.getInternalName(), new GenericType(parameter.getIdentifier()));
        }
    }

    @Override
    public void visit(TypeParameters parameters) {
        for (TypeParameter parameter : parameters) {
            parameter.accept(this);
        }
    }

    // --- TypeArgumentVisitor --- //

    @Override
    public void visit(TypeArguments arguments) {
        for (int i = 0; i < arguments.size(); i++) {
            TypeArgument typeArgument = arguments.get(i);
            if (typeArgument instanceof ObjectType) {
                ObjectType ot = (ObjectType) typeArgument;
                GenericType genericType = internalNameToGenericType.get(ot.getInternalName());
                if (genericType != null) {
                    arguments.set(i, genericType);
                }
            }
        }
    }

    @Override public void visit(WildcardExtendsTypeArgument argument) {}
    @Override public void visit(WildcardSuperTypeArgument argument) {}

    @Override public void visit(DiamondTypeArgument argument) {}
    @Override public void visit(WildcardTypeArgument argument) {}
    @Override public void visit(PrimitiveType type) {}
    @Override public void visit(InnerObjectType type) {}
    @Override public void visit(GenericType type) {}
    @Override public void visit(ObjectType type) {}
}
