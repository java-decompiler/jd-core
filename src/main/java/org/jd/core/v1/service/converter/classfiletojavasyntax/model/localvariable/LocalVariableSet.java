/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable;

import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;

public class LocalVariableSet {
    private AbstractLocalVariable[] array = new AbstractLocalVariable[10];
    private int size;

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
            } else if (lv.getFromOffset() < newLV.getFromOffset()) {
                if (newLV == lv) {
                    throw new IllegalStateException("newLV == lv");
                }
                newLV.setNext(lv);
                array[index] = newLV;
            } else {
                AbstractLocalVariable previous = lv;

                lv = lv.getNext();

                while (lv != null && lv.getFromOffset() > newLV.getFromOffset()) {
                    previous = lv;
                    lv = lv.getNext();
                }

                if (previous == newLV) {
                    throw new IllegalStateException("previous == newLV");
                }
                previous.setNext(newLV);

                if (newLV == lv) {
                    throw new IllegalStateException("newLV == lv");
                }
                newLV.setNext(lv);
            }
        }

        size++;
    }

    public AbstractLocalVariable root(int index) {
        if (index < array.length) {
            AbstractLocalVariable lv = array[index];

            if (lv != null) {
                while (lv.getNext() != null) {
                    assert lv != lv.getNext();
                    lv = lv.getNext();
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
                if (lv.getFromOffset() <= offset) {
                    if (previous == null) {
                        array[index] = lv.getNext();
                    } else {
                        previous.setNext(lv.getNext());
                    }

                    size--;
                    lv.setNext(null);
                    return lv;
                }

                previous = lv;
                assert lv != lv.getNext();
                lv = lv.getNext();
            }
        }

        return null;
    }

    public AbstractLocalVariable get(int index, int offset) {
        if (index < array.length) {
            AbstractLocalVariable lv = array[index];

            while (lv != null) {
                if (lv.getFromOffset() <= offset && offset <= lv.getToOffset() + 1) {
                    return lv;
                }

                assert lv != lv.getNext();
                lv = lv.getNext();
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
                if (lv.getFromOffset() == offset) {
                    ObjectLocalVariable olv = (ObjectLocalVariable)lv;
                    olv.type = type;
                    break;
                }

                assert lv != lv.getNext();
                lv = lv.getNext();
            }
        }
    }

    public void update(int index, int offset, GenericType type) {
        if (index < array.length) {
            AbstractLocalVariable previous = null;
            AbstractLocalVariable lv = array[index];

            while (lv != null) {
                if (lv.getFromOffset() == offset) {
                    GenericLocalVariable glv = new GenericLocalVariable(index, lv.getFromOffset(), type, lv.getName());
                    glv.setNext(lv.getNext());

                    if (previous == null) {
                        array[index] = glv;
                    } else {
                        if (previous == glv) {
                            throw new IllegalStateException("previous == glv");
                        }
                        previous.setNext(glv);
                    }

                    break;
                }

                previous = lv;
                assert lv != lv.getNext();
                lv = lv.getNext();
            }
        }
    }

    public AbstractLocalVariable[] initialize(Frame rootFrame) {
        AbstractLocalVariable[] cache = new AbstractLocalVariable[array.length];

        for (int index=array.length-1; index>=0; index--) {
            AbstractLocalVariable lv = array[index];

            if (lv != null) {
                AbstractLocalVariable previous = null;

                while (lv.getNext() != null) {
                    previous = lv;
                    assert lv != lv.getNext();
                    lv = lv.getNext();
                }

                if (lv.getFromOffset() == 0) {
                    if (previous == null) {
                        array[index] = lv.getNext();
                    } else {
                        previous.setNext(lv.getNext());
                    }

                    size--;
                    lv.setNext(null);
                    rootFrame.addLocalVariable(lv);
                    cache[index] = lv;
                }
            }
        }

        return cache;
    }
}
