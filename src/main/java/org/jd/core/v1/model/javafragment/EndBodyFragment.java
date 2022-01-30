/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javafragment;

import org.jd.core.v1.model.fragment.EndFlexibleBlockFragment;

public class EndBodyFragment extends EndFlexibleBlockFragment implements JavaFragment {
    private final StartBodyFragment start;

    public EndBodyFragment(int minimalLineCount, int lineCount, int maximalLineCount, int weight, String label, StartBodyFragment start) {
        super(minimalLineCount, lineCount, maximalLineCount, weight, label);
        this.start = start;
        start.setEndBodyFragment(this);
    }

    void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    public StartBodyFragment getStartBodyFragment() {
        return start;
    }

    @Override
    public boolean incLineCount(boolean force) {
        if (lineCount >= maximalLineCount) {
            return false;
        }
        lineCount++;
        // Update start body fragment
        if (!force && lineCount == 1 && start.getLineCount() == 0) {
            start.setLineCount(lineCount);
        }
        return true;
    }

    @Override
    public boolean decLineCount(boolean force) {
        if (lineCount <= minimalLineCount) {
            return false;
        }
        lineCount--;
        if (!force && lineCount == 0) {
            start.setLineCount(lineCount);
        }
        return true;
    }

    @Override
    public void accept(JavaFragmentVisitor visitor) {
        visitor.visit(this);
    }
}
