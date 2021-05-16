package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class InstructionLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	public Method method;
	public Instruction instruction;
	public int firstOffset;
	public int lastOffset;
	
	public InstructionLayoutBlock(
		byte tag, int firstLineNumber, int lastLineNumber, 
		int minimalLineCount, int maximalLineCount, int preferedLineCount,
		ClassFile classFile, 
		Method method, 
		Instruction instruction,
		int firstOffset, int lastOffset) 
	{
		super(
			tag, firstLineNumber, lastLineNumber, 
			minimalLineCount, maximalLineCount, preferedLineCount);
		this.classFile = classFile;
		this.method = method;
		this.instruction = instruction;
		this.firstOffset = firstOffset;
		this.lastOffset = lastOffset;
	}
}
