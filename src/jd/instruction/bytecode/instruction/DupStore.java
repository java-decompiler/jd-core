package jd.instruction.bytecode.instruction;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;
import jd.instruction.bytecode.ByteCodeConstants;

public class DupStore extends Instruction 
{
	public Instruction objectref;
	public DupLoad dupLoad1;
	public DupLoad dupLoad2;
	
	public DupStore(
		int opcode, int offset, int lineNumber, Instruction objectref)
	{
		super(opcode, offset, lineNumber);
		this.objectref = objectref;
		this.dupLoad1 = new DupLoad(
				ByteCodeConstants.DUPLOAD, offset, lineNumber, this);
		this.dupLoad2 = new DupLoad(
				ByteCodeConstants.DUPLOAD, offset, lineNumber, this);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		return this.objectref.getReturnedSignature(constants, localVariables);
	}

	public DupLoad getDupLoad1() 
	{
		return dupLoad1;
	}

	public DupLoad getDupLoad2() 
	{
		return dupLoad2;
	}
}
