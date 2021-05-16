package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.AThrow;
import jd.instruction.bytecode.instruction.Instruction;


public class AThrowFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;

		list.add(new AThrow(opcode, offset, lineNumber, stack.pop()));
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
