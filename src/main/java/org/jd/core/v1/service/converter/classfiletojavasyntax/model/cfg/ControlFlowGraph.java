/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg;

import org.apache.bcel.classfile.Method;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.block.api.BlockProcessor;
import org.jd.core.v1.util.DefaultList;

import java.util.HashSet;
import java.util.Set;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_JUMP;

public class ControlFlowGraph {
    private final Method method;
    private final DefaultList<BasicBlock> list = new DefaultList<>() {

        private static final long serialVersionUID = 1L;

        @Override
        public BasicBlock remove(int index) {
            throw new UnsupportedOperationException();
        }
    };
    private int[] offsetToLineNumbers;

    public ControlFlowGraph(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public DefaultList<BasicBlock> getBasicBlocks() {
        return list;
    }

    public BasicBlock getStart() {
        return list.get(0);
    }

    public BasicBlock newBasicBlock(BasicBlock original) {
        BasicBlock basicBlock = new BasicBlock(this, list.size(), original);
        list.add(basicBlock);
        return basicBlock;
    }

    public BasicBlock newBasicBlock(int fromOffset, int toOffset) {
        return newBasicBlock(0, fromOffset, toOffset);
    }

    public BasicBlock newBasicBlock(int type, int fromOffset, int toOffset) {
        BasicBlock basicBlock = new BasicBlock(this, list.size(), type, fromOffset, toOffset, true);
        list.add(basicBlock);
        return basicBlock;
    }

    public BasicBlock newBasicBlock(int type, int fromOffset, int toOffset, boolean inverseCondition) {
        BasicBlock basicBlock = new BasicBlock(this, list.size(), type, fromOffset, toOffset, inverseCondition);
        list.add(basicBlock);
        return basicBlock;
    }

    public BasicBlock newBasicBlock(int type, int fromOffset, int toOffset, Set<BasicBlock> predecessors) {
        BasicBlock basicBlock = new BasicBlock(this, list.size(), type, fromOffset, toOffset, true, predecessors);
        list.add(basicBlock);
        return basicBlock;
    }


    public BasicBlock newJumpBasicBlock(BasicBlock bb, BasicBlock target) {
        Set<BasicBlock> predecessors = new HashSet<>();

        predecessors.add(bb);
        target.getPredecessors().remove(bb);

        return newBasicBlock(TYPE_JUMP, bb.getFromOffset(), target.getFromOffset(), predecessors);
    }
    
    public void setOffsetToLineNumbers(int[] offsetToLineNumbers) {
        this.offsetToLineNumbers = offsetToLineNumbers;
    }

    public int getLineNumber(int offset) {
        return offsetToLineNumbers == null || offset < 0 ? 0 : offsetToLineNumbers[offset];
    }
    
    public void accept(BlockProcessor blockProcessor) {
        blockProcessor.process(this);
    }

    public boolean contains(int basickBlockType) {
        for (BasicBlock basicBlock : list) {
            if (basicBlock.getType() == basickBlockType) {
                return true;
            }
        }
        return false;
    }
}
