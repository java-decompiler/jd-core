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

import jd.core.model.instruction.bytecode.instruction.attribute.ValuerefAttribute;


public class PutStatic extends GetStatic implements ValuerefAttribute
{
	public Instruction valueref;

	public PutStatic(
		int opcode, int offset, int lineNumber, int index, Instruction valueref)
	{
		super(opcode, offset, lineNumber, index);
		this.valueref = valueref;
	}

	public Instruction getValueref() 
	{
		return valueref;
	}

	public void setValueref(Instruction valueref) 
	{
		this.valueref = valueref;
	}
}
