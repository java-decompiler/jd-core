/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.util;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("unchecked")
public class DefaultList<E> extends ArrayList<E> {
    protected static DefaultList EMPTY_LIST = new DefaultList() {
        public Object set(int var1, Object var2) {
            throw new UnsupportedOperationException();
        }
        public void add(int var1, Object var2) {
            throw new UnsupportedOperationException();
        }
        public Object remove(int var1) {
            throw new UnsupportedOperationException();
        }
    };

    public DefaultList() {}

    public DefaultList(int capacity) {
        super(capacity);
    }

    public DefaultList(Collection<E> collection) {
        super(collection);
    }

    public DefaultList(E element, E... elements) {
        ensureCapacity(elements.length + 1);

        add(element);

        for (E e : elements) {
            add(e);
        }
    }

    public DefaultList(E[] elements) {
        if ((elements != null) && (elements.length > 0)) {
            ensureCapacity(elements.length);

            for (E e : elements) {
                add(e);
            }
        }
    }

    public E getFirst() {
        return (E)get(0);
    }

    public E getLast() {
        return (E)get(size()-1);
    }

    public E removeFirst() {
        return (E)remove(0);
    }

    public E removeLast() {
        return (E)remove(size()-1);
    }

    public boolean isList() {
        return true;
    }

    public static final <T> DefaultList<T> emptyList() {
        return EMPTY_LIST;
    }
}
