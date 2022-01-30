/*
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.block.api.BlockProcessor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.block.impl.InLoopConditionalBranchProcessor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.block.impl.MergeStatementBlockProcessor;

import java.util.ArrayList;
import java.util.List;

public final class ControlFlowGraphPreReducer {

    private static List<BlockProcessor> blockProcessors = new ArrayList<>();
    static {
        blockProcessors.add(new MergeStatementBlockProcessor());
        blockProcessors.add(new InLoopConditionalBranchProcessor());
    }
    
    private ControlFlowGraphPreReducer() {
        super();
    }

    public static void reduce(ControlFlowGraph cfg) {
        blockProcessors.stream().forEach(cfg::accept);
    }
}
