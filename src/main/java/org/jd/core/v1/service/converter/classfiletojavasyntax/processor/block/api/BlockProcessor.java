/*
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.processor.block.api;

import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;

public interface BlockProcessor {
    
    boolean accept(BasicBlock bb);

    void process(BasicBlock bb);

    default void process(ControlFlowGraph cfg) {
        cfg.getBasicBlocks().stream().filter(this::accept).forEach(this::process);
    }
}
