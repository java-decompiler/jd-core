/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.AttributeCode;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.TypeReferenceDotClassExpression;
import org.jd.core.v1.model.javasyntax.statement.AssertStatement;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.CommentStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.ExceptionHandler;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.SwitchCase;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.MergeTryWithResourcesStatementVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.RemoveBinaryOpReturnStatementsVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.RemoveFinallyStatementsVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchFirstLineNumberVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.UpdateIntegerConstantTypeVisitor;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.DefaultStack;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.bcel.Const.ACC_SYNTHETIC;
import static org.apache.bcel.Const.ASTORE;
import static org.apache.bcel.Const.MAJOR_1_7;
import static org.jd.core.v1.model.javasyntax.expression.Expression.UNKNOWN_LINE_NUMBER;
import static org.jd.core.v1.model.javasyntax.expression.NoExpression.NO_EXPRESSION;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_UNDEFINED_OBJECT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_BYTE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_CHAR;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_DOUBLE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_FLOAT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_INT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_LONG;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_SHORT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_BOOLEAN_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BOOLEAN;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BYTE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_CHAR;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_DOUBLE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_FLOAT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_INT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_LONG;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_SHORT;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.GROUP_END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.GROUP_SINGLE_SUCCESSOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.LOOP_END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.LOOP_START;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITIONAL_BRANCH;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION_AND;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION_OR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_GOTO;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_GOTO_IN_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_IF;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_IF_ELSE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_INFINITE_GOTO;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_JSR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_JUMP;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_LOOP;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_LOOP_CONTINUE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_LOOP_END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_LOOP_START;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RET;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN_VALUE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_START;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_STATEMENTS;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_SWITCH;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_SWITCH_BREAK;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_SWITCH_DECLARATION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_THROW;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY_DECLARATION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY_ECLIPSE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY_JSR;

public class StatementMaker {
    protected static final SwitchCaseComparator SWITCH_CASE_COMPARATOR = new SwitchCaseComparator();
    protected static final NullExpression FINALLY_EXCEPTION_EXPRESSION = new NullExpression(new ObjectType(StringConstants.JAVA_LANG_EXCEPTION, "java.lang.Exception", "Exception"));
    protected static final MergeTryWithResourcesStatementVisitor MERGE_TRY_WITH_RESOURCES_STATEMENT_VISITOR = new MergeTryWithResourcesStatementVisitor();

    private final TypeMaker typeMaker;
    private final Map<String, BaseType> typeBounds;
    private final LocalVariableMaker localVariableMaker;
    private final ByteCodeParser byteCodeParser;
    private final int majorVersion;
    private final String internalTypeName;
    private final ClassFileBodyDeclaration bodyDeclaration;
    private final DefaultStack<Expression> stack = new DefaultStack<>();
    private final Deque<Expression> enclosingInstances = new ArrayDeque<>();
    private final RemoveFinallyStatementsVisitor removeFinallyStatementsVisitor;
    private final RemoveBinaryOpReturnStatementsVisitor removeBinaryOpReturnStatementsVisitor;
    private final UpdateIntegerConstantTypeVisitor updateIntegerConstantTypeVisitor;
    private final SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
    private final MemberVisitor memberVisitor = new MemberVisitor();
    private boolean removeFinallyStatementsFlag;
    private boolean mergeTryWithResourcesStatementFlag;

    public StatementMaker(TypeMaker typeMaker, LocalVariableMaker localVariableMaker, ClassFileConstructorOrMethodDeclaration comd) {
        ClassFile classFile = comd.getClassFile();

        this.typeMaker = typeMaker;
        this.typeBounds = comd.getTypeBounds();
        this.localVariableMaker = localVariableMaker;
        this.majorVersion = classFile.getMajorVersion();
        this.internalTypeName = classFile.getInternalTypeName();
        this.bodyDeclaration = comd.getBodyDeclaration();
        this.byteCodeParser = new ByteCodeParser(typeMaker, localVariableMaker, classFile, this.bodyDeclaration, comd);
        this.removeFinallyStatementsVisitor = new RemoveFinallyStatementsVisitor();
        this.removeBinaryOpReturnStatementsVisitor = new RemoveBinaryOpReturnStatementsVisitor(localVariableMaker);
        this.updateIntegerConstantTypeVisitor = new UpdateIntegerConstantTypeVisitor(comd.getReturnedType());
    }

