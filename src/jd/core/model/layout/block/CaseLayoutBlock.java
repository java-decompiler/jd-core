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
package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.fast.instruction.FastSwitch;


public class CaseLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	public Method method;
	public FastSwitch fs;
	public int firstIndex;
	public int lastIndex;
	
	public CaseLayoutBlock(
		byte tag, ClassFile classFile, Method method, 
		FastSwitch fs, int firstIndex, int lastIndex) 
	{
		this(
			tag, classFile, method, fs, 
			firstIndex, lastIndex, lastIndex-firstIndex);
	}
	
	protected CaseLayoutBlock(
		byte tag, ClassFile classFile, Method method, FastSwitch fs, 
		int firstIndex, int lastIndex, int preferedLineCount) 
	{
		super(
			tag, 
			Instruction.UNKNOWN_LINE_NUMBER, 
			Instruction.UNKNOWN_LINE_NUMBER, 
			0, preferedLineCount, preferedLineCount);
		
		this.classFile = classFile;
		this.method = method;
		this.fs = fs;
		this.firstIndex = firstIndex;
		this.lastIndex = lastIndex;
	}
}
