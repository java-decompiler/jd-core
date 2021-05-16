package jd.core.model.instruction.fast.instruction;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;


/**
 * while, do-while & if
 */
public class FastSwitch extends BranchInstruction
{
	public Instruction test;
	public Pair[] pairs;

	public FastSwitch(
		int opcode, int offset, int lineNumber, int branch, 
		Instruction test, Pair[] pairs)
	{
		super(opcode, offset, lineNumber, branch);
		this.test = test;
		this.pairs = pairs;
	}	

	public static class Pair implements Comparable<Pair>
	{
		private boolean defaultFlag;
		private int key;
		private int offset;
		private List<Instruction> instructions;
		
		public Pair(boolean defaultFlag, int key, int offset) 
		{
			this.defaultFlag = defaultFlag;
			this.key = key;
			this.offset = offset;
			this.instructions = null;
		}
		
		public boolean isDefault() 
		{
			return defaultFlag;
		}
		
		public int getKey() 
		{
			return key;
		}
		public void setKey(int key) 
		{
			this.key = key;
		}
		
		public int getOffset() 
		{
			return offset;
		}
		
		public List<Instruction> getInstructions() 
		{
			return instructions;
		}
		public void setInstructions(List<Instruction> instructions) 
		{
			this.instructions = instructions;
		}

		public int compareTo(Pair p)
		{
			int diffOffset = this.offset - p.offset;
			
			if (diffOffset != 0)
				return diffOffset;
			
			return this.isDefault() ? 1 : p.isDefault() ? -1 : 
				(this.key - p.key);
		}
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables)
	{
		return null;
	}
}
