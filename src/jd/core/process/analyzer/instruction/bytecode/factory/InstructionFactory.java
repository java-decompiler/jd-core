package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;


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
