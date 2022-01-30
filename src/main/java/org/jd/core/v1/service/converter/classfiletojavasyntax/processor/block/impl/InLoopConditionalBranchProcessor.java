/*
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.processor.block.impl;

import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.block.api.BlockProcessor;

import java.util.Set;
import java.util.function.UnaryOperator;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITIONAL_BRANCH;

public class InLoopConditionalBranchProcessor implements BlockProcessor {

    @Override
    public boolean accept(BasicBlock bb) {
        return bb.getEnclosingLoop() != null && bb.getType() == TYPE_CONDITIONAL_BRANCH
                && (hasIsolatedSuccessor(bb, BasicBlock::getNext, BasicBlock::getBranch) || hasIsolatedSuccessor(bb, BasicBlock::getBranch, BasicBlock::getNext));
    }

    @Override
    public void process(BasicBlock bb) {
        bb.flip();
    }

    public boolean hasIsolatedSuccessor(BasicBlock bb, UnaryOperator<BasicBlock> leftSuccessorFunction, UnaryOperator<BasicBlock> rightSuccessorFunction) {
        BasicBlock successor = leftSuccessorFunction.apply(bb);
        Set<BasicBlock> sucessorPredecessors = successor.getPredecessors();
        return successor.getType() == TYPE_CONDITIONAL_BRANCH && sucessorPredecessors.size() >= 5
                && sucessorPredecessors.stream().filter(b -> b.getIndex() != bb.getIndex()).allMatch(b -> rightSuccessorFunction.apply(b).getIndex() == successor.getIndex());
    }
}
