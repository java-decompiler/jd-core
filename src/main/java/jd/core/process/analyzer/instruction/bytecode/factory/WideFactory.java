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

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.AStore;
import jd.core.model.instruction.bytecode.instruction.IInc;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
import jd.core.model.instruction.bytecode.instruction.Ret;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;

public class WideFactory implements InstructionFactory
{
    @Override
    public int create(
            ClassFile classFile, Method method, List<Instruction> list,
            List<Instruction> listForAnalyze,
            Deque<Instruction> stack, byte[] code, int offset,
            int lineNumber, boolean[] jumps)
    {
        final int opcode = code[offset+1] & 255;
        final int index = (code[offset+2] & 255) << 8 | code[offset+3] & 255;

        if (opcode == Const.IINC)
        {
            final int count =
                (short)((code[offset+4] & 255) << 8 | code[offset+5] & 255);
            Instruction instruction = new IInc(
                opcode, offset, lineNumber, index, count);

            list.add(instruction);
            listForAnalyze.add(instruction);

            return 5;
        }
        if (opcode == Const.RET)
        {
            list.add(new Ret(opcode, offset, lineNumber, index));
        }
        else
        {
            Instruction instruction = null;
            switch (opcode)
            {
            case Const.ILOAD:
                instruction = new LoadInstruction(
                    Const.ILOAD, offset, lineNumber, index, "I");
                stack.push(instruction);
                break;
            case Const.FLOAD:
                instruction = new LoadInstruction(
                    ByteCodeConstants.LOAD, offset, lineNumber, index, "F");
                stack.push(instruction);
                break;
            case Const.ALOAD:
                instruction = new ALoad(
                    Const.ALOAD, offset, lineNumber, index);
                stack.push(instruction);
                break;
            case Const.LLOAD:
                instruction = new LoadInstruction(
                    ByteCodeConstants.LOAD, offset, lineNumber, index, "J");
                stack.push(instruction);
                break;
            case Const.DLOAD:
                instruction = new LoadInstruction(
                    ByteCodeConstants.LOAD, offset, lineNumber, index, "D");
                stack.push(instruction);
                break;
            case Const.ISTORE:
                instruction = new StoreInstruction(
                    Const.ISTORE, offset, lineNumber,
                    index, "I", stack.pop());
                list.add(instruction);
                break;
            case Const.FSTORE:
                instruction = new StoreInstruction(
                    ByteCodeConstants.STORE, offset, lineNumber,
                    index, "F", stack.pop());
                list.add(instruction);
                break;
            case Const.ASTORE:
                instruction = new AStore(
                    Const.ASTORE, offset, lineNumber,
                    index, stack.pop());
                list.add(instruction);
                break;
            case Const.LSTORE:
                instruction = new StoreInstruction(
                    ByteCodeConstants.STORE, offset, lineNumber,
                    index, "J", stack.pop());
                list.add(instruction);
                break;
            case Const.DSTORE:
                instruction = new StoreInstruction(
                    ByteCodeConstants.STORE, offset, lineNumber,
                    index, "D", stack.pop());
                list.add(instruction);
                break;
            default:
                throw new UnexpectedOpcodeException(opcode);
            }

            listForAnalyze.add(instruction);
        }

        return 2;
    }
}
