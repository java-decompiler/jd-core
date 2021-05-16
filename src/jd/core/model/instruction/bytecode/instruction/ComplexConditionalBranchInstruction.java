package jd.core.model.instruction.bytecode.instruction;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.fast.FastConstants;


public class ComplexConditionalBranchInstruction extends ConditionalBranchInstruction
{
	public List<Instruction> instructions;
	
	public ComplexConditionalBranchInstruction(
			int opcode, int offset, int lineNumber, int cmp, 
			List<Instruction> instructions, int branch)
	{
		super(opcode, offset, lineNumber, cmp, branch);
		this.instructions = instructions;
	}	
	
	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		return null;
	}
	
	public int getPriority()
	{
		return (this.cmp == FastConstants.CMP_AND) ? 12 : 13;
	}
}
