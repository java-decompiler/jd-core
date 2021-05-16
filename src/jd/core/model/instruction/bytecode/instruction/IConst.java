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

/**
 * Pseudo type 'X' correspond � char, byte, short ou int ...
 */
public class IConst extends ConstInstruction 
{
	public String signature;	

	public IConst(int opcode, int offset, int lineNumber, int value)
	{
		super(opcode, offset, lineNumber, value);
		
		/* Definition de la signature:
		 * 'X' si TBF_INT_INT|TBF_INT_SHORT|TBF_INT_BYTE|TBF_INT_CHAR|TBF_INT_BOOLEAN est possible
		 * 'Y' si TBF_INT_INT|TBF_INT_SHORT|TBF_INT_BYTE|TBF_INT_CHAR est possible
		 * 'C' si TBF_INT_INT|TBF_INT_SHORT|TBF_INT_CHAR est possible
		 * 'B' si TBF_INT_INT|TBF_INT_SHORT|TBF_INT_BYTE est possible
		 * 'S' si TBF_INT_INT|TBF_INT_SHORT est possible
		 * 'I' si TBF_INT_INT est possible
		 */

		if (value < 0)
		{
			if (value >= Byte.MIN_VALUE)
				this.signature = "B";		
			else if (value >= Short.MIN_VALUE)
				this.signature = "S";				
			else 
				this.signature = "I";				
		}
		else
		{
			if (value <= 1)
				this.signature = "X";				
			else if (value <= Byte.MAX_VALUE)
				this.signature = "Y";				
			else if (value <= Character.MAX_VALUE)
				this.signature = "C";				
			else if (value <= Short.MAX_VALUE)
				this.signature = "S";				
			else 
				this.signature = "I";				
		}
	}
	
	public String getSignature() 
	{
		return signature;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return this.signature;
	}

	public void setReturnedSignature(String signature) 
	{
		this.signature = signature;
	}
}
