/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;

import java.util.HashSet;
import java.util.Set;

public class WatchDog {
    protected final Set<Link> links = new HashSet<>();

    public void clear() {
        links.clear();
    }

    public void check(BasicBlock parent, BasicBlock child) {
        if (child != null && !child.matchType(BasicBlock.GROUP_END)) {
            Link link = new Link(parent, child);

            if (links.contains(link)) {
                throw new IllegalStateException("CFG watchdog: parent=" + parent + ", child=" + child);
            }

            links.add(link);
        }
    }

    protected static class Link {
        private final int parentIndex;
        private final int childIndex;

        public Link(BasicBlock parent, BasicBlock child) {
            this.parentIndex = parent.getIndex();
            this.childIndex = child.getIndex();
        }

        @Override
        public int hashCode() {
            return 4_807_589 + parentIndex + 31 * childIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || o.getClass() != getClass()) {
                return false;
            }
            Link other = (Link) o;
            return parentIndex == other.parentIndex && childIndex == other.childIndex;
        }
    }
}
