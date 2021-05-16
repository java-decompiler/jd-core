package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class GenericTypeNameLayoutBlock extends TypeNameLayoutBlock 
{
	public String signature;
	
	public GenericTypeNameLayoutBlock(
		ClassFile classFile, String signature) 
	{
		super(
			LayoutBlockConstants.GENERIC_TYPE_NAME, 
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 0, 0, classFile);
		this.signature = signature;
	}
}
