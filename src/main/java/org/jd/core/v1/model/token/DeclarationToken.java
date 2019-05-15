/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.token;

import org.jd.core.v1.api.printer.Printer;

public class DeclarationToken implements Token {

    public static final int TYPE_FLAG = Printer.TYPE_FLAG;
    public static final int FIELD_FLAG = Printer.FIELD_FLAG;
    public static final int METHOD_FLAG = Printer.METHOD_FLAG;
    public static final int CONSTRUCTOR_FLAG = Printer.CONSTRUCTOR_FLAG;

    protected int flags;
    protected String internalTypeName;
    protected String name;
    protected String descriptor;

    public DeclarationToken(int flags, String internalTypeName, String name, String descriptor) {
        this.flags = flags;
        this.internalTypeName = internalTypeName;
        this.name = name;
        this.descriptor = descriptor;
    }

    public int getFlags() {
        return flags;
    }

    public String getInternalTypeName() {
        return internalTypeName;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String toString() {
        return "DeclarationToken{declaration='" + name + "'}";
    }

    @Override
    public void accept(TokenVisitor visitor) {
        visitor.visit(this);
    }
}
