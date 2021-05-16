package jd.instruction.bytecode.instruction;

import jd.Constants;
import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;
import jd.classfile.constant.ConstantValue;

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
		
		return (cv.tag == Constants.CONSTANT_Double) ? "D" : "J";
	}
}
