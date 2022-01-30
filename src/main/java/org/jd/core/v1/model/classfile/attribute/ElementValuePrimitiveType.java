/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.classfile.attribute;

import org.apache.bcel.classfile.Constant;

public class ElementValuePrimitiveType implements AttributeElementValue {
    /*
     * type = {'B', 'D', 'F', 'I', 'J', 'S', 'Z', 'C', 's'}
     */
    private final int type;
    private final Constant constValue;

    public ElementValuePrimitiveType(int type, Constant constValue) {
        this.type = type;
        this.constValue = constValue;
    }

    public int getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public <T extends Constant> T getConstValue() {
        return (T)constValue;
    }

    @Override
    public void accept(ElementValueVisitor visitor) {
        visitor.visit(this);
    }
}
