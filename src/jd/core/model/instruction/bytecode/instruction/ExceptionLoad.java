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
import jd.core.util.UtilConstants;

public class ExceptionLoad extends IndexInstruction 
{
	final public int exceptionNameIndex;
	
	public ExceptionLoad(
		int opcode, int offset, int lineNumber, int signatureIndex)
	{
		super(opcode, offset, lineNumber, UtilConstants.INVALID_INDEX);
		this.exceptionNameIndex = signatureIndex;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if ((constants == null) || (this.exceptionNameIndex == 0))
			return null;
		
		return constants.getConstantUtf8(this.exceptionNameIndex);
	}
}
