package jd.instruction.bytecode.instruction;

import java.util.List;

import jd.classfile.ConstantPool;
import jd.classfile.LocalVariables;



public class InvokeNew extends Instruction 
{
	public int classIndex;
	public int classMethodref;
	public List<Instruction> args;

	public InvokeNew(
		int opcode, int offset, int lineNumber, int classIndex, 
		int classMethodref, List<Instruction> args)
	{
		super(opcode, offset, lineNumber);
		this.classIndex = classIndex;
		this.classMethodref = classMethodref;
		this.args = args;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		if (constants == null)
			return null;
		
		return 'L' + constants.getConstantClassName(this.classIndex) + ';';
	}
}
