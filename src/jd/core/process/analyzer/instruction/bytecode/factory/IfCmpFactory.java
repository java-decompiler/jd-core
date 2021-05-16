package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class IfCmpFactory extends InstructionFactory
{
	protected int cmp;
	
	public IfCmpFactory(int cmp)
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
		final Instruction value2 = stack.pop();
		final Instruction value1 = stack.pop();
		
		final Instruction newInstruction = new IfCmp(
			ByteCodeConstants.IFCMP, offset, lineNumber, 
			this.cmp, value1, value2, branch);
			
		list.add(newInstruction);
		listForAnalyze.add(newInstruction);

		return ByteCodeConstants.NO_OF_OPERANDS[opcode];			
	}
}
