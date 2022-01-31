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
package jd.core.process.analyzer.instruction.fast.reconstructor;

import org.apache.bcel.Const;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.process.analyzer.classfile.visitor.ReplaceDupLoadVisitor;

/*
 * Retrait des instructions DupLoads & DupStore associés à  une constante ou un
 * attribut:
 * DupStore( GetField | GetStatic | BIPush | SIPush | ALoad )
 * ...
 * ???( DupLoad )
 * ...
 * ???( DupLoad )
 */
public final class RemoveDupConstantsAttributes
{
    private RemoveDupConstantsAttributes() {
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
            DupStore dupstore = (DupStore)list.get(dupStoreIndex);

            int opcode = dupstore.getObjectref().getOpcode();

            if (/*(opcode != Const.GETFIELD) &&
                (opcode != Const.GETSTATIC) &&*/
                opcode != Const.BIPUSH &&
                opcode != Const.SIPUSH /*&&
                (opcode != Const.ALOAD) &&
                (opcode != Const.ILOAD)*/) {
                continue;
            }

            Instruction i = dupstore.getObjectref();
            int dupLoadIndex = dupStoreIndex+1;
            ReplaceDupLoadVisitor visitor =
                new ReplaceDupLoadVisitor(dupstore, i);
            final int length = list.size();

            // 1er substitution
            while (dupLoadIndex < length)
            {
                visitor.visit(list.get(dupLoadIndex));
                if (visitor.getParentFound() != null) {
                    break;
                }
                dupLoadIndex++;
            }

            visitor.init(dupstore, i);

            // 2eme substitution
            while (dupLoadIndex < length)
            {
                visitor.visit(list.get(dupLoadIndex));
                if (visitor.getParentFound() != null) {
                    break;
                }
                dupLoadIndex++;
            }

            list.remove(dupStoreIndex--);
        }
    }
}
