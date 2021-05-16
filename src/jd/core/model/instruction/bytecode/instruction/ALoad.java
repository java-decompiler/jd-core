package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.util.StringConstants;

public class ALoad extends LoadInstruction 
{
	public ALoad(int opcode, int offset, int lineNumber, int index)
	{
		super(opcode, offset, lineNumber, index, null);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if ((constants == null) || (localVariables == null))
			return null;
		
		LocalVariable lv = localVariables.getLocalVariableWithIndexAndOffset(this.index, this.offset);
		
		if ((lv != null) && (lv.signature_index > 0))
			return constants.getConstantUtf8(lv.signature_index);
		
		return StringConstants.INTERNAL_OBJECT_SIGNATURE;
	}
}
