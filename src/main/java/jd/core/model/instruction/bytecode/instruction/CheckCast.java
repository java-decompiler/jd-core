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

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantUtf8;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.util.SignatureUtil;

public class CheckCast extends IndexInstruction
{
    private Instruction objectref;

    public CheckCast(
        int opcode, int offset, int lineNumber,
        int index, Instruction objectref)
    {
        super(opcode, offset, lineNumber, index);
        this.setObjectref(objectref);
    }

    @Override
    public String getReturnedSignature(
            ConstantPool constants, LocalVariables localVariables)
    {
        if (constants == null) {
            return null;
        }

        Constant c = constants.get(this.getIndex());

        if (c instanceof ConstantUtf8)
        {
            // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ConstantUtf8 cutf8 = (ConstantUtf8) c;
            return cutf8.getBytes();
        }
        ConstantClass cc = (ConstantClass)c;
        String signature = constants.getConstantUtf8(cc.getNameIndex());
        if (signature.charAt(0) != '[') {
            signature = SignatureUtil.createTypeName(signature);
        }
        return signature;
    }

    @Override
    public int getPriority()
    {
        return 2;
    }

    public Instruction getObjectref() {
        return objectref;
    }

    public void setObjectref(Instruction objectref) {
        this.objectref = objectref;
    }
}
