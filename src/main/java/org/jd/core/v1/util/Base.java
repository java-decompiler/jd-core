/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.util;

@SuppressWarnings("unchecked")
public interface Base<T> {
    default boolean isList() {
        return false;
    }

    default T getFirst() {
        return (T)this;
    }

    default DefaultList<T> getList() {
        return (DefaultList<T>)this;
    }

    default int size() {
        return 1;
    }
}
