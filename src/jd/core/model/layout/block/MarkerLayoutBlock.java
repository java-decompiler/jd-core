package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class MarkerLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	public MarkerLayoutBlock other;
	
	public MarkerLayoutBlock(byte tag, ClassFile classFile) 
	{
		super(
			tag, 
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 0, 0);
		this.classFile = classFile;
		this.other = null;
	}
}
