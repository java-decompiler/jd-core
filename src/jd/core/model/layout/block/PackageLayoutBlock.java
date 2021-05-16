package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class PackageLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	
	public PackageLayoutBlock(ClassFile classFile) 
	{
		super(
			LayoutBlockConstants.PACKAGE, 
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 0, 0);
		this.classFile = classFile;
	}
}
