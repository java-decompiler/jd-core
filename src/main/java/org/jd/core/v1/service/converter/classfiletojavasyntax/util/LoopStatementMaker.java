/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.statement.*;
import org.jd.core.v1.model.javasyntax.type.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileForEachStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileForStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.GenericLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.ObjectLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import static org.jd.core.v1.model.javasyntax.statement.ContinueStatement.CONTINUE;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_ITERABLE;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_OBJECT;

public class LoopStatementMaker {
    protected static final RemoveLastContinueStatementVisitor REMOVE_LAST_CONTINUE_STATEMENT_VISITOR = new RemoveLastContinueStatementVisitor();

    public static Statement makeLoop(
            int majorVersion, Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker,
            BasicBlock loopBasicBlock, Statements statements, Expression condition, Statements subStatements,
            Statements jumps) {
        Statement loop = makeLoop(majorVersion, typeBounds, localVariableMaker, loopBasicBlock, statements, condition, subStatements);
        int continueOffset = loopBasicBlock.getSub1().getFromOffset();
        int breakOffset = loopBasicBlock.getNext().getFromOffset();

        if (breakOffset <= 0) {
            breakOffset = loopBasicBlock.getToOffset();
        }

        return makeLabels(loopBasicBlock.getIndex(), continueOffset, breakOffset, loop, jumps);
    }

    protected static Statement makeLoop(
            int majorVersion, Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker,
            BasicBlock loopBasicBlock, Statements statements, Expression condition, Statements subStatements) {
        boolean forEachSupported = (majorVersion >= 49); // (majorVersion >= Java 5)

        subStatements.accept(REMOVE_LAST_CONTINUE_STATEMENT_VISITOR);

        if (forEachSupported) {
            Statement statement = makeForEachArray(typeBounds, localVariableMaker, statements, condition, subStatements);

            if (statement != null) {
                return statement;
            }

            statement = makeForEachList(typeBounds, localVariableMaker, statements, condition, subStatements);

            if (statement != null) {
                return statement;
            }
        }

        int lineNumber = (condition == null) ? Expression.UNKNOWN_LINE_NUMBER : condition.getLineNumber();
        int subStatementsSize = subStatements.size();

        switch (subStatementsSize) {
            case 0:
                if (lineNumber > 0) {
                    // Known line numbers
                    BaseExpression init = extractInit(statements, lineNumber);

                    if (init != null) {
                        return newClassFileForStatement(localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, null, null);
                    }
                }
                break;
            case 1:
                if (lineNumber > 0) {
                    // Known line numbers
                    Statement subStatement = subStatements.getFirst();
                    BaseExpression init = extractInit(statements, lineNumber);

                    if (subStatement.getClass() == ExpressionStatement.class) {
                        Expression subExpression = ((ExpressionStatement) subStatement).getExpression();

                        if (subExpression.getLineNumber() == lineNumber) {
                            return newClassFileForStatement(
                                localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, subExpression, null);
                        } else if (init != null) {
                            return newClassFileForStatement(
                                localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, null, subStatement);
                        }
                    } else if (init != null) {
                        return newClassFileForStatement(
                            localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, null, subStatement);
                    }
                } else {
                    // Unknown line numbers => Just try to find 'for (expression;;expression)'
                    return createForStatementWithoutLineNumber(localVariableMaker, loopBasicBlock, statements, condition, subStatements);
                }
                break;
            default:
                if (lineNumber > 0) {
                    // Known line numbers
                    SearchFirstLineNumberVisitor visitor = new SearchFirstLineNumberVisitor();

                    subStatements.get(0).accept(visitor);

                    int firstLineNumber = visitor.getLineNumber();

                    // Populates 'update'
                    Expressions update = extractUpdate(subStatements, firstLineNumber);
                    BaseExpression init = extractInit(statements, lineNumber);

                    if ((init != null) || (update.size() > 0)) {
                        return newClassFileForStatement(
                            localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, update, subStatements);
                    }
                } else {
                    // Unknown line numbers => Just try to find 'for (expression;;expression)'
                    return createForStatementWithoutLineNumber(localVariableMaker, loopBasicBlock, statements, condition, subStatements);
                }
                break;
        }

        return new WhileStatement(condition, subStatements);
    }

