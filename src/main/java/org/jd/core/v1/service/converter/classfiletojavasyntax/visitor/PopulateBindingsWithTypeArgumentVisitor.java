/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.type.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import java.util.Iterator;
import java.util.Map;

public class PopulateBindingsWithTypeArgumentVisitor implements TypeArgumentVisitor {
    protected TypeArgumentToTypeVisitor typeArgumentToTypeVisitor = new TypeArgumentToTypeVisitor();
    protected TypeMaker typeMaker;
    protected Map<String, TypeArgument> bindings;
    protected BaseTypeArgument current;

    public PopulateBindingsWithTypeArgumentVisitor(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
        this.current = null;
    }

    public void init(Map<String, TypeArgument> bindings, BaseTypeArgument typeArgument) {
        this.bindings = bindings;
        this.current = typeArgument;
    }

    @Override
    public void visit(TypeArguments arguments) {
        if ((current != null) && current.isTypeArgumentList()) {
            Iterator<TypeArgument> typeArgumentIterator = arguments.iterator();
            Iterator<TypeArgument> typeGenericArgumentIterator = current.getTypeArgumentList().iterator();

            while (typeArgumentIterator.hasNext()) {
                current = typeGenericArgumentIterator.next();
                typeArgumentIterator.next().accept(this);
            }
        }
    }

    @Override public void visit(GenericType type) {
        if (bindings.containsKey(type.getName())) {
            TypeArgument typeArgument = bindings.get(type.getName());

            if (typeArgument == null) {
                if (current == null) {
                    bindings.put(type.getName(), WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                } else {
                    bindings.put(type.getName(), (TypeArgument) current);
                }
            } else if ((current != null) && !current.equals(typeArgument)) {
                typeArgumentToTypeVisitor.init();
                typeArgument.accept(typeArgumentToTypeVisitor);
                Type t1 = typeArgumentToTypeVisitor.getType();

                typeArgumentToTypeVisitor.init();
                current.accept(typeArgumentToTypeVisitor);
                Type t2 = typeArgumentToTypeVisitor.getType();

                if (!t1.createType(0).equals(t2.createType(0))) {
                    if (t1.isObject() && t2.isObject()) {
                        ObjectType ot1 = (ObjectType)t1;
                        ObjectType ot2 = (ObjectType)t2;

                        if (typeMaker.isAssignable(ot1, ot2)) {
                            bindings.put(type.getName(), typeArgument);
                        } else if (typeMaker.isAssignable(ot2, ot1)) {
                            bindings.put(type.getName(), (TypeArgument) current);
                        } else {
                            bindings.put(type.getName(), WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                        }
                    } else {
                        bindings.put(type.getName(), WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                    }
                }
            }
        }
    }

    @Override
    public void visit(WildcardExtendsTypeArgument type) {
        if ((current != null) && (current.getClass() == WildcardExtendsTypeArgument.class)) {
            current = ((WildcardExtendsTypeArgument) current).getType();
            type.getType().accept(this);
        }
    }

    @Override
    public void visit(WildcardSuperTypeArgument type) {
        if ((current != null) && (current.getClass() == WildcardSuperTypeArgument.class)) {
            current = ((WildcardSuperTypeArgument) current).getType();
            type.getType().accept(this);
        }
    }

    @Override
    public void visit(ObjectType type) {
        if ((current != null) && (type.getTypeArguments() != null)) {
            if ((current.getClass() == ObjectType.class) || (current.getClass() == InnerObjectType.class)) {
                current = ((ObjectType) current).getTypeArguments();
                type.getTypeArguments().accept(this);
            }
        }
    }

    @Override
    public void visit(InnerObjectType type) {
        if ((current != null) && (type.getTypeArguments() != null) && (current.getClass() == InnerObjectType.class)) {
            current = ((InnerObjectType)current).getTypeArguments();
            type.getTypeArguments().accept(this);
        }
    }

    @Override public void visit(DiamondTypeArgument argument) {}
    @Override public void visit(WildcardTypeArgument type) {}
    @Override public void visit(PrimitiveType type) {}
}
