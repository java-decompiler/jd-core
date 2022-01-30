/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.classfile.constant;

import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.Visitor;

/**
 * POJO for CONSTANT_Fieldref, CONSTANT_Methodref and CONSTANT_InterfaceMethodref.
 */
public class ConstantMemberRef extends ConstantCP {

    public static final byte CONSTANT_MEMBER_REF = 19; // Unofficial constant

    public ConstantMemberRef(int classIndex, int nameAndTypeIndex) {
        super(CONSTANT_MEMBER_REF, classIndex, nameAndTypeIndex);
    }

    @Override
    public void accept(Visitor v) {
        throw new UnsupportedOperationException();
    }
}