    public Statements make(ControlFlowGraph cfg, Statements statements) {
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

        // Replace pattern "synthetic_local_var = ...; return synthetic_local_var;" with "return ...;"
        statements.accept(removeBinaryOpReturnStatementsVisitor);

        // Remove last 'return' statement
        if (!statements.isEmpty() && statements.getLast().isReturnStatement()) {
            statements.removeLast();
        }

        localVariableMaker.popFrame();

        // Update integer constant type to 'byte', 'char', 'short' or 'int'
        statements.accept(updateIntegerConstantTypeVisitor);

        // Change ++i; with i++;
        replacePreOperatorWithPostOperator(statements);

        boolean breakToReturn = breakToReturn(cfg, statements, jumps);

        if (!breakToReturn && !jumps.isEmpty()) {
            updateJumpStatements(jumps, cfg);
        }

        return statements;
    }

    protected boolean breakToReturn(ControlFlowGraph cfg, Statements statements, Statements jumps) {
        boolean breakToReturn = false;
        if (jumps.size() == 1) {
            ClassFileBreakContinueStatement jumpStatement = (ClassFileBreakContinueStatement)jumps.get(0);
            for (Statement stmt : statements) {
                if (stmt.isReturnExpressionStatement() && stmt.getLineNumber() == cfg.getLineNumber(jumpStatement.getTargetOffset())) {
                    int lineNumber = cfg.getLineNumber(jumpStatement.getOffset()) + 1;
                    jumpStatement.setStatement(new ReturnExpressionStatement(lineNumber, stmt.getExpression().copyTo(lineNumber)));
                    breakToReturn = true;
                }
            }
        }
        return breakToReturn;
    }

    /**
     * A recursive, next neighbour first, statements builder from basic blocks.
     *
     * @param basicBlock Current basic block
     * @param statements List to populate
     */
    protected void makeStatements(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps) {
        Statements subStatements;
        Statements elseStatements;
        Expression condition;
        Expression exp1;
        Expression exp2;

        switch (basicBlock.getType()) {
            case TYPE_START:
                watchdog.check(basicBlock, basicBlock.getNext());
                makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
                break;
            case TYPE_END:
                break;
            case TYPE_STATEMENTS:
                watchdog.check(basicBlock, basicBlock.getNext());
                // intended fall through
            case TYPE_THROW:
                parseByteCode(basicBlock, statements);
                makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
                break;
            case TYPE_RETURN:
                Method method = basicBlock.getControlFlowGraph().getMethod();
                if (method.isLambda()) {
                    parseByteCode(basicBlock, statements);
                } else {
                    statements.add(ReturnStatement.RETURN);
                }
                break;
            case TYPE_RETURN_VALUE, TYPE_GOTO_IN_TERNARY_OPERATOR, TYPE_RET:
                parseByteCode(basicBlock, statements);
                break;
            case TYPE_SWITCH:
                parseSwitch(watchdog, basicBlock, statements, jumps);
                break;
            case TYPE_SWITCH_BREAK, TYPE_LOOP_END:
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
                if (!basicBlock.getSub2().matchType(TYPE_LOOP_END|TYPE_LOOP_CONTINUE|TYPE_LOOP_START) && stack.size() != backup.size()) {
                    stack.copy(backup);
                }
                watchdog.check(basicBlock, basicBlock.getSub2());
                elseStatements = makeSubStatements(watchdog, basicBlock.getSub2(), statements, jumps);
                statements.add(new IfElseStatement(condition, subStatements, elseStatements));
                watchdog.check(basicBlock, basicBlock.getNext());
                makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
                break;
            case TYPE_CONDITION:
                if (basicBlock.getSub1() != END) {
                    stack.push(makeExpression(watchdog, basicBlock.getSub1(), statements, jumps));
                }
                if (basicBlock.getSub2() != END) {
                    stack.push(makeExpression(watchdog, basicBlock.getSub2(), statements, jumps));
                }
                parseByteCode(basicBlock, statements);
                break;
            case TYPE_CONDITION_OR:
                watchdog.check(basicBlock, basicBlock.getSub1());
                exp1 = makeExpression(watchdog, basicBlock.getSub1(), statements, jumps);
                watchdog.check(basicBlock, basicBlock.getSub2());
                exp2 = makeExpression(watchdog, basicBlock.getSub2(), statements, jumps);
                stack.push(new BinaryOperatorExpression(basicBlock.getFirstLineNumber(), TYPE_BOOLEAN, exp1, "||", exp2, 14));
                break;
            case TYPE_CONDITION_AND:
                watchdog.check(basicBlock, basicBlock.getSub1());
                exp1 = makeExpression(watchdog, basicBlock.getSub1(), statements, jumps);
                watchdog.check(basicBlock, basicBlock.getSub2());
                exp2 = makeExpression(watchdog, basicBlock.getSub2(), statements, jumps);
                stack.push(new BinaryOperatorExpression(basicBlock.getFirstLineNumber(), TYPE_BOOLEAN, exp1, "&&", exp2, 13));
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
            case TYPE_LOOP_START, TYPE_LOOP_CONTINUE:
                statements.add(ContinueStatement.CONTINUE);
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
                throw new IllegalStateException("Unexpected basic block: " + basicBlock.getTypeName() + ':' + basicBlock.getIndex());
        }
    }

