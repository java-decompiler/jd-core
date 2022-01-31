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

import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;

public class IfCmp extends ConditionalBranchInstruction
{
    private Instruction value1;
    private Instruction value2;

    public IfCmp(
        int opcode, int offset, int lineNumber, int cmp,
        Instruction value1, Instruction value2, int branch)
    {
        super(opcode, offset, lineNumber, cmp, branch);
        this.setValue1(value1);
        this.setValue2(value2);
    }

    @Override
    public int getPriority()
    {
        return ByteCodeUtil.getCmpPriority(this.getCmp());
    }

    public Instruction getValue1() {
        return value1;
    }

    public void setValue1(Instruction value1) {
        this.value1 = value1;
    }

    public Instruction getValue2() {
        return value2;
    }

    public void setValue2(Instruction value2) {
        this.value2 = value2;
    }
}
