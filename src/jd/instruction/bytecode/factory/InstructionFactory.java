package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.instruction.Instruction;


public abstract class InstructionFactory 
{
	public abstract int create(
			ClassFile classFile, 
			Method method, 
			List<Instruction> list, 
			List<Instruction> listForAnalyze,
			Stack<Instruction> stack,
			byte[] code, 
			int offset, 
			int lineNumber,
			boolean[] jumps);
}