    protected Statements makeSubStatements(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps, BasicBlock updateBasicBlock) {
        Statements subStatements = makeSubStatements(watchdog, basicBlock, statements, jumps);

        if (updateBasicBlock != null) {
            subStatements.addAll(makeSubStatements(watchdog, updateBasicBlock, statements, jumps));
        }

        return subStatements;
    }

    protected Statements makeSubStatements(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps) {
        Statements subStatements = new Statements();

        if (!statements.isEmpty() && statements.getLast().isMonitorEnterStatement()) {
            subStatements.add(statements.removeLast());
        }

        localVariableMaker.pushFrame(subStatements);
        makeStatements(watchdog, basicBlock, subStatements, jumps);
        localVariableMaker.popFrame();
        replacePreOperatorWithPostOperator(subStatements);

        if (!subStatements.isEmpty() && subStatements.getFirst().isMonitorEnterStatement()) {
            statements.add(subStatements.removeFirst());
        }

        return subStatements;
    }

    protected Expression makeExpression(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps) {
        int initialStatementCount = statements.size();

        makeStatements(watchdog, basicBlock, statements, jumps);

        if (stack.isEmpty()) {
            // Interesting... Kotlin pattern.
            // https://github.com/JetBrains/intellij-community/blob/master/platform/built-in-server/src/org/jetbrains/builtInWebServer/SingleConnectionNetService.kt
            // final override fun connectToProcess(...)
            return new StringConstantExpression("JD-Core does not support Kotlin");
        }
        Expression expression = stack.pop();
        if (statements.size() > initialStatementCount) {
            // Is a multi-assignment ?
            Expression boe = statements.getLast().getExpression();

            if (boe != NO_EXPRESSION) {
                if (boe.getRightExpression() == expression) {
                    // Pattern matched -> Multi-assignment
                    statements.removeLast();
                    expression = boe;
                } else if (expression.isNewArray()) {
                    expression = NewArrayMaker.make(statements, expression);
                }
            }
        }
        return expression;
    }

