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
package jd.core.process.analyzer.instruction.fast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.CodeException;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.AStore;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Jsr;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.model.instruction.bytecode.instruction.MonitorExit;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.Switch;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastSynchronized;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;
import jd.core.process.analyzer.instruction.bytecode.ComparisonInstructionAnalyzer;
import jd.core.process.analyzer.instruction.fast.visitor.CheckLocalVariableUsedVisitor;
import jd.core.process.analyzer.instruction.fast.visitor.FastCompareInstructionVisitor;
import jd.core.process.analyzer.util.InstructionUtil;
import jd.core.util.IntSet;
import jd.core.util.UtilConstants;


/*
 * Aglomeration des informations 'CodeException'
 */
public class FastCodeExceptionAnalyzer 
{	
	public static List<FastCodeException> AggregateCodeExceptions(
			Method method, List<Instruction> list)
	{
		CodeException[] arrayOfCodeException = method.getCodeExceptions();		

		if ((arrayOfCodeException == null) || (arrayOfCodeException.length == 0))
			return null;
		
		// Aggregation des 'finally' et des 'catch' executant le meme bloc
		List<FastAggregatedCodeException> fastAggregatedCodeExceptions = 
			new ArrayList<FastAggregatedCodeException>(
				arrayOfCodeException.length);
		PopulateListOfFastAggregatedCodeException(
			method, list, fastAggregatedCodeExceptions);
			
		int length = fastAggregatedCodeExceptions.size();
		ArrayList<FastCodeException> fastCodeExceptions = 
			new ArrayList<FastCodeException>(length);
		
		// Aggregation des blocs 'finally' aux blocs 'catch'
		// Add first
		fastCodeExceptions.add(NewFastCodeException(
			list, fastAggregatedCodeExceptions.get(0)));
		
		// Add or update
		for (int i=1; i<length; ++i)
		{
			FastAggregatedCodeException fastAggregatedCodeException = 
				fastAggregatedCodeExceptions.get(i);
			
			// Update 'FastCodeException' for 'codeException'
			if (!UpdateFastCodeException(
					fastCodeExceptions, fastAggregatedCodeException))
				// Not found -> Add new entry
				fastCodeExceptions.add(NewFastCodeException(
					list, fastAggregatedCodeException));
		}
		
		// Sort by 1)tryFromOffset 2)maxOffset 3)tryToOffset
		// Necessaire pour le calcul de 'afterOffset' des structures try-catch
		// par 'ComputeAfterOffset'
		Collections.sort(fastCodeExceptions);

		// Aggregation des blocs 'catch'
		// Reduce of FastCodeException after UpdateFastCodeException(...)
		for (int i=fastCodeExceptions.size()-1; i>=1; --i)
		{
			FastCodeException fce1 = fastCodeExceptions.get(i);
			FastCodeException fce2 = fastCodeExceptions.get(i-1);
			
			if ((fce1.tryFromOffset == fce2.tryFromOffset) && 
				(fce1.tryToOffset == fce2.tryToOffset) && 
				(fce1.synchronizedFlag == fce2.synchronizedFlag) &&
				((fce1.afterOffset == UtilConstants.INVALID_OFFSET) || (fce1.afterOffset > fce2.maxOffset)) &&
				((fce2.afterOffset == UtilConstants.INVALID_OFFSET) || (fce2.afterOffset > fce1.maxOffset)))
			{
				// Append catches
				fce2.catches.addAll(fce1.catches);
				Collections.sort(fce2.catches);
				// Append finally
				if (fce2.nbrFinally == 0)
				{
					fce2.finallyFromOffset = fce1.finallyFromOffset;
					fce2.nbrFinally        = fce1.nbrFinally;
				}
				// Update 'maxOffset'
				if (fce2.maxOffset < fce1.maxOffset)
					fce2.maxOffset = fce1.maxOffset;
				// Update 'afterOffset'
				if ((fce2.afterOffset == UtilConstants.INVALID_OFFSET) ||
					((fce1.afterOffset != UtilConstants.INVALID_OFFSET) && 
					 (fce1.afterOffset < fce2.afterOffset)))
					fce2.afterOffset = fce1.afterOffset;
				// Remove last FastCodeException
				fastCodeExceptions.remove(i);
			}
		}
		
		// Search 'switch' instructions, sort case offset
		ArrayList<int[]> switchCaseOffsets = SearchSwitchCaseOffsets(list);
		
		for (int i=fastCodeExceptions.size()-1; i>=0; --i)
		{
			FastCodeException fce = fastCodeExceptions.get(i);
			
			// Determine type
			DefineType(list, fce);
			
			if (fce.type == FastConstants.TYPE_UNDEFINED)
				System.err.println("Undefined type catch");
				
			// Compute afterOffset
			ComputeAfterOffset(
				method, list, switchCaseOffsets, fastCodeExceptions, fce, i);
			
			length = list.size();
			if ((fce.afterOffset == UtilConstants.INVALID_OFFSET) && (length > 0))
			{
				Instruction lastInstruction = list.get(length-1);
				fce.afterOffset = lastInstruction.offset;
				
				if ((lastInstruction.opcode != ByteCodeConstants.RETURN) &&
					(lastInstruction.opcode != ByteCodeConstants.XRETURN))
					// Set afterOffset to a virtual instruction after list. 
					fce.afterOffset++;
			}
		}

		// Sort by 1)tryFromOffset 2)maxOffset 3)tryToOffset
		Collections.sort(fastCodeExceptions);
		
		return fastCodeExceptions;
	}
	
	private static void PopulateListOfFastAggregatedCodeException(
		Method method, List<Instruction> list,
		List<FastAggregatedCodeException> fastAggregatedCodeExceptions)
	{
		int length = method.getCode().length;
		if (length == 0)
			return;

		FastAggregatedCodeException[] array = 
			new FastAggregatedCodeException[length];		

		CodeException[] arrayOfCodeException = method.getCodeExceptions();				
		length = arrayOfCodeException.length;
		for (int i=0; i<length; i++)
		{
			CodeException codeException = arrayOfCodeException[i];
			
			if (array[codeException.handler_pc] == null)
			{
				FastAggregatedCodeException face = 
					new FastAggregatedCodeException(
						i, codeException.start_pc, codeException.end_pc, 
						codeException.handler_pc, codeException.catch_type);				
				fastAggregatedCodeExceptions.add(face);
				array[codeException.handler_pc] = face;
			}
			else
			{
				FastAggregatedCodeException face = array[codeException.handler_pc];
				// ATTENTION: la modification de 'end_pc' implique la 
				//            reecriture de 'defineType(...) !!	
				if (face.catch_type == 0)
				{
					face.nbrFinally++;
				}
				else
				{
					// Ce type d'exception a-t-il deja ete ajoute ?					
					if (IsNotAlreadyStored(face, codeException.catch_type))
					{
						// Non
						if (face.otherCatchTypes == null)
							face.otherCatchTypes = new int[length];
						face.otherCatchTypes[i] = codeException.catch_type;
					}
				}
			}
		}
		
		int i = fastAggregatedCodeExceptions.size();
		while (i-- > 0)
		{
			FastAggregatedCodeException face = 
				fastAggregatedCodeExceptions.get(i);
			
			if ((face.catch_type == 0) && IsASynchronizedBlock(list, face))
			{
				face.synchronizedFlag = true;
			}
		}
	}

	private static boolean IsNotAlreadyStored(
		FastAggregatedCodeException face, int catch_type)
	{
		if (face.catch_type == catch_type)
			return false;
		
		if (face.otherCatchTypes != null)
		{
			int i = face.otherCatchTypes.length;
			
			while (i-- > 0)
			{
				if (face.otherCatchTypes[i] == catch_type)
					return false;
			}
		}
			
		return true;
	}
	
	private static boolean IsASynchronizedBlock(
		List<Instruction> list, FastAggregatedCodeException face)
	{
		int index = InstructionUtil.getIndexForOffset(list, face.start_pc);
	
		if (index == -1)
			return false;
			
		if (list.get(index).opcode == ByteCodeConstants.MONITOREXIT)
		{
			// Cas particulier Jikes 1.2.2
			return true;
		}
		
		if (index < 1)
			return false;

		/* Recherche si le bloc finally contient une instruction 
		 * monitorexit ayant le meme index que l'instruction 
		 * monitorenter avant le bloc try.
		 * Byte code++:
		 *  5: System.out.println("start");
		 *  8: localTestSynchronize = this
		 *  11: monitorenter (localTestSynchronize);        <----
		 *  17: System.out.println("in synchronized");
		 *  21: monitorexit localTestSynchronize;
		 *  22: goto 30;
		 *  25: localObject2 = finally;
		 *  27: monitorexit localTestSynchronize;           <====
		 *  29: throw localObject1;
		 *  35: System.out.println("end");
		 *  38: return;
		 */
		Instruction instruction = list.get(index-1);

		if (instruction.opcode != ByteCodeConstants.MONITORENTER)
			return false;
		
		int varMonitorIndex;
		MonitorEnter monitorEnter = (MonitorEnter)instruction;
					
		switch (monitorEnter.objectref.opcode)
		{
		case ByteCodeConstants.ALOAD:
			{
				if (index < 2)
					return false;
				instruction = list.get(index-2);
				if (instruction.opcode != ByteCodeConstants.ASTORE)
					return false;
				AStore astore = (AStore)instruction;
				varMonitorIndex = astore.index;
			}
			break;
		case ByteCodeConstants.ASSIGNMENT:
			{
				AssignmentInstruction ai = 
					(AssignmentInstruction)monitorEnter.objectref;
				if (ai.value1.opcode != ByteCodeConstants.ALOAD)
					return false;
				ALoad aload = (ALoad)ai.value1;
				varMonitorIndex = aload.index;
			}
			break;
		default:
			return false;
		}
		
		boolean checkMonitorExit = false;
		int length = list.size();
		index = InstructionUtil.getIndexForOffset(list, face.handler_pc);
		
		while (index < length)
		{
			instruction = list.get(index++);
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.MONITOREXIT:
				checkMonitorExit = true;
				MonitorExit monitorExit = (MonitorExit)instruction;	
				
				if ((monitorExit.objectref.opcode == ByteCodeConstants.ALOAD) &&
					(((ALoad)monitorExit.objectref).index == varMonitorIndex))
					return true;
				break;
			case ByteCodeConstants.RETURN:
			case ByteCodeConstants.XRETURN:
			case ByteCodeConstants.ATHROW:
				return false;
			}
		}
		
		if ((checkMonitorExit == false) && (index == length))
		{
			// Aucune instruction 'MonitorExit' n'a ete trouv�e. Cas de la 
			// double instruction 'synchronized' imbriqu�e pour le JDK 1.1.8
			return true;
		}
		
