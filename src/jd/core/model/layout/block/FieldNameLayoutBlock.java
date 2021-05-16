package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Field;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class FieldNameLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	public Field field;
	
	public FieldNameLayoutBlock(ClassFile classFile, Field field) 
	{
		super(
			LayoutBlockConstants.FIELD_NAME,
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 0, 0);
		this.classFile = classFile;
		this.field = field;
	}
}
