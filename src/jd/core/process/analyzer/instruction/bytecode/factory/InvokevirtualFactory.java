package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;
import jd.core.util.InvalidParameterException;


public class InvokevirtualFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,  
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		final int index = ((code[offset+1] & 255) << 8) | (code[offset+2] & 255);
		
		ConstantMethodref cmr = 
			classFile.getConstantPool().getConstantMethodref(index);
		if (cmr == null)
			throw new InvalidParameterException(
					"Invalid ConstantMethodref index");
		
		int nbrOfParameters = cmr.getNbrOfParameters();
		ArrayList<Instruction> args = new ArrayList<Instruction>(nbrOfParameters);
		
		for (int i=nbrOfParameters; i>0; --i)
			args.add(stack.pop());
		
		Collections.reverse(args);

		Instruction objectref = stack.pop();

		final Instruction instruction = new Invokevirtual(
			opcode, offset, lineNumber, index, objectref, args);

		if (cmr.returnAResult())
			stack.push(instruction);
		else
			list.add(instruction);
		
		listForAnalyze.add(instruction);
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
