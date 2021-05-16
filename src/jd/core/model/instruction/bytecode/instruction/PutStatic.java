package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.instruction.bytecode.instruction.attribute.ValuerefAttribute;


public class PutStatic extends GetStatic implements ValuerefAttribute
{
	public Instruction valueref;

	public PutStatic(
		int opcode, int offset, int lineNumber, int index, Instruction valueref)
	{
		super(opcode, offset, lineNumber, index);
		this.valueref = valueref;
	}

	public Instruction getValueref() 
	{
		return valueref;
	}

	public void setValueref(Instruction valueref) 
	{
		this.valueref = valueref;
	}
}
