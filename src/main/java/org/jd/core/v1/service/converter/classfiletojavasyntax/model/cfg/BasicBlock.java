/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg;

import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ByteCodeUtil;
import org.jd.core.v1.util.DefaultList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BasicBlock {
    public static final int TYPE_DELETED                         = 0;
    public static final int TYPE_START                           = 1 << 0;
    public static final int TYPE_END                             = 1 << 1;
    public static final int TYPE_STATEMENTS                      = 1 << 2;
    public static final int TYPE_THROW                           = 1 << 3;
    public static final int TYPE_RETURN                          = 1 << 4;
    public static final int TYPE_RETURN_VALUE                    = 1 << 5;
    public static final int TYPE_SWITCH_DECLARATION              = 1 << 6;
    public static final int TYPE_SWITCH                          = 1 << 7;
    public static final int TYPE_SWITCH_BREAK                    = 1 << 8;
    public static final int TYPE_TRY_DECLARATION                 = 1 << 9;
    public static final int TYPE_TRY                             = 1 << 10;
    public static final int TYPE_TRY_JSR                         = 1 << 11;
    public static final int TYPE_TRY_ECLIPSE                     = 1 << 12;
    public static final int TYPE_JSR                             = 1 << 13;
    public static final int TYPE_RET                             = 1 << 14;
    public static final int TYPE_CONDITIONAL_BRANCH              = 1 << 15;
    public static final int TYPE_IF                              = 1 << 16;
    public static final int TYPE_IF_ELSE                         = 1 << 17;
    public static final int TYPE_CONDITION                       = 1 << 18;
    public static final int TYPE_CONDITION_OR                    = 1 << 19;
    public static final int TYPE_CONDITION_AND                   = 1 << 20;
    public static final int TYPE_CONDITION_TERNARY_OPERATOR      = 1 << 21;
    public static final int TYPE_LOOP                            = 1 << 22;
    public static final int TYPE_LOOP_START                      = 1 << 23;
    public static final int TYPE_LOOP_CONTINUE                   = 1 << 24;
    public static final int TYPE_LOOP_END                        = 1 << 25;
    public static final int TYPE_GOTO                            = 1 << 26;
    public static final int TYPE_INFINITE_GOTO                   = 1 << 27;
    public static final int TYPE_GOTO_IN_TERNARY_OPERATOR        = 1 << 28;
    public static final int TYPE_TERNARY_OPERATOR                = 1 << 29;
    public static final int TYPE_JUMP                            = 1 << 30;

    public static final int GROUP_SINGLE_SUCCESSOR  = TYPE_START|TYPE_STATEMENTS|TYPE_TRY_DECLARATION|TYPE_JSR|TYPE_LOOP|TYPE_IF|TYPE_IF_ELSE|TYPE_SWITCH|TYPE_TRY|TYPE_TRY_JSR|TYPE_TRY_ECLIPSE|TYPE_GOTO|TYPE_GOTO_IN_TERNARY_OPERATOR|TYPE_TERNARY_OPERATOR;
    public static final int GROUP_SYNTHETIC         = TYPE_START|TYPE_END|TYPE_CONDITIONAL_BRANCH|TYPE_SWITCH_DECLARATION|TYPE_TRY_DECLARATION|TYPE_RET|TYPE_GOTO|TYPE_JUMP;
    public static final int GROUP_CODE              = TYPE_STATEMENTS|TYPE_THROW|TYPE_RETURN|TYPE_RETURN_VALUE|TYPE_SWITCH_DECLARATION|TYPE_CONDITIONAL_BRANCH|TYPE_JSR|TYPE_RET|TYPE_SWITCH|TYPE_GOTO|TYPE_INFINITE_GOTO|TYPE_GOTO_IN_TERNARY_OPERATOR|TYPE_CONDITION|TYPE_CONDITION_TERNARY_OPERATOR;
    public static final int GROUP_END               = TYPE_END|TYPE_THROW|TYPE_RETURN|TYPE_RETURN_VALUE|TYPE_RET|TYPE_SWITCH_BREAK|TYPE_LOOP_START|TYPE_LOOP_CONTINUE|TYPE_LOOP_END|TYPE_INFINITE_GOTO|TYPE_JUMP;
    public static final int GROUP_CONDITION         = TYPE_CONDITION|TYPE_CONDITION_OR|TYPE_CONDITION_AND|TYPE_CONDITION_TERNARY_OPERATOR;

    protected static final String[] TYPE_NAMES = {
        "DELETED", "START", "END", "STATEMENTS", "THROW", "RETURN", "RETURN_VALUE", "SWITCH_DECLARATION", "SWITCH",
        "SWITCH_BREAK", "TRY_DECLARATION", "TRY", "TRY_JSR", "TYPE_TRY_ECLIPSE", "JSR", "RET", "CONDITIONAL_BRANCH",
        "IF", "IF_ELSE", "CONDITION", "CONDITION_OR", "CONDITION_AND", "CONDITION_TERNARY_OPERATOR", "LOOP",
        "LOOP_START", "LOOP_CONTINUE", "LOOP_END", "GOTO", "INFINITE_GOTO", "GOTO_IN_TERNARY_OP", "TERNARY_OP", "JUMP"
    };

    protected static final DefaultList<ExceptionHandler> EMPTY_EXCEPTION_HANDLERS = DefaultList.emptyList();
    protected static final DefaultList<SwitchCase> EMPTY_SWITCH_CASES = DefaultList.emptyList();

    public static final BasicBlock SWITCH_BREAK = new ImmutableBasicBlock(TYPE_SWITCH_BREAK);
    public static final BasicBlock LOOP_START = new ImmutableBasicBlock(TYPE_LOOP_START);
    public static final BasicBlock LOOP_CONTINUE = new ImmutableBasicBlock(TYPE_LOOP_CONTINUE);
    public static final BasicBlock LOOP_END = new ImmutableBasicBlock(TYPE_LOOP_END);
    public static final BasicBlock END = new ImmutableBasicBlock(TYPE_END);
    public static final BasicBlock RETURN = new ImmutableBasicBlock(TYPE_RETURN);

    private final ControlFlowGraph controlFlowGraph;

    private final int index;
    private int type;

    private int fromOffset;
    private int toOffset;

    private BasicBlock next;
    private BasicBlock branch;
    private BasicBlock condition;
    private boolean inverseCondition;
    private BasicBlock sub1;
    private BasicBlock sub2;
    private DefaultList<ExceptionHandler> exceptionHandlers = EMPTY_EXCEPTION_HANDLERS;
    private DefaultList<SwitchCase> switchCases = EMPTY_SWITCH_CASES;
    private final Set<BasicBlock> predecessors;
    private Loop enclosingLoop;

    public BasicBlock(ControlFlowGraph controlFlowGraph, int index, BasicBlock original) {
        this(controlFlowGraph, index, original, new HashSet<>());
    }

    public BasicBlock(ControlFlowGraph controlFlowGraph, int index, BasicBlock original, Set<BasicBlock> predecessors) {
        this.controlFlowGraph = controlFlowGraph;
        this.index = index;
        this.type = original.type;
        this.fromOffset = original.fromOffset;
        this.toOffset = original.toOffset;
        this.setNext(original.next);
        this.setBranch(original.branch);
        this.condition = original.condition;
        this.inverseCondition = original.inverseCondition;
        this.sub1 = original.sub1;
        this.sub2 = original.sub2;
        this.exceptionHandlers = original.exceptionHandlers;
        this.switchCases = original.switchCases;
        this.predecessors = predecessors;
    }

    public BasicBlock(ControlFlowGraph controlFlowGraph, int index, int type, int fromOffset, int toOffset, boolean inverseCondition) {
        this(controlFlowGraph, index, type, fromOffset, toOffset, inverseCondition, new HashSet<>());
    }

    public BasicBlock(ControlFlowGraph controlFlowGraph, int index, int type, int fromOffset, int toOffset, boolean inverseCondition, Set<BasicBlock> predecessors) {
        this.controlFlowGraph = controlFlowGraph;
        this.index = index;
        this.type = type;
        this.fromOffset = fromOffset;
        this.toOffset = toOffset;
        this.next = this.branch = this.condition = this.sub1 = this.sub2 = END;
        this.predecessors = predecessors;
        this.inverseCondition = inverseCondition;
    }

    public ControlFlowGraph getControlFlowGraph() {
        return controlFlowGraph;
    }

    public int getIndex() {
        return index;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getFromOffset() {
        return fromOffset;
    }

    public void setFromOffset(int fromOffset) {
        this.fromOffset = fromOffset;
    }

    public int getToOffset() {
        return toOffset;
    }

    public void setToOffset(int toOffset) {
        this.toOffset = toOffset;
    }

    public int getFirstLineNumber() {
        return controlFlowGraph.getLineNumber(this.fromOffset);
    }

    public int getLastLineNumber() {
        return controlFlowGraph.getLineNumber(this.toOffset-1);
    }

    public BasicBlock getNext() {
        return next;
    }

    public void setNext(BasicBlock next) {
        this.next = next;
    }

    public BasicBlock getBranch() {
        return branch;
    }

    public void setBranch(BasicBlock branch) {
        this.branch = branch;
    }

    public DefaultList<ExceptionHandler> getExceptionHandlers() {
        return exceptionHandlers;
    }

    public DefaultList<SwitchCase> getSwitchCases() {
        return switchCases;
    }

    public void setSwitchCases(DefaultList<SwitchCase> switchCases) {
        this.switchCases = switchCases;
    }

    public BasicBlock getCondition() {
        return condition;
    }

    public void setCondition(BasicBlock condition) {
        this.condition = condition;
    }

    public BasicBlock getSub1() {
        return sub1;
    }

    public void setSub1(BasicBlock sub1) {
        this.sub1 = sub1;
    }

    public BasicBlock getSub2() {
        return sub2;
    }

    public void setSub2(BasicBlock sub2) {
        this.sub2 = sub2;
    }

    public Set<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public boolean mustInverseCondition() {
        return inverseCondition;
    }

    public void setInverseCondition(boolean inverseCondition) {
        this.inverseCondition = inverseCondition;
    }

    public boolean contains(BasicBlock basicBlock) {
        if (next == basicBlock || branch == basicBlock) {
            return true;
        }

        for (ExceptionHandler exceptionHandler : exceptionHandlers) {
            if (exceptionHandler.getBasicBlock() == basicBlock) {
                return true;
            }
        }

        for (SwitchCase switchCase : switchCases) {
            if (switchCase.getBasicBlock() == basicBlock) {
                return true;
            }
        }

        return sub1 == basicBlock || sub2 == basicBlock;
    }

    public void replace(BasicBlock old, BasicBlock nevv) {
        if (next == old) {
            setNext(nevv);
        }

        if (branch == old) {
            setBranch(nevv);
        }

        for (ExceptionHandler exceptionHandler : exceptionHandlers) {
            exceptionHandler.replace(old, nevv);
        }

        for (SwitchCase switchCase : switchCases) {
            switchCase.replace(old, nevv);
        }

        if (sub1 == old) {
            sub1 = nevv;
        }

        if (sub2 == old) {
            sub2 = nevv;
        }

        if (predecessors.remove(old) && nevv != END) {
            predecessors.add(nevv);
        }
    }

    public void replace(Set<BasicBlock> olds, BasicBlock nevv) {
        if (olds.contains(next)) {
            setNext(nevv);
        }

        if (olds.contains(branch)) {
            setBranch(nevv);
        }

        for (ExceptionHandler exceptionHandler : exceptionHandlers) {
            exceptionHandler.replace(olds, nevv);
        }

        for (SwitchCase switchCase : switchCases) {
            switchCase.replace(olds, nevv);
        }

        if (olds.contains(sub1)) {
            sub1 = nevv;
        }

        if (olds.contains(sub2)) {
            sub2 = nevv;
        }

        predecessors.removeAll(olds);
        predecessors.add(nevv);
    }

    public void addExceptionHandler(String internalThrowableName, BasicBlock basicBlock) {
        if (exceptionHandlers == EMPTY_EXCEPTION_HANDLERS) {
            // Add a first handler
            exceptionHandlers = new DefaultList<>();
        } else {
            for (ExceptionHandler exceptionHandler : exceptionHandlers) {
                if (exceptionHandler.getBasicBlock() == basicBlock) {
                    // Found -> Add an other 'internalThrowableName'
                    exceptionHandler.addInternalThrowableName(internalThrowableName);
                    return;
                }
            }
        }
        exceptionHandlers.add(new ExceptionHandler(internalThrowableName, basicBlock));
    }

    public void inverseCondition() {
        switch (type) {
            case TYPE_CONDITION, TYPE_CONDITION_TERNARY_OPERATOR, TYPE_GOTO_IN_TERNARY_OPERATOR:
                inverseCondition ^= true;
                break;
            case TYPE_CONDITION_AND:
                type = TYPE_CONDITION_OR;
                sub1.inverseCondition();
                sub2.inverseCondition();
                break;
            case TYPE_CONDITION_OR:
                type = TYPE_CONDITION_AND;
                sub1.inverseCondition();
                sub2.inverseCondition();
                break;
            default:
                throw new IllegalStateException("Invalid condition");
        }
    }

    public void endCondition() {
        setNext(END);
        setBranch(END);
    }

    public boolean matchType(int types) {
        return (type & types) != 0;
    }

    public String getTypeName() {
        return TYPE_NAMES[type==0 ? 0 : Integer.numberOfTrailingZeros(type)+1];
    }

    public boolean isLoopExitCondition(Loop loop) {
        return loop != null && index == loop.getStart().getIndex() && branch == LOOP_END;
    }
    
    public boolean isOutsideLoop(Loop loop) {
        return loop != null && !loop.getMembers().contains(this);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("BasicBlock{index=").append(index).append(", from=").append(fromOffset).append(", to=").append(toOffset).append(", type=")
                .append(getTypeName()).append(", inverseCondition=").append(inverseCondition);

        if (!predecessors.isEmpty()) {
            s.append(", predecessors=[");

            Iterator<BasicBlock> iterator = predecessors.iterator();

            if (iterator.hasNext()) {
                s.append(iterator.next().getIndex());
                while (iterator.hasNext()) {
                    s.append(", ").append(iterator.next().getIndex());
                }
            }

            s.append("]");
        }

        return s + "}";
    }

    @Override
    public int hashCode() {
        return 378_887_654 + index;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        return index == ((BasicBlock) obj).index;
    }

    public static class ExceptionHandler {
        private final String internalThrowableName;
        private DefaultList<String> otherInternalThrowableNames;
        private BasicBlock basicBlock;

        public ExceptionHandler(String internalThrowableName, BasicBlock basicBlock) {
            this.internalThrowableName = internalThrowableName;
            this.basicBlock = basicBlock;
        }

        public String getInternalThrowableName() {
            return internalThrowableName;
        }

        public DefaultList<String> getOtherInternalThrowableNames() {
            return otherInternalThrowableNames;
        }

        public BasicBlock getBasicBlock() {
            return basicBlock;
        }

        public void setBasicBlock(BasicBlock basicBlock) {
            this.basicBlock = basicBlock;
        }

        public void addInternalThrowableName(String internalThrowableName) {
            if (otherInternalThrowableNames == null) {
                otherInternalThrowableNames = new DefaultList<>();
            }
            otherInternalThrowableNames.add(internalThrowableName);
        }

        public void replace(BasicBlock old, BasicBlock nevv) {
            if (basicBlock == old) {
                basicBlock = nevv;
            }
        }

        public void replace(Set<BasicBlock> olds, BasicBlock nevv) {
            if (olds.contains(basicBlock)) {
                basicBlock = nevv;
            }
        }

        @Override
        public String toString() {
            if (otherInternalThrowableNames == null) {
                return "BasicBlock.Handler{" + internalThrowableName + " -> " + basicBlock + "}";
            }
            return "BasicBlock.Handler{" + internalThrowableName + ", " + otherInternalThrowableNames + " -> " + basicBlock + "}";
        }
    }

    public static class SwitchCase {
        private final int value;
        private final int offset;
        private BasicBlock basicBlock;
        private final boolean defaultCase;

        public SwitchCase(BasicBlock basicBlock) {
            this(0, basicBlock, true);
        }

        public SwitchCase(int value, BasicBlock basicBlock) {
            this(value, basicBlock, false);
        }

        public SwitchCase(int value, BasicBlock basicBlock, boolean defaultCase) {
            this.value = value;
            this.offset = basicBlock.getFromOffset();
            this.basicBlock = basicBlock;
            this.defaultCase = defaultCase;
        }
        
        public int getValue() {
            return value;
        }

        public int getOffset() {
            return offset;
        }

        public BasicBlock getBasicBlock() {
            return basicBlock;
        }

        public void setBasicBlock(BasicBlock basicBlock) {
            this.basicBlock = basicBlock;
        }

        public boolean isDefaultCase() {
            return defaultCase;
        }

        public void replace(BasicBlock old, BasicBlock nevv) {
            if (basicBlock == old) {
                basicBlock = nevv;
            }
        }

        public void replace(Set<BasicBlock> olds, BasicBlock nevv) {
            if (olds.contains(basicBlock)) {
                basicBlock = nevv;
            }
        }

        @Override
        public String toString() {
            if (defaultCase) {
                return "BasicBlock.SwitchCase{default: " + basicBlock + "}";
            }
            return "BasicBlock.SwitchCase{'" + value + "': " + basicBlock + "}";
        }
    }

    protected static class ImmutableBasicBlock extends BasicBlock {
        public ImmutableBasicBlock(int type) {
            super(
                null, -1, type, 0, 0, true,
                new HashSet<BasicBlock>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean add(BasicBlock e) { return false; }
                }
            );
        }

        @Override
        public int getFirstLineNumber() { return 0; }
        @Override
        public int getLastLineNumber() { return 0; }
    }

    public BasicBlock getSinglePredecessor(int type) {
        if (predecessors.size() != 1) {
            return null;
        }
        return getFirstPredecessor(type);
    }

    public BasicBlock getFirstPredecessor(int type) {
        for (BasicBlock predecessor : predecessors) {
            if (predecessor.getType() == type) {
                return predecessor;
            }
        }
        return null;
    }

    public Loop getEnclosingLoop() {
        return enclosingLoop;
    }

    public void setEnclosingLoop(Loop enclosingLoop) {
        this.enclosingLoop = enclosingLoop;
    }

    public void flip() {
        BasicBlock tmp = next;
        setNext(branch);
        setBranch(tmp);
        ByteCodeUtil.invertLastOpCode(this);
    }
}
