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
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.process.analyzer.classfile.visitor.CompareInstructionVisitor;


/*
 * Recontruction des operateurs d'assignation depuis les motifs :
 * 1) Operation sur les attributs de classes: 
 *    PutStatic(BinaryOperator(GetStatic(), ...))
 * 2) Operation sur les attributs d'instance: 
 *    PutField(objectref, BinaryOperator(GetField(objectref), ...))
 * 3) Operation sur les variables locales: 
 *    Store(BinaryOperator(Load(), ...))
 * 4) Operation sur les variables locales: 
 *    IStore(BinaryOperator(ILoad(), ...))
 * 5) Operation sur des tableaux: 
 *    ArrayStore(arrayref, indexref, 
 *               BinaryOperator(ArrayLoad(arrayref, indexref), ...))
 */
public class AssignmentOperatorReconstructor 
{
	public static void Reconstruct(List<Instruction> list)
	{
		int index = list.size();
		
		while (index-- > 0)
		{
			Instruction i = list.get(index);
			
			switch (i.opcode)
			{
			case ByteCodeConstants.PUTSTATIC:
				if (((PutStatic)i).valueref.opcode == 
						ByteCodeConstants.BINARYOP)
					index = ReconstructPutStaticOperator(list, index, i);
				break;
			case ByteCodeConstants.PUTFIELD:
				if (((PutField)i).valueref.opcode == 
						ByteCodeConstants.BINARYOP)
					index = ReconstructPutFieldOperator(list, index, i);
				break;
			case ByteCodeConstants.ISTORE:
				if (((StoreInstruction)i).valueref.opcode == 
						ByteCodeConstants.BINARYOP)
				{
					BinaryOperatorInstruction boi = (BinaryOperatorInstruction)
						((StoreInstruction)i).valueref;
					if (boi.value1.opcode == ByteCodeConstants.ILOAD) 
						index = ReconstructStoreOperator(list, index, i, boi);
				}
				break;
			case ByteCodeConstants.STORE:
				if (((StoreInstruction)i).valueref.opcode == 
						ByteCodeConstants.BINARYOP)
				{
					BinaryOperatorInstruction boi = (BinaryOperatorInstruction)
						((StoreInstruction)i).valueref;
					if (boi.value1.opcode == ByteCodeConstants.LOAD) 
						index = ReconstructStoreOperator(list, index, i, boi);
				}
				break;
			case ByteCodeConstants.ARRAYSTORE:
				if (((ArrayStoreInstruction)i).valueref.opcode == 
						ByteCodeConstants.BINARYOP)
					index = ReconstructArrayOperator(list, index, i);
				break;
			}
		}
	}

	/*
	 * PutStatic(BinaryOperator(GetStatic(), ...))
	 */
	private static int ReconstructPutStaticOperator(
		List<Instruction> list, int index, Instruction i)
	{
		PutStatic putStatic = (PutStatic)i;
		BinaryOperatorInstruction boi = 
			(BinaryOperatorInstruction)putStatic.valueref;
		
		if (boi.value1.opcode != ByteCodeConstants.GETSTATIC)
			return index;
		
		GetStatic getStatic = (GetStatic)boi.value1;
		
		if ((putStatic.lineNumber != getStatic.lineNumber) ||
			(putStatic.index != getStatic.index))
			return index;
		
		String newOperator = boi.operator + "=";
		
		list.set(index, new AssignmentInstruction(
			ByteCodeConstants.ASSIGNMENT, putStatic.offset,
			getStatic.lineNumber, boi.getPriority(), newOperator,
			getStatic, boi.value2));
		
		return index;		
	}
	
