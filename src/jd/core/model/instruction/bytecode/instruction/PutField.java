package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.instruction.bytecode.instruction.attribute.ValuerefAttribute;


public class PutField extends GetField implements ValuerefAttribute
{
	public Instruction valueref;

	public PutField(
			int opcode, int offset, int lineNumber, int index, 
			Instruction objectref, Instruction valueref)
	{
		super(opcode, offset, lineNumber, index, objectref);
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
