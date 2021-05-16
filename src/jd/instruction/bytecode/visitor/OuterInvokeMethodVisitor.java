package jd.instruction.bytecode.visitor;

import java.util.HashMap;

import jd.Constants;
import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.accessor.Accessor;
import jd.classfile.accessor.InvokeMethodAccessor;
import jd.classfile.constant.ConstantMethodref;
import jd.classfile.constant.ConstantNameAndType;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.Invokespecial;
import jd.instruction.bytecode.instruction.Invokestatic;
import jd.instruction.bytecode.instruction.Invokevirtual;

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
			(accessor.tag != Constants.ACCESSOR_INVOKEMETHOD))
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
		
		Instruction objectref = is.args.remove(0);
				
		switch (ima.methodOpcode)
		{
		case ByteCodeConstants.INVOKESPECIAL:
			return new Invokespecial(
				ByteCodeConstants.INVOKESPECIAL, i.offset, i.lineNumber, 
				cmrIndex, objectref, is.args);
		case ByteCodeConstants.INVOKEVIRTUAL:
			return new Invokevirtual(
				ByteCodeConstants.INVOKEVIRTUAL, i.offset, i.lineNumber, 
				cmrIndex, objectref, is.args);
		default:
			return i;
		}
	}
}
