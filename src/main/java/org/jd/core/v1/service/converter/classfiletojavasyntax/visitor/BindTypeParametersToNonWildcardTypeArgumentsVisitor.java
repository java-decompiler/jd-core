/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
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

import java.util.Map;

public class BindTypeParametersToNonWildcardTypeArgumentsVisitor implements TypeParameterVisitor, TypeArgumentVisitor {
    private Map<String, TypeArgument> bindings;
    private BaseTypeArgument result;

    public void init(Map<String, TypeArgument> bindings) {
        this.bindings = bindings;
        this.result = null;
    }

    public BaseTypeArgument getTypeArgument() {
        return result;
    }

    // --- TypeParameterVisitor --- //
    @Override
    public void visit(TypeParameter parameter) {
        result = bindings.get(parameter.getIdentifier());

        if (result != null) {
            result.accept(this);
        }
    }

    @Override
    public void visit(TypeParameterWithTypeBounds parameter) {
        result = bindings.get(parameter.getIdentifier());

        if (result != null) {
            result.accept(this);
        }
    }

    @Override
    public void visit(TypeParameters parameters) {
        int size = parameters.size();
        TypeArguments arguments = new TypeArguments(size);

        for (TypeParameter parameter : parameters) {
            parameter.accept(this);

            if (result == null) {
                return;
            }

            arguments.add((TypeArgument)result);
        }

        result = arguments;
    }

    // --- TypeArgumentVisitor --- //
    @Override public void visit(WildcardExtendsTypeArgument argument) { result = argument.type(); }
    @Override public void visit(WildcardSuperTypeArgument argument) { result = argument.type(); }

    @Override public void visit(DiamondTypeArgument argument) { result = null; }
    @Override public void visit(WildcardTypeArgument argument) { result = null; }

    @Override public void visit(TypeArguments arguments) {}
    @Override public void visit(PrimitiveType type) {}
    @Override public void visit(ObjectType type) {}
    @Override public void visit(InnerObjectType type) {}
    @Override public void visit(GenericType type) {}
}
