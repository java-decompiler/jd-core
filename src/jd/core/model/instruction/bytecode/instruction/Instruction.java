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

public abstract class Instruction 
{
	public static int UNKNOWN_LINE_NUMBER = 0;
	
	public int opcode;
	public int offset;
	public int lineNumber;

	public Instruction(int opcode, int offset, int lineNumber)
	{
		this.opcode = opcode;
		this.offset = offset;
		this.lineNumber = lineNumber;
	}

	public abstract String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables);
	
	public int getPriority()
	{
		return 0;
	}
}
