/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.cfg;

import org.apache.bcel.classfile.Method;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.ExceptionHandler;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.SwitchCase;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ByteCodeWriter;
import org.jd.core.v1.util.DefaultList;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.GROUP_CODE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.LOOP_CONTINUE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.LOOP_END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.LOOP_START;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.RETURN;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.SWITCH_BREAK;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITIONAL_BRANCH;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION_AND;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION_OR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_GOTO;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_GOTO_IN_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_IF;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_IF_ELSE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_INFINITE_GOTO;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_JSR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_JUMP;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_LOOP;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RET;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN_VALUE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_START;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_STATEMENTS;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_SWITCH;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_SWITCH_DECLARATION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_THROW;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY_DECLARATION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY_ECLIPSE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY_JSR;

/**
 * A state diagram writer.
 *
 * http://plantuml.com/state.html
 * http://plantuml.com/plantuml
 */
public final class ControlFlowGraphPlantUMLWriter {
    private static final String EOL = "\\n\\\n";

    private static final String REDUCED = "<<Reduced>>\n";

    private static final String AS = "\" as ";

    private static final String STATE = "state \"";
    
    private static final int MAX_OFFSET = Integer.MAX_VALUE;

    private ControlFlowGraphPlantUMLWriter() {
    }
    
