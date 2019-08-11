/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Constants;
import org.jd.core.v1.model.classfile.attribute.AttributeCode;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.statement.*;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileMonitorEnterStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileMonitorExitStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.*;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.DefaultStack;

import java.util.*;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.*;

public class StatementMaker {
    protected static final SwitchCaseComparator SWITCH_CASE_COMPARATOR = new SwitchCaseComparator();
    protected static final NullExpression FINALLY_EXCEPTION_EXPRESSION = new NullExpression(new ObjectType("java/lang/Exception", "java.lang.Exception", "Exception"));
    protected static final MergeTryWithResourcesStatementVisitor MERGE_TRY_WITH_RESOURCES_STATEMENT_VISITOR = new MergeTryWithResourcesStatementVisitor();

    protected ObjectTypeMaker objectTypeMaker;
    protected SignatureParser signatureParser;
    protected LocalVariableMaker localVariableMaker;
    protected ByteCodeParser byteCodeParser;
    protected int majorVersion;
    protected String internalTypeName;
    protected ClassFileBodyDeclaration bodyDeclaration;
    protected DefaultStack<Expression> stack = new DefaultStack<>();
    protected RemoveFinallyStatementsVisitor removeFinallyStatementsVisitor;
    protected RemoveBinaryOpReturnStatementsVisitor removeBinaryOpReturnStatementsVisitor;
    protected final UpdateIntegerConstantTypeVisitor updateIntegerConstantTypeVisitor;
    protected SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
    protected MemberVisitor memberVisitor = new MemberVisitor();
    protected boolean removeFinallyStatementsFlag = false;
    protected boolean mergeTryWithResourcesStatementFlag = false;

    public StatementMaker(
            ObjectTypeMaker objectTypeMaker, SignatureParser signatureParser, LocalVariableMaker localVariableMaker,
            ClassFile classFile, ClassFileBodyDeclaration bodyDeclaration, Type returnedType) {
        this.objectTypeMaker = objectTypeMaker;
        this.signatureParser = signatureParser;
        this.localVariableMaker = localVariableMaker;
        this.majorVersion = classFile.getMajorVersion();
        this.internalTypeName = classFile.getInternalTypeName();
        this.bodyDeclaration = bodyDeclaration;
        this.byteCodeParser = new ByteCodeParser(objectTypeMaker, signatureParser, localVariableMaker, internalTypeName, classFile, bodyDeclaration, returnedType);
        this.removeFinallyStatementsVisitor = new RemoveFinallyStatementsVisitor(localVariableMaker);
        this.removeBinaryOpReturnStatementsVisitor = new RemoveBinaryOpReturnStatementsVisitor(localVariableMaker);
        this.updateIntegerConstantTypeVisitor = new UpdateIntegerConstantTypeVisitor(signatureParser, returnedType);
    }

    public Statements make(ControlFlowGraph cfg) {
        Statements statements = new Statements();
        Statements jumps = new Statements();
        WatchDog watchdog = new WatchDog();

        localVariableMaker.pushFrame(statements);

        // Generate statements
        makeStatements(watchdog, cfg.getStart(), statements, jumps);

        // Remove 'finally' statements
        if (removeFinallyStatementsFlag) {
            removeFinallyStatementsVisitor.init();
            statements.accept(removeFinallyStatementsVisitor);
        }

        // Merge 'try-with-resources' statements
        if (mergeTryWithResourcesStatementFlag) {
            statements.accept(MERGE_TRY_WITH_RESOURCES_STATEMENT_VISITOR);
        }

        // Replace pattern "local_var_2 = ...; return local_var_2;" with "return ...;"
        statements.accept(removeBinaryOpReturnStatementsVisitor);

        // Remove last 'return' statement
        if (!statements.isEmpty() && (statements.getLast().getClass() == ReturnStatement.class)) {
            statements.removeLast();
        }

        localVariableMaker.popFrame();

        // Update integer constant type to 'byte', 'char', 'short' or 'int'
        statements.accept(updateIntegerConstantTypeVisitor);

        // Change ++i; with i++;
        replacePreOperatorWithPostOperator(statements);

        if (!jumps.isEmpty()) {
            updateJumpStatements(jumps);
        }

        return statements;
    }

