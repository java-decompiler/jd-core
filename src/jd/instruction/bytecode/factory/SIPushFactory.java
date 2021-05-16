package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.SIPush;


public class SIPushFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,   
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		final int s = ((code[offset+1] & 0xFF) << 8) | (code[offset+2] & 0xFF);
		
		stack.push(new SIPush(opcode, offset, lineNumber, s));
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
