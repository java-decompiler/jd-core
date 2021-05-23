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
package jd.core.process.analyzer.classfile.reconstructor;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.Invokespecial;
import jd.core.model.instruction.bytecode.instruction.New;


/*
 * Recontruction de l'instruction 'new' depuis le motif :
 * Invokespecial(New, <init>, [ IConst_1 ])
 */
public class SimpleNewInstructionReconstructor 
	extends NewInstructionReconstructorBase
{
	public static void Reconstruct(
		ClassFile classFile, Method method, List<Instruction> list)
	{
		for (int invokespecialIndex=0; 
			 invokespecialIndex<list.size(); 
			 invokespecialIndex++)
		{
			if (list.get(invokespecialIndex).opcode != ByteCodeConstants.INVOKESPECIAL)
				continue;
			
			Invokespecial is = (Invokespecial)list.get(invokespecialIndex);
			
			if (is.objectref.opcode != ByteCodeConstants.NEW)
				continue;
			
			New nw = (New)is.objectref;		
			InvokeNew invokeNew = new InvokeNew(
				ByteCodeConstants.INVOKENEW, is.offset, 
				nw.lineNumber, is.index, is.args);
			
			list.set(invokespecialIndex, invokeNew);
			
			InitAnonymousClassConstructorParameterName(
				classFile, method, invokeNew);
		}
	}
}
