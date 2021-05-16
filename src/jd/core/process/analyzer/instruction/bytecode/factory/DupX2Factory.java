package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class DupX2Factory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze, 
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		Instruction i1 = stack.pop();
		Instruction i2 = stack.pop();

		DupStore dupStore1 = new DupStore(
			ByteCodeConstants.DUPSTORE, offset, lineNumber, i1);
		
		list.add(dupStore1);	

		String signature2 = i2.getReturnedSignature(
				classFile.getConstantPool(), null);
		
		if ("J".equals(signature2) || "D".equals(signature2))
		{
			// ..., value2, value1 => ..., value1, value2, value1
			stack.push(dupStore1.getDupLoad1());
			stack.push(i2);
			stack.push(dupStore1.getDupLoad2());			
		}
		else
		{
			// ..., value3, value2, value1 => ..., value1, value3, value2, value1
			Instruction i3 = stack.pop();

			stack.push(dupStore1.getDupLoad1());
			stack.push(i3);
			stack.push(i2);
			stack.push(dupStore1.getDupLoad2());
		}	
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
