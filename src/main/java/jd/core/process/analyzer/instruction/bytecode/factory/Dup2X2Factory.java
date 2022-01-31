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
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class Dup2X2Factory implements InstructionFactory
{
    @Override
    public int create(
            ClassFile classFile, Method method, List<Instruction> list,
            List<Instruction> listForAnalyze,
            Deque<Instruction> stack, byte[] code, int offset,
            int lineNumber, boolean[] jumps)
    {
        final int opcode = code[offset] & 255;
        Instruction i1 = stack.pop();
        Instruction i2 = stack.pop();

        String signature1 = i1.getReturnedSignature(
                classFile.getConstantPool(), method.getLocalVariables());
        String signature2 = i2.getReturnedSignature(
                classFile.getConstantPool(), method.getLocalVariables());

        if ("J".equals(signature1) || "D".equals(signature1))
        {
            if ("J".equals(signature2) || "D".equals(signature2))
            {
                // ..., value2, value1 => ..., value1, value2, value1
                DupStore dupStore1 = new DupStore(
                        ByteCodeConstants.DUPSTORE, offset, lineNumber, i1);

                list.add(dupStore1);
                stack.push(dupStore1.getDupLoad1());
                stack.push(i2);
                stack.push(dupStore1.getDupLoad2());
            }
            else
            {
                // ..., value3, value2, value1 => ..., value1, value3, value2, value1
                Instruction i3 = stack.pop();

                DupStore dupStore1 = new DupStore(
                        ByteCodeConstants.DUPSTORE, offset, lineNumber, i1);

                list.add(dupStore1);
                stack.push(dupStore1.getDupLoad1());
                stack.push(i3);
                stack.push(i2);
                stack.push(dupStore1.getDupLoad2());
            }
        }
        else
        {
            Instruction i3 = stack.pop();

            String signature3 = i3.getReturnedSignature(
                    classFile.getConstantPool(), method.getLocalVariables());

            if ("J".equals(signature3) || "D".equals(signature3))
            {
                // ..., value3, value2, value1 => ..., value2, value1, value3, value2, value1
                DupStore dupStore1 = new DupStore(
                    ByteCodeConstants.DUPSTORE, offset, lineNumber, i1);
                DupStore dupStore2 = new DupStore(
                    ByteCodeConstants.DUPSTORE, offset, lineNumber, i2);

                list.add(dupStore1);
                list.add(dupStore2);

                stack.push(dupStore2.getDupLoad1());
                stack.push(dupStore1.getDupLoad1());
                stack.push(i3);
                stack.push(dupStore2.getDupLoad2());
                stack.push(dupStore1.getDupLoad2());
            }
            else
            {
                // ..., value4, value3, value2, value1 => ..., value2, value1, value4, value3, value2, value1
                Instruction i4 = stack.pop();

                DupStore dupStore1 = new DupStore(
                    ByteCodeConstants.DUPSTORE, offset, lineNumber, i1);
                DupStore dupStore2 = new DupStore(
                    ByteCodeConstants.DUPSTORE, offset, lineNumber, i2);

                list.add(dupStore1);
                list.add(dupStore2);

                stack.push(dupStore2.getDupLoad1());
                stack.push(dupStore1.getDupLoad1());
                stack.push(i4);
                stack.push(i3);
                stack.push(dupStore2.getDupLoad2());
                stack.push(dupStore1.getDupLoad2());
            }
        }

        return Const.getNoOfOperands(opcode);
    }
}
