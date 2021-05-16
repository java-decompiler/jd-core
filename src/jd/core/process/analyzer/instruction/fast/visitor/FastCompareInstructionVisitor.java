package jd.core.process.analyzer.instruction.fast.visitor;

import java.util.List;

import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastSynchronized;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;
import jd.core.process.analyzer.classfile.visitor.CompareInstructionVisitor;


public class FastCompareInstructionVisitor extends CompareInstructionVisitor
{
	public boolean visit(
		List<Instruction> list1, List<Instruction> list2, 
		int index1, int index2, int length)
	{
		if ((index1+length <= list1.size()) && (index2+length <= list2.size()))
		{
			while (length-- > 0)
			{
				if (!visit(list1.get(index1++), list2.get(index2++)))
					return false;
			}
		}
		
		return true;
	}
	
	public boolean visit(Instruction i1, Instruction i2)
	{
		if (i1.opcode != i2.opcode)
			return false;
		
		switch (i1.opcode)
		{
		case FastConstants.TRY:
			{
				FastTry ft1 = (FastTry)i1;
				FastTry ft2 = (FastTry)i2;
				
				int i = ft1.catches.size();
					
				if (i != ft2.catches.size())
					return false;
				
				if (ft1.finallyInstructions == null)
				{
					if (ft2.finallyInstructions != null)
						return false;
				}
				else if (ft2.finallyInstructions == null)
				{
					if (ft1.finallyInstructions != null)
						return false;
				}
				else
				{
					if (! visit(
							ft1.finallyInstructions, 
							ft2.finallyInstructions))
						return false;
				}
				
				while (i-- > 0)
				{
					FastCatch fc1 = ft1.catches.get(i);
					FastCatch fc2 = ft2.catches.get(i);
					
					if ((fc1.exceptionTypeIndex != fc2.exceptionTypeIndex) ||
						(! visit(fc1.instructions, fc2.instructions)) ||
						(! CompareExceptionTypeIndexes(
							fc1.otherExceptionTypeIndexes, fc2.otherExceptionTypeIndexes)))
						return false;
				}
				
				return visit(ft1.instructions, ft2.instructions);
			}
		case FastConstants.SYNCHRONIZED:
			{
				FastSynchronized fs1 = (FastSynchronized)i1;
				FastSynchronized fs2 = (FastSynchronized)i2;
				
				if (! visit(fs1.monitor, fs2.monitor))
					return false;
				
				return visit(fs1.instructions, fs2.instructions);
			}
		default:
			return super.visit(i1, i2);
		}
	}
	
	private static boolean CompareExceptionTypeIndexes(
		int otherExceptionTypeIndexes1[], int otherExceptionTypeIndexes2[])
	{
		if (otherExceptionTypeIndexes1 == null)
		{
			return otherExceptionTypeIndexes2 == null;
		}
		else
		{
			if (otherExceptionTypeIndexes2 == null)
				return false;
			
			int i = otherExceptionTypeIndexes1.length;
			
			if (i != otherExceptionTypeIndexes2.length)
				return false;
			
			while (i-- > 0)
			{
				if (otherExceptionTypeIndexes1[i] != otherExceptionTypeIndexes2[i])
					return false;
			}
			
			return true;
		}
	}
}