    /**
     * A recursive, next neighbour first, statements builder from basic blocks.
     *
     * @param basicBlock Current basic block
     * @param statements List to populate
     */
    @SuppressWarnings("unchecked")
    protected void makeStatements(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps) {
        Statements subStatements, elseStatements;
        Expression condition, exp1, exp2;

        switch (basicBlock.getType()) {
            case TYPE_START:
                watchdog.check(basicBlock, basicBlock.getNext());
                makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
                break;
            case TYPE_END:
                break;
            case TYPE_STATEMENTS:
                watchdog.check(basicBlock, basicBlock.getNext());
            case TYPE_THROW:
                parseByteCode(basicBlock, statements);
                makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
                break;
            case TYPE_RETURN:
                statements.add(ReturnStatement.RETURN);
                break;
            case TYPE_RETURN_VALUE:
            case TYPE_GOTO_IN_TERNARY_OPERATOR:
                parseByteCode(basicBlock, statements);
                break;
            case TYPE_SWITCH:
                parseSwitch(watchdog, basicBlock, statements, jumps);
                break;
            case TYPE_SWITCH_BREAK:
                statements.add(BreakStatement.BREAK);
                break;
            case TYPE_TRY:
                parseTry(watchdog, basicBlock, statements, jumps, false, false);
                break;
            case TYPE_TRY_JSR:
                parseTry(watchdog, basicBlock, statements, jumps, true, false);
                break;
            case TYPE_TRY_ECLIPSE:
                parseTry(watchdog, basicBlock, statements, jumps, false, true);
                break;
            case TYPE_JSR:
                parseJSR(watchdog, basicBlock, statements, jumps);
                break;
            case TYPE_RET:
                parseByteCode(basicBlock, statements);
                break;
            case TYPE_IF:
                parseIf(watchdog, basicBlock, statements, jumps);
                break;
            case TYPE_IF_ELSE:
                watchdog.check(basicBlock, basicBlock.getCondition());
                makeStatements(watchdog, basicBlock.getCondition(), statements, jumps);
                condition = stack.pop();
                DefaultStack<Expression> backup = new DefaultStack<>(stack);
                watchdog.check(basicBlock, basicBlock.getSub1());
                subStatements = makeSubStatements(watchdog, basicBlock.getSub1(), statements, jumps);
                if (!basicBlock.getSub2().matchType(TYPE_LOOP_END|TYPE_LOOP_CONTINUE|TYPE_LOOP_START) && (stack.size() != backup.size())) {
                    stack.copy(backup);
                }
                watchdog.check(basicBlock, basicBlock.getSub2());
                elseStatements = makeSubStatements(watchdog, basicBlock.getSub2(), statements, jumps);
                statements.add(new IfElseStatement(condition, subStatements, elseStatements));
                watchdog.check(basicBlock, basicBlock.getNext());
                makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
                break;
            case TYPE_CONDITION:
                parseByteCode(basicBlock, statements);
                break;
            case TYPE_CONDITION_OR:
                watchdog.check(basicBlock, basicBlock.getSub1());
                exp1 = makeExpression(watchdog, basicBlock.getSub1(), statements, jumps);
                watchdog.check(basicBlock, basicBlock.getSub2());
                exp2 = makeExpression(watchdog, basicBlock.getSub2(), statements, jumps);
                stack.push(new BinaryOperatorExpression(basicBlock.getFirstLineNumber(), PrimitiveType.TYPE_BOOLEAN, exp1, "||", exp2, 14));
                break;
            case TYPE_CONDITION_AND:
                watchdog.check(basicBlock, basicBlock.getSub1());
                exp1 = makeExpression(watchdog, basicBlock.getSub1(), statements, jumps);
                watchdog.check(basicBlock, basicBlock.getSub2());
                exp2 = makeExpression(watchdog, basicBlock.getSub2(), statements, jumps);
                stack.push(new BinaryOperatorExpression(basicBlock.getFirstLineNumber(), PrimitiveType.TYPE_BOOLEAN, exp1, "&&", exp2, 13));
                break;
            case TYPE_CONDITION_TERNARY_OPERATOR:
                watchdog.check(basicBlock, basicBlock.getCondition());
                makeStatements(watchdog, basicBlock.getCondition(), statements, jumps);
                condition = stack.pop();
                backup = new DefaultStack<>(stack);
                watchdog.check(basicBlock, basicBlock.getSub1());
                exp1 = makeExpression(watchdog, basicBlock.getSub1(), statements, jumps);
                if (stack.size() != backup.size()) {
                    stack.copy(backup);
                }
                watchdog.check(basicBlock, basicBlock.getSub2());
                exp2 = makeExpression(watchdog, basicBlock.getSub2(), statements, jumps);
                stack.push(parseTernaryOperator(basicBlock.getFirstLineNumber(), condition, exp1, exp2));
                parseByteCode(basicBlock, statements);
                break;
            case TYPE_TERNARY_OPERATOR:
                watchdog.check(basicBlock, basicBlock.getCondition());
                makeStatements(watchdog, basicBlock.getCondition(), statements, jumps);
                condition = stack.pop();
                backup = new DefaultStack<>(stack);
                watchdog.check(basicBlock, basicBlock.getSub1());
                exp1 = makeExpression(watchdog, basicBlock.getSub1(), statements, jumps);
                if (stack.size() != backup.size()) {
                    stack.copy(backup);
                }
                watchdog.check(basicBlock, basicBlock.getSub2());
                exp2 = makeExpression(watchdog, basicBlock.getSub2(), statements, jumps);
                stack.push(parseTernaryOperator(basicBlock.getFirstLineNumber(), condition, exp1, exp2));
                watchdog.check(basicBlock, basicBlock.getNext());
                makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
                break;
            case TYPE_LOOP:
                parseLoop(watchdog, basicBlock, statements, jumps);
                break;
            case TYPE_LOOP_START:
            case TYPE_LOOP_CONTINUE:
                statements.add(ContinueStatement.CONTINUE);
                break;
            case TYPE_LOOP_END:
                statements.add(BreakStatement.BREAK);
                break;
            case TYPE_JUMP:
                Statement jump = new ClassFileBreakContinueStatement(basicBlock.getFromOffset(), basicBlock.getToOffset());
                statements.add(jump);
                jumps.add(jump);
                break;
            case TYPE_INFINITE_GOTO:
                statements.add(new WhileStatement(BooleanExpression.TRUE, null));
                break;
            default:
                assert false : "Unexpected basic block: " + basicBlock.getTypeName() + ':' + basicBlock.getIndex();
                break;
        }
    }

