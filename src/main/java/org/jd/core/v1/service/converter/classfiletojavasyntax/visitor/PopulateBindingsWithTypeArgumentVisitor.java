/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.type.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_CLASS;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_CLASS_WILDCARD;

public class PopulateBindingsWithTypeArgumentVisitor implements TypeArgumentVisitor {
    protected TypeArgumentToTypeVisitor typeArgumentToTypeVisitor = new TypeArgumentToTypeVisitor();
    protected TypeMaker typeMaker;
    protected Map<String, BaseType> contextualTypeBounds;
    protected Map<String, TypeArgument> bindings;
    protected Map<String, BaseType> typeBounds;
    protected BaseTypeArgument current;

    public PopulateBindingsWithTypeArgumentVisitor(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
        this.current = null;
    }

    public void init(Map<String, BaseType> contextualTypeBounds, Map<String, TypeArgument> bindings, Map<String, BaseType> typeBounds, BaseTypeArgument typeArgument) {
        this.contextualTypeBounds = contextualTypeBounds;
        this.bindings = bindings;
        this.typeBounds = typeBounds;
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
        String typeName = type.getName();

        if (bindings.containsKey(typeName)) {
            TypeArgument typeArgument = bindings.get(type.getName());

            if (current != null) {
                if ((current.getClass() == GenericType.class) && !equals(contextualTypeBounds.get(typeName), typeBounds.get(((GenericType)current).getName()))) {
                    return; // Incompatible bounds
                }

                if (typeArgument == null) {
                    bindings.put(typeName, convertCurrentTypeArgument());
                } else if (!current.equals(typeArgument)) {
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
                                bindings.put(typeName, typeArgument);
                            } else if (typeMaker.isAssignable(ot2, ot1)) {
                                bindings.put(typeName, convertCurrentTypeArgument());
                            } else {
                                bindings.put(typeName, WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean equals(BaseType bt1, BaseType bt2) {
        if (bt1 == null) {
            return (bt2 == null);
        } else {
            return (bt2 != null) && bt1.equals(bt2);
        }
    }

    protected TypeArgument convertCurrentTypeArgument() {
        if ((current != null) && (current.getClass() == ObjectType.class)) {
            ObjectType ot = (ObjectType)current;

            if ((ot.getTypeArguments() == null) && ot.getInternalName().equals(TYPE_CLASS.getInternalName())) {
                return TYPE_CLASS_WILDCARD;
            }
        }

        return (TypeArgument)current;
    }

    @Override
    public void visit(WildcardExtendsTypeArgument type) {
        if (current != null) {
            if (current.getClass() == WildcardExtendsTypeArgument.class) {
                current = ((WildcardExtendsTypeArgument) current).getType();
                type.getType().accept(this);
            } else {
                type.getType().accept(this);
            }
        }
    }

    @Override
    public void visit(WildcardSuperTypeArgument type) {
        if (current != null) {
            if (current.getClass() == WildcardSuperTypeArgument.class) {
                current = ((WildcardSuperTypeArgument) current).getType();
                type.getType().accept(this);
            } else {
                type.getType().accept(this);
            }
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
