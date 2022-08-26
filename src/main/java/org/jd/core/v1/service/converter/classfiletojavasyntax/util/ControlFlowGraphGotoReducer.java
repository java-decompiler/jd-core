/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.util.StringConstants;

import java.util.Set;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_DELETED;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_GOTO;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_INFINITE_GOTO;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN;

public final class ControlFlowGraphGotoReducer {

    private ControlFlowGraphGotoReducer() {
    }

    public static void reduce(ControlFlowGraph cfg, boolean splitReturns) {
        for (BasicBlock basicBlock : cfg.getBasicBlocks()) {
            if (basicBlock.getType() == TYPE_GOTO) {
                BasicBlock successor = basicBlock.getNext();

                if (basicBlock == successor) {
                    basicBlock.getPredecessors().remove(basicBlock);
                    basicBlock.setType(TYPE_INFINITE_GOTO);
                } else if (splitReturns
                        && successor.getType() == TYPE_RETURN
                        && !StringConstants.CLASS_CONSTRUCTOR.equals(cfg.getMethod().getName())) {
                    basicBlock.setType(TYPE_RETURN);
                    basicBlock.setNext(END);
                    successor.getPredecessors().remove(basicBlock);
                } else {
                    Set<BasicBlock> successorPredecessors = successor.getPredecessors();
                    successorPredecessors.remove(basicBlock);

                    for (BasicBlock predecessor : basicBlock.getPredecessors()) {
                        predecessor.replace(basicBlock, successor);
                        successorPredecessors.add(predecessor);
                    }

                    basicBlock.setType(TYPE_DELETED);
                }
            }
        }
    }

    public static void reduce(ControlFlowGraph cfg) {
        reduce(cfg, false);
    }
}
