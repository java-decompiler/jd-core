package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

public class MultiANewArray extends IndexInstruction
{
	public Instruction dimensions[];
	
	public MultiANewArray(
		int opcode, int offset, int lineNumber, 
		int index, Instruction[] dimensions)
	{
		super(opcode, offset, lineNumber, index);
		this.dimensions = dimensions;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if (constants == null)
			return null;
		
		return constants.getConstantClassName(this.index);
	}
}
