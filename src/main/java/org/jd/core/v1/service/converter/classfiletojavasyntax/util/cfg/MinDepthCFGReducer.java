package org.jd.core.v1.service.converter.classfiletojavasyntax.util.cfg;

import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ByteCodeUtil;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphReducer;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.*;

public class MinDepthCFGReducer extends ControlFlowGraphReducer {
    
    public MinDepthCFGReducer(Method method) {
        super(method);
    }

    @Override
    protected boolean needToUpdateConditionTernaryOperator(BasicBlock basicBlock, BasicBlock nextNext) {
        return ByteCodeUtil.getMinDepth(nextNext) == -1;
    }

    @Override
    protected boolean needToUpdateCondition(BasicBlock basicBlock, BasicBlock nextNext) {
        BasicBlock nextNextNext = nextNext.getNext();
        BasicBlock nextNextBranch = nextNext.getBranch();
        return nextNextNext.getType() == TYPE_GOTO_IN_TERNARY_OPERATOR
                && nextNextNext.getPredecessors().size() == 1
                && nextNextNext.getNext().matchType(TYPE_CONDITIONAL_BRANCH | TYPE_CONDITION)
                && nextNextBranch.matchType(TYPE_STATEMENTS | TYPE_GOTO_IN_TERNARY_OPERATOR)
                && nextNextNext.getNext() == nextNextBranch.getNext()
                && nextNextBranch.getPredecessors().size() == 1
                && nextNextNext.getNext().getPredecessors().size() == 2
                && ByteCodeUtil.getMinDepth(nextNextNext.getNext()) == -2;
    }

    @Override
    protected void maybeEndCondition(BasicBlock condition) {
        condition.setNext(END);
        condition.setBranch(END);
    }

    @Override
    protected boolean needToCreateIf(BasicBlock branch, BasicBlock nextNext, int maxOffset) {
        return nextNext.getFromOffset() < branch.getFromOffset() && nextNext.getPredecessors().size() == 1;
    }

    @Override
    protected boolean needToCreateIfElse(BasicBlock branch, BasicBlock nextNext, BasicBlock branchNext) {
        return nextNext.getFromOffset() > branch.getFromOffset() && branchNext.matchType(GROUP_END);
    }
}
