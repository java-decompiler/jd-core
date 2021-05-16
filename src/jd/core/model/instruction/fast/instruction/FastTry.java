package jd.core.model.instruction.fast.instruction;

import java.util.List;

import jd.core.model.instruction.bytecode.instruction.Instruction;


/**
 * try-catch-finally
 */
public class FastTry extends FastList
{
	public List<FastCatch>   catches;
	public List<Instruction> finallyInstructions;
	
	public FastTry(
			int opcode, int offset, int lineNumber, int branch, 
			List<Instruction> instructions, List<FastCatch> catches, 
			List<Instruction> finallyInstructions)
	{
		super(opcode, offset, lineNumber, branch, instructions);
		this.catches = catches;
		this.finallyInstructions = finallyInstructions;
	}
	
	public static class FastCatch
	{
		public int offset;
		public int exceptionOffset;
		public int exceptionTypeIndex;
		public int otherExceptionTypeIndexes[];
		public int localVarIndex;
		public List<Instruction> instructions;
		
		public FastCatch(
				int offset, int exceptionOffset, int exceptionTypeIndex, 
				int otherExceptionTypeIndexes[], int localVarIndex, 
				List<Instruction> instructions)
		{		
			this.offset = offset;
			this.exceptionOffset = exceptionOffset;
			this.exceptionTypeIndex = exceptionTypeIndex;
			this.otherExceptionTypeIndexes = otherExceptionTypeIndexes;
			this.localVarIndex = localVarIndex;
			this.instructions = instructions;
		}
	}
}