    public static Statement makeLoop(LocalVariableMaker localVariableMaker, BasicBlock loopBasicBlock, Statements statements, Statements subStatements, Statements jumps) {
        subStatements.accept(REMOVE_LAST_CONTINUE_STATEMENT_VISITOR);

        Statement loop = makeLoop(localVariableMaker, loopBasicBlock, statements, subStatements);
        int continueOffset = loopBasicBlock.getSub1().getFromOffset();
        int breakOffset = loopBasicBlock.getNext().getFromOffset();

        if (breakOffset <= 0) {
            breakOffset = loopBasicBlock.getToOffset();
        }

        return makeLabels(loopBasicBlock.getIndex(), continueOffset, breakOffset, loop, jumps);
    }

    protected static Statement makeLoop(LocalVariableMaker localVariableMaker, BasicBlock loopBasicBlock, Statements statements, Statements subStatements) {
        int subStatementsSize = subStatements.size();

        if ((subStatementsSize > 0) && (subStatements.getLast() == CONTINUE)) {
            subStatements.removeLast();
            subStatementsSize--;
        }

        switch (subStatementsSize) {
            case 0:
                break;
            case 1:
                Statement subStatement = subStatements.getFirst();

                if (subStatement.getClass() == ExpressionStatement.class) {
                    Expression subExpression = ((ExpressionStatement) subStatement).getExpression();
                    int lineNumber = subExpression.getLineNumber();

                    if (lineNumber > 0) {
                        // Known line numbers
                        BaseExpression init = extractInit(statements, lineNumber);

                        if (init != null) {
                            return newClassFileForStatement(localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, null, subExpression, null);
                        }
                    }
                }
                break;
            default:
                SearchFirstLineNumberVisitor visitor = new SearchFirstLineNumberVisitor();

                subStatements.get(0).accept(visitor);

                int firstLineNumber = visitor.getLineNumber();

                if (firstLineNumber > 0) {
                    // Populates 'update'
                    Expressions update = extractUpdate(subStatements, firstLineNumber);

                    if (update.size() > 0) {
                        // Populates 'init'
                        BaseExpression init = extractInit(statements, update.getFirst().getLineNumber());

                        return newClassFileForStatement(localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, null, update, subStatements);
                    }
                } else {
                    // Unknown line numbers => Just try to find 'for (expression;;expression)'
                    Statement statement = createForStatementWithoutLineNumber(localVariableMaker, loopBasicBlock, statements, BooleanExpression.TRUE, subStatements);

                    if (statement != null) {
                        return statement;
                    }
                }
        }

        return new WhileStatement(BooleanExpression.TRUE, subStatements);
    }

    public static Statement makeDoWhileLoop(BasicBlock loopBasicBlock, Expression condition, Statements subStatements, Statements jumps) {
        subStatements.accept(REMOVE_LAST_CONTINUE_STATEMENT_VISITOR);

        Statement loop = new DoWhileStatement(condition, subStatements);
        int continueOffset = loopBasicBlock.getSub1().getFromOffset();
        int breakOffset = loopBasicBlock.getNext().getFromOffset();

        if (breakOffset <= 0) {
            breakOffset = loopBasicBlock.getToOffset();
        }

        return makeLabels(loopBasicBlock.getIndex(), continueOffset, breakOffset, loop, jumps);
    }

