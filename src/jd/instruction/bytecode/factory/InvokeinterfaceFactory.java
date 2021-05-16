package jd.instruction.bytecode.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.classfile.constant.ConstantInterfaceMethodref;
import jd.exception.InvalidParameterException;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.Invokeinterface;


public class InvokeinterfaceFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,  
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		final int index = ((code[offset+1] & 255) << 8) | (code[offset+2] & 255);
		final int count = code[offset+3] & 255;

		ConstantInterfaceMethodref cimr = 
			classFile.getConstantPool().getConstantInterfaceMethodref(index);
		if (cimr == null)
			throw new InvalidParameterException(
					"Invalid ConstantInterfaceMethodref index");
		
		int nbrOfParameters = cimr.getNbrOfParameters();
		ArrayList<Instruction> args = new ArrayList<Instruction>(nbrOfParameters);
		
		for (int i=nbrOfParameters; i>0; --i)
			args.add(stack.pop());
		
		Collections.reverse(args);
		
		Instruction objectref = stack.pop();
		
		final Instruction instruction = new Invokeinterface(
			opcode, offset, lineNumber, index, count, objectref, args);
			
		if (cimr.returnAResult())
			stack.push(instruction);
		else
			list.add(instruction);
		
		listForAnalyze.add(instruction);
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
