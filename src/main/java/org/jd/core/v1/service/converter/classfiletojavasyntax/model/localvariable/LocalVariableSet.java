/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable;

import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;

public class LocalVariableSet {
    protected AbstractLocalVariable[] array = new AbstractLocalVariable[10];
    protected int size = 0;

    public void add(int index, AbstractLocalVariable newLV) {
        if (index >= array.length) {
            // Increases array
            Object[] tmp = array;
            array = new AbstractLocalVariable[index * 2];
            System.arraycopy(tmp, 0, array, 0, tmp.length);
            // Store
            array[index] = newLV;
        } else {
            AbstractLocalVariable lv = array[index];

            if (lv == null) {
                array[index] = newLV;
            } else if (lv.fromOffset < newLV.fromOffset) {
                assert newLV != lv;
                newLV.next = lv;
                array[index] = newLV;
            } else {
                AbstractLocalVariable previous = lv;

                lv = lv.next;

                while ((lv != null) && (lv.fromOffset > newLV.fromOffset)) {
                    previous = lv;
                    lv = lv.next;
                }

                assert previous != newLV;
                previous.next = newLV;

                assert newLV != lv;
                newLV.next = lv;
            }
        }

        size++;
    }

    public AbstractLocalVariable root(int index) {
        if (index < array.length) {
            AbstractLocalVariable lv = array[index];

            if (lv != null) {
                while (lv.next != null) {
                    assert lv != lv.next;
                    lv = lv.next;
                }
                return lv;
            }
        }

        return null;
    }

    public AbstractLocalVariable remove(int index, int offset) {
        if (index < array.length) {
            AbstractLocalVariable previous = null;
            AbstractLocalVariable lv = array[index];

            while (lv != null) {
                if (lv.fromOffset <= offset) {
                    if (previous == null) {
                        array[index] = lv.next;
                    } else {
                        previous.next = lv.next;
                    }

                    size--;
                    lv.next = null;
                    return lv;
                }

                previous = lv;
                assert lv != lv.next;
                lv = lv.next;
            }
        }

        return null;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void update(int index, int offset, ObjectType type) {
        if (index < array.length) {
            AbstractLocalVariable lv = array[index];

            while (lv != null) {
                if (lv.fromOffset == offset) {
                    ObjectLocalVariable olv = (ObjectLocalVariable)lv;
                    olv.fromType = olv.toType = type;
                    break;
                }

                assert lv != lv.next;
                lv = lv.next;
            }
        }
    }

    public void update(int index, int offset, GenericType type) {
        if (index < array.length) {
            AbstractLocalVariable previous = null;
            AbstractLocalVariable lv = array[index];

            while (lv != null) {
                if (lv.fromOffset == offset) {
                    GenericLocalVariable glv = new GenericLocalVariable(index, lv.fromOffset, type, lv.name);
                    glv.next = lv.next;

                    if (previous == null) {
                        array[index] = glv;
                    } else {
                        assert previous != glv;
                        previous.next = glv;
                    }

                    break;
                }

                previous = lv;
                assert lv != lv.next;
                lv = lv.next;
            }
        }
    }

    public AbstractLocalVariable[] initialize(Frame rootFrame) {
        AbstractLocalVariable[] cache = new AbstractLocalVariable[array.length];

        for (int index=array.length-1; index>=0; index--) {
            AbstractLocalVariable lv = array[index];

            if (lv != null) {
                AbstractLocalVariable previous = null;

                while (lv.next != null) {
                    previous = lv;
                    assert lv != lv.next;
                    lv = lv.next;
                }

                if (lv.fromOffset == 0) {
                    if (previous == null) {
                        array[index] = lv.next;
                    } else {
                        previous.next = lv.next;
                    }

                    size--;
                    lv.next = null;
                    rootFrame.addLocalVariable(lv);
                    cache[index] = lv;
                }
            }
        }

        return cache;
    }
}