    @SuppressWarnings("unchecked")
    protected static BaseExpression extractInit(Statements statements, int lineNumber) {
        if (lineNumber > 0) {
            switch (statements.size()) {
                case 0:
                    break;
                case 1:
                    Statement statement = statements.getFirst();

                    if (statement.getClass() != ExpressionStatement.class) {
                        break;
                    }

                    Expression expression = ((ExpressionStatement) statement).getExpression();

                    if ((expression.getLineNumber() != lineNumber) ||
                        (expression.getClass() != BinaryOperatorExpression.class) ||
                        (((BinaryOperatorExpression) expression).getRightExpression().getLineNumber() == 0)) {
                        break;
                    }

                    statements.clear();
                    return expression;
                default:
                    Expressions init = new Expressions();
                    ListIterator<Statement> iterator = statements.listIterator(statements.size());

                    while (iterator.hasPrevious()) {
                        statement = iterator.previous();

                        if (statement.getClass() != ExpressionStatement.class) {
                            break;
                        }

                        expression = ((ExpressionStatement) statement).getExpression();

                        if ((expression.getLineNumber() != lineNumber) ||
                            (expression.getClass() != BinaryOperatorExpression.class) ||
                            (((BinaryOperatorExpression) expression).getRightExpression().getLineNumber() == 0)) {
                            break;
                        }

                        init.add(expression);
                        iterator.remove();
                    }

                    if (init.size() > 0) {
                        if (init.size() > 1)
                            Collections.reverse(init);
                        return init;
                    }
                    break;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected static Expressions extractUpdate(Statements statements, int firstLineNumber) {
        Expressions update = new Expressions();
        ListIterator<Statement> iterator = statements.listIterator(statements.size());

        // Populates 'update'
        while (iterator.hasPrevious()) {
            Statement statement = iterator.previous();

            if (statement.getClass() != ExpressionStatement.class)
                break;
            Expression expression = ((ExpressionStatement)statement).getExpression();
            if (expression.getLineNumber() >= firstLineNumber)
                break;

            iterator.remove();
            update.add(expression);
        }

        if (update.size() > 1) {
            Collections.reverse(update);
        }

        return update;
    }

    protected static Statement createForStatementWithoutLineNumber(
            LocalVariableMaker localVariableMaker, BasicBlock basicBlock, Statements statements, Expression condition, Statements subStatements) {

        if (!statements.isEmpty()) {
            Statement statement = statements.getLast();

            if (statement.getClass() == ExpressionStatement.class) {
                Expression init = ((ExpressionStatement) statement).getExpression();

                if (init.getClass() == BinaryOperatorExpression.class) {
                    BinaryOperatorExpression boe = (BinaryOperatorExpression) init;

                    if (boe.getLeftExpression().getClass() == ClassFileLocalVariableReferenceExpression.class) {
                        AbstractLocalVariable localVariable = ((ClassFileLocalVariableReferenceExpression) boe.getLeftExpression()).getLocalVariable();

                        statement = subStatements.getLast();

                        if (statement.getClass() == ExpressionStatement.class) {
                            Expression update = ((ExpressionStatement) statement).getExpression();
                            Class clazz = update.getClass();
                            Expression expression;

                            if (clazz == BinaryOperatorExpression.class) {
                                expression = ((BinaryOperatorExpression) update).getLeftExpression();
                            } else if (clazz == PreOperatorExpression.class) {
                                PreOperatorExpression poe = (PreOperatorExpression) update;
                                expression = poe.getExpression();
                                update = new PostOperatorExpression(poe.getLineNumber(), expression, poe.getOperator());
                            } else if (clazz == PostOperatorExpression.class) {
                                expression = ((PostOperatorExpression) update).getExpression();
                            } else {
                                return new WhileStatement(condition, subStatements);
                            }

                            if ((expression.getClass() == ClassFileLocalVariableReferenceExpression.class) &&
                                    (((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable() == localVariable)) {
                                statements.removeLast();
                                subStatements.removeLast();

                                if (condition == BooleanExpression.TRUE) {
                                    condition = null;
                                }

                                return newClassFileForStatement(
                                    localVariableMaker, basicBlock.getFromOffset(), basicBlock.getToOffset(), init, condition, update, subStatements);
                            }
                        }
                    }
                }
            }
        }

        return new WhileStatement(condition, subStatements);
    }

    protected static Statement makeForEachArray(
            Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker, Statements statements,
            Expression condition, Statements subStatements) {
        if (condition == null) {
            return null;
        }

        int statementsSize = statements.size();

        if ((statementsSize < 3) || (subStatements.size() < 2)) {
            return null;
        }

        // len$ = arr$.length;
        Statement statement = statements.get(statementsSize-2);

        if (statement.getClass() != ExpressionStatement.class) {
            return null;
        }

        int lineNumber = condition.getLineNumber();
        Expression expression = ((ExpressionStatement)statement).getExpression();

        if ((expression.getLineNumber() != lineNumber) || (expression.getClass() != BinaryOperatorExpression.class)) {
            return null;
        }

        BinaryOperatorExpression boe = (BinaryOperatorExpression)expression;

        if ((boe.getRightExpression().getClass() != LengthExpression.class) || (boe.getLeftExpression().getClass() != ClassFileLocalVariableReferenceExpression.class)) {
            return null;
        }

        expression = ((LengthExpression)boe.getRightExpression()).getExpression();

        if (expression.getClass() != ClassFileLocalVariableReferenceExpression.class) {
            return null;
        }

        AbstractLocalVariable syntheticArray = ((ClassFileLocalVariableReferenceExpression)expression).getLocalVariable();
        AbstractLocalVariable syntheticLength = ((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable();

        if ((syntheticArray.getName() != null) && (syntheticArray.getName().indexOf('$') == -1)) {
            return null;
        }

        // String s = arr$[i$];
        statement = subStatements.getFirst();

        if (statement.getClass() != ExpressionStatement.class) {
            return null;
        }

        expression = ((ExpressionStatement)statement).getExpression();

        if (expression.getClass() != BinaryOperatorExpression.class) {
            return null;
        }

        boe = (BinaryOperatorExpression)expression;

        if ((boe.getRightExpression().getClass() != ArrayExpression.class) ||
                (boe.getLeftExpression().getClass() != ClassFileLocalVariableReferenceExpression.class) ||
                (boe.getLineNumber() != condition.getLineNumber())) {
            return null;
        }

        ArrayExpression arrayExpression = (ArrayExpression)boe.getRightExpression();

        if ((arrayExpression.getExpression().getClass() != ClassFileLocalVariableReferenceExpression.class) ||
                (arrayExpression.getIndex().getClass() != ClassFileLocalVariableReferenceExpression.class)) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)arrayExpression.getExpression()).getLocalVariable() != syntheticArray) {
            return null;
        }

        AbstractLocalVariable syntheticIndex = ((ClassFileLocalVariableReferenceExpression)arrayExpression.getIndex()).getLocalVariable();
        AbstractLocalVariable item = ((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable();

        if ((syntheticIndex.getName() != null) && (syntheticIndex.getName().indexOf('$') == -1)) {
            return null;
        }

        // arr$ = array;
        statement = statements.get(statementsSize-3);

        if (statement.getClass() != ExpressionStatement.class) {
            return null;
        }

        expression = ((ExpressionStatement)statement).getExpression();

        if (expression.getClass() != BinaryOperatorExpression.class) {
            return null;
        }

        boe = (BinaryOperatorExpression)expression;

        if (boe.getLeftExpression().getClass() != ClassFileLocalVariableReferenceExpression.class) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable() != syntheticArray) {
            return null;
        }

        Type arrayType = boe.getRightExpression().getType();
        Expression array = boe.getRightExpression();

        // i$ = 0;
        statement = statements.get(statementsSize-1);

        if (statement.getClass() != ExpressionStatement.class) {
            return null;
        }

        expression = ((ExpressionStatement)statement).getExpression();

        if (expression.getClass() != BinaryOperatorExpression.class) {
            return null;
        }

        boe = (BinaryOperatorExpression)expression;

        if ((boe.getLineNumber() != lineNumber) ||
                (boe.getLeftExpression().getClass() != ClassFileLocalVariableReferenceExpression.class) ||
                (boe.getRightExpression().getClass() != IntegerConstantExpression.class)) {
            return null;
        }
        if ((((IntegerConstantExpression)boe.getRightExpression()).getValue() != 0) ||
                (((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable() != syntheticIndex)) {
            return null;
        }

        // i$ < len$;
        if (condition.getClass() != BinaryOperatorExpression.class) {
            return null;
        }

        boe = (BinaryOperatorExpression)condition;

        if ((boe.getLeftExpression().getClass() != ClassFileLocalVariableReferenceExpression.class) ||
                (boe.getRightExpression().getClass() != ClassFileLocalVariableReferenceExpression.class)) {
            return null;
        }
        if ((((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable() != syntheticIndex) ||
                (((ClassFileLocalVariableReferenceExpression)boe.getRightExpression()).getLocalVariable() != syntheticLength)) {
            return null;
        }

        // ++i$;
        statement = subStatements.getLast();

        if (statement.getClass() != ExpressionStatement.class) {
            return null;
        }

        expression = ((ExpressionStatement)statement).getExpression();

        if ((expression.getLineNumber() != lineNumber) || (expression.getClass() != PostOperatorExpression.class)) {
            return null;
        }

        // Found
        statements.removeLast();
        statements.removeLast();
        statements.removeLast();

        subStatements.removeFirst();
        subStatements.removeLast();

        item.setDeclared(true);
        Type type = arrayType.createType(arrayType.getDimension()-1);

        if (ObjectType.TYPE_OBJECT.equals(item.getType())) {
            ((ObjectLocalVariable)item).setType(typeBounds, type);
        } else if (item.getType().isGeneric()) {
            ((GenericLocalVariable)item).setType((GenericType)type);
        } else {
            item.typeOnRight(typeBounds, type);
        }

        localVariableMaker.removeLocalVariable(syntheticArray);
        localVariableMaker.removeLocalVariable(syntheticIndex);
        localVariableMaker.removeLocalVariable(syntheticLength);

        return new ClassFileForEachStatement(item, array, subStatements);
    }

    protected static Statement makeForEachList(
            Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker, Statements statements,
            Expression condition, Statements subStatements) {
        if (condition == null) {
            return null;
        }

        if ((statements.size() < 1) || (subStatements.size() < 1)) {
            return null;
        }

        // i$.hasNext();
        if (condition.getClass() != ClassFileMethodInvocationExpression.class) {
            return null;
        }

        MethodInvocationExpression mie = (MethodInvocationExpression)condition;

        if (!mie.getName().equals("hasNext") || !mie.getInternalTypeName().equals("java/util/Iterator") ||
                (mie.getExpression().getClass() != ClassFileLocalVariableReferenceExpression.class)) {
            return null;
        }

        AbstractLocalVariable syntheticIterator = ((ClassFileLocalVariableReferenceExpression)mie.getExpression()).getLocalVariable();

        // Iterator i$ = list.iterator();
        Statement lastStatement = statements.getLast();

        if (lastStatement.getClass() != ExpressionStatement.class) {
            return null;
        }

        ExpressionStatement es = (ExpressionStatement)lastStatement;

        if (es.getExpression().getClass() != BinaryOperatorExpression.class) {
            return null;
        }

        BinaryOperatorExpression boe = (BinaryOperatorExpression)es.getExpression();

        if ((boe == null) ||
                (boe.getLeftExpression().getClass() != ClassFileLocalVariableReferenceExpression.class) ||
                (boe.getRightExpression().getClass() != ClassFileMethodInvocationExpression.class) ||
                (boe.getLineNumber() != condition.getLineNumber())) {
            return null;
        }

        mie = (MethodInvocationExpression)boe.getRightExpression();

        if (!mie.getName().equals("iterator") || !mie.getDescriptor().equals("()Ljava/util/Iterator;")) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable() != syntheticIterator) {
            return null;
        }

        Expression list = mie.getExpression();

        if (list.getClass() == CastExpression.class) {
            list = ((CastExpression)list).getExpression();
        }

        // String s = (String)i$.next();
        Statement firstSubStatement = subStatements.get(0);

        if (firstSubStatement.getClass() != ExpressionStatement.class) {
            return null;
        }

        es = (ExpressionStatement)firstSubStatement;

        if (es.getExpression().getClass() != BinaryOperatorExpression.class) {
            return null;
        }

        boe = (BinaryOperatorExpression)es.getExpression();

        if (boe.getLeftExpression().getClass() != ClassFileLocalVariableReferenceExpression.class) {
            return null;
        }

        Expression expression = boe.getRightExpression();

        if (boe.getRightExpression().getClass() == CastExpression.class) {
            expression = ((CastExpression)expression).getExpression();
        }
        if (expression.getClass() != ClassFileMethodInvocationExpression.class) {
            return null;
        }

        mie = (MethodInvocationExpression)expression;

        if (!mie.getName().equals("next") ||
                !mie.getInternalTypeName().equals("java/util/Iterator") ||
                (mie.getExpression().getClass() != ClassFileLocalVariableReferenceExpression.class)) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)mie.getExpression()).getLocalVariable() != syntheticIterator) {
            return null;
        }

        // Check if 'i$' is not used in sub-statements
        SearchLocalVariableReferenceVisitor visitor1 = new SearchLocalVariableReferenceVisitor();

        visitor1.init(syntheticIterator.getIndex());

        for (int i=1, len=subStatements.size(); i<len; i++) {
            subStatements.get(i).accept(visitor1);
        }

        if (visitor1.containsReference()) {
            return null;
        }

        // Found
        AbstractLocalVariable item = ((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable();

        statements.removeLast();
        subStatements.remove(0);
        item.setDeclared(true);

        if (syntheticIterator.getReferences().size() == 3) {
            // If local variable is used only for this loop
            localVariableMaker.removeLocalVariable(syntheticIterator);
        }

        Type type = list.getType();

        if (type.isObject()) {
            ObjectType listType = (ObjectType)type;

            if (listType.getTypeArguments() == null) {
                if (!TYPE_OBJECT.equals(item.getType())) {
                    if (list.getClass() == ClassFileNewExpression.class) {
                        ClassFileNewExpression ne = (ClassFileNewExpression)list;
                        ne.setType(listType.createType(DiamondTypeArgument.DIAMOND));
                    } else {
                        list = new CastExpression(TYPE_ITERABLE.createType(item.getType()), list);
                    }
                }
            } else {
                CreateTypeFromTypeArgumentVisitor visitor2 = new CreateTypeFromTypeArgumentVisitor();
                listType.getTypeArguments().accept(visitor2);
                type = visitor2.getType();

                if (type != null) {
                    if (TYPE_OBJECT.equals(item.getType())) {
                        ((ObjectLocalVariable) item).setType(typeBounds, type);
                    } else if (item.getType().isGeneric()) {
                        ((GenericLocalVariable) item).setType((GenericType) type);
                    } else {
                        item.typeOnRight(typeBounds, type);
                    }
                }
            }
        }

        return new ClassFileForEachStatement(item, list, subStatements);
    }

    @SuppressWarnings("unchecked")
    protected static Statement makeLabels(int loopIndex, int continueOffset, int breakOffset, Statement loop, Statements jumps) {
        if (!jumps.isEmpty()) {
            Iterator<Statement> iterator = jumps.iterator();
            String label = "label" + loopIndex;
            boolean createLabel = false;

            while (iterator.hasNext()) {
                ClassFileBreakContinueStatement statement = (ClassFileBreakContinueStatement)iterator.next();
                int offset = statement.getOffset();
                int targetOffset = statement.getTargetOffset();

                if (targetOffset == continueOffset) {
                    statement.setStatement(new ContinueStatement(label));
                    createLabel = true;
                    iterator.remove();
                } else if (targetOffset == breakOffset) {
                    statement.setStatement(new BreakStatement(label));
                    createLabel = true;
                    iterator.remove();
                } else if ((continueOffset <= offset) && (offset < breakOffset)) {
                    if ((continueOffset <= targetOffset) && (targetOffset < breakOffset)) {
                        if (statement.isContinueLabel()) {
                            statement.setStatement(new ContinueStatement(label));
                            createLabel = true;
                        } else {
                            statement.setStatement(CONTINUE);
                        }
                        iterator.remove();
                    } else {
                        statement.setContinueLabel(true);
                    }
                }
            }

            if (createLabel) {
                return new LabelStatement(label, loop);
            }
        }

        return loop;
    }

    protected static ClassFileForStatement newClassFileForStatement(
            LocalVariableMaker localVariableMaker, int fromOffset, int toOffset, BaseExpression init,
            Expression condition, BaseExpression update, BaseStatement statements) {
        ChangeFrameOfLocalVariablesVisitor visitor = new ChangeFrameOfLocalVariablesVisitor(localVariableMaker);

        if (condition != null) {
            condition.accept(visitor);
        }
        if (update != null) {
            update.accept(visitor);
        }

        return new ClassFileForStatement(fromOffset, toOffset, init, condition, update, statements);
    }
}
