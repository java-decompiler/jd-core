package jd.instruction.bytecode.instruction.attribute;

import jd.instruction.bytecode.instruction.Instruction;

public interface ValuerefAttribute 
{
	public Instruction getValueref();
	
	public void setValueref(Instruction valueref);
}