    protected void parseSwitch(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps) {
        parseByteCode(basicBlock, statements);

        List<SwitchCase> switchCases = basicBlock.getSwitchCases();
        SwitchStatement switchStatement = (SwitchStatement)statements.getLast();
        Expression condition = switchStatement.getCondition();
        Type conditionType = condition.getType();
        List<SwitchStatement.Block> blocks = switchStatement.getBlocks();
        DefaultStack<Expression> localStack = new DefaultStack<>(stack);

        switchCases.sort(SWITCH_CASE_COMPARATOR);

        SwitchCase sc;
        BasicBlock bb;
        int j;
        Statements subStatements;
        for (int i=0, len=switchCases.size(); i<len; i++) {
            sc = switchCases.get(i);
            bb = sc.getBasicBlock();
            j = i + 1;

            while (j < len && bb == switchCases.get(j).getBasicBlock()) {
                j++;
            }

            subStatements = new Statements();

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

        if (size > 3 && condition.isLocalVariableReferenceExpression() && statements.get(size-2).isSwitchStatement()) {
            // Check pattern & make 'switch-string'
            SwitchStatementMaker.makeSwitchString(localVariableMaker, statements, switchStatement);
        } else if (condition.isArrayExpression()) {
            // Check pattern & make 'switch-enum'
            SwitchStatementMaker.makeSwitchEnum(bodyDeclaration, switchStatement, typeMaker);
        }

        makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
    }

    protected void parseTry(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps, boolean jsr, boolean eclipse) {
        Statements tryStatements;
        DefaultList<TryStatement.CatchClause> catchClauses = new DefaultList<>();
        Statements finallyStatements = null;

        tryStatements = makeSubStatements(watchdog, basicBlock.getSub1(), statements, jumps);
        if (basicBlock.getNext().getType() == TYPE_LOOP_CONTINUE) {
            tryStatements.add(ContinueStatement.CONTINUE);
            basicBlock.setNext(END);
        }
        for (ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
            if (exceptionHandler.getInternalThrowableName() == null) {
                // 'finally' handler
                stack.push(FINALLY_EXCEPTION_EXPRESSION);

                finallyStatements = makeSubStatements(watchdog, exceptionHandler.getBasicBlock(), statements, jumps);

                if (!finallyStatements.getFirst().isMonitorEnterStatement()) {
                    removeFinallyStatementsFlag |= !jsr;

                    Expression leftExpression = finallyStatements.getFirst().getExpression().getLeftExpression();

                    if (leftExpression.isLocalVariableReferenceExpression()) {
                        Statement statement = finallyStatements.getLast();

                        if (statement.isThrowStatement()) {
                            // Remove synthetic local variable
                            Expression expression = statement.getExpression();

                            if (expression.isLocalVariableReferenceExpression()) {
                                ClassFileLocalVariableReferenceExpression vre1 = (ClassFileLocalVariableReferenceExpression) expression;
                                ClassFileLocalVariableReferenceExpression vre2 = (ClassFileLocalVariableReferenceExpression) leftExpression;

                                if (vre1.getLocalVariable() == vre2.getLocalVariable()) {
                                    localVariableMaker.removeLocalVariable(vre2.getLocalVariable());
                                    // Remove first statement (storage of finally localVariable)
                                    finallyStatements.removeFirst();
                                }
                            }
                        }
                    }

                    // Remove last statement (throw finally localVariable)
                    finallyStatements.removeLast();
                }
            } else {
                stack.push(new NullExpression(typeMaker.makeFromInternalTypeName(exceptionHandler.getInternalThrowableName())));

                Statements catchStatements = new Statements();
                localVariableMaker.pushFrame(catchStatements);

                BasicBlock bb = exceptionHandler.getBasicBlock();
                int lineNumber = bb.getControlFlowGraph().getLineNumber(bb.getFromOffset());
                int index = ByteCodeParser.getExceptionLocalVariableIndex(bb);
                ObjectType ot = typeMaker.makeFromInternalTypeName(exceptionHandler.getInternalThrowableName());
                int offset = bb.getFromOffset();
                byte[] code = bb.getControlFlowGraph().getMethod().<AttributeCode>getAttribute("Code").getCode();

                if (code[offset] == ASTORE) {
                    offset += 2;
                } else {
                    offset++;    // POP, ASTORE_1 ... ASTORE_3
                }

                AbstractLocalVariable exception = localVariableMaker.getExceptionLocalVariable(index, offset, ot);

                if (bb != null && bb.getNext().getPredecessors().size() > 1) {
                    bb.getNext().getPredecessors().remove(bb);
                    bb.setNext(END);
                }

                makeStatements(watchdog, bb, catchStatements, jumps);
                localVariableMaker.popFrame();
                removeExceptionReference(catchStatements);

                if (lineNumber != UNKNOWN_LINE_NUMBER) {
                    searchFirstLineNumberVisitor.init();
                    searchFirstLineNumberVisitor.visit(catchStatements);
                    if (searchFirstLineNumberVisitor.getLineNumber() == lineNumber) {
                        lineNumber = UNKNOWN_LINE_NUMBER;
                    }
                }

                replacePreOperatorWithPostOperator(catchStatements);

                ClassFileTryStatement.CatchClause cc = new ClassFileTryStatement.CatchClause(lineNumber, ot, exception, catchStatements);

                if (exceptionHandler.getOtherInternalThrowableNames() != null) {
                    for (String name : exceptionHandler.getOtherInternalThrowableNames()) {
                        cc.addType(typeMaker.makeFromInternalTypeName(name));
                    }
                }

                catchClauses.add(cc);
            }
        }

        // 'try', 'try-with-resources' or 'synchronized' ?
        Statement statement = null;

        if (finallyStatements != null && !finallyStatements.isEmpty() && finallyStatements.getFirst().isMonitorExitStatement()) {
            statement = SynchronizedStatementMaker.make(localVariableMaker, statements, tryStatements);
        } else {
            if (majorVersion >= MAJOR_1_7) {
                assert !jsr;
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
        if (!catchStatements.isEmpty() && catchStatements.getFirst().isExpressionStatement()) {
            Expression exp = catchStatements.getFirst().getExpression();

            if (exp.isBinaryOperatorExpression()) {
                if (exp.getLeftExpression().isLocalVariableReferenceExpression() && exp.getRightExpression().isNullExpression()) {
                    catchStatements.removeFirst();
                }
            } else if (exp.isNullExpression()) {
                catchStatements.removeFirst();
            }
        }
    }

    protected void parseJSR(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps) {
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

    protected void parseIf(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps) {
        BasicBlock condition = basicBlock.getCondition();

        if (condition.getType() == TYPE_CONDITION_AND) {
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

            if (subStatements.getFirst().isThrowStatement()) {
                Expression e = subStatements.getFirst().getExpression();
                if (e.isNewExpression()) {
                    BaseExpression parameters = e.getParameters();
                    if (parameters != null && !parameters.isList()) {
                        message = parameters.getFirst();
                    }
                }
            }

            statements.add(new AssertStatement(cond, message));
            makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
        } else {
            makeStatements(watchdog, basicBlock.getCondition(), statements, jumps);
            Expression cond = stack.pop();
            DefaultStack<Expression> backup = new DefaultStack<>(stack);
            Statements subStatements = makeSubStatements(watchdog, basicBlock.getSub1(), statements, jumps);
            if (stack.size() != backup.size()) {
                stack.copy(backup);
            }
            statements.add(new IfStatement(cond, subStatements));
            int index = statements.size();
            makeStatements(watchdog, basicBlock.getNext(), statements, jumps);

            if (stack.size() == 1 && basicBlock.getNext().matchType(TYPE_GOTO_IN_TERNARY_OPERATOR) && !statements.isEmpty() && statements.getLast() instanceof IfStatement) {
                IfStatement ifStatement = (IfStatement) statements.getLast();
                BaseStatement thenStatements = ifStatement.getStatements();
                if (thenStatements.size() == 1) {
                    Statement thenStatement = thenStatements.getFirst();
                    if (thenStatement instanceof ReturnExpressionStatement) {
                        ReturnExpressionStatement returnExp = (ReturnExpressionStatement) thenStatement;
                        if (returnExp.getExpression().getType().equals(stack.peek().getType())) {
                            Expression exp = stack.pop();
                            statements.add(new ReturnExpressionStatement(exp.getLineNumber(), exp));
                        }
                    }
                }
            }
            
            if (subStatements.size() == 1 &&
                    index+1 == statements.size() &&
                    subStatements.getFirst().isReturnExpressionStatement() &&
                    statements.get(index).isReturnExpressionStatement()) {
                Statement cfres1 = subStatements.getFirst();

                if (cond.getLineNumber() >= cfres1.getLineNumber()) {
                    Statement cfres2 = statements.get(index);

                    if (cfres1.getLineNumber() == cfres2.getLineNumber()) {
                        statements.subList(index-1, statements.size()).clear();
                        statements.add(new ReturnExpressionStatement(newTernaryOperatorExpression(
                            cfres1.getLineNumber(), cond, cfres1.getExpression(), cfres2.getExpression())));
                    }
                }
            }
        }
    }

    protected void parseLoop(WatchDog watchdog, BasicBlock basicBlock, Statements statements, Statements jumps) {
        BasicBlock sub1 = basicBlock.getSub1();
        BasicBlock updateBasicBlock = null;

        if (sub1.getType() == TYPE_IF && sub1.getCondition() == END) {
            updateBasicBlock = sub1.getNext();
            sub1 = sub1.getSub1();
        }

        if (sub1.getType() == TYPE_IF || (sub1.getType() == TYPE_IF_ELSE && sub1.getSub2() == LOOP_END)) {
            BasicBlock ifBB = sub1;

            if (ifBB.getNext() == LOOP_END || ifBB.getSub2() == LOOP_END) {
                // 'while' or 'for' loop
                makeStatements(watchdog, ifBB.getCondition(), statements, jumps);
                statements.add(LoopStatementMaker.makeLoop(
                    majorVersion, typeBounds, localVariableMaker, basicBlock, statements, stack.pop(),
                    makeSubStatements(watchdog, ifBB.getSub1(), statements, jumps, updateBasicBlock), jumps));
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
                    statements.add(LoopStatementMaker.makeDoWhileLoop(basicBlock, stack.pop(), subStatements, jumps));
                } else {
                    // 'while' or 'for' loop
                    ifBB.getCondition().inverseCondition();
                    makeStatements(watchdog, ifBB.getCondition(), statements, jumps);
                    statements.add(LoopStatementMaker.makeLoop(
                        majorVersion, typeBounds, localVariableMaker, basicBlock, statements, stack.pop(),
                        makeSubStatements(watchdog, ifBB.getNext(), statements, jumps, updateBasicBlock), jumps));
                }

                makeStatements(watchdog, basicBlock.getNext(), statements, jumps);
                return;
            }
        }

        BasicBlock next = sub1.getNext();
        BasicBlock last = sub1;

        while (next.matchType(GROUP_SINGLE_SUCCESSOR) && next.getPredecessors().size() == 1) {
            last = next;
            next = next.getNext();
        }

        if (next == LOOP_START && last.getType() == TYPE_IF && last.getSub1() == LOOP_END && countStartLoop(sub1) == 1) {
            // 'do-while'
            Statements subStatements;

            last.getCondition().inverseCondition();
            last.setType(TYPE_END);

            if (sub1.getType() == TYPE_LOOP && sub1.getNext() == last && countStartLoop(sub1.getSub1()) == 0) {
                changeEndLoopToStartLoop(new BitSet(), sub1.getSub1());
                subStatements = makeSubStatements(watchdog, sub1.getSub1(), statements, jumps, updateBasicBlock);

                if (subStatements.getLast() != ContinueStatement.CONTINUE) {
                    throw new IllegalStateException("StatementMaker.parseLoop(...) : unexpected basic block for create a do-while loop");
                }

                subStatements.removeLast();
            } else {
                createDoWhileContinue(last);
                subStatements = makeSubStatements(watchdog, sub1, statements, jumps, updateBasicBlock);
            }

            makeStatements(watchdog, last.getCondition(), subStatements, jumps);
            statements.add(LoopStatementMaker.makeDoWhileLoop(basicBlock, stack.pop(), subStatements, jumps));
        } else {
            // Infinite loop
            statements.add(LoopStatementMaker.makeLoop(
                localVariableMaker, basicBlock, statements, makeSubStatements(watchdog, sub1, statements, jumps, updateBasicBlock), jumps));
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
                case TYPE_TRY, TYPE_TRY_JSR, TYPE_TRY_ECLIPSE:
                    count += countStartLoop(bb.getSub1());

                    for (ExceptionHandler exceptionHandler : bb.getExceptionHandlers()) {
                        count += countStartLoop(exceptionHandler.getBasicBlock());
                    }
                    break;
                case TYPE_IF_ELSE, TYPE_TERNARY_OPERATOR:
                    count += countStartLoop(bb.getSub2());
                    // intended fall through
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
        if (!basicBlock.matchType(GROUP_END| TYPE_LOOP_END) && !visited.get(basicBlock.getIndex())) {
            visited.set(basicBlock.getIndex());

            switch (basicBlock.getType()) {
                case TYPE_CONDITIONAL_BRANCH, TYPE_JSR, TYPE_CONDITION:
                    if (basicBlock.getBranch() == LOOP_END) {
                        basicBlock.setBranch(LOOP_START);
                    } else {
                        changeEndLoopToStartLoop(visited, basicBlock.getBranch());
                    }
                    // intended fall through
                case TYPE_START, TYPE_STATEMENTS, TYPE_GOTO, TYPE_GOTO_IN_TERNARY_OPERATOR, TYPE_LOOP:
                    if (basicBlock.getNext() == LOOP_END) {
                        basicBlock.setNext(LOOP_START);
                    } else {
                        changeEndLoopToStartLoop(visited, basicBlock.getNext());
                    }
                    break;
                case TYPE_TRY_DECLARATION, TYPE_TRY, TYPE_TRY_JSR, TYPE_TRY_ECLIPSE:
                    for (ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                        if (exceptionHandler.getBasicBlock() == LOOP_END) {
                            exceptionHandler.setBasicBlock(LOOP_START);
                        } else {
                            changeEndLoopToStartLoop(visited, exceptionHandler.getBasicBlock());
                        }
                    }
                    break;
                case TYPE_IF_ELSE, TYPE_TERNARY_OPERATOR:
                    if (basicBlock.getSub2() == LOOP_END) {
                        basicBlock.setSub2(LOOP_START);
                    } else {
                        changeEndLoopToStartLoop(visited, basicBlock.getSub2());
                    }
                    // intended fall through
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
                case TYPE_SWITCH, TYPE_SWITCH_DECLARATION:
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
        if (ObjectType.TYPE_CLASS.equals(exp1.getType()) && ObjectType.TYPE_CLASS.equals(exp2.getType()) && condition.isBinaryOperatorExpression() && condition.getLeftExpression().isFieldReferenceExpression() && condition.getRightExpression().isNullExpression()) {
            FieldReferenceExpression freCond = (FieldReferenceExpression) condition.getLeftExpression();

            if (freCond.getInternalTypeName().equals(internalTypeName)) {
                String fieldName = freCond.getName();

                if (fieldName.startsWith(StringConstants.CLASS_DOLLAR)) {
                    if ("==".equals(condition.getOperator()) && exp1.isBinaryOperatorExpression() && checkFieldReference(fieldName, exp2)) {
                        if (exp1.getRightExpression().isMethodInvocationExpression() && checkFieldReference(fieldName, exp1.getLeftExpression())) {
                            MethodInvocationExpression mie = (MethodInvocationExpression) exp1.getRightExpression();

                            if (mie.getParameters().isStringConstantExpression() && StringConstants.CLASS_DOLLAR.equals(mie.getName()) && mie.getInternalTypeName().equals(internalTypeName)) {
                                // JDK 1.4.2 '.class' found ==> Convert '(class$java$lang$String == null) ? (class$java$lang$String = TestDotClass.class$("java.lang.String") : class$java$lang$String)' to 'String.class'
                                return createObjectTypeReferenceDotClassExpression(lineNumber, fieldName, mie);
                            }
                        }
                    } else if ("!=".equals(condition.getOperator()) && exp2.isBinaryOperatorExpression() && checkFieldReference(fieldName, exp1) && exp2.getRightExpression().isMethodInvocationExpression() && checkFieldReference(fieldName, exp2.getLeftExpression())) {
                        MethodInvocationExpression mie = (MethodInvocationExpression) exp2.getRightExpression();

                        if (mie.getParameters().isStringConstantExpression() && StringConstants.CLASS_DOLLAR.equals(mie.getName()) && mie.getInternalTypeName().equals(internalTypeName)) {
                            // JDK 1.1.8 '.class' found ==> Convert '(class$java$lang$String != null) ? class$java$lang$String : (class$java$lang$String = TestDotClass.class$("java.lang.String"))' to 'String.class'
                            return createObjectTypeReferenceDotClassExpression(lineNumber, fieldName, mie);
                        }
                    }
                }
            }
        }

        return newTernaryOperatorExpression(lineNumber, condition, exp1, exp2);
    }

    protected TernaryOperatorExpression newTernaryOperatorExpression(int lineNumber, Expression condition, Expression expressionTrue, Expression expressionFalse) {
        Type expressionTrueType = expressionTrue.getType();
        Type expressionFalseType = expressionFalse.getType();
        Type type;

        if (expressionTrue.isNullExpression()) {
            type = expressionFalseType;
        } else if (expressionFalse.isNullExpression() || expressionTrueType.equals(expressionFalseType)) {
            type = expressionTrueType;
        } else if (expressionTrueType.isPrimitiveType() && expressionFalseType.isPrimitiveType()) {
            int flags = ((PrimitiveType)expressionTrueType).getFlags() | ((PrimitiveType)expressionFalseType).getFlags();

            if ((flags & FLAG_DOUBLE) != 0) {
                type = TYPE_DOUBLE;
            } else if ((flags & FLAG_FLOAT) != 0) {
                type = TYPE_FLOAT;
            } else if ((flags & FLAG_LONG) != 0) {
                type = TYPE_LONG;
            } else {
                flags = ((PrimitiveType)expressionTrueType).getFlags() & ((PrimitiveType)expressionFalseType).getFlags();

                if ((flags & FLAG_INT) != 0) {
                    type = TYPE_INT;
                } else if ((flags & FLAG_SHORT) != 0) {
                    type = TYPE_SHORT;
                } else if ((flags & FLAG_CHAR) != 0) {
                    type = TYPE_CHAR;
                } else if ((flags & FLAG_BYTE) != 0) {
                    type = TYPE_BYTE;
                } else {
                    type = MAYBE_BOOLEAN_TYPE;
                }
            }
        } else if (expressionTrueType.isObjectType() && expressionFalseType.isObjectType()) {
            if (typeMaker.isAssignable(typeBounds, (ObjectType)expressionTrueType, (ObjectType)expressionFalseType)) {
                type = getTernaryOperatorExpressionType((ObjectType)expressionTrueType, (ObjectType)expressionFalseType);
            } else if (typeMaker.isAssignable(typeBounds, (ObjectType)expressionFalseType, (ObjectType)expressionTrueType)) {
                type = getTernaryOperatorExpressionType((ObjectType)expressionFalseType, (ObjectType)expressionTrueType);
            } else {
                type = TYPE_UNDEFINED_OBJECT;
            }
        } else {
            type = TYPE_UNDEFINED_OBJECT;
        }

        return new TernaryOperatorExpression(lineNumber, type, condition, expressionTrue, expressionFalse);
    }

    protected Type getTernaryOperatorExpressionType(ObjectType ot1, ObjectType ot2) {
        if (ot1.getTypeArguments() != null) {
            if (ot2.getTypeArguments() == null) {
                return ot1.createType(null);
            }
            if (!ot1.isTypeArgumentAssignableFrom(typeMaker, Collections.emptyMap(), typeBounds, ot2)) {
                if (ot2.isTypeArgumentAssignableFrom(typeMaker, Collections.emptyMap(), typeBounds, ot1)) {
                    return ot1.createType(ot2.getTypeArguments());
                }
                return ot1.createType(null);
            }
        }
        return ot1;
    }

    protected boolean checkFieldReference(String fieldName, Expression expression) {
        if (expression.isFieldReferenceExpression()) {
            FieldReferenceExpression fre = (FieldReferenceExpression) expression;
            return fre.getName().equals(fieldName) && fre.getInternalTypeName().equals(internalTypeName);
        }

        return false;
    }

    protected Expression createObjectTypeReferenceDotClassExpression(int lineNumber, String fieldName, MethodInvocationExpression mie) {
        // Add SYNTHETIC flags to field
        memberVisitor.init(fieldName);

        for (ClassFileFieldDeclaration field : bodyDeclaration.getFieldDeclarations()) {
            field.getFieldDeclarators().accept(memberVisitor);
            if (memberVisitor.found()) {
                field.setFlags(field.getFlags() | ACC_SYNTHETIC);
                break;
            }
        }

        // Add SYNTHETIC flags to method named 'class$'
        memberVisitor.init(StringConstants.CLASS_DOLLAR);

        for (ClassFileConstructorOrMethodDeclaration member : bodyDeclaration.getMethodDeclarations()) {
            member.accept(memberVisitor);
            if (memberVisitor.found()) {
                member.setFlags(member.getFlags() | ACC_SYNTHETIC);
                break;
            }
        }

        String typeName = mie.getParameters().getStringValue();
        ObjectType ot = typeMaker.makeFromInternalTypeName(typeName.replace('.', '/'));

        return new TypeReferenceDotClassExpression(lineNumber, ot);
    }

    protected void parseByteCode(BasicBlock basicBlock, Statements statements) {
        byteCodeParser.parse(basicBlock, statements, stack, enclosingInstances);
    }

    protected void replacePreOperatorWithPostOperator(Statements statements) {
        Iterator<Statement> iterator = statements.iterator();

        Statement statement;
        while (iterator.hasNext()) {
            statement = iterator.next();

            if (statement.getExpression().isPreOperatorExpression()) {
                Expression poe = statement.getExpression();
                String operator = poe.getOperator();

                if ("++".equals(operator) || "--".equals(operator)) {
                    if (statement.isExpressionStatement()) {
                        ExpressionStatement es = (ExpressionStatement) statement;
                        // Replace pre-operator statement with post-operator statement
                        es.setExpression(new PostOperatorExpression(poe.getLineNumber(), poe.getExpression(), operator));
                    } else if (statement.isReturnExpressionStatement()) {
                        ReturnExpressionStatement res = (ReturnExpressionStatement) statement;
                        // Replace pre-operator statement with post-operator statement
                        res.setExpression(new PostOperatorExpression(poe.getLineNumber(), poe.getExpression(), operator));
                    }
                }
            }
        }
    }

    protected void updateJumpStatements(Statements jumps, ControlFlowGraph cfg) {
        Iterator<Statement> iterator = jumps.iterator();

        while (iterator.hasNext()) {
            ClassFileBreakContinueStatement statement = (ClassFileBreakContinueStatement)iterator.next();
            CommentStatement commentStatement = new CommentStatement();
            commentStatement.setText("// goto line number " + cfg.getLineNumber(statement.getTargetOffset()));
            statement.setStatement(commentStatement);
        }
    }

    protected static class SwitchCaseComparator implements java.io.Serializable, Comparator<SwitchCase> {
        /**
         * Comparators should be Serializable: A non-serializable Comparator can prevent an otherwise-Serializable ordered collection from being serializable.
         */
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(SwitchCase sc1, SwitchCase sc2) {
            int diff = sc1.getOffset() - sc2.getOffset();

            if (diff != 0) {
                return diff;
            }

            return sc1.getValue() - sc2.getValue();
        }
    }

    protected static class MemberVisitor extends AbstractJavaSyntaxVisitor {
        private String name;
        private boolean found;

        public void init(String name) {
            this.name = name;
            this.found = false;
        }

        public boolean found() {
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
}
