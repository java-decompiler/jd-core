package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.IInc;
import jd.core.model.instruction.bytecode.instruction.ILoad;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class ILoadFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,  
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		int index;
		
		if (opcode == ByteCodeConstants.ILOAD)
			index = code[offset+1] & 255;
		else
			index = opcode - ByteCodeConstants.ILOAD_0;
		
		Instruction instruction = 
			new ILoad(ByteCodeConstants.ILOAD, offset, lineNumber, index);		
		
		if (stack.isEmpty() || jumps[offset])
		{		
			// Normal case
			stack.push(instruction);		
		}
		else
		{
			Instruction last = stack.lastElement();
		
			if (last.opcode == ByteCodeConstants.IINC)
			{
				if (((IInc)last).index == index) 
				{
					listForAnalyze.add(instruction);
	
					// Replace temporary IInc instruction by a pre-inc instruction				
					IInc iinc = (IInc)last;			
					stack.pop();
					stack.push(instruction = new IncInstruction(
						ByteCodeConstants.PREINC, iinc.offset, 
						iinc.lineNumber, instruction, iinc.count));
				}
				else
				{
					// Unkwown pattern. Move IInc instruction from stack to list.
					stack.pop();
					list.add(last);	
					listForAnalyze.add(last);
					// Store ILoad instruction					
					stack.push(instruction);
				}
			}
			else
			{
				// Normal case
				stack.push(instruction);	
			}
		}		

		listForAnalyze.add(instruction);

		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
