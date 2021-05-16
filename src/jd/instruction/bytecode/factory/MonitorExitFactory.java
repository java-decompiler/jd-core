package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.MonitorExit;


public class MonitorExitFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,  
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;

		MonitorExit me = new MonitorExit(opcode, offset, lineNumber, stack.pop());
		list.add(me);
		listForAnalyze.add(me);
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
