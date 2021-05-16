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

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.ConstInstruction;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.process.analyzer.util.ReconstructorUtil;


/*
 * Recontruction des pre-incrementations depuis le motif :
 * DupStore( (i - 1F) )
 * ...
 * {?Store | PutField | PutStatic}( DupLoad )
 * ...
 * ???( DupLoad )
 */
public class PreIncReconstructor 
{
	public static void Reconstruct(List<Instruction> list)
	{
		int length = list.size();

		for (int dupStoreIndex=0; dupStoreIndex<length; dupStoreIndex++)
		{
			if (list.get(dupStoreIndex).opcode != ByteCodeConstants.DUPSTORE)
				continue;

			// DupStore trouv�
			DupStore dupstore = (DupStore)list.get(dupStoreIndex);
			
			if ((dupstore.objectref.opcode != ByteCodeConstants.BINARYOP))
				continue;
			
			BinaryOperatorInstruction boi = 
				(BinaryOperatorInstruction)dupstore.objectref;
			
			if ((boi.value2.opcode != ByteCodeConstants.ICONST) && 
				(boi.value2.opcode != ByteCodeConstants.LCONST) && 
				(boi.value2.opcode != ByteCodeConstants.DCONST) && 
				(boi.value2.opcode != ByteCodeConstants.FCONST))
				continue;

			ConstInstruction ci = (ConstInstruction)boi.value2;
			
			if (ci.value != 1)
				continue;
			
			int value;
			
			if (boi.operator.equals("+"))
				value = 1;
			else if (boi.operator.equals("-"))
				value = -1;
			else
				continue;			
			
			int xstorePutfieldPutstaticIndex = dupStoreIndex;
			
			while (++xstorePutfieldPutstaticIndex < length)
			{
				Instruction i = list.get(xstorePutfieldPutstaticIndex);
				Instruction dupload = null;
				
				switch (i.opcode)
				{
				case ByteCodeConstants.ASTORE:
					if ((boi.value1.opcode == ByteCodeConstants.ALOAD) && 
						(((StoreInstruction)i).valueref.opcode == ByteCodeConstants.DUPLOAD) &&
						(((IndexInstruction)i).index == ((IndexInstruction)boi.value1).index))
						// 1er DupLoad trouv�
						dupload = (DupLoad)((StoreInstruction)i).valueref;
						break;
				case ByteCodeConstants.ISTORE:
					if ((boi.value1.opcode == ByteCodeConstants.ILOAD) &&
						(((StoreInstruction)i).valueref.opcode == ByteCodeConstants.DUPLOAD) &&
						(((IndexInstruction)i).index == ((IndexInstruction)boi.value1).index))
						// 1er DupLoad trouv�
						dupload = (DupLoad)((StoreInstruction)i).valueref;
						break;
				case ByteCodeConstants.STORE:
					if ((boi.value1.opcode == ByteCodeConstants.LOAD) &&
						(((StoreInstruction)i).valueref.opcode == ByteCodeConstants.DUPLOAD) &&
						(((IndexInstruction)i).index == ((IndexInstruction)boi.value1).index))
						// 1er DupLoad trouv�
						dupload = (DupLoad)((StoreInstruction)i).valueref;
					break;
				case ByteCodeConstants.PUTFIELD:
					if ((boi.value1.opcode == ByteCodeConstants.GETFIELD) &&
						(((PutField)i).valueref.opcode == ByteCodeConstants.DUPLOAD) &&
					    (((IndexInstruction)i).index == ((IndexInstruction)boi.value1).index))
						// 1er DupLoad trouv�
						dupload = (DupLoad)((PutField)i).valueref;
					break;
				case ByteCodeConstants.PUTSTATIC:
					if ((boi.value1.opcode == ByteCodeConstants.GETSTATIC) &&
						(((PutStatic)i).valueref.opcode == ByteCodeConstants.DUPLOAD) &&
				        (((IndexInstruction)i).index == ((IndexInstruction)boi.value1).index))
						// 1er DupLoad trouv�
						dupload = (DupLoad)((PutStatic)i).valueref;
					break;
				}
					
				if ((dupload == null) || (dupload.offset != dupstore.offset))
					continue;
				
				Instruction preinc = new IncInstruction(
					ByteCodeConstants.PREINC, boi.offset, 
					boi.lineNumber, boi.value1, value);

				ReconstructorUtil.ReplaceDupLoad(
						list, xstorePutfieldPutstaticIndex+1, dupstore, preinc);
				
				list.remove(xstorePutfieldPutstaticIndex);
				list.remove(dupStoreIndex);
				dupStoreIndex--;
				length = list.size();
				break;
			}			
		}	
	}
}
