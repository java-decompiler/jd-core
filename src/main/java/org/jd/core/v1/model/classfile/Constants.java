/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.classfile;

public interface Constants {
    // Access flag for Class, Field, Method, Nested class
    short ACC_PUBLIC       = 0x0001; // C F M N
    short ACC_PRIVATE      = 0x0002; //   F M N
    short ACC_PROTECTED    = 0x0004; //   F M N
    short ACC_STATIC       = 0x0008; // C F M N
    short ACC_FINAL        = 0x0010; // C F M N
    short ACC_SYNCHRONIZED = 0x0020; //     M
    short ACC_SUPER        = 0x0020; // C
    short ACC_VOLATILE     = 0x0040; //   F
    short ACC_BRIDGE       = 0x0040; //     M
    short ACC_TRANSIENT    = 0x0080; //   F
    short ACC_VARARGS      = 0x0080; //     M
    short ACC_NATIVE       = 0x0100; //     M
    short ACC_INTERFACE    = 0x0200; // C     N
    short ACC_ABSTRACT     = 0x0400; // C   M N
    short ACC_STRICT       = 0x0800; //     M
    short ACC_SYNTHETIC    = 0x1000; // C F M N
    short ACC_ANNOTATION   = 0x2000; // C     N
    short ACC_ENUM         = 0x4000; // C F   N
}
