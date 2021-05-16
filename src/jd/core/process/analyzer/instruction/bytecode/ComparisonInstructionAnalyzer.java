package jd.core.process.analyzer.instruction.bytecode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;


/**
 * Aggrege les instructions 'if'
 */
public class ComparisonInstructionAnalyzer 
{	
	/*
	 *                            debut de liste        fin de liste
	 *                            |                                |
	 * Liste    ... --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ...
	 */
	public static void Aggregate(List<Instruction> list)
	{
		int afterOffest = -1;
		int index = list.size();
		
		while (index-- > 0)
		{
			Instruction instruction = list.get(index);
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.IF:
			case ByteCodeConstants.IFCMP:
			case ByteCodeConstants.IFXNULL:
				if (index > 0)
				{
					Instruction prevI = list.get(index-1);

					switch (prevI.opcode)
					{
					case ByteCodeConstants.IF:
					case ByteCodeConstants.IFCMP:
					case ByteCodeConstants.IFXNULL:	
					case ByteCodeConstants.GOTO:
						{
							BranchInstruction bi     = (BranchInstruction)instruction;
							BranchInstruction prevBi = (BranchInstruction)prevI;
							
							int prevBiJumpOffset = prevBi.GetJumpOffset();
							
							// Le 2eme if appartient-il au meme bloc que le 1er ?
							if ((prevBiJumpOffset == bi.GetJumpOffset()) ||
								((prevBi.branch > 0) && (prevBiJumpOffset <= afterOffest)))
							{
								// Oui
								// Test complexe : plusieurs instructions byte-code de test
								index = AnalyzeIfInstructions(
									list, index, bi, afterOffest);
							}
						}
						break;
					}	
				}
			}
			
			afterOffest = instruction.offset;
		}
	}
	
	/*
	 *                            debut de liste        fin de liste
	 *                            |               index            |
	 *                            |                   |            |
	 * Liste    ... --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ...
	 *                                            if  if  ?
	 *                                            |   |   |
	 * Offsets                                    |   |   afterLastBiOffset
	 * Instruction                    (if) prevLast   last (if)
	 */
	private static int AnalyzeIfInstructions(
			List<Instruction> list, int index, 
			BranchInstruction lastBi, int afterOffest)
	{
		int arrayLength = list.get(list.size()-1).offset;
		boolean[] offsetToPreviousGotoFlag = new boolean[arrayLength];
		boolean[] inversedTernaryOpLogic = new boolean[arrayLength];
		
		// Recherche de l'indexe de la premiere instruction 'if' du bloc et 
		// initialisation de 'offsetToPreviousGotoFlag'
		int firstIndex = SearchFirstIndex(
			list, index, lastBi, afterOffest, 
			offsetToPreviousGotoFlag, inversedTernaryOpLogic);
		
		firstIndex = ReduceFirstIndex(list, firstIndex, index);
		
		if (firstIndex < index)
		{
			// Extraction des instructions de test formant un bloc
			List<Instruction> branchInstructions = 
				new ArrayList<Instruction>(index - firstIndex + 1);
	
			branchInstructions.add(lastBi);
			while (index > firstIndex)
				branchInstructions.add(list.remove(--index));	

			Collections.reverse(branchInstructions);
			list.set(index, CreateIfInstructions(
				offsetToPreviousGotoFlag, inversedTernaryOpLogic,
				branchInstructions, lastBi));
		}
		
		return index;
	}
	
	private static int ReduceFirstIndex(
		List<Instruction> list, int firstIndex, int lastIndex)
	{
		int firstOffset = (firstIndex == 0) ? 0 : list.get(firstIndex-1).offset;
		int newFirstOffset = firstOffset;
		int lastOffset = list.get(lastIndex).offset;
		
		// Reduce 'firstIndex' with previous instructions
		int index = firstIndex;
		while (index-- > 0)
		{
			Instruction i = list.get(index);
			
			switch (i.opcode)
			{
			case ByteCodeConstants.IF:
			case ByteCodeConstants.IFCMP:
			case ByteCodeConstants.IFXNULL:	
			case ByteCodeConstants.GOTO:
				int jumpOffset = ((BranchInstruction)i).GetJumpOffset();
				if ((newFirstOffset < jumpOffset) && (jumpOffset <= lastOffset))
					newFirstOffset = jumpOffset;
			}
		}
		
		// Reduce 'firstIndex' with next instructions
		index = list.size();
		while (--index > lastIndex)
		{
			Instruction i = list.get(index);
			
			switch (i.opcode)
			{
			case ByteCodeConstants.IF:
			case ByteCodeConstants.IFCMP:
			case ByteCodeConstants.IFXNULL:	
			case ByteCodeConstants.GOTO:
				int jumpOffset = ((BranchInstruction)i).GetJumpOffset();
				if ((newFirstOffset < jumpOffset) && (jumpOffset <= lastOffset))
					newFirstOffset = jumpOffset;
			}
		}
		
		// Search index associated with 'firstOffset'
		if (newFirstOffset != firstOffset)
		{
			for (index=firstIndex; index<=lastIndex; index++)
			{
				Instruction i = list.get(index);
				if (i.offset > newFirstOffset)
				{
					firstIndex = index;
					break;
				}
			}
		}
		
		return firstIndex;
	}

	private static int SearchFirstIndex(
		List<Instruction> list, int lastIndex, 
		BranchInstruction lastBi, int afterOffest, 
		boolean[] offsetToPreviousGotoFlag, 
		boolean[] inversedTernaryOpLogic)
	{
		int index = lastIndex;
		int lastBiJumpOffset = lastBi.GetJumpOffset();

		Instruction nextInstruction = lastBi;

		while (index-- > 0)
		{
			Instruction instruction = list.get(index);
			int opcode = instruction.opcode;
			
			if ((opcode == ByteCodeConstants.IF) ||
				(opcode == ByteCodeConstants.IFCMP) ||
				(opcode == ByteCodeConstants.IFXNULL))
			{
				BranchInstruction bi = (BranchInstruction)instruction;
				int jumpOffset = bi.GetJumpOffset();				

				// L'instruction if courante appartient-elle au meme bloc que le 1er ?
				if (jumpOffset == lastBiJumpOffset)
				{
					if ((bi.branch > 0) && 
						(instruction.lineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
						(nextInstruction.lineNumber != Instruction.UNKNOWN_LINE_NUMBER))
					{
						if (instruction.lineNumber+2 <= nextInstruction.lineNumber)
						{
							// Amelioration par rapport a JAD : possibilite de 
							// construire deux instructions 'if' pourtant compatibles
							break; // Non
						}
						else
						{
							// Est-ce que l'une des instructions suivantes a un 
							// numero de ligne <= a instruction.lineNumber et < 
							// a nextInstruction.lineNumber
							int lenght = list.size();
							boolean instructionBetweenIf = false;
									
							// ATTENTION: Fragment de code ralentissement grandement la decompilation 
							for (int i=lastIndex+1; i<lenght; i++)
							{
								Instruction ins = list.get(i);
								
								if (ins.opcode == ByteCodeConstants.IINC)
								{
									int lineNumber = ins.lineNumber;
									
									if ((lineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
										(instruction.lineNumber <= lineNumber) &&
										(lineNumber < nextInstruction.lineNumber))
									{
										instructionBetweenIf = true;
										break;
									}
								}
							}
							// ATTENTION: Fragment de code ralentissement grandement la decompilation 
							
							if (instructionBetweenIf)
							{
								break; // Non
							}
						}		
					}
				}
				else if ((jumpOffset != lastBiJumpOffset) &&
					     ((bi.branch <= 0) || (jumpOffset > afterOffest)))
				{
					break; // Non
				}
			}
			else if (opcode == ByteCodeConstants.GOTO)
			{
				Goto g = (Goto)instruction;
				int jumpOffset = g.GetJumpOffset();								

				// Ce 'goto' appartient-il au meme bloc que le 1er 'if' ?
				if ((jumpOffset != lastBiJumpOffset) &&
					((jumpOffset <= nextInstruction.offset) || 
					 (jumpOffset > afterOffest)))
					break; // Non
				
				// Recherche de l'offset de l'instruction avant le 'goto'
				if (index <= 0)
					break; // Non
				
				Instruction lastInstructionValue1 = list.get(index-1);
				opcode = lastInstructionValue1.opcode;
				
				if ((opcode != ByteCodeConstants.IF) &&
					(opcode != ByteCodeConstants.IFCMP) &&
					(opcode != ByteCodeConstants.IFXNULL))
					break; // Non
				
				int jumpOffsetValue1 = 
					((BranchInstruction)lastInstructionValue1).GetJumpOffset();
				
				if ((g.offset < jumpOffsetValue1) && 
					(jumpOffsetValue1 <= lastBi.offset))
					break; // Non
				
				// offset de l'instruction avant le saut du goto
				Instruction lastInstructionValue2 = list.get(lastIndex);
				
				for (int jumpIndex=lastIndex-1; jumpIndex>index; jumpIndex--)
				{
					Instruction jumpInstruction = list.get(jumpIndex);
					if (jumpOffset > jumpInstruction.offset)
					{
						lastInstructionValue2 = jumpInstruction;
						break;
					}
				}
				
				opcode = lastInstructionValue2.opcode;
				
				if ((opcode != ByteCodeConstants.IF) &&
					(opcode != ByteCodeConstants.IFCMP) &&
					(opcode != ByteCodeConstants.IFXNULL))
					break; // Non

				int jumpOffsetValue2 = 
					((BranchInstruction)lastInstructionValue2).GetJumpOffset();

				if (jumpOffsetValue1 == jumpOffsetValue2)
				{
					// Oui ! Séquence dans le bon sens
					int nextOffset = nextInstruction.offset;
					for (int j=g.offset+1; j<nextOffset; j++)
						offsetToPreviousGotoFlag[j] = true;
				}
				else if (jumpOffset == jumpOffsetValue2)
				{
					// Oui ! Séquence inversee : les offsets du Goto et du 1er
					// sous-test sont inversés => il FAUT inverser le 1er test
					int nextOffset = nextInstruction.offset;
					for (int j=g.offset+1; j<nextOffset; j++)
						offsetToPreviousGotoFlag[j] = true;
					
					inversedTernaryOpLogic[g.offset] = true;
				}
				else
				{
					// Non
					break; 
				}
			}
			else
			{
				break;
			}
			
			nextInstruction = instruction;
		}	
		
		return index+1;
	}
	
	private static ComplexConditionalBranchInstruction CreateIfInstructions(
		boolean[] offsetToPreviousGotoFlag,
		boolean[] inversedTernaryOpLogic,
		List<Instruction> branchInstructions, 
		BranchInstruction lastBi)
	{
		// Reconstruction des operateurs ternaires 
		//  -> Elimination des instructions 'Goto'
		ReconstructTernaryOperators(
			offsetToPreviousGotoFlag, inversedTernaryOpLogic, 
			branchInstructions, lastBi);
				
		// Creation de l'instruction ComplexBranchList
		ComplexConditionalBranchInstruction cbl = 
			AssembleAndCreateIfInstructions(branchInstructions, lastBi);
		
		// Affectation des comparaisons	& des operateurs
		SetOperator(cbl, lastBi, false);

		return cbl;
	}
	
	private static void ReconstructTernaryOperators(
			boolean[] offsetToPreviousGotoFlag,
			boolean[] inversedTernaryOpLogic,
			List<Instruction> branchInstructions, 
			BranchInstruction lastBi)
	{
		if (branchInstructions.size() <= 1)
			return;
		
		// Recherche des instructions 'if' sautant vers des instructions 'goto'
		// en commencant par la derniere instruction
		int index = branchInstructions.size()-1;
		int nextOffest = branchInstructions.get(index).offset;
		
		while (index-- > 0)
		{
			Instruction i = branchInstructions.get(index);
			
			switch (i.opcode)
			{
			case ByteCodeConstants.IF:
			case ByteCodeConstants.IFCMP:
			case ByteCodeConstants.IFXNULL:
				BranchInstruction lastTernaryOpTestBi = (BranchInstruction)i;
				int lastTernaryOpTestBiJumpOffset = 
					lastTernaryOpTestBi.GetJumpOffset();		
				
				if ((lastTernaryOpTestBiJumpOffset < 0) || 
					(lastBi.offset < lastTernaryOpTestBiJumpOffset) || 
					!offsetToPreviousGotoFlag[lastTernaryOpTestBiJumpOffset])
					break;
				
				// Extraction de la sous liste d'instructions constituant
				// le test de l'operateur ternaire
				ArrayList<Instruction> ternaryOpTestInstructions =
					new ArrayList<Instruction>();
				ternaryOpTestInstructions.add(lastTernaryOpTestBi);
				
				while (index > 0)
				{
					Instruction ternaryOpTestInstruction = 
						branchInstructions.get(--index);
					
					int opcode = ternaryOpTestInstruction.opcode;
					
					if ((opcode != ByteCodeConstants.IF) &&
						(opcode != ByteCodeConstants.IFCMP) &&
						(opcode != ByteCodeConstants.IFXNULL) &&
						(opcode != ByteCodeConstants.GOTO))
					{
						index++;
						break;
					}

					BranchInstruction bi = 
						(BranchInstruction)ternaryOpTestInstruction;
					int branchOffset = bi.branch;
					int jumpOffset = bi.offset + branchOffset;				
					
					// L'instruction if courante appartient-elle au meme bloc que le 1er ?
					if ((jumpOffset != lastTernaryOpTestBiJumpOffset) &&
						((branchOffset <= 0) || (jumpOffset > nextOffest)))
					{
						// Non
						index++;
						break;
					}
					
					branchInstructions.remove(index);
					ternaryOpTestInstructions.add(ternaryOpTestInstruction);
				}
				
				Instruction test;
				
				if (ternaryOpTestInstructions.size() > 1)
				{
					Collections.reverse(ternaryOpTestInstructions);
					test = CreateIfInstructions(
						offsetToPreviousGotoFlag, inversedTernaryOpLogic,
						ternaryOpTestInstructions, lastTernaryOpTestBi);
				}
				else
				{
					test = lastTernaryOpTestBi;
				}
				InverseComparison(test);
				
				// Extraction de la sous liste d'instructions constituant
				// la premiere valeur de l'operateur ternaire (instructions
				// entre le test et l'instruction 'goto')
				ArrayList<Instruction> ternaryOpValue1Instructions =
					new ArrayList<Instruction>();
				
				index++;
				
				while (index < branchInstructions.size())
				{
					Instruction instruction = 
						branchInstructions.get(index);
					
					if (instruction.offset >= lastTernaryOpTestBiJumpOffset)
						break;
					
					ternaryOpValue1Instructions.add(instruction);
					branchInstructions.remove(index);
				}
				
				// Remove last 'goto' instruction
				Goto g = (Goto)ternaryOpValue1Instructions.remove(
					ternaryOpValue1Instructions.size()-1);
				
				BranchInstruction value1;
				
				if (ternaryOpValue1Instructions.size() > 1)
				{
					BranchInstruction lastTernaryOpValueBi = 
						(BranchInstruction)ternaryOpValue1Instructions.get(
							ternaryOpValue1Instructions.size()-1);
					// Creation de l'instruction ComplexBranchList
					value1 = AssembleAndCreateIfInstructions(
						ternaryOpValue1Instructions, lastTernaryOpValueBi);
				}
				else
				{
					value1 = (BranchInstruction)ternaryOpValue1Instructions.get(
						ternaryOpValue1Instructions.size()-1);
				}
				
				int gotoJumpOffset;

				if (inversedTernaryOpLogic[g.offset])
				{
					gotoJumpOffset = value1.GetJumpOffset();
					InverseComparison(value1);
				}
				else
				{
					gotoJumpOffset = g.GetJumpOffset();
				}
				
				// Extraction de la sous liste d'instructions constituant
				// la seconde valeur de l'operateur ternaire (instructions entre
				// l'instruction 'goto' et la prochaine instruction 'goto' ou
				// jusqu'au saut du test)
				ArrayList<Instruction> ternaryOpValue2Instructions =
					new ArrayList<Instruction>();
				
				while (index < branchInstructions.size())
				{
					Instruction instruction = 
						branchInstructions.get(index);
					
					if ((instruction.opcode == ByteCodeConstants.GOTO) || 
						(instruction.offset >= gotoJumpOffset))
						break;
					
					ternaryOpValue2Instructions.add(instruction);
					branchInstructions.remove(index);
				}

				BranchInstruction value2;

				if (ternaryOpValue2Instructions.size() > 1)
				{
					BranchInstruction lastTernaryOpValueBi = 
						(BranchInstruction)ternaryOpValue2Instructions.get(
							ternaryOpValue2Instructions.size()-1);
					// Creation de l'instruction ComplexBranchList
					value2 = AssembleAndCreateIfInstructions(
						ternaryOpValue2Instructions, lastTernaryOpValueBi);
				}
				else
				{
					value2 = (BranchInstruction)ternaryOpValue2Instructions.get(
						ternaryOpValue2Instructions.size()-1);
				}

				index--;
				
				// Create ternary operator
				TernaryOperator to = new TernaryOperator(
					ByteCodeConstants.TERNARYOP, value2.offset, 
					test.lineNumber, test, value1, value2);
				
				ArrayList<Instruction> instructions = 
					new ArrayList<Instruction>(1);
				instructions.add(to);
				
				// Create complex if instruction
				ComplexConditionalBranchInstruction cbl = new ComplexConditionalBranchInstruction(
					ByteCodeConstants.COMPLEXIF, value2.offset, test.lineNumber,
					ByteCodeConstants.CMP_NONE, instructions, 
					value2.branch);
				
				branchInstructions.set(index, cbl);
				
				break;
			}
			
			nextOffest = i.offset;
		}
	}
	
	private static ComplexConditionalBranchInstruction AssembleAndCreateIfInstructions(
			List<Instruction> branchInstructions, 
			BranchInstruction lastBi)
	{
		int length = branchInstructions.size();
		int lastBiOffset  = lastBi.offset;
		
		// Search sub test block
		for (int i=0; i<length; ++i)
		{
			BranchInstruction branchInstruction = 
				(BranchInstruction)branchInstructions.get(i);
			int jumpOffset = branchInstruction.GetJumpOffset();

			if ((branchInstruction.branch > 0) && (jumpOffset < lastBiOffset))
			{
				// Inner jump
				BranchInstruction subLastBi = lastBi;
				ArrayList<Instruction> subBranchInstructions =
					new ArrayList<Instruction>();
				
				// Extract sub list
				subBranchInstructions.add(branchInstruction);
				i++;
				
				while (i < length)
				{
					branchInstruction = 
						(BranchInstruction)branchInstructions.get(i);
					
					if (branchInstruction.offset >= jumpOffset)
						break;
					
					subBranchInstructions.add(branchInstruction);
					subLastBi = branchInstruction;
					branchInstructions.remove(i);
					--length;
				}
				
				--i;
				
				if (subBranchInstructions.size() > 1)
				{
					// Recursive call
					branchInstructions.set(i, 
							AssembleAndCreateIfInstructions(
									subBranchInstructions, subLastBi));
				}
			}	
		}
		
		//
		AnalyzeLastTestBlock(branchInstructions);

		// First branch instruction line number 
		int lineNumber = branchInstructions.get(0).lineNumber;
		
		return new ComplexConditionalBranchInstruction(
			ByteCodeConstants.COMPLEXIF, lastBi.offset, lineNumber,
			ByteCodeConstants.CMP_NONE, branchInstructions, lastBi.branch);
	}
	
	private static void AnalyzeLastTestBlock(
		List<Instruction> branchInstructions)
	{
		int length = branchInstructions.size();
			
		if (length > 1)
		{
			length--;
			BranchInstruction branchInstruction = 
				(BranchInstruction)branchInstructions.get(0);
			int firstJumpOffset = branchInstruction.GetJumpOffset();
				
			// Search sub list
			for (int i=1; i<length; ++i)
			{
				branchInstruction = 
					(ConditionalBranchInstruction)branchInstructions.get(i);
				int jumpOffset = branchInstruction.GetJumpOffset();
				
				if (firstJumpOffset != jumpOffset)
				{
					BranchInstruction subLastBi = branchInstruction;
					ArrayList<Instruction> subJumpInstructions =
						new ArrayList<Instruction>(length);

					// Extract sub list
					subJumpInstructions.add(branchInstruction);
					i++;
					
					while (i <= length)
					{
						subLastBi = (BranchInstruction)branchInstructions.remove(i);
						subJumpInstructions.add(subLastBi);
						length--;
					}
	
					// Recursive call
					AnalyzeLastTestBlock(subJumpInstructions);
						
					// First branch instruction line number 
					int lineNumber = branchInstructions.get(0).lineNumber;
					
					branchInstructions.set(
						--i, new ComplexConditionalBranchInstruction(
							ByteCodeConstants.COMPLEXIF, subLastBi.offset, 
							lineNumber, ByteCodeConstants.CMP_NONE, 
							subJumpInstructions, subLastBi.branch));
				}
			}
		}
	}

	private static void SetOperator(
		ComplexConditionalBranchInstruction cbl, 
		BranchInstruction lastBi, boolean inverse)
	{
		List<Instruction> instructions = cbl.instructions;
		int lastIndex = instructions.size()-1;
		BranchInstruction firstBi = (BranchInstruction)instructions.get(0);

		if (firstBi.GetJumpOffset() == lastBi.GetJumpOffset())
		{
			cbl.cmp = inverse ? 
				ByteCodeConstants.CMP_AND : ByteCodeConstants.CMP_OR;
			
			for (int i=0; i<=lastIndex; ++i)
				SetOperator(instructions.get(i), inverse);
		}
		else
		{	
			cbl.cmp = inverse ? 
				ByteCodeConstants.CMP_OR : ByteCodeConstants.CMP_AND;
			
			// Inverse all comparaisons except last one
			boolean tmpInverse = !inverse;
			int i = 0;
			
			while (i < lastIndex)
				SetOperator(instructions.get(i++), tmpInverse);
			
			SetOperator(instructions.get(i), inverse);
		}
	}
	
	private static void SetOperator(Instruction instruction, boolean inverse)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.TERNARYOP:
			{
				TernaryOperator to = (TernaryOperator)instruction;
				SetOperator(to.value1, inverse);
				SetOperator(to.value2, inverse);
			}
			break;
		case ByteCodeConstants.COMPLEXIF:
			{
				ComplexConditionalBranchInstruction cbl = (ComplexConditionalBranchInstruction)instruction;
				int length = cbl.instructions.size();
				
				if (length == 1)
				{
					SetOperator(cbl.instructions.get(0), inverse);
				}
				else if (length > 1)
				{
					SetOperator(
						cbl, 
						(BranchInstruction)cbl.instructions.get(length-1), 
						inverse);
				}
			}
			break;
		default:
			{
				if (inverse)
				{
					ConditionalBranchInstruction cbi = 
						(ConditionalBranchInstruction)instruction;
					cbi.cmp = ByteCodeConstants.CMP_MAX_INDEX - cbi.cmp;
				}
			}
			break;
		}
	}
	
	public static void InverseComparison(Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFCMP:
		case ByteCodeConstants.IFXNULL:
			ConditionalBranchInstruction cbi = 
				(ConditionalBranchInstruction)instruction;
			cbi.cmp = ByteCodeConstants.CMP_MAX_INDEX - cbi.cmp;
			break;
		case ByteCodeConstants.COMPLEXIF:
			ComplexConditionalBranchInstruction ccbi = 
				(ComplexConditionalBranchInstruction)instruction;
			ccbi.cmp = ByteCodeConstants.CMP_OR - ccbi.cmp;
			for (int i=ccbi.instructions.size()-1; i>=0; --i)
				InverseComparison(ccbi.instructions.get(i));
			break;
		case ByteCodeConstants.TERNARYOP:
			TernaryOperator to = (TernaryOperator)instruction;
			InverseComparison(to.value1);
			InverseComparison(to.value2);
			break;
//		default:
//			System.out.println("debug");
		}
	}
	
	public static int GetLastIndex(List<Instruction> list, int firstIndex)
	{
		int lenght = list.size();
		int index = firstIndex+1;
		
		// Recherche de la potentielle derniere instruction de saut
		while (index < lenght)
		{
			Instruction instruction = list.get(index);
			int opcode = instruction.opcode;
			
			if ((opcode == ByteCodeConstants.IF) ||
				(opcode == ByteCodeConstants.IFCMP) ||
				(opcode == ByteCodeConstants.IFXNULL) ||
				(opcode == ByteCodeConstants.GOTO))
			{
				index++;
			}
			else
			{
				break;
			}
		}
		
		if (index-1 == firstIndex)
			return firstIndex;
		
		boolean[] dummy = new boolean[list.get(lenght-1).offset];
		
		while (--index > firstIndex)
		{
			// Verification que la potentielle derniere instruction de saut a 
			// comme premiere instruction l'instruction à l'indexe 'firstIndex'
			// Recherche de l'indexe de la premiere instruction 'if' du bloc et 
			// initialisation de 'offsetToPreviousGotoFlag'	
			BranchInstruction lastBi = (BranchInstruction)list.get(index);
			int afterOffest = (index+1 < lenght) ? list.get(index+1).offset : -1;

			int firstIndexTmp = SearchFirstIndex(
				list, index, lastBi, afterOffest, dummy, dummy);
			
			firstIndexTmp = ReduceFirstIndex(list, firstIndexTmp, index);
			
			if (firstIndex == firstIndexTmp)
			{
				// Trouvé
				break;
			}
		}
		
		return index;
	}
}