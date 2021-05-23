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
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantNameAndType;

public class GetField extends IndexInstruction
{
	public Instruction objectref;

	public GetField(
		int opcode, int offset, int lineNumber, 
		int index, Instruction objectref)
	{
		super(opcode, offset, lineNumber, index);
		this.objectref = objectref;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if (constants == null)
			return null;
		
		ConstantFieldref cfr = constants.getConstantFieldref(this.index);
		if (cfr == null)
			return null;
		
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cfr.name_and_type_index);
		if (cnat == null)
			return null;
		
		return constants.getConstantUtf8(cnat.descriptor_index);
	}
}
