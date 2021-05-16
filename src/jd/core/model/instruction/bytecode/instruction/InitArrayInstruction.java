package jd.core.model.instruction.bytecode.instruction;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;


public class InitArrayInstruction extends Instruction 
{
	public Instruction newArray;
	public List<Instruction> values;
	
	public InitArrayInstruction(
			int opcode, int offset, int lineNumber, 
			Instruction newArray, List<Instruction> values)
	{
		super(opcode, offset, lineNumber);
		this.newArray = newArray;
		this.values = values;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		return this.newArray.getReturnedSignature(constants, localVariables);
	}
}
