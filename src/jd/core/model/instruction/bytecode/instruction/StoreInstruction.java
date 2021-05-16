package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.instruction.bytecode.instruction.attribute.ValuerefAttribute;


public class StoreInstruction 
	extends LoadInstruction implements ValuerefAttribute
{
	public Instruction valueref;

	public StoreInstruction(
			int opcode, int offset, int lineNumber, int index, 
			String signature, Instruction valueref)
	{
		super(opcode, offset, lineNumber, index, signature);
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
