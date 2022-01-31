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

/*
 * La construction de cette instruction ne suit pas les règles générales !
 * C'est la seule exception. Cette instruction, appartement au package
 * 'bytecode', ne peut être construite qu'après avoir aggloméré les instructions
 * 'if'. Cette instruction est affichée par une classe du package 'bytecode' et
 * est construite par une classe du package 'fast'.
 */
public class TernaryOperator extends Instruction
{
    private Instruction test;
    private Instruction value1;
    private Instruction value2;

    public TernaryOperator(
            int opcode, int offset, int lineNumber,
            Instruction test, Instruction value1, Instruction value2)
    {
        super(opcode, offset, lineNumber);
        this.setTest(test);
        this.setValue1(value1);
        this.setValue2(value2);
    }

    @Override
    public String getReturnedSignature(
            ConstantPool constants, LocalVariables localVariables)
    {
        if (this.getValue1() != null) {
            return this.getValue1().getReturnedSignature(constants, localVariables);
        }
        return this.getValue2().getReturnedSignature(constants, localVariables);
    }

    @Override
    public int getPriority()
    {
        return 13;
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

    public Instruction getTest() {
        return test;
    }

    public void setTest(Instruction test) {
        this.test = test;
    }
}