	/*
	 * PutField(objectref, BinaryOperator(GetField(objectref), ...))
	 */
	private static int ReconstructPutFieldOperator(
		List<Instruction> list, int index, Instruction i)
	{
		PutField putField = (PutField)i;
		BinaryOperatorInstruction boi = 
			(BinaryOperatorInstruction)putField.valueref;
		
		if (boi.value1.opcode != ByteCodeConstants.GETFIELD)
			return index;
		
		GetField getField = (GetField)boi.value1;
		CompareInstructionVisitor visitor = new CompareInstructionVisitor();

		if ((putField.lineNumber != getField.lineNumber) ||
			(putField.index != getField.index) ||
			!visitor.visit(putField.objectref, getField.objectref))
			return index;
		
		if (putField.objectref.opcode == ByteCodeConstants.DUPLOAD)
		{
			// Remove DupStore & DupLoad
			DupLoad dupLoad = (DupLoad)getField.objectref; 
			index = DeleteDupStoreInstruction(list, index, dupLoad);
			getField.objectref = dupLoad.dupStore.objectref;
		}
		
		String newOperator = boi.operator + "=";
		
		list.set(index, new AssignmentInstruction(
			ByteCodeConstants.ASSIGNMENT, putField.offset,
			getField.lineNumber, boi.getPriority(), newOperator,
			getField, boi.value2));
		
		return index;		
	}
	
	/*
	 * StoreInstruction(BinaryOperator(LoadInstruction(), ...))
	 */
	private static int ReconstructStoreOperator(
		List<Instruction> list, int index, 
		Instruction i, BinaryOperatorInstruction boi)
	{
		StoreInstruction si = (StoreInstruction)i;		
		LoadInstruction li = (LoadInstruction)boi.value1;
		
		if ((si.lineNumber != li.lineNumber) || (si.index != li.index))
			return index;
		
		String newOperator = boi.operator + "=";
		
		list.set(index, new AssignmentInstruction(
			ByteCodeConstants.ASSIGNMENT, si.offset,
			li.lineNumber, boi.getPriority(), newOperator,
			li, boi.value2));
		
		return index;		
	}
	
	/*
	 * ArrayStore(arrayref, indexref, 
	 *            BinaryOperator(ArrayLoad(arrayref, indexref), ...))
	 */
	private static int ReconstructArrayOperator(
		List<Instruction> list, int index, Instruction i)
	{
		ArrayStoreInstruction asi = (ArrayStoreInstruction)i;
		BinaryOperatorInstruction boi = (BinaryOperatorInstruction)asi.valueref;
		
		if (boi.value1.opcode != ByteCodeConstants.ARRAYLOAD)
			return index;
		
		ArrayLoadInstruction ali = (ArrayLoadInstruction)boi.value1;
		CompareInstructionVisitor visitor = new CompareInstructionVisitor();
		
		if ((asi.lineNumber != ali.lineNumber) ||
			!visitor.visit(asi.arrayref, ali.arrayref) || 
			!visitor.visit(asi.indexref, ali.indexref))
			return index;
		
		if (asi.arrayref.opcode == ByteCodeConstants.DUPLOAD)
		{
			// Remove DupStore & DupLoad
			DupLoad dupLoad = (DupLoad)ali.arrayref; 
			index = DeleteDupStoreInstruction(list, index, dupLoad);
			ali.arrayref = dupLoad.dupStore.objectref;
		}
		
		if (asi.indexref.opcode == ByteCodeConstants.DUPLOAD)
		{
			// Remove DupStore & DupLoad
			DupLoad dupLoad = (DupLoad)ali.indexref; 
			index = DeleteDupStoreInstruction(list, index, dupLoad);
			ali.indexref = dupLoad.dupStore.objectref;
		}
		
		String newOperator = boi.operator + "=";
		
		list.set(index, new AssignmentInstruction(
			ByteCodeConstants.ASSIGNMENT, asi.offset,
			ali.lineNumber, boi.getPriority(), newOperator,
			ali, boi.value2));
		
		return index;
	}
	
	private static int DeleteDupStoreInstruction(
		List<Instruction> list, int index, DupLoad dupLoad)
	{
		int indexTmp = index;
		
		while (indexTmp-- > 0)
		{
			Instruction i = list.get(indexTmp);

			if (dupLoad.dupStore == i)
			{
				list.remove(indexTmp);
				return --index;
			}
		}
		
		return index;
	}
}
