package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class ExtendsSuperInterfacesLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	
	public ExtendsSuperInterfacesLayoutBlock(ClassFile classFile) 
	{
		this(
			LayoutBlockConstants.EXTENDS_SUPER_INTERFACES,
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 1, 1, classFile);
	}

	protected ExtendsSuperInterfacesLayoutBlock(
		byte tag, int firstLineNumber, int lastLineNumber,
		int minimalLineCount, int maximalLineCount, int preferedLineCount, 
		ClassFile classFile) 
	{
		super(
			tag, firstLineNumber, lastLineNumber, 
			minimalLineCount, maximalLineCount, preferedLineCount);
		this.classFile = classFile;
	}	
}
