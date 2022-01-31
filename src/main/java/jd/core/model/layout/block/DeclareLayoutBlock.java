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

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class DeclareLayoutBlock extends LayoutBlock
{
    private final ClassFile classFile;
    private final Method method;
    private final Instruction instruction;

    public DeclareLayoutBlock(
        ClassFile classFile, Method method, Instruction instruction)
    {
        super(
            LayoutBlockConstants.DECLARE,
            Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER,
            0, 0, 0);
        this.classFile = classFile;
        this.method = method;
        this.instruction = instruction;
    }

    public ClassFile getClassFile() {
        return classFile;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public Method getMethod() {
        return method;
    }
}
