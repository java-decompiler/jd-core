/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.reference;

import org.jd.core.v1.util.DefaultList;

public class ElementValues extends DefaultList<BaseElementValue> implements BaseElementValue {
    private static final long serialVersionUID = 1L;

    public ElementValues() {
    }

    public ElementValues(int capacity) {
        super(capacity);
    }

    @Override
    public void accept(ReferenceVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "ElementValues{" + super.toString() + "}";
    }
}
