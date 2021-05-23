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
package jd.core.process.analyzer.instruction.fast;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;
import jd.core.model.instruction.fast.instruction.FastList;
import jd.core.model.instruction.fast.instruction.FastSwitch;
import jd.core.model.instruction.fast.instruction.FastTest2Lists;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.util.StringConstants;



public class DupLocalVariableAnalyzer
{
	public static void Declare(
		ClassFile classFile, Method method, List<Instruction> list)
	{	
		RecursiveDeclare(
			classFile.getConstantPool(), method.getLocalVariables(), 
			method.getCode().length, list);
	}

	private static void RecursiveDeclare(
			ConstantPool constants, 
			LocalVariables localVariables, 
			int codeLength,
			List<Instruction> list)
	{		
		int length = list.size();
		
		// Appels recursifs
		for (int index=0; index<length; index++)
		{
			Instruction instruction = list.get(index);
			
			switch (instruction.opcode)
			{
			case FastConstants.WHILE:
			case FastConstants.DO_WHILE:
			case FastConstants.INFINITE_LOOP:
			case FastConstants.FOR:
			case FastConstants.FOREACH:
			case FastConstants.IF_:
			case FastConstants.SYNCHRONIZED:
				{
					List<Instruction> instructions = 
						((FastList)instruction).instructions;
					if (instructions != null)
						RecursiveDeclare(
							constants, localVariables, codeLength, instructions);
				}
				break;	
				
			case FastConstants.IF_ELSE:
				{
					FastTest2Lists ft2l = (FastTest2Lists)instruction;
					RecursiveDeclare(
						constants, localVariables, codeLength, ft2l.instructions);				
					RecursiveDeclare(
						constants, localVariables, codeLength, ft2l.instructions2);
				}
				break;	
				
			case FastConstants.SWITCH:
			case FastConstants.SWITCH_ENUM:
			case FastConstants.SWITCH_STRING:
				{
					FastSwitch.Pair[] pairs = ((FastSwitch)instruction).pairs;
					if (pairs != null)
						for (int i=pairs.length-1; i>=0; --i)
						{
							List<Instruction> instructions = pairs[i].getInstructions();					
							if (instructions != null)
								RecursiveDeclare(
									constants, localVariables, codeLength, instructions);		
						}
				}
				break;
				
			case FastConstants.TRY:
				{
					FastTry ft = (FastTry)instruction;
					RecursiveDeclare(
						constants, localVariables, codeLength, ft.instructions);
					
					if (ft.catches != null)
						for (int i=ft.catches.size()-1; i>=0; --i)
							RecursiveDeclare(
								constants, localVariables, 
								codeLength, ft.catches.get(i).instructions);
					
					if (ft.finallyInstructions != null)
						RecursiveDeclare(
							constants, localVariables, 
							codeLength, ft.finallyInstructions);
				}
			}
		}	
		
		// Declaration des variables locales temporaires
		for (int i=0; i<length; i++)
		{
			Instruction instruction = list.get(i);
			
			if (instruction.opcode != ByteCodeConstants.DUPSTORE)
				continue;
			
			DupStore dupStore = (DupStore)instruction;
			
			String signature = 
				dupStore.objectref.getReturnedSignature(constants, localVariables);
			
			int signatureIndex = constants.addConstantUtf8(signature);
			int nameIndex = constants.addConstantUtf8(
				StringConstants.TMP_LOCAL_VARIABLE_NAME + 
				dupStore.offset + "_" + 
				((DupStore)instruction).objectref.offset);
			int varIndex = localVariables.size();
				
			LocalVariable lv = new LocalVariable(
				dupStore.offset, codeLength, 
				nameIndex, signatureIndex, varIndex);
			lv.declarationFlag = true;
			localVariables.add(lv);

			list.set(i, new FastDeclaration(
				FastConstants.DECLARE, dupStore.offset, 
				Instruction.UNKNOWN_LINE_NUMBER, lv.index, dupStore));
		}
	}
}
