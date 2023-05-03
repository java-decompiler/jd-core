/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Loop {
    private BasicBlock start;
    private final Set<BasicBlock> members;
    private BasicBlock end;

    public Loop(BasicBlock start, Set<BasicBlock> members, BasicBlock end) {
        this.start = start;
        this.members = members;
        this.end = end;
    }

    public BasicBlock getStart() {
        return start;
    }

    public void setStart(BasicBlock start) {
        this.start = start;
    }

    public Set<BasicBlock> getMembers() {
        return members;
    }

    public int getMemberCount() {
        return members.size();
    }

    public BasicBlock getEnd() {
        return end;
    }

    public void setEnd(BasicBlock end) {
        this.end = end;
    }

    public void updateEnclosingLoop() {
        members.forEach(this::updateEnclosingLoop);
    }

    private void updateEnclosingLoop(BasicBlock member) {
        member.setEnclosingLoop(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Loop loop = (Loop) o;

        return Objects.equals(start, loop.start) 
            && Objects.equals(members, loop.members)
            && Objects.equals(end, loop.end);
    }

    @Override
    public int hashCode() {
        int result = 258_190_310 + start.hashCode();
        result = 31 * result + members.hashCode();
        return 31 * result + Objects.hash(end);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Loop{start=").append(start.getIndex()).append(", members=[");

        if (members != null && !members.isEmpty()) {
            Iterator<BasicBlock> iterator = members.iterator();
            str.append(iterator.next().getIndex());

            while (iterator.hasNext()) {
                str.append(", ").append(iterator.next().getIndex());
            }
        }

        return str + "], end=" + Optional.ofNullable(end).map(BasicBlock::getIndex).orElse(-1) + "}";
    }
}
