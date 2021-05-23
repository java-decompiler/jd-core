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
package jd.core.process.analyzer.classfile.visitor;

import java.util.HashMap;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.accessor.Accessor;
import jd.core.model.classfile.accessor.AccessorConstants;
import jd.core.model.classfile.accessor.InvokeMethodAccessor;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokeinterface;
import jd.core.model.instruction.bytecode.instruction.Invokespecial;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;

/*
 * Replace 'EntitlementFunctionLibrary.access$000()' 
 * par 'EntitlementFunctionLibrary.kernelId'
 */
public class OuterInvokeMethodVisitor extends OuterGetStaticVisitor
{
	public OuterInvokeMethodVisitor(
		HashMap<String, ClassFile> innerClassesMap, ConstantPool constants)
	{
		super(innerClassesMap, constants);
	}

	protected Accessor match(Instruction i)
	{
		if (i.opcode != ByteCodeConstants.INVOKESTATIC)		
			return null; 
		
		Invokestatic is = (Invokestatic)i;
		ConstantMethodref cmr = 
			constants.getConstantMethodref(is.index);
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cmr.name_and_type_index);
		String descriptor = 
			constants.getConstantUtf8(cnat.descriptor_index);

		// Zero parameter ?
		if (descriptor.charAt(1) == ')')
			return null;

		String className = constants.getConstantClassName(cmr.class_index);
		ClassFile classFile = this.innerClassesMap.get(className);
		if (classFile == null)
			return null;
		
		String name = 
			constants.getConstantUtf8(cnat.name_index);
		
		Accessor accessor = classFile.getAccessor(name, descriptor);
		
		if ((accessor == null) ||
			(accessor.tag != AccessorConstants.ACCESSOR_INVOKEMETHOD))
			return null;
		
		return accessor;
	}
	
	protected Instruction newInstruction(Instruction i, Accessor a)
	{
		InvokeMethodAccessor ima = (InvokeMethodAccessor)a;
		Invokestatic is = (Invokestatic)i;
		
		int nameIndex = this.constants.addConstantUtf8(ima.methodName);
		int descriptorIndex = 
			this.constants.addConstantUtf8(ima.methodDescriptor);
		int cnatIndex = 
			this.constants.addConstantNameAndType(nameIndex, descriptorIndex);
		
		int classNameIndex = this.constants.addConstantUtf8(ima.className);	
		int classIndex = this.constants.addConstantClass(classNameIndex);
		
		int cmrIndex = constants.addConstantMethodref(
			classIndex, cnatIndex, 
			ima.listOfParameterSignatures, ima.returnedSignature);
		
		switch (ima.methodOpcode)
		{
		case ByteCodeConstants.INVOKESPECIAL:
			{
				Instruction objectref = is.args.remove(0);		
				return new Invokespecial(
					ByteCodeConstants.INVOKESPECIAL, i.offset, i.lineNumber, 
					cmrIndex, objectref, is.args);
			}
		case ByteCodeConstants.INVOKEVIRTUAL:
			{
				Instruction objectref = is.args.remove(0);		
				return new Invokevirtual(
					ByteCodeConstants.INVOKEVIRTUAL, i.offset, i.lineNumber, 
					cmrIndex, objectref, is.args);
			}
		case ByteCodeConstants.INVOKEINTERFACE:
			{
				Instruction objectref = is.args.remove(0);		
				return new Invokeinterface(
					ByteCodeConstants.INVOKEINTERFACE, i.offset, i.lineNumber, 
					cmrIndex, objectref, is.args);
			}
		case ByteCodeConstants.INVOKESTATIC:
			{
				return new Invokestatic(
					ByteCodeConstants.INVOKESTATIC, i.offset, i.lineNumber, 
					cmrIndex, is.args);
			}
		default:
			return i;
		}
	}
}
