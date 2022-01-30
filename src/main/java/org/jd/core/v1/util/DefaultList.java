/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DefaultList<E> extends ArrayList<E> {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes")
    protected static final EmptyList EMPTY_LIST = new EmptyList();

    public DefaultList() {}

    public DefaultList(int capacity) {
        super(capacity);
    }

    public DefaultList(Collection<E> collection) {
        super(collection);
    }

    @SafeVarargs
    public DefaultList(E element, E... elements) {
        ensureCapacity(elements.length + 1);

        add(element);

        for (E e : elements) {
            add(e);
        }
    }

    public DefaultList(E[] elements) {
        if (elements != null && elements.length > 0) {
            ensureCapacity(elements.length);

            for (E e : elements) {
                add(e);
            }
        }
    }

    public E getFirst() {
        return get(0);
    }

    public E getLast() {
        return get(size()-1);
    }

    public E removeFirst() {
        return remove(0);
    }

    public E removeLast() {
        return remove(size()-1);
    }

    public boolean isList() {
        return true;
    }

    public DefaultList<E> getList() {
        return this;
    }

    @SuppressWarnings("unchecked")
    public static <T> DefaultList<T> emptyList() {
        return EMPTY_LIST;
    }

    protected static class EmptyList<E> extends DefaultList<E> {

        private static final long serialVersionUID = 1L;
        public EmptyList() { super(0); }

        @Override
        public E set(int index, E e) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void add(int index, E e) {
            throw new UnsupportedOperationException();
        }
        @Override
        public E remove(int index) {
            throw new UnsupportedOperationException();
        }
        @Override
        public Iterator<E> iterator() { return emptyIterator(); }

        @SuppressWarnings("unchecked")
        public static <T> Iterator<T> emptyIterator() {
            return (Iterator<T>) EmptyIterator.EMPTY_ITERATOR;
        }
    }

    private static class EmptyIterator<E> implements Iterator<E> {

        private static final EmptyIterator<Object> EMPTY_ITERATOR = new EmptyIterator<>();

        @Override
        public boolean hasNext() { return false; }
        @Override
        public E next() { throw new NoSuchElementException(); }
        @Override
        public void remove() { throw new UnsupportedOperationException(); }

    }
}
