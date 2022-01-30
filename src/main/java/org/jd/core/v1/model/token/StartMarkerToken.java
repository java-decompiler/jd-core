/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.token;

import org.jd.core.v1.api.printer.Printer;

public record StartMarkerToken(int type) implements Token {

    public static final StartMarkerToken COMMENT = new StartMarkerToken(Printer.COMMENT);
    public static final StartMarkerToken JAVADOC = new StartMarkerToken(Printer.JAVADOC);
    public static final StartMarkerToken IMPORT_STATEMENTS = new StartMarkerToken(Printer.IMPORT_STATEMENTS);

    @Override
    public String toString() {
        return "StartMarkerToken{'" + type + "'}";
    }

    @Override
    public void accept(TokenVisitor visitor) {
        visitor.visit(this);
    }
}
