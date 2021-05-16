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
package jd.core.process.analyzer.instruction.bytecode;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;


public class InstructionListException extends RuntimeException
{
	private static final long serialVersionUID = -2987531947933382754L;

	public InstructionListException(
		ClassFile classFile, Method method, int offset, Throwable cause) 
	{ 
		super(FormatMessage(classFile, method, offset), cause); 
	}
	
	private static String FormatMessage(
		ClassFile classFile, Method method, int offset)
	{
		ConstantPool constants = classFile.getConstantPool();
		
		String name = constants.getConstantUtf8(method.name_index);
		String descriptor = constants.getConstantUtf8(method.descriptor_index);
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("method='");
		sb.append(name);
		sb.append("', descriptor='");
		sb.append(descriptor);
		sb.append("', offset=");
		sb.append(offset);
		
		return sb.toString();
	}
}
