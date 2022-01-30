/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.classfile;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;

public class ConstantPool {
    private final Constant[] constants;

    public ConstantPool(Constant[] constants) {
        this.constants = constants;
    }

    @SuppressWarnings("unchecked")
    public <T extends Constant> T getConstant(int index) {
        return (T)constants[index];
    }

    public String getConstantTypeName(int index) {
        ConstantClass cc = (ConstantClass)constants[index];
        ConstantUtf8 cutf8 = (ConstantUtf8)constants[cc.getNameIndex()];
        return cutf8.getBytes();
    }

    public String getConstantString(int index) {
        ConstantString cString = (ConstantString)constants[index];
        ConstantUtf8 cutf8 = (ConstantUtf8)constants[cString.getStringIndex()];
        return cutf8.getBytes();
    }

    public String getConstantUtf8(int index) {
        ConstantUtf8 cutf8 = (ConstantUtf8)constants[index];
        return cutf8.getBytes();
    }

    public Constant getConstantValue(int index) {
        Constant constant = constants[index];

        if (constant instanceof ConstantString) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ConstantString cs = (ConstantString) constant;
            constant = constants[cs.getStringIndex()];
        }

        return constant;
    }

    @Override
    public String toString() {
        return "ConstantPool";
    }
}
