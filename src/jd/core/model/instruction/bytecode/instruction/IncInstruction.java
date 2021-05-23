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

public class IncInstruction extends Instruction 
{
	public Instruction value;
	public int count;
	
	public IncInstruction(
		int opcode, int offset, int lineNumber, Instruction value, int count)
	{
		super(opcode, offset, lineNumber);
		this.value = value;
		this.count = count;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		return (this.value == null) ? null : 
			this.value.getReturnedSignature(constants, localVariables);
	}

	public int getPriority()
	{
		if ((this.count == 1) || (this.count == -1))
			// Operator '++' or '--'
			return 2;
		// Operator '+=' or '-='				
		return 14;
	}
}
