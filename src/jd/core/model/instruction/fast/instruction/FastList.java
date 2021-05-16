package jd.core.model.instruction.fast.instruction;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;


/**
 * list & while(true)
 */
public class FastList extends BranchInstruction
{
	public List<Instruction> instructions;
	
	public FastList(
		int opcode, int offset, int lineNumber, 
		int branch, List<Instruction> instructions)
	{
		super(opcode, offset, lineNumber, branch);
		this.instructions = instructions;
	}
	
	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		return null;
	}
}