    protected Statements<Statement> makeSubStatements(WatchDog watchdog, BasicBlock basicBlock, Statements<Statement> statements, Statements jumps, Statements<Statement> updateStatements) {
        Statements<Statement> subStatements = makeSubStatements(watchdog, basicBlock, statements, jumps);

        if (updateStatements != null) {
            subStatements.addAll(updateStatements);
        }

        return subStatements;
    }

    protected Statements<Statement> makeSubStatements(WatchDog watchdog, BasicBlock basicBlock, Statements<Statement> statements, Statements jumps) {
        Statements<Statement> subStatements = new Statements<>();

        if (!statements.isEmpty() && (statements.getLast().getClass() == ClassFileMonitorEnterStatement.class)) {
            subStatements.add(statements.removeLast());
        }

        localVariableMaker.pushFrame(subStatements);
        makeStatements(watchdog, basicBlock, subStatements, jumps);
        localVariableMaker.popFrame();
        replacePreOperatorWithPostOperator(subStatements);

        if (!subStatements.isEmpty() && (subStatements.getFirst().getClass() == ClassFileMonitorEnterStatement.class)) {
            statements.add(subStatements.remove(0));
        }

        return subStatements;
    }

    protected Expression makeExpression(WatchDog watchdog, BasicBlock basicBlock, Statements<Statement> statements, Statements jumps) {
        int initialStatementCount = statements.size();

        makeStatements(watchdog, basicBlock, statements, jumps);

        if (stack.isEmpty()) {
            // Interesting... Kotlin pattern.
            // https://github.com/JetBrains/intellij-community/blob/master/platform/built-in-server/src/org/jetbrains/builtInWebServer/SingleConnectionNetService.kt
            // final override fun connectToProcess(...)
            return new StringConstantExpression("JD-Core does not support Kotlin");
        } else {
            Expression expression = stack.pop();

            if (statements.size() > initialStatementCount) {
                // Is a multi-assignment ?
                Statement lastStatement = statements.getLast();

                if (lastStatement.getClass() == ExpressionStatement.class) {
                    Expression expr = ((ExpressionStatement) lastStatement).getExpression();

                    if (expr.getClass() == BinaryOperatorExpression.class) {
                        BinaryOperatorExpression boe = (BinaryOperatorExpression) expr;

                        if (boe.getRightExpression() == expression) {
                            // Pattern matched -> Multi-assignment
                            statements.removeLast();
                            expression = boe;
                        }
                    }
                }
            }

            return expression;
        }
    }

    protected void parseSwitch(WatchDog watchdog, BasicBlock basicBlock, Statements<Statement> statements, Statements jumps) {
        parseByteCode(basicBlock, statements);

        List<SwitchCase> switchCases = basicBlock.getSwitchCases();
        SwitchStatement switchStatement = (SwitchStatement)statements.getLast();
        Expression condition = switchStatement.getCondition();
        Type conditionType = condition.getType();
        List<SwitchStatement.Block> blocks = switchStatement.getBlocks();
        DefaultStack<Expression> localStack = new DefaultStack<Expression>(stack);

        switchCases.sort(SWITCH_CASE_COMPARATOR);

        for (int i=0, len=switchCases.size(); i<len; i++) {
            SwitchCase sc = switchCases.get(i);
            BasicBlock bb = sc.getBasicBlock();
            int j = i + 1;

            while ((j < len) && (bb == switchCases.get(j).getBasicBlock())) {
                j++;
            }

            Statements subStatements = new Statements();

            stack.copy(localStack);
            makeStatements(watchdog, bb, subStatements, jumps);
            replacePreOperatorWithPostOperator(subStatements);

            if (sc.isDefaultCase()) {
                blocks.add(new SwitchStatement.LabelBlock(SwitchStatement.DEFAULT_LABEL, subStatements));
            } else if (j == i + 1) {
                SwitchStatement.Label label = new SwitchStatement.ExpressionLabel(new IntegerConstantExpression(conditionType, sc.getValue()));
                blocks.add(new SwitchStatement.LabelBlock(label, subStatements));
            } else {
                DefaultList<SwitchStatement.Label> labels = new DefaultList<>(j - i);

                for (; i<j; i++) {
                    labels.add(new SwitchStatement.ExpressionLabel(new IntegerConstantExpression(conditionType, switchCases.get(i).getValue())));
                }

                blocks.add(new SwitchStatement.MultiLabelsBlock(labels, subStatements));
                i--;
            }
        }

        int size = statements.size();
        Class conditionClass = condition.getClass();

        if ((size > 3) && (conditionClass == ClassFileLocalVariableReferenceExpression.class) && (statements.get(size-2).getClass() == SwitchStatement.class)) {
            // Check pattern & make 'switch-string'
            SwitchStatementMaker.makeSwitchString(localVariableMaker, statements, switchStatement);
        } else if ((bodyDeclaration.getInnerTypeDeclarations() != null) && (conditionClass == ArrayExpression.class)) {
            // Check pattern & make 'switch-enum'
            SwitchStatementMaker.makeSwitchEnum(bodyDeclaration, switchStatement);
        }

        makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
    }

