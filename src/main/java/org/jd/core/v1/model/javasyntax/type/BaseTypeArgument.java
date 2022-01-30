/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

import org.jd.core.v1.util.DefaultList;

import java.util.Map;

public interface BaseTypeArgument extends TypeArgumentVisitable {
    @SuppressWarnings("unused")
    default boolean isTypeArgumentAssignableFrom(Map<String, BaseType> typeBounds, BaseTypeArgument typeArgument) {
        return false;
    }

    default boolean isTypeArgumentList() {
        return false;
    }

    default TypeArgument getTypeArgumentFirst() {
        return (TypeArgument)this;
    }

    @SuppressWarnings("unchecked")
    default DefaultList<TypeArgument> getTypeArgumentList() {
        return (DefaultList<TypeArgument>)this;
    }

    default int typeArgumentSize() {
        return 1;
    }

    default boolean isGenericTypeArgument() { return false; }
    default boolean isInnerObjectTypeArgument() { return false; }
    default boolean isObjectTypeArgument() { return false; }
    default boolean isPrimitiveTypeArgument() { return false; }
    default boolean isWildcardExtendsTypeArgument() { return false; }
    default boolean isWildcardSuperTypeArgument() { return false; }
    default boolean isWildcardTypeArgument() { return false; }

    default Type type() { return ObjectType.TYPE_UNDEFINED_OBJECT; }
}
