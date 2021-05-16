package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.model.classfile.constant.ConstantValue;

public class Ldc2W extends LdcInstruction 
{
	public Ldc2W(int opcode, int offset, int lineNumber, int index)
	{
		super(opcode, offset, lineNumber, index);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if (constants == null)
			return null;
		
		ConstantValue cv = constants.getConstantValue(this.index);
		
		if (cv == null)
			return null;
		
		return (cv.tag == ConstantConstant.CONSTANT_Double) ? "D" : "J";
	}
}
