package org.jd.core.v1.service.converter.classfiletojavasyntax.util.cfg;

import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ByteCodeUtil;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphReducer;

public class CmpDepthCFGReducer extends ControlFlowGraphReducer {

    public CmpDepthCFGReducer(Method method) {
        super(method);
    }

    @Override
    protected boolean needToUpdateConditionTernaryOperator(BasicBlock basicBlock, BasicBlock nextNext) {
        return ByteCodeUtil.evalStackDepth(basicBlock) + 1 == -ByteCodeUtil.evalStackDepth(nextNext);
    }

    @Override
    protected boolean needToUpdateCondition(BasicBlock basicBlock, BasicBlock nextNext) {
        return false;
    }

    @Override
    protected void maybeEndCondition(BasicBlock condition) {
        // do not end condition
    }

    @Override
    protected boolean needToCreateIf(BasicBlock branch, BasicBlock nextNext, int maxOffset) {
        return nextNext.getFromOffset() < maxOffset && nextNext.getPredecessors().size() == 1;
    }

    @Override
    protected boolean needToCreateIfElse(BasicBlock branch, BasicBlock nextNext, BasicBlock branchNext) {
        return true;
    }
}
