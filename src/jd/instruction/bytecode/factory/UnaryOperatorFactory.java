package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.UnaryOperatorInstruction;


public class UnaryOperatorFactory extends InstructionFactory
{
	private int priority;
	private String signature;
	private String operator;
	
	public UnaryOperatorFactory(
			int priority, String signature, String operator)
	{
		this.priority = priority;
		this.signature = signature;
		this.operator = operator;
	}
	
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze, Stack<Instruction> stack, 
			byte[] code, int offset, int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		final Instruction i = stack.pop();
		
		stack.push(new UnaryOperatorInstruction(
			ByteCodeConstants.UNARYOP, offset, lineNumber, this.priority, 
			this.signature, this.operator, i));
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
