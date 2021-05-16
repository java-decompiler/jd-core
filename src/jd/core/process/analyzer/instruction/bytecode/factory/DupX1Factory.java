package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class DupX1Factory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze, 
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		Instruction i1 = stack.pop();
		Instruction i2 = stack.pop();
	
		// ..., value2, value1 => ..., value1, value2, value1
		DupStore dupStore1 = new DupStore(
			ByteCodeConstants.DUPSTORE, offset, lineNumber, i1);
			
		list.add(dupStore1);	
		stack.push(dupStore1.getDupLoad1());
		stack.push(i2);
		stack.push(dupStore1.getDupLoad2());
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
