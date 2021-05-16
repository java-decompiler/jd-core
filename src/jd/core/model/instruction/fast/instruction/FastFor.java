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
package jd.core.model.instruction.fast.instruction;

import java.util.List;

import jd.core.model.instruction.bytecode.instruction.Instruction;


/**
 * while, do-while & if
 */
public class FastFor extends FastTestList
{
	public Instruction init;
	public Instruction inc;
	
	public FastFor(
			int opcode, int offset, int lineNumber, int branch, 
			Instruction init, Instruction test, Instruction inc, 
			List<Instruction> instructions)
	{
		super(opcode, offset, lineNumber, branch, test, instructions);
		this.init = init;
		this.inc = inc;
	}
}
