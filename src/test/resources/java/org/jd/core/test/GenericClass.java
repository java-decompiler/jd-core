/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.test;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
@SuppressWarnings("all")
public class GenericClass<T1,                                                             // Implicit 'extends Object'
                     T2 extends Object,                                                   // Explicit 'extends Object'
                     T3 extends AnnotatedClass,                                           // Extends class
                     T4 extends Serializable,                                             // Extends interface
                     T5 extends Serializable & Comparable,                                // Extends two interfaces
                     T6 extends AnnotatedClass & Serializable & Comparable<GenericClass>, // Extends class and two interfaces
                     T7 extends Map<?, ?>,
                     T8 extends Map<? extends Number, ? super Serializable>,
                     T9 extends T8>
        extends ArrayList<T7>
        implements Serializable, Comparable<T1> {

    private static final long serialVersionUID = 1L;
    public List<List<? extends GenericClass>> list1 = new ArrayList<>();
    public List<List<? super GenericClass>> list2;

    public GenericClass() {
        super(10);
        list2 = new ArrayList<>();
    }

    public <T> void fromArrayToCollection(T[] a, Collection<T> c) {
        for (T o : a) {
            c.add(o);
        }
    }

    public <T> void copy(List<T> dest, List<? extends T> src) {
        // ...
    }

    public <T, S extends T> List<? extends Number> copy2(List<? super T> dest, List<S> src) throws InvalidParameterException, ClassCastException {
        // ...
        return null;
    }

    @SuppressWarnings("hiding")
    public <T1, T2 extends Exception> List<? extends Number> print(List<? super T1> list) throws T2, InvalidParameterException {
        // ...
        return null;
    }

    public int scopesAndVariables(int i) {
        int result;

        List<String> as = new ArrayList<>(i + 1);
        System.out.println(as);

        {
            int j = i;
            String s = "test " + j;
            System.out.println(s);
            int jj = 123;
            System.out.println(jj);
            result = 1;
        }
        {
            int k = i;
            List<Double> l = new ArrayList<>(k + 3);
            System.out.println(l);
            int kk = 456;
            System.out.println(kk);
            result = 2;
        }

        return result;
    }

    public int varargs(int firstParameter, int... lastParameters) {
        return firstParameter;
    }

    @SuppressWarnings("unused")
    public <R, T, L extends List<String>> R genericAssignment(int i, int j, String[] envs, String[] opts, String[] args, T t, L l) {
        l.add(envs[0]);

        t = (T)opts[1];

        T tt = t;

        return null;
    }

    @Override
    public int compareTo(T1 o) {
        return 0;
    }

    public T1 call() {
        return call(0);
    }

    public T1 call(int i) {
        return (T1)this;
    }
}
