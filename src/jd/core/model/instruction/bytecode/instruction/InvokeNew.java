package jd.core.model.instruction.bytecode.instruction;

import java.util.List;

import jd.core.model.instruction.fast.FastConstants;


public class InvokeNew extends InvokeInstruction 
{
	public int enumValueFieldRefIndex;

	public InvokeNew(
		int opcode, int offset, int lineNumber,
		int index, List<Instruction> args)
	{
		super(opcode, offset, lineNumber, index, args);
		this.enumValueFieldRefIndex = 0;
	}

	public void transformToEnumValue(GetStatic getStatic)
	{
		this.opcode = FastConstants.ENUMVALUE;
		this.enumValueFieldRefIndex = getStatic.index;
	}
}
