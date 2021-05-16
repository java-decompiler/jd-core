package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

public class ANewArray extends IndexInstruction
{
	public Instruction dimension;
	
	public ANewArray(
		int opcode, int offset, int lineNumber, 
		int index, Instruction dimension)
	{
		super(opcode, offset, lineNumber, index);
		this.dimension = dimension;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if (constants == null)
			return null;
		
		String str = constants.getConstantClassName(this.index);
		
		if (str.length() == 0)
		{
			return null;
		}
		else if (str.charAt(0) == '[')
		{
			return "[" + constants.getConstantClassName(this.index);
		}
		else
		{		
			return "[L" + constants.getConstantClassName(this.index) + ';';
		}
	}
}
