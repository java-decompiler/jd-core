package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class Dup2X2Factory extends InstructionFactory
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
	
		String signature1 = i1.getReturnedSignature(
				classFile.getConstantPool(), method.getLocalVariables());
		String signature2 = i2.getReturnedSignature(
				classFile.getConstantPool(), method.getLocalVariables());
		
		if ("J".equals(signature1) || "D".equals(signature1))
		{
			if ("J".equals(signature2) || "D".equals(signature2))
			{
				// ..., value2, value1 => ..., value1, value2, value1
				DupStore dupStore1 = new DupStore(
						ByteCodeConstants.DUPSTORE, offset, lineNumber, i1); 
				
				list.add(dupStore1);			
				stack.push(dupStore1.getDupLoad1());
				stack.push(i2);
				stack.push(dupStore1.getDupLoad2());				
			}
			else
			{
				// ..., value3, value2, value1 => ..., value1, value3, value2, value1
				Instruction i3 = stack.pop();
				
				DupStore dupStore1 = new DupStore(
						ByteCodeConstants.DUPSTORE, offset, lineNumber, i1); 
				
				list.add(dupStore1);			
				stack.push(dupStore1.getDupLoad1());
				stack.push(i3);
				stack.push(i2);
				stack.push(dupStore1.getDupLoad2());	
			}
		}
		else
		{
			Instruction i3 = stack.pop();
			
			String signature3 = i3.getReturnedSignature(
					classFile.getConstantPool(), method.getLocalVariables());
			
			if ("J".equals(signature3) || "D".equals(signature3))
			{
				// ..., value3, value2, value1 => ..., value2, value1, value3, value2, value1
				DupStore dupStore1 = new DupStore(
					ByteCodeConstants.DUPSTORE, offset, lineNumber, i1);
				DupStore dupStore2 = new DupStore(
					ByteCodeConstants.DUPSTORE, offset, lineNumber, i2);
					
				list.add(dupStore1);	
				list.add(dupStore2);	
				
				stack.push(dupStore2.getDupLoad1());
				stack.push(dupStore1.getDupLoad1());
				stack.push(i3);
				stack.push(dupStore2.getDupLoad2());
				stack.push(dupStore1.getDupLoad2());
			}
			else
			{
				// ..., value4, value3, value2, value1 => ..., value2, value1, value4, value3, value2, value1
				Instruction i4 = stack.pop();
				
				DupStore dupStore1 = new DupStore(
					ByteCodeConstants.DUPSTORE, offset, lineNumber, i1);
				DupStore dupStore2 = new DupStore(
					ByteCodeConstants.DUPSTORE, offset, lineNumber, i2);
					
				list.add(dupStore1);	
				list.add(dupStore2);	
				
				stack.push(dupStore2.getDupLoad1());
				stack.push(dupStore1.getDupLoad1());
				stack.push(i4);
				stack.push(i3);
				stack.push(dupStore2.getDupLoad2());
				stack.push(dupStore1.getDupLoad2());
			}
		}	
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
