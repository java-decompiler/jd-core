/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.type.AbstractNopTypeVisitor;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.model.javasyntax.type.Types;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;

import java.util.Map;

import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_OBJECT;

public class BindTypesToTypesVisitor extends AbstractNopTypeVisitor {
    private final TypeArgumentToTypeVisitor typeArgumentToTypeVisitor = new TypeArgumentToTypeVisitor();
    private final BindTypeArgumentsToTypeArgumentsVisitor bindTypeArgumentsToTypeArgumentsVisitor = new BindTypeArgumentsToTypeArgumentsVisitor();

    private Map<String, TypeArgument> bindings;
    private BaseType result;

    public void setBindings(Map<String, TypeArgument> bindings) {
        this.bindings = bindings;
        bindTypeArgumentsToTypeArgumentsVisitor.setBindings(this.bindings);
    }

    public void init() {
        this.result = null;
    }

    public BaseType getType() {
        return result;
    }

    @Override
    public void visit(PrimitiveType type) {
        result = type;
    }

    @Override
    public void visit(ObjectType type) {
        BaseTypeArgument typeArguments = type.getTypeArguments();

        if (typeArguments == null) {
            result = type;
        } else {
            bindTypeArgumentsToTypeArgumentsVisitor.init();
            typeArguments.accept(bindTypeArgumentsToTypeArgumentsVisitor);
            BaseTypeArgument ta = bindTypeArgumentsToTypeArgumentsVisitor.getTypeArgument();

            if (typeArguments == ta) {
                result = type;
            } else {
                result = type.createType(ta);
            }
        }
    }

    @Override
    public void visit(InnerObjectType type) {
        type.getOuterType().accept(this);

        BaseTypeArgument typeArguments = type.getTypeArguments();

        if (type.getOuterType() == result) {
            if (typeArguments == null) {
                result = type;
            } else {
                bindTypeArgumentsToTypeArgumentsVisitor.init();
                typeArguments.accept(bindTypeArgumentsToTypeArgumentsVisitor);
                BaseTypeArgument ta = bindTypeArgumentsToTypeArgumentsVisitor.getTypeArgument();

                if (typeArguments == ta) {
                    result = type;
                } else {
                    result = type.createType(ta);
                }
            }
        } else {
            ObjectType outerObjectType = (ObjectType) result;

            if (typeArguments != null) {
                bindTypeArgumentsToTypeArgumentsVisitor.init();
                typeArguments.accept(bindTypeArgumentsToTypeArgumentsVisitor);
                typeArguments = bindTypeArgumentsToTypeArgumentsVisitor.getTypeArgument();

                if (WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT == typeArguments) {
                    typeArguments = null;
                }
            }

            result = new InnerObjectType(type.getInternalName(), type.getQualifiedName(), type.getName(), typeArguments, type.getDimension(), outerObjectType);
        }
    }

    @Override
    public void visit(GenericType type) {
        TypeArgument ta = bindings.get(type.getName());

        if ((ta == null) || (ta == WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT)) {
            result = TYPE_OBJECT.createType(type.getDimension());
        } else {
            typeArgumentToTypeVisitor.init();
            ta.accept(typeArgumentToTypeVisitor);
            Type t = typeArgumentToTypeVisitor.getType();
            result = t.createType(t.getDimension() + type.getDimension());
        }
    }

    @Override
    public void visit(Types types) {
        int size = types.size();
        int i;

        for (i=0; i<size; i++) {
            Type t = types.get(i);
            t.accept(this);
            if (result != t) {
                break;
            }
        }

        if (i == size) {
            result = types;
        } else {
            Types newTypes = new Types(size);

            newTypes.addAll(types.subList(0, i));
            newTypes.add((Type) result);

            for (i++; i<size; i++) {
                Type t = types.get(i);
                t.accept(this);
                newTypes.add((Type) result);
            }

            result = newTypes;
        }
    }
}
