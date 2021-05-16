package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.bytecode.ByteCodeConstants;


public class IBinaryOperatorInstruction extends BinaryOperatorInstruction 
{
	public IBinaryOperatorInstruction(
			int opcode, int offset, int lineNumber, int priority, 
			String operator, Instruction value1, Instruction value2)
	{
		super(
			opcode, offset, lineNumber, priority, 
			null, operator, value1, value2);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		String signature;

		switch (this.value1.opcode)
		{
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.SIPUSH:
			signature = this.value2.getReturnedSignature(constants, localVariables);
			if (signature == null)
				signature = this.value1.getReturnedSignature(constants, localVariables);
			return signature;
		default:
			signature = this.value1.getReturnedSignature(constants, localVariables);
			if (signature == null)
				signature = this.value2.getReturnedSignature(constants, localVariables);
			return signature;
		}
	}
}
