/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.process.analyzer.instruction.fast.reconstructor;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.process.analyzer.instruction.bytecode.ComparisonInstructionAnalyzer;


/*
 * Recontruction de l'instruction 'return (b1 == 1);' depuis la sequence : 
 * 46: if (b1 == 1)
 *   46: return true;
 * 48: return false;
 */
public class TernaryOpInReturnReconstructor 
{
	public static void Reconstruct(List<Instruction> list)
	{
		for (int index=list.size()-1; index>=0; --index)
		{
			if (list.get(index).opcode != ByteCodeConstants.XRETURN)
				continue;
			
			ReturnInstruction ri1 = (ReturnInstruction)list.get(index);
			int opcode = ri1.valueref.opcode;
			
			if ((opcode != ByteCodeConstants.SIPUSH) && 
				(opcode != ByteCodeConstants.BIPUSH) && 
				(opcode != ByteCodeConstants.ICONST))
				continue;
			
			IConst iConst1 = (IConst)ri1.valueref;

			if (!"Z".equals(iConst1.signature))
				continue;
			
			if (index <= 0)
				continue;
			
			int index2 = index - 1;
						
			if (list.get(index2).opcode != ByteCodeConstants.XRETURN)
				continue;
			
			ReturnInstruction ri2 = (ReturnInstruction)list.get(index2);
			
			if ((ri1.lineNumber != Instruction.UNKNOWN_LINE_NUMBER) && 
				(ri1.lineNumber > ri2.lineNumber))
				continue;
			
			opcode = ri2.valueref.opcode;
			
			if ((opcode != ByteCodeConstants.SIPUSH) && 
				(opcode != ByteCodeConstants.BIPUSH) && 
				(opcode != ByteCodeConstants.ICONST))
				continue;
				
			IConst iConst2 = (IConst)ri2.valueref;

			if (!"Z".equals(iConst2.signature))
				continue;
			
			if (index2 <= 0)
				continue;
			
			Instruction instruction = list.get(--index2);

			opcode = instruction.opcode;
			
			if ((opcode != ByteCodeConstants.IF) && 
				(opcode != ByteCodeConstants.IFCMP) &&
				(opcode != ByteCodeConstants.IFXNULL) &&
				(opcode != ByteCodeConstants.COMPLEXIF))
				continue;
			
			BranchInstruction bi = (BranchInstruction)instruction;			
			int offset = bi.GetJumpOffset();
			
			if ((ri2.offset >= offset) || (offset > ri1.offset))
				continue;
			
			// Verification qu'aucune instruction saute sur 'ri1'
			boolean found = false;
			int i = index2;
			
			while (i-- > 0)
			{
				instruction = list.get(i);
				opcode = instruction.opcode;
				
				if (opcode == ByteCodeConstants.GOTO)
				{
					int jumpOffset = ((Goto)instruction).GetJumpOffset();
					if ((ri2.offset < jumpOffset) && (jumpOffset <= ri1.offset))
					{
						found = true;
						break;
					}
				}
				else if ((opcode == ByteCodeConstants.IF) ||
						 (opcode == ByteCodeConstants.IFCMP) ||
						 (opcode == ByteCodeConstants.IFXNULL) ||
						 (opcode == ByteCodeConstants.COMPLEXIF))
				{
					int jumpOffset = 
						((BranchInstruction)instruction).GetJumpOffset();
					if ((ri2.offset < jumpOffset) && (jumpOffset <= ri1.offset))
					{
						found = true;
						break;
					}										
				}
			}
			
			if (found == true)
				continue;
			
			if (iConst2.value == 1)
				ComparisonInstructionAnalyzer.InverseComparison( bi );
			
			list.remove(index);
			list.remove(index2);
			
			ri2.valueref = bi;
			
			index -= 3;
		}
	}
}
