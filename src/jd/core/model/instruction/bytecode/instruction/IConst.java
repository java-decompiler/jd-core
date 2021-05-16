package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

/**
 * Pseudo type 'X' correspond à char, byte, short ou int ...
 */
public class IConst extends ConstInstruction 
{
	public String signature;	

	public IConst(int opcode, int offset, int lineNumber, int value)
	{
		super(opcode, offset, lineNumber, value);
		
		/* Definition de la signature:
		 * 'X' si TBF_INT_INT|TBF_INT_SHORT|TBF_INT_BYTE|TBF_INT_CHAR|TBF_INT_BOOLEAN est possible
		 * 'Y' si TBF_INT_INT|TBF_INT_SHORT|TBF_INT_BYTE|TBF_INT_CHAR est possible
		 * 'C' si TBF_INT_INT|TBF_INT_SHORT|TBF_INT_CHAR est possible
		 * 'B' si TBF_INT_INT|TBF_INT_SHORT|TBF_INT_BYTE est possible
		 * 'S' si TBF_INT_INT|TBF_INT_SHORT est possible
		 * 'I' si TBF_INT_INT est possible
		 */

		if (value < 0)
		{
			if (value >= Byte.MIN_VALUE)
				this.signature = "B";		
			else if (value >= Short.MIN_VALUE)
				this.signature = "S";				
			else 
				this.signature = "I";				
		}
		else
		{
			if (value <= 1)
				this.signature = "X";				
			else if (value <= Byte.MAX_VALUE)
				this.signature = "Y";				
			else if (value <= Character.MAX_VALUE)
				this.signature = "C";				
			else if (value <= Short.MAX_VALUE)
				this.signature = "S";				
			else 
				this.signature = "I";				
		}
	}
	
	public String getSignature() 
	{
		return signature;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return this.signature;
	}

	public void setReturnedSignature(String signature) 
	{
		this.signature = signature;
	}
}
