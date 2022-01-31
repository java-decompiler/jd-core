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
package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

public class AssertInstruction extends Instruction
{
    private Instruction test;
    private Instruction msg;

    public AssertInstruction(
        int opcode, int offset, int lineNumber,
        Instruction test, Instruction msg)
    {
        super(opcode, offset, lineNumber);
        this.setTest(test);
        this.setMsg(msg);
    }

    @Override
    public String getReturnedSignature(
            ConstantPool constants, LocalVariables localVariables)
    {
        return null;
    }

    public Instruction getTest() {
        return test;
    }

    public void setTest(Instruction test) {
        this.test = test;
    }

    public Instruction getMsg() {
        return msg;
    }

    public void setMsg(Instruction msg) {
        this.msg = msg;
    }
}
