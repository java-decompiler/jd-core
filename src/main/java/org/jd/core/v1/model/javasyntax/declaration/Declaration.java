/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.declaration;

public interface Declaration {
    public final static int FLAG_PUBLIC     = 0x0001;
    public final static int FLAG_PRIVATE    = 0x0002;
    public final static int FLAG_PROTECTED  = 0x0004;
    public final static int FLAG_STATIC     = 0x0008;
    public final static int FLAG_FINAL      = 0x0010;
    public final static int FLAG_BRIDGE     = 0x0040;
    public final static int FLAG_VARARGS    = 0x0080;
    public final static int FLAG_NATIVE     = 0x0100;
    public final static int FLAG_INTERFACE  = 0x0200;
    public final static int FLAG_ABSTRACT   = 0x0400;
    public final static int FLAG_SYNTHETIC  = 0x1000;
    public final static int FLAG_ANNOTATION = 0x2000;
    public final static int FLAG_ENUM       = 0x4000;

    void accept(DeclarationVisitor visitor);
}
