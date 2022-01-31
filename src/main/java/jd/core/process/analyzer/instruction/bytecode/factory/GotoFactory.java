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
package jd.core.process.analyzer.instruction.bytecode.factory;

import org.apache.bcel.Const;

import java.util.Deque;
import java.util.List;

import static jd.core.model.instruction.bytecode.ByteCodeConstants.ARRAYLOAD;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.ASSIGNMENT;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.DCONST;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.DUPLOAD;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.EXCEPTIONLOAD;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.ICONST;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.INVOKENEW;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.LOAD;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.OUTERTHIS;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.POSTINC;
import static org.apache.bcel.Const.ACONST_NULL;
import static org.apache.bcel.Const.ARRAYLENGTH;
import static org.apache.bcel.Const.CHECKCAST;
import static org.apache.bcel.Const.DCMPG;
import static org.apache.bcel.Const.DUP;
import static org.apache.bcel.Const.GETFIELD;
import static org.apache.bcel.Const.GETSTATIC;
import static org.apache.bcel.Const.INSTANCEOF;
import static org.apache.bcel.Const.INVOKEINTERFACE;
import static org.apache.bcel.Const.INVOKEVIRTUAL;
import static org.apache.bcel.Const.MULTIANEWARRAY;
import static org.apache.bcel.Const.NEW;
import static org.apache.bcel.Const.SALOAD;
import static org.apache.bcel.Const.WIDE;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;

public class GotoFactory implements InstructionFactory
{
    @Override
    public int create(
            ClassFile classFile, Method method, List<Instruction> list,
            List<Instruction> listForAnalyze,
            Deque<Instruction> stack, byte[] code, int offset,
            int lineNumber, boolean[] jumps)
    {
        final int opcode = code[offset] & 255;
        final int value  =
            (short)((code[offset+1] & 255) << 8 | code[offset+2] & 255);

        if (!stack.isEmpty() && !list.isEmpty()) {
            generateTernaryOpStore(
                list, listForAnalyze, stack, code, offset, value);
        }

        list.add(new Goto(opcode, offset, lineNumber, value));

        return Const.getNoOfOperands(opcode);
    }

    private static void generateTernaryOpStore(
        List<Instruction> list, List<Instruction> listForAnalyze,
        Deque<Instruction> stack, byte[] code, int offset, int value)
    {
        int i = list.size();

        while (i-- > 0)
        {
            Instruction previousInstruction = list.get(i);

            if (ByteCodeUtil.isIfInstruction(previousInstruction.getOpcode(), false))
            {
                // Gestion de l'operateur ternaire
                final int ternaryOp2ndValueOffset =
                    search2ndValueOffset(code, offset, offset+value);

                final Instruction value0 = stack.pop();
                TernaryOpStore tos = new TernaryOpStore(
                    ByteCodeConstants.TERNARYOPSTORE, offset-1,
                    value0.getLineNumber(), value0, ternaryOp2ndValueOffset);

                list.add(tos);
                listForAnalyze.add(tos);
                return;
            }
        }
    }

    private static int search2ndValueOffset(
            byte[] code, int offset, int jumpOffset)
    {
        int result = offset;

        while (offset < jumpOffset)
        {
            int opcode = code[offset] & 255;
            // on retient l'offset de la derniere opération placant une
            // information sur la pile.
            if (opcode >= ACONST_NULL   && opcode <= SALOAD          // 1..53
             || opcode >= DUP           && opcode <= DCMPG           // 89..152
             || opcode == GETSTATIC                                  // 178
             || opcode == GETFIELD                                   // 180
             || opcode >= INVOKEVIRTUAL && opcode <= INVOKEINTERFACE // 182..185
             || opcode >= NEW           && opcode <= ARRAYLENGTH     // 187..190
             || opcode >= CHECKCAST     && opcode <= INSTANCEOF      // 192..193
             || opcode >= WIDE          && opcode <= MULTIANEWARRAY  // 196..197
             || opcode >= ICONST        && opcode <= DCONST          // 256..259
             || opcode == DUPLOAD                                    // 263
             || opcode >= ASSIGNMENT    && opcode <= LOAD            // 265..268
             || opcode >= EXCEPTIONLOAD && opcode <= ARRAYLOAD       // 270..271
             || opcode >= INVOKENEW     && opcode <= POSTINC         // 274..278
             || opcode == OUTERTHIS                                  // 285
            )
            {
                result = offset;
            }

            int nbOfOperands = Const.getNoOfOperands(opcode);

            switch (nbOfOperands)
            {
            case Const.UNPREDICTABLE:
                switch (opcode)
                {
                case Const.TABLESWITCH:
                    offset = ByteCodeUtil.nextTableSwitchOffset(code, offset);
                    break;
                case Const.LOOKUPSWITCH:
                    offset = ByteCodeUtil.nextLookupSwitchOffset(code, offset);
                    break;
                case Const.WIDE:
                    offset = ByteCodeUtil.nextWideOffset(code, offset);
                }
                break;
            case Const.UNDEFINED:
                break;
            default:
                offset += nbOfOperands;
            }

            ++offset;
        }

        return result;
    }
}
