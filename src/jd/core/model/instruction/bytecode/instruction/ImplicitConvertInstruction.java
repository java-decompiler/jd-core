package jd.core.model.instruction.bytecode.instruction;



public class ImplicitConvertInstruction extends ConvertInstruction 
{
	public ImplicitConvertInstruction(
			int opcode, int offset, int lineNumber, 
			Instruction value, String signature)
	{
		super(opcode, offset, lineNumber, value, signature);
	}
	
	public int getPriority()
	{
		return this.value.getPriority();
	}
}
