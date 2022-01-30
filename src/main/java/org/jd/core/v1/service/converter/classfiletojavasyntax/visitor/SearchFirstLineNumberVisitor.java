/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.ArrayExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.CommentExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.EnumConstantReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaFormalParametersExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LengthExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NewArray;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.NewInitializedArray;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.ParenthesesExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.expression.TypeReferenceDotClassExpression;
import org.jd.core.v1.model.javasyntax.statement.AssertStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LambdaExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.model.javasyntax.statement.SynchronizedStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;

import java.util.List;

public class SearchFirstLineNumberVisitor extends AbstractJavaSyntaxVisitor {
    private int lineNumber;

    public SearchFirstLineNumberVisitor() {
        lineNumber = -1;
    }

    public void init() {
        lineNumber = -1;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public void visit(Statements statements) {
        if (lineNumber == -1) {
            List<Statement> list = statements;

            for (Statement statement : list) {
                statement.accept(this);

                if (lineNumber != -1) {
                    break;
                }
            }
        }
    }

    @Override
    public void visit(ArrayExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(BinaryOperatorExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(BooleanExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(CastExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(CommentExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(ConstructorInvocationExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(ConstructorReferenceExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(DoubleConstantExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(EnumConstantReferenceExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(FieldReferenceExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(FloatConstantExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(IntegerConstantExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(InstanceOfExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(LambdaFormalParametersExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(LambdaIdentifiersExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(LengthExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(LocalVariableReferenceExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(LongConstantExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(MethodReferenceExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(NewArray expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(NewExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(NewInitializedArray expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(NullExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(ObjectTypeReferenceExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(ParenthesesExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(PostOperatorExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(PreOperatorExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(StringConstantExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(SuperExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(TernaryOperatorExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(ThisExpression expression) { lineNumber = expression.getLineNumber(); }
    @Override
    public void visit(TypeReferenceDotClassExpression expression) { lineNumber = expression.getLineNumber(); }

    @Override
    public void visit(AssertStatement statement) {
        statement.getCondition().accept(this);
    }

    @Override
    public void visit(ExpressionStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(ForEachStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(IfStatement statement) {
        statement.getCondition().accept(this);
    }

    @Override
    public void visit(IfElseStatement statement) {
        statement.getCondition().accept(this);
    }

    @Override
    public void visit(LambdaExpressionStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(MethodInvocationExpression expression) {
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(SwitchStatement statement) {
        statement.getCondition().accept(this);
    }

    @Override
    public void visit(SynchronizedStatement statement) {
        statement.getMonitor().accept(this);
    }

    @Override
    public void visit(ThrowStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(WhileStatement statement) {
        if (statement.getCondition() != null) {
            statement.getCondition().accept(this);
        } else if (statement.getStatements() != null) {
            statement.getStatements().accept(this);
        }
    }

    @Override
    public void visit(DoWhileStatement statement) {
        if (statement.getStatements() != null) {
            statement.getStatements().accept(this);
        } else if (statement.getCondition() != null) {
            statement.getCondition().accept(this);
        }
    }

    @Override
    public void visit(ForStatement statement) {
        if (statement.getInit() != null) {
            statement.getInit().accept(this);
        } else if (statement.getCondition() != null) {
            statement.getCondition().accept(this);
        } else if (statement.getUpdate() != null) {
            statement.getUpdate().accept(this);
        } else if (statement.getStatements() != null) {
            statement.getStatements().accept(this);
        }
    }

    @Override
    public void visit(ReturnExpressionStatement statement) {
        statement.getExpression().accept(this);
    }

    @Override
    public void visit(TryStatement statement) {
        if (statement.getResources() != null) {
            acceptListStatement(statement.getResources());
        } else {
            statement.getTryStatements().accept(this);
        }
    }
}
