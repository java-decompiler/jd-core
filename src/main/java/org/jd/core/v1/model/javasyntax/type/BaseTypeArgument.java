/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

import org.jd.core.v1.util.DefaultList;

@SuppressWarnings("unchecked")
public interface BaseTypeArgument extends TypeVisitable {
    boolean isTypeArgumentAssignableFrom(BaseTypeArgument typeArgument);

    default TypeArgument getTypeArgumentFirst() {
        return (TypeArgument)this;
    }

    default DefaultList<TypeArgument> getTypeArgumentList() {
        return (DefaultList<TypeArgument>)this;
    }

    default int typeArgumentSize() {
        return 1;
    }
}
