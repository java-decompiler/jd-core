package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.fast.instruction.FastSwitch;

public class CaseEnumLayoutBlock extends CaseLayoutBlock 
{
	public int switchMapKeyIndex;
	
	public CaseEnumLayoutBlock(
		ClassFile classFile, Method method,
		FastSwitch fs, int firstIndex, int lastIndex, 
		int switchMapKeyIndex) 
	{
		super(
			LayoutBlockConstants.FRAGMENT_CASE_ENUM,
			classFile, method, fs, 
			firstIndex, lastIndex, lastIndex-firstIndex);
		
		this.switchMapKeyIndex = switchMapKeyIndex;
	}
}
