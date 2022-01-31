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
package jd.core.process.analyzer.classfile.reconstructor;

import org.apache.bcel.Const;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.process.analyzer.classfile.visitor.ReplaceDupLoadVisitor;

/*
 * Elimine la séquence suivante:
 * DupStore( ALoad(0) )
 * ...
 * ???( DupLoad )
 * ...
 * ???( DupLoad )
 */
public final class DupStoreThisReconstructor
{
    private DupStoreThisReconstructor() {
        super();
    }

    public static void reconstruct(List<Instruction> list)
    {
        for (int dupStoreIndex=0; dupStoreIndex<list.size(); dupStoreIndex++)
        {
            if (list.get(dupStoreIndex).getOpcode() != ByteCodeConstants.DUPSTORE) {
                continue;
            }

            // DupStore trouvé
            DupStore dupStore = (DupStore)list.get(dupStoreIndex);

            if (dupStore.getObjectref().getOpcode() != Const.ALOAD ||
                ((ALoad)dupStore.getObjectref()).getIndex() != 0) {
                continue;
            }

            // Fait-il parti d'un motif 'synchronized' ?
            if (dupStoreIndex+2 < list.size())
            {
                Instruction instruction = list.get(dupStoreIndex+2);
                if (instruction.getOpcode() == Const.MONITORENTER)
                {
                    MonitorEnter me = (MonitorEnter)instruction;
                    if (me.getObjectref().getOpcode() == ByteCodeConstants.DUPLOAD &&
                        ((DupLoad)me.getObjectref()).getDupStore() == dupStore)
                    {
                        // On passe
                        continue;
                    }
                }
            }

            ReplaceDupLoadVisitor visitor =
                new ReplaceDupLoadVisitor(dupStore, dupStore.getObjectref());

            int length = list.size();
            int index = dupStoreIndex+1;

            for (; index<length; index++)
            {
                visitor.visit(list.get(index));
                if (visitor.getParentFound() != null) {
                    break;
                }
            }

            visitor.init(dupStore, dupStore.getObjectref());

            for (; index<length; index++)
            {
                visitor.visit(list.get(index));
                if (visitor.getParentFound() != null) {
                    break;
                }
            }

            list.remove(dupStoreIndex--);
        }
    }
}
