package jd.core.process.analyzer.instruction.fast.reconstructor;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.process.analyzer.instruction.bytecode.ComparisonInstructionAnalyzer;
import jd.core.process.analyzer.instruction.fast.visitor.ReplaceInstructionVisitor;


/*
 * Reconstruction des operateurs ternaires.
 * Contenu de la liste d'instructions :
 *  offset-1) If instruction (IF || IFCMP || IFXNULL || COMPLEXIF)
 *    offset) TernaryOpStore
 *  offset+1) Goto
 */
public class TernaryOpReconstructor 
{
	public static void Reconstruct(List<Instruction> list)
	{
		int length = list.size();
		
		for (int index=1; index<length; ++index)
		{
			Instruction i = list.get(index);
			
			if ((i.opcode == ByteCodeConstants.TERNARYOPSTORE) && 
				(index+2<length))
			{
				// Search test
				Instruction gi = list.get(index+1);
				Instruction afterGi = list.get(index+2);
				Instruction test = null;
				int indexTest = index;
				
				while (indexTest-- > 0)
				{
					Instruction instruction = list.get(indexTest);
					int opcode = instruction.opcode;
					
					if ((opcode == ByteCodeConstants.IF) ||
						(opcode == ByteCodeConstants.IFCMP) ||
						(opcode == ByteCodeConstants.IFXNULL) ||
						(opcode == ByteCodeConstants.COMPLEXIF))
					{
						int jumpOffset = 
							((BranchInstruction)instruction).GetJumpOffset();
						if ((gi.offset < jumpOffset) && 
							(jumpOffset <= afterGi.offset))
						{
							test = instruction;
							break;
						}
					}
				}
				
				if (test == null)
					continue;
				
				TernaryOpStore value1 = (TernaryOpStore)i;
				
				ComparisonInstructionAnalyzer.InverseComparison(test);
				
				TernaryOperator fto = new TernaryOperator(
					ByteCodeConstants.TERNARYOP, 
					value1.ternaryOp2ndValueOffset, test.lineNumber,
					test, value1.objectref, null);
				
				ReplaceInstructionVisitor visitor = 
					new ReplaceInstructionVisitor(
							value1.ternaryOp2ndValueOffset, fto);
				
				int indexVisitor = index+2;
				while ((indexVisitor<length) && (visitor.getOldInstruction()==null))
					visitor.visit(list.get(indexVisitor++));
				
				fto.value2 = visitor.getOldInstruction();
				
				if (isBooleanConstant(fto.value1) && 
					isBooleanConstant(fto.value2))
				{
					if (((IConst)fto.value1).value == 0)
						ComparisonInstructionAnalyzer.InverseComparison(fto.test);
					
					visitor.init(fto.offset, fto.test);
					
					indexVisitor = index+2;
					while ((indexVisitor<length) && (visitor.getOldInstruction()==null))
						visitor.visit(list.get(indexVisitor++));
				}
				
				// Remove Goto
				list.remove(index+1);
				// Remove TernaryOpStore
				list.remove(index);
				// Remove test
				list.remove(indexTest);
				
				index -= 2;
				length -= 3;
			}
		}
	}
	
	private static boolean isBooleanConstant(Instruction instruction)
	{
		if (instruction == null)
			return false;
		
		switch (instruction.opcode)
		{
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.SIPUSH:			
			return "Z".equals(instruction.getReturnedSignature(null, null));
		default:
			return false;
		}
	}
}
