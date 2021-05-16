package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

public class DupLoad extends Instruction 
{
	public DupStore dupStore;
	
	public DupLoad(int opcode, int offset, int lineNumber, DupStore dupStore)
	{
		super(opcode, offset, lineNumber);
		this.dupStore = dupStore;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if (dupStore == null)
			throw new RuntimeException("DupLoad without DupStore");
		
		return dupStore.getReturnedSignature(constants, localVariables);
	}
}
