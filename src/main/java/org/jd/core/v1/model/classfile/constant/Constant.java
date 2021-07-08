/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.classfile.constant;

/**
 * @See https://docs.oracle.com/javase/specs/jvms/se16/html/jvms-4.html
 */
public abstract class Constant {
    public static final byte CONSTANT_UNKNOWN            = 0;
    public static final byte CONSTANT_UTF8               = 1;
    public static final byte CONSTANT_INTEGER            = 3;
    public static final byte CONSTANT_FLOAT              = 4;
    public static final byte CONSTANT_LONG               = 5;
    public static final byte CONSTANT_DOUBLE             = 6;
    public static final byte CONSTANT_CLASS              = 7;
    public static final byte CONSTANT_STRING             = 8;
    public static final byte CONSTANT_FIELDREF           = 9;
    public static final byte CONSTANT_METHODREF          = 10;
    public static final byte CONSTANT_INTERFACEMETHODREF = 11;
    public static final byte CONSTANT_NAMEANDTYPE        = 12;
    public static final byte CONSTANT_METHODHANDLE       = 15;
    public static final byte CONSTANT_METHODTYPE         = 16;
    public static final byte CONSTANT_INVOKEDYNAMIC      = 18;
    public static final byte CONSTANT_MEMBERREF          = 19; // Unofficial constant

    protected byte tag;

    protected Constant(byte tag) {
        this.tag = tag;
    }

    public byte getTag() {
        return tag;
    }
}
