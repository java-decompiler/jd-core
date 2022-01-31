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
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.ConstInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.process.analyzer.util.ReconstructorUtil;

/*
 * Recontruction des pre-incrementations depuis le motif :
 * DupStore( (i - 1F) )
 * ...
 * {?Store | PutField | PutStatic}( DupLoad )
 * ...
 * ???( DupLoad )
 */
public final class PreIncReconstructor
{
    private PreIncReconstructor() {
        super();
    }

    public static void reconstruct(List<Instruction> list)
    {
        int length = list.size();

        for (int dupStoreIndex=0; dupStoreIndex<length; dupStoreIndex++)
        {
            if (list.get(dupStoreIndex).getOpcode() != ByteCodeConstants.DUPSTORE) {
                continue;
            }

            // DupStore trouvé
            DupStore dupstore = (DupStore)list.get(dupStoreIndex);

            if (dupstore.getObjectref().getOpcode() != ByteCodeConstants.BINARYOP) {
                continue;
            }

            BinaryOperatorInstruction boi =
                (BinaryOperatorInstruction)dupstore.getObjectref();

            if (boi.getValue2().getOpcode() != ByteCodeConstants.ICONST &&
                boi.getValue2().getOpcode() != ByteCodeConstants.LCONST &&
                boi.getValue2().getOpcode() != ByteCodeConstants.DCONST &&
                boi.getValue2().getOpcode() != ByteCodeConstants.FCONST) {
                continue;
            }

            ConstInstruction ci = (ConstInstruction)boi.getValue2();

            if (ci.getValue() != 1) {
                continue;
            }

            int value;

            if ("+".equals(boi.getOperator())) {
                value = 1;
            } else if ("-".equals(boi.getOperator())) {
                value = -1;
            } else {
                continue;
            }

            int xstorePutfieldPutstaticIndex = dupStoreIndex;

            while (++xstorePutfieldPutstaticIndex < length)
            {
                Instruction i = list.get(xstorePutfieldPutstaticIndex);
                Instruction dupload = null;
                switch (i.getOpcode())
                {
                case Const.ASTORE:
                    if (boi.getValue1().getOpcode() == Const.ALOAD &&
                        ((StoreInstruction)i).getValueref().getOpcode() == ByteCodeConstants.DUPLOAD &&
                        ((IndexInstruction)i).getIndex() == ((IndexInstruction)boi.getValue1()).getIndex()) {
                        // 1er DupLoad trouvé
                        dupload = ((StoreInstruction)i).getValueref();
                    }
                    break;
                case Const.ISTORE:
                    if (boi.getValue1().getOpcode() == Const.ILOAD &&
                        ((StoreInstruction)i).getValueref().getOpcode() == ByteCodeConstants.DUPLOAD &&
                        ((IndexInstruction)i).getIndex() == ((IndexInstruction)boi.getValue1()).getIndex()) {
                        // 1er DupLoad trouvé
                        dupload = ((StoreInstruction)i).getValueref();
                    }
                    break;
                case ByteCodeConstants.STORE:
                    if (boi.getValue1().getOpcode() == ByteCodeConstants.LOAD &&
                        ((StoreInstruction)i).getValueref().getOpcode() == ByteCodeConstants.DUPLOAD &&
                        ((IndexInstruction)i).getIndex() == ((IndexInstruction)boi.getValue1()).getIndex()) {
                        // 1er DupLoad trouvé
                        dupload = ((StoreInstruction)i).getValueref();
                    }
                    break;
                case Const.PUTFIELD:
                    if (boi.getValue1().getOpcode() == Const.GETFIELD &&
                        ((PutField)i).getValueref().getOpcode() == ByteCodeConstants.DUPLOAD &&
                        ((IndexInstruction)i).getIndex() == ((IndexInstruction)boi.getValue1()).getIndex()) {
                        // 1er DupLoad trouvé
                        dupload = ((PutField)i).getValueref();
                    }
                    break;
                case Const.PUTSTATIC:
                    if (boi.getValue1().getOpcode() == Const.GETSTATIC &&
                        ((PutStatic)i).getValueref().getOpcode() == ByteCodeConstants.DUPLOAD &&
                        ((IndexInstruction)i).getIndex() == ((IndexInstruction)boi.getValue1()).getIndex()) {
                        // 1er DupLoad trouvé
                        dupload = ((PutStatic)i).getValueref();
                    }
                    break;
                }

                if (dupload == null || dupload.getOffset() != dupstore.getOffset()) {
                    continue;
                }

                Instruction preinc = new IncInstruction(
                    ByteCodeConstants.PREINC, boi.getOffset(),
                    boi.getLineNumber(), boi.getValue1(), value);

                ReconstructorUtil.replaceDupLoad(
                        list, xstorePutfieldPutstaticIndex+1, dupstore, preinc);

                list.remove(xstorePutfieldPutstaticIndex);
                list.remove(dupStoreIndex);
                dupStoreIndex--;
                length = list.size();
                break;
            }
        }
    }
}
