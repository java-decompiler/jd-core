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

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

public class InitArrayInstruction extends Instruction
{
    private Instruction newArray;
    private final List<Instruction> values;

    public InitArrayInstruction(
            int opcode, int offset, int lineNumber,
            Instruction newArray, List<Instruction> values)
    {
        super(opcode, offset, lineNumber);
        this.setNewArray(newArray);
        this.values = values;
    }

    @Override
    public String getReturnedSignature(
            ConstantPool constants, LocalVariables localVariables)
    {
        return this.getNewArray().getReturnedSignature(constants, localVariables);
    }

    public List<Instruction> getValues() {
        return values;
    }

    public Instruction getNewArray() {
        return newArray;
    }

    public void setNewArray(Instruction newArray) {
        this.newArray = newArray;
    }
}
