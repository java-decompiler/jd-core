package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class DeclareLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	public Method method;
	public Instruction instruction;
	
	public DeclareLayoutBlock(
		ClassFile classFile, Method method, Instruction instruction) 
	{
		super(
			LayoutBlockConstants.DECLARE, 
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 0, 0);
		this.classFile = classFile;
		this.method = method;
		this.instruction = instruction;
	}
}
