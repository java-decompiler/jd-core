/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.statement;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.util.DefaultList;

import java.util.List;

public class TryStatement implements Statement {
    protected List<Resource> resources;
    protected BaseStatement tryStatements;
    protected List<CatchClause> catchClauses;
    protected BaseStatement finallyStatements;

    public TryStatement(BaseStatement tryStatements, List<CatchClause> catchClauses, BaseStatement finallyStatements) {
        this.resources = null;
        this.tryStatements = tryStatements;
        this.catchClauses = catchClauses;
        this.finallyStatements = finallyStatements;
    }

    public TryStatement(List<Resource> resources, BaseStatement tryStatements, List<CatchClause> catchClauses, BaseStatement finallyStatements) {
        this.resources = resources;
        this.tryStatements = tryStatements;
        this.catchClauses = catchClauses;
        this.finallyStatements = finallyStatements;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public BaseStatement getTryStatements() {
        return tryStatements;
    }

    public void setTryStatements(BaseStatement tryStatements) {
        this.tryStatements = tryStatements;
    }

    public List<CatchClause> getCatchClauses() {
        return catchClauses;
    }

    public BaseStatement getFinallyStatements() {
        return finallyStatements;
    }

    public void setFinallyStatements(BaseStatement finallyStatements) {
        this.finallyStatements = finallyStatements;
    }

    public static class Resource implements Statement {
        protected ObjectType type;
        protected String name;
        protected Expression expression;

        public Resource(ObjectType type, String name, Expression expression) {
            this.type = type;
            this.name = name;
            this.expression = expression;
        }

        public ObjectType getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public Expression getExpression() {
            return expression;
        }

        public void setExpression(Expression expression) {
            this.expression = expression;
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class CatchClause implements Statement {
        protected int lineNumber;
        protected ObjectType type;
        protected DefaultList<ObjectType> otherTypes = null;
        protected String name;
        protected BaseStatement statements;

        public CatchClause(int lineNumber, ObjectType type, String name, BaseStatement statements) {
            this.lineNumber = lineNumber;
            this.type = type;
            this.name = name;
            this.statements = statements;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public ObjectType getType() {
            return type;
        }

        public DefaultList<ObjectType> getOtherTypes() {
            return otherTypes;
        }

        public String getName() {
            return name;
        }

        public BaseStatement getStatements() {
            return statements;
        }

        public void addType(ObjectType type) {
            if (otherTypes == null)
                otherTypes = new DefaultList<>();
            otherTypes.add(type);
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visit(this);
        }
    }

    @Override
    public void accept(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
