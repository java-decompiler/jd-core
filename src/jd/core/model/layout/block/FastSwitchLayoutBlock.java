package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.fast.instruction.FastSwitch;


public class FastSwitchLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	public Method method;
	public FastSwitch fs;
	
	public FastSwitchLayoutBlock(
		byte tag, int firstLineNumber, int lastLineNumber, 
		int minimalLineCount, int maximalLineCount, int preferedLineCount,
		ClassFile classFile, Method method, FastSwitch fs) 
	{
		super(
			tag, firstLineNumber, lastLineNumber, 
			minimalLineCount, maximalLineCount, preferedLineCount);
		
		this.classFile = classFile;
		this.method = method;
		this.fs = fs;
	}
}
