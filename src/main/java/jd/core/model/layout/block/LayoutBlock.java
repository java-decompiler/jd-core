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
package jd.core.model.layout.block;

import jd.core.model.layout.section.LayoutSection;

/**
 * bloc(i).lastLineNumber = bloc(i).firstLineNumber + bloc(i).minimalLineCount
 *
 * bloc(i).firstLineNumber + bloc(i).minimalLineCount <=
 *   bloc(i+1).firstLineNumber <=
 *     bloc(i).firstLineNumber + bloc(i).maximalLineCount
 */
public class LayoutBlock {

    private byte tag;

    private final int firstLineNumber;
    private final int lastLineNumber;

    private final int minimalLineCount;
    private final int maximalLineCount;
    private int preferedLineCount;

    private int lineCount;

    private int index;
    private LayoutSection section;

    public LayoutBlock(
        byte tag, int firstLineNumber, int lastLineNumber, int lineCount)
    {
        this.setTag(tag);
        this.firstLineNumber = firstLineNumber;
        this.lastLineNumber = lastLineNumber;
        this.minimalLineCount = lineCount;
        this.maximalLineCount = lineCount;
        this.setPreferedLineCount(lineCount);
        this.setLineCount(lineCount);
        this.setIndex(0);
        this.setSection(null);
    }

    public LayoutBlock(
        byte tag, int firstLineNumber, int lastLineNumber,
        int minimalLineCount, int maximalLineCount, int preferedLineCount)
    {
        this.setTag(tag);
        this.firstLineNumber = firstLineNumber;
        this.lastLineNumber = lastLineNumber;
        this.minimalLineCount = minimalLineCount;
        this.maximalLineCount = maximalLineCount;
        this.setPreferedLineCount(preferedLineCount);
        this.setLineCount(preferedLineCount);
        this.setIndex(0);
        this.setSection(null);
    }

    public int getLastLineNumber() {
        return lastLineNumber;
    }

    public int getMaximalLineCount() {
        return maximalLineCount;
    }

    public int getMinimalLineCount() {
        return minimalLineCount;
    }

    public int getFirstLineNumber() {
        return firstLineNumber;
    }

    public byte getTag() {
        return tag;
    }

    protected void setTag(byte tag) {
        this.tag = tag;
    }

    public int getLineCount() {
        return lineCount;
    }

    public int setLineCount(int lineCount) {
        this.lineCount = lineCount;
        return lineCount;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public LayoutSection getSection() {
        return section;
    }

    public void setSection(LayoutSection section) {
        this.section = section;
    }

    public int getPreferedLineCount() {
        return preferedLineCount;
    }

    public void setPreferedLineCount(int preferedLineCount) {
        this.preferedLineCount = preferedLineCount;
    }
}
