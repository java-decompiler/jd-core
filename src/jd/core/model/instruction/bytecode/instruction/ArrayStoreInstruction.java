package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.instruction.bytecode.instruction.attribute.ValuerefAttribute;


public class ArrayStoreInstruction 
	extends ArrayLoadInstruction implements ValuerefAttribute
{
	public Instruction valueref;
	
	public ArrayStoreInstruction(
			int opcode, int offset, int lineNumber, Instruction arrayref, 
			Instruction indexref, String signature, Instruction valueref)
	{
		super(opcode, offset, lineNumber, arrayref, indexref, signature);
		this.valueref = valueref;
	}

	public Instruction getValueref() 
	{
		return this.valueref;
	}

	public void setValueref(Instruction valueref) 
	{
		this.valueref = valueref;	
	}
}
