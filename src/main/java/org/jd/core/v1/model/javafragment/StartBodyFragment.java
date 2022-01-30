/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javafragment;

import org.jd.core.v1.model.fragment.StartFlexibleBlockFragment;

public class StartBodyFragment extends StartFlexibleBlockFragment implements JavaFragment {
    private EndBodyFragment end;

    public StartBodyFragment(int minimalLineCount, int lineCount, int maximalLineCount, int weight, String label) {
        super(minimalLineCount, lineCount, maximalLineCount, weight, label);
    }

    public EndBodyFragment getEndBodyFragment() {
        return end;
    }

    void setEndBodyFragment(EndBodyFragment end) {
        this.end = end;
    }

    void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    @Override
    public boolean incLineCount(boolean force) {
        if (lineCount >= maximalLineCount) {
            return false;
        }
        lineCount++;
        // Update end body fragment
        if (!force && lineCount == 1 && end.getLineCount() == 0) {
            end.setLineCount(lineCount);
        }
        return true;
    }

    @Override
    public boolean decLineCount(boolean force) {
        if (lineCount <= minimalLineCount) {
            return false;
        }
        lineCount--;
        // Update end body fragment
        if (!force && lineCount == 1) {
            end.setLineCount(lineCount);
        }
        return true;
    }

    @Override
    public void accept(JavaFragmentVisitor visitor) {
        visitor.visit(this);
    }
}
