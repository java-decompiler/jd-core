/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.statement.AssertStatement;
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
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.model.javasyntax.statement.SynchronizedStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.TypeDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;

public class RemoveLastContinueStatementVisitor extends AbstractJavaSyntaxVisitor {
    @Override
    public void visit(Statements list) {
        if (! list.isEmpty()) {
            Statement last = list.getLast();

            if (last instanceof ContinueStatement) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                list.removeLast();
                visit(list);
            } else {
                last.accept(this);
            }
        }
    }

    @Override
    public void visit(IfElseStatement statement) {
        safeAccept(statement.getStatements());
        statement.getElseStatements().accept(this);
    }

    @Override
    public void visit(TryStatement statement) {
        statement.getTryStatements().accept(this);
        safeAcceptListStatement(statement.getCatchClauses());
        safeAccept(statement.getFinallyStatements());
    }

    @Override
    public void visit(SwitchStatement statement) { acceptListStatement(statement.getBlocks()); }
    @Override
    public void visit(SwitchStatement.LabelBlock statement) { statement.getStatements().accept(this); }
    @Override
    public void visit(SwitchStatement.MultiLabelsBlock statement) { statement.getStatements().accept(this); }
    @Override
    public void visit(IfStatement statement) { safeAccept(statement.getStatements()); }
    @Override
    public void visit(SynchronizedStatement statement) { safeAccept(statement.getStatements()); }
    @Override
    public void visit(TryStatement.CatchClause statement) { safeAccept(statement.getStatements()); }

    @Override
    public void visit(DoWhileStatement statement) {}
    @Override
    public void visit(ForEachStatement statement) {}
    @Override
    public void visit(ForStatement statement) {}
    @Override
    public void visit(WhileStatement statement) {}
    @Override
    public void visit(AssertStatement statement) {}
    @Override
    public void visit(BreakStatement statement) {}
    @Override
    public void visit(CommentStatement statement) {}
    @Override
    public void visit(ContinueStatement statement) {}
    @Override
    public void visit(ExpressionStatement statement) {}
    @Override
    public void visit(LabelStatement statement) {}
    @Override
    public void visit(LambdaExpressionStatement statement) {}
    @Override
    public void visit(LocalVariableDeclarationStatement statement) {}
    @Override
    public void visit(ReturnExpressionStatement statement) {}
    @Override
    public void visit(ReturnStatement statement) {}
    @Override
    public void visit(SwitchStatement.ExpressionLabel statement) {}
    @Override
    public void visit(ThrowStatement statement) {}
    @Override
    public void visit(TypeDeclarationStatement statement) {}
    @Override
    public void visit(TryStatement.Resource statement) {}
}
