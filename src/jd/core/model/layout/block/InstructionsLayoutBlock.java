package jd.core.model.layout.block;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class InstructionsLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	public Method method;
	public List<Instruction> instructions;
	public int firstIndex;
	public int lastIndex;
	public int firstOffset;
	public int lastOffset;

	public InstructionsLayoutBlock(
		int firstLineNumber, int lastLineNumber, 
		int minimalLineCount, int maximalLineCount, int preferedLineCount,
		ClassFile classFile, 
		Method method, 
		List<Instruction> instructions, 
		int firstIndex, int lastIndex,
		int firstOffset, int lastOffset) 
	{
		super(
			LayoutBlockConstants.INSTRUCTIONS, 
			firstLineNumber, lastLineNumber, 
			minimalLineCount, maximalLineCount, preferedLineCount);
		this.classFile = classFile;
		this.method = method;
		this.instructions = instructions;
		this.firstIndex = firstIndex;
		this.lastIndex = lastIndex;
		this.firstOffset = firstOffset;
		this.lastOffset = lastOffset;
	}
}
