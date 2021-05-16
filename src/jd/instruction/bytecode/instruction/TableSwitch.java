package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;


public class TableSwitch extends Switch
{
	public int low;
	public int high;
	
	public TableSwitch(
			int opcode, int offset, int lineNumber, Instruction key, 
			int defaultOffset, int[] offsets, int low, int high)
	{
		super(opcode, offset, lineNumber, key, defaultOffset, offsets);
		this.low = low;
		this.high = high;
	}
}