		return false;
	}
	
	private static boolean UpdateFastCodeException(
			List<FastCodeException> fastCodeExceptions, 
			FastAggregatedCodeException fastAggregatedCodeException)
	{
		int length = fastCodeExceptions.size();
		
		if (fastAggregatedCodeException.catch_type == 0)
		{
			// Finally
			
			// Same start and end offsets
			for (int i=0; i<length; ++i)
			{
				FastCodeException fce = fastCodeExceptions.get(i);

				if ((fce.finallyFromOffset == UtilConstants.INVALID_OFFSET) && 
				    (fastAggregatedCodeException.start_pc == fce.tryFromOffset) &&
				    (fastAggregatedCodeException.end_pc == fce.tryToOffset) && 
				    (fastAggregatedCodeException.handler_pc > fce.maxOffset) &&
				    (fastAggregatedCodeException.synchronizedFlag == false))
				{
					if ((fce.afterOffset == UtilConstants.INVALID_OFFSET) ||
						((fastAggregatedCodeException.end_pc < fce.afterOffset) &&
						 (fastAggregatedCodeException.handler_pc < fce.afterOffset)))
					{
						fce.maxOffset = fastAggregatedCodeException.handler_pc;
						fce.finallyFromOffset = fastAggregatedCodeException.handler_pc;
						fce.nbrFinally += fastAggregatedCodeException.nbrFinally;
						return true;
					}
				}
			}		

			// Old algo
			for (int i=0; i<length; ++i)
			{
				FastCodeException fce = fastCodeExceptions.get(i);

				if ((fce.finallyFromOffset == UtilConstants.INVALID_OFFSET) && 
				    (fastAggregatedCodeException.start_pc == fce.tryFromOffset) &&
				    (fastAggregatedCodeException.end_pc >= fce.tryToOffset) && 
				    (fastAggregatedCodeException.handler_pc > fce.maxOffset) &&
				    (fastAggregatedCodeException.synchronizedFlag == false))
				{
					if ((fce.afterOffset == UtilConstants.INVALID_OFFSET) ||
						((fastAggregatedCodeException.end_pc < fce.afterOffset) &&
						 (fastAggregatedCodeException.handler_pc < fce.afterOffset)))
					{
						fce.maxOffset = fastAggregatedCodeException.handler_pc;
						fce.finallyFromOffset = fastAggregatedCodeException.handler_pc;
						fce.nbrFinally += fastAggregatedCodeException.nbrFinally;
						return true;
					}
					/* Mis en commentaire a cause d'erreurs pour le jdk1.5.0 dans 
					 * TryCatchFinallyClass.complexMethodTryCatchCatchFinally()
					 * 
					 * else if ((fce.catches != null) && 
							 (fce.afterOffset == fastAggregatedCodeException.end_pc))
					{
						fce.finallyFromOffset = fastAggregatedCodeException.handler_pc;
						fce.nbrFinally += fastAggregatedCodeException.nbrFinally;
						return true;							
					} */
				}
			}		
		}
				
		return false;
	}

	private static FastCodeException NewFastCodeException(
		List<Instruction> list, FastAggregatedCodeException fastCodeException)
	{			
		FastCodeException fce = new FastCodeException(
			fastCodeException.start_pc, 
			fastCodeException.end_pc, 
			fastCodeException.handler_pc,
			fastCodeException.synchronizedFlag);
		
		if (fastCodeException.catch_type == 0)
		{
			fce.finallyFromOffset = fastCodeException.handler_pc;
			fce.nbrFinally += fastCodeException.nbrFinally;
		}
		else
		{
			fce.catches.add(new FastCodeExceptionCatch(
				fastCodeException.catch_type,
				fastCodeException.otherCatchTypes, 
				fastCodeException.handler_pc));		
		}
		
		// Approximation a affin�e par la methode 'ComputeAfterOffset'
		fce.afterOffset = SearchAfterOffset(list, fastCodeException.handler_pc);
	
		return fce;
	}

	/*
	 * Recherche l'offset apres le bloc try-catch-finally
	 */
	private static int SearchAfterOffset(List<Instruction> list, int offset)
	{
		// Search instruction at 'offset'
		int index = InstructionUtil.getIndexForOffset(list, offset);
		
		if (index <= 0)
			return offset;
		
		// Search previous 'goto' instruction
		Instruction i = list.get(--index);
		
		switch (i.opcode)
		{
		case ByteCodeConstants.GOTO:
			int branch = ((Goto)i).branch;
			if (branch < 0)
				return UtilConstants.INVALID_OFFSET;
			int jumpOffset = i.offset + branch;
			index = InstructionUtil.getIndexForOffset(list, jumpOffset);
			if (index <= 0)
				return UtilConstants.INVALID_OFFSET;
			i = list.get(index);
			if (i.opcode != ByteCodeConstants.JSR)
				return jumpOffset;
			branch = ((Jsr)i).branch;
			if (branch > 0)
				return i.offset + branch;
			return jumpOffset+1;

		case ByteCodeConstants.RET:
			// Particularite de la structure try-catch-finally du JDK 1.1.8:  
			// une sous routine termine le bloc precedent 'offset'. 
			// Strategie : recheche de l'instruction goto, sautant apres 
			// 'offset', et suivie par le sequence d'instructions suivante :
		    //  30: goto +105 -> 135
		    //  33: astore_3
		    //  34: jsr +5 -> 39
		    //  37: aload_3
		    //  38: athrow
		    //  39: astore 4
		    //  41: ...
		    //  45: ret 4
			while (--index >= 3)
			{
				if ((list.get(index).opcode == ByteCodeConstants.ATHROW) &&
					(list.get(index-1).opcode == ByteCodeConstants.JSR) &&
					(list.get(index-2).opcode == ByteCodeConstants.ASTORE) &&
					(list.get(index-3).opcode == ByteCodeConstants.GOTO))
				{
					Goto g = (Goto)list.get(index-3);
					return g.GetJumpOffset();
				}
			}			

		default:
			return UtilConstants.INVALID_OFFSET;
		}
	}

	private static ArrayList<int[]> SearchSwitchCaseOffsets(
			List<Instruction> list)
	{
		ArrayList<int[]> switchCaseOffsets = new ArrayList<int[]>();
		
		int i = list.size();
		while (i-- > 0)
		{
			Instruction instruction = list.get(i);
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.TABLESWITCH:
			case ByteCodeConstants.LOOKUPSWITCH:
				Switch s = (Switch)instruction;
				int j = s.offsets.length;
				int[] offsets = new int[j+1];
				
				offsets[j] = s.offset + s.defaultOffset;
				while (j-- > 0)
					offsets[j] = s.offset + s.offsets[j];
				
				Arrays.sort(offsets);
				switchCaseOffsets.add(offsets);
				break;			
			}
		}
		
		return switchCaseOffsets;
	}
	
	private static void DefineType(
			List<Instruction> list, FastCodeException fastCodeException)
	{
		// Contains finally ?
		switch (fastCodeException.nbrFinally) 
		{
		case 0:
			// No
			fastCodeException.type = FastConstants.TYPE_CATCH;			
			break;
		case 1:
			// 1.1.8, 1.3.1, 1.4.2 or eclipse 677
			// Yes, contains catch ?
			if ((fastCodeException.catches == null)	|| 
				(fastCodeException.catches.size() == 0))
			{
				// No
				int index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.finallyFromOffset);
				if (index < 0)
					return;

				// Search 'goto' instruction
				Instruction instruction = list.get(index-1);
				switch (instruction.opcode)
				{				
				case ByteCodeConstants.GOTO:
					if (TryBlockContainsJsr(list, fastCodeException))
					{
						fastCodeException.type = FastConstants.TYPE_118_FINALLY;
					}
					else
					{
						// Search previous 'goto' instruction
						switch (list.get(index-2).opcode)
						{
						case ByteCodeConstants.MONITOREXIT:
							fastCodeException.type = FastConstants.TYPE_118_SYNCHRONIZED;
							break;
						default:
							// TYPE_ECLIPSE_677_FINALLY or TYPE_118_FINALLY_2 ?
							int jumpOffset = ((Goto)instruction).GetJumpOffset();
							instruction = 
								InstructionUtil.getInstructionAt(list, jumpOffset);
							
							if (instruction.opcode == ByteCodeConstants.JSR)
								fastCodeException.type = 
									FastConstants.TYPE_118_FINALLY_2;
							else
								fastCodeException.type = 
									FastConstants.TYPE_ECLIPSE_677_FINALLY;	
						}
					}
					break;
				case ByteCodeConstants.RETURN:
				case ByteCodeConstants.XRETURN:
					if (TryBlockContainsJsr(list, fastCodeException))
					{
						fastCodeException.type = FastConstants.TYPE_118_FINALLY;
					}
					else
					{
						// Search previous 'return' instruction
						switch (list.get(index-2).opcode)
						{
						case ByteCodeConstants.MONITOREXIT:
							fastCodeException.type = FastConstants.TYPE_118_SYNCHRONIZED;
							break;
						default:
							// TYPE_ECLIPSE_677_FINALLY or TYPE_142 ?
							Instruction firstFinallyInstruction = list.get(index+1);
							int exceptionIndex = ((AStore)list.get(index)).index;
							int lenght = list.size();
							
							// Search throw instruction
							while (++index < lenght)
							{
								instruction = list.get(index);								
								if (instruction.opcode == ByteCodeConstants.ATHROW)
								{
									AThrow athrow = (AThrow)instruction;									
									if ((athrow.value.opcode == ByteCodeConstants.ALOAD) && 
										( ((ALoad)athrow.value).index == exceptionIndex ))
										break;
								}
							}
						
							if (++index >= lenght)
							{
								fastCodeException.type = FastConstants.TYPE_142;
							}
							else
							{
								instruction = list.get(index);
								
								fastCodeException.type = 
									((instruction.opcode != firstFinallyInstruction.opcode) ||
									 (firstFinallyInstruction.lineNumber == Instruction.UNKNOWN_LINE_NUMBER) ||
									 (firstFinallyInstruction.lineNumber != instruction.lineNumber)) ?
											 FastConstants.TYPE_142 :
											 FastConstants.TYPE_ECLIPSE_677_FINALLY;
							}
						}
					}
					break;
				case ByteCodeConstants.ATHROW:
					// Search 'jsr' instruction after 'astore' instruction
					switch (list.get(index+1).opcode)
					{
					case ByteCodeConstants.JSR:
						fastCodeException.type = 
							FastConstants.TYPE_118_FINALLY_THROW;
						break;
					default:
						if (list.get(index).opcode == 
										ByteCodeConstants.MONITOREXIT)
							fastCodeException.type = 
								FastConstants.TYPE_118_FINALLY;	
						else
							fastCodeException.type = 
								FastConstants.TYPE_142_FINALLY_THROW;	
					}
					break;
				case ByteCodeConstants.RET:
					// Double synchronized blocks compiled with the JDK 1.1.8
					fastCodeException.type = 
						FastConstants.TYPE_118_SYNCHRONIZED_DOUBLE;	
					break;
				}
			}
			else
			{
				// Yes, contains catch(s) & finally 
				int index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.catches.get(0).fromOffset);
				if (index < 0)
					return;

				// Search 'goto' instruction in try block
				Instruction instruction = list.get(--index);
				if (instruction.opcode == ByteCodeConstants.GOTO)
				{
					Goto g = (Goto)instruction;

					// Search previous 'goto' instruction
					instruction = list.get(--index);
					if (instruction.opcode == ByteCodeConstants.JSR)
					{
						fastCodeException.type = FastConstants.TYPE_131_CATCH_FINALLY;
					}
					else
					{
	                    // Search jump 'goto' instruction
						index = InstructionUtil.getIndexForOffset(
							list, g.GetJumpOffset());
						instruction = list.get(index);
						
						if (instruction.opcode == ByteCodeConstants.JSR)
						{
							fastCodeException.type = 
								FastConstants.TYPE_118_CATCH_FINALLY;
						}
						else
						{
							instruction = list.get(index - 1);
							
							if (instruction.opcode == ByteCodeConstants.ATHROW)
								fastCodeException.type = 
									FastConstants.TYPE_ECLIPSE_677_CATCH_FINALLY;
							else
								fastCodeException.type = 
									FastConstants.TYPE_118_CATCH_FINALLY_2;
						}
					}	
				}
				else
				{
					if (instruction.opcode == ByteCodeConstants.RET)
					{
						fastCodeException.type = 
							FastConstants.TYPE_118_CATCH_FINALLY;
					}
					else
					{
						// Search previous instruction
						instruction = list.get(--index);
						if (instruction.opcode == ByteCodeConstants.JSR)
						{
							fastCodeException.type = 
								FastConstants.TYPE_131_CATCH_FINALLY;
						}
					}
				}
			}
			break;
		default:
			// 1.3.1, 1.4.2, 1.5.0, jikes 1.2.2 or eclipse 677
			// Yes, contains catch ?
			if ((fastCodeException.catches == null)	|| 
				(fastCodeException.catches.size() == 0))
			{
				// No, 1.4.2 or jikes 1.2.2 ?
				int index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.tryToOffset);
				if (index < 0)
					return;
				
				Instruction instruction = list.get(index);
				
				switch (instruction.opcode)
				{
				case ByteCodeConstants.JSR:
					fastCodeException.type = FastConstants.TYPE_131_CATCH_FINALLY;
					break;
				case ByteCodeConstants.ATHROW:
					fastCodeException.type = FastConstants.TYPE_JIKES_122;
					break;
				case ByteCodeConstants.GOTO:
					Goto g = (Goto)instruction;
					
					// Search previous 'goto' instruction
					instruction = InstructionUtil.getInstructionAt(
						list, g.GetJumpOffset());
					if (instruction == null)
						return;
		
					if ((instruction.opcode == ByteCodeConstants.JSR) && 
						(((Jsr)instruction).branch < 0))
					{
						fastCodeException.type = FastConstants.TYPE_JIKES_122;
					}
					else
					{
						if ((index > 0) && (list.get(index-1).opcode == ByteCodeConstants.JSR))
							fastCodeException.type = FastConstants.TYPE_131_CATCH_FINALLY;
						else
							fastCodeException.type = FastConstants.TYPE_142;
					}
					break;
				case ByteCodeConstants.POP:
					DefineTypeJikes122Or142(
						list, fastCodeException, ((Pop)instruction).objectref, index);
					break;
				case ByteCodeConstants.ASTORE:
					DefineTypeJikes122Or142(
						list, fastCodeException, ((AStore)instruction).valueref, index);
					break;
				case ByteCodeConstants.RETURN:
				case ByteCodeConstants.XRETURN:
					// 1.3.1, 1.4.2 or jikes 1.2.2 ?
					if ((index > 0) && (list.get(index-1).opcode == ByteCodeConstants.JSR))
						fastCodeException.type = FastConstants.TYPE_131_CATCH_FINALLY;
					else
						fastCodeException.type = FastConstants.TYPE_142;
					break;
				default:		
					fastCodeException.type = FastConstants.TYPE_142;
				}
			}
			else
			{
				// Yes, contains catch(s) & multiple finally 
				// Control que toutes les instructions 'goto' sautent sur la 
				// meme instruction.
				boolean uniqueJumpAddressFlag = true;
				int uniqueJumpAddress = -1;
			
				if (fastCodeException.catches != null)
				{
					for (int i=fastCodeException.catches.size()-1; i>=0; --i)
					{
						FastCodeExceptionCatch fcec = 
							fastCodeException.catches.get(i);
						int index = InstructionUtil.getIndexForOffset(
								list, fcec.fromOffset);
						if (index != -1)
						{
							Instruction instruction = list.get(index-1);
							if (instruction.opcode == ByteCodeConstants.GOTO)
							{
								int branch  = ((Goto)instruction).branch;
								if (branch > 0)
								{
									int jumpAddress = instruction.offset + branch;
									if (uniqueJumpAddress == -1)
									{
										uniqueJumpAddress = jumpAddress;
									}
									else if (uniqueJumpAddress != jumpAddress)
									{
										uniqueJumpAddressFlag = false;
										break;
									}
								}
							}
						}					
					}				
				}
					
				int index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.finallyFromOffset);
				if (index < 0)
					return;

				Instruction instruction = list.get(--index);

				if ((uniqueJumpAddressFlag) && 
					(instruction.opcode == ByteCodeConstants.GOTO))
				{
					int branch  = ((Goto)instruction).branch;
					if (branch > 0)
					{
						int jumpAddress = instruction.offset + branch;
						if (uniqueJumpAddress == -1)
							uniqueJumpAddress = jumpAddress;
						else if (uniqueJumpAddress != jumpAddress)
							uniqueJumpAddressFlag = false;
					}
				}
				
				if (!uniqueJumpAddressFlag)
				{
					fastCodeException.type = FastConstants.TYPE_ECLIPSE_677_CATCH_FINALLY;
					return;
				}
	
				index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.tryToOffset);
				if (index < 0)
					return;
				
				instruction = list.get(index);
				
				switch (instruction.opcode)
				{
				case ByteCodeConstants.JSR:
					fastCodeException.type = FastConstants.TYPE_131_CATCH_FINALLY;
					break;
				case ByteCodeConstants.ATHROW:
					fastCodeException.type = FastConstants.TYPE_JIKES_122;
					break;
				case ByteCodeConstants.GOTO:
					Goto g = (Goto)instruction;
					
					// Search previous 'goto' instruction
					instruction = InstructionUtil.getInstructionAt(
						list, g.GetJumpOffset());
					if (instruction == null)
						return;
		
					if ((instruction.opcode == ByteCodeConstants.JSR) && 
						(((Jsr)instruction).branch < 0))
					{
						fastCodeException.type = FastConstants.TYPE_JIKES_122;
					}
					else
					{
						if ((index > 0) && (list.get(index-1).opcode == ByteCodeConstants.JSR))
							fastCodeException.type = FastConstants.TYPE_131_CATCH_FINALLY;
						else
							fastCodeException.type = FastConstants.TYPE_142;
					}
					break;
				case ByteCodeConstants.POP:
					DefineTypeJikes122Or142(
						list, fastCodeException, ((Pop)instruction).objectref, index);
					break;
				case ByteCodeConstants.ASTORE:
					DefineTypeJikes122Or142(
						list, fastCodeException, ((AStore)instruction).valueref, index);
					break;
				case ByteCodeConstants.RETURN:
				case ByteCodeConstants.XRETURN:
					// 1.3.1, 1.4.2 or jikes 1.2.2 ?
					instruction = InstructionUtil.getInstructionAt(
						list, uniqueJumpAddress);
					if ((instruction != null) &&
						(instruction.opcode == ByteCodeConstants.JSR) && 
						( ((Jsr)instruction).branch < 0 ))
						fastCodeException.type = FastConstants.TYPE_JIKES_122;
					else if ((index > 0) && (list.get(index-1).opcode == ByteCodeConstants.JSR))
						fastCodeException.type = FastConstants.TYPE_131_CATCH_FINALLY;
					else
						fastCodeException.type = FastConstants.TYPE_142;
					break;
				default:		
					// TYPE_ECLIPSE_677_FINALLY or TYPE_142 ?
					index = InstructionUtil.getIndexForOffset(
							list, fastCodeException.finallyFromOffset);
					Instruction firstFinallyInstruction = list.get(index+1);
					
					if (firstFinallyInstruction.opcode != ByteCodeConstants.ASTORE)
					{
						fastCodeException.type = FastConstants.TYPE_142;
					}
					else
					{				
						int exceptionIndex = ((AStore)list.get(index)).index;
						int lenght = list.size();
						
						// Search throw instruction
						while (++index < lenght)
						{
							instruction = list.get(index);								
							if (instruction.opcode == ByteCodeConstants.ATHROW)
							{
								AThrow athrow = (AThrow)instruction;									
								if ((athrow.value.opcode == ByteCodeConstants.ALOAD) && 
									( ((ALoad)athrow.value).index == exceptionIndex ))
									break;
							}
						}
						
						if (++index >= lenght)
						{
							fastCodeException.type = FastConstants.TYPE_142;
						}
						else
						{
							instruction = list.get(index);
							
							fastCodeException.type = 
								((instruction.opcode != firstFinallyInstruction.opcode) ||
								 (firstFinallyInstruction.lineNumber == Instruction.UNKNOWN_LINE_NUMBER) ||
								 (firstFinallyInstruction.lineNumber != instruction.lineNumber)) ?
										 FastConstants.TYPE_142 :
										 FastConstants.TYPE_ECLIPSE_677_CATCH_FINALLY;
						}
					}
				}
			}
		}
	}
	
	private static boolean TryBlockContainsJsr(
		List<Instruction> list, FastCodeException fastCodeException)
	{
		int index = InstructionUtil.getIndexForOffset(
				list, fastCodeException.tryToOffset);
		
		if (index != -1)
		{
			int tryFromOffset = fastCodeException.tryFromOffset;
			
			for (;;)
			{
				Instruction instruction = list.get(index);
				
				if (instruction.offset <= tryFromOffset)
				{
					break;
				}
				
				if (instruction.opcode == ByteCodeConstants.JSR)
				{
					if (((Jsr)instruction).GetJumpOffset() > 
						fastCodeException.finallyFromOffset)
					{
						return true;
					}
				}
				
				if (index == 0)
				{
					break;
				}
				
				index--;
			}
		}
		
		return false;
	}
	
	private static void DefineTypeJikes122Or142(
			List<Instruction> list, FastCodeException fastCodeException,
			Instruction instruction, int index)
	{
		if (instruction.opcode == ByteCodeConstants.EXCEPTIONLOAD)
		{
			instruction = list.get(--index);
			
			if (instruction.opcode == ByteCodeConstants.GOTO)
			{
				int jumpAddress = ((Goto)instruction).GetJumpOffset();
				
				instruction = InstructionUtil.getInstructionAt(list, jumpAddress);
				
				if ((instruction != null) && 
					(instruction.opcode == ByteCodeConstants.JSR))
				{
					fastCodeException.type = FastConstants.TYPE_JIKES_122;
					return;
				}
			}
		}
		
		fastCodeException.type = FastConstants.TYPE_142;
	}
	
	private static void ComputeAfterOffset(
			Method method, List<Instruction> list, 
			ArrayList<int[]> switchCaseOffsets,
			ArrayList<FastCodeException> fastCodeExceptions,
			FastCodeException fastCodeException, int fastCodeExceptionIndex)
	{
		switch (fastCodeException.type)
		{
		case FastConstants.TYPE_118_CATCH_FINALLY:
			{
				// Strategie : Trouver l'instruction suivant 'ret' de la sous
				// routine 'finally'.
				int index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.afterOffset);
				if ((index < 0) || (index >= list.size()))
					return;
	
				int length = list.size();
				IntSet offsetSet = new IntSet();
				int retCounter = 0;
				
				// Search 'ret' instruction
				// Permet de prendre en compte les sous routines imbriqu�es
				while (++index < length)
				{
					Instruction i = list.get(index);
					
					switch (i.opcode)
					{
					case ByteCodeConstants.JSR:
						offsetSet.add(((Jsr)i).GetJumpOffset());
						break;
						
					case ByteCodeConstants.RET:
						if (offsetSet.size() == retCounter)
						{
							fastCodeException.afterOffset = i.offset + 1;
							return;
						}
						retCounter++;
						break;
					}
				}
			}
			break;
		case FastConstants.TYPE_118_CATCH_FINALLY_2:
			{
				Instruction instruction = InstructionUtil.getInstructionAt(
						list, fastCodeException.afterOffset);
				if (instruction == null)
					return;
				
				fastCodeException.afterOffset = instruction.offset + 1;
			}
			break;
		case FastConstants.TYPE_118_FINALLY_2:
			{
				int index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.afterOffset);
				if ((index < 0) || (index >= list.size()))
					return;
				
				Instruction i = list.get(++index);
				if (i.opcode != ByteCodeConstants.GOTO)
					return;
				
				fastCodeException.afterOffset = ((Goto)i).GetJumpOffset();				
			}
			break;
		case FastConstants.TYPE_JIKES_122:
			// Le traitement suivant etait faux pour reconstruire la methode
			// "basic.data.TestTryCatchFinally .methodTryFinally1()" compile 
			// par "Eclipse Java Compiler v_677_R32x, 3.2.1 release".
