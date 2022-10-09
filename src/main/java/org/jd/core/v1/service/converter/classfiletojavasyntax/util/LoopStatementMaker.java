/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.ArrayExpression;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.LabelStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardSuperTypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileForEachStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileForStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.GenericLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.ObjectLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.ChangeFrameOfLocalVariablesVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.CreateTypeFromTypeArgumentVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.RemoveLastContinueStatementVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchFirstLineNumberVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchFromOffsetVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchLocalVariableReferenceVisitor;

import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import static org.apache.bcel.Const.MAJOR_1_5;
import static org.jd.core.v1.model.javasyntax.statement.ContinueStatement.CONTINUE;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_ITERABLE;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_OBJECT;

public final class LoopStatementMaker {

    private LoopStatementMaker() {
        super();
    }

    private static final RemoveLastContinueStatementVisitor REMOVE_LAST_CONTINUE_STATEMENT_VISITOR = new RemoveLastContinueStatementVisitor();

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

    private static Statement makeLoop(
            int majorVersion, Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker,
            BasicBlock loopBasicBlock, Statements statements, Expression condition, Statements subStatements) {
        boolean forEachSupported = majorVersion >= MAJOR_1_5;

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

        int lineNumber = condition == null ? Expression.UNKNOWN_LINE_NUMBER : condition.getLineNumber();
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

                    if (subStatement.isExpressionStatement()) {
                        Expression subExpression = subStatement.getExpression();

                        if (subExpression.getLineNumber() == lineNumber) {
                            return newClassFileForStatement(
                                localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, subExpression, null);
                        }
                        if (init != null) {
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
                if (lineNumber <= 0) {
                    // Unknown line numbers => Just try to find 'for (expression;;expression)'
                    return createForStatementWithoutLineNumber(localVariableMaker, loopBasicBlock, statements, condition, subStatements);
                }
            // Known line numbers
            SearchFirstLineNumberVisitor visitor = new SearchFirstLineNumberVisitor();

            subStatements.getFirst().accept(visitor);

            int firstLineNumber = visitor.getLineNumber();

            // Populates 'update'
            Expressions update = extractUpdate(subStatements, firstLineNumber);
            BaseExpression init = extractInit(statements, lineNumber);

            if (init != null || !update.isEmpty()) {
                return newClassFileForStatement(
                    localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, update, subStatements);
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

    private static Statement makeLoop(LocalVariableMaker localVariableMaker, BasicBlock loopBasicBlock, Statements statements, Statements subStatements) {
        int subStatementsSize = subStatements.size();

        if (subStatementsSize > 0 && subStatements.getLast() == CONTINUE) {
            subStatements.removeLast();
            subStatementsSize--;
        }

        switch (subStatementsSize) {
            case 0:
                break;
            case 1:
                Statement subStatement = subStatements.getFirst();

                if (subStatement.isExpressionStatement()) {
                    Expression subExpression = subStatement.getExpression();
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

                if (firstLineNumber <= 0) {
                    // Unknown line numbers => Just try to find 'for (expression;;expression)'
                    return createForStatementWithoutLineNumber(localVariableMaker, loopBasicBlock, statements, BooleanExpression.TRUE, subStatements);
                }
            // Populates 'update'
            Expressions update = extractUpdate(subStatements, firstLineNumber);

            if (!update.isEmpty()) {
                // Populates 'init'
                BaseExpression init = extractInit(statements, update.getFirst().getLineNumber());

                return newClassFileForStatement(localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, null, update, subStatements);
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

    private static BaseExpression extractInit(Statements statements, int lineNumber) {
        if (lineNumber > 0) {
            switch (statements.size()) {
                case 0:
                    break;
                case 1:
                    Statement statement = statements.getFirst();

                    if (!statement.isExpressionStatement()) {
                        break;
                    }

                    Expression expression = statement.getExpression();

                    if (expression.getLineNumber() != lineNumber ||
                        !expression.isBinaryOperatorExpression() ||
                        expression.getRightExpression().getLineNumber() == 0) {
                        break;
                    }

                    statements.clear();
                    return expression;
                default:
                    Expressions init = new Expressions();
                    ListIterator<Statement> iterator = statements.listIterator(statements.size());

                    while (iterator.hasPrevious()) {
                        statement = iterator.previous();

                        if (!statement.isExpressionStatement()) {
                            break;
                        }

                        expression = statement.getExpression();

                        if (expression.getLineNumber() != lineNumber ||
                            !expression.isBinaryOperatorExpression() ||
                            expression.getRightExpression().getLineNumber() == 0) {
                            break;
                        }

                        init.add(expression);
                        iterator.remove();
                    }

                    if (!init.isEmpty()) {
                        if (init.size() > 1) {
                            Collections.reverse(init);
                        }
                        return init;
                    }
                    break;
            }
        }

        return null;
    }

    private static Expressions extractUpdate(Statements statements, int firstLineNumber) {
        Expressions update = new Expressions();
        ListIterator<Statement> iterator = statements.listIterator(statements.size());

        // Populates 'update'
        while (iterator.hasPrevious()) {
            Statement statement = iterator.previous();
            if (!statement.isExpressionStatement()) {
                break;
            }
            Expression expression = statement.getExpression();
            if (expression.getLineNumber() >= firstLineNumber) {
                break;
            }
            iterator.remove();
            update.add(expression);
        }

        if (update.size() > 1) {
            Collections.reverse(update);
        }

        return update;
    }

    private static Statement createForStatementWithoutLineNumber(
            LocalVariableMaker localVariableMaker, BasicBlock basicBlock, Statements statements, Expression condition, Statements subStatements) {

        if (!statements.isEmpty()) {
            Expression init = statements.getLast().getExpression();

            if (init.getLeftExpression().isLocalVariableReferenceExpression()) {
                AbstractLocalVariable localVariable = ((ClassFileLocalVariableReferenceExpression) init.getLeftExpression()).getLocalVariable();
                Expression update = subStatements.getLast().getExpression();
                Expression expression;

                if (update.isBinaryOperatorExpression()) {
                    expression = update.getLeftExpression();
                } else if (update.isPreOperatorExpression()) {
                    expression = update.getExpression();
                    update = new PostOperatorExpression(update.getLineNumber(), expression, update.getOperator());
                } else if (update.isPostOperatorExpression()) {
                    expression = update.getExpression();
                } else {
                    return new WhileStatement(condition, subStatements);
                }

                if (expression.isLocalVariableReferenceExpression() &&
                        ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable() == localVariable) {
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

        return new WhileStatement(condition, subStatements);
    }

    private static Statement makeForEachArray(
            Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker, Statements statements,
            Expression condition, Statements subStatements) {
        if (condition == null) {
            return null;
        }

        int statementsSize = statements.size();

        if (statementsSize < 3 || subStatements.size() < 2) {
            return null;
        }

        // len$ = arr$.length;
        Statement statement = statements.get(statementsSize-2);

        if (!statement.isExpressionStatement()) {
            return null;
        }

        int lineNumber = condition.getLineNumber();
        Expression expression = statement.getExpression();

        if (expression.getLineNumber() != lineNumber || !expression.isBinaryOperatorExpression()) {
            return null;
        }

        if (!expression.getRightExpression().isLengthExpression() || !expression.getLeftExpression().isLocalVariableReferenceExpression()) {
            return null;
        }

        Expression boe = expression;

        expression = boe.getRightExpression().getExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable syntheticArray = ((ClassFileLocalVariableReferenceExpression)expression).getLocalVariable();
        AbstractLocalVariable syntheticLength = ((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable();

        if (syntheticArray.getName() != null && syntheticArray.getName().indexOf('$') == -1) {
            return null;
        }

        // String s = arr$[i$];
        expression = subStatements.getFirst().getExpression();

        if (!expression.getRightExpression().isArrayExpression() ||
                !expression.getLeftExpression().isLocalVariableReferenceExpression() ||
                expression.getLineNumber() != condition.getLineNumber()) {
            return null;
        }

        ArrayExpression arrayExpression = (ArrayExpression)expression.getRightExpression();

        if (!arrayExpression.getExpression().isLocalVariableReferenceExpression() || !arrayExpression.getIndex().isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)arrayExpression.getExpression()).getLocalVariable() != syntheticArray) {
            return null;
        }

        AbstractLocalVariable syntheticIndex = ((ClassFileLocalVariableReferenceExpression)arrayExpression.getIndex()).getLocalVariable();
        AbstractLocalVariable item = ((ClassFileLocalVariableReferenceExpression)expression.getLeftExpression()).getLocalVariable();

        if (syntheticIndex.getName() != null && syntheticIndex.getName().indexOf('$') == -1) {
            return null;
        }

        // arr$ = array;
        expression = statements.get(statementsSize-3).getExpression();

        if (!expression.getLeftExpression().isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)expression.getLeftExpression()).getLocalVariable() != syntheticArray) {
            return null;
        }

        Type arrayType = expression.getRightExpression().getType();
        Expression array = expression.getRightExpression();

        // i$ = 0;
        expression = statements.get(statementsSize-1).getExpression();

        if (expression.getLineNumber() != lineNumber ||
                !expression.getLeftExpression().isLocalVariableReferenceExpression() ||
                !expression.getRightExpression().isIntegerConstantExpression()) {
            return null;
        }
        if (expression.getRightExpression().getIntegerValue() != 0 ||
                ((ClassFileLocalVariableReferenceExpression)expression.getLeftExpression()).getLocalVariable() != syntheticIndex) {
            return null;
        }

        // i$ < len$;
        if (!condition.getLeftExpression().isLocalVariableReferenceExpression() || !condition.getRightExpression().isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)condition.getLeftExpression()).getLocalVariable() != syntheticIndex ||
                ((ClassFileLocalVariableReferenceExpression)condition.getRightExpression()).getLocalVariable() != syntheticLength) {
            return null;
        }

        // ++i$;
        expression = subStatements.getLast().getExpression();

        if (expression.getLineNumber() != lineNumber || !expression.isPostOperatorExpression()) {
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
        } else if (item.getType().isGenericType() && type.isGenericType()) {
            ((GenericLocalVariable)item).setType((GenericType)type);
        } else {
            item.typeOnRight(typeBounds, type);
        }

        localVariableMaker.removeLocalVariable(syntheticArray);
        localVariableMaker.removeLocalVariable(syntheticIndex);
        localVariableMaker.removeLocalVariable(syntheticLength);

        if (array instanceof CastExpression) {
            CastExpression castExpression = (CastExpression) array;
            Type leftArrayType = item.getType().createType(item.getType().getDimension() + 1);
            Type rightArrayType = castExpression.getExpression().getType();
            if (leftArrayType.equals(rightArrayType)) {
                return new ClassFileForEachStatement(item, castExpression.getExpression(), subStatements);
            }
        }
        
        return new ClassFileForEachStatement(item, array, subStatements);
    }

    private static Statement makeForEachList(
            Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker, Statements statements,
            Expression condition, Statements subStatements) {
        if (condition == null) {
            return null;
        }

        if (statements.isEmpty() || subStatements.isEmpty()) {
            return null;
        }

        // i$.hasNext();
        if (!condition.isMethodInvocationExpression()) {
            return null;
        }

        MethodInvocationExpression mie = (MethodInvocationExpression)condition;

        if (!"hasNext".equals(mie.getName()) || !"java/util/Iterator".equals(mie.getInternalTypeName()) ||
                !mie.getExpression().isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable syntheticIterator = ((ClassFileLocalVariableReferenceExpression)mie.getExpression()).getLocalVariable();

        // Iterator i$ = list.iterator();
        Expression boe = statements.getLast().getExpression();

        if (boe == null ||
                !boe.getLeftExpression().isLocalVariableReferenceExpression() ||
                !boe.getRightExpression().isMethodInvocationExpression() ||
                boe.getLineNumber() != condition.getLineNumber()) {
            return null;
        }

        mie = (MethodInvocationExpression)boe.getRightExpression();

        if (!"iterator".equals(mie.getName()) || !"()Ljava/util/Iterator;".equals(mie.getDescriptor())) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable() != syntheticIterator) {
            return null;
        }

        Expression list = mie.getExpression();

        if (list.isCastExpression()) {
            list = list.getExpression();
        }

        // String s = (String)i$.next();
        boe = subStatements.getFirst().getExpression();

        if (!boe.getLeftExpression().isLocalVariableReferenceExpression()) {
            return null;
        }

        Expression expression = boe.getRightExpression();

        if (boe.getRightExpression().isCastExpression()) {
            expression = expression.getExpression();
        }
        if (!expression.isMethodInvocationExpression()) {
            return null;
        }

        mie = (MethodInvocationExpression)expression;

        if (!"next".equals(mie.getName()) ||
                !"java/util/Iterator".equals(mie.getInternalTypeName()) ||
                !mie.getExpression().isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)mie.getExpression()).getLocalVariable() != syntheticIterator) {
            return null;
        }

        // Check if 'i$' is not used in sub-statements
        SearchLocalVariableReferenceVisitor visitor1 = new SearchLocalVariableReferenceVisitor();

        visitor1.init(syntheticIterator);

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

        if (type.isObjectType()) {
            ObjectType listType = (ObjectType)type;

            if (listType.getTypeArguments() == null || listType.getTypeArguments().isWildcardTypeArgument() || listType.getTypeArguments().isGenericTypeArgument()) {
                if (list.isNewExpression()) {
                    ClassFileNewExpression ne = (ClassFileNewExpression) list;
                    ne.setType(listType.createType(item.getType()));
                } else {
                    list = new CastExpression(TYPE_ITERABLE.createType(item.getType()), list);
                }
            } else {
                CreateTypeFromTypeArgumentVisitor visitor2 = new CreateTypeFromTypeArgumentVisitor();
                listType.getTypeArguments().accept(visitor2);
                type = visitor2.getType();

                if (type != null) {
                    if (TYPE_OBJECT.equals(item.getType())) {
                        if (item instanceof ObjectLocalVariable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                            ObjectLocalVariable olv = (ObjectLocalVariable) item;
                            olv.setType(typeBounds, type);
                        }
                    } else if (item.getType().isGenericType()) {
                        if (item instanceof GenericLocalVariable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                            GenericLocalVariable glv = (GenericLocalVariable) item;
                            glv.setType((GenericType) type);
                        }
                    } else {
                        item.typeOnRight(typeBounds, type);
                    }
                }
            }
        }

        if (list instanceof MethodInvocationExpression && item.getType() instanceof ObjectType) {
            MethodInvocationExpression exp = (MethodInvocationExpression) list;
            ObjectType ot = (ObjectType) item.getType();
            if (ot.getTypeArguments() instanceof WildcardExtendsTypeArgument || ot.getTypeArguments() instanceof WildcardSuperTypeArgument) {
                exp.setNonWildcardTypeArguments(null);
            }
        }
        
        return new ClassFileForEachStatement(item, list, subStatements);
    }

    private static Statement makeLabels(int loopIndex, int continueOffset, int breakOffset, Statement loop, Statements jumps) {
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
                } else if (continueOffset <= offset && offset < breakOffset) {
                    if (continueOffset <= targetOffset && targetOffset < breakOffset) {
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

    private static ClassFileForStatement newClassFileForStatement(
            LocalVariableMaker localVariableMaker, int fromOffset, int toOffset, BaseExpression init,
            Expression condition, BaseExpression update, BaseStatement statements) {
        if (init != null) {
            SearchFromOffsetVisitor visitor = new SearchFromOffsetVisitor();

            init.accept(visitor);

            int offset = visitor.getOffset();

            if (fromOffset > offset) {
                fromOffset = offset;
            }
        }

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
