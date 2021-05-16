package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class ArrayLoadInstructionFactory extends InstructionFactory
{
	private String signature;
	
	public ArrayLoadInstructionFactory(String signature)
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
		final Instruction index = stack.pop();
		final Instruction arrayref = stack.pop();
		final Instruction instruction = new ArrayLoadInstruction(
				ByteCodeConstants.ARRAYLOAD, offset, lineNumber, arrayref, 
				index, this.signature);
			
		stack.push(instruction);
		listForAnalyze.add(instruction);
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
