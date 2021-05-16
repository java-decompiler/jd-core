package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class ThrowsLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	public Method method;
	public boolean nullCodeFlag;
	
	public ThrowsLayoutBlock(
		ClassFile classFile, Method method, boolean nullCodeFlag) 
	{
		super(
			LayoutBlockConstants.THROWS, 
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 1, 1);
		this.classFile = classFile;		
		this.method = method;
		this.nullCodeFlag = nullCodeFlag;
	}
}
