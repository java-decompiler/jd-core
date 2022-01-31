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

public class MethodNameLayoutBlock extends LayoutBlock
{
    private final ClassFile classFile;
    private final Method method;
    private final String signature;
    private final boolean descriptorFlag;
    private final boolean nullCodeFlag;

    public MethodNameLayoutBlock(
        ClassFile classFile, Method method, String signature, 
        boolean descriptorFlag, boolean nullCodeFlag)
    {
        super(
            LayoutBlockConstants.METHOD_NAME,
            Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER,
            0, 0, 0);
        this.classFile = classFile;
        this.method = method;
        this.signature = signature;
        this.descriptorFlag = descriptorFlag;
        this.nullCodeFlag = nullCodeFlag;
    }

    public ClassFile getClassFile() {
        return classFile;
    }

    public Method getMethod() {
        return method;
    }

    public boolean hasNullCodeFlag() {
        return nullCodeFlag;
    }

    public String getSignature() {
        return signature;
    }

    public boolean hasDescriptorFlag() {
        return descriptorFlag;
    }

    public boolean isLambda() {
        return false;
    }
}
