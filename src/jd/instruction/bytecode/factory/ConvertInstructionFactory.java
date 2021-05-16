package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.ConvertInstruction;
import jd.instruction.bytecode.instruction.Instruction;


public class ConvertInstructionFactory extends InstructionFactory
{
	private String signature;
	
	public ConvertInstructionFactory(String signature)
	{
		this.signature = signature;
	}
	
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;

		stack.push(new ConvertInstruction(
				ByteCodeConstants.CONVERT, offset, lineNumber, 
				stack.pop(), this.signature));
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
