/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.statement.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.LocalVariableMaker;
import org.jd.core.v1.util.DefaultList;

import java.util.List;

public class RemoveFinallyStatementsVisitor implements StatementVisitor {
    protected static final DeclaredSyntheticLocalVariableVisitor DECLARED_SYNTHETIC_LOCAL_VARIABLE_VISITOR = new DeclaredSyntheticLocalVariableVisitor();

    protected LocalVariableMaker localVariableMaker;
    protected int statementCountInFinally;
    protected int statementCountToRemove;

    public RemoveFinallyStatementsVisitor(LocalVariableMaker localVariableMaker) {
        this.localVariableMaker = localVariableMaker;
    }

    public void init() {
        this.statementCountInFinally = 0;
        this.statementCountToRemove = 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(Statements statements) {
        DefaultList<Statement> stmts = statements;
        int size = statements.size();

        if (size > 0) {
            int i = size;
            int oldStatementCountToRemove = statementCountToRemove;

            // Check last statement
            Statement lastStatement = stmts.getLast();
            Class lastStatementClass = lastStatement.getClass();

            if ((lastStatementClass == ReturnExpressionStatement.class) || (lastStatementClass == ReturnStatement.class)) {
                statementCountToRemove = statementCountInFinally;
                i--;
            } else if (lastStatementClass == ThrowStatement.class) {
                statementCountToRemove = 0;
                i--;
            } else {
                WhileStatement whileStatement = getInfiniteWhileStatement(lastStatement);

                if (whileStatement != null) {
                    // Infinite loop => Do not remove any statement
                    statementCountToRemove = 0;
                    i--;
                    whileStatement.getStatements().accept(this);
                }
            }

            // Remove 'finally' statements
            if (statementCountToRemove > 0) {
                // Remove 'finally' statements
                if (i > statementCountToRemove) {
                    List<Statement> list = statements.subList(i - statementCountToRemove, i);

                    for (Statement statement : list) {
                        statement.accept(DECLARED_SYNTHETIC_LOCAL_VARIABLE_VISITOR);
                    }

                    lastStatement.accept(DECLARED_SYNTHETIC_LOCAL_VARIABLE_VISITOR);
                    list.clear();
                    i -= statementCountToRemove;
                    statementCountToRemove = 0;
                } else {
                    List<Statement> list = statements;

                    for (Statement statement : list) {
                        statement.accept(DECLARED_SYNTHETIC_LOCAL_VARIABLE_VISITOR);
                    }

                    list.clear();
                    if (i < size) {
                        list.add(lastStatement);
                    }
                    statementCountToRemove -= i;
                    i = 0;
                }
            }

            // Recursive visit
            while (i-- > 0) {
                stmts.get(i).accept(this);

                if (statementCountToRemove > 0) {
                    if (i + statementCountToRemove < statements.size()) {
                        statements.subList(i + 1, i + 1 + statementCountToRemove).clear();
                        statementCountToRemove = 0;
                    } else {
                        //assert false : "Error. Eclipse try-finally ?";
                    }
                }
            }

            statementCountToRemove = oldStatementCountToRemove;
        }
    }

    private static WhileStatement getInfiniteWhileStatement(Statement statement) {
        if (statement.getClass() == LabelStatement.class) {
            statement = ((LabelStatement)statement).getStatement();
        }

        if ((statement == null) || (statement.getClass() != WhileStatement.class)) {
            return null;
        }

        WhileStatement whileStatement = (WhileStatement)statement;

        if (whileStatement.getCondition().getClass() != BooleanExpression.class) {
            return null;
        }

        BooleanExpression booleanExpression = (BooleanExpression)whileStatement.getCondition();

        if (booleanExpression.isFalse()) {
            return null;
        }

        return whileStatement;
    }

    @Override
    public void visit(IfElseStatement statement) {
        statement.getStatements().accept(this);
        statement.getElseStatements().accept(this);
    }

    @Override
    public void visit(SwitchStatement statement) {
        for (SwitchStatement.Block block : statement.getBlocks()) {
            block.getStatements().accept(this);
        }
    }

    @Override
    public void visit(TryStatement statement) {
        ClassFileTryStatement ts = (ClassFileTryStatement)statement;
        Statements tryStatements = (Statements)ts.getTryStatements();
        Statements finallyStatements = (Statements)ts.getFinallyStatements();

        safeAccept(finallyStatements);

        if (ts.isJsr() || (finallyStatements == null) || (finallyStatements.size() == 0)) {
            tryStatements.accept(this);
            safeAcceptListStatement(statement.getCatchClauses());
        } else if (ts.isEclipse()) {
            List<TryStatement.CatchClause> catchClauses = statement.getCatchClauses();
            int oldStatementCountInFinally = statementCountInFinally;
            int finallyStatementsSize = finallyStatements.size();

            statementCountInFinally += finallyStatementsSize;

            removeFinallyStatements(tryStatements);

            statementCountToRemove = finallyStatementsSize;

            if (catchClauses != null) {
                for (TryStatement.CatchClause cc : catchClauses) {
                    removeFinallyStatements((Statements)cc.getStatements());
                }
            }

            statementCountInFinally = oldStatementCountInFinally;

            if (statement.getResources() != null) {
                ts.setFinallyStatements(null);
            }
        } else {
            List<TryStatement.CatchClause> catchClauses = statement.getCatchClauses();
            int oldStatementCountInFinally = statementCountInFinally;
            int oldStatementCountToRemove = statementCountToRemove;
            int finallyStatementsSize = finallyStatements.size();

            statementCountInFinally += finallyStatementsSize;
            statementCountToRemove += finallyStatementsSize;

            removeFinallyStatements(tryStatements);

            if (catchClauses != null) {
                for (TryStatement.CatchClause cc : catchClauses) {
                    removeFinallyStatements((Statements)cc.getStatements());
                }
            }

            statementCountInFinally = oldStatementCountInFinally;
            statementCountToRemove = oldStatementCountToRemove;

            if (statement.getResources() != null) {
                ts.setFinallyStatements(null);
            }
        }
    }

    public void removeFinallyStatements(Statements list) {
        if ((list.size() == 1) && (list.get(0).getClass() == ClassFileTryStatement.class)) {
            int oldStatementCountToRemove = statementCountToRemove;

            assert list.getFirst().getClass() == ClassFileTryStatement.class;

            statementCountToRemove = 0;
            list.accept(this);
            statementCountToRemove = oldStatementCountToRemove;
        } else {
            list.accept(this);
        }
    }

    @Override public void visit(DoWhileStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(ForEachStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(ForStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(IfStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(SynchronizedStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(TryStatement.CatchClause statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(WhileStatement statement) { safeAccept(statement.getStatements()); }

    @Override public void visit(SwitchStatement.LabelBlock statement) { statement.getStatements().accept(this); }
    @Override public void visit(SwitchStatement.MultiLabelsBlock statement) { statement.getStatements().accept(this); }

    @Override public void visit(AssertStatement statement) {}
    @Override public void visit(BreakStatement statement) {}
    @Override public void visit(ByteCodeStatement statement) {}
    @Override public void visit(CommentStatement statement) {}
    @Override public void visit(ContinueStatement statement) {}
    @Override public void visit(ExpressionStatement statement) {}
    @Override public void visit(LabelStatement statement) {}
    @Override public void visit(LambdaExpressionStatement statement) {}
    @Override public void visit(LocalVariableDeclarationStatement statement) {}
    @Override public void visit(ReturnExpressionStatement statement) {}
    @Override public void visit(ReturnStatement statement) {}
    @Override public void visit(SwitchStatement.DefaultLabel statement) {}
    @Override public void visit(SwitchStatement.ExpressionLabel statement) {}
    @Override public void visit(ThrowStatement statement) {}
    @Override public void visit(TryStatement.Resource statement) {}
    @Override public void visit(TypeDeclarationStatement statement) {}

    protected void safeAccept(BaseStatement list) {
        if (list != null)
            list.accept(this);
    }

    protected void safeAcceptListStatement(List<? extends Statement> list) {
        if (list != null) {
            for (Statement statement : list)
                statement.accept(this);
        }
    }
}
