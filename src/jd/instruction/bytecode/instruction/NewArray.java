package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;
import jd.classfile.analyzer.SignatureAnalyzer;

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
		String signature = SignatureAnalyzer.GetSignatureFromType(this.type);
		
		return (signature == null) ? null : "[" + signature;
	}
}
