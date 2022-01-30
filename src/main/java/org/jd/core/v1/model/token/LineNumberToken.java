/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.token;

/**
 * Must be create between StartStatementToken and EndStatementToken
 */
public record LineNumberToken(int lineNumber) implements Token {

    @Override
    public String toString() {
        return "LineNumberToken{" + lineNumber + "}";
    }

    @Override
    public void accept(TokenVisitor visitor) {
        visitor.visit(this);
    }
}
