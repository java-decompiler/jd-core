/*******************************************************************************
 * Copyright (C) 2022 GPLv3
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

import static jd.core.model.instruction.bytecode.instruction.Instruction.UNKNOWN_LINE_NUMBER;
import static jd.core.model.layout.block.LayoutBlockConstants.FRAGMENT_ARROW;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;

public class LambdaArrowLayoutBlock extends LayoutBlock
{
    private final ClassFile classFile;
    private final Method method;

    public LambdaArrowLayoutBlock(ClassFile classFile, Method method)
    {
        super(
            FRAGMENT_ARROW,
            UNKNOWN_LINE_NUMBER,
            UNKNOWN_LINE_NUMBER,
            0, 0, 0);

        this.classFile = classFile;
        this.method = method;
    }

    public ClassFile getClassFile() {
        return classFile;
    }

    public Method getMethod() {
        return method;
    }
}
