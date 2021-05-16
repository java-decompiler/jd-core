package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ImplicitConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class ImplicitConvertInstructionFactory extends InstructionFactory
{
	private String signature;
	
	public ImplicitConvertInstructionFactory(String signature)
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

		stack.push(new ImplicitConvertInstruction(
				ByteCodeConstants.IMPLICITCONVERT, offset, lineNumber, 
				stack.pop(), this.signature));
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
