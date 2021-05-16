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
 * La construction de cette instruction ne suit pas les r�gles g�n�rales !
 * C'est la seule exception. Cette instruction, appartement au package 
 * 'bytecode', ne peut etre construite qu'apres avoir aglom�r�e les instructions
 * 'if'. Cette instruction est affich�e par une classe du package 'bytecode' et 
 * est construite par une classe du package 'fast'. 
 */
public class TernaryOperator extends Instruction
{
	public Instruction test;
	public Instruction value1;
	public Instruction value2;
	
	public TernaryOperator(
			int opcode, int offset, int lineNumber, 
			Instruction test, Instruction value1, Instruction value2)
	{
		super(opcode, offset, lineNumber);
		this.test = test;
		this.value1 = value1;
		this.value2 = value2;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		if (this.value1 != null)
			return this.value1.getReturnedSignature(constants, localVariables);
		else
			return this.value2.getReturnedSignature(constants, localVariables);
	}

	public int getPriority()
	{
		return 13;
	}
}
