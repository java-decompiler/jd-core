package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.DupStore;
import jd.instruction.bytecode.instruction.Instruction;


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

		DupStore dupStore = new DupStore(
			ByteCodeConstants.DUPSTORE, offset, lineNumber, i1);
		
		list.add(dupStore);	

		String signature = i2.getReturnedSignature(
				classFile.getConstantPool(), null);
		
		if ("J".equals(signature) || "D".equals(signature))
		{
			stack.push(dupStore.getDupLoad1());
			stack.push(i2);
			stack.push(dupStore.getDupLoad2());			
		}
		else
		{
			Instruction i3 = stack.pop();

			stack.push(dupStore.getDupLoad1());
			stack.push(i3);
			stack.push(i2);
			stack.push(dupStore.getDupLoad2());
		}	
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
