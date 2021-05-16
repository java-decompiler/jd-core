package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class MethodStaticLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;

	public MethodStaticLayoutBlock(ClassFile classFile) 
	{
		super(
			LayoutBlockConstants.METHOD_STATIC, 
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 0, 0);
		this.classFile = classFile;		
	}
}
