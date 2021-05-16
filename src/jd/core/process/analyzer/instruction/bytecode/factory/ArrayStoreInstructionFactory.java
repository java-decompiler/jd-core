package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class ArrayStoreInstructionFactory extends InstructionFactory
{
	private String signature;
	
	public ArrayStoreInstructionFactory(String signature)
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
		final Instruction valueref = stack.pop();
		final Instruction index = stack.pop();
		final Instruction arrayref = stack.pop();
		final Instruction instruction = new ArrayStoreInstruction(
				ByteCodeConstants.ARRAYSTORE, offset, lineNumber, arrayref, 
				index, this.signature, valueref);
		
		list.add(instruction);
		listForAnalyze.add(instruction);
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
