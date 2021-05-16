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

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.util.UtilConstants;


public class NewInstructionReconstructorBase 
{
	/*
	 * Methode permettznt l'affichage des variables locales d'une methode d'une
	 * outer class dans une inner class
	 */
	public static void InitAnonymousClassConstructorParameterName(
		ClassFile classFile, Method method, InvokeNew invokeNew)
	{
		ConstantPool constants = classFile.getConstantPool();
		ConstantMethodref cmr = 
			constants.getConstantMethodref(invokeNew.index);		
		String internalClassName = constants.getConstantClassName(
			cmr.class_index);
		ClassFile innerClassFile = 
			classFile.getInnerClassFile(internalClassName);
				
		if (innerClassFile != null)
		{
			// Initialize inner and anonymous class field names
			Field[] innerFields = innerClassFile.getFields();
			
			if (innerFields != null)
			{
				int i = innerFields.length;
				int argsLength = invokeNew.args.size();
				ConstantPool innerConstants = innerClassFile.getConstantPool();
				LocalVariables localVariables = method.getLocalVariables();
				
				while (i-- > 0)
				{
					Field innerField = innerFields[i];
					int index = innerField.anonymousClassConstructorParameterIndex;
					
					if (index != UtilConstants.INVALID_INDEX)
					{
						innerField.anonymousClassConstructorParameterIndex =
							UtilConstants.INVALID_INDEX;
						
						if (index < argsLength)
						{
							Instruction arg = invokeNew.args.get(index);
							
							if (arg.opcode == ByteCodeConstants.CHECKCAST)
								arg = ((CheckCast)arg).objectref;
								
							switch (arg.opcode)
							{
							case ByteCodeConstants.LOAD:
							case ByteCodeConstants.ALOAD:
							case ByteCodeConstants.ILOAD:								
								LocalVariable lv = 
									localVariables
										.getLocalVariableWithIndexAndOffset(
											((IndexInstruction)arg).index, 
											arg.offset);
								
								if (lv != null)
								{
									// Ajout du nom du parametre au ConstantPool
									// de la class anonyme 
									String name = 
										constants.getConstantUtf8(lv.name_index);
									innerField.outerMethodLocalVariableNameIndex = 
										innerConstants.addConstantUtf8(name);
									// Ajout du flag 'final' sur la variable 
									// locale de la methode contenant 
									// l'instruction "new"
									lv.finalFlag = true;									
								}
							}			
						}
					}
				}
			}	
		}
	}
}
