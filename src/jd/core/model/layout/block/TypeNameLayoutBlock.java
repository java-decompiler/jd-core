package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class TypeNameLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	
	public TypeNameLayoutBlock(ClassFile classFile) 
	{
		this(
			LayoutBlockConstants.TYPE_NAME, 
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 0, 0, classFile);
	}
	
	protected TypeNameLayoutBlock(
		byte tag, int firstLineNumber, int lastLineNumber,
		int minimalLineCount, int maximalLineCount,
		int preferedLineCount, ClassFile classFile) 
	{
		super(
			tag, firstLineNumber, lastLineNumber, 
			minimalLineCount, maximalLineCount, preferedLineCount);
		this.classFile = classFile;		
	}
}
