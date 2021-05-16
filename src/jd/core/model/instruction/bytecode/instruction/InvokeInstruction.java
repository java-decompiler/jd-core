package jd.core.model.instruction.bytecode.instruction;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.constant.ConstantMethodref;


public abstract class InvokeInstruction extends IndexInstruction
{
	public List<Instruction> args;
	
	public InvokeInstruction(
		int opcode, int offset, int lineNumber, 
		int index, List<Instruction> args)
	{
		super(opcode, offset, lineNumber, index);
		this.args = args;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if (constants == null)
			return null;
		
		ConstantMethodref cmr = constants.getConstantMethodref(this.index);
		
		return cmr.getReturnedSignature();
	}

	public List<String> getListOfParameterSignatures(ConstantPool constants) 
	{
		if (constants == null)
			return null;
		
		ConstantMethodref cmr = constants.getConstantMethodref(this.index);
		
		return cmr.getListOfParameterSignatures();
	}
}