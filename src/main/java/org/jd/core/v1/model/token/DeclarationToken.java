/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.token;

public class DeclarationToken implements Token {

    private final int type;
    protected final String internalTypeName;
    protected final String name;
    private final String descriptor;

    public DeclarationToken(int type, String internalTypeName, String name, String descriptor) {
        this.type = type;
        this.internalTypeName = internalTypeName;
        this.name = name;
        this.descriptor = descriptor;
    }

    public int getType() {
        return type;
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

    @Override
    public String toString() {
        return "DeclarationToken{declaration='" + name + "'}";
    }

    @Override
    public void accept(TokenVisitor visitor) {
        visitor.visit(this);
    }
}
