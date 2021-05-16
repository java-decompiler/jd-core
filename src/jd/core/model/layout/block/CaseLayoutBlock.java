package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.fast.instruction.FastSwitch;


public class CaseLayoutBlock extends LayoutBlock 
{
	public ClassFile classFile;
	public Method method;
	public FastSwitch fs;
	public int firstIndex;
	public int lastIndex;
	
	public CaseLayoutBlock(
		byte tag, ClassFile classFile, Method method, 
		FastSwitch fs, int firstIndex, int lastIndex) 
	{
		this(
			tag, classFile, method, fs, 
			firstIndex, lastIndex, lastIndex-firstIndex);
	}
	
	protected CaseLayoutBlock(
		byte tag, ClassFile classFile, Method method, FastSwitch fs, 
		int firstIndex, int lastIndex, int preferedLineCount) 
	{
		super(
			tag, 
			Instruction.UNKNOWN_LINE_NUMBER, 
			Instruction.UNKNOWN_LINE_NUMBER, 
			0, preferedLineCount, preferedLineCount);
		
		this.classFile = classFile;
		this.method = method;
		this.fs = fs;
		this.firstIndex = firstIndex;
		this.lastIndex = lastIndex;
	}
}
