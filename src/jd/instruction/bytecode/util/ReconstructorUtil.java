package jd.instruction.bytecode.util;

import java.util.List;

import jd.instruction.bytecode.instruction.DupStore;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.fast.visitor.ReplaceDupLoadVisitor;


public class ReconstructorUtil 
{
	public static Instruction ReplaceDupLoad(
			List<Instruction> list, int index, 
			DupStore dupStore, Instruction newInstruction)
	{
		ReplaceDupLoadVisitor visitor = 
			new ReplaceDupLoadVisitor(dupStore, newInstruction);
		int length = list.size();
		
		for (int i=index; i<length; i++)
		{
			visitor.visit(list.get(i));
			if (visitor.getParentFound() != null)
				break;
		}
		
		return visitor.getParentFound();
	}
}
