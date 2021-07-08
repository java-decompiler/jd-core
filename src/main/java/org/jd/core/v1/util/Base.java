/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface Base<T> extends Iterable<T> {
    default boolean isList() {
        return false;
    }

    @SuppressWarnings("unchecked")
    default T getFirst() {
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    default T getLast() {
        return (T)this;
    }

    default DefaultList<T> getList() {
        throw new UnsupportedOperationException();
    }

    default int size() {
        return 1;
    }

    @Override
    default Iterator<T> iterator() {
        return new Iterator<T>() {
            private boolean hasNext = true;
            @Override
            public boolean hasNext() {
                return hasNext;
            }
            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                if (hasNext) {
                    hasNext = false;
                    return (T)Base.this;
                }
                throw new NoSuchElementException();
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
