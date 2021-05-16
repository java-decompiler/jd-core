package jd.core.model.instruction.bytecode.instruction;

import java.util.List;



public class Invokespecial extends InvokeNoStaticInstruction 
{
	public Invokespecial(int opcode, int offset, int lineNumber, int index, 
			             Instruction objectref, List<Instruction> args)
	{
		super(opcode, offset, lineNumber, index, objectref, args);
	}
}
