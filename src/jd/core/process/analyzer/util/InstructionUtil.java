package jd.core.process.analyzer.util;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;




public class InstructionUtil 
{
	public static Instruction getInstructionAt(
			List<Instruction> list, int offset)
	{
		if ((list == null) || (list.size() == 0))
			return null;
		
		if (list.get(0).offset >= offset)
			return list.get(0);			
			
		int length = list.size();
		
		if (length == 1)
			return null;
		
		if (list.get(length-1).offset < offset)
			return null;
		
		int firstIndex = 0;
		int lastIndex = length-1;
		
		while (true)
		{
			int medIndex = (lastIndex + firstIndex) / 2;
			Instruction i = list.get(medIndex);
			
			if (i.offset < offset)
				firstIndex = medIndex+1;
			else if (list.get(medIndex-1).offset >= offset)
				lastIndex = medIndex-1;
			else
				return i;
		}
	}
	
	public static int getIndexForOffset(
			List<Instruction> list, int offset)
	{
		if (offset < 0)
			throw new RuntimeException("offset=" + offset);
		
		if ((list == null) || (list.size() == 0))
			return -1;
		
		if (list.get(0).offset >= offset)
			return 0;			
			
		int length = list.size();
		
		if (length == 1)
			return -1;
		
		if (list.get(length-1).offset < offset)
			return -1;
		
		int firstIndex = 0;
		int lastIndex = length-1;
		
		while (true)
		{
			int medIndex = (lastIndex + firstIndex) / 2;
			Instruction i = list.get(medIndex);
			
			if (i.offset < offset)
				firstIndex = medIndex+1;
			else if (list.get(medIndex-1).offset >= offset)
				lastIndex = medIndex-1;
			else
				return medIndex;
		}
	}
	
	public static boolean CheckNoJumpToInterval(
			List<Instruction> list, int firstIndex, int afterIndex, 
			int firstOffset, int lastOffset)
		{
			for (int index=firstIndex; index<afterIndex; index++)
			{
				Instruction i = list.get(index);
				
				switch (i.opcode)
				{
				case ByteCodeConstants.IF:
				case ByteCodeConstants.IFCMP:
				case ByteCodeConstants.IFXNULL:	
				case ByteCodeConstants.GOTO:
					int jumpOffset = ((BranchInstruction)i).GetJumpOffset();
					if ((firstOffset < jumpOffset) && (jumpOffset <= lastOffset))
						return false;
				}
			}

			return true;
		}
}
