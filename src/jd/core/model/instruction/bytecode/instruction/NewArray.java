package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.util.SignatureUtil;

public class NewArray extends Instruction 
{
	public int type;
	public Instruction dimension;
	
	public NewArray(
		int opcode, int offset, int lineNumber, int type, Instruction dimension)
	{
		super(opcode, offset, lineNumber);
		this.type = type;
		this.dimension = dimension;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		String signature = SignatureUtil.GetSignatureFromType(this.type);
		
		return (signature == null) ? null : "[" + signature;
	}
}