    public static String write(ControlFlowGraph cfg) {
        if (cfg.getBasicBlocks() == null) {
            return null;
        }
        Set<BasicBlock> set = new TreeSet<>(Comparator.comparingInt(BasicBlock::getIndex));

        search(set, cfg.getStart());

        DefaultList<BasicBlock> list = new DefaultList<>(set);

        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam state {\n");
        sb.append("  BackgroundColor<<Reduced>> #BBD7B7\n");
        sb.append("  BorderColor<<Reduced>> Green\n");
        sb.append("  BackgroundColor<<Synthetic>> PowderBlue\n");
        sb.append("  BorderColor<<Synthetic>> DodgerBlue\n");
        sb.append("  BackgroundColor<<ToReduce>> Orange\n");
        sb.append("  BorderColor<<ToReduce>> #FF740E\n");
        sb.append("}\n");

        sb.append("skinparam BackgroundColor #2B2B2B\n");
        sb.append("skinparam state {\n");
        sb.append("  StartColor #999999\n");
        sb.append("  BackgroundColor #D6BF55\n");
        sb.append("  BorderColor #F6DF57\n");
        sb.append("}\n");
        sb.append("skinparam sequence {\n");
        sb.append("  ArrowColor #999999\n");
        sb.append("  ArrowFontColor #AAAAAA\n");
        sb.append("}\n");

        Method method = cfg.getMethod();

        for (BasicBlock basicBlock : list) {
            writeState(sb, method, basicBlock);
        }

        for (BasicBlock basicBlock : list) {
            writeLink(sb, basicBlock);
        }
        sb.append("@enduml\n");
        return sb.toString();
    }

    private static void search(Set<BasicBlock> set, BasicBlock basicBlock) {
        if (!set.contains(basicBlock)) {
            set.add(basicBlock);

            switch (basicBlock.getType()) {
                case TYPE_START, TYPE_STATEMENTS, TYPE_LOOP, TYPE_GOTO, TYPE_GOTO_IN_TERNARY_OPERATOR:
                    search(set, basicBlock.getNext());
                    break;
                case TYPE_CONDITIONAL_BRANCH, TYPE_JSR:
                    search(set, basicBlock.getNext());
                    search(set, basicBlock.getBranch());
                    break;
                case TYPE_SWITCH:
                    search(set, basicBlock.getNext());
                    // intended fall through
                case TYPE_SWITCH_DECLARATION:
                    for (SwitchCase switchCase : basicBlock.getSwitchCases()) {
                        search(set, switchCase.getBasicBlock());
                    }
                    break;
                case TYPE_TRY, TYPE_TRY_JSR, TYPE_TRY_ECLIPSE:
                    search(set, basicBlock.getSub1());
                    // intended fall through
                case TYPE_TRY_DECLARATION:
                    search(set, basicBlock.getNext());
                    for (BasicBlock.ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                        search(set, exceptionHandler.getBasicBlock());
                    }
                    break;
                case TYPE_IF:
                    search(set, basicBlock.getCondition());
                    search(set, basicBlock.getNext());
                    search(set, basicBlock.getSub1());
                    break;
                case TYPE_IF_ELSE, TYPE_TERNARY_OPERATOR:
                    search(set, basicBlock.getNext());
                    // intended fall through
                case TYPE_CONDITION_TERNARY_OPERATOR:
                    search(set, basicBlock.getCondition());
                    // intended fall through
                case TYPE_CONDITION, TYPE_CONDITION_OR, TYPE_CONDITION_AND:
                    search(set, basicBlock.getSub1());
                    search(set, basicBlock.getSub2());
                    break;
            }
        }
    }

    private static void writeState(StringBuilder sb, Method method, BasicBlock basicBlock) {
        if (basicBlock.getFromOffset() > MAX_OFFSET) {
            return;
        }

        String id = getStateId(basicBlock);

        switch (basicBlock.getType()) {
            case TYPE_STATEMENTS, TYPE_IF, TYPE_IF_ELSE, TYPE_TERNARY_OPERATOR, TYPE_TRY, TYPE_TRY_JSR, TYPE_TRY_ECLIPSE:
                sb.append(STATE).append(basicBlock.getTypeName()).append(" : ").append(basicBlock.getIndex()).append(AS).append(id).append(REDUCED);
                writeStateOffsets(sb, id, basicBlock);
                writeLineNumbers(sb, id, basicBlock);
                writeStateEnd(sb, id, basicBlock.getNext(), "next");
                writeStatePredecessors(sb, id, basicBlock);
                writeStateCode(sb, id, method, basicBlock);
                break;
            case TYPE_RETURN:
                if (basicBlock == RETURN) {
                    break;
                }
                // intended fall through
            case TYPE_THROW, TYPE_RETURN_VALUE, TYPE_RET, TYPE_SWITCH, TYPE_INFINITE_GOTO, TYPE_GOTO_IN_TERNARY_OPERATOR:
                sb.append(STATE).append(basicBlock.getTypeName()).append(" : ").append(basicBlock.getIndex()).append(AS).append(id).append(REDUCED);
                writeStateOffsets(sb, id, basicBlock);
                writeLineNumbers(sb, id, basicBlock);
                writeStatePredecessors(sb, id, basicBlock);
                writeStateCode(sb, id, method, basicBlock);
                break;
            case TYPE_CONDITION, TYPE_CONDITION_OR, TYPE_CONDITION_AND, TYPE_CONDITION_TERNARY_OPERATOR:
                sb.append(STATE).append(basicBlock.getTypeName()).append(" : ").append(basicBlock.getIndex()).append(AS).append(id).append(REDUCED);
                writeStateOffsets(sb, id, basicBlock);
                writeLineNumbers(sb, id, basicBlock);
                writeInverseCondition(sb, id, basicBlock);
                writeStateCode(sb, id, method, basicBlock);
                break;
            case TYPE_JSR:
                sb.append(STATE).append(basicBlock.getTypeName()).append(" : ").append(basicBlock.getIndex()).append(AS).append(id).append('\n');
                writeStateOffsets(sb, id, basicBlock);
                writeLineNumbers(sb, id, basicBlock);
                writeStateEnd(sb, id, basicBlock.getNext(), "next");
                writeStateEnd(sb, id, basicBlock.getBranch(), "branch");
                writeStatePredecessors(sb, id, basicBlock);
                writeStateCode(sb, id, method, basicBlock);
                break;
            case TYPE_CONDITIONAL_BRANCH:
                sb.append(STATE).append(basicBlock.getTypeName()).append(" : ").append(basicBlock.getIndex()).append(AS).append(id).append('\n');
                writeStateOffsets(sb, id, basicBlock);
                writeLineNumbers(sb, id, basicBlock);
                writeStatePredecessors(sb, id, basicBlock);
                writeStateCode(sb, id, method, basicBlock);
                break;
            case TYPE_TRY_DECLARATION, TYPE_GOTO, TYPE_SWITCH_DECLARATION:
                sb.append(STATE).append(basicBlock.getTypeName()).append(" : ").append(basicBlock.getIndex()).append(AS).append(id).append("<<ToReduce>>\n");
                writeStateOffsets(sb, id, basicBlock);
                writeLineNumbers(sb, id, basicBlock);
                writeStatePredecessors(sb, id, basicBlock);
                writeStateCode(sb, id, method, basicBlock);
                break;
            case TYPE_LOOP:
                sb.append(STATE).append(basicBlock.getTypeName()).append(" : ").append(basicBlock.getIndex()).append(AS).append(id).append(" {\n");
                sb.append("[*] --> ").append(getStateId(basicBlock.getSub1())).append('\n');

                Set<BasicBlock> set = new HashSet<>();

                search(set, basicBlock.getSub1());

                set.remove(basicBlock);

                for (BasicBlock bb : set) {
                    writeState(sb, method, bb);
                }
                for (BasicBlock bb : set) {
                    writeLink(sb, bb);
                }
                sb.append("}\n");
                writeStateOffsets(sb, id, basicBlock);
                writeStateEnd(sb, id, basicBlock.getNext(), "next");
                writeStatePredecessors(sb, id, basicBlock);
                break;
            case TYPE_JUMP:
                sb.append(STATE).append(basicBlock.getTypeName()).append(" : ").append(basicBlock.getIndex()).append(AS).append(id).append("<<Synthetic>>\n");
                sb.append(id).append(" : offset = ").append(basicBlock.getFromOffset()).append("\n");
                sb.append(id).append(" : targetOffset = ").append(basicBlock.getToOffset()).append("\n");
                break;
        }
    }

    private static void writeStateOffsets(StringBuilder sb, String id, BasicBlock basicBlock) {
        sb.append(id).append(" : fromOffset = ").append(basicBlock.getFromOffset()).append("\n");
        sb.append(id).append(" : toOffset = ").append(basicBlock.getToOffset()).append("\n");
    }

    private static void writeStatePredecessors(StringBuilder sb, String id, BasicBlock basicBlock) {
        Set<BasicBlock> predecessors = basicBlock.getPredecessors();

        if (!predecessors.isEmpty()) {
            sb.append(id).append(" : predecessors = [");

            Iterator<BasicBlock> iterator = predecessors.iterator();

            sb.append(iterator.next().getIndex());
            while (iterator.hasNext()) {
                sb.append(", ").append(iterator.next().getIndex());
            }

            sb.append("]\n");
        }
    }

    private static void writeInverseCondition(StringBuilder sb, String id, BasicBlock basicBlock) {
        if (basicBlock.matchType(TYPE_CONDITION|TYPE_CONDITION_TERNARY_OPERATOR|TYPE_GOTO_IN_TERNARY_OPERATOR)) {
            sb.append(id).append(" : inverseCondition = ").append(basicBlock.mustInverseCondition()).append("\n");
        }
    }

    private static void writeStateCode(StringBuilder sb, String id, Method method, BasicBlock basicBlock) {
        if ((method != null) && basicBlock.matchType(GROUP_CODE) && (basicBlock.getFromOffset() < basicBlock.getToOffset())) {
            String byteCode = new ByteCodeWriter().write("  ", method, basicBlock.getFromOffset(), basicBlock.getToOffset());

            byteCode = byteCode.substring(0, byteCode.length()-1).replace("\n", EOL).replace('[', '{');
            sb.append(id).append(" : code =").append(EOL).append(byteCode).append("\n");
        }
    }

    private static void writeLineNumbers(StringBuilder sb, String id, BasicBlock basicBlock) {
        if (basicBlock.getFirstLineNumber() > 0) {
            sb.append(id).append(" : firstLineNumber = ").append(basicBlock.getFirstLineNumber()).append("\n");
        }
        if (basicBlock.getLastLineNumber() > 0) {
            sb.append(id).append(" : lastLineNumber = ").append(basicBlock.getLastLineNumber()).append("\n");
        }
    }

    private static void writeStateEnd(StringBuilder sb, String id, BasicBlock basicBlock, String label) {
        if (basicBlock == END) {
            sb.append(id).append(" : ").append(label).append(" = &#9673;\n");
        }
    }

    private static void writeLink(StringBuilder sb, BasicBlock basicBlock) {
        if (basicBlock.getFromOffset() > MAX_OFFSET) {
            return;
        }

        String id = getStateId(basicBlock);

        switch (basicBlock.getType()) {
            case TYPE_START:
                sb.append("[*] --> ").append(getStateId(basicBlock.getNext())).append('\n');
                break;
            case TYPE_STATEMENTS, TYPE_GOTO, TYPE_GOTO_IN_TERNARY_OPERATOR:
                writeLink(sb, id, basicBlock.getNext(), "next");
                break;
            case TYPE_CONDITION:
                writeLink(sb, id, basicBlock.getSub1(), "sub1");
                writeLink(sb, id, basicBlock.getSub2(), "sub2");
                // intended fall through
            case TYPE_CONDITIONAL_BRANCH:
                writeLink(sb, id, basicBlock.getNext(), "next");
                writeLink(sb, id, basicBlock.getBranch(), "branch");
                break;
            case TYPE_SWITCH:
                writeLink(sb, id, basicBlock.getNext(), "next");
                // intended fall through
            case TYPE_SWITCH_DECLARATION:
                BasicBlock next = basicBlock.getNext();

                for (SwitchCase switchCase : basicBlock.getSwitchCases()) {
                    if ((switchCase.getBasicBlock() != END) && (switchCase.getBasicBlock() != next)) {
                        writeLink(sb, id, switchCase.getBasicBlock(), switchCase.isDefaultCase() ? "default" : "case: " + switchCase.getValue());
                    }
                }
                break;
            case TYPE_TRY, TYPE_TRY_JSR, TYPE_TRY_ECLIPSE:
                writeLink(sb, id, basicBlock.getSub1(), "try");
                writeLink(sb, id, basicBlock.getNext(), "next");

                for (ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                    sb.append(id).append(" --> ").append(getStateId(exceptionHandler.getBasicBlock()));

                    if (exceptionHandler.getInternalThrowableName() == null) {
                        sb.append(" : finally");
                    } else {
                        sb.append(" : catch ").append(exceptionHandler.getInternalThrowableName());

                        if (exceptionHandler.getOtherInternalThrowableNames() != null) {
                            for (String name : exceptionHandler.getOtherInternalThrowableNames()) {
                                sb.append("\\ncatch ").append(name);
                            }
                        }
                    }

                    sb.append('\n');
                }
                break;
            case TYPE_TRY_DECLARATION:
                writeLink(sb, id, basicBlock.getNext(), "try");

                for (BasicBlock.ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                    sb.append(id).append(" --> ").append(getStateId(exceptionHandler.getBasicBlock()));

                    if (exceptionHandler.getInternalThrowableName() == null) {
                        sb.append(" : finally");
                    } else {
                        sb.append(" : catch ").append(exceptionHandler.getInternalThrowableName());

                        if (exceptionHandler.getOtherInternalThrowableNames() != null) {
                            for (String name : exceptionHandler.getOtherInternalThrowableNames()) {
                                sb.append("\\ncatch ").append(name);
                            }
                        }
                    }

                    sb.append('\n');
                }
                break;
            case TYPE_JSR:
                writeLink(sb, id, basicBlock.getNext(), "next");
                writeLink(sb, id, basicBlock.getBranch(), "jsr");
                break;
            case TYPE_LOOP:
                if (basicBlock.getNext() != null) {
                    writeLink(sb, id, basicBlock.getNext(), "next");
                }
                break;
            case TYPE_IF_ELSE, TYPE_TERNARY_OPERATOR:
                writeLink(sb, id, basicBlock.getSub2(), "else");
                // intended fall through
            case TYPE_IF:
                writeLink(sb, id, basicBlock.getCondition(), "condition");
                writeLink(sb, id, basicBlock.getNext(), "next");
                writeLink(sb, id, basicBlock.getSub1(), "then");
                break;
            case TYPE_CONDITION_TERNARY_OPERATOR:
                writeLink(sb, id, basicBlock.getCondition(), "condition");
                // intended fall through
            case TYPE_CONDITION_OR, TYPE_CONDITION_AND:
                writeLink(sb, id, basicBlock.getSub1(), "left");
                writeLink(sb, id, basicBlock.getSub2(), "right");
                break;
        }
    }

    private static void writeLink(StringBuilder sb, String fromId, BasicBlock to, String label) {
        if (to.getFromOffset() > MAX_OFFSET) {
            return;
        }

        if (to == SWITCH_BREAK) {
            sb.append("state \"SWITCH_BREAK\" as switch_break_").append(fromId).append('\n');
            sb.append(fromId).append(" --> switch_break_").append(fromId).append(" : ").append(label).append('\n');
        } else if (to == LOOP_START) {
            sb.append("state \"LOOP_START\" as start_loop_").append(fromId).append('\n');
            sb.append(fromId).append(" --> start_loop_").append(fromId).append(" : ").append(label).append('\n');
        } else if (to == LOOP_CONTINUE) {
            sb.append("state \"LOOP_CONTINUE\" as continue_loop_").append(fromId).append('\n');
            sb.append(fromId).append(" --> continue_loop_").append(fromId).append(" : ").append(label).append('\n');
        } else if (to == LOOP_END) {
            sb.append("state \"LOOP_END\" as end_loop_").append(fromId).append('\n');
            sb.append(fromId).append(" --> end_loop_").append(fromId).append(" : ").append(label).append('\n');
        } else if (to == RETURN) {
            sb.append("state \"RETURN\" as return_").append(fromId).append('\n');
            sb.append(fromId).append(" --> return_").append(fromId).append(" : ").append(label).append('\n');
        } else if (to != END) {
            sb.append(fromId).append(" --> state_").append(to.getIndex()).append(" : ").append(label).append('\n');
        }
    }

    private static String getStateId(BasicBlock basicBlock) {
        return (basicBlock == END) || (basicBlock == LOOP_END) ? "[*]" : "state_" + basicBlock.getIndex();
    }
}
