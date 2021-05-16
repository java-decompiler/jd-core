package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;

public class IInc extends IndexInstruction 
{
	public int count;
	
	public IInc(int opcode, int offset, int lineNumber, int index, int count)
	{
		super(opcode, offset, lineNumber, index);
		this.count = count;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		if ((constants == null) || (localVariables == null))
			return null;
		
		LocalVariable lv = localVariables.getLocalVariableWithIndexAndOffset(this.index, this.offset);
		
		if ((lv == null) || (lv.signature_index <= 0))
			return null;
		
		return constants.getConstantUtf8(lv.signature_index);
	}

	public int getPriority()
	{
		if ((this.count == 1) || (this.count == -1))
			// Operator '++' or '--'
			return 2;
		// Operator '+=' or '-='				
		return 14;
	}
}
