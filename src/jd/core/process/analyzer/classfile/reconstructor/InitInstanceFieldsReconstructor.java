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

import java.util.ArrayList;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokespecial;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.process.analyzer.classfile.visitor.CompareInstructionVisitor;
import jd.core.process.analyzer.classfile.visitor.SearchInstructionByOpcodeVisitor;


public class InitInstanceFieldsReconstructor 
{
	public static void Reconstruct(ClassFile classFile)
	{
		ArrayList<PutField> putFieldList = new ArrayList<PutField>();
    	ConstantPool constants = classFile.getConstantPool();
    	Method[] methods = classFile.getMethods();
    	
    	if (methods == null)
    		return;

		int methodIndex = methods.length;
		Method putFieldListMethod = null;
		
		// Recherche du dernier constructeur ne faisait pas appel a 'this(...)'
		while (methodIndex > 0)
		{
	    	final Method method = methods[--methodIndex];
	    	
	    	if (((method.access_flags & (ClassFileConstants.ACC_SYNTHETIC|ClassFileConstants.ACC_BRIDGE)) != 0) ||
	    		(method.getCode() == null) ||
	    		(method.getFastNodes() == null) ||
	    		(method.containsError() == true) ||
	    		(method.name_index != constants.instanceConstructorIndex))
	    		continue;
			
			List<Instruction> list = method.getFastNodes();
	    	if (list == null)
	    		continue;
	    	
			int length = list.size();
			
			if (length > 0)
			{
				int j = GetSuperCallIndex(classFile, constants, list);
				
				if (j < 0)
					continue;
					
				j++;
				
				int lineNumberBefore = (j > 0) ? 
					list.get(j-1).lineNumber : Instruction.UNKNOWN_LINE_NUMBER;
				Instruction instruction = null;
					
				// Store init values
				while (j < length)
				{
					instruction = list.get(j++);
					if (instruction.opcode != ByteCodeConstants.PUTFIELD)
						break;
					
					PutField putField = (PutField)instruction;
					ConstantFieldref cfr = constants.getConstantFieldref(putField.index);

					if ((cfr.class_index != classFile.getThisClassIndex()) ||
						(putField.objectref.opcode != ByteCodeConstants.ALOAD))
						break;
					
					ALoad aLaod = (ALoad)putField.objectref;
					if (aLaod.index != 0)
						break;
					
					Instruction valueInstruction = 
						SearchInstructionByOpcodeVisitor.visit(
								putField.valueref, ByteCodeConstants.ALOAD);
					if ((valueInstruction != null) && 
						(((ALoad)valueInstruction).index != 0))
						break;
					if (SearchInstructionByOpcodeVisitor.visit(
							putField.valueref, ByteCodeConstants.LOAD) != null)
						break;
					if (SearchInstructionByOpcodeVisitor.visit(
							putField.valueref, ByteCodeConstants.ILOAD) != null)
						break;

					putFieldList.add(putField);
					putFieldListMethod = method;
				}
				
				// Filter list of 'PUTFIELD'
				if ((lineNumberBefore != Instruction.UNKNOWN_LINE_NUMBER) && 
					(instruction != null))
				{
					int i = putFieldList.size();
					int lineNumberAfter = instruction.lineNumber;

					// Si l'instruction qui suit la serie de 'PUTFIELD' est une
					// 'RETURN' ayant le meme numero de ligne que le dernier
					// 'PUTFIELD', le constructeur est synthetique et ne sera
					// pas filtre.
					if ((instruction.opcode != ByteCodeConstants.RETURN) ||
						(j != length) || (i == 0) ||
						(lineNumberAfter != putFieldList.get(i-1).lineNumber))
					{					
						while (i-- > 0)
						{
							int lineNumber = putFieldList.get(i).lineNumber;
							
							if ((lineNumberBefore <= lineNumber) &&
								(lineNumber <= lineNumberAfter))
							{
								// Remove 'PutField' instruction if it used in 
								// code block of constructor
								putFieldList.remove(i);
							}
						}
					}
				}
			}
			
			break;
		}
	    
		// Filter list
		CompareInstructionVisitor visitor =	new CompareInstructionVisitor();

		while (methodIndex > 0)
		{
	    	final Method method = methods[--methodIndex];
	    	
	    	if (((method.access_flags & 
	    			(ClassFileConstants.ACC_SYNTHETIC|ClassFileConstants.ACC_BRIDGE)) != 0))
	    		continue;
	    	if (method.getCode() == null)
	    		continue;
			if (method.name_index != constants.instanceConstructorIndex)
				continue;
			
			List<Instruction> list = method.getFastNodes();
			int length = list.size();
			
			if (length > 0)
			{
				// Filter init values
				int j = GetSuperCallIndex(classFile, constants, list);
				
				if (j < 0)
					continue;
				
				int firstPutFieldIndex = j + 1;
				int putFieldListLength = putFieldList.size();

				// If 'putFieldList' is more loonger than 'list', 
				// remove extra 'putField'.
				while (firstPutFieldIndex+putFieldListLength > length)
					putFieldList.remove(--putFieldListLength);					

				for (int i=0; i<putFieldListLength; i++)
				{
					Instruction initFieldInstruction = putFieldList.get(i);
					Instruction instruction = list.get(firstPutFieldIndex+i);
					
					if ((initFieldInstruction.lineNumber != instruction.lineNumber) ||
						!visitor.visit(initFieldInstruction, instruction))
					{
						while (i < putFieldListLength)
							putFieldList.remove(--putFieldListLength);
						break;
					}					
				}
			}
		}
		
		// Setup initial values
		int putFieldListLength = putFieldList.size();
		Field[] fields = classFile.getFields();
		
		if ((putFieldListLength > 0) && (fields != null))
		{		
			int fieldLength = fields.length;
			int putFieldListIndex = putFieldListLength;
				
			while (putFieldListIndex-- > 0)
			{
				PutField putField = putFieldList.get(putFieldListIndex);			
				ConstantFieldref cfr = 
					constants.getConstantFieldref(putField.index);
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cfr.name_and_type_index);
				int fieldIndex;
			
				for (fieldIndex=0; fieldIndex<fieldLength; fieldIndex++)
				{
					Field field = fields[fieldIndex];
				
					if ((cnat.name_index == field.name_index) &&
						(cnat.descriptor_index == field.descriptor_index) &&
						((field.access_flags & ClassFileConstants.ACC_STATIC) == 0))	
					{
						// Field found
						Instruction valueref = putField.valueref;
						field.setValueAndMethod(valueref, putFieldListMethod);
						if (valueref.opcode == FastConstants.NEWANDINITARRAY)
							valueref.opcode = FastConstants.INITARRAY;
						break;
					}
				}
				
				if (fieldIndex == fieldLength)
				{
					// Field not found
					// Remove putField not use to initialize fields
					putFieldList.remove(putFieldListIndex);
					putFieldListLength--;					
				}
			}
			
			if (putFieldListLength > 0)
			{
				// Remove instructions from constructors
				methodIndex = methods.length;
				
				while (methodIndex-- > 0)
				{
			    	final Method method = methods[methodIndex];
			    	
			    	if (((method.access_flags & 
			    			(ClassFileConstants.ACC_SYNTHETIC|ClassFileConstants.ACC_BRIDGE)) != 0))
			    		continue;
			    	if (method.getCode() == null)
			    		continue;
					if (method.name_index != constants.instanceConstructorIndex)
						continue;
					
					List<Instruction> list = method.getFastNodes();
					int length = list.size();
					
					if (length > 0)
					{
						// Remove instructions
						putFieldListIndex = 0;
						int putFieldIndex = putFieldList.get(putFieldListIndex).index;
						
						for (int index=0; index<length; index++)
						{
							Instruction instruction = list.get(index);						
							if (instruction.opcode != ByteCodeConstants.PUTFIELD)
								continue;
							
							PutField putField = (PutField)instruction;
							if (putField.index != putFieldIndex)
								continue;
							
							ConstantFieldref cfr = 
								constants.getConstantFieldref(putField.index);
							if ((cfr.class_index != classFile.getThisClassIndex()) ||
								(putField.objectref.opcode != ByteCodeConstants.ALOAD))
								continue;
							
							ALoad aLaod = (ALoad)putField.objectref;
							if (aLaod.index != 0)
								continue;
							
							list.remove(index--);
							length--;
							
							if (++putFieldListIndex >= putFieldListLength)
								break;
							putFieldIndex = 
								putFieldList.get(putFieldListIndex).index;
							
						}
					}
				}
			}
		}
	}	
	
	private static int GetSuperCallIndex(
		ClassFile classFile, ConstantPool constants, List<Instruction> list)
	{
		int length = list.size();
		
		for (int i=0; i<length; i++)
		{
			Instruction instruction = list.get(i);
			
			if (instruction.opcode != ByteCodeConstants.INVOKESPECIAL)
				continue;
				
			Invokespecial is = (Invokespecial)instruction;
			
			if ((is.objectref.opcode != ByteCodeConstants.ALOAD) ||
				(((ALoad)is.objectref).index != 0))
				continue;
			
			ConstantMethodref cmr = constants.getConstantMethodref(is.index);			
			ConstantNameAndType cnat = 
				constants.getConstantNameAndType(cmr.name_and_type_index);
			
			if (cnat.name_index != constants.instanceConstructorIndex)
				continue;
			
			if (cmr.class_index == classFile.getThisClassIndex())
				return -1;
			
			return i;
		}
		
		return -1;
	}
}
