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
