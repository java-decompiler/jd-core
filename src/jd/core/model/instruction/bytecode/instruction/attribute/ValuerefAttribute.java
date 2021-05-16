package jd.core.model.instruction.bytecode.instruction.attribute;

import jd.core.model.instruction.bytecode.instruction.Instruction;

public interface ValuerefAttribute 
{
	public Instruction getValueref();
	
	public void setValueref(Instruction valueref);
}
