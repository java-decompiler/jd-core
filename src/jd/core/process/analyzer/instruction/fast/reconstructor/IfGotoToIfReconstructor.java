package jd.core.process.analyzer.instruction.fast.reconstructor;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.process.analyzer.instruction.bytecode.ComparisonInstructionAnalyzer;


/**
 * Transformation de l'ensemble 'if-break' en simple 'if':
 * 
 * 35: ifge +6 -> 41
 * 38: goto +56 -> 94
 * 41: ...
 * 
 *  to
 *  
 * 35: ifLT +59 -> 94
 * 41: ...
 *  
 */
public class IfGotoToIfReconstructor 
{	
	public static void Reconstruct(List<Instruction> list)
	{
		int length = list.size();
		if (length < 3)
			return;
		
		int index = length-2;
		
		while (index-- > 0)
		{
			Instruction i = list.get(index);
			
			switch (i.opcode)
			{
			case ByteCodeConstants.IF:
			case ByteCodeConstants.IFCMP:
			case ByteCodeConstants.IFXNULL:
			case ByteCodeConstants.COMPLEXIF:
				BranchInstruction bi = (BranchInstruction)i;

				i = list.get(index+1);
				if (i.opcode != ByteCodeConstants.GOTO)
					continue;
				
				Goto g = (Goto)i;
				
				i = list.get(index+2);
				int jumpOffset = bi.GetJumpOffset();
				
				if ((jumpOffset <= g.offset) || (i.offset < jumpOffset))
					continue;
				
				// Setup BranchInstruction
				bi.branch = g.GetJumpOffset() - bi.offset;
				ComparisonInstructionAnalyzer.InverseComparison(bi);
				// Delete Goto
				list.remove(index+1);
			}
		}
	}
}