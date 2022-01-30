/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.statement.AssertStatement;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.CommentStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LabelStatement;
import org.jd.core.v1.model.javasyntax.statement.LambdaExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.NoStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.StatementVisitor;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.model.javasyntax.statement.SynchronizedStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.TypeDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;

import java.util.List;

public class MergeTryWithResourcesStatementVisitor implements StatementVisitor {

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
        BaseStatement tryStatements = statement.getTryStatements();

        safeAcceptListStatement(statement.getResources());
        tryStatements.accept(this);
        safeAcceptListStatement(statement.getCatchClauses());
        safeAccept(statement.getFinallyStatements());

        if (tryStatements.size() == 1) {
            Statement first = tryStatements.getFirst();

            if (first.isTryStatement()) {
                ClassFileTryStatement cfswrs1 = (ClassFileTryStatement)statement;
                ClassFileTryStatement cfswrs2 = (ClassFileTryStatement)first;

                if (cfswrs2.getResources() != null && cfswrs2.getCatchClauses() == null && cfswrs2.getFinallyStatements() == null) {
                    // Merge 'try' and 'try-with-resources" statements
                    cfswrs1.setTryStatements(cfswrs2.getTryStatements());
                    cfswrs1.addResources(cfswrs2.getResources());
                }
            }
        }
    }

    @Override public void visit(DoWhileStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(ForEachStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(ForStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(IfStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(Statements list) { acceptListStatement(list); }
    @Override public void visit(SynchronizedStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(TryStatement.CatchClause statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(WhileStatement statement) { safeAccept(statement.getStatements()); }

    @Override public void visit(SwitchStatement.LabelBlock statement) { statement.getStatements().accept(this); }
    @Override public void visit(SwitchStatement.MultiLabelsBlock statement) { statement.getStatements().accept(this); }

    @Override public void visit(AssertStatement statement) {}
    @Override public void visit(BreakStatement statement) {}
    @Override public void visit(CommentStatement statement) {}
    @Override public void visit(ContinueStatement statement) {}
    @Override public void visit(ExpressionStatement statement) {}
    @Override public void visit(LabelStatement statement) {}
    @Override public void visit(LambdaExpressionStatement statement) {}
    @Override public void visit(LocalVariableDeclarationStatement statement) {}
    @Override public void visit(NoStatement statement) {}
    @Override public void visit(ReturnExpressionStatement statement) {}
    @Override public void visit(ReturnStatement statement) {}
    @Override public void visit(SwitchStatement.DefaultLabel statement) {}
    @Override public void visit(SwitchStatement.ExpressionLabel statement) {}
    @Override public void visit(ThrowStatement statement) {}
    @Override public void visit(TryStatement.Resource statement) {}
    @Override public void visit(TypeDeclarationStatement statement) {}

    protected void safeAccept(BaseStatement list) {
        if (list != null) {
            list.accept(this);
        }
    }

    protected void acceptListStatement(List<? extends Statement> list) {
        for (Statement statement : list) {
            statement.accept(this);
        }
    }

    protected void safeAcceptListStatement(List<? extends Statement> list) {
        if (list != null) {
            for (Statement statement : list) {
                statement.accept(this);
            }
        }
    }
}