    @SuppressWarnings("unchecked")
    protected void parseTry(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps, boolean jsr, boolean eclipse) {
        Statements tryStatements;
        DefaultList<TryStatement.CatchClause> catchClauses = new DefaultList<>();
        Statements<Statement> finallyStatements = null;
        int assertStackSize = stack.size();

        tryStatements = makeSubStatements(watchdog, basicBlock.getSub1(), statements, jumps);

        for (ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
            assert stack.size() == assertStackSize : "parseTry : problem with stack";

            if (exceptionHandler.getInternalThrowableName() == null) {
                stack.push(FINALLY_EXCEPTION_EXPRESSION);

                finallyStatements = makeSubStatements(watchdog, exceptionHandler.getBasicBlock(), statements, jumps);

                if (finallyStatements.get(0).getClass() != ClassFileMonitorExitStatement.class) {
                    removeFinallyStatementsFlag |= (jsr == false);

                    Statement statement = finallyStatements.getFirst();

                    if (statement.getClass() == ExpressionStatement.class) {
                        Expression expression = ((ExpressionStatement) statement).getExpression();

                        if (expression.getClass() == BinaryOperatorExpression.class) {
                            BinaryOperatorExpression boe = (BinaryOperatorExpression) expression;

                            if (boe.getLeftExpression().getClass() == ClassFileLocalVariableReferenceExpression.class) {
                                statement = finallyStatements.getLast();

                                if (statement.getClass() == ThrowStatement.class) {
                                    // Remove synthetic local variable
                                    expression = ((ThrowStatement)statement).getExpression();

                                    if (expression.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                                        ClassFileLocalVariableReferenceExpression vre1 = (ClassFileLocalVariableReferenceExpression) expression;
                                        ClassFileLocalVariableReferenceExpression vre2 = (ClassFileLocalVariableReferenceExpression) boe.getLeftExpression();

                                        if (vre1.getLocalVariable() == vre2.getLocalVariable()) {
                                            localVariableMaker.removeLocalVariable(vre2.getLocalVariable());
                                            // Remove first statement (storage of finally localVariable)
                                            finallyStatements.remove(0);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Remove last statement (throw finally localVariable)
                    finallyStatements.removeLast();
                }
            } else {
                stack.push(new NullExpression(objectTypeMaker.makeFromInternalTypeName(exceptionHandler.getInternalThrowableName())));

                Statements catchStatements = new Statements();
                localVariableMaker.pushFrame(catchStatements);

                BasicBlock bb = exceptionHandler.getBasicBlock();
                int lineNumber = bb.getControlFlowGraph().getLineNumber(bb.getFromOffset());
                int index = ByteCodeParser.getExceptionLocalVariableIndex(bb);
                ObjectType ot = objectTypeMaker.makeFromInternalTypeName(exceptionHandler.getInternalThrowableName());
                int offset = bb.getFromOffset();
                byte[] code = bb.getControlFlowGraph().getMethod().<AttributeCode>getAttribute("Code").getCode();

                if (code[offset] == 58) {
                    offset += 2; // ASTORE
                } else {
                    offset++;    // POP, ASTORE_1 ... ASTORE_3
                }

                AbstractLocalVariable exception = localVariableMaker.getExceptionLocalVariable(index, offset, ot);

                makeStatements(watchdog, bb, catchStatements, jumps);
                localVariableMaker.popFrame();
                removeExceptionReference(catchStatements);

                if (lineNumber != Expression.UNKNOWN_LINE_NUMBER) {
                    searchFirstLineNumberVisitor.init();
                    searchFirstLineNumberVisitor.visit(catchStatements);
                    if (searchFirstLineNumberVisitor.getLineNumber() == lineNumber) {
                        lineNumber = Expression.UNKNOWN_LINE_NUMBER;
                    }
                }

                replacePreOperatorWithPostOperator(catchStatements);

                ClassFileTryStatement.CatchClause cc = new ClassFileTryStatement.CatchClause(lineNumber, ot, exception, catchStatements);

                if (exceptionHandler.getOtherInternalThrowableNames() != null) {
                    for (String name : exceptionHandler.getOtherInternalThrowableNames()) {
                        cc.addType(objectTypeMaker.makeFromInternalTypeName(name));
                    }
                }

                catchClauses.add(cc);
            }
        }

        // 'try', 'try-with-resources' or 'synchronized' ?
        Statement statement = null;

        if ((finallyStatements != null) && (finallyStatements.size() > 0) && (finallyStatements.get(0).getClass() == ClassFileMonitorExitStatement.class)) {
            statement = SynchronizedStatementMaker.make(localVariableMaker, statements, tryStatements);
        } else {
            if (majorVersion >= 51) { // (majorVersion >= Java 7)
                assert jsr == false;
                statement = TryWithResourcesStatementMaker.make(localVariableMaker, statements, tryStatements, catchClauses, finallyStatements);
            }
            if (statement == null) {
                statement = new ClassFileTryStatement(tryStatements, catchClauses, finallyStatements, jsr, eclipse);
            } else {
                mergeTryWithResourcesStatementFlag = true;
            }
        }

        statements.add(statement);
        makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
    }

    protected void removeExceptionReference(Statements catchStatements) {
        if ((catchStatements.size() > 0) && (catchStatements.get(0).getClass() == ExpressionStatement.class)) {
            ExpressionStatement es = (ExpressionStatement) catchStatements.get(0);

            if (es.getExpression().getClass() == BinaryOperatorExpression.class) {
                BinaryOperatorExpression boe = (BinaryOperatorExpression)es.getExpression();

                if ((boe.getLeftExpression().getClass() == ClassFileLocalVariableReferenceExpression.class) && (boe.getRightExpression().getClass() == NullExpression.class)) {
                    catchStatements.remove(0);
                }
            } else if (es.getExpression().getClass() == NullExpression.class) {
                catchStatements.remove(0);
            }
        }
    }

    protected void parseJSR(WatchDog watchdog, BasicBlock basicBlock, Statements<Statement> statements, Statements jumps) {
        int statementCount = statements.size();

        parseByteCode(basicBlock, statements);
        makeStatements(watchdog, basicBlock.getBranch(), statements, jumps);
        makeStatements(watchdog, basicBlock.getNext(), statements, jumps);

        // Remove synthetic local variable
        ExpressionStatement es = (ExpressionStatement)statements.get(statementCount);
        BinaryOperatorExpression boe = (BinaryOperatorExpression)es.getExpression();
        ClassFileLocalVariableReferenceExpression vre = (ClassFileLocalVariableReferenceExpression)boe.getLeftExpression();

        localVariableMaker.removeLocalVariable(vre.getLocalVariable());
        // Remove first statement (storage of JSR return offset)
        statements.remove(statementCount);
    }

    @SuppressWarnings("unchecked")
    protected void parseIf(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps) {
        BasicBlock condition = basicBlock.getCondition();

        if (condition.getType() == BasicBlock.TYPE_CONDITION_AND) {
            condition = condition.getSub1();
        }

        if (ByteCodeParser.isAssertCondition(internalTypeName, condition)) {
            Expression cond;

            if (condition == basicBlock.getCondition()) {
                cond = new BooleanExpression(condition.getFirstLineNumber(), false);
            } else {
                condition = basicBlock.getCondition().getSub2();
                condition.inverseCondition();
                makeStatements(watchdog, condition, statements, jumps);
                cond = stack.pop();
            }

            Statements subStatements = makeSubStatements(watchdog, basicBlock.getSub1(), statements, jumps);
            Expression message = null;

            if (subStatements.get(0).getClass() == ThrowStatement.class) {
                Expression e = ((ThrowStatement)subStatements.get(0)).getExpression();
                if (e.getClass() == NewExpression.class) {
                    BaseExpression parameters = ((NewExpression)e).getParameters();
                    if ((parameters != null) && !parameters.isList()) {
                        message = parameters.getFirst();
                    }
                }
            }

            statements.add(new AssertStatement(cond, message));
            makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
        } else {
            makeStatements(watchdog, basicBlock.getCondition(), statements, jumps);
            Expression cond = stack.pop();
            DefaultStack<Expression> backup = new DefaultStack<Expression>(stack);
            Statements subStatements = makeSubStatements(watchdog, basicBlock.getSub1(), statements, jumps);
            if (stack.size() != backup.size()) {
                stack.copy(backup);
            }
            statements.add(new IfStatement(cond, subStatements));
            int index = statements.size();
            makeStatements(watchdog, basicBlock.getNext(), statements, jumps);

            if ((subStatements.size() == 1) &&
                    (index+1 == statements.size()) &&
                    (subStatements.get(0).getClass() == ReturnExpressionStatement.class) &&
                    (statements.get(index).getClass() == ReturnExpressionStatement.class)) {
                ReturnExpressionStatement cfres1 = (ReturnExpressionStatement)subStatements.get(0);

                if (cond.getLineNumber() >= cfres1.getLineNumber()) {
                    ReturnExpressionStatement cfres2 = (ReturnExpressionStatement)statements.get(index);

                    if (cfres1.getLineNumber() == cfres2.getLineNumber()) {
                        statements.subList(index-1, statements.size()).clear();
                        statements.add(new ReturnExpressionStatement(new TernaryOperatorExpression(cfres1.getLineNumber(), cond, cfres1.getExpression(), cfres2.getExpression())));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void parseLoop(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps) {
        BasicBlock sub1 = basicBlock.getSub1();
        Statements<Statement> updateStatements = null;

        if ((sub1.getType() == TYPE_IF) && (sub1.getCondition() == END)) {
            updateStatements = makeSubStatements(watchdog, sub1.getNext(), statements, jumps);
            sub1 = sub1.getSub1();
        }

        if (sub1.getType() == TYPE_IF) {
            BasicBlock ifBB = sub1;

            if (ifBB.getNext() == LOOP_END) {
                // 'while' or 'for' loop
                makeStatements(watchdog, ifBB.getCondition(), statements, jumps);
                statements.add(LoopStatementMaker.makeLoop(localVariableMaker, basicBlock, statements, stack.pop(), makeSubStatements(watchdog, ifBB.getSub1(), statements, jumps, updateStatements), jumps));
                makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
                return;
            }

            if (ifBB.getSub1() == LOOP_END) {
                if (ifBB.getNext() == LOOP_START) {
                    // 'do-while' pattern
                    ifBB.getCondition().inverseCondition();

                    Statements subStatements = new Statements();

                    makeStatements(watchdog, ifBB.getCondition(), subStatements, jumps);
                    replacePreOperatorWithPostOperator(subStatements);
                    statements.add(LoopStatementMaker.makeDoWhileLoop(basicBlock, ifBB, stack.pop(), subStatements, jumps));
                } else {
                    // 'while' or 'for' loop
                    ifBB.getCondition().inverseCondition();
                    makeStatements(watchdog, ifBB.getCondition(), statements, jumps);
                    statements.add(LoopStatementMaker.makeLoop(localVariableMaker, basicBlock, statements, stack.pop(), makeSubStatements(watchdog, ifBB.getNext(), statements, jumps, updateStatements), jumps));
                }

                makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
                return;
            }
        }

        BasicBlock next = sub1.getNext();
        BasicBlock last = sub1;

        while (next.matchType(GROUP_SINGLE_SUCCESSOR) && (next.getPredecessors().size() == 1)) {
            last = next;
            next = next.getNext();
        }

        if ((next == LOOP_START) && (last.getType() == TYPE_IF) && (last.getSub1() == LOOP_END) && (countStartLoop(sub1) == 1)) {
            // 'do-while'
            Statements subStatements;

            last.getCondition().inverseCondition();
            last.setType(TYPE_END);

            if ((sub1.getType() == TYPE_LOOP) && (sub1.getNext() == last) && (countStartLoop(sub1.getSub1()) == 0)) {
                changeEndLoopToStartLoop(new BitSet(), sub1.getSub1());
                subStatements = makeSubStatements(watchdog, sub1.getSub1(), statements, jumps, updateStatements);

                assert subStatements.getLast() == ContinueStatement.CONTINUE : "StatementMaker.parseLoop(...) : unexpected basic block for create a do-while loop";

                subStatements.removeLast();
            } else {
                createDoWhileContinue(last);
                subStatements = makeSubStatements(watchdog, sub1, statements, jumps, updateStatements);
            }

            makeStatements(watchdog, last.getCondition(), subStatements, jumps);
            statements.add(LoopStatementMaker.makeDoWhileLoop(basicBlock, last, stack.pop(), subStatements, jumps));
        } else {
            // Infinite loop
            statements.add(LoopStatementMaker.makeLoop(basicBlock, statements, makeSubStatements(watchdog, sub1, statements, jumps, updateStatements), jumps));
        }

        makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
    }

    protected int countStartLoop(BasicBlock bb) {
        int count = 0;

        while (bb.matchType(GROUP_SINGLE_SUCCESSOR)) {
            switch (bb.getType()) {
                case TYPE_SWITCH:
                    for (SwitchCase switchCase : bb.getSwitchCases()) {
                        count += countStartLoop(switchCase.getBasicBlock());
                    }
                    break;
                case TYPE_TRY:
                case TYPE_TRY_JSR:
                case TYPE_TRY_ECLIPSE:
                    count += countStartLoop(bb.getSub1());

                    for (ExceptionHandler exceptionHandler : bb.getExceptionHandlers()) {
                        count += countStartLoop(exceptionHandler.getBasicBlock());
                    }
                    break;
                case TYPE_IF_ELSE:
                case TYPE_TERNARY_OPERATOR:
                    count += countStartLoop(bb.getSub2());
                case TYPE_IF:
                    count += countStartLoop(bb.getSub1());
                    break;
            }

            bb = bb.getNext();
        }

        if (bb.getType() == TYPE_LOOP_START) {
            count++;
        }

        return count;
    }

    protected void createDoWhileContinue(BasicBlock last) {
        boolean change;

        do {
            change = false;

            for (BasicBlock predecessor : last.getPredecessors()) {
                if (predecessor.getType() == TYPE_IF) {
                    BasicBlock l = predecessor.getSub1();

                    if (l.matchType(GROUP_SINGLE_SUCCESSOR)) {
                        BasicBlock n = l.getNext();

                        while (n.matchType(GROUP_SINGLE_SUCCESSOR)) {
                            l = n;
                            n = n.getNext();
                        }

                        if (n == END) {
                            // Transform 'if' to 'if-continue'
                            predecessor.getCondition().inverseCondition();
                            predecessor.setNext(predecessor.getSub1());
                            last.getPredecessors().remove(predecessor);
                            last.getPredecessors().add(l);
                            l.setNext(last);
                            predecessor.setSub1(LOOP_START);
                            change = true;
                        }
                    }
                }
            }
        } while (change);
    }

    protected static void changeEndLoopToStartLoop(BitSet visited, BasicBlock basicBlock) {
        if (!basicBlock.matchType(GROUP_END| TYPE_LOOP_END) && (visited.get(basicBlock.getIndex()) == false)) {
            visited.set(basicBlock.getIndex());

            switch (basicBlock.getType()) {
                case TYPE_CONDITIONAL_BRANCH:
                case TYPE_JSR:
                case TYPE_CONDITION:
                    if (basicBlock.getBranch() == LOOP_END) {
                        basicBlock.setBranch(LOOP_START);
                    } else {
                        changeEndLoopToStartLoop(visited, basicBlock.getBranch());
                    }
                case TYPE_START:
                case TYPE_STATEMENTS:
                case TYPE_GOTO:
                case TYPE_GOTO_IN_TERNARY_OPERATOR:
                case TYPE_LOOP:
                    if (basicBlock.getNext() == LOOP_END) {
                        basicBlock.setNext(LOOP_START);
                    } else {
                        changeEndLoopToStartLoop(visited, basicBlock.getNext());
                    }
                    break;
                case TYPE_TRY_DECLARATION:
                case TYPE_TRY:
                case TYPE_TRY_JSR:
                case TYPE_TRY_ECLIPSE:
                    for (BasicBlock.ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                        if (exceptionHandler.getBasicBlock() == LOOP_END) {
                            exceptionHandler.setBasicBlock(LOOP_START);
                        } else {
                            changeEndLoopToStartLoop(visited, exceptionHandler.getBasicBlock());
                        }
                    }
                    break;
                case TYPE_IF_ELSE:
                case TYPE_TERNARY_OPERATOR:
                    if (basicBlock.getSub2() == LOOP_END) {
                        basicBlock.setSub2(LOOP_START);
                    } else {
                        changeEndLoopToStartLoop(visited, basicBlock.getSub2());
                    }
                case TYPE_IF:
                    if (basicBlock.getSub1() == LOOP_END) {
                        basicBlock.setSub1(LOOP_START);
                    } else {
                        changeEndLoopToStartLoop(visited, basicBlock.getSub1());
                    }
                    if (basicBlock.getNext() == LOOP_END) {
                        basicBlock.setNext(LOOP_START);
                    } else {
                        changeEndLoopToStartLoop(visited, basicBlock.getNext());
                    }
                    break;
                case TYPE_SWITCH:
                case TYPE_SWITCH_DECLARATION:
                    for (SwitchCase switchCase : basicBlock.getSwitchCases()) {
                        if (switchCase.getBasicBlock() == LOOP_END) {
                            switchCase.setBasicBlock(LOOP_START);
                        } else {
                            changeEndLoopToStartLoop(visited, switchCase.getBasicBlock());
                        }
                    }
                    break;
            }
        }
    }

    protected Expression parseTernaryOperator(int lineNumber, Expression condition, Expression exp1, Expression exp2) {
        if ((exp1.getType() == ObjectType.TYPE_CLASS) && (exp2.getType() == ObjectType.TYPE_CLASS) && (condition.getClass() == BinaryOperatorExpression.class)) {
            BinaryOperatorExpression boeCond = (BinaryOperatorExpression) condition;

            if ((boeCond.getLeftExpression().getClass() == FieldReferenceExpression.class) && (boeCond.getRightExpression().getClass() == NullExpression.class)) {
                FieldReferenceExpression freCond = (FieldReferenceExpression) boeCond.getLeftExpression();

                if (freCond.getInternalTypeName().equals(internalTypeName)) {
                    String fieldName = freCond.getName();

                    if (fieldName.startsWith("class$")) {
                        if (boeCond.getOperator().equals("==") && (exp1.getClass() == BinaryOperatorExpression.class) && checkFieldReference(fieldName, exp2)) {
                            BinaryOperatorExpression boe1 = (BinaryOperatorExpression) exp1;

                            if ((boe1.getRightExpression().getClass() == MethodInvocationExpression.class) && checkFieldReference(fieldName, boe1.getLeftExpression())) {
                                MethodInvocationExpression mie = (MethodInvocationExpression) boe1.getRightExpression();

                                if ((mie.getParameters().getClass() == StringConstantExpression.class) && mie.getName().equals("class$") && mie.getInternalTypeName().equals(internalTypeName)) {
                                    // JDK 1.4.2 '.class' found ==> Convert '(class$java$lang$String == null) ? (class$java$lang$String = TestDotClass.class$("java.lang.String") : class$java$lang$String)' to 'String.class'
                                    return createObjectTypeReferenceDotClassExpression(lineNumber, fieldName, mie);
                                }
                            }
                        } else if (boeCond.getOperator().equals("!=") && (exp2.getClass() == BinaryOperatorExpression.class) && checkFieldReference(fieldName, exp1)) {
                            BinaryOperatorExpression boe2 = (BinaryOperatorExpression) exp2;

                            if ((boe2.getRightExpression().getClass() == MethodInvocationExpression.class) && checkFieldReference(fieldName, boe2.getLeftExpression())) {
                                MethodInvocationExpression mie = (MethodInvocationExpression) boe2.getRightExpression();

                                if ((mie.getParameters().getClass() == StringConstantExpression.class) && mie.getName().equals("class$") && mie.getInternalTypeName().equals(internalTypeName)) {
                                    // JDK 1.1.8 '.class' found ==> Convert '(class$java$lang$String != null) ? class$java$lang$String : (class$java$lang$String = TestDotClass.class$("java.lang.String"))' to 'String.class'
                                    return createObjectTypeReferenceDotClassExpression(lineNumber, fieldName, mie);
                                }
                            }
                        }
                    }
                }
            }
        }

        return new TernaryOperatorExpression(lineNumber, condition, exp1, exp2);
    }

    protected boolean checkFieldReference(String fieldName, Expression expression) {
        if (expression.getClass() != FieldReferenceExpression.class)
            return false;

        FieldReferenceExpression fre = (FieldReferenceExpression)expression;

        return fre.getName().equals(fieldName) && fre.getInternalTypeName().equals(internalTypeName);
    }

    @SuppressWarnings("unchecked")
    protected Expression createObjectTypeReferenceDotClassExpression(int lineNumber, String fieldName, MethodInvocationExpression mie) {
        // Add SYNTHETIC flags to field
        memberVisitor.init(fieldName);

        for (ClassFileFieldDeclaration field : bodyDeclaration.getFieldDeclarations()) {
            field.getFieldDeclarators().accept(memberVisitor);
            if (memberVisitor.isFound()) {
                field.setFlags(field.getFlags() | Constants.ACC_SYNTHETIC);
                break;
            }
        }

        // Add SYNTHETIC flags to method named 'class$'
        memberVisitor.init("class$");

        for (ClassFileConstructorOrMethodDeclaration member : bodyDeclaration.getMethodDeclarations()) {
            member.accept(memberVisitor);
            if (memberVisitor.isFound()) {
                member.setFlags(member.getFlags() | Constants.ACC_SYNTHETIC);
                break;
            }
        }

        String typeName = ((StringConstantExpression) mie.getParameters()).getString();
        ObjectType ot = objectTypeMaker.makeFromInternalTypeName(typeName.replace('.', '/'));

        return new TypeReferenceDotClassExpression(lineNumber, ot);
    }

    protected void parseByteCode(BasicBlock basicBlock, Statements<Statement> statements) {
        byteCodeParser.parse(basicBlock, statements, stack);
    }

    @SuppressWarnings("unchecked")
    protected void replacePreOperatorWithPostOperator(Statements statements) {
        Iterator<Statement> iterator = statements.iterator();

        while (iterator.hasNext()) {
            Statement statement = iterator.next();

            if (statement.getClass() == ExpressionStatement.class) {
                ExpressionStatement cfes = (ExpressionStatement)statement;

                if (cfes.getExpression().getClass() == PreOperatorExpression.class) {
                    PreOperatorExpression poe = (PreOperatorExpression)cfes.getExpression();
                    String operator = poe.getOperator();

                    if ("++".equals(operator) || "--".equals(operator)) {
                        // Replace pre-operator statement with post-operator statement
                        cfes.setExpression(new PostOperatorExpression(poe.getLineNumber(), poe.getExpression(), operator));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void updateJumpStatements(Statements jumps) {
        assert false : "StatementMaker.updateJumpStatements(stmt) : 'jumps' list is not empty";

        Iterator<ClassFileBreakContinueStatement> iterator = jumps.iterator();

        while (iterator.hasNext()) {
            ClassFileBreakContinueStatement statement = iterator.next();

            statement.setStatement(new CommentStatement("// Byte code: goto -> " + statement.getTargetOffset()));
        }
    }

    protected static class SwitchCaseComparator implements Comparator<SwitchCase> {
        @Override
        public int compare(SwitchCase sc1, SwitchCase sc2) {
            int diff = sc1.getOffset() - sc2.getOffset();

            if (diff != 0)
                return diff;

            return sc1.getValue() - sc2.getValue();
        }
    }

    protected static class MemberVisitor extends AbstractJavaSyntaxVisitor {
        protected String name;
        protected boolean found;

        public void init(String name) {
            this.name = name;
            this.found = false;
        }

        public boolean isFound() {
            return found;
        }

        @Override
        public void visit(FieldDeclarator declaration) {
            found |= declaration.getName().equals(name);
        }

        @Override
        public void visit(MethodDeclaration declaration) {
            found |= declaration.getName().equals(name);
        }
    }

    protected static class NopBitSet extends BitSet {
        @Override public boolean get(int var1) { return false; }
        @Override public void set(int var1) {}
    }
}
