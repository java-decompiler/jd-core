package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class DupFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze, 
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		Instruction i = stack.pop();
		
		// ..., value => ..., value, value
		DupStore dupStore = new DupStore(
			ByteCodeConstants.DUPSTORE, offset, lineNumber, i);
		
		list.add(dupStore);			
		stack.push(dupStore.getDupLoad1());
		stack.push(dupStore.getDupLoad2());
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
