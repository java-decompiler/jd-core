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
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.Invokespecial;
import jd.core.model.instruction.bytecode.instruction.New;
import jd.core.process.analyzer.util.ReconstructorUtil;


/*
 * Recontruction de l'instruction 'new' depuis le motif :
 * DupStore( New(java/lang/Long) )
 * ...
 * Invokespecial(DupLoad, <init>, [ IConst_1 ])
 * ...
 * ??? DupLoad
 */
public class NewInstructionReconstructor extends NewInstructionReconstructorBase
{
	public static void Reconstruct(
			ClassFile classFile, Method method, List<Instruction> list)
	{
		for (int dupStoreIndex=0; dupStoreIndex<list.size(); dupStoreIndex++)
		{
			if (list.get(dupStoreIndex).opcode != ByteCodeConstants.DUPSTORE)
				continue;
			
			DupStore ds = (DupStore)list.get(dupStoreIndex);
			
			if (ds.objectref.opcode != ByteCodeConstants.NEW)
				continue;
			
			int invokespecialIndex = dupStoreIndex;
			final int length = list.size();
			
			while (++invokespecialIndex < length)
			{
				Instruction instruction = list.get(invokespecialIndex);

				if (instruction.opcode != ByteCodeConstants.INVOKESPECIAL)
					continue;
				
				Invokespecial is = (Invokespecial)instruction;

				if (is.objectref.opcode != ByteCodeConstants.DUPLOAD)
					continue;
				
				DupLoad dl = (DupLoad)is.objectref;
				
				if (dl.offset != ds.offset)
					continue;
				
				ConstantPool constants = classFile.getConstantPool();
				ConstantMethodref cmr = constants.getConstantMethodref(is.index);
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cmr.name_and_type_index);
						
				if (cnat.name_index == constants.instanceConstructorIndex)
				{
					New nw = (New)ds.objectref;						
					InvokeNew invokeNew = new InvokeNew(
						ByteCodeConstants.INVOKENEW, is.offset, 
						nw.lineNumber, is.index, is.args);
					
					Instruction parentFound = ReconstructorUtil.ReplaceDupLoad(
						list, invokespecialIndex+1, ds, invokeNew);
					
					list.remove(invokespecialIndex);
					if (parentFound == null)
						list.set(dupStoreIndex, invokeNew);
					else
						list.remove(dupStoreIndex--);
					
					InitAnonymousClassConstructorParameterName(
						classFile, method, invokeNew);
					break;
				}
			}							
		}
	}
}
