/*
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.processor.block.impl;

import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.block.api.BlockProcessor;

import java.util.Objects;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_STATEMENTS;

public class MergeStatementBlockProcessor implements BlockProcessor {

    @Override
    public boolean accept(BasicBlock bb) {
        return bb.getType() == TYPE_STATEMENTS 
            && bb.getNext().getType() == TYPE_STATEMENTS 
            && bb.getNext().getFromOffset() == bb.getToOffset()
            && Objects.equals(bb.getNext().getSinglePredecessor(TYPE_STATEMENTS), bb);
    }

    @Override
    public void process(BasicBlock bb) {
        bb.setToOffset(bb.getNext().getToOffset());
        bb.setNext(bb.getNext().getNext());
    }

}
