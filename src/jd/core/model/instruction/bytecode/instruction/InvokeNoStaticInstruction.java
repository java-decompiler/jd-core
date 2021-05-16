package jd.core.model.instruction.bytecode.instruction;

import java.util.List;

public abstract class InvokeNoStaticInstruction extends InvokeInstruction 
{
	public Instruction objectref;
	
	public InvokeNoStaticInstruction(
			int opcode, int offset, int lineNumber, int index, 
			Instruction objectref, List<Instruction> args)
	{
		super(opcode, offset, lineNumber, index, args);
		this.objectref = objectref;
	}
}