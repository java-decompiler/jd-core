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
package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.TableSwitch;


public class TableSwitchFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,  
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;

		// Skip padding
		int i = (offset+4) & 0xFFFC;
		
		final int defaultOffset = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;
		
		final int low = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;
		
		final int high = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;

		int length = high - low + 1;
		int[] offsets = new int[length];

		for (int j=0; j<length; j++)
		{
			offsets[j] = 
				((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
	            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
							
			i += 4;
		}
		
		final Instruction key = stack.pop();

		list.add(new TableSwitch(
			opcode, offset, lineNumber, key, defaultOffset, 
			offsets, low, high));
		
		return (i - offset - 1);
	}
}