//			{
//				int index = InstructionUtil.getIndexForOffset(
//						list, fastCodeException.afterOffset);
//				if ((index < 0) || (index >= list.size()))
//					return;
//				
//				Instruction i = list.get(++index);
//				
//				fastCodeException.afterOffset = i.offset;
//			}
			break;
		case FastConstants.TYPE_ECLIPSE_677_FINALLY:
			{
				int index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.finallyFromOffset);
				if (index < 0)
					return;
				
				int lenght = list.size();
				Instruction instruction = list.get(index);
				
				switch (instruction.opcode)
				{
				case ByteCodeConstants.POP:
					{
						// Search the first throw instruction
						while (++index < lenght)
						{
							instruction = list.get(index);								
							if (instruction.opcode == ByteCodeConstants.ATHROW)
							{
								fastCodeException.afterOffset = 
									instruction.offset + 1;
								break;
							}
						}
					}
					break;
				case ByteCodeConstants.ASTORE:
					{
						// L'un des deux cas les plus complexes : 
						// - le bloc 'finally' est dupliqu� deux fois.
						// - aucun 'goto' ne saute apres le dernier bloc finally.
						// Methode de calcul de 'afterOffset' : 
						// - compter le nombre d'instructions entre le d�but du 1er bloc 
						//   'finally' et le saut du goto en fin de bloc 'try'.
						// - Ajouter ce nombre � l'index de l'instruction vers laquelle 
						//   saute le 'goto' precedent le 1er bloc 'finally'.
						int finallyStartIndex = index+1;
						int exceptionIndex = ((AStore)instruction).index;
							
						// Search throw instruction
						while (++index < lenght)
						{
							instruction = list.get(index);								
							if (instruction.opcode == ByteCodeConstants.ATHROW)
							{
								AThrow athrow = (AThrow)instruction;									
								if ((athrow.value.opcode == ByteCodeConstants.ALOAD) && 
									( ((ALoad)athrow.value).index == exceptionIndex ))
									break;
							}
						}
						
						index += (index - finallyStartIndex + 1);
						
						if (index < lenght)
							fastCodeException.afterOffset = list.get(index).offset;
					}
					break;
				}
			}
			break;
		case FastConstants.TYPE_ECLIPSE_677_CATCH_FINALLY:	
			{
				// L'un des deux cas les plus complexes : 
				// - le bloc 'finally' est dupliqu� deux ou trois fois.
				// - aucun 'goto' ne saute apres le dernier bloc finally.
				// Methode de calcul de 'afterOffset' : 
				// - compter le nombre d'instructions entre le d�but du 1er bloc 
				//   'finally' et le saut du goto en fin de bloc 'try'.
				// - Ajouter ce nombre � l'index de l'instruction vers laquelle 
				//   saute le 'goto' precedent le 1er bloc 'finally'.
				int index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.finallyFromOffset);
				if (index < 0)
					return;
				
				Instruction instruction = list.get(index);
				
				if (instruction.opcode != ByteCodeConstants.ASTORE)
					return;
				
				int finallyStartIndex = index+1;
				int exceptionIndex = ((AStore)instruction).index;
				int lenght = list.size();
					
				// Search throw instruction
				while (++index < lenght)
				{
					instruction = list.get(index);								
					if (instruction.opcode == ByteCodeConstants.ATHROW)
					{
						AThrow athrow = (AThrow)instruction;									
						if ((athrow.value.opcode == ByteCodeConstants.ALOAD) && 
							( ((ALoad)athrow.value).index == exceptionIndex ))
							break;
					}
				}
				
				int delta = index - finallyStartIndex;				
				index += delta + 1;
				int afterOffset = list.get(index).offset;
				
				// Verification de la presence d'un bloc 'finally' pour les blocs 
				// 'catch'.
				if ((index < lenght) && 
					(list.get(index).opcode == ByteCodeConstants.GOTO))
				{
					Goto g = (Goto)list.get(index);
					int jumpOffset = g.GetJumpOffset();
					int indexTmp = index + delta + 1;
					
					if ((indexTmp < lenght) &&
					    (list.get(indexTmp-1).offset < jumpOffset) && 
						(jumpOffset <= list.get(indexTmp).offset))
					{
						// Reduction de 'afterOffset' a l'aide des 'Branch Instructions'
						afterOffset = ReduceAfterOffsetWithBranchInstructions(
							list, fastCodeException, 
							fastCodeException.finallyFromOffset, 
							list.get(indexTmp).offset);
						
						// Reduction de 'afterOffset' a l'aide des numeros de ligne
						if (! fastCodeException.synchronizedFlag)
						{
							afterOffset = ReduceAfterOffsetWithLineNumbers(
								list, fastCodeException, afterOffset);
						}
						
						// Reduction de 'afterOffset' a l'aide des instructions de
						// gestion des exceptions englobantes	
						afterOffset = ReduceAfterOffsetWithExceptions(
							fastCodeExceptions, fastCodeException.tryFromOffset, 
							fastCodeException.finallyFromOffset, afterOffset);
					}
				}

				fastCodeException.afterOffset = afterOffset;
			}
			break;
		case FastConstants.TYPE_118_FINALLY:
			{
				// Re-estimation de la valeur de l'attribut 'afterOffset'.
				// Strategie : le bon offset, apres le bloc 'try-finally', se 
				// trouve apres l'instruction 'ret' de la sous procedure du 
				// bloc 'finally'.
				int index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.finallyFromOffset);
				if (index <= 0)
					return;
				
				int length = list.size();
				
				// Gestion des instructions JSR imbriquees
				int offsetOfJsrsLength = list.get(length-1).offset + 1;
				boolean[] offsetOfJsrs = new boolean[offsetOfJsrsLength];
				int level = 0;
				
				while (++index < length)
				{
					Instruction i = list.get(index);
					
					if (offsetOfJsrs[i.offset])
						level++;

					if (i.opcode == ByteCodeConstants.JSR)
					{
						int jumpOffset = ((Jsr)i).GetJumpOffset();
						if (jumpOffset < offsetOfJsrsLength)
							offsetOfJsrs[jumpOffset] = true;
					}
					else if (i.opcode == ByteCodeConstants.RET)
					{
						if (level <= 1)
						{
							fastCodeException.afterOffset = i.offset+1;
							break;
						}
						else
						{
							level--;
						}
					}
				}	
			}
			break;	
		case FastConstants.TYPE_118_FINALLY_THROW:
		case FastConstants.TYPE_131_CATCH_FINALLY:
			{
				int index = InstructionUtil.getIndexForOffset(
						list, fastCodeException.finallyFromOffset);
				if (index <= 0)
					return;
				
				// Search last 'ret' instruction of the finally block
				int length = list.size();
				
				while (++index < length)
				{
					Instruction i = list.get(index);
					if (i.opcode == ByteCodeConstants.RET)
					{
						fastCodeException.afterOffset = (++index < length) ?
							list.get(index).offset :
							i.offset+1;
						break;
					}
				}
			}
			break;	
		default:
			{
				int length = list.size();
	
				// Re-estimation de la valeur de l'attribut 'afterOffset'.
				// Strategie : parcours du bytecode jusqu'a trouver une 
				// instruction de saut vers la derniere instruction 'return', 
				// ou une instruction 'athrow' ou une instruction de saut 
				// n�gatif allant en deca du debut du dernier block. Le parcours
				// du bytecode doit prendre en compte les sauts positifs.
				
				// Calcul de l'offset apres la structure try-catch
				int afterOffset = fastCodeException.afterOffset;
				if (afterOffset == -1)
					afterOffset = list.get(length-1).offset + 1;
				
				// Reduction de 'afterOffset' a l'aide des 'Branch Instructions'		
				afterOffset = ReduceAfterOffsetWithBranchInstructions(
						list, fastCodeException, fastCodeException.maxOffset, 
						afterOffset);
					
				// Reduction de 'afterOffset' a l'aide des numeros de ligne
				if (! fastCodeException.synchronizedFlag)
				{
					afterOffset = ReduceAfterOffsetWithLineNumbers(
							list, fastCodeException, afterOffset);
				}
				
				// Reduction de 'afterOffset' a l'aide des instructions 'switch'
				afterOffset = ReduceAfterOffsetWithSwitchInstructions(
						switchCaseOffsets, fastCodeException.tryFromOffset, 
						fastCodeException.maxOffset, afterOffset);
				
				// Reduction de 'afterOffset' a l'aide des instructions de gestion
				// des exceptions englobantes	
				fastCodeException.afterOffset = afterOffset = 
					ReduceAfterOffsetWithExceptions(
						fastCodeExceptions, fastCodeException.tryFromOffset, 
						fastCodeException.maxOffset, afterOffset);
						
				// Recherche de la 1ere exception d�butant apres 'maxOffset' 
				int tryFromOffset = Integer.MAX_VALUE;
				int tryIndex = fastCodeExceptionIndex + 1;
				while (tryIndex < fastCodeExceptions.size())
				{
					int tryFromOffsetTmp = 
						fastCodeExceptions.get(tryIndex).tryFromOffset;
					if (tryFromOffsetTmp > fastCodeException.maxOffset)
					{
						tryFromOffset = tryFromOffsetTmp;
						break;
					}
					tryIndex++;
				}
					
				// Parcours	
				int maxIndex = InstructionUtil.getIndexForOffset(
						list, fastCodeException.maxOffset);
				int index = maxIndex;
				while (index < length)
				{
					Instruction instruction = list.get(index);
						
					if (instruction.offset >= afterOffset)
						break;
	
					if (instruction.offset > tryFromOffset)
					{
						// Saut des blocs try-catch-finally
						FastCodeException fce = 
							fastCodeExceptions.get(tryIndex);
						int afterOffsetTmp = fce.afterOffset;
						
						// Recherche du plus grand offset de fin parmi toutes 
						// les exceptions d�butant � l'offset 'tryFromOffset'
						for (;;)
						{
							if (++tryIndex >= fastCodeExceptions.size())
							{
								tryFromOffset = Integer.MAX_VALUE;
								break;
							}			
							int tryFromOffsetTmp = 
								fastCodeExceptions.get(tryIndex).tryFromOffset;
							if (fce.tryFromOffset != tryFromOffsetTmp)
							{
								tryFromOffset = tryFromOffsetTmp;
								break;
							}	
							FastCodeException fceTmp = 
								fastCodeExceptions.get(tryIndex);
							if (afterOffsetTmp < fceTmp.afterOffset)
								afterOffsetTmp = fceTmp.afterOffset;
						}
							
						while ((index < length) && 
							   (list.get(index).offset < afterOffsetTmp))
							index++;
					}
					else
					{
						switch (instruction.opcode)
						{
						case ByteCodeConstants.ATHROW:
						case ByteCodeConstants.RETURN:
						case ByteCodeConstants.XRETURN:
							// Verification que toutes les variables
							// locales utilisees sont definie dans le 
							// bloc du dernier catch ou de finally
							if (CheckLocalVariableUsedVisitor.Visit(
									method.getLocalVariables(), 
									fastCodeException.maxOffset, 
									instruction))
							{
								// => Instruction incluse au bloc
								fastCodeException.afterOffset = instruction.offset+1;				
							}
							// Verification que l'instruction participe a un
							// operateur ternaire
							else if (CheckTernaryOperator(list, index))
							{
								// => Instruction incluse au bloc
								fastCodeException.afterOffset = instruction.offset+1;							
							}
							else
							{
								if (index+1 >= length)
								{
									// Derniere instruction de la liste									
									if (instruction.opcode == ByteCodeConstants.ATHROW)
									{
										// Dernier 'throw' 
										// => Instruction incluse au bloc
										fastCodeException.afterOffset = instruction.offset+1;	
									}
									else
									{
										// Dernier 'return' 
										// => Instruction placee apres le bloc
										fastCodeException.afterOffset = instruction.offset;
									}
								}
								else
								{
									// Une instruction du bloc 'try-catch-finally' 
									// saute-t-elle vers l'instuction qui suit  
									// cette instruction ?
									int tryFromIndex = 
										InstructionUtil.getIndexForOffset(
											list, fastCodeException.tryFromOffset);
									int beforeInstructionOffset = (index==0) ? 
										0 : list.get(index-1).offset;
									
									if (InstructionUtil.CheckNoJumpToInterval(
										list, tryFromIndex, maxIndex, 
										beforeInstructionOffset, instruction.offset))
									{
										// Aucune instruction du bloc 
										// 'try-catch-finally' ne saute vers 
										// cette instruction.
										// => Instruction incluse au bloc
										fastCodeException.afterOffset = instruction.offset+1;
									}
									else
									{
										// Une instruction du bloc 
										// 'try-catch-finally' saute vers 
										// cette instruction.
										// => Instruction placee apres le bloc
										fastCodeException.afterOffset = instruction.offset;
									}
								}
							}							
							return;
						case ByteCodeConstants.GOTO:	
						case ByteCodeConstants.IFCMP:
						case ByteCodeConstants.IF:
						case ByteCodeConstants.IFXNULL:
							int jumpOffsetTmp;
							
							if (instruction.opcode == ByteCodeConstants.GOTO)
							{
								jumpOffsetTmp = 
									((BranchInstruction)instruction).GetJumpOffset();
							}
							else
							{
								// L'aggregation des instructions 'if' n'a pas
								// encore ete executee. Recherche du plus petit
								// offset de saut parmi toutes les instructions
								// 'if' qui suivent.
								index = ComparisonInstructionAnalyzer.GetLastIndex(
									list, index);
								BranchInstruction lastBi = 
									(BranchInstruction)list.get(index);
								jumpOffsetTmp = lastBi.GetJumpOffset();
							}
							
							if (jumpOffsetTmp > instruction.offset)
							{
								// Saut positif
								if (jumpOffsetTmp < afterOffset)
								{
									while (++index < length)	
									{
										if (list.get(index).offset >= jumpOffsetTmp)
										{
											--index;
											break;
										}
									}
								}
								else 
								{
									if ((instruction.opcode == ByteCodeConstants.GOTO) ||
										(jumpOffsetTmp != afterOffset))
									{
										// Une instruction du bloc 'try-catch-finally' 
										// saute-t-elle vers cett instuction ?
										int tryFromIndex = 
											InstructionUtil.getIndexForOffset(
												list, fastCodeException.tryFromOffset);
										int beforeInstructionOffset = (index==0) ? 
												0 : list.get(index-1).offset;
										
										if (InstructionUtil.CheckNoJumpToInterval(
												list, tryFromIndex, maxIndex, 
												beforeInstructionOffset, instruction.offset))
										{
											// Aucune instruction du bloc 
											// 'try-catch-finally' ne saute vers 
											// cette instuction 
											// => Instruction incluse au bloc
											fastCodeException.afterOffset = instruction.offset+1;
										}
										else
										{
											// Une instruction du bloc 
											// 'try-catch-finally' saute vers 
											// cette instuction 
											// => Instruction plac�e apres le bloc
											fastCodeException.afterOffset = instruction.offset;
										}
									}
									//else
									//{
										// Si l'instruction est un saut conditionnel
										// et si l'offset de saut est le meme que 'afterOffset',
										// alors l'instruction fait partie du dernier bloc.
									//}
									return;
								}
							}
							else if (jumpOffsetTmp <= fastCodeException.tryFromOffset)
							{
								// Saut negatif
								if ((index > 0) && 
									(instruction.lineNumber != Instruction.UNKNOWN_LINE_NUMBER))
								{
									Instruction beforeInstruction = list.get(index-1);									
									if (instruction.lineNumber ==
											beforeInstruction.lineNumber)
									{
										// For instruction ?
										if ((beforeInstruction.opcode == 
												ByteCodeConstants.ASTORE) && 
											((AStore)beforeInstruction).valueref.opcode == 
												ByteCodeConstants.EXCEPTIONLOAD)
										{
											// Non
											fastCodeException.afterOffset = 
												instruction.offset;											
										}
										else if ((beforeInstruction.opcode == 
												ByteCodeConstants.POP) && 
											((Pop)beforeInstruction).objectref.opcode == 
												ByteCodeConstants.EXCEPTIONLOAD)
										{
											// Non
											fastCodeException.afterOffset = 
												instruction.offset;											
										}
										else
										{
											// Oui
											fastCodeException.afterOffset = 
												beforeInstruction.offset;											
										}
										return;
									}	
								}
								fastCodeException.afterOffset = 
									instruction.offset;								
								return;
							}							
							break;
						case FastConstants.LOOKUPSWITCH:
						case FastConstants.TABLESWITCH:
							Switch s = (Switch)instruction;
							
							// Search max offset
							int maxOffset = s.defaultOffset;
							int i = s.offsets.length;
							while (i-- > 0)
							{
								int offset = s.offsets[i];
								if (maxOffset < offset)
									maxOffset = offset;
							}
								
							if (maxOffset < afterOffset)
							{
								while (++index < length)	
								{
									if (list.get(index).offset >= maxOffset)
									{
										--index;
										break;
									}
								}
							}
							break;							
						}
						index++;
					}
				}
			}
		}
	}
	
	private static boolean CheckTernaryOperator(List<Instruction> list, int index)
	{
		// Motif des operateurs ternaires :
		//  index-3) If instruction (IF || IFCMP || IFXNULL || COMPLEXIF)
		//  index-2) TernaryOpStore
		//  index-1) Goto
		//    index) (X)Return 
		if ((index > 2) && 
			(list.get(index-1).opcode == FastConstants.GOTO) && 
			(list.get(index-2).opcode == FastConstants.TERNARYOPSTORE))
		{
			Goto g = (Goto)list.get(index-1);
			int jumpOffset = g.GetJumpOffset();
			int returnOffset = list.get(index).offset;
			if ((g.offset < jumpOffset) && (jumpOffset < returnOffset))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static int ReduceAfterOffsetWithBranchInstructions(
		List<Instruction> list, FastCodeException fastCodeException, 
		int firstOffset, int afterOffset)
	{
		Instruction instruction;
		
		// Check previous instructions
		int index = InstructionUtil.getIndexForOffset(
				list, fastCodeException.tryFromOffset);
		
		if (index != -1)
		{
			while (index-- > 0)
			{
				instruction = list.get(index);
				
				switch (instruction.opcode)
				{
				case ByteCodeConstants.IF:
				case ByteCodeConstants.IFCMP:
				case ByteCodeConstants.IFXNULL:	
				case ByteCodeConstants.GOTO:
					int jumpOffset = ((BranchInstruction)instruction).GetJumpOffset();
					if ((firstOffset < jumpOffset) && (jumpOffset < afterOffset))
						afterOffset = jumpOffset;
				}
			}
		}
		
		// Check next instructions
		index = list.size();
		do
		{
			instruction = list.get(--index);
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.IF:
			case ByteCodeConstants.IFCMP:
			case ByteCodeConstants.IFXNULL:	
			case ByteCodeConstants.GOTO:
				int jumpOffset = ((BranchInstruction)instruction).GetJumpOffset();
				if ((firstOffset < jumpOffset) && (jumpOffset < afterOffset))
					afterOffset = jumpOffset;
			}
		}		
		while (instruction.offset > afterOffset);
		
		return afterOffset;
	}

	private static int ReduceAfterOffsetWithLineNumbers(
		List<Instruction> list, FastCodeException fastCodeException, 
		int afterOffset)
	{
		int fromIndex = InstructionUtil.getIndexForOffset(
				list, fastCodeException.tryFromOffset);
		int index = fromIndex;
		
		if (index != -1)
		{
			// Search first line number
			int lenght = list.size();
			int firstLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
			Instruction instruction;
			
			do
			{
				instruction = list.get(index++);
				
				if (instruction.lineNumber != Instruction.UNKNOWN_LINE_NUMBER)
				{
					firstLineNumber = instruction.lineNumber;
					break;
				}				
			}
			while ((instruction.offset < afterOffset) && (index < lenght));
			
			if (firstLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
			{
				// Exclude instruction with a smaller line number
				int maxOffset = fastCodeException.maxOffset;
				index = InstructionUtil.getIndexForOffset(list, afterOffset);
				
				if (index != -1)
				{
					while (index-- > 0)
					{
						instruction = list.get(index);
						
						if ((instruction.offset <= maxOffset))
						{
							break;
						}
						
						if ((instruction.lineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
							(instruction.lineNumber >= firstLineNumber))
						{
							break;
						}
						
						// L'instruction a un numero de ligne inferieur aux
						// instructions du bloc 'try'. A priori, elle doit etre
						// place apres le bloc 'catch'.
						
						// Est-ce une instruction de saut ? Si oui, est-ce que
						// la placer hors du bloc 'catch' genererait deux points
						// de sortie du bloc ?						
						if (instruction.opcode == ByteCodeConstants.GOTO)
						{
							int jumpOffset = ((Goto)instruction).GetJumpOffset();
							
							if (! InstructionUtil.CheckNoJumpToInterval(
									list,
									fromIndex, index, 
									jumpOffset-1, jumpOffset))
							{
								break;
							}
						}

						// Est-ce une instruction 'return' ? Si oui, est-ce que
						// la placer hors du bloc 'catch' genererait deux points
						// de sortie du bloc ?						
						if (instruction.opcode == ByteCodeConstants.RETURN)
						{
							int maxIndex = InstructionUtil.getIndexForOffset(
									list, maxOffset);
							
							if (list.get(maxIndex-1).opcode == instruction.opcode)
							{
								break;
							}
						}
						
						/*
						 * A QUOI SERT CE BLOC ? A QUEL CAS D'UTILISATION
						 * CORRESPOND T IL ?
						 * / 
						if (instruction.opcode != ByteCodeConstants.IINC)
						{
							if (// Check previous instructions
								InstructionUtil.CheckNoJumpToInterval(
									list,
									0, index, 
									maxOffset, instruction.offset) &&
								// Check next instructions			
								InstructionUtil.CheckNoJumpToInterval(
									list,
									index+1, lenght, 
									maxOffset, instruction.offset))
							{
								break;
							}
						}
						/ * */
						
						afterOffset = instruction.offset;
					}
				}
			}
		}
		
		return afterOffset;
	}

	private static int ReduceAfterOffsetWithSwitchInstructions(
		ArrayList<int[]> switchCaseOffsets, 
		int firstOffset, int lastOffset, int afterOffset)
	{
		int i = switchCaseOffsets.size();
		while (i-- > 0)
		{
			int[] offsets = switchCaseOffsets.get(i);
			
			int j = offsets.length;
			if (j > 1)
			{
				int offset2 = offsets[--j];
				
				while (j-- > 0)
				{
					int offset1 = offsets[j];
					
					if ((offset1 != -1) &&
						(offset1 <= firstOffset) && (lastOffset < offset2))
					{
						if ((afterOffset == -1) || (afterOffset > offset2))
							afterOffset = offset2;
					}
					
					offset2 = offset1;
				}
			}
		}
		
		return afterOffset;
	}
	
	private static int ReduceAfterOffsetWithExceptions(
		ArrayList<FastCodeException> fastCodeExceptions, 
		int fromOffset, int maxOffset, int afterOffset)
	{
		int i = fastCodeExceptions.size();
		while (i-- > 0)
		{
			FastCodeException fastCodeException = fastCodeExceptions.get(i);
			
			int toOffset = fastCodeException.finallyFromOffset;
			
			if (fastCodeException.catches != null)
			{
				int j = fastCodeException.catches.size();
				while (j-- > 0)
				{
					FastCodeExceptionCatch fcec = fastCodeException.catches.get(j);
					
					if ((toOffset != -1) && 
						(fcec.fromOffset <= fromOffset) && 
						(maxOffset < toOffset))
					{
						if ((afterOffset == -1) || (afterOffset > toOffset))
							afterOffset = toOffset;
					}					
					
					toOffset = fcec.fromOffset;
				}
			}
			
			if ((fastCodeException.tryFromOffset <= fromOffset) && 
				(maxOffset < toOffset))
			{
				if ((afterOffset == -1) || (afterOffset > toOffset))
					afterOffset = toOffset;
			}
		}

		return afterOffset;
	}
	
	public static void FormatFastTry(
		LocalVariables localVariables, FastCodeException fce, 
		FastTry fastTry, int returnOffset)
	{
		switch (fce.type)
		{
		case FastConstants.TYPE_CATCH:
			FormatCatch(localVariables, fce, fastTry);
			break;
		case FastConstants.TYPE_118_FINALLY:
			Format118Finally(localVariables, fce, fastTry);
			break;
		case FastConstants.TYPE_118_FINALLY_2:
			Format118Finally2(fce, fastTry);
			break;
		case FastConstants.TYPE_118_FINALLY_THROW:
			Format118FinallyThrow(fastTry);
			break;			
		case FastConstants.TYPE_118_CATCH_FINALLY:
			Format118CatchFinally(fce, fastTry);
			break;
		case FastConstants.TYPE_118_CATCH_FINALLY_2:
			Format118CatchFinally2(fce, fastTry);
			break;
		case FastConstants.TYPE_131_CATCH_FINALLY:
			Format131CatchFinally(localVariables, fce, fastTry);
			break;
		case FastConstants.TYPE_142:
			Format142(localVariables, fce, fastTry);
			break;
		case FastConstants.TYPE_142_FINALLY_THROW:
			Format142FinallyThrow(fastTry);
			break;
		case FastConstants.TYPE_JIKES_122:
			FormatJikes122(localVariables, fce, fastTry, returnOffset);		
			break;
		case FastConstants.TYPE_ECLIPSE_677_FINALLY:
			FormatEclipse677Finally(fce, fastTry);	
			break;
		case FastConstants.TYPE_ECLIPSE_677_CATCH_FINALLY:
			FormatEclipse677CatchFinally(fce, fastTry, returnOffset);
		}
	}
	
	private static void FormatCatch(
		LocalVariables localVariables, FastCodeException fce, FastTry fastTry)
	{
		List<Instruction> tryInstructions = fastTry.instructions;
		int jumpOffset = -1;

		// Remove last 'goto' instruction in try block
		if (tryInstructions.size() > 0)
		{
			int lastIndex = tryInstructions.size() - 1;
			Instruction instruction = tryInstructions.get(lastIndex);
			
			if (instruction.opcode == ByteCodeConstants.GOTO)
			{
				int tmpJumpOffset = ((Goto)instruction).GetJumpOffset();
				
				if ((tmpJumpOffset < fce.tryFromOffset) || 
					(instruction.offset < tmpJumpOffset))
				{
					jumpOffset = tmpJumpOffset;
					fce.tryToOffset = instruction.offset;
					tryInstructions.remove(lastIndex);
				}
			}
		}
		
		// Remove JSR instruction in try block before 'return' instruction
		FormatFastTry_RemoveJsrInstructionAndCompactStoreReturn(
			tryInstructions, localVariables, Instruction.UNKNOWN_LINE_NUMBER);
		
		int i = fastTry.catches.size();	
		while (i-- > 0)
		{
			List<Instruction> catchInstructions = 
				fastTry.catches.get(i).instructions;
			
			// Remove first catch instruction in each catch block
			if (FormatCatch_RemoveFirstCatchInstruction(catchInstructions.get(0)))
				catchInstructions.remove(0);
			
			// Remove last 'goto' instruction
			if (catchInstructions.size() > 0)
			{
				int lastIndex = catchInstructions.size() - 1;
				Instruction instruction = catchInstructions.get(lastIndex);
				
				if (instruction.opcode == ByteCodeConstants.GOTO)
				{
					int tmpJumpOffset = ((Goto)instruction).GetJumpOffset();
					
					if ((tmpJumpOffset < fce.tryFromOffset) || 
						(instruction.offset < tmpJumpOffset))
					{
						if (jumpOffset == -1)
						{
							jumpOffset = tmpJumpOffset;
							fce.catches.get(i).toOffset = instruction.offset;
							catchInstructions.remove(lastIndex);
						}
						else if (jumpOffset == tmpJumpOffset)
						{
							fce.catches.get(i).toOffset = instruction.offset;
							catchInstructions.remove(lastIndex);
						}
					}
				}
				
				// Remove JSR instruction in try block before 'return' instruction
				FormatFastTry_RemoveJsrInstructionAndCompactStoreReturn(
					catchInstructions, localVariables, 
					Instruction.UNKNOWN_LINE_NUMBER);
			}
		}
	}
	
	private static boolean FormatCatch_RemoveFirstCatchInstruction(
		Instruction instruction)
	{
		switch (instruction.opcode) 
		{
		case ByteCodeConstants.POP:
			return 
				((Pop)instruction).objectref.opcode == 
				ByteCodeConstants.EXCEPTIONLOAD;	

		case ByteCodeConstants.ASTORE:
			return 
				((AStore)instruction).valueref.opcode == 
				ByteCodeConstants.EXCEPTIONLOAD;	

		default:
			return false;
		}
	}
	
	private static void Format118Finally(
		LocalVariables localVariables, FastCodeException fce, FastTry fastTry)
	{
		List<Instruction> tryInstructions = fastTry.instructions;
		int length = tryInstructions.size();
		
		// Remove last 'goto' instruction in try block
		if (tryInstructions.get(--length).opcode == ByteCodeConstants.GOTO)
		{
			Goto g = (Goto)tryInstructions.remove(length);
			fce.tryToOffset = g.offset;
		}
		// Remove last 'jsr' instruction in try block
		if (tryInstructions.get(--length).opcode != ByteCodeConstants.JSR)
			throw new UnexpectedInstructionException();
		tryInstructions.remove(length);
		
		// Remove JSR instruction in try block before 'return' instruction
		int finallyInstructitonsLineNumber = 
				fastTry.finallyInstructions.get(0).lineNumber;
		FormatFastTry_RemoveJsrInstructionAndCompactStoreReturn(
			tryInstructions, localVariables, finallyInstructitonsLineNumber);

		Format118FinallyThrow(fastTry);
	}

	private static void Format118Finally2(
		FastCodeException fce, FastTry fastTry)
	{
		List<Instruction> tryInstructions = fastTry.instructions;
		int tryInstructionsLength = tryInstructions.size();
		
		// Remove last 'goto' instruction in try block
		if (tryInstructions.get(tryInstructionsLength-1).opcode == 
				ByteCodeConstants.GOTO)
		{
			Goto g = (Goto)tryInstructions.remove(--tryInstructionsLength);
			fce.tryToOffset = g.offset;
		}
		
		List<Instruction> finallyInstructions = fastTry.finallyInstructions;
		int finallyInstructionsLength = finallyInstructions.size();

		// Update all offset of instructions 'goto' and 'ifxxx' if 
		// (finallyInstructions.gt(0).offset) < (jump offset) && 
		// (jump offset) < (finallyInstructions.gt(5).offset)
		if (finallyInstructionsLength > 5)
		{
			int firstFinallyOffset = finallyInstructions.get(0).offset;
			int lastFinallyOffset = finallyInstructions.get(5).offset;
			
			while (tryInstructionsLength-- > 0)
			{
				Instruction instruction = 
					tryInstructions.get(tryInstructionsLength);
				int jumpOffset;
				
				switch (instruction.opcode)
				{
				case ByteCodeConstants.IFCMP:
					{
						jumpOffset = ((IfCmp)instruction).GetJumpOffset();
						
						if ((firstFinallyOffset < jumpOffset) && 
							(jumpOffset <= lastFinallyOffset))
							((IfCmp)instruction).branch = 
								firstFinallyOffset - instruction.offset;
					}
					break;
				case ByteCodeConstants.IF:
				case ByteCodeConstants.IFXNULL:
					{
						jumpOffset = 
							((IfInstruction)instruction).GetJumpOffset();
						
						if ((firstFinallyOffset < jumpOffset) && 
							(jumpOffset <= lastFinallyOffset))
							((IfInstruction)instruction).branch = 
								firstFinallyOffset - instruction.offset;						
					}
					break;
				case ByteCodeConstants.COMPLEXIF:
					{
						jumpOffset = 
							((BranchInstruction)instruction).GetJumpOffset();
						
						if ((firstFinallyOffset < jumpOffset) && 
							(jumpOffset <= lastFinallyOffset))
							((ComplexConditionalBranchInstruction)instruction).branch = 
								firstFinallyOffset - instruction.offset;						
					}
					break;
				case ByteCodeConstants.GOTO:
					{
						jumpOffset = ((Goto)instruction).GetJumpOffset();
						
						if ((firstFinallyOffset < jumpOffset) && 
							(jumpOffset <= lastFinallyOffset))
							((Goto)instruction).branch = 
								firstFinallyOffset - instruction.offset;
					}
					break;
				}
			}
		}
		
		// Remove last 'ret' instruction in finally block
		finallyInstructions.remove(finallyInstructionsLength - 1);
		// Remove 'AStore ExceptionLoad' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'jsr' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'athrow' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'jsr' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'goto' instruction in finally block
		finallyInstructions.remove(0);		
		// Remove 'AStore ReturnAddressLoad' instruction in finally block
		finallyInstructions.remove(0);
	}

	private static void Format118FinallyThrow(FastTry fastTry)
	{
		List<Instruction> finallyInstructions = fastTry.finallyInstructions;
		int length = finallyInstructions.size();
		
		// Remove last 'ret' instruction in finally block
		Instruction i = finallyInstructions.get(--length);
		if (i.opcode != ByteCodeConstants.RET)
			throw new UnexpectedInstructionException();
		finallyInstructions.remove(length);
		// Remove 'AStore ExceptionLoad' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'jsr' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'athrow' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'astore' instruction (returnAddress) in finally block
		finallyInstructions.remove(0);
	}
	
	private static void Format118CatchFinally(
			FastCodeException fce, FastTry fastTry)
	{
		List<Instruction> tryInstructions = fastTry.instructions;
		int tryInstructionsLength = tryInstructions.size();
		
		// Remove last 'goto' instruction in try block
		if (tryInstructions.get(--tryInstructionsLength).opcode == 
			ByteCodeConstants.GOTO)
		{
			Goto g = (Goto)tryInstructions.remove(tryInstructionsLength);
			fce.tryToOffset = g.offset;
		}
		
		// Format catch blockes
		int i = fastTry.catches.size()-1;			
		if (i >= 0)
		{			
			List<Instruction>  catchInstructions = 
				fastTry.catches.get(i).instructions;
			int catchInstructionsLength = catchInstructions.size();
			
			switch (catchInstructions.get(--catchInstructionsLength).opcode)
			{
			case ByteCodeConstants.GOTO:
				// Remove 'goto' instruction in catch block				
				catchInstructions.remove(catchInstructionsLength);
				// Remove 'jsr' instruction in catch block				
				catchInstructions.remove(--catchInstructionsLength);
				break;
			case ByteCodeConstants.RETURN:
			case ByteCodeConstants.XRETURN:
				// Remove 'jsr' instruction in catch block				
				catchInstructions.remove(--catchInstructionsLength);
				
				if ((catchInstructionsLength > 0) && 
					(catchInstructions.get(catchInstructionsLength-1).opcode == ByteCodeConstants.ATHROW))
				{
					// Remove 'return' instruction after a 'throw' instruction		
					catchInstructions.remove(catchInstructionsLength);
				}
				
				break;
			}
			
			// Remove first catch instruction in each catch block
			catchInstructions.remove(0);

			while (i-- > 0)
			{
				catchInstructions = fastTry.catches.get(i).instructions;
				catchInstructionsLength = catchInstructions.size();
				
				switch (catchInstructions.get(--catchInstructionsLength).opcode)
				{
				case ByteCodeConstants.GOTO:
					// Remove 'goto' instruction in catch block				
					Instruction in = 
						catchInstructions.remove(catchInstructionsLength);
					fce.catches.get(i).toOffset = in.offset;
					break;
				case ByteCodeConstants.RETURN:
				case ByteCodeConstants.XRETURN:
					// Remove 'jsr' instruction in catch block				
					catchInstructions.remove(--catchInstructionsLength);
					break;
				}
				
				// Remove first catch instruction in each catch block
				catchInstructions.remove(0);
			}
		}

		List<Instruction>  finallyInstructions = fastTry.finallyInstructions;
		int finallyInstructionsLength = finallyInstructions.size();
		
		// Remove last 'ret' instruction in finally block
		finallyInstructions.remove(--finallyInstructionsLength);
		// Remove 'AStore ExceptionLoad' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'jsr' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'athrow' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'AStore ExceptionLoad' instruction in finally block
		finallyInstructions.remove(0);
	}

	private static void Format118CatchFinally2(
		FastCodeException fce, FastTry fastTry)
	{
		List<Instruction> tryInstructions = fastTry.instructions;
		int tryInstructionsLength = tryInstructions.size();
		
		// Remove last 'goto' instruction in try block
		if (tryInstructions.get(--tryInstructionsLength).opcode == 
			ByteCodeConstants.GOTO)
		{
			Goto g = (Goto)tryInstructions.remove(tryInstructionsLength);
			fce.tryToOffset = g.offset;
		}
		
		// Format catch blockes
		int i = fastTry.catches.size();			
		while (i-- > 0)
		{
			List<Instruction> catchInstructions = fastTry.catches.get(i).instructions;
			int catchInstructionsLength = catchInstructions.size();
			// Remove 'goto' instruction in catch block				
			Instruction in = 
				catchInstructions.remove(catchInstructionsLength - 1);
			fce.catches.get(i).toOffset = in.offset;
			// Remove first catch instruction in each catch block
			catchInstructions.remove(0);
		}

		// Remove 'Pop ExceptionLoad' instruction in finally block
		List<Instruction>  finallyInstructions = fastTry.finallyInstructions;
		finallyInstructions.remove(0);		
	}	

	/* Deux variantes existes. La sous procedure [finally] ne se trouve pas
	 * toujours dans le block 'finally'. 
	 */
	private static void Format131CatchFinally(
		LocalVariables localVariables, FastCodeException fce, FastTry fastTry)
	{
		List<Instruction> tryInstructions = fastTry.instructions;
		int length = tryInstructions.size();
		
		// Remove last 'goto' instruction in try block
		if (tryInstructions.get(--length).opcode == ByteCodeConstants.GOTO)
		{
			Goto g = (Goto)tryInstructions.remove(length);
			fce.tryToOffset = g.offset;
		}
		// Remove JSR instruction in try block before 'return' instruction
		int finallyInstructitonsLineNumber = 
				fastTry.finallyInstructions.get(0).lineNumber;
		int jumpOffset = FormatFastTry_RemoveJsrInstructionAndCompactStoreReturn(
				tryInstructions, localVariables, finallyInstructitonsLineNumber);
		// Remove last 'jsr' instruction in try block
		length = tryInstructions.size();
		if (tryInstructions.get(--length).opcode == ByteCodeConstants.JSR)
		{
			Jsr jsr = (Jsr)tryInstructions.remove(length);
			jumpOffset = jsr.GetJumpOffset();		
		}
		if (jumpOffset == -1)
			throw new UnexpectedInstructionException();
		
		List<Instruction> finallyInstructions = fastTry.finallyInstructions;

		if (jumpOffset < finallyInstructions.get(0).offset)
		{
			// La sous procedure [finally] se trouve dans l'un des blocs 'catch'.
			
			// Recherche et extraction de la sous procedure
			int i = fastTry.catches.size();	
			while (i-- > 0)
			{
				List<Instruction> catchInstructions = 
					fastTry.catches.get(i).instructions;
				
				if ((catchInstructions.size() == 0) || 
					(catchInstructions.get(0).offset > jumpOffset))
					continue;

				// Extract
				int index = 
					InstructionUtil.getIndexForOffset(catchInstructions, jumpOffset);
				finallyInstructions.clear();
				
				while (catchInstructions.get(index).opcode != ByteCodeConstants.RET)
					finallyInstructions.add(catchInstructions.remove(index));
				if (catchInstructions.get(index).opcode == ByteCodeConstants.RET)
					finallyInstructions.add(catchInstructions.remove(index));
				
				break;
			}

			// Format catch blockes
			i = fastTry.catches.size();	
			while (i-- > 0)
			{
				List<Instruction> catchInstructions = 
					fastTry.catches.get(i).instructions;
				length = catchInstructions.size();
				
				// Remove last 'goto' instruction
				if (catchInstructions.get(--length).opcode == ByteCodeConstants.GOTO)
				{
					Goto g = (Goto)catchInstructions.remove(length);
					fce.catches.get(i).toOffset = g.offset;
				}
				// Remove last 'jsr' instruction
				if (catchInstructions.get(--length).opcode == ByteCodeConstants.JSR)
					catchInstructions.remove(length);
				// Remove JSR instruction in try block before 'return' instruction
				FormatFastTry_RemoveJsrInstructionAndCompactStoreReturn(
					catchInstructions, localVariables, 
					finallyInstructitonsLineNumber);
				// Remove first catch instruction in each catch block
				catchInstructions.remove(0);
			}
			
			// Format finally block
			length = finallyInstructions.size();
			
			// Remove last 'ret' instruction in finally block
			finallyInstructions.remove(--length);
			// Remove 'AStore ReturnAddressLoad' instruction in finally block
			finallyInstructions.remove(0);			
		}
		else
		{	
			// La sous procedure [finally] se trouve dans le bloc 'finally'. 
			
			// Format catch blockes
			int i = fastTry.catches.size();	
			while (i-- > 0)
			{
				List<Instruction> catchInstructions = 
					fastTry.catches.get(i).instructions;
				length = catchInstructions.size();
				
				// Remove last 'goto' instruction
				if (catchInstructions.get(--length).opcode == ByteCodeConstants.GOTO)
				{
					Goto g = (Goto)catchInstructions.remove(length);
					fce.catches.get(i).toOffset = g.offset;
				}
				// Remove last 'jsr' instruction
				if (catchInstructions.get(--length).opcode == ByteCodeConstants.JSR)
					catchInstructions.remove(length);
				// Remove JSR instruction in try block before 'return' instruction
				FormatFastTry_RemoveJsrInstructionAndCompactStoreReturn(
					catchInstructions, localVariables,
					finallyInstructitonsLineNumber);
				// Remove first catch instruction in each catch block
				catchInstructions.remove(0);
			}
			
			// Format finally block
			length = finallyInstructions.size();
			
			// Remove last 'ret' instruction in finally block
			finallyInstructions.remove(--length);
			// Remove 'AStore ExceptionLoad' instruction in finally block
			finallyInstructions.remove(0);
			// Remove 'jsr' instruction in finally block
			finallyInstructions.remove(0);
			// Remove 'athrow' instruction in finally block
			finallyInstructions.remove(0);
			// Remove 'astore' instruction in finally block
			finallyInstructions.remove(0);		
		}
	}
	
	private static void Format142(
		LocalVariables localVariables, FastCodeException fce, FastTry fastTry)
	{
		List<Instruction> finallyInstructions = fastTry.finallyInstructions;
		int finallyInstructitonsSize = finallyInstructions.size();
		
		// Remove last 'athrow' instruction in finally block
		if (finallyInstructions.get(finallyInstructitonsSize-1).opcode == 
				ByteCodeConstants.ATHROW)
		{
			finallyInstructions.remove(finallyInstructitonsSize-1);
		}
		// Remove 'astore' or 'monitorexit' instruction in finally block
		switch (finallyInstructions.get(0).opcode)
		{
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.POP:
			finallyInstructions.remove(0);	
		}
		finallyInstructitonsSize = finallyInstructions.size();
		
		if (finallyInstructitonsSize > 0)
		{
			FastCompareInstructionVisitor visitor = 
				new FastCompareInstructionVisitor();
	
			List<Instruction> tryInstructions = fastTry.instructions;
			int length = tryInstructions.size();
			
			switch (tryInstructions.get(length-1).opcode)
			{
			case ByteCodeConstants.GOTO:
				// Remove last 'goto' instruction in try block
				Goto g = (Goto)tryInstructions.get(--length);
				if (g.branch > 0)
				{
					tryInstructions.remove(length);
					fce.tryToOffset = g.offset;
				}
				break;
			}
		
			// Remove finally instructions in try block before 'return' instruction
			Format142_RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
				localVariables, visitor, tryInstructions, finallyInstructions);
			
			if (fastTry.catches != null)
			{
				// Format catch blockes
				int i = fastTry.catches.size();	
				while (i-- > 0)
				{
					List<Instruction> catchInstructions = 
						fastTry.catches.get(i).instructions;	
					length = catchInstructions.size();
					
					switch (catchInstructions.get(length-1).opcode)
					{
					case ByteCodeConstants.GOTO:
						// Remove last 'goto' instruction in try block
						Goto g = (Goto)catchInstructions.get(--length);
						if (g.branch > 0)
						{
							catchInstructions.remove(length);
							fce.catches.get(i).toOffset = g.offset;
						}
						break;
					}
					
					// Remove finally instructions before 'return' instruction
					Format142_RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
						localVariables, visitor, catchInstructions, finallyInstructions);				
					// Remove first catch instruction in each catch block
					if (catchInstructions.size() > 0)
						catchInstructions.remove(0);
				}		
			}
		}
	}	

	private static void Format142_RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
		LocalVariables localVariables, 
		FastCompareInstructionVisitor visitor,
		List<Instruction> instructions, 
		List<Instruction> finallyInstructions)
	{
		int index = instructions.size();
		int finallyInstructitonsSize = finallyInstructions.size();
		int finallyInstructitonsLineNumber = finallyInstructions.get(0).lineNumber;
			
		boolean match = (index >= finallyInstructitonsSize) && visitor.visit(
			instructions, finallyInstructions, 
			index-finallyInstructitonsSize, 0, finallyInstructitonsSize);
		
		// Remove last finally instructions
		if (match)
		{
			for (int j=0; j<finallyInstructitonsSize && index>0; ++j)
				instructions.remove(--index);
		}
		
		while (index-- > 0)
		{
			Instruction instruction = instructions.get(index);
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.RETURN:
			case ByteCodeConstants.ATHROW:
				{
					match = (index >= finallyInstructitonsSize) && visitor.visit(
						instructions, finallyInstructions, 
						index-finallyInstructitonsSize, 0, finallyInstructitonsSize);
					
					if (match)
					{
						// Remove finally instructions
						for (int j=0; j<finallyInstructitonsSize && index>0; ++j)
							instructions.remove(--index);
					}
					
					if ((instruction.lineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
						(instruction.lineNumber >= finallyInstructitonsLineNumber))
					{
						instruction.lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
					}
				}
				break;
			case ByteCodeConstants.XRETURN:
				{
					match = (index >= finallyInstructitonsSize) && visitor.visit(
						instructions, finallyInstructions, 
						index-finallyInstructitonsSize, 0, finallyInstructitonsSize);
					
					if (match)
					{
						// Remove finally instructions
						for (int j=0; j<finallyInstructitonsSize && index>0; ++j)
							instructions.remove(--index);
					}
					
					// Compact AStore + Return
					ReturnInstruction ri = (ReturnInstruction)instruction;
					
					if (ri.lineNumber != Instruction.UNKNOWN_LINE_NUMBER)
					{				
						switch (ri.valueref.opcode)
						{
						case ByteCodeConstants.ALOAD:
							if (instructions.get(index-1).opcode == ByteCodeConstants.ASTORE)
								index = CompactStoreReturn(
										instructions, localVariables, ri, 
										index, finallyInstructitonsLineNumber);
							break;
						case ByteCodeConstants.LOAD:
							if (instructions.get(index-1).opcode == ByteCodeConstants.STORE)
								index = CompactStoreReturn(
										instructions, localVariables, ri, 
										index, finallyInstructitonsLineNumber);
							break;
						case ByteCodeConstants.ILOAD:
							if (instructions.get(index-1).opcode == ByteCodeConstants.ISTORE)
								index = CompactStoreReturn(
										instructions, localVariables, ri, 
										index, finallyInstructitonsLineNumber);
							break;
						}					
					}
				}
				break;
			case FastConstants.TRY:
				{
					// Recursive calls
					FastTry ft = (FastTry)instruction;
					
					Format142_RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
						localVariables, visitor,
						ft.instructions, finallyInstructions);
					
					if (ft.catches != null)
					{
						int i = ft.catches.size();			
						while (i-- > 0)
						{
							Format142_RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
								localVariables, visitor,
								ft.catches.get(i).instructions, finallyInstructions);
						}
					}
					
					if (ft.finallyInstructions != null)
					{
						Format142_RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
							localVariables, visitor,
							ft.finallyInstructions, finallyInstructions);
					}
				}
				break;
			case FastConstants.SYNCHRONIZED:
				{
					// Recursive calls
					FastSynchronized fs = (FastSynchronized)instruction;
	
					Format142_RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
						localVariables, visitor,
						fs.instructions, finallyInstructions);
				}
				break;
			}
		}
	}
	
	private static int CompactStoreReturn(
		List<Instruction> instructions, LocalVariables localVariables, 
		ReturnInstruction ri, int index, int finallyInstructitonsLineNumber)
	{
		IndexInstruction load = (IndexInstruction)ri.valueref;
		StoreInstruction store = (StoreInstruction)instructions.get(index-1);
		
		if ((load.index == store.index) && 
			((load.lineNumber <= store.lineNumber) ||
			 (load.lineNumber >= finallyInstructitonsLineNumber)))
		{
			// TODO A ameliorer !!
			// Remove local variable
			LocalVariable lv = localVariables.
				getLocalVariableWithIndexAndOffset(
						store.index, store.offset);
			
			if ((lv != null) && (lv.start_pc == store.offset) && 
				(lv.start_pc + lv.length <= ri.offset))
				localVariables.
					removeLocalVariableWithIndexAndOffset(
							store.index, store.offset);
			// Replace returned instruction
			ri.valueref = store.valueref;
			if (ri.lineNumber > store.lineNumber)
				ri.lineNumber = store.lineNumber;
			// Remove 'store' instruction
			instructions.remove(--index);						
		}	
		
		return index;
	}

	private static void Format142FinallyThrow(FastTry fastTry)
	{
		// Remove last 'athrow' instruction in finally block
		fastTry.finallyInstructions.remove(fastTry.finallyInstructions.size()-1);
		// Remove 'astore' instruction in finally block
		fastTry.finallyInstructions.remove(0);		
	}
	
	private static void FormatJikes122(
		LocalVariables localVariables, FastCodeException fce, 
		FastTry fastTry, int returnOffset)
	{
		List<Instruction> tryInstructions = fastTry.instructions;
		int lastIndex = tryInstructions.size()-1;
		Instruction lastTryInstruction = tryInstructions.get(lastIndex);
		int lastTryInstructionOffset = lastTryInstruction.offset;
		
		// Remove last 'goto' instruction in try block
		if (tryInstructions.get(lastIndex).opcode == ByteCodeConstants.GOTO)
		{
			Goto g = (Goto)tryInstructions.remove(lastIndex);
			fce.tryToOffset = g.offset;
		}
		// Remove Jsr instruction before return instructions
		int finallyInstructitonsLineNumber = 
				fastTry.finallyInstructions.get(0).lineNumber;
		FormatFastTry_RemoveJsrInstructionAndCompactStoreReturn(
			tryInstructions, localVariables, finallyInstructitonsLineNumber);
		
		// Format catch blockes
		int i = fastTry.catches.size();	
		while (i-- > 0)
		{
			List<Instruction> catchInstructions = 
				fastTry.catches.get(i).instructions;
			lastIndex = catchInstructions.size()-1;
			
			// Remove last 'goto' instruction in try block
			if (catchInstructions.get(lastIndex).opcode == ByteCodeConstants.GOTO)
			{
				Goto g = (Goto)catchInstructions.remove(lastIndex);
				fce.catches.get(i).toOffset = g.offset;
			}
			// Remove Jsr instruction before return instructions
			FormatFastTry_RemoveJsrInstructionAndCompactStoreReturn(
				catchInstructions, localVariables, 
				finallyInstructitonsLineNumber);	
			// Change negative jump goto to return offset 
			FormatFastTry_FormatNegativeJumpOffset(
				catchInstructions, lastTryInstructionOffset, returnOffset);
			// Remove first catch instruction in each catch block
			catchInstructions.remove(0);
		}

		List<Instruction> finallyInstructions = fastTry.finallyInstructions;
		int length = finallyInstructions.size();

		// Remove last 'jsr' instruction in finally block
		finallyInstructions.remove(--length);
		// Remove last 'ret' or 'athrow' instruction in finally block
		finallyInstructions.remove(--length);
		// Remove 'AStore ExceptionLoad' instruction in finally block
		finallyInstructions.remove(0);
		// Remove 'jsr' instruction in finally block
		if (finallyInstructions.get(0).opcode == ByteCodeConstants.JSR)
			finallyInstructions.remove(0);
		// Remove 'athrow' instruction in finally block
		if (finallyInstructions.get(0).opcode == ByteCodeConstants.ATHROW)
			finallyInstructions.remove(0);
		// Remove 'astore' instruction in finally block
		if (finallyInstructions.get(0).opcode == ByteCodeConstants.ASTORE)
			finallyInstructions.remove(0);		
	}
	
	private static int FormatFastTry_RemoveJsrInstructionAndCompactStoreReturn(
		List<Instruction> instructions, LocalVariables localVariables, 
		int finallyInstructitonsLineNumber)
	{
		int jumpOffset = UtilConstants.INVALID_OFFSET;
		int index = instructions.size();

		while (index-- > 1)
		{			
			if (instructions.get(index).opcode == ByteCodeConstants.JSR)
			{
				// Remove Jsr instruction
				Jsr jsr = (Jsr)instructions.remove(index);
				jumpOffset = jsr.GetJumpOffset();
			}
		}
		
		index = instructions.size();

		while (index-- > 1)
		{
			Instruction instruction = instructions.get(index);
			
			if (instruction.opcode == ByteCodeConstants.XRETURN)
			{
				// Compact AStore + Return
				ReturnInstruction ri = (ReturnInstruction)instruction;
				
				if (ri.lineNumber != Instruction.UNKNOWN_LINE_NUMBER)
				{				
					switch (ri.valueref.opcode)
					{
					case ByteCodeConstants.ALOAD:
						if (instructions.get(index-1).opcode == ByteCodeConstants.ASTORE)
							index = CompactStoreReturn(
									instructions, localVariables, ri, 
									index, finallyInstructitonsLineNumber);
						break;
					case ByteCodeConstants.LOAD:
						if (instructions.get(index-1).opcode == ByteCodeConstants.STORE)
							index = CompactStoreReturn(
									instructions, localVariables, ri, 
									index, finallyInstructitonsLineNumber);
						break;
					case ByteCodeConstants.ILOAD:
						if (instructions.get(index-1).opcode == ByteCodeConstants.ISTORE)
							index = CompactStoreReturn(
									instructions, localVariables, ri, 
									index, finallyInstructitonsLineNumber);
						break;
					}					
				}
			}
		}
		
		return jumpOffset;
	}
	
	private static void FormatFastTry_FormatNegativeJumpOffset(
		List<Instruction> instructions, 
		int lastTryInstructionOffset, int returnOffset)
	{
		int i = instructions.size();
		
		while (i-- > 0)
		{
			Instruction instruction = instructions.get(i);	
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.GOTO:
				Goto g = (Goto)instruction;
				int jumpOffset = g.GetJumpOffset();
				
				if (jumpOffset < lastTryInstructionOffset)
				{
					// Change jump offset
					g.branch = returnOffset - g.offset;
				}
				break;
			}
		}		
	}
		
	private static void FormatEclipse677Finally(
		FastCodeException fce, FastTry fastTry)
	{
		// Remove instructions in finally block
		List<Instruction> finallyInstructions = fastTry.finallyInstructions;
		
		Instruction instruction = finallyInstructions.get(0);
		
		switch (instruction.opcode)
		{
		case ByteCodeConstants.POP:
			{
				// Remove 'pop' instruction in finally block
				finallyInstructions.remove(0);
				
				List<Instruction> tryInstructions = fastTry.instructions;
				int lastIndex = tryInstructions.size()-1;
	
				// Remove last 'goto' instruction in try block
				if (tryInstructions.get(lastIndex).opcode == ByteCodeConstants.GOTO)
				{
					Goto g = (Goto)tryInstructions.remove(lastIndex);
					fce.tryToOffset = g.offset;
				}
			}
			break;
			
		case ByteCodeConstants.ASTORE:
			{
				int exceptionIndex = ((AStore)instruction).index;		
				int index = finallyInstructions.size();
				int athrowOffset = -1;
				int afterAthrowOffset = -1;
				
				// Search throw instruction
				while (index-- > 0)
				{
					instruction = finallyInstructions.get(index);	
					if (instruction.opcode == ByteCodeConstants.ATHROW)
					{
						AThrow athrow = (AThrow)instruction;									
						if ((athrow.value.opcode == ByteCodeConstants.ALOAD) && 
							( ((ALoad)athrow.value).index == exceptionIndex ))
						{
							// Remove last 'athrow' instruction in finally block
							athrowOffset = instruction.offset;
							finallyInstructions.remove(index);		
							break;
						}
					}
					afterAthrowOffset = instruction.offset;
					finallyInstructions.remove(index);		
				}	
		
				// Remove 'astore' instruction in finally block
				Instruction astore = finallyInstructions.remove(0);
				
				List<Instruction> tryInstructions = fastTry.instructions;
				int lastIndex = tryInstructions.size()-1;
				
				// Remove last 'goto' instruction in try block
				if (tryInstructions.get(lastIndex).opcode == ByteCodeConstants.GOTO)
				{
					Goto g = (Goto)tryInstructions.remove(lastIndex);
					fce.tryToOffset = g.offset;
				}
				
				// Remove finally instructions before 'return' instruction
				int finallyInstructitonsSize = finallyInstructions.size();
				FormatEclipse677Finally_RemoveFinallyInstructionsBeforeReturn(
					tryInstructions, finallyInstructitonsSize);
				
				// Format 'ifxxx' instruction jumping to finally block
				FormatEclipse677Finally_FormatIfInstruction(
					tryInstructions, athrowOffset, afterAthrowOffset, astore.offset);
			}
			break;
		}
	}

	private static void FormatEclipse677Finally_FormatIfInstruction(
		List<Instruction> instructions, int athrowOffset, 
		int afterAthrowOffset, int afterTryOffset)
	{
		int i = instructions.size();
		
		while (i-- > 0)
		{
			Instruction instruction = instructions.get(i);	
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.IF:
			case ByteCodeConstants.IFXNULL:
			case ByteCodeConstants.COMPLEXIF:
				IfInstruction ifi = (IfInstruction)instruction;
				int jumpOffset = ifi.GetJumpOffset();
				
				if ((athrowOffset < jumpOffset) && (jumpOffset <= afterAthrowOffset))
				{
					// Change jump offset
					ifi.branch = afterTryOffset - ifi.offset;
				}
				break;
			}
		}
	}
	
	private static void FormatEclipse677Finally_RemoveFinallyInstructionsBeforeReturn(
			List<Instruction> instructions, int finallyInstructitonsSize)
	{
		int i = instructions.size();

		while (i-- > 0)
		{
			switch (instructions.get(i).opcode)
			{
			case ByteCodeConstants.RETURN:
			case ByteCodeConstants.XRETURN:
				// Remove finally instructions
				for (int j=0; j<finallyInstructitonsSize && i>0; ++j)
					instructions.remove(--i);
				break;
			}
		}
	}
	
	private static void FormatEclipse677CatchFinally(
		FastCodeException fce, FastTry fastTry, int returnOffset)
	{
		// Remove instructions in finally block
		List<Instruction> finallyInstructions = fastTry.finallyInstructions;
		
		int exceptionIndex = ((AStore)finallyInstructions.get(0)).index;		
		int index = finallyInstructions.size();
		int athrowOffset = -1;
		int afterAthrowOffset = -1;
		
		// Search throw instruction
		while (index-- > 0)
		{
			Instruction instruction = finallyInstructions.get(index);	
			if (instruction.opcode == ByteCodeConstants.ATHROW)
			{
				AThrow athrow = (AThrow)instruction;									
				if ((athrow.value.opcode == ByteCodeConstants.ALOAD) && 
					( ((ALoad)athrow.value).index == exceptionIndex ))
				{
					// Remove last 'athrow' instruction in finally block
					athrowOffset = finallyInstructions.remove(index).offset;		
					break;
				}
			}
			afterAthrowOffset = instruction.offset;
			finallyInstructions.remove(index);		
		}	

		// Remove 'astore' instruction in finally block
		finallyInstructions.remove(0);

		List<Instruction> tryInstructions = fastTry.instructions;
		int lastIndex = tryInstructions.size()-1;
		Instruction lastTryInstruction = tryInstructions.get(lastIndex);
		int lastTryInstructionOffset = lastTryInstruction.offset;
		
		// Remove last 'goto' instruction in try block
		if (lastTryInstruction.opcode == ByteCodeConstants.GOTO)
		{
			Goto g = (Goto)tryInstructions.remove(lastIndex);
			fce.tryToOffset = g.offset;
		}
		
		// Remove finally instructions before 'return' instruction
		int finallyInstructitonsSize = finallyInstructions.size();
		FormatEclipse677Finally_RemoveFinallyInstructionsBeforeReturn(
			tryInstructions, finallyInstructitonsSize);
		
		// Format 'ifxxx' instruction jumping to finally block
		FormatEclipse677Finally_FormatIfInstruction(
			tryInstructions, athrowOffset, 
			afterAthrowOffset, lastTryInstructionOffset+1);

		// Format catch blockes
		int i = fastTry.catches.size();	
		while (i-- > 0)
		{
			FastCatch fastCatch = fastTry.catches.get(i);
			List<Instruction> catchInstructions = fastCatch.instructions;
			index = catchInstructions.size();
			
			Instruction lastInstruction = catchInstructions.get(index-1);
			int lastInstructionOffset = lastInstruction.offset;
			
			if (lastInstruction.opcode == ByteCodeConstants.GOTO)
			{
				// Remove last 'goto' instruction
				Goto g = (Goto)catchInstructions.remove(--index);
				fce.catches.get(i).toOffset = g.offset;
				int jumpOffset = g.GetJumpOffset();
				
				if (jumpOffset > fastTry.offset)
				{
					// Remove finally block instructions
					for (int j=finallyInstructitonsSize; j>0; --j)
						catchInstructions.remove(--index);
				}
			}

			// Remove finally instructions before 'return' instruction
			FormatEclipse677Finally_RemoveFinallyInstructionsBeforeReturn(
				catchInstructions, finallyInstructitonsSize);

			// Format 'ifxxx' instruction jumping to finally block
			FormatEclipse677Finally_FormatIfInstruction(
				catchInstructions, athrowOffset, 
				afterAthrowOffset, lastInstructionOffset+1);

			// Change negative jump goto to return offset 
			FormatFastTry_FormatNegativeJumpOffset(
				catchInstructions, lastTryInstructionOffset, returnOffset);
			
			// Remove first catch instruction in each catch block
			catchInstructions.remove(0);
		}
	}
	
	public static class FastCodeException 
		implements Comparable<FastCodeException>
	{
		public int tryFromOffset;
		public int tryToOffset;
		public List<FastCodeExceptionCatch> catches;
		public int finallyFromOffset;
		public int nbrFinally;
		public int maxOffset;
		public int afterOffset;
		public int type;
		public boolean synchronizedFlag;

		FastCodeException(
			int tryFromOffset, int tryToOffset,
			int maxOffset, boolean synchronizedFlag)
		{
			this.tryFromOffset = tryFromOffset;
			this.tryToOffset = tryToOffset;
			this.catches = new ArrayList<FastCodeExceptionCatch>();
			this.finallyFromOffset = UtilConstants.INVALID_OFFSET;
			this.nbrFinally = 0;
			this.maxOffset = maxOffset;
			this.afterOffset = UtilConstants.INVALID_OFFSET;
			this.type = FastConstants.TYPE_UNDEFINED;
			this.synchronizedFlag = synchronizedFlag;
		}
		
	    public int compareTo(FastCodeException other)
	    {
	    	// Sort by 1)tryFromOffset 2)maxOffset 3)tryToOffset
	    	if (this.tryFromOffset != other.tryFromOffset)
	    		return this.tryFromOffset - other.tryFromOffset;
	    	
	    	if (this.maxOffset != other.maxOffset)
	    		return other.maxOffset - this.maxOffset;

	    	return other.tryToOffset - this.tryToOffset;
	    }
	}
	
	public static class FastCodeExceptionCatch 
		implements Comparable<FastCodeExceptionCatch>
	{
		public int type;
		public int otherTypes[];
		public int fromOffset;
		public int toOffset;
		
		public FastCodeExceptionCatch(
			int type, int otherCatchTypes[], int fromOffset)
		{
			this.type = type;
			this.otherTypes = otherCatchTypes;			
			this.fromOffset = fromOffset;
			this.toOffset = UtilConstants.INVALID_OFFSET;
		}
		
	    public int compareTo(FastCodeExceptionCatch other)
	    {
	    	return this.fromOffset - other.fromOffset;
	    }
	}

	public static class FastAggregatedCodeException extends CodeException
	{
		public int     otherCatchTypes[];
		public int     nbrFinally;
		public boolean synchronizedFlag = false;
		
		public FastAggregatedCodeException(
			int index, int start_pc, int end_pc, int handler_pc, int catch_type) 
		{
			super(index, start_pc, end_pc, handler_pc, catch_type);
			this.otherCatchTypes = null;
			this.nbrFinally = (catch_type == 0) ? 1 : 0;
		}
	}
	
	public static int ComputeTryToIndex(	
		List<Instruction> instructions, FastCodeException fce, 
		int lastIndex, int maxOffset)
	{
		// Parcours
		int beforeMaxOffset = fce.tryFromOffset;
		int index = InstructionUtil.getIndexForOffset(
				instructions, fce.tryFromOffset);

		while (index <= lastIndex)
		{
			Instruction instruction = instructions.get(index);

			if (instruction.offset > maxOffset)
				return index-1;
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.ATHROW:
			case ByteCodeConstants.RETURN:
			case ByteCodeConstants.XRETURN:
				{
					if (instruction.offset >= beforeMaxOffset)
						return index;	// Inclus au bloc 'try'
				}
				break;
			case ByteCodeConstants.GOTO:	
				{
					int jumpOffset = ((BranchInstruction)instruction).GetJumpOffset();
					
					if (jumpOffset > instruction.offset)
					{
						// Saut positif
						if (jumpOffset < maxOffset)
						{			
							// Saut dans les limites
							if (beforeMaxOffset < jumpOffset)
								beforeMaxOffset = jumpOffset;
						}
						else
						{
							// Saut au del� des limites
							if (instruction.offset >= beforeMaxOffset)
								return index;	// Inclus au bloc 'try'
						}
					}
					else
					{
						// Saut negatif
						if (jumpOffset < fce.tryFromOffset)
						{
							// Saut au del� des limites
							if (instruction.offset >= beforeMaxOffset)
								return index;	// Inclus au bloc 'try'
						}
					}		
				}
				break;
			case ByteCodeConstants.IFCMP:
			case ByteCodeConstants.IF:
			case ByteCodeConstants.IFXNULL:
				{
					// L'aggregation des instructions 'if' n'a pas
					// encore ete executee. Recherche du plus petit
					// offset de saut parmi toutes les instructions
					// 'if' qui suivent.
					index = ComparisonInstructionAnalyzer.GetLastIndex(instructions, index);
					BranchInstruction lastBi = (BranchInstruction)instructions.get(index);
					int jumpOffset = lastBi.GetJumpOffset();
					
					if (jumpOffset > instruction.offset)
					{
						// Saut positif
						if (jumpOffset < maxOffset)
						{
							// Saut dans les limites
							if (beforeMaxOffset < jumpOffset)
								beforeMaxOffset = jumpOffset;
						}
						// else
						// {
						// 	// Saut au del� des limites, 'break' ?
						// }
					}
					// else
					// {
					// 	// Saut negatif, 'continue' ?						
					//}		
				}
				break;
			case FastConstants.LOOKUPSWITCH:
			case FastConstants.TABLESWITCH:
				{
					Switch s = (Switch)instruction;
					
					// Search max offset
					int maxSitchOffset = s.defaultOffset;
					int i = s.offsets.length;
					while (i-- > 0)
					{
						int offset = s.offsets[i];
						if (maxSitchOffset < offset)
							maxSitchOffset = offset;
					}
					maxSitchOffset += s.offset;
						
					if (maxSitchOffset > instruction.offset)
					{
						// Saut positif
						if (maxSitchOffset < maxOffset)
						{
							// Saut dans les limites
							if (beforeMaxOffset < maxSitchOffset)
								beforeMaxOffset = maxSitchOffset;
						}
						// else
						// {
						// 	// Saut au del� des limites, 'break' ?
						// }
					}
					// else
					// {
					// 	// Saut negatif, 'continue' ?						
					//}		
					break;	
				}
			}
			
			index++;
		}
		
		return index;
	}
}
