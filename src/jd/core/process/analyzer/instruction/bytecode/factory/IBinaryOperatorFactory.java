package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.IBinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class IBinaryOperatorFactory extends InstructionFactory
{
	protected int priority;
	protected String operator;
	
	public IBinaryOperatorFactory(int priority, String operator)
	{
		this.priority = priority;
		this.operator = operator;
	}
	
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		final Instruction i2 = stack.pop();
		final Instruction i1 = stack.pop();	
		
		final Instruction instruction = new IBinaryOperatorInstruction(
			ByteCodeConstants.BINARYOP, offset, lineNumber, this.priority, 
			this.operator, i1, i2);
			
		stack.push(instruction);
		listForAnalyze.add(instruction);
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
