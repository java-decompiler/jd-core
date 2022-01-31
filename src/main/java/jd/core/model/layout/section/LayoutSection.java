/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.model.layout.section;

public class LayoutSection implements Comparable<LayoutSection>
{
    private final int index;
    private int firstBlockIndex;
    private int lastBlockIndex;

    private final int originalLineCount;

    private boolean relayout;
    private int score;
    private final boolean containsError;

    public LayoutSection(
        int index,
        int firstBlockIndex, int lastBlockIndex,
        int firstLineNumber, int lastLineNumber,
        boolean containsError)
    {
        this.index = index;
        this.setFirstBlockIndex(firstBlockIndex);
        this.setLastBlockIndex(lastBlockIndex);
        this.originalLineCount = lastLineNumber - firstLineNumber;
        this.setRelayout(true);
        this.setScore(0);
        this.containsError = containsError;
    }

    @Override
    public int compareTo(LayoutSection o)
    {
        return o.getScore() - this.getScore();
    }

    @Override
    public int hashCode() {
        return score;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return compareTo((LayoutSection) obj) == 0;
    }

    public int getOriginalLineCount() {
        return originalLineCount;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isRelayout() {
        return relayout;
    }

    public void setRelayout(boolean relayout) {
        this.relayout = relayout;
    }

    public int getFirstBlockIndex() {
        return firstBlockIndex;
    }

    public void setFirstBlockIndex(int firstBlockIndex) {
        this.firstBlockIndex = firstBlockIndex;
    }

    public int getLastBlockIndex() {
        return lastBlockIndex;
    }

    public void setLastBlockIndex(int lastBlockIndex) {
        this.lastBlockIndex = lastBlockIndex;
    }

    public boolean containsError() {
        return containsError;
    }

    public int getIndex() {
        return index;
    }
}
