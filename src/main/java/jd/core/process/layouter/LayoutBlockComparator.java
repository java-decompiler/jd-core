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
package jd.core.process.layouter;

import java.util.Comparator;

import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.layout.block.LayoutBlock;

/*
 * Tri par ordre croissant, les blocs sans numéro de ligne sont places a la fin.
 */
public class LayoutBlockComparator implements java.io.Serializable, Comparator<LayoutBlock>
{
    /**
     * Comparators should be Serializable: A non-serializable Comparator can prevent an otherwise-Serializable ordered collection from being serializable.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(LayoutBlock lb1, LayoutBlock lb2) {
        if (lb1.getLastLineNumber() == Instruction.UNKNOWN_LINE_NUMBER)
        {
            if (lb2.getLastLineNumber() == Instruction.UNKNOWN_LINE_NUMBER)
            {
                // Tri par l'index pour éviter les ecarts de tri entre la
                // version Java et C++
                return lb1.getIndex() - lb2.getIndex();
            }
            // lb1 > lb2
            return 1;
        }
        if (lb2.getLastLineNumber() == Instruction.UNKNOWN_LINE_NUMBER)
        {
            // lb1 < lb2
            return -1;
        }
        return lb1.getLastLineNumber() - lb2.getLastLineNumber();
    }
}
