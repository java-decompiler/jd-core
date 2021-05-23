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
import jd.core.model.instruction.bytecode.ByteCodeConstants;

public class DupStore extends Instruction 
{
	public Instruction objectref;
	public DupLoad dupLoad1;
	public DupLoad dupLoad2;
	
	public DupStore(
		int opcode, int offset, int lineNumber, Instruction objectref)
	{
		super(opcode, offset, lineNumber);
		this.objectref = objectref;
		this.dupLoad1 = new DupLoad(
				ByteCodeConstants.DUPLOAD, offset, lineNumber, this);
		this.dupLoad2 = new DupLoad(
				ByteCodeConstants.DUPLOAD, offset, lineNumber, this);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		return this.objectref.getReturnedSignature(constants, localVariables);
	}

	public DupLoad getDupLoad1() 
	{
		return dupLoad1;
	}

	public DupLoad getDupLoad2() 
	{
		return dupLoad2;
	}
}
