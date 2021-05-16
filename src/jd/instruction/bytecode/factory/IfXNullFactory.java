package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.DupLoad;
import jd.instruction.bytecode.instruction.IfInstruction;
import jd.instruction.bytecode.instruction.Instruction;


public class IfXNullFactory extends InstructionFactory
{
	public int cmp;

	public IfXNullFactory(int cmp)
	{
		this.cmp = cmp;
	}
		
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,  
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		final int branch = 
			(short)(((code[offset+1] & 255) << 8) | (code[offset+2] & 255));
		
		list.add(new IfInstruction(
			ByteCodeConstants.IFXNULL, offset, lineNumber, 
			this.cmp, stack.pop(), branch));

		if (!stack.isEmpty())
		{
			Instruction instruction = stack.lastElement();			
			if (instruction.opcode == ByteCodeConstants.DUPLOAD)
			{
				int nextOffset = 
					offset + ByteCodeConstants.NO_OF_OPERANDS[opcode] + 1;
				
				if (nextOffset < code.length)
				{
					switch (code[nextOffset] & 255)
					{
					case ByteCodeConstants.POP:
					case ByteCodeConstants.ARETURN:
						// Duplicate 'DupLoad' instruction used by 
						// DotClass118BReconstructor
						DupLoad dp = (DupLoad)instruction;
						stack.push(new DupLoad(
							dp.opcode, dp.offset, dp.lineNumber, dp.dupStore));
					}
				}
			}
		}
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
