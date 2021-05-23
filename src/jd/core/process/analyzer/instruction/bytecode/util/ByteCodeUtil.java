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
package jd.core.process.analyzer.instruction.bytecode.util;

import jd.core.model.instruction.bytecode.ByteCodeConstants;


public class ByteCodeUtil 
{
	public static int NextTableSwitchOffset(byte[] code, int index)
	{
		// Skip padding
		int i = (index+4) & 0xFFFC;
		
		i += 4;
		
		final int low = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;
		
		final int high = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;
		i += 4 * (high - low + 1);
	
		return i - 1;		
	}

	public static int NextLookupSwitchOffset(byte[] code, int index)
	{
		// Skip padding
		int i = (index+4) & 0xFFFC;
		
		i += 4;
		
		final int npairs = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;
		i += 8*npairs;
		
		return i - 1;	
	}

	public static int NextWideOffset(byte[] code, int index)
	{
		final int opcode = code[index+1] & 255;
		
		return index + ((opcode == ByteCodeConstants.IINC) ? 5 : 3);
	}
	
	public static int NextInstructionOffset(byte[] code, int index)
	{
		final int opcode = code[index] & 255;
		
		switch (opcode)
		{
		case ByteCodeConstants.TABLESWITCH:
			return NextTableSwitchOffset(code, index);
			
		case ByteCodeConstants.LOOKUPSWITCH:
			return NextLookupSwitchOffset(code, index);
			
		case ByteCodeConstants.WIDE:
			return NextWideOffset(code, index);
			
		default:
			return index + 1 + ByteCodeConstants.NO_OF_OPERANDS[opcode];
		}
	}

	public static boolean JumpTo(byte[] code, int offset, int targetOffset) {
		if (offset != -1) {
			int codeLength = code.length;

			for (int i = 0; i < 10; i++) {
				if (offset == targetOffset) {
					return true;
				} else {
					if (offset >= codeLength)
						break;

					int opcode = code[offset] & 255;

					if (opcode == ByteCodeConstants.GOTO) {
						offset += (short) (((code[offset + 1] & 255) << 8) | (code[offset + 2] & 255));
					} else if (opcode == ByteCodeConstants.GOTO_W) {

						offset += ((code[offset + 1] & 255) << 24) | ((code[offset + 2] & 255) << 16)
								| ((code[offset + 3] & 255) << 8) | (code[offset + 4] & 255);
					} else {
						break;
					}
				}
			}
		}

		return false;
	}
}
