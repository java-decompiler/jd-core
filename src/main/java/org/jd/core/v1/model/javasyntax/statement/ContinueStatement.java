/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.statement;

import java.util.Objects;

public class ContinueStatement implements Statement {
    public static final ContinueStatement CONTINUE = new ContinueStatement();

    private final String label;

    protected ContinueStatement() {
        this.label = null;
    }

    public ContinueStatement(String label) {
        Objects.requireNonNull(label);

        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean isContinueStatement() { return true; }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
