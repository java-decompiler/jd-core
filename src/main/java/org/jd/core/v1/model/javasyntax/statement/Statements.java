/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.statement;

import org.jd.core.v1.util.DefaultList;

import java.util.List;

public class Statements extends DefaultList<Statement> implements BaseStatement {
    private static final long serialVersionUID = 1L;

    public Statements() {}

    public Statements(List<Statement> list) {
        super(list);
        if (list.size() <= 1) {
            throw new IllegalArgumentException("Use 'Statement' implementation instead");
        }
    }

    public Statements(Statement statement, Statement... statements) {
        super(statement, statements);
        if (statements.length <= 0) {
            throw new IllegalArgumentException("Use 'Statement' implementation instead");
        }
    }

    @Override
    public boolean isStatements() { return true; }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
