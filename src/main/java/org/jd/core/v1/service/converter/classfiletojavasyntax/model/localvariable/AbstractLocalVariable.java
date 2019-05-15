/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable;

import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.util.DefaultList;

public abstract class AbstractLocalVariable implements LocalVariable {
    protected Frame frame;
    protected AbstractLocalVariable next;
    protected boolean declared;
    protected int index;
    protected int fromOffset;
    protected int toOffset;
    protected String name;
    protected int dimension;
    protected DefaultList<ClassFileLocalVariableReferenceExpression> references = new DefaultList<>();

    public AbstractLocalVariable(int index, int offset, int dimension) {
        this.declared = (offset == 0);
        this.index = index;
        this.fromOffset = offset;
        this.toOffset = offset;
        this.dimension = dimension;
    }

    public AbstractLocalVariable(int index, int offset, int dimension, boolean declared) {
        this.declared = declared;
        this.index = index;
        this.fromOffset = offset;
        this.toOffset = offset;
        this.dimension = dimension;
    }

    public AbstractLocalVariable(int index, int offset, String name, int dimension) {
        this.declared = (offset == 0);
        this.index = index;
        this.fromOffset = offset;
        this.toOffset = offset;
        this.name = name;
        this.dimension = dimension;
    }

    public AbstractLocalVariable(int index, int offset, String name, int dimension, boolean declared) {
        this.declared = declared;
        this.index = index;
        this.fromOffset = offset;
        this.toOffset = offset;
        this.name = name;
        this.dimension = dimension;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    public AbstractLocalVariable getNext() {
        return next;
    }

    public void setNext(AbstractLocalVariable next) {
        this.next = next;
    }

    public boolean isDeclared() {
        return declared;
    }

    public void setDeclared(boolean declared) {
        this.declared = declared;
    }

    public int getIndex() {
        return index;
    }

    public int getFromOffset() {
        return fromOffset;
    }

    public void setFromOffset(int fromOffset) {
        assert fromOffset <= toOffset;
        this.fromOffset = fromOffset;
    }

    public int getToOffset() {
        return toOffset;
    }

    public void setToOffset(int offset) {
        if (this.fromOffset > offset)
            this.fromOffset = offset;
        if (this.toOffset < offset)
            this.toOffset = offset;
    }

    @Override public String getName() { return name; }

    public int getDimension() { return dimension; }

    public DefaultList<ClassFileLocalVariableReferenceExpression> getReferences() {
        return references;
    }

    public abstract boolean isAssignable(Type otherType);
    public abstract boolean isAssignable(AbstractLocalVariable other);

    public abstract void leftReduce(Type otherType);
    public abstract void leftReduce(AbstractLocalVariable other);

    public abstract void rightReduce(Type otherType);
    public abstract void rightReduce(AbstractLocalVariable other);

    @Override
    public void addReference(ClassFileLocalVariableReferenceExpression reference) {
        references.add(reference);
    }
}
