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
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;

public class IBinaryOperatorInstruction extends BinaryOperatorInstruction
{
    public IBinaryOperatorInstruction(
            int opcode, int offset, int lineNumber, int priority,
            String operator, Instruction value1, Instruction value2)
    {
        super(
            opcode, offset, lineNumber, priority,
            null, operator, value1, value2);
    }

    @Override
    public String getReturnedSignature(
            ConstantPool constants, LocalVariables localVariables)
    {
        String signature;
        if (ByteCodeUtil.isLoadIntValue(this.getValue1().getOpcode()))
        {
            signature = this.getValue2().getReturnedSignature(constants, localVariables);
            if (signature == null) {
                signature = this.getValue1().getReturnedSignature(constants, localVariables);
            }
        } else {
            signature = this.getValue1().getReturnedSignature(constants, localVariables);
            if (signature == null) {
                signature = this.getValue2().getReturnedSignature(constants, localVariables);
            }
        }
        return signature;
    }
}
