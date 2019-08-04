/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

public interface Type extends TypeArgument, BaseType, TypeBoundList {
    String getName();

    String getDescriptor();

    int getDimension();

    Type createType(int dimension);

    default boolean isPrimitive() {
        return false;
    }

    default boolean isObject() {
        return false;
    }

    default boolean isGeneric() {
        return false;
    }
}
