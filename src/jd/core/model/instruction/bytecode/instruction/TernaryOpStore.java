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
 * Cree pour la construction de l'operateur ternaire.
 * Instancie par IfCmpFactory, IfFactory, IfXNullFactory & GotoFactory
 */
public class TernaryOpStore extends Instruction 
{
	public Instruction objectref;
	public int         ternaryOp2ndValueOffset;
	
	public TernaryOpStore(
			int opcode, int offset, int lineNumber, Instruction objectref, 
			int ternaryOp2ndValueOffset)
	{
		super(opcode, offset, lineNumber);
		this.objectref = objectref;
		this.ternaryOp2ndValueOffset = ternaryOp2ndValueOffset;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return this.objectref.getReturnedSignature(constants, localVariables);
	}
}
