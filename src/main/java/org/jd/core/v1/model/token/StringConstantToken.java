/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.token;

public record StringConstantToken(String text, String ownerInternalName) implements Token {

    public String getOwnerInternalName() {
        return ownerInternalName;
    }

    @Override
    public String toString() {
        return "StringConstantToken{'" + text + "'}";
    }

    @Override
    public void accept(TokenVisitor visitor) {
        visitor.visit(this);
    }
}
