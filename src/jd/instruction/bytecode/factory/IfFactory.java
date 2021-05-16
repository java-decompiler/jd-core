package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.IfInstruction;
import jd.instruction.bytecode.instruction.Instruction;


public class IfFactory extends InstructionFactory
{
	protected int cmp;
	
	public IfFactory(int cmp)
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
		final Instruction value = stack.pop();
		
		list.add(new IfInstruction(
			ByteCodeConstants.IF, offset, lineNumber, this.cmp, value, branch));
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
