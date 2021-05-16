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
import java.util.HashMap;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.AttributeSignature;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.AStore;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BIPush;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.ExceptionLoad;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.IInc;
import jd.core.model.instruction.bytecode.instruction.ILoad;
import jd.core.model.instruction.bytecode.instruction.IStore;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;
import jd.core.model.instruction.bytecode.instruction.Jsr;
import jd.core.model.instruction.bytecode.instruction.Ldc;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
import jd.core.model.instruction.bytecode.instruction.LookupSwitch;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.model.instruction.bytecode.instruction.MonitorExit;
import jd.core.model.instruction.bytecode.instruction.Return;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.Switch;
import jd.core.model.instruction.bytecode.instruction.TableSwitch;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;
import jd.core.model.instruction.fast.instruction.FastFor;
import jd.core.model.instruction.fast.instruction.FastForEach;
import jd.core.model.instruction.fast.instruction.FastInstruction;
import jd.core.model.instruction.fast.instruction.FastLabel;
import jd.core.model.instruction.fast.instruction.FastList;
import jd.core.model.instruction.fast.instruction.FastSwitch;
import jd.core.model.instruction.fast.instruction.FastSwitch.Pair;
import jd.core.model.instruction.fast.instruction.FastSynchronized;
import jd.core.model.instruction.fast.instruction.FastTest2Lists;
import jd.core.model.instruction.fast.instruction.FastTestList;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;
import jd.core.model.reference.ReferenceMap;
import jd.core.process.analyzer.classfile.reconstructor.AssignmentOperatorReconstructor;
import jd.core.process.analyzer.classfile.visitor.SearchInstructionByOpcodeVisitor;
import jd.core.process.analyzer.instruction.bytecode.ComparisonInstructionAnalyzer;
import jd.core.process.analyzer.instruction.bytecode.reconstructor.AssertInstructionReconstructor;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;
import jd.core.process.analyzer.instruction.fast.FastCodeExceptionAnalyzer.FastCodeException;
import jd.core.process.analyzer.instruction.fast.FastCodeExceptionAnalyzer.FastCodeExceptionCatch;
import jd.core.process.analyzer.instruction.fast.reconstructor.DotClass118BReconstructor;
import jd.core.process.analyzer.instruction.fast.reconstructor.DotClassEclipseReconstructor;
import jd.core.process.analyzer.instruction.fast.reconstructor.EmptySynchronizedBlockReconstructor;
import jd.core.process.analyzer.instruction.fast.reconstructor.IfGotoToIfReconstructor;
import jd.core.process.analyzer.instruction.fast.reconstructor.InitArrayInstructionReconstructor;
import jd.core.process.analyzer.instruction.fast.reconstructor.RemoveDupConstantsAttributes;
import jd.core.process.analyzer.instruction.fast.reconstructor.TernaryOpInReturnReconstructor;
import jd.core.process.analyzer.instruction.fast.reconstructor.TernaryOpReconstructor;
import jd.core.process.analyzer.util.InstructionUtil;
import jd.core.util.IntSet;
import jd.core.util.SignatureUtil;
import jd.core.util.StringConstants;

/*
 *    Analyze
 *       |
 *       v
 *   AnalyzeList <-----------------+ <-----------------------------+ <--+
 *    |  |  |  |                   |                               |    |
 *    |  |  |  v                   |       1)Remove continue inst. |    |
 *    |  |  | AnalyzeBackIf -->AnalyzeLoop 2)Call AnalyzeList      |    | 
 *    |  |  v                      |       3)Remove break &        |    |
 *    |  | AnalyzeBackGoto --------+         labeled break         |    |
 *    |  v                                                         |    |
 *    | AnalyzeIfAndIfElse ----------------------------------------+    |
 *    v                                                                 |
 *   AnalyzeXXXXSwitch -------------------------------------------------+
 */
public class FastInstructionListBuilder {
	// Declaration constants
	private static final boolean DECLARED = true;
	private static final boolean NOT_DECLARED = false;

	/*
	 * debut de liste fin de liste | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ...
	 */
	public static void Build(ReferenceMap referenceMap, ClassFile classFile, Method method, List<Instruction> list)
			throws Exception {
		if ((list == null) || list.isEmpty())
			return;

		// Agregation des declarations CodeException
		List<FastCodeException> lfce = FastCodeExceptionAnalyzer.AggregateCodeExceptions(method, list);

		// Initialyze delaclation flags
		LocalVariables localVariables = method.getLocalVariables();
		InitDelcarationFlags(localVariables);

		// Initialisation de l'ensemle des offsets d'etiquette
		IntSet offsetLabelSet = new IntSet();

		// Initialisation de 'returnOffset' ...
		int returnOffset = -1;
		if (list.size() > 0) {
			Instruction instruction = list.get(list.size() - 1);
			if (instruction.opcode == ByteCodeConstants.RETURN)
				returnOffset = instruction.offset;
		}

		// Recursive call
		if (lfce != null)
			for (int i = lfce.size() - 1; i >= 0; --i) {
				FastCodeException fce = lfce.get(i);
				if (fce.synchronizedFlag)
					CreateSynchronizedBlock(referenceMap, classFile, list, localVariables, fce);
				else
					CreateFastTry(referenceMap, classFile, method, list, localVariables, fce, returnOffset);
			}

		ExecuteReconstructors(referenceMap, classFile, list, localVariables);

		AnalyzeList(classFile, method, list, localVariables, offsetLabelSet, -1, -1, -1, -1, -1, -1, returnOffset);

		// Add labels
		if (offsetLabelSet.size() > 0)
			AddLabels(list, offsetLabelSet);
	}

	private static void InitDelcarationFlags(LocalVariables localVariables) {
		int nbrOfLocalVariables = localVariables.size();
		int indexOfFirstLocalVariable = localVariables.getIndexOfFirstLocalVariable();

		for (int i = 0; i < indexOfFirstLocalVariable && i < nbrOfLocalVariables; ++i)
			localVariables.getLocalVariableAt(i).declarationFlag = DECLARED;

		for (int i = indexOfFirstLocalVariable; i < nbrOfLocalVariables; ++i) {
			LocalVariable lv = localVariables.getLocalVariableAt(i);
			lv.declarationFlag = lv.exceptionOrReturnAddress ? DECLARED : NOT_DECLARED;
		}
	}

	private static void CreateSynchronizedBlock(ReferenceMap referenceMap, ClassFile classFile, List<Instruction> list,
			LocalVariables localVariables, FastCodeException fce) {
		int index = InstructionUtil.getIndexForOffset(list, fce.tryFromOffset);
		Instruction instruction = list.get(index);
		int synchronizedBlockJumpOffset = -1;

		if (fce.type == FastConstants.TYPE_118_FINALLY) {
			// Retrait de la sous proc�dure allant de "monitorexit" � "ret"
			// Byte code:
			// 0: aload_1
			// 1: astore_3
			// 2: aload_3
			// 3: monitorenter <----- tryFromIndex
			// 4: aload_0
			// 5: invokevirtual 6 TryCatchFinallyClassForTest:inTry ()V
			// 8: iconst_2
			// 9: istore_2
			// 10: jsr +8 -> 18
			// 13: iload_2
			// 14: ireturn
			// 15: aload_3 <===== finallyFromOffset
			// 16: monitorexit
			// 17: athrow
			// 18: astore 4 <~~~~~ Entr�e de la sous procecure ('jsr')
			// 20: aload_3
			// 21: monitorexit
			// 22: ret 4 <-----

			// Save 'index'
			int tryFromIndex = index;

			// Search offset of sub procedure entry
			index = InstructionUtil.getIndexForOffset(list, fce.finallyFromOffset);
			int subProcedureOffset = list.get(index + 2).offset;

			// Remove 'jsr' instructions
			while (index-- > tryFromIndex) {
				instruction = list.get(index);
				if (instruction.opcode != ByteCodeConstants.JSR)
					continue;

				int jumpOffset = ((Jsr) instruction).GetJumpOffset();
				list.remove(index);

				if (jumpOffset == subProcedureOffset)
					break;
			}

			// Remove instructions of finally block
			int finallyFromOffset = fce.finallyFromOffset;
			index = InstructionUtil.getIndexForOffset(list, fce.afterOffset);
			if (index == -1) {
				index = list.size() - 1;
				while (list.get(index).offset >= finallyFromOffset)
					list.remove(index--);
			} else if (index > 0) {
				index--;
				while (list.get(index).offset >= finallyFromOffset)
					list.remove(index--);
			}

			// Extract try blockes
			List<Instruction> instructions = new ArrayList<Instruction>();
			if (index > 0) {
				int tryFromOffset = fce.tryFromOffset;

				while (list.get(index).offset >= tryFromOffset)
					instructions.add(list.remove(index--));
			}

			int fastSynchronizedOffset;

			if (instructions.size() > 0) {
				Instruction lastInstruction = instructions.get(0);
				fastSynchronizedOffset = lastInstruction.offset;
			} else {
				fastSynchronizedOffset = -1;
			}

			synchronizedBlockJumpOffset = SearchMinusJumpOffset(instructions, 0, instructions.size(),
					fce.tryFromOffset, fce.afterOffset);
			Collections.reverse(instructions);

			// Analyze lists of instructions
			ExecuteReconstructors(referenceMap, classFile, instructions, localVariables);

			// Remove 'monitorenter (localTestSynchronize1 = xxx)'
			MonitorEnter menter = (MonitorEnter) list.remove(index--);
			int fastSynchronizedLineNumber = menter.lineNumber;

			// Remove local variable for monitor
			if (menter.objectref.opcode != ByteCodeConstants.ALOAD)
				throw new UnexpectedInstructionException();
			int varMonitorIndex = ((IndexInstruction) menter.objectref).index;
			localVariables.removeLocalVariableWithIndexAndOffset(varMonitorIndex, menter.offset);

			// Search monitor
			AStore astore = (AStore) list.get(index);
			Instruction monitor = astore.valueref;

			//
			int branch = 1;
			if ((fastSynchronizedOffset != -1) && (synchronizedBlockJumpOffset != -1))
				branch = synchronizedBlockJumpOffset - fastSynchronizedOffset;

			FastSynchronized fastSynchronized = new FastSynchronized(FastConstants.SYNCHRONIZED,
					fastSynchronizedOffset, fastSynchronizedLineNumber, branch, instructions);
			fastSynchronized.monitor = monitor;

			// Replace 'astore localTestSynchronize1'
			list.set(index, fastSynchronized);
		} else if (fce.type == FastConstants.TYPE_118_SYNCHRONIZED_DOUBLE) {
			// Byte code:
			// 0: getstatic 10 java/lang/System:out Ljava/io/PrintStream;
			// 3: ldc 1
			// 5: invokevirtual 11 java/io/PrintStream:println
			// (Ljava/lang/String;)V
			// 8: aload_0
			// 9: astore_1
			// 10: aload_1
			// 11: monitorenter
			// 12: aload_0
			// 13: invokespecial 8 TestSynchronize:getMonitor
			// ()Ljava/lang/Object;
			// 16: astore 4
			// 18: aload 4
			// 20: monitorenter
			// 21: getstatic 10 java/lang/System:out Ljava/io/PrintStream;
			// 24: ldc 2
			// 26: invokevirtual 11 java/io/PrintStream:println
			// (Ljava/lang/String;)V
			// 29: iconst_1
			// 30: istore_3
			// 31: jsr +12 -> 43
			// 34: jsr +19 -> 53
			// 37: iload_3
			// 38: ireturn
			// 39: aload 4
			// 41: monitorexit
			// 42: athrow
			// 43: astore 5
			// 45: aload 4
			// 47: monitorexit
			// 48: ret 5
			// 50: aload_1
			// 51: monitorexit
			// 52: athrow
			// 53: astore_2
			// 54: aload_1
			// 55: monitorexit
			// 56: ret 2

			// Extract try blockes
			List<Instruction> instructions = new ArrayList<Instruction>();
			instruction = list.remove(index);
			int fastSynchronizedOffset = instruction.offset;
			instructions.add(instruction);

			synchronizedBlockJumpOffset = SearchMinusJumpOffset(instructions, 0, instructions.size(),
					fce.tryFromOffset, fce.afterOffset);

			// Remove 'monitorenter'
			MonitorEnter menter = (MonitorEnter) list.remove(index - 1);

			// Search monitor
			AStore astore = (AStore) list.get(index - 2);
			Instruction monitor = astore.valueref;

			// Remove local variable for monitor
			int varMonitorIndex = astore.index;
			localVariables.removeLocalVariableWithIndexAndOffset(varMonitorIndex, menter.offset);

			//
			int branch = 1;
			if (synchronizedBlockJumpOffset != -1)
				branch = synchronizedBlockJumpOffset - fastSynchronizedOffset;

			FastSynchronized fastSynchronized = new FastSynchronized(FastConstants.SYNCHRONIZED,
					fastSynchronizedOffset, menter.lineNumber, branch, instructions);
			fastSynchronized.monitor = monitor;

			// Replace 'astore localTestSynchronize1'
			list.set(index - 2, fastSynchronized);
		} else if (instruction.opcode == ByteCodeConstants.MONITOREXIT) {
			if (list.get(--index).opcode == ByteCodeConstants.MONITORENTER) {
				// Cas particulier des blocks synchronises vides avec le
				// jdk 1.1.8.
				// Byte code++:
				// 3: monitorenter;
				// 10: monitorexit;
				// 11: return contentEquals(paramStringBuffer);
				// 12: localObject = finally;
				// 14: monitorexit;
				// 16: throw localObject;
				// ou
				// 5: System.out.println("start");
				// 9: localTestSynchronize = this;
				// 11: monitorenter;
				// 14: monitorexit;
				// 15: goto 21;
				// 19: monitorexit;
				// 20: throw finally;
				// 26: System.out.println("end");
				// Remove previous 'monitorenter' instruction
				Instruction monitor;
				MonitorEnter me = (MonitorEnter) list.remove(index);
				if (me.objectref.opcode == ByteCodeConstants.ASSIGNMENT) {
					AssignmentInstruction ai = (AssignmentInstruction) me.objectref;
					AStore astore = (AStore) ai.value1;
					monitor = ai.value2;
					// Remove local variable for monitor
					localVariables.removeLocalVariableWithIndexAndOffset(astore.index, astore.offset);
					// Remove 'monitorexit' instruction
					list.remove(index);
				} else {
					// Remove 'monitorexit' instruction
					list.remove(index);
					// Remove 'astore'
					AStore astore = (AStore) list.remove(--index);
					monitor = astore.valueref;
					// Remove local variable for monitor
					localVariables.removeLocalVariableWithIndexAndOffset(astore.index, astore.offset);
				}

				List<Instruction> instructions = new ArrayList<Instruction>();
				Instruction gi = list.remove(index);

				if ((gi.opcode != ByteCodeConstants.GOTO) || (((Goto) gi).GetJumpOffset() != fce.afterOffset))
					instructions.add(gi);

				// Remove 'localObject = finally' instruction
				if (list.get(index).opcode == ByteCodeConstants.ASTORE)
					list.remove(index);
				// Remove 'monitorexit' instruction
				Instruction monitorexit = list.remove(index);

				// Analyze lists of instructions
				ExecuteReconstructors(referenceMap, classFile, instructions, localVariables);

				//
				FastSynchronized fastSynchronized = new FastSynchronized(FastConstants.SYNCHRONIZED,
						monitorexit.offset, instruction.lineNumber, 1, instructions);
				fastSynchronized.monitor = monitor;

				// Replace 'throw localObject' instruction
				list.set(index, fastSynchronized);
			} else {
				// Cas particulier Jikes 1.2.2
				// Remove previous goto instruction
				list.remove(index);
				// Remove 'monitorexit'
				list.remove(index);
				// Remove 'throw finally'
				list.remove(index);
				// Remove localTestSynchronize1 = xxx

				MonitorEnter menter;
				Instruction monitor;
				int varMonitorIndex;

				instruction = list.remove(index);

				switch (instruction.opcode) {
				case ByteCodeConstants.ASTORE:
					menter = (MonitorEnter) list.remove(index);
					AStore astore = (AStore) instruction;
					varMonitorIndex = astore.index;
					monitor = astore.valueref;
					break;
				case ByteCodeConstants.MONITORENTER:
					menter = (MonitorEnter) instruction;
					AssignmentInstruction ai = (AssignmentInstruction) menter.objectref;
					astore = (AStore) ai.value1;
					varMonitorIndex = astore.index;
					monitor = ai.value2;
					break;
				default:
					throw new UnexpectedInstructionException();
				}

				// Remove local variable for monitor
				localVariables.removeLocalVariableWithIndexAndOffset(varMonitorIndex, menter.offset);

				List<Instruction> instructions = new ArrayList<Instruction>();
				while (true) {
					instruction = list.get(index);

					if (instruction.opcode == ByteCodeConstants.MONITOREXIT) {
						MonitorExit mexit = (MonitorExit) instruction;
						if (mexit.objectref.opcode == ByteCodeConstants.ALOAD) {
							LoadInstruction li = (LoadInstruction) mexit.objectref;
							if (li.index == varMonitorIndex)
								break;
						}
					}

					instructions.add(list.remove(index));
				}

				if ((index + 1 < list.size()) && (list.get(index + 1).opcode == ByteCodeConstants.XRETURN)) {
					// Si l'instruction retourn�e poss�de un offset inferieur a
					// celui de l'instruction 'monitorexit', l'instruction
					// 'return' est ajoute au bloc synchronise.
					Instruction monitorexit = list.get(index);
					Instruction value = ((ReturnInstruction) list.get(index + 1)).valueref;

					if (monitorexit.offset > value.offset)
						instructions.add(list.remove(index + 1));
				}

				// Analyze lists of instructions
				ExecuteReconstructors(referenceMap, classFile, instructions, localVariables);

				synchronizedBlockJumpOffset = SearchMinusJumpOffset(instructions, 0, instructions.size(),
						fce.tryFromOffset, fce.afterOffset);

				//
				int branch = 1;
				if (synchronizedBlockJumpOffset != -1)
					branch = synchronizedBlockJumpOffset - instruction.offset;

				FastSynchronized fastSynchronized = new FastSynchronized(FastConstants.SYNCHRONIZED,
						instruction.offset, menter.lineNumber, branch, instructions);
				fastSynchronized.monitor = monitor;

				// Replace 'monitorexit localTestSynchronize1'
				list.set(index, fastSynchronized);
			}
		} else {
			// Cas g�n�ral
			if (fce.afterOffset > list.get(list.size() - 1).offset)
				index = list.size();
			else
				index = InstructionUtil.getIndexForOffset(list, fce.afterOffset);
			int lastOffset = list.get(--index).offset;

			// Remove instructions of finally block
			Instruction i = null;
			int finallyFromOffset = fce.finallyFromOffset;
			while (list.get(index).offset >= finallyFromOffset)
				i = list.remove(index--);

			// Store last 'AStore' to delete last "throw' instruction later
			int exceptionLoadIndex = -1;
			if ((i != null) && (i.opcode == ByteCodeConstants.ASTORE)) {
				AStore astore = (AStore) i;
				if (astore.valueref.opcode == ByteCodeConstants.EXCEPTIONLOAD)
					exceptionLoadIndex = astore.index;
			}

			// Extract try blockes
			List<Instruction> instructions = new ArrayList<Instruction>();
			i = null;
			if (index > 0) {
				int tryFromOffset = fce.tryFromOffset;
				i = list.get(index);

				if (i.offset >= tryFromOffset) {
					instructions.add(i);

					while (index-- > 0) {
						i = list.get(index);
						if (i.offset < tryFromOffset)
							break;
						list.remove(index + 1);
						instructions.add(i);
					}
					list.set(index + 1, null);
				}
			}

			Instruction lastInstruction = instructions.get(0);

			synchronizedBlockJumpOffset = SearchMinusJumpOffset(instructions, 0, instructions.size(),
					fce.tryFromOffset, fce.afterOffset);
			Collections.reverse(instructions);

			int lineNumber;

			if (i == null) {
				lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
			} else {
				lineNumber = i.lineNumber;
			}

			// Reduce lists of instructions
			int lenght = instructions.size();

			// Get local variable index for monitor
			int monitorLocalVariableIndex = GetMonitorLocalVariableIndex(list, index);

			if (lenght > 0) {
				// Remove 'Goto' or jump 'return' & set 'afterListOffset'
				lastInstruction = instructions.get(lenght - 1);
				switch (lastInstruction.opcode) {
				case ByteCodeConstants.GOTO:
					instructions.remove(--lenght);
					if (lenght > 0)
						lastInstruction = instructions.get(lenght - 1);
					break;
				case ByteCodeConstants.XRETURN:
					--lenght;
					if (lenght > 0)
						lastInstruction = instructions.get(lenght - 1);
					break;
				}

				// Remove all MonitorExit instructions
				RemoveAllMonitorExitInstructions(instructions, lenght, monitorLocalVariableIndex);

				// Remove last "throw finally" instructions
				int lastIndex = list.size() - 1;
				i = list.get(lastIndex);
				if ((i != null) && (i.opcode == ByteCodeConstants.ATHROW)) {
					AThrow at = (AThrow) list.get(lastIndex);
					switch (at.value.opcode) {
					case ByteCodeConstants.EXCEPTIONLOAD: {
						ExceptionLoad el = (ExceptionLoad) at.value;
						if (el.exceptionNameIndex == 0)
							list.remove(lastIndex);
					}
						break;
					case ByteCodeConstants.ALOAD: {
						ALoad aload = (ALoad) at.value;
						if (aload.index == exceptionLoadIndex)
							list.remove(lastIndex);
					}
						break;
					}
				}
			}

			// Remove local variable for monitor
			if (monitorLocalVariableIndex != -1) {
				MonitorEnter menter = (MonitorEnter) list.get(index);
				localVariables.removeLocalVariableWithIndexAndOffset(monitorLocalVariableIndex, menter.offset);
			}

			int branch = 1;
			if (synchronizedBlockJumpOffset != -1)
				branch = synchronizedBlockJumpOffset - lastOffset;

			FastSynchronized fastSynchronized = new FastSynchronized(FastConstants.SYNCHRONIZED, lastOffset,
					lineNumber, branch, instructions);

			// Analyze lists of instructions
			ExecuteReconstructors(referenceMap, classFile, instructions, localVariables);

			// Store new FastTry instruction
			list.set(index + 1, fastSynchronized);

			// Extract monitor
			fastSynchronized.monitor = FormatAndExtractMonitor(list, index);
		}
	}

	private static Instruction FormatAndExtractMonitor(List<Instruction> list, int index) {
		// Remove "monitorenter localTestSynchronize1"
		MonitorEnter menter = (MonitorEnter) list.remove(index--);

		switch (menter.objectref.opcode) {
		case ByteCodeConstants.ASSIGNMENT:
			return ((AssignmentInstruction) menter.objectref).value2;
		case ByteCodeConstants.DUPLOAD:
			// Remove Astore(DupLoad)
			list.remove(index--);
			// Remove DupStore(...)
			DupStore dupstore = (DupStore) list.remove(index);
			return dupstore.objectref;
		case ByteCodeConstants.ALOAD:
			AStore astore = (AStore) list.remove(index);
			return astore.valueref;
		default:
			return null;
		}
	}

	private static void RemoveAllMonitorExitInstructions(List<Instruction> instructions, int lenght,
			int monitorLocalVariableIndex) {
		int index = lenght;

		while (index-- > 0) {
			Instruction instruction = instructions.get(index);

			if (instruction.opcode == ByteCodeConstants.MONITOREXIT) {
				MonitorExit mexit = (MonitorExit) instruction;

				if (mexit.objectref.opcode == ByteCodeConstants.ALOAD) {
					int aloadIndex = ((ALoad) mexit.objectref).index;
					if (aloadIndex == monitorLocalVariableIndex)
						instructions.remove(index);
				}
			} else if (instruction.opcode == FastConstants.TRY) {
				FastTry ft = (FastTry) instruction;

				RemoveAllMonitorExitInstructions(ft.instructions, ft.instructions.size(), monitorLocalVariableIndex);

				int i = ft.catches.size();

				while (i-- > 0) {
					FastCatch fc = ft.catches.get(i);
					RemoveAllMonitorExitInstructions(fc.instructions, fc.instructions.size(), monitorLocalVariableIndex);
				}

				if (ft.finallyInstructions != null)
					RemoveAllMonitorExitInstructions(ft.finallyInstructions, ft.finallyInstructions.size(),
							monitorLocalVariableIndex);
			} else if (instruction.opcode == FastConstants.SYNCHRONIZED) {
				FastSynchronized fsy = (FastSynchronized) instruction;

				RemoveAllMonitorExitInstructions(fsy.instructions, fsy.instructions.size(), monitorLocalVariableIndex);
			}
		}
	}

	private static int GetMonitorLocalVariableIndex(List<Instruction> list, int index) {
		MonitorEnter menter = (MonitorEnter) list.get(index);

		switch (menter.objectref.opcode) {
		case ByteCodeConstants.DUPLOAD:
			return ((AStore) list.get(index - 1)).index;
		case ByteCodeConstants.ALOAD:
			return ((ALoad) menter.objectref).index;
		case ByteCodeConstants.ASSIGNMENT:
			Instruction i = ((AssignmentInstruction) menter.objectref).value1;
			if (i.opcode == ByteCodeConstants.ALOAD)
				return ((ALoad) i).index;
		default:
			return -1;
		}
	}

	private static void CreateFastTry(ReferenceMap referenceMap, ClassFile classFile, Method method,
			List<Instruction> list, LocalVariables localVariables, FastCodeException fce, int returnOffset)
			throws Exception {
		int afterListOffset = fce.afterOffset;
		int tryJumpOffset = -1;
		int lastIndex = list.size() - 1;
		int index;

		if ((afterListOffset == -1) || (afterListOffset > list.get(lastIndex).offset)) {
			index = lastIndex;
		} else {
			index = InstructionUtil.getIndexForOffset(list, afterListOffset);
			assert (index != -1);
			--index;
		}

		int lastOffset = list.get(index).offset;
		// /30-12-2012///int lastOffset = fce.tryToOffset;

		// Extract finally block
		List<Instruction> finallyInstructions = null;
		if (fce.finallyFromOffset > 0) {
			int finallyFromOffset = fce.finallyFromOffset;
			finallyInstructions = new ArrayList<Instruction>();

			while (list.get(index).offset >= finallyFromOffset)
				finallyInstructions.add(list.remove(index--));

			if (finallyInstructions.size() == 0)
				throw new RuntimeException("Unexpected structure for finally block");

			Collections.reverse(finallyInstructions);
			//////////////////////////////////afterListOffset = finallyInstructions.get(0).offset;

			// Calcul de l'offset le plus haut pour le block 'try'
			int firstOffset = finallyInstructions.get(0).offset;
			int minimalJumpOffset = SearchMinusJumpOffset(
					finallyInstructions, 0, finallyInstructions.size(), 
					firstOffset, afterListOffset);
			
			afterListOffset = firstOffset;	
			
			if ((minimalJumpOffset != -1) && (afterListOffset > minimalJumpOffset))
				afterListOffset = minimalJumpOffset;				
		}

		// Extract catch blockes
		List<FastCatch> catches = null;
		if (fce.catches != null) 
		{
			int i = fce.catches.size();
			catches = new ArrayList<FastCatch>(i);

			while (i-- > 0) {
				FastCodeExceptionCatch fcec = fce.catches.get(i);
				fcec.toOffset = afterListOffset;
				int fromOffset = fcec.fromOffset;
				List<Instruction> instructions = new ArrayList<Instruction>();

				while (list.get(index).offset >= fromOffset) {
					instructions.add(list.remove(index));
					if (index == 0)
						break;
					index--;
				}

				int instructionsLength = instructions.size();

				if (instructionsLength > 0) {
					Instruction lastInstruction = instructions.get(0);

					int tryJumpOffsetTmp = SearchMinusJumpOffset(instructions, 0, instructionsLength,
							fce.tryFromOffset, fce.afterOffset);
					if (tryJumpOffsetTmp != -1) {
						if (tryJumpOffset == -1)
							tryJumpOffset = tryJumpOffsetTmp;
						else if (tryJumpOffset > tryJumpOffsetTmp)
							tryJumpOffset = tryJumpOffsetTmp;
					}

					Collections.reverse(instructions);

					// Search exception type and local variables index
					ExceptionLoad el = SearchExceptionLoadInstruction(instructions);
					if (el != null) 
					{
						int offset = lastInstruction.offset;

						catches.add(0, new FastCatch(
							offset, el.offset, fcec.type, fcec.otherTypes, 
							el.index, instructions));

						////////////////////afterListOffset = instructions.get(0).offset;
						
						// Calcul de l'offset le plus haut pour le block 'try'
						int firstOffset = instructions.get(0).offset;
						int minimalJumpOffset = SearchMinusJumpOffset(
								instructions, 0, instructions.size(), 
								firstOffset, offset);
						
						if (afterListOffset > firstOffset)
							afterListOffset = firstOffset;

						if ((minimalJumpOffset != -1) && (afterListOffset > minimalJumpOffset))
							afterListOffset = minimalJumpOffset;						
					} else {
						throw new UnexpectedInstructionException();
					}
				} else {
					throw new RuntimeException("Empty catch block");
				}
			}
		}

		// Extract try blockes
		List<Instruction> tryInstructions = new ArrayList<Instruction>();

		if (fce.tryToOffset < afterListOffset) {
			index = FastCodeExceptionAnalyzer.ComputeTryToIndex(list, fce, index, afterListOffset);
		}

		int tryFromOffset = fce.tryFromOffset;
		Instruction i = list.get(index);

		if (i.offset >= tryFromOffset) {
			tryInstructions.add(i);

			while (index-- > 0) {
				i = list.get(index);
				if (i.offset < tryFromOffset)
					break;
				list.remove(index + 1);
				tryInstructions.add(i);
			}
			list.set(index + 1, null);
		}

		int tryJumpOffsetTmp = SearchMinusJumpOffset(tryInstructions, 0, tryInstructions.size(), fce.tryFromOffset,
				fce.tryToOffset);
		if (tryJumpOffsetTmp != -1) {
			if (tryJumpOffset == -1)
				tryJumpOffset = tryJumpOffsetTmp;
			else if (tryJumpOffset > tryJumpOffsetTmp)
				tryJumpOffset = tryJumpOffsetTmp;
		}

		Collections.reverse(tryInstructions);

		int lineNumber = tryInstructions.get(0).lineNumber;

		if (tryJumpOffset == -1)
			tryJumpOffset = lastOffset + 1;

		FastTry fastTry = new FastTry(FastConstants.TRY, lastOffset, lineNumber, tryJumpOffset - lastOffset,
				tryInstructions, catches, finallyInstructions);

		// Reduce lists of instructions
		FastCodeExceptionAnalyzer.FormatFastTry(localVariables, fce, fastTry, returnOffset);

		// Analyze lists of instructions
		ExecuteReconstructors(referenceMap, classFile, tryInstructions, localVariables);

		if (catches != null) 
		{
			int length = catches.size();

			for (int j = 0; j < length; ++j) {
				FastCatch fc = catches.get(j);
				List<Instruction> catchInstructions = fc.instructions;
				ExecuteReconstructors(referenceMap, classFile, catchInstructions, localVariables);
			}
		}

		if (finallyInstructions != null)
			ExecuteReconstructors(referenceMap, classFile, finallyInstructions, localVariables);

		// Store new FastTry instruction
		list.set(index + 1, fastTry);
	}

	private static ExceptionLoad SearchExceptionLoadInstruction(List<Instruction> instructions) throws Exception {
		int length = instructions.size();

		for (int i = 0; i < length; i++) {
			Instruction instruction = SearchInstructionByOpcodeVisitor.visit(instructions.get(i),
					ByteCodeConstants.EXCEPTIONLOAD);

			if (instruction != null)
				return (ExceptionLoad) instruction;
		}

		return null;
	}

	/*
	 * debut de liste fin de liste | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
	 * beforeLoopEntryOffset afterLoopOffset
	 */
	private static void ExecuteReconstructors(ReferenceMap referenceMap, ClassFile classFile, List<Instruction> list,
			LocalVariables localVariables) {
		// Reconstruction des blocs synchronis�s vide
		EmptySynchronizedBlockReconstructor.Reconstruct(localVariables, list);
		// Recontruction du mot cl� '.class' pour le JDK 1.1.8 - B
		DotClass118BReconstructor.Reconstruct(referenceMap, classFile, list);
		// Recontruction du mot cl� '.class' pour le compilateur d'Eclipse
		DotClassEclipseReconstructor.Reconstruct(referenceMap, classFile, list);
		// Transformation de l'ensemble 'if-break' en simple 'if'
		// A executer avant 'ComparisonInstructionAnalyzer'
		IfGotoToIfReconstructor.Reconstruct(list);
		// Aggregation des instructions 'if'
		// A executer apres 'AssignmentInstructionReconstructor',
		// 'IfGotoToIfReconstructor'
		// A executer avant 'TernaryOpReconstructor'
		ComparisonInstructionAnalyzer.Aggregate(list);
		// Recontruction des instructions 'assert'. Cette operation doit etre
		// executee apres 'ComparisonInstructionAnalyzer'.
		AssertInstructionReconstructor.Reconstruct(classFile, list);
		// Create ternary operator before analisys of local variables.
		// A executer apr�s 'ComparisonInstructionAnalyzer'
		TernaryOpReconstructor.Reconstruct(list);
		// Recontruction des initialisations de tableaux
		// Cette operation doit etre executee apres
		// 'AssignmentInstructionReconstructor'.
		InitArrayInstructionReconstructor.Reconstruct(list);
		// Recontruction des operations binaires d'assignement
		AssignmentOperatorReconstructor.Reconstruct(list);
		// Retrait des instructions DupLoads & DupStore associ�s �
		// une constante ou un attribut.
		RemoveDupConstantsAttributes.Reconstruct(list);
	}

	// Remove 'goto' jumping on next instruction
	private static void RemoveNoJumpGotoInstruction(List<Instruction> list, int afterListOffset) {
		int index = list.size();

		if (index == 0)
			return;

		Instruction instruction = list.get(--index);
		int lastInstructionOffset = instruction.offset;

		if (instruction.opcode == ByteCodeConstants.GOTO) {
			int branch = ((Goto) instruction).branch;
			if ((branch >= 0) && (instruction.offset + branch <= afterListOffset))
				list.remove(index);
		}

		while (index-- > 0) {
			instruction = list.get(index);

			if (instruction.opcode == ByteCodeConstants.GOTO) {
				int branch = ((Goto) instruction).branch;
				if ((branch >= 0) && (instruction.offset + branch <= lastInstructionOffset))
					list.remove(index);
			}

			lastInstructionOffset = instruction.offset;
		}
	}

	/*
	 * Effacement de instruction 'return' inutile sauf celle en fin de methode
	 * necessaire a 'InitInstanceFieldsReconstructor".
	 */
	private static void RemoveSyntheticReturn(List<Instruction> list, int afterListOffset, int returnOffset) {
		if (afterListOffset == returnOffset) {
			int index = list.size();

			if (index == 1) {
				RemoveSyntheticReturn(list, --index);
			} else if (index-- > 1) {
				if (list.get(index).lineNumber < list.get(index - 1).lineNumber) {
					RemoveSyntheticReturn(list, index);
				}
			}
		}
	}

	private static void RemoveSyntheticReturn(List<Instruction> list, int index) {
		switch (list.get(index).opcode) {
		case FastConstants.RETURN:
			list.remove(index);
			break;
		case FastConstants.LABEL:
			FastLabel fl = (FastLabel) list.get(index);
			if (fl.instruction.opcode == FastConstants.RETURN)
				fl.instruction = null;
		}
	}

	private static void AddCastInstructionOnReturn(
		ClassFile classFile, Method method, List<Instruction> list)
	{
	    ConstantPool constants = classFile.getConstantPool();
	    LocalVariables localVariables = method.getLocalVariables();

	    AttributeSignature as = method.getAttributeSignature();
	    int signatureIndex = (as == null) ? 
	    		method.descriptor_index : as.signature_index;
	    String signature = constants.getConstantUtf8(signatureIndex);
	    String methodReturnedSignature = 
	    		SignatureUtil.GetMethodReturnedSignature(signature);
	    
		int index = list.size();
		
		while (index-- > 0) 
		{
			Instruction instruction = list.get(index);

			if (instruction.opcode == ByteCodeConstants.XRETURN) 
			{
				ReturnInstruction ri = (ReturnInstruction)instruction;
				String returnedSignature = 
					ri.valueref.getReturnedSignature(constants, localVariables);
				
				if ((returnedSignature != null) &&
					returnedSignature.equals(StringConstants.INTERNAL_OBJECT_SIGNATURE) &&
					! methodReturnedSignature.equals(StringConstants.INTERNAL_OBJECT_SIGNATURE))
			    {
				    signatureIndex = constants.addConstantUtf8(methodReturnedSignature);	 
				    
					if (ri.valueref.opcode == ByteCodeConstants.CHECKCAST)
					{
						((CheckCast)ri.valueref).index = signatureIndex;
					}
					else
					{
					    ri.valueref = new CheckCast(
							ByteCodeConstants.CHECKCAST, ri.valueref.offset, 
							ri.valueref.lineNumber, signatureIndex, ri.valueref);
					}
			    }
				
				/* if (! methodReturnedSignature.equals(returnedSignature))
				{
					if (SignatureUtil.IsPrimitiveSignature(methodReturnedSignature))
				    {
				    	ri.valueref = new ConvertInstruction(
							ByteCodeConstants.CONVERT, ri.valueref.offset, 
							ri.valueref.lineNumber, ri.valueref, 
							methodReturnedSignature);
				    }
				    else if (! StringConstants.INTERNAL_OBJECT_SIGNATURE.equals(methodReturnedSignature))
				    {
				    	signature = SignatureUtil.GetInnerName(methodReturnedSignature);
					    signatureIndex = constants.addConstantUtf8(signature);
					    int classIndex = constants.addConstantClass(signatureIndex);			    
					    ri.valueref = new CheckCast(
							ByteCodeConstants.CHECKCAST, ri.valueref.offset, 
							ri.valueref.lineNumber, classIndex, ri.valueref);
				    }
				}*/			
			}
		}
	}
	
	/*
	 * debut de liste fin de liste | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
	 * beforeLoopEntryOffset afterLoopOffset
	 * 
	 * 
	 * beforeLoopEntryOffset & loopEntryOffset: utile pour la generation
	 * d'instructions 'continue' beforeListOffset: utile pour la generation de
	 * declarations de variable endLoopOffset & afterLoopOffset: utile pour la
	 * generation d'instructions 'break' afterListOffset: utile pour la
	 * generation d'instructions 'if-else' = lastBodyWhileLoop.offset
	 * 
	 * WHILE instruction avant boucle | goto | beforeSubListOffset instructions
	 * | instruction | beforeLoopEntryOffset if a saut negatif |
	 * loopEntryOffset, endLoopOffset, afterListOffset instruction apres boucle
	 * | afterLoopOffset
	 * 
	 * DO_WHILE instruction avant boucle | beforeListOffset instructions |
	 * instruction | beforeLoopEntryOffset if a saut negatif | loopEntryOffset,
	 * endLoopOffset, afterListOffset instruction apres boucle | afterLoopOffset
	 * 
	 * FOR instruction avant boucle | goto | beforeListOffset instructions |
	 * instruction | beforeLoopEntryOffset iinc | loopEntryOffset,
	 * afterListOffset if a saut negatif | endLoopOffset instruction apres
	 * boucle | afterLoopOffset
	 * 
	 * 
	 * INFINITE_LOOP instruction avant boucle | beforeListOffset instructions |
	 * instruction | beforeLoopEntryOffset goto a saut negatif |
	 * loopEntryOffset, endLoopOffset, afterListOffset instruction apres boucle
	 * | afterLoopOffset
	 */
	private static void AnalyzeList(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
			int afterBodyLoopOffset, int beforeListOffset, int afterListOffset, int breakOffset, int returnOffset) 
	{
		// Create loops
		CreateLoops(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
				beforeListOffset, afterListOffset, returnOffset);

		// Create switch
		CreateSwitch(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
				afterBodyLoopOffset, afterListOffset, returnOffset);

		AnalyzeTryAndSynchronized(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset,
				loopEntryOffset, afterBodyLoopOffset, beforeListOffset, afterListOffset, breakOffset, returnOffset);

		// Recontruction de la sequence 'return (b1 == 1);' apres la
		// determination des types de variable
		// A executer apr�s 'ComparisonInstructionAnalyzer'
		TernaryOpInReturnReconstructor.Reconstruct(list);

		// Create labeled 'break'
		// Cet appel permettait de reduire le nombre d'imbrication des 'if' en
		// Augmentant le nombre de 'break' et 'continue'.
		// CreateContinue(
		// list, beforeLoopEntryOffset, loopEntryOffset, returnOffset);

		// Create if and if-else
		CreateIfElse(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
				afterBodyLoopOffset, afterListOffset, breakOffset, returnOffset);

		// Remove 'goto' instruction jumping to next instruction
		RemoveNopGoto(list);

		// // Compacte les instructions 'store' suivies d'instruction 'return'
		// // A executer avant l'ajout des declarations.
		// StoreReturnAnalyzer.Cleanup(list, localVariables);

		// Add local variable declarations
		AddDeclarations(list, localVariables, beforeListOffset);

		// Remove 'goto' jumping on next instruction
		// A VALIDER A LONG TERME.
		// MODIFICATION AJOUTER SUITE A UNE MAUVAISE RECONSTRUCTION
		// DES BLOCS tyr-catch GENERES PAR LE JDK 1.1.8.
		// SI CELA PERTURBE LA RECONSTRUCTION DES INSTRUCTIONS if,
		// 1) MODIFIER LES SAUTS DES INSTRUCTIONS goto DANS FormatCatch
		// 2) DEPLACER CETTE METHODE APRES L'APPEL A
		// FastInstructionListBuilder.Build(...)
		RemoveNoJumpGotoInstruction(list, afterListOffset);

		// Create labeled 'break'
		CreateBreakAndContinue(method, list, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
				afterBodyLoopOffset, afterListOffset, breakOffset, returnOffset);

		// Retrait des instructions DupStore associ�es � une seule
		// instruction DupLoad
		SingleDupLoadAnalyzer.Cleanup(list);

		// Remove synthetic 'return'
		RemoveSyntheticReturn(list, afterListOffset, returnOffset);
		
		// Add cast instruction on return
		AddCastInstructionOnReturn(classFile, method, list);
	}

	private static void AnalyzeTryAndSynchronized(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
			int afterBodyLoopOffset, int beforeListOffset, int afterListOffset, int breakOffset, int returnOffset) {
		int index = list.size();

		while (index-- > 0) {
			Instruction instruction = list.get(index);

			switch (instruction.opcode) {
			case FastConstants.TRY: {
				FastTry ft = (FastTry) instruction;
				int tmpBeforeListOffset = (index > 0) ? list.get(index - 1).offset : beforeListOffset;

				// Try block
				AnalyzeList(classFile, method, ft.instructions, localVariables, offsetLabelSet, beforeLoopEntryOffset,
						loopEntryOffset, afterBodyLoopOffset, tmpBeforeListOffset, afterListOffset, breakOffset,
						returnOffset);

				// Catch blocks
				int length = ft.catches.size();
				for (int i = 0; i < length; i++) {
					AnalyzeList(classFile, method, ft.catches.get(i).instructions, localVariables, offsetLabelSet,
							beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, tmpBeforeListOffset,
							afterListOffset, breakOffset, returnOffset);
				}

				// Finally block
				if (ft.finallyInstructions != null) {
					AnalyzeList(classFile, method, ft.finallyInstructions, localVariables, offsetLabelSet,
							beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, tmpBeforeListOffset,
							afterListOffset, breakOffset, returnOffset);
				}
			}
				break;
			case FastConstants.SYNCHRONIZED: {
				FastSynchronized fs = (FastSynchronized) instruction;
				int tmpBeforeListOffset = (index > 0) ? list.get(index - 1).offset : beforeListOffset;

				AnalyzeList(classFile, method, fs.instructions, localVariables, offsetLabelSet, beforeLoopEntryOffset,
						loopEntryOffset, afterBodyLoopOffset, tmpBeforeListOffset, afterListOffset, breakOffset,
						returnOffset);
			}
				break;
			case FastConstants.MONITORENTER:
			case FastConstants.MONITOREXIT: {
				// Effacement des instructions 'monitor*' pour les cas
				// exceptionnels des blocs synchronises vide.
				list.remove(index);
			}
				break;
			}

			afterListOffset = instruction.offset;
		}
	}

	private static void RemoveNopGoto(List<Instruction> list) {
		int length = list.size();

		if (length > 1) {
			int nextOffset = list.get(length - 1).offset;

			for (int index = length - 2; index >= 0; --index) {
				Instruction instruction = list.get(index);

				if (instruction.opcode == FastConstants.GOTO) {
					Goto gi = (Goto) instruction;

					if ((gi.branch >= 0) && (gi.GetJumpOffset() <= nextOffset))
						list.remove(index);
				}

				nextOffset = instruction.offset;
			}
		}
	}

	/*
	 * Strategie : 1) Les instructions 'store' et 'for' sont pass�es en revue.
	 * Si elles referencent une variables locales non encore declar�e et dont la
	 * port�e est incluse � la liste, une declaration est ins�r�e. 2) Le tableau
	 * des variables locales est pass� en revue. Pour toutes variables locales
	 * non encore declar�es et dont la port�e est incluse � la liste courante,
	 * on declare les variables en debut de bloc.
	 */
	private static void AddDeclarations(List<Instruction> list, LocalVariables localVariables, int beforeListOffset) {
		int length = list.size();

		if (length > 0) {
			// 1) Ajout de declaration sur les instructions 'store' et 'for'
			StoreInstruction si;
			LocalVariable lv;

			int lastOffset = list.get(length - 1).offset;

			for (int i = 0; i < length; i++) 
			{
				Instruction instruction = (Instruction) list.get(i);

				switch (instruction.opcode) {
				case ByteCodeConstants.ASTORE:
				case ByteCodeConstants.ISTORE:
				case ByteCodeConstants.STORE:
					si = (StoreInstruction) instruction;
					lv = localVariables.getLocalVariableWithIndexAndOffset(si.index, si.offset);
					if ((lv != null) && (lv.declarationFlag == NOT_DECLARED) && (beforeListOffset < lv.start_pc)
							&& (lv.start_pc + lv.length - 1 <= lastOffset)) {
						list.set(i, new FastDeclaration(FastConstants.DECLARE, si.offset, si.lineNumber, si.index, si));
						lv.declarationFlag = DECLARED;
						UpdateNewAndInitArrayInstruction(si);
					}
					break;
				case FastConstants.FOR:
					FastFor ff = (FastFor) instruction;
					if (ff.init != null) {
						switch (ff.init.opcode) {
						case FastConstants.ASTORE:
						case FastConstants.ISTORE:
						case FastConstants.STORE:
							si = (StoreInstruction) ff.init;
							lv = localVariables.getLocalVariableWithIndexAndOffset(si.index, si.offset);
							if ((lv != null) && (lv.declarationFlag == NOT_DECLARED)
									&& (beforeListOffset < lv.start_pc) && (lv.start_pc + lv.length - 1 <= lastOffset)) {
								ff.init = new FastDeclaration(FastConstants.DECLARE, si.offset, si.lineNumber,
										si.index, si);
								lv.declarationFlag = DECLARED;
								UpdateNewAndInitArrayInstruction(si);
							}
						}
					}
					break;
				}
			}

			// 2) Ajout de declaration pour toutes variables non encore
			// declar�es
			// TODO A affiner. Exemple:
			// 128: String message; <--- Erreur de positionnement. La
			// d�claration se limite � l'instruction
			// 'if-else'. Dupliquer dans chaque bloc.
			// 237: if (!(partnerParameters.isActive()))
			// {
			// 136: if (this.loggerTarget.isDebugEnabled())
			// {
			// 128: message = String.format("Le partenaire [%s] n'est p...
			// 136: this.loggerTarget.debug(message);
			// }
			// }
			// else if (StringUtils.equalsIgnoreCase((String)parameter...
			// {
			// 165: request.setAttribute("SSO_PARTNER_PARAMETERS", partne...
			// 184: request.setAttribute("SSO_TOKEN_VALUE", request.getPa...
			// 231: if (this.loggerTarget.isDebugEnabled())
			// {
			// 223: message = String.format("Prise en compte de la dema...
			// partnerParameters.getCpCode(), parameterName });
			// 231: this.loggerTarget.debug(message);
			// }
			int lvLength = localVariables.size();

			for (int i = 0; i < lvLength; i++) {
				lv = localVariables.getLocalVariableAt(i);

				if ((lv.declarationFlag == NOT_DECLARED) && (beforeListOffset < lv.start_pc)
						&& (lv.start_pc + lv.length - 1 <= lastOffset)) {
					int indexForNewDeclaration = InstructionUtil.getIndexForOffset(list, lv.start_pc);

					if (indexForNewDeclaration == -1) {
						// 'start_pc' offset not found
						indexForNewDeclaration = 0;
					}

					list.add(indexForNewDeclaration, new FastDeclaration(FastConstants.DECLARE, lv.start_pc,
							Instruction.UNKNOWN_LINE_NUMBER, lv.index, null));
					lv.declarationFlag = DECLARED;
				}
			}
		}
	}

	private static void UpdateNewAndInitArrayInstruction(Instruction instruction) {
		switch (instruction.opcode) {
		case FastConstants.ASTORE:
			Instruction valueref = ((StoreInstruction) instruction).valueref;
			if (valueref.opcode == FastConstants.NEWANDINITARRAY)
				valueref.opcode = FastConstants.INITARRAY;
		}
	}

	// private static void CreateContinue(
	// List<Instruction> list, int beforeLoopEntryOffset,
	// int loopEntryOffset, int returnOffset)
	// {
	// int length = list.size();
	// for (int index=0; index<length; index++)
	// {
	// Instruction instruction = list.get(index);
	//
	// switch (instruction.opcode)
	// {
	// case FastConstants.IF:
	// case FastConstants.IFCMP:
	// case FastConstants.IFXNULL:
	// case FastConstants.COMPLEXIF:
	// {
	// BranchInstruction bi = (BranchInstruction)instruction;
	// int jumpOffset = bi.GetJumpOffset();
	//
	// /* if (jumpOffset == returnOffset)
	// {
	// if (index+1 < length)
	// {
	// Instruction nextInstruction = list.get(index+1);
	//
	// // Si il n'y a pas assez de place pour une sequence
	// // 'if' + 'return', un simple 'if' sera cree.
	// if ((bi.lineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
	// (bi.lineNumber+1 == nextInstruction.lineNumber))
	// continue;
	// }
	//
	// List<Instruction> instructions =
	// new ArrayList<Instruction>(1);
	// instructions.add(new Return(
	// ByteCodeConstants.RETURN, bi.offset,
	// Instruction.UNKNOWN_LINE_NUMBER));
	// list.set(index, new FastTestList(
	// FastConstants.IF_, bi.offset, bi.lineNumber,
	// jumpOffset-bi.offset, bi, instructions));
	// }
	// else */ if ((beforeLoopEntryOffset < jumpOffset) &&
	// (jumpOffset <= loopEntryOffset))
	// {
	// if (index+1 < length)
	// {
	// Instruction nextInstruction = list.get(index+1);
	//
	// // Si il n'y a pas assez de place pour une sequence
	// // 'if' + 'continue', un simple 'if' sera cree.
	// if ((bi.lineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
	// (index+1 < length) &&
	// (bi.lineNumber+1 == nextInstruction.lineNumber))
	// continue;
	//
	// // Si l'instruction de test est suivie d'une seule instruction
	// // 'return', la sequence 'if' + 'continue' n'est pas construite.
	// if ((nextInstruction.opcode == ByteCodeConstants.RETURN) ||
	// (nextInstruction.opcode == ByteCodeConstants.XRETURN))
	// continue;
	// }
	//
	// list.set(index, new FastInstruction(
	// FastConstants.IF_CONTINUE, bi.offset,
	// bi.lineNumber, bi));
	// }
	// }
	// break;
	// }
	// }
	// }

	private static void CreateBreakAndContinue(Method method, List<Instruction> list, IntSet offsetLabelSet,
			int beforeLoopEntryOffset, int loopEntryOffset, int afterBodyLoopOffset, int afterListOffset,
			int breakOffset, int returnOffset) {
		int length = list.size();

		for (int index = 0; index < length; index++) 
		{
			Instruction instruction = list.get(index);

			switch (instruction.opcode) {
			case FastConstants.IF:
			case FastConstants.IFCMP:
			case FastConstants.IFXNULL:
			case FastConstants.COMPLEXIF: 
				{
					BranchInstruction bi = (BranchInstruction) instruction;
					int jumpOffset = bi.GetJumpOffset();
	
					if ((beforeLoopEntryOffset < jumpOffset) && (jumpOffset <= loopEntryOffset)) {
						list.set(index, new FastInstruction(
							FastConstants.IF_CONTINUE, bi.offset, bi.lineNumber, bi));
					} else if (ByteCodeUtil.JumpTo(method.getCode(), breakOffset, jumpOffset)) {
						list.set(index, new FastInstruction(
							FastConstants.IF_BREAK, bi.offset, bi.lineNumber, bi));
					} else {
						// Si la m�thode retourne 'void' et si l'instruction
						// saute un goto qui saut sur un goto ... qui saute
						// sur 'returnOffset', g�n�rer 'if-return'.
						if (ByteCodeUtil.JumpTo(method.getCode(), jumpOffset, returnOffset)) {
							List<Instruction> instructions = new ArrayList<Instruction>(1);
							instructions.add(new Return(ByteCodeConstants.RETURN, bi.offset,
									Instruction.UNKNOWN_LINE_NUMBER));
							list.set(index, new FastTestList(FastConstants.IF_, bi.offset, bi.lineNumber, jumpOffset
									- bi.offset, bi, instructions));
						} else {
							// Si l'instruction saute vers un '?return' simple, 
							// duplication de l'instruction cible pour eviter la 
							// generation d'une instruction *_LABELED_BREAK.
							byte[] code = method.getCode();
							
							// Reconnaissance bas niveau de la sequence 
							// '?load_?' suivie de '?return' en fin de methode. 
							if (code.length == jumpOffset+2)
							{
								LoadInstruction load = DuplicateLoadInstruction(
									code[jumpOffset] & 255, bi.offset, 
									Instruction.UNKNOWN_LINE_NUMBER);
								if (load != null)
								{
									ReturnInstruction ri = DuplacateReturnInstruction(
										code[jumpOffset+1] & 255, bi.offset, 
										Instruction.UNKNOWN_LINE_NUMBER, load);									
									if (ri != null)
									{
										List<Instruction> instructions = new ArrayList<Instruction>(1);
										instructions.add(ri);
										list.set(index, new FastTestList(
											FastConstants.IF_, bi.offset, bi.lineNumber, 
											jumpOffset-bi.offset, bi, instructions));
										break;
									}
								}
							}

							offsetLabelSet.add(jumpOffset);
							list.set(index, new FastInstruction(
								FastConstants.IF_LABELED_BREAK, bi.offset, bi.lineNumber, bi));
						}
					}
				}
				break;

			case FastConstants.GOTO: 
				{
					Goto g = (Goto) instruction;
					int jumpOffset = g.GetJumpOffset();
					int lineNumber = g.lineNumber;
					
					if ((index == 0) || (list.get(index-1).lineNumber == lineNumber))
						lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
	
					if ((beforeLoopEntryOffset < jumpOffset) && (jumpOffset <= loopEntryOffset)) {
						// L'instruction 'goto' saute vers le debut de la boucle
						if ((afterListOffset == afterBodyLoopOffset) && (index + 1 == length)) {
							// L'instruction 'goto' est la derniere instruction
							// a s'executer dans la boucle. Elle ne sert a rien.
							list.remove(index);
						} else {
							// Creation d'une instruction 'continue'
							list.set(index, new FastInstruction(
								FastConstants.GOTO_CONTINUE, g.offset, lineNumber, null));
						}
					} else if (ByteCodeUtil.JumpTo(method.getCode(), breakOffset, jumpOffset)) {
						list.set(index, new FastInstruction(
							FastConstants.GOTO_BREAK, g.offset, lineNumber, null));
					} else {
						// Si la m�thode retourne 'void' et si l'instruction
						// saute un goto qui saut sur un goto ... qui saute
						// sur 'returnOffset', g�n�rer 'return'.
						if (ByteCodeUtil.JumpTo(method.getCode(), jumpOffset, returnOffset)) {
							list.set(index, new Return(
								ByteCodeConstants.RETURN, g.offset, lineNumber));
						} else {
							// Si l'instruction saute vers un '?return' simple, 
							// duplication de l'instruction cible pour eviter la 
							// generation d'une instruction *_LABELED_BREAK.
							byte[] code = method.getCode();
							
							// Reconnaissance bas niveau de la sequence 
							// '?load_?' suivie de '?return' en fin de methode. 
							if (code.length == jumpOffset+2)
							{
								LoadInstruction load = DuplicateLoadInstruction(
									code[jumpOffset] & 255, g.offset, lineNumber);
								if (load != null)
								{
									ReturnInstruction ri = DuplacateReturnInstruction(
										code[jumpOffset+1] & 255, g.offset, lineNumber, load);									
									if (ri != null)
									{
										// Si l'instruction precedente est un
										// '?store' sur la meme variable et si
										// elle a le meme numero de ligne 
										// => aggregation
										if (index > 0)
										{
											instruction = list.get(index-1);
											
											if ((load.lineNumber == instruction.lineNumber) &&
												(ByteCodeConstants.ISTORE <= instruction.opcode) && 
												(instruction.opcode <= ByteCodeConstants.ASTORE_3) &&
												(load.index == ((StoreInstruction)instruction).index))
											{
												StoreInstruction si = (StoreInstruction)instruction;
												ri.valueref = si.valueref;
												list.remove(--index);
												length--;
											}
										}
										
										list.set(index, ri);
										break;
									}
								}
							}
							
							offsetLabelSet.add(jumpOffset);
							list.set(index, new FastInstruction(
								FastConstants.GOTO_LABELED_BREAK, 
								g.offset, lineNumber, g));
						}
					}
				}
				break;
			}
		}
	}
	
	private static LoadInstruction DuplicateLoadInstruction(
		int opcode, int offset, int lineNumber)
	{
		switch (opcode)
		{
		case ByteCodeConstants.ILOAD:
			return new ILoad(ByteCodeConstants.ILOAD, offset, lineNumber, 0);
		case ByteCodeConstants.LLOAD:
			return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, 0, "J");
		case ByteCodeConstants.FLOAD:
			return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, 0, "F");
		case ByteCodeConstants.DLOAD:
			return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, 0, "D");
		case ByteCodeConstants.ALOAD:
			return new ALoad(ByteCodeConstants.ALOAD, offset, lineNumber, 0);
		case ByteCodeConstants.ILOAD_0:
		case ByteCodeConstants.ILOAD_1:
		case ByteCodeConstants.ILOAD_2:
		case ByteCodeConstants.ILOAD_3:
			return new ILoad(ByteCodeConstants.ILOAD, offset, lineNumber, opcode-ByteCodeConstants.ILOAD_0);	
		case ByteCodeConstants.LLOAD_0:
		case ByteCodeConstants.LLOAD_1:
		case ByteCodeConstants.LLOAD_2:
		case ByteCodeConstants.LLOAD_3:
			return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, opcode-ByteCodeConstants.LLOAD_0, "J");
		case ByteCodeConstants.FLOAD_0:
		case ByteCodeConstants.FLOAD_1:
		case ByteCodeConstants.FLOAD_2:
		case ByteCodeConstants.FLOAD_3:
			return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, opcode-ByteCodeConstants.FLOAD_0, "F");
		case ByteCodeConstants.DLOAD_0:
		case ByteCodeConstants.DLOAD_1:
		case ByteCodeConstants.DLOAD_2:
		case ByteCodeConstants.DLOAD_3:
			return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, opcode-ByteCodeConstants.DLOAD_0, "D");
		case ByteCodeConstants.ALOAD_0:
		case ByteCodeConstants.ALOAD_1:
		case ByteCodeConstants.ALOAD_2:
		case ByteCodeConstants.ALOAD_3:
			return new ALoad(ByteCodeConstants.ALOAD, offset, lineNumber, opcode-ByteCodeConstants.ALOAD_0);
		default:
			return null;
		}
	}
	
	private static ReturnInstruction DuplacateReturnInstruction(
		int opcode, int offset, int lineNumber, Instruction instruction)
	{
		switch (opcode)
		{
		case ByteCodeConstants.IRETURN:
		case ByteCodeConstants.LRETURN:
		case ByteCodeConstants.FRETURN:
		case ByteCodeConstants.DRETURN:
		case ByteCodeConstants.ARETURN:
			return new ReturnInstruction(ByteCodeConstants.XRETURN, offset, lineNumber, instruction);
		default:
			return null;
		}
	}
	
	private static int UnoptimizeIfElseInLoop(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeListOffset, int afterListOffset,
			int returnOffset, int offset, int jumpOffset, int index) {
		int firstLoopInstructionIndex = InstructionUtil.getIndexForOffset(list, jumpOffset);
		if (firstLoopInstructionIndex != -1) {
			int length = list.size();
			if (index + 1 < length) {
				int afterLoopInstructionOffset = list.get(index + 1).offset;

				// Changement du calcul du saut : on considere que
				// l'instruction vers laquelle le saut negatif pointe.
				// int afterLoopJumpOffset = SearchMinusJumpOffset(
				// list, firstLoopInstructionIndex, index,
				// jumpOffset-1, afterLoopInstructionOffset);
				int afterLoopJumpOffset;
				Instruction firstLoopInstruction = list.get(firstLoopInstructionIndex);

				switch (firstLoopInstruction.opcode) {
				case FastConstants.IF:
				case FastConstants.IFCMP:
				case FastConstants.IFXNULL:
				case FastConstants.COMPLEXIF:
				case FastConstants.GOTO:
				case FastConstants.TRY:
				case FastConstants.SYNCHRONIZED:
					BranchInstruction bi = (BranchInstruction) firstLoopInstruction;
					afterLoopJumpOffset = bi.GetJumpOffset();
					break;
				default:
					afterLoopJumpOffset = -1;
					break;
				}

				if (afterLoopJumpOffset > afterLoopInstructionOffset) {
					int afterLoopInstructionIndex = InstructionUtil.getIndexForOffset(list, afterLoopJumpOffset);

					if ((afterLoopInstructionIndex == -1) && (afterLoopJumpOffset <= afterListOffset))
						afterLoopInstructionIndex = length;

					if (afterLoopInstructionIndex != -1) {
						int lastInstructionoffset = list.get(afterLoopInstructionIndex - 1).offset;

						if (// Check previous instructions
						InstructionUtil.CheckNoJumpToInterval(list, 0, firstLoopInstructionIndex, offset,
								lastInstructionoffset) &&
						// Check next instructions
								InstructionUtil.CheckNoJumpToInterval(list, afterLoopInstructionIndex, list.size(),
										offset, lastInstructionoffset)) {
							// Pattern 1:
							// 530: it = s.iterator();
							// 539: if (!it.hasNext()) goto 572;
							// 552: nodeAgentSearch = (ObjectName)it.next();
							// 564: if
							// (nodeAgentSearch.getCanonicalName().indexOf(this.asName)
							// <= 0) goto 532; <---
							// 568: found = true;
							// 569: goto 572;
							// 572: ...
							// Pour:
							// it = s.iterator();
							// while (it.hasNext()) {
							// nodeAgentSearch = (ObjectName)it.next();
							// if
							// (nodeAgentSearch.getCanonicalName().indexOf(this.asName)
							// > 0) {
							// found = true;
							// break;
							// }
							// }
							// Modification de la liste des instructions en:
							// 530: it = s.iterator();
							// 539: if (!it.hasNext()) goto 572;
							// 552: nodeAgentSearch = (ObjectName)it.next();
							// 564: if
							// (nodeAgentSearch.getCanonicalName().indexOf(this.asName)
							// <= 0) goto 532; <---
							// 568: found = true;
							// 569: goto 572;
							// 569: goto 532; <===
							// 572: ...

							// Pattern 2:
							// 8: this.byteOff = paramInt1;
							// 16: if (this.byteOff>=paramInt2) goto 115; <---
							// ...
							// 53: if (i >= 0) goto 76;
							// 59: tmp59_58 = this;
							// 72: paramArrayOfChar[this.charOff++] = (char)i;
							// 73: goto 11;
							// 80: if (!this.subMode) goto 102;
							// 86: tmp86_85 = this;
							// 98: paramArrayOfChar[(this.charOff++)] = 65533;
							// 99: goto 11; <---
							// 104: this.badInputLength = 1;
							// 114: throw new UnknownCharacterException();
							// 122: return this.charOff - paramInt3;
							// Pour:
							// for(byteOff = i; byteOff < j;)
							// {
							// ...
							// if(byte0 >= 0)
							// {
							// ac[charOff++] = (char)byte0;
							// }
							// else if(subMode)
							// {
							// ac[charOff++] = '\uFFFD';
							// }
							// else
							// {
							// badInputLength = 1;
							// throw new UnknownCharacterException();
							// }
							// }
							// return charOff - k;
							// Modification de la liste des instructions en:
							// 8: this.byteOff = paramInt1;
							// 16: if (this.byteOff>=paramInt2) goto 115; <---
							// ...
							// 53: if (i >= 0) goto 76;
							// 59: tmp59_58 = this;
							// 72: paramArrayOfChar[this.charOff++] = (char)i;
							// 73: goto 11;
							// 80: if (!this.subMode) goto 102;
							// 86: tmp86_85 = this;
							// 98: paramArrayOfChar[(this.charOff++)] = 65533;
							// 99: goto 11; <---
							// 104: this.badInputLength = 1;
							// 114: throw new UnknownCharacterException();
							// 114: goto 11; <===
							// 122: return this.charOff - paramInt3;
							Instruction lastInstruction = list.get(afterLoopInstructionIndex - 1);
							// Attention: le goto genere a le meme offset que
							// l'instruction precedente.
							Goto newGi = new Goto(ByteCodeConstants.GOTO, lastInstruction.offset,
									Instruction.UNKNOWN_LINE_NUMBER, jumpOffset - lastInstruction.offset);
							list.add(afterLoopInstructionIndex, newGi);

							return AnalyzeBackGoto(classFile, method, list, localVariables, offsetLabelSet,
									beforeListOffset, afterLoopJumpOffset, returnOffset, afterLoopInstructionIndex,
									newGi, jumpOffset);
						}
					}
				}
			}
		}

		return -1;
	}

	private static int UnoptimizeIfiniteLoop(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeListOffset, int afterListOffset,
			int returnOffset, BranchInstruction bi, int jumpOffset, int jumpIndex) {
		// Original:
		// 8: ...
		// 18: ...
		// 124: if (this.used.containsKey(localObject)) goto 9;
		// 127: goto 130;
		// 134: return ScriptRuntime.toString(localObject);
		// Ajout d'une insruction 'goto':
		// 8: ...
		// 18: ...
		// 124: if (this.used.containsKey(localObject)) goto 127+1; <---
		// 127: goto 130;
		// 127+1: GOTO 9 <===
		// 134: return ScriptRuntime.toString(localObject);
		int length = list.size();

		if (jumpIndex + 1 >= length)
			return -1;

		Instruction instruction = list.get(jumpIndex + 1);

		if (instruction.opcode != ByteCodeConstants.GOTO)
			return -1;

		int afterGotoOffset = (jumpIndex + 2 >= length) ? afterListOffset : list.get(jumpIndex + 2).offset;

		Goto g = (Goto) instruction;
		int jumpGotoOffset = g.GetJumpOffset();

		if ((g.offset >= jumpGotoOffset) || (jumpGotoOffset > afterGotoOffset))
			return -1;

		// Motif de code trouv�
		int newGotoOffset = g.offset + 1;

		// 1) Modification de l'offset de saut
		bi.SetJumpOffset(newGotoOffset);

		// 2) Ajout d'une nouvelle instruction 'goto'
		Goto newGoto = new Goto(ByteCodeConstants.GOTO, newGotoOffset, Instruction.UNKNOWN_LINE_NUMBER, jumpOffset
				- newGotoOffset);
		list.add(jumpIndex + 2, newGoto);

		return AnalyzeBackGoto(classFile, method, list, localVariables, offsetLabelSet, beforeListOffset,
				jumpGotoOffset, returnOffset, jumpIndex + 2, newGoto, jumpOffset);
	}

	/*
	 * debut de liste fin de liste | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
	 * beforeLoopEntryOffset afterLoopOffset
	 */
	private static void CreateLoops(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
			int beforeListOffset, int afterListOffset, int returnOffset) {
		// Unoptimize loop in loop
		int index = list.size();

		while (index-- > 0) {
			Instruction instruction = list.get(index);

			switch (instruction.opcode) {
			case FastConstants.IF:
			case FastConstants.IFCMP:
			case FastConstants.IFXNULL:
			case FastConstants.COMPLEXIF:
			case FastConstants.GOTO:
				if (UnoptimizeLoopInLoop(list, beforeListOffset, index, instruction))
					index++;
			}
		}

		// Create loops
		index = list.size();

		while (index-- > 0) {
			Instruction instruction = list.get(index);

			switch (instruction.opcode) {
			case FastConstants.IF:
			case FastConstants.IFCMP:
			case FastConstants.IFXNULL:
			case FastConstants.COMPLEXIF: {
				BranchInstruction bi = (BranchInstruction) instruction;
				if (bi.branch < 0) {
					int jumpOffset = bi.GetJumpOffset();

					if ((beforeListOffset < jumpOffset)
							&& ((beforeLoopEntryOffset >= jumpOffset) || (jumpOffset > loopEntryOffset))) {
						int newIndex = UnoptimizeIfElseInLoop(classFile, method, list, localVariables, offsetLabelSet,
								beforeListOffset, afterListOffset, returnOffset, bi.offset, jumpOffset, index);

						if (newIndex == -1) {
							newIndex = UnoptimizeIfiniteLoop(classFile, method, list, localVariables, offsetLabelSet,
									beforeListOffset, afterListOffset, returnOffset, bi, jumpOffset, index);
						}

						if (newIndex == -1) {
							index = AnalyzeBackIf(classFile, method, list, localVariables, offsetLabelSet,
									beforeListOffset, returnOffset, index, bi);
						} else {
							index = newIndex;
						}
					}
				}
			}
				break;
			case FastConstants.GOTO: {
				Goto gi = (Goto) instruction;
				if (gi.branch < 0) {
					int jumpOffset = gi.GetJumpOffset();

					if ((beforeListOffset < jumpOffset)
							&& ((beforeLoopEntryOffset >= jumpOffset) || (jumpOffset > loopEntryOffset))) {
						int newIndex = UnoptimizeIfElseInLoop(classFile, method, list, localVariables, offsetLabelSet,
								beforeListOffset, afterListOffset, returnOffset, gi.offset, jumpOffset, index);

						if (newIndex == -1) {
							index = AnalyzeBackGoto(classFile, method, list, localVariables, offsetLabelSet,
									beforeListOffset, gi.offset, returnOffset, index, gi, jumpOffset);
						} else {
							index = newIndex;
						}
					}
				}
			}
				break;
			case FastConstants.TRY:
			case FastConstants.SYNCHRONIZED: {
				FastList fl = (FastList) instruction;
				if (fl.instructions.size() > 0) {
					int previousOffset = (index > 0) ? list.get(index - 1).offset : beforeListOffset;
					int jumpOffset = fl.GetJumpOffset();

					if ((jumpOffset != -1) && (previousOffset >= jumpOffset) && (beforeListOffset < jumpOffset)
							&& ((beforeLoopEntryOffset >= jumpOffset) || (jumpOffset > loopEntryOffset))) {
						fl.branch = 1;
						int afterSubListOffset = (index + 1 < list.size()) ? list.get(index + 1).offset
								: afterListOffset;
						index = AnalyzeBackGoto(classFile, method, list, localVariables, offsetLabelSet,
								beforeListOffset, afterSubListOffset, returnOffset, index, fl, jumpOffset);
					}
				}
			}
				break;
			}
		}
	}

	private static boolean UnoptimizeLoopInLoop(List<Instruction> list, int beforeListOffset, int index,
			Instruction instruction) {
		// Retrait de l'optimisation des boucles dans les boucles c.a.d. rajout
		// de l'instruction 'goto' supprim�e.
		//
		// Original: Optimisation:
		// | |
		// ,----+ if <----. ,----+ if <-.
		// | | | | | |
		// | ,--+ if <--. | | + if --'<--.
		// | | | | | | | |
		// | | + goto -' | | + goto ----'
		// | '->+ GOTO ---' '--->|
		// '--->| |
		// |
		//
		// Original: Optimisation:
		// | |
		// ,----+ goto ,----+ goto
		// | ,--+ GOTO <-. | |
		// | | | <---. | | | <---.
		// | | | | | | | |
		// | '->+ if --' | | + if --'<--.
		// | | | | | |
		// '--->+ if ----' '--->+ if ------'
		// | |
		//
		BranchInstruction bi = (BranchInstruction) instruction;
		if (bi.branch >= 0)
			return false;

		int jumpOffset = bi.GetJumpOffset();
		if (jumpOffset <= beforeListOffset)
			return false;

		int indexBi = index;

		// Recherche de l'instruction cible et verification qu'aucune
		// instruction switch dans l'intervale ne saute pas a l'exterieur
		// de l'intervale.
		for (;;) {
			if (index == 0)
				return false;

			instruction = list.get(--index);

			if (instruction.offset <= jumpOffset)
				break;

			switch (instruction.opcode) {
			case FastConstants.LOOKUPSWITCH:
			case FastConstants.TABLESWITCH:
				Switch s = (Switch) instruction;
				if (s.offset + s.defaultOffset > bi.offset)
					return false;
				int j = s.offsets.length;
				while (j-- > 0) {
					if (s.offset + s.offsets[j] > bi.offset)
						return false;
				}
				break;
			}
		}

		instruction = list.get(index + 1);

		if (bi == instruction)
			return false;

		switch (instruction.opcode) {
		case FastConstants.IF:
		case FastConstants.IFCMP:
		case FastConstants.IFXNULL:
		case FastConstants.COMPLEXIF:
			BranchInstruction bi2 = (BranchInstruction) instruction;

			if (bi2.branch >= 0)
				return false;

			// Verification qu'aucune instruction switch definie avant
			// l'intervale ne saute dans l'intervale.
			for (int i = 0; i < index; i++) {
				instruction = list.get(i);

				switch (instruction.opcode) {
				case FastConstants.LOOKUPSWITCH:
				case FastConstants.TABLESWITCH:
					Switch s = (Switch) instruction;
					if (s.offset + s.defaultOffset > bi2.offset)
						return false;
					int j = s.offsets.length;
					while (j-- > 0) {
						if (s.offset + s.offsets[j] > bi2.offset)
							return false;
					}
					break;
				}
			}

			// Unoptimize loop in loop
			int jumpOffset2 = bi2.GetJumpOffset();

			// Recherche de l'instruction cible et verification qu'aucune
			// instruction switch dans l'intervale ne saute pas a l'exterieur
			// de l'intervale.
			for (;;) {
				if (index == 0)
					return false;

				instruction = list.get(--index);

				if (instruction.offset <= jumpOffset2)
					break;

				switch (instruction.opcode) {
				case FastConstants.LOOKUPSWITCH:
				case FastConstants.TABLESWITCH:
					Switch s = (Switch) instruction;
					if (s.offset + s.defaultOffset > bi.offset)
						return false;
					int j = s.offsets.length;
					while (j-- > 0) {
						if (s.offset + s.offsets[j] > bi.offset)
							return false;
					}
					break;
				}
			}

			Instruction target = list.get(index + 1);

			if (bi2 == target)
				return false;

			// Verification qu'aucune instruction switch definie avant
			// l'intervale ne saute dans l'intervale.
			for (int i = 0; i < index; i++) {
				instruction = list.get(i);

				switch (instruction.opcode) {
				case FastConstants.LOOKUPSWITCH:
				case FastConstants.TABLESWITCH:
					Switch s = (Switch) instruction;
					if (s.offset + s.defaultOffset > bi2.offset)
						return false;
					int j = s.offsets.length;
					while (j-- > 0) {
						if (s.offset + s.offsets[j] > bi2.offset)
							return false;
					}
					break;
				}
			}

			if (bi.opcode == ByteCodeConstants.GOTO) {
				// Original: Optimisation:
				// | |
				// ,----+ if <----. ,----+ if <-.
				// | | | | | |
				// | ,--+ if <--. | | + if --'<--.
				// | | | | | | | |
				// | | + goto -' | | + goto ----'
				// | '->+ GOTO ---' '--->|
				// '--->| |
				// |

				// 1) Create 'goto'
				list.add(indexBi + 1, new Goto(ByteCodeConstants.GOTO, bi.offset + 1, Instruction.UNKNOWN_LINE_NUMBER,
						jumpOffset2 - bi.offset - 1));
				// 2) Modify branch offset of first loop
				bi2.SetJumpOffset(bi.offset + 1);
			} else {
				// Original: Optimisation:
				// | |
				// ,----+ goto ,----+ goto
				// | ,--+ GOTO <-. | |
				// | | | <---. | | | <---.
				// | | | | | | | |
				// | '->+ if --' | | + if --'<--.
				// | | | | | |
				// '--->+ if ----' '--->+ if ------'
				// | |
				if ((target.opcode == ByteCodeConstants.GOTO) && (((Goto) target).GetJumpOffset() == jumpOffset2)) {
					// 'goto' exists
					// 1) Modify branch offset of first loop
					bi.SetJumpOffset(jumpOffset2);
				} else {
					// Goto does not exist
					// 1) Create 'goto'
					list.add(index + 1, new Goto(ByteCodeConstants.GOTO, jumpOffset2 - 1,
							Instruction.UNKNOWN_LINE_NUMBER, jumpOffset - jumpOffset2 + 1));
					// 2) Modify branch offset of first loop
					bi.SetJumpOffset(jumpOffset2 - 1);
					return true;
				}
			}
		}

		return false;
	}

	/*
	 * debut de liste fin de liste | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
	 * beforeLoopEntryOffset afterLoopOffset
	 */
	private static void CreateIfElse(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
			int afterBodyLoopOffset, int afterListOffset, int breakOffset, int returnOffset) {
		// Create if and if-else
		int length = list.size();
		for (int index = 0; index < length; index++) {
			Instruction instruction = list.get(index);

			switch (instruction.opcode) {
			case FastConstants.IF:
			case FastConstants.IFCMP:
			case FastConstants.IFXNULL:
			case FastConstants.COMPLEXIF:
				AnalyzeIfAndIfElse(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset,
						loopEntryOffset, afterBodyLoopOffset, afterListOffset, breakOffset, returnOffset, index,
						(ConditionalBranchInstruction) instruction);
				length = list.size();
				break;
			}
		}
	}

	/*
	 * debut de liste fin de liste | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
	 * beforeLoopEntryOffset afterLoopOffset
	 */
	private static void CreateSwitch(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
			int afterBodyLoopOffset, int afterListOffset, int returnOffset) {
		// Create switch
		for (int index = 0; index < list.size(); index++) {
			Instruction instruction = list.get(index);

			switch (instruction.opcode) {
			case FastConstants.LOOKUPSWITCH:
				AnalyzeLookupSwitch(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset,
						loopEntryOffset, afterBodyLoopOffset, afterListOffset, returnOffset, index,
						(LookupSwitch) instruction);
				break;

			case FastConstants.TABLESWITCH:
				index = AnalyzeTableSwitch(classFile, method, list, localVariables, offsetLabelSet,
						beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, afterListOffset, returnOffset,
						index, (TableSwitch) instruction);
				break;
			}
		}
	}

	private static void RemoveLocalVariable(Method method, IndexInstruction ii) 
	{
		LocalVariable lv = method.getLocalVariables().searchLocalVariableWithIndexAndOffset(ii.index, ii.offset);

		if ((lv != null) && (ii.offset == lv.start_pc)) {
			method.getLocalVariables().removeLocalVariableWithIndexAndOffset(ii.index, ii.offset);
		}
	}

	/*
	 * debut de liste fin de liste | testIndex | | | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
	 * beforeLoopEntryOffset afterLoopOffset
	 * 
	 * Pour les boucles 'for', beforeLoopEntryOffset & loopEntryOffset encadrent
	 * l'instruction d'incrementation.
	 */
	private static int AnalyzeBackIf(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeListOffset, int returnOffset,
			int testIndex, Instruction test) {
		int index = testIndex - 1;
		List<Instruction> subList = new ArrayList<Instruction>();
		int firstOffset = ((BranchInstruction) test).GetJumpOffset();

		int beforeLoopEntryOffset = (index >= 0) ? list.get(index).offset : beforeListOffset;

		// Move body of loop in a new list
		while ((index >= 0) && (list.get(index).offset >= firstOffset))
			subList.add(list.remove(index--));

		int subListLength = subList.size();

		// Search escape offset
		if (index >= 0)
			beforeListOffset = list.get(index).offset;
		int breakOffset = SearchMinusJumpOffset(subList, 0, subListLength, beforeListOffset, test.offset);

		// Search jump instruction before 'while' loop
		Instruction jumpInstructionBeforeLoop = null;

		if (index >= 0) {
			int i = index + 1;

			while (i-- > 0) {
				Instruction instruction = list.get(i);

				switch (instruction.opcode) {
				case FastConstants.IF:
				case FastConstants.IFCMP:
				case FastConstants.IFXNULL:
				case FastConstants.COMPLEXIF:
				case FastConstants.TRY:
				case FastConstants.SYNCHRONIZED:
				case FastConstants.GOTO:
					BranchInstruction bi = (BranchInstruction) instruction;
					int offset = bi.GetJumpOffset();
					int lastBodyOffset = (subList.size() > 0) ? subList.get(0).offset : bi.offset;

					if ((lastBodyOffset < offset) && (offset <= test.offset)) {
						jumpInstructionBeforeLoop = bi;
						i = 0; // Fin de boucle
					}
				}
			}
		}

		if (jumpInstructionBeforeLoop != null) 
		{
			// Remove 'goto' before 'while' loop
			if (jumpInstructionBeforeLoop.opcode == FastConstants.GOTO)
				list.remove(index--);

			Instruction beforeLoop = ((index >= 0) && (index < list.size())) ? list.get(index) : null;

			Instruction lastBodyLoop = null;
			Instruction beforeLastBodyLoop = null;

			if (subListLength > 0) {
				lastBodyLoop = subList.get(0);

				if (subListLength > 1) {
					beforeLastBodyLoop = subList.get(1);

					// V�rification qu'aucune instruction ne saute entre
					// 'lastBodyLoop' et 'test'
					if (!InstructionUtil.CheckNoJumpToInterval(subList, 0, subListLength, lastBodyLoop.offset,
							test.offset)) {
						// 'lastBodyLoop' ne peut pas etre l'instruction
						// d'incrementation d'une boucle 'for'
						lastBodyLoop = null;
						beforeLastBodyLoop = null;
					}
				}
			}

			// if instruction before while loop affect same variable
			// last instruction of loop, create For loop.
			int typeLoop = GetLoopType(beforeLoop, test, beforeLastBodyLoop, lastBodyLoop);

			switch (typeLoop) {
			case 2: // while (test)
				if (subListLength > 0) {
					Collections.reverse(subList);
					AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
							test.offset, test.offset, jumpInstructionBeforeLoop.offset, test.offset, breakOffset,
							returnOffset);
				}

				int branch = 1;
				if (breakOffset != -1)
					branch = breakOffset - test.offset;

				list.set(++index, new FastTestList(
					FastConstants.WHILE, test.offset, test.lineNumber, 
					branch, test, subList));
				break;
			case 3: // for (beforeLoop; test;)
				// Remove initialisation instruction before sublist
				list.remove(index);

				if (subListLength > 0) {
					Collections.reverse(subList);
					AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
							test.offset, test.offset, jumpInstructionBeforeLoop.offset, test.offset, breakOffset,
							returnOffset);
				}

				CreateForLoopCase1(classFile, method, list, index, beforeLoop, test, subList, breakOffset);
				break;
			case 6: // for (; test; lastBodyLoop)
				if (subListLength > 1) {
					Collections.reverse(subList);
					// Remove incrementation instruction
					subList.remove(--subListLength);

					AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLastBodyLoop.offset,
							lastBodyLoop.offset, lastBodyLoop.offset, jumpInstructionBeforeLoop.offset,
							lastBodyLoop.offset, breakOffset, returnOffset);

					branch = 1;
					if (breakOffset != -1)
						branch = breakOffset - test.offset;

					list.set(++index, new FastFor(FastConstants.FOR, test.offset, test.lineNumber, branch, null, test,
							lastBodyLoop, subList));
				} else {
					if (subListLength == 1) {
						AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
								test.offset, test.offset, jumpInstructionBeforeLoop.offset, test.offset, breakOffset,
								returnOffset);
					}

					branch = 1;
					if (breakOffset != -1)
						branch = breakOffset - test.offset;

					list.set(++index, new FastTestList(FastConstants.WHILE, test.offset, test.lineNumber, branch, test,
							subList));
				}
				break;
			case 7: // for (beforeLoop; test; lastBodyLoop)
				if (subListLength > 0) {
					// Remove initialisation instruction before sublist
					list.remove(index);

					Collections.reverse(subList);
					// Remove incrementation instruction
					subList.remove(--subListLength);

					if (subListLength > 0)
						AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet,
								beforeLastBodyLoop.offset, lastBodyLoop.offset, lastBodyLoop.offset,
								jumpInstructionBeforeLoop.offset, lastBodyLoop.offset, breakOffset, returnOffset);
				}

				index = CreateForLoopCase3(classFile, method, list, index, beforeLoop, test, lastBodyLoop, subList,
						breakOffset);
				break;
			default:
				throw new UnexpectedElementException("AnalyzeBackIf");
			}
		} else {
			if (subListLength > 0) {
				Collections.reverse(subList);
				AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
						test.offset, test.offset, beforeListOffset, test.offset, breakOffset, returnOffset);

				int branch = 1;
				if (breakOffset != -1)
					branch = breakOffset - test.offset;

				list.set(++index, new FastTestList(FastConstants.DO_WHILE, test.offset,
						Instruction.UNKNOWN_LINE_NUMBER, branch, test, subList));
			} else {
				// 'do-while' avec une liste d'instructions vide devient un
				// 'while'.
				list.set(++index, new FastTestList(FastConstants.WHILE, test.offset, test.lineNumber, 1, test, null));
			}
		}

		return index;
	}

	private static int SearchMinusJumpOffset(
			List<Instruction> list, 
			int fromIndex, int toIndex, 
			int beforeListOffset, int lastListOffset) 
	{
		int breakOffset = -1;
		int index = toIndex;

		while (index-- > fromIndex) {
			Instruction instruction = list.get(index);

			switch (instruction.opcode) {
			case FastConstants.GOTO:
			case FastConstants.IF:
			case FastConstants.IFCMP:
			case FastConstants.IFXNULL:
			case FastConstants.COMPLEXIF:
				BranchInstruction bi = (BranchInstruction) instruction;
				int jumpOffset = bi.GetJumpOffset();

				if ((jumpOffset != -1) && ((jumpOffset <= beforeListOffset) || (lastListOffset < jumpOffset))) 
				{
					if ((breakOffset == -1) || (breakOffset > jumpOffset))
						breakOffset = jumpOffset;
				}
				break;
			case FastConstants.FOR:
			case FastConstants.FOREACH:
			case FastConstants.WHILE:
			case FastConstants.DO_WHILE:
			case FastConstants.SYNCHRONIZED:
				FastList fl = (FastList) instruction;
				List<Instruction> instructions = fl.instructions;
				if (instructions != null) 
				{
					jumpOffset = SearchMinusJumpOffset(instructions, 0, instructions.size(), beforeListOffset,
							lastListOffset);

					if ((jumpOffset != -1) && ((jumpOffset <= beforeListOffset) || (lastListOffset < jumpOffset))) {
						if ((breakOffset == -1) || (breakOffset > jumpOffset))
							breakOffset = jumpOffset;
					}
				}
				break;
			case FastConstants.TRY:
				FastTry ft = (FastTry) instruction;

				jumpOffset = ft.GetJumpOffset();

				if ((jumpOffset != -1) && ((jumpOffset <= beforeListOffset) || (lastListOffset < jumpOffset))) {
					if ((breakOffset == -1) || (breakOffset > jumpOffset))
						breakOffset = jumpOffset;
				}

				// Try block
				instructions = ft.instructions;
				jumpOffset = SearchMinusJumpOffset(instructions, 0, instructions.size(), beforeListOffset,
						lastListOffset);

				if ((jumpOffset != -1) && ((jumpOffset <= beforeListOffset) || (lastListOffset < jumpOffset))) 
				{
					if ((breakOffset == -1) || (breakOffset > jumpOffset))
						breakOffset = jumpOffset;
				}

				// Catch blocks
				int i = ft.catches.size();
				while (i-- > 0) 
				{
					List<Instruction> catchInstructions = ft.catches.get(i).instructions;
					jumpOffset = SearchMinusJumpOffset(catchInstructions, 0, catchInstructions.size(),
							beforeListOffset, lastListOffset);

					if ((jumpOffset != -1) && ((jumpOffset <= beforeListOffset) || (lastListOffset < jumpOffset))) 
					{
						if ((breakOffset == -1) || (breakOffset > jumpOffset))
							breakOffset = jumpOffset;
					}
				}

				// Finally block
				if (ft.finallyInstructions != null) 
				{
					List<Instruction> finallyInstructions = ft.finallyInstructions;
					jumpOffset = SearchMinusJumpOffset(finallyInstructions, 0, finallyInstructions.size(),
							beforeListOffset, lastListOffset);

					if ((jumpOffset != -1) && ((jumpOffset <= beforeListOffset) || (lastListOffset < jumpOffset))) 
					{
						if ((breakOffset == -1) || (breakOffset > jumpOffset))
							breakOffset = jumpOffset;
					}
				}
				break;
			case FastConstants.SWITCH:
			case FastConstants.SWITCH_ENUM:
			case FastConstants.SWITCH_STRING:
				FastSwitch fs = (FastSwitch) instruction;

				jumpOffset = fs.GetJumpOffset();

				if ((jumpOffset != -1) && ((jumpOffset <= beforeListOffset) || (lastListOffset < jumpOffset))) 
				{
					if ((breakOffset == -1) || (breakOffset > jumpOffset))
						breakOffset = jumpOffset;
				}

				i = fs.pairs.length;
				while (i-- > 0) 
				{
					List<Instruction> caseInstructions = 
						fs.pairs[i].getInstructions();
					if (caseInstructions != null) 
					{
						jumpOffset = SearchMinusJumpOffset(caseInstructions, 0, caseInstructions.size(),
								beforeListOffset, lastListOffset);

						if ((jumpOffset != -1) && ((jumpOffset <= beforeListOffset) || (lastListOffset < jumpOffset))) {
							if ((breakOffset == -1) || (breakOffset > jumpOffset))
								breakOffset = jumpOffset;
						}
					}
				}
				break;
			}
		}

		return breakOffset;
	}

	private static int GetMaxOffset(Instruction beforeWhileLoop, Instruction test) {
		return (beforeWhileLoop.offset > test.offset) ? beforeWhileLoop.offset : test.offset;
	}

	private static int GetMaxOffset(Instruction beforeWhileLoop, Instruction test, Instruction lastBodyWhileLoop) {
		int offset = GetMaxOffset(beforeWhileLoop, test);

		return (offset > lastBodyWhileLoop.offset) ? offset : lastBodyWhileLoop.offset;
	}

	private static Instruction CreateForEachVariableInstruction(Instruction i) {
		switch (i.opcode) {
		case FastConstants.DECLARE:
			((FastDeclaration) i).instruction = null;
			return i;
		case FastConstants.ASTORE:
			return new ALoad(FastConstants.ALOAD, i.offset, i.lineNumber, ((AStore) i).index);
		case FastConstants.ISTORE:
			return new ILoad(FastConstants.ILOAD, i.offset, i.lineNumber, ((IStore) i).index);
		case FastConstants.STORE:
			return new LoadInstruction(FastConstants.LOAD, i.offset, i.lineNumber, ((StoreInstruction) i).index,
					((StoreInstruction) i).getReturnedSignature(null, null));
		default:
			return i;
		}
	}

	private static void CreateForLoopCase1(ClassFile classFile, Method method, List<Instruction> list,
			int beforeWhileLoopIndex, Instruction beforeWhileLoop, Instruction test, List<Instruction> subList,
			int breakOffset) {
		int forLoopOffset = GetMaxOffset(beforeWhileLoop, test);

		int branch = 1;
		if (breakOffset != -1)
			branch = breakOffset - forLoopOffset;

		// Is a for-each pattern ?
		if (IsAForEachIteratorPattern(classFile, method, beforeWhileLoop, test, subList)) {
			Instruction variable = CreateForEachVariableInstruction(subList.remove(0));

			InvokeNoStaticInstruction insi = (InvokeNoStaticInstruction) (((AStore) beforeWhileLoop).valueref);
			Instruction values = insi.objectref;

			// Remove iterator local variable
			RemoveLocalVariable(method, (StoreInstruction) beforeWhileLoop);

			list.set(beforeWhileLoopIndex, new FastForEach(FastConstants.FOREACH, forLoopOffset,
					beforeWhileLoop.lineNumber, branch, variable, values, subList));
		} else {
			list.set(beforeWhileLoopIndex, new FastFor(FastConstants.FOR, forLoopOffset, beforeWhileLoop.lineNumber,
					branch, beforeWhileLoop, test, null, subList));
		}
	}

	private static int CreateForLoopCase3(ClassFile classFile, Method method, List<Instruction> list,
			int beforeWhileLoopIndex, Instruction beforeWhileLoop, Instruction test, Instruction lastBodyWhileLoop,
			List<Instruction> subList, int breakOffset) {
		int forLoopOffset = GetMaxOffset(beforeWhileLoop, test, lastBodyWhileLoop);

		int branch = 1;
		if (breakOffset != -1)
			branch = breakOffset - forLoopOffset;

		// Is a for-each pattern ?
		switch (GetForEachArrayPatternType(classFile, beforeWhileLoop, test, lastBodyWhileLoop, list,
				beforeWhileLoopIndex, subList)) {
		case 1: // SUN 1.5
		{
			Instruction variable = CreateForEachVariableInstruction(subList.remove(0));

			StoreInstruction beforeBeforeWhileLoop = (StoreInstruction) list.remove(--beforeWhileLoopIndex);
			AssignmentInstruction ai = (AssignmentInstruction) ((ArrayLength) beforeBeforeWhileLoop.valueref).arrayref;
			Instruction values = ai.value2;

			// Remove lenght local variable
			RemoveLocalVariable(method, beforeBeforeWhileLoop);
			// Remove index local variable
			RemoveLocalVariable(method, (StoreInstruction) beforeWhileLoop);
			// Remove array tmp local variable
			RemoveLocalVariable(method, (AStore) ai.value1);

			list.set(beforeWhileLoopIndex, new FastForEach(FastConstants.FOREACH, forLoopOffset, variable.lineNumber,
					branch, variable, values, subList));
		}
			break;
		case 2: // SUN 1.6
		{
			Instruction variable = CreateForEachVariableInstruction(subList.remove(0));

			StoreInstruction beforeBeforeWhileLoop = (StoreInstruction) list.remove(--beforeWhileLoopIndex);

			StoreInstruction beforeBeforeBeforeWhileLoop = (StoreInstruction) list.remove(--beforeWhileLoopIndex);
			Instruction values = beforeBeforeBeforeWhileLoop.valueref;

			// Remove lenght local variable
			RemoveLocalVariable(method, beforeBeforeWhileLoop);
			// Remove index local variable
			RemoveLocalVariable(method, (StoreInstruction) beforeWhileLoop);
			// Remove array tmp local variable
			RemoveLocalVariable(method, beforeBeforeBeforeWhileLoop);

			list.set(beforeWhileLoopIndex, new FastForEach(FastConstants.FOREACH, forLoopOffset, variable.lineNumber,
					branch, variable, values, subList));
		}
			break;
		case 3: // IBM
		{
			Instruction variable = CreateForEachVariableInstruction(subList.remove(0));

			StoreInstruction siIndex = (StoreInstruction) list.remove(--beforeWhileLoopIndex);

			StoreInstruction siTmpArray = (StoreInstruction) list.remove(--beforeWhileLoopIndex);
			Instruction values = siTmpArray.valueref;

			// Remove lenght local variable
			RemoveLocalVariable(method, (StoreInstruction) beforeWhileLoop);
			// Remove index local variable
			RemoveLocalVariable(method, siIndex);
			// Remove array tmp local variable
			RemoveLocalVariable(method, siTmpArray);

			list.set(beforeWhileLoopIndex, new FastForEach(FastConstants.FOREACH, forLoopOffset, variable.lineNumber,
					branch, variable, values, subList));
		}
			break;
		default: {
			list.set(beforeWhileLoopIndex, new FastFor(FastConstants.FOR, forLoopOffset, beforeWhileLoop.lineNumber,
					branch, beforeWhileLoop, test, lastBodyWhileLoop, subList));
		}
		}

		return beforeWhileLoopIndex;
	}

	/*
	 * Pattern: 7: List strings = new ArrayList(); 44: for (Iterator
	 * localIterator = strings.iterator(); localIterator.hasNext(); ) { 29:
	 * String s = (String)localIterator.next(); 34: System.out.println(s); }
	 */
	private static boolean IsAForEachIteratorPattern(ClassFile classFile, Method method, Instruction init,
			Instruction test, List<Instruction> subList) {
		// Tests: (Java 5 or later) + (Not empty sub list)
		if ((classFile.getMajorVersion() < 49) || (subList.size() == 0))
			return false;

		Instruction firstInstruction = subList.get(0);

		// Test: Same line number
		if (test.lineNumber != firstInstruction.lineNumber)
			return false;

		// Test 'init' instruction: Iterator localIterator = strings.iterator()
		if (init.opcode != ByteCodeConstants.ASTORE)
			return false;
		AStore astoreIterator = (AStore) init;
		if ((astoreIterator.valueref.opcode != ByteCodeConstants.INVOKEINTERFACE)
				&& (astoreIterator.valueref.opcode != ByteCodeConstants.INVOKEVIRTUAL))
			return false;
		LocalVariable lv = method.getLocalVariables().getLocalVariableWithIndexAndOffset(astoreIterator.index,
				astoreIterator.offset);
		if ((lv == null) || (lv.signature_index == 0))
			return false;
		ConstantPool constants = classFile.getConstantPool();
		InvokeNoStaticInstruction insi = (InvokeNoStaticInstruction) astoreIterator.valueref;
		ConstantMethodref cmr = constants.getConstantMethodref(insi.index);
		ConstantNameAndType cnat = constants.getConstantNameAndType(cmr.name_and_type_index);
		String iteratorMethodName = constants.getConstantUtf8(cnat.name_index);
		if (!"iterator".equals(iteratorMethodName))
			return false;
		String iteratorMethodDescriptor = constants.getConstantUtf8(cnat.descriptor_index);
		if (!"()Ljava/util/Iterator;".equals(iteratorMethodDescriptor))
			return false;

		// Test 'test' instruction: localIterator.hasNext()
		if (test.opcode != ByteCodeConstants.IF)
			return false;
		IfInstruction ifi = (IfInstruction) test;
		if (ifi.value.opcode != ByteCodeConstants.INVOKEINTERFACE)
			return false;
		insi = (InvokeNoStaticInstruction) ifi.value;
		if ((insi.objectref.opcode != ByteCodeConstants.ALOAD)
				|| (((ALoad) insi.objectref).index != astoreIterator.index))
			return false;
		cmr = constants.getConstantMethodref(insi.index);
		cnat = constants.getConstantNameAndType(cmr.name_and_type_index);
		String hasNextMethodName = constants.getConstantUtf8(cnat.name_index);
		if (!"hasNext".equals(hasNextMethodName))
			return false;
		String hasNextMethodDescriptor = constants.getConstantUtf8(cnat.descriptor_index);
		if (!"()Z".equals(hasNextMethodDescriptor))
			return false;

		// Test first instruction: String s = (String)localIterator.next()
		if (firstInstruction.opcode != FastConstants.DECLARE)
			return false;
		FastDeclaration declaration = (FastDeclaration) firstInstruction;
		if (declaration.instruction == null)
			return false;
		if (declaration.instruction.opcode != FastConstants.ASTORE)
			return false;
		AStore astoreVariable = (AStore) declaration.instruction;

		if (astoreVariable.valueref.opcode == FastConstants.CHECKCAST)
		{
			// Une instruction Cast est utilis�e si le type de l'interation
			// n'est pas Object.
			CheckCast cc = (CheckCast) astoreVariable.valueref;
			if (cc.objectref.opcode != FastConstants.INVOKEINTERFACE)
				return false;
			insi = (InvokeNoStaticInstruction) cc.objectref;
		}
		else
		{
			if (astoreVariable.valueref.opcode != FastConstants.INVOKEINTERFACE)
				return false;
			insi = (InvokeNoStaticInstruction)astoreVariable.valueref;
		}		
		
		if ((insi.objectref.opcode != ByteCodeConstants.ALOAD)
				|| (((ALoad) insi.objectref).index != astoreIterator.index))
			return false;
		cmr = constants.getConstantMethodref(insi.index);
		cnat = constants.getConstantNameAndType(cmr.name_and_type_index);
		String nextMethodName = constants.getConstantUtf8(cnat.name_index);
		if (!"next".equals(nextMethodName))
			return false;
		String nextMethodDescriptor = constants.getConstantUtf8(cnat.descriptor_index);
		if (!"()Ljava/lang/Object;".equals(nextMethodDescriptor))
			return false;

		return true;
	}

	/*
	 * Pattern SUN 1.5: 14: String[] strings = { "a", "b" }; 20: int j =
	 * (arrayOfString1 = strings).length; 48: for (int i = 0; i < j; ++i) { 33:
	 * String s = arrayOfString1[i]; 38: System.out.println(s); }
	 * 
	 * Return 0: No pattern 1: Pattern SUN 1.5
	 */
	private static int GetForEachArraySun15PatternType(Instruction init, Instruction test, Instruction inc,
			Instruction firstInstruction, StoreInstruction siLenght) {
		// Test before 'for' instruction: j = (arrayOfString1 = strings).length;
		ArrayLength al = ((ArrayLength) siLenght.valueref);
		if (al.arrayref.opcode != ByteCodeConstants.ASSIGNMENT)
			return 0;
		AssignmentInstruction ai = (AssignmentInstruction) al.arrayref;
		if ((!ai.operator.equals("=")) || (ai.value1.opcode != ByteCodeConstants.ASTORE))
			return 0;
		StoreInstruction siTmpArray = (StoreInstruction) ai.value1;

		// Test 'init' instruction: int i = 0
		if (init.opcode != ByteCodeConstants.ISTORE)
			return 0;
		StoreInstruction siIndex = (StoreInstruction) init;
		if (siIndex.valueref.opcode != ByteCodeConstants.ICONST)
			return 0;
		IConst iconst = (IConst) siIndex.valueref;
		if ((iconst.value != 0) || (!iconst.signature.equals("I")))
			return 0;

		// Test 'test' instruction: i < j
		if (test.opcode != ByteCodeConstants.IFCMP)
			return 0;
		IfCmp ifcmp = (IfCmp) test;
		if ((ifcmp.value1.opcode != ByteCodeConstants.ILOAD) || (ifcmp.value2.opcode != ByteCodeConstants.ILOAD)
				|| (((ILoad) ifcmp.value1).index != siIndex.index) || (((ILoad) ifcmp.value2).index != siLenght.index))
			return 0;

		// Test 'inc' instruction: ++i
		if ((inc.opcode != ByteCodeConstants.IINC) || (((IInc) inc).index != siIndex.index)
				|| (((IInc) inc).count != 1))
			return 0;

		// Test first instruction: String s = arrayOfString1[i];
		if (firstInstruction.opcode == FastConstants.DECLARE) {
			FastDeclaration declaration = (FastDeclaration) firstInstruction;
			if (declaration.instruction == null)
				return 0;
			firstInstruction = declaration.instruction;
		}
		if ((firstInstruction.opcode != FastConstants.STORE) && (firstInstruction.opcode != FastConstants.ASTORE)
				&& (firstInstruction.opcode != FastConstants.ISTORE))
			return 0;
		StoreInstruction siVariable = (StoreInstruction) firstInstruction;
		if (siVariable.valueref.opcode != ByteCodeConstants.ARRAYLOAD)
			return 0;
		ArrayLoadInstruction ali = (ArrayLoadInstruction) siVariable.valueref;
		if ((ali.arrayref.opcode != ByteCodeConstants.ALOAD) || (ali.indexref.opcode != ByteCodeConstants.ILOAD)
				|| (((ALoad) ali.arrayref).index != siTmpArray.index)
				|| (((ILoad) ali.indexref).index != siIndex.index))
			return 0;

		return 1;
	}

	/*
	 * Pattern SUN 1.6: String[] arr$ = { "a", "b" }; int len$ = arr$.length;
	 * for(int i$ = 0; i$ < len$; i$++) { String s = arr$[i$];
	 * System.out.println(s); }
	 * 
	 * Return 0: No pattern 2: Pattern SUN 1.6
	 */
	private static int GetForEachArraySun16PatternType(Instruction init, Instruction test, Instruction inc,
			Instruction firstInstruction, StoreInstruction siLenght, Instruction beforeBeforeForInstruction) {
		// Test before 'for' instruction: len$ = arr$.length;
		ArrayLength al = ((ArrayLength) siLenght.valueref);
		if (al.arrayref.opcode != ByteCodeConstants.ALOAD)
			return 0;

		// Test before before 'for' instruction: arr$ = ...;
		if (beforeBeforeForInstruction.opcode != ByteCodeConstants.ASTORE)
			return 0;
		StoreInstruction siTmpArray = (StoreInstruction) beforeBeforeForInstruction;
		if (siTmpArray.index != ((IndexInstruction) al.arrayref).index)
			return 0;

		// Test 'init' instruction: int i = 0
		if (init.opcode != ByteCodeConstants.ISTORE)
			return 0;
		StoreInstruction siIndex = (StoreInstruction) init;
		if (siIndex.valueref.opcode != ByteCodeConstants.ICONST)
			return 0;
		IConst iconst = (IConst) siIndex.valueref;
		if ((iconst.value != 0) || (!iconst.signature.equals("I")))
			return 0;

		// Test 'test' instruction: i < j
		if (test.opcode != ByteCodeConstants.IFCMP)
			return 0;
		IfCmp ifcmp = (IfCmp) test;
		if ((ifcmp.value1.opcode != ByteCodeConstants.ILOAD) || (ifcmp.value2.opcode != ByteCodeConstants.ILOAD)
				|| (((ILoad) ifcmp.value1).index != siIndex.index) || (((ILoad) ifcmp.value2).index != siLenght.index))
			return 0;

		// Test 'inc' instruction: ++i
		if ((inc.opcode != ByteCodeConstants.IINC) || (((IInc) inc).index != siIndex.index)
				|| (((IInc) inc).count != 1))
			return 0;

		// Test first instruction: String s = arrayOfString1[i];
		if (firstInstruction.opcode == FastConstants.DECLARE) {
			FastDeclaration declaration = (FastDeclaration) firstInstruction;
			if (declaration.instruction == null)
				return 0;
			firstInstruction = declaration.instruction;
		}
		if ((firstInstruction.opcode != FastConstants.STORE) && (firstInstruction.opcode != FastConstants.ASTORE)
				&& (firstInstruction.opcode != FastConstants.ISTORE))
			return 0;
		StoreInstruction siVariable = (StoreInstruction) firstInstruction;
		if (siVariable.valueref.opcode != ByteCodeConstants.ARRAYLOAD)
			return 0;
		ArrayLoadInstruction ali = (ArrayLoadInstruction) siVariable.valueref;
		if ((ali.arrayref.opcode != ByteCodeConstants.ALOAD) || (ali.indexref.opcode != ByteCodeConstants.ILOAD)
				|| (((ALoad) ali.arrayref).index != siTmpArray.index)
				|| (((ILoad) ali.indexref).index != siIndex.index))
			return 0;

		return 2;
	}

	/*
	 * Pattern IBM: 81: Object localObject = args; 84: GUIMap guiMap = 0; 116:
	 * for (GUIMap localGUIMap1 = localObject.length; guiMap < localGUIMap1;
	 * ++guiMap) { 99: String arg = localObject[guiMap]; 106:
	 * System.out.println(arg); }
	 * 
	 * Return 0: No pattern 3: Pattern IBM
	 */
	private static int GetForEachArrayIbmPatternType(ClassFile classFile, Instruction init, Instruction test,
			Instruction inc, List<Instruction> list, int beforeWhileLoopIndex, Instruction firstInstruction,
			StoreInstruction siIndex) {
		// Test before 'for' instruction: guiMap = 0;
		IConst icont = ((IConst) siIndex.valueref);
		if (icont.value != 0)
			return 0;

		// Test before before 'for' instruction: Object localObject = args;
		if (beforeWhileLoopIndex < 2)
			return 0;
		Instruction beforeBeforeForInstruction = list.get(beforeWhileLoopIndex - 2);
		// Test: Same line number
		if (test.lineNumber != beforeBeforeForInstruction.lineNumber)
			return 0;
		if (beforeBeforeForInstruction.opcode != ByteCodeConstants.ASTORE)
			return 0;
		StoreInstruction siTmpArray = (StoreInstruction) beforeBeforeForInstruction;

		// Test 'init' instruction: localGUIMap1 = localObject.length
		if (init.opcode != ByteCodeConstants.ISTORE)
			return 0;
		StoreInstruction siLenght = (StoreInstruction) init;
		if (siLenght.valueref.opcode != ByteCodeConstants.ARRAYLENGTH)
			return 0;
		ArrayLength al = (ArrayLength) siLenght.valueref;
		if (al.arrayref.opcode != ByteCodeConstants.ALOAD)
			return 0;
		if (((ALoad) al.arrayref).index != siTmpArray.index)
			return 0;

		// Test 'test' instruction: guiMap < localGUIMap1
		if (test.opcode != ByteCodeConstants.IFCMP)
			return 0;
		IfCmp ifcmp = (IfCmp) test;
		if ((ifcmp.value1.opcode != ByteCodeConstants.ILOAD) || (ifcmp.value2.opcode != ByteCodeConstants.ILOAD)
				|| (((ILoad) ifcmp.value1).index != siIndex.index) || (((ILoad) ifcmp.value2).index != siLenght.index))
			return 0;

		// Test 'inc' instruction: ++i
		if ((inc.opcode != ByteCodeConstants.IINC) || (((IInc) inc).index != siIndex.index)
				|| (((IInc) inc).count != 1))
			return 0;

		// Test first instruction: String arg = localObject[guiMap];
		if (firstInstruction.opcode != FastConstants.DECLARE)
			return 0;
		FastDeclaration declaration = (FastDeclaration) firstInstruction;
		if (declaration.instruction == null)
			return 0;
		if ((declaration.instruction.opcode != FastConstants.STORE)
				&& (declaration.instruction.opcode != FastConstants.ASTORE)
				&& (declaration.instruction.opcode != FastConstants.ISTORE))
			return 0;
		StoreInstruction siVariable = (StoreInstruction) declaration.instruction;
		if (siVariable.valueref.opcode != ByteCodeConstants.ARRAYLOAD)
			return 0;
		ArrayLoadInstruction ali = (ArrayLoadInstruction) siVariable.valueref;
		if ((ali.arrayref.opcode != ByteCodeConstants.ALOAD) || (ali.indexref.opcode != ByteCodeConstants.ILOAD)
				|| (((ALoad) ali.arrayref).index != siTmpArray.index)
				|| (((ILoad) ali.indexref).index != siIndex.index))
			return 0;

		return 3;
	}

	/*
	 * Pattern SUN 1.5: 14: String[] strings = { "a", "b" }; 20: int j =
	 * (arrayOfString1 = strings).length; 48: for (int i = 0; i < j; ++i) { 33:
	 * String s = arrayOfString1[i]; 38: System.out.println(s); }
	 * 
	 * Pattern SUN 1.6: String[] arr$ = { "a", "b" }; int len$ = arr$.length;
	 * for(int i$ = 0; i$ < len$; i$++) { String s = arr$[i$];
	 * System.out.println(s); }
	 * 
	 * Pattern IBM: 81: Object localObject = args; 84: GUIMap guiMap = 0; 116:
	 * for (GUIMap localGUIMap1 = localObject.length; guiMap < localGUIMap1;
	 * ++guiMap) { 99: String arg = localObject[guiMap]; 106:
	 * System.out.println(arg); }
	 * 
	 * Return 0: No pattern 1: Pattern SUN 1.5 2: Pattern SUN 1.6 3: Pattern IBM
	 */
	private static int GetForEachArrayPatternType(ClassFile classFile, Instruction init, Instruction test,
			Instruction inc, List<Instruction> list, int beforeWhileLoopIndex, List<Instruction> subList) {
		// Tests: (Java 5 or later) + (Not empty sub list)
		if ((classFile.getMajorVersion() < 49) || (beforeWhileLoopIndex == 0) || (subList.size() == 0))
			return 0;

		Instruction firstInstruction = subList.get(0);

		// Test: Same line number
		if (test.lineNumber != firstInstruction.lineNumber)
			return 0;

		Instruction beforeForInstruction = list.get(beforeWhileLoopIndex - 1);

		// Test: Same line number
		if (test.lineNumber != beforeForInstruction.lineNumber)
			return 0;

		// Test before 'for' instruction:
		// SUN 1.5: j = (arrayOfString1 = strings).length;
		// SUN 1.6: len$ = arr$.length;
		// IBM : guiMap = 0;
		if (beforeForInstruction.opcode != ByteCodeConstants.ISTORE)
			return 0;
		StoreInstruction si = (StoreInstruction) beforeForInstruction;
		if (si.valueref.opcode == ByteCodeConstants.ARRAYLENGTH) {
			ArrayLength al = ((ArrayLength) si.valueref);
			if (al.arrayref.opcode == ByteCodeConstants.ASSIGNMENT) {
				return GetForEachArraySun15PatternType(init, test, inc, firstInstruction, si);
			} else if (beforeWhileLoopIndex > 1) {
				Instruction beforeBeforeForInstruction = list.get(beforeWhileLoopIndex - 2);
				return GetForEachArraySun16PatternType(init, test, inc, firstInstruction, si,
						beforeBeforeForInstruction);
			}
		}

		if (si.valueref.opcode == ByteCodeConstants.ICONST)
			return GetForEachArrayIbmPatternType(classFile, init, test, inc, list, beforeWhileLoopIndex,
					firstInstruction, si);

		return 0;
	}

	/*
	 * Type de boucle infinie: 0: for (;;) 1: for (beforeLoop; ;) 2: while
	 * (test) 3: for (beforeLoop; test;) 4: for (; ; lastBodyLoop) 5: for
	 * (beforeLoop; ; lastBodyLoop) 6: for (; test; lastBodyLoop) 7: for
	 * (beforeLoop; test; lastBodyLoop)
	 */
	private static int GetLoopType(Instruction beforeLoop, Instruction test, Instruction beforeLastBodyLoop,
			Instruction lastBodyLoop) {
		if (beforeLoop == null) {
			// Cas possibles : 0, 2, 4, 6
			/*
			 * 0: for (;;) 2: while (test) 4: for (; ; lastBodyLoop) 6: for (;
			 * test; lastBodyLoop)
			 */
			if (test == null) {
				// Cas possibles : 0, 4
				if (lastBodyLoop == null) {
					// Cas possibles : 0
					return 0;
				} else {
					// Cas possibles : 0, 4
					return ((beforeLastBodyLoop != null) && (beforeLastBodyLoop.lineNumber > lastBodyLoop.lineNumber)) ? 4
							: 0;
				}
			} else {
				// Cas possibles : 0, 2, 4, 6
				/*
				 * 2: while (test) 6: for (; test; lastBodyLoop)
				 */
				if (lastBodyLoop == null) {
					// Cas possibles : 0, 2
					return 2;
				} else {
					// Cas possibles : 0, 2, 4, 6
					if (test.lineNumber == Instruction.UNKNOWN_LINE_NUMBER) {
						return 2;
					} else {
						return (test.lineNumber == lastBodyLoop.lineNumber) ? 6 : 2;
					}
				}
			}
		} else {
			if (beforeLoop.opcode == FastConstants.ASSIGNMENT) {
				beforeLoop = ((AssignmentInstruction) beforeLoop).value1;
			}

			// Cas possibles : 0, 1, 2, 3, 4, 5, 6, 7
			if (test == null) {
				// Cas possibles : 0, 1, 4, 5
				/*
				 * 0: for (;;) 1: for (beforeLoop; ;) 4: for (; ; lastBodyLoop)
				 * 5: for (beforeLoop; ; lastBodyLoop)
				 */
				if (lastBodyLoop == null) {
					// Cas possibles : 0, 1
					return 0;
				} else {
					if (lastBodyLoop.opcode == FastConstants.ASSIGNMENT) {
						lastBodyLoop = ((AssignmentInstruction) lastBodyLoop).value1;
					}

					// Cas possibles : 0, 1, 4, 5
					if (beforeLoop.lineNumber == Instruction.UNKNOWN_LINE_NUMBER) {
						// beforeLoop & lastBodyLoop sont-elles des instructions
						// d'affectation ou d'incrementation ?
						// (a|d|f|i|l|s)store ou iinc ?
						return CheckBeforeLoopAndLastBodyLoop(beforeLoop, lastBodyLoop) ? 5 : 0;
					} else if (beforeLoop.lineNumber == lastBodyLoop.lineNumber) {
						return 5;
					} else {
						return ((beforeLastBodyLoop != null) && 
								(beforeLastBodyLoop.lineNumber > lastBodyLoop.lineNumber)) ? 4 : 0;
					}
				}
			} else {
				// Cas possibles : 2, 3, 4, 5, 6, 7
				if (lastBodyLoop == null) {
					// Cas possibles : 2, 3
					/*
					 * 2: while (test) 3: for (beforeLoop; test;)
					 */
					if (beforeLoop.lineNumber == Instruction.UNKNOWN_LINE_NUMBER) {
						return 2;
					} else {
						return (beforeLoop.lineNumber == test.lineNumber) ? 3 : 2;
					}
				} else {
					if (lastBodyLoop.opcode == FastConstants.ASSIGNMENT) {
						lastBodyLoop = ((AssignmentInstruction) lastBodyLoop).value1;
					}

					// Cas possibles : 0, 1, 2, 3, 4, 5, 6, 7
					if (beforeLoop.lineNumber == Instruction.UNKNOWN_LINE_NUMBER) {
						// beforeLoop & lastBodyLoop sont-elles des instructions
						// d'affectation ou d'incrementation ?
						// (a|d|f|i|l|s)store ou iinc ?
						/*
						 * 2: while (test) 7: for (beforeLoop; test;
						 * lastBodyLoop)
						 */
						return CheckBeforeLoopAndLastBodyLoop(beforeLoop, lastBodyLoop) ? 7 : 2;
					} else {
						if (beforeLastBodyLoop == null) {
							if (beforeLoop.lineNumber == test.lineNumber) {
								// Cas possibles : 3, 7
								/*
								 * 3: for (beforeLoop; test;) 7: for
								 * (beforeLoop; test; lastBodyLoop)
								 */
								return (beforeLoop.lineNumber == lastBodyLoop.lineNumber) ? 7 : 3;
							} else {
								// Cas possibles : 2, 6
								/*
								 * 2: while (test) 6: for (; test; lastBodyLoop)
								 */
								return (test.lineNumber == lastBodyLoop.lineNumber) ? 6 : 2;
							}
						} else {
							if (beforeLastBodyLoop.lineNumber >= lastBodyLoop.lineNumber) {
								// Cas possibles : 6, 7
								/*
								 * 6: for (; test; lastBodyLoop) 7: for
								 * (beforeLoop; test; lastBodyLoop)
								 */
								if (beforeLoop.lineNumber == test.lineNumber) {
									return 7;
								} else {
									return CheckBeforeLoopAndLastBodyLoop(beforeLoop, lastBodyLoop) ? 7 : 6;
								}
							} else {
								// Cas possibles : 2, 3
								/*
								 * 2: while (test) 3: for (beforeLoop; test;)
								 */
								return (beforeLoop.lineNumber == test.lineNumber) ? 3 : 2;
							}
						}
					}
				}
			}
		}
	}

	private static boolean CheckBeforeLoopAndLastBodyLoop(Instruction beforeLoop, Instruction lastBodyLoop) {
		switch (beforeLoop.opcode) {
		case FastConstants.LOAD:
		case FastConstants.STORE:
		case FastConstants.ALOAD:
		case FastConstants.ASTORE:
		case FastConstants.GETSTATIC:
		case FastConstants.PUTSTATIC:
		case FastConstants.GETFIELD:
		case FastConstants.PUTFIELD: {
			switch (lastBodyLoop.opcode) {
			case FastConstants.LOAD:
			case FastConstants.STORE:
			case FastConstants.ALOAD:
			case FastConstants.ASTORE:
			case FastConstants.GETSTATIC:
			case FastConstants.PUTSTATIC:
			case FastConstants.GETFIELD:
			case FastConstants.PUTFIELD: {
				return ((IndexInstruction) beforeLoop).index == ((IndexInstruction) lastBodyLoop).index;
			}
			}
		}
			break;
		case FastConstants.ISTORE: {
			if ((beforeLoop.opcode == lastBodyLoop.opcode) || (lastBodyLoop.opcode == FastConstants.IINC)) {
				return ((IndexInstruction) beforeLoop).index == ((IndexInstruction) lastBodyLoop).index;
			}
		}
			break;
		}

		return false;
	}

	/*
	 * debut de liste fin de liste | gotoIndex | | | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
	 * beforeLoopEntryOffset afterLoopOffset
	 */
	private static int AnalyzeBackGoto(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeListOffset, int afterSubListOffset,
			int returnOffset, int jumpInstructionIndex, Instruction jumpInstruction, int firstOffset) {
		List<Instruction> subList = new ArrayList<Instruction>();
		int index = jumpInstructionIndex - 1;

		switch (jumpInstruction.opcode) {
		case FastConstants.TRY:
		case FastConstants.SYNCHRONIZED:
			subList.add(list.get(jumpInstructionIndex));
			list.set(jumpInstructionIndex, null);
		}

		while ((index >= 0) && (list.get(index).offset >= firstOffset))
			subList.add(list.remove(index--));

		int subListLength = subList.size();

		if (subListLength > 0) 
		{
			Instruction beforeLoop = (index >= 0) ? list.get(index) : null;
			if (beforeLoop != null)
				beforeListOffset = beforeLoop.offset;
			Instruction instruction = subList.get(subListLength - 1);

			// Search escape offset
			int breakOffset = SearchMinusJumpOffset(subList, 0, subListLength, beforeListOffset, jumpInstruction.offset);

			// Search test instruction
			BranchInstruction test = null;

			switch (instruction.opcode) {
			case FastConstants.IF:
			case FastConstants.IFCMP:
			case FastConstants.IFXNULL:
			case FastConstants.COMPLEXIF:
				BranchInstruction bi = (BranchInstruction) instruction;
				if (bi.GetJumpOffset() == breakOffset)
					test = bi;
				break;
			}

			Instruction lastBodyLoop = null;
			Instruction beforeLastBodyLoop = null;

			if (subListLength > 0) {
				lastBodyLoop = subList.get(0);

				if (lastBodyLoop == test) {
					lastBodyLoop = null;
				} else if (subListLength > 1) {
					beforeLastBodyLoop = subList.get(1);
					if (beforeLastBodyLoop == test)
						beforeLastBodyLoop = null;

					// V�rification qu'aucune instruction ne saute entre
					// 'lastBodyLoop' et 'jumpInstruction'
					if (!InstructionUtil.CheckNoJumpToInterval(subList, 0, subListLength, lastBodyLoop.offset,
							jumpInstruction.offset)) {
						// 'lastBodyLoop' ne peut pas etre l'instruction
						// d'incrementation d'une boucle 'for'
						lastBodyLoop = null;
						beforeLastBodyLoop = null;
					} else if (!InstructionUtil.CheckNoJumpToInterval(subList, 0, subListLength, beforeListOffset,
							firstOffset)) {
						// 'lastBodyLoop' ne peut pas etre l'instruction
						// d'incrementation d'une boucle 'for'
						lastBodyLoop = null;
						beforeLastBodyLoop = null;
					}
				}
			}

			int typeLoop = GetLoopType(beforeLoop, test, beforeLastBodyLoop, lastBodyLoop);

			switch (typeLoop) {
			case 0: // for (;;)
			{
				Collections.reverse(subList);
				Instruction firstBodyLoop = subList.get(0);

				AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeListOffset,
						firstBodyLoop.offset, afterSubListOffset, beforeListOffset, afterSubListOffset, breakOffset,
						returnOffset);

				int branch = 1;
				if (breakOffset != -1)
					branch = breakOffset - jumpInstruction.offset;

				list.set(++index, new FastList(FastConstants.INFINITE_LOOP, jumpInstruction.offset,
						Instruction.UNKNOWN_LINE_NUMBER, branch, subList));
			}
				break;
			case 1: // for (beforeLoop; ;)
			{
				Collections.reverse(subList);
				Instruction firstBodyLoop = subList.get(0);

				AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoop.offset,
						firstBodyLoop.offset, afterSubListOffset, beforeListOffset, afterSubListOffset, breakOffset,
						returnOffset);

				int branch = 1;
				if (breakOffset != -1)
					branch = breakOffset - jumpInstruction.offset;

				list.set(++index, new FastList(FastConstants.INFINITE_LOOP, jumpInstruction.offset,
						Instruction.UNKNOWN_LINE_NUMBER, branch, subList));
			}
				break;
			case 2: // while (test)
			{
				// Remove test
				subList.remove(--subListLength);

				if (subListLength > 0) {
					Collections.reverse(subList);

					int beforeTestOffset;

					if (beforeLoop == null)
						beforeTestOffset = (beforeListOffset == -1) ? -1 : beforeListOffset;
					else
						beforeTestOffset = beforeLoop.offset;

					AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeTestOffset,
							test.offset, afterSubListOffset, test.offset, afterSubListOffset, breakOffset, returnOffset);
				}

				int branch = 1;
				if (breakOffset != -1)
					branch = breakOffset - jumpInstruction.offset;

				// 'while'
				ComparisonInstructionAnalyzer.InverseComparison(test);
				list.set(++index, new FastTestList(FastConstants.WHILE, jumpInstruction.offset, test.lineNumber,
						branch, test, subList));
			}
				break;
			case 3: // for (beforeLoop; test;)
			{
				// Remove initialisation instruction before sublist
				list.remove(index);
				// Remove test
				subList.remove(--subListLength);

				if (subListLength > 0) {
					lastBodyLoop = subList.get(0);
					Collections.reverse(subList);

					AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoop.offset,
							test.offset, afterSubListOffset, test.offset, afterSubListOffset, breakOffset, returnOffset);
				}

				ComparisonInstructionAnalyzer.InverseComparison(test);
				CreateForLoopCase1(classFile, method, list, index, beforeLoop, test, subList, breakOffset);
			}
				break;
			case 4: // for (; ; lastBodyLoop)
			{
				Collections.reverse(subList);
				// Remove incrementation instruction
				subList.remove(--subListLength);

				if (subListLength > 0) {
					beforeLastBodyLoop = subList.get(--subListLength);

					AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLastBodyLoop.offset,
							lastBodyLoop.offset, lastBodyLoop.offset, beforeListOffset, afterSubListOffset,
							breakOffset, returnOffset);
				}

				int branch = 1;
				if (breakOffset != -1)
					branch = breakOffset - jumpInstruction.offset;

				list.set(++index, new FastFor(FastConstants.FOR, jumpInstruction.offset, lastBodyLoop.lineNumber,
						branch, null, null, lastBodyLoop, subList));
			}
				break;
			case 5: // for (beforeLoop; ; lastBodyLoop)
				// Remove initialisation instruction before sublist
				list.remove(index);

				Collections.reverse(subList);
				// Remove incrementation instruction
				subList.remove(--subListLength);

				if (subListLength > 0) {
					beforeLastBodyLoop = subList.get(--subListLength);

					AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLastBodyLoop.offset,
							lastBodyLoop.offset, lastBodyLoop.offset, beforeListOffset, afterSubListOffset,
							breakOffset, returnOffset);
				}

				int branch = 1;
				if (breakOffset != -1)
					branch = breakOffset - jumpInstruction.offset;

				list.set(index, new FastFor(FastConstants.FOR, jumpInstruction.offset, lastBodyLoop.lineNumber, branch,
						beforeLoop, null, lastBodyLoop, subList));
				break;
			case 6: // for (; test; lastBodyLoop)
				// Remove test
				subList.remove(--subListLength);

				if (subListLength > 1) {
					Collections.reverse(subList);
					// Remove incrementation instruction
					subList.remove(--subListLength);

					if (subListLength > 0) {
						beforeLastBodyLoop = subList.get(subListLength - 1);

						AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet,
								beforeLastBodyLoop.offset, lastBodyLoop.offset, lastBodyLoop.offset, test.offset,
								afterSubListOffset, breakOffset, returnOffset);
					}

					branch = 1;
					if (breakOffset != -1)
						branch = breakOffset - jumpInstruction.offset;

					ComparisonInstructionAnalyzer.InverseComparison(test);
					list.set(++index, new FastFor(FastConstants.FOR, jumpInstruction.offset, lastBodyLoop.lineNumber,
							branch, null, test, lastBodyLoop, subList));
				} else {
					if (subListLength == 1) {
						int beforeTestOffset;

						if (beforeLoop == null)
							beforeTestOffset = (beforeListOffset == -1) ? -1 : beforeListOffset;
						else
							beforeTestOffset = beforeLoop.offset;

						AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeTestOffset,
								test.offset, lastBodyLoop.offset, test.offset, afterSubListOffset, breakOffset,
								returnOffset);
					}

					branch = 1;
					if (breakOffset != -1)
						branch = breakOffset - jumpInstruction.offset;

					// 'while'
					ComparisonInstructionAnalyzer.InverseComparison(test);
					list.set(++index, new FastTestList(FastConstants.WHILE, jumpInstruction.offset, test.lineNumber,
							branch, test, subList));
				}
				break;
			case 7: // for (beforeLoop; test; lastBodyLoop)
				// Remove initialisation instruction before sublist
				list.remove(index);
				// Remove test
				subList.remove(--subListLength);

				Collections.reverse(subList);
				// Remove incrementation instruction
				subList.remove(--subListLength);

				if (subListLength > 0) {
					beforeLastBodyLoop = subList.get(subListLength - 1);

					AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLastBodyLoop.offset,
							lastBodyLoop.offset, lastBodyLoop.offset, test.offset, afterSubListOffset, breakOffset,
							returnOffset);
				}

				ComparisonInstructionAnalyzer.InverseComparison(test);
				index = CreateForLoopCase3(classFile, method, list, index, beforeLoop, test, lastBodyLoop, subList,
						breakOffset);
				break;
			}
		} else {
			// Empty infinite loop
			list.set(++index, new FastList(FastConstants.INFINITE_LOOP, jumpInstruction.offset,
					Instruction.UNKNOWN_LINE_NUMBER, 0, subList));
		}

		return index;
	}

	/*
	 * debut de liste fin de liste | testIndex | | | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
	 * beforeLoopEntryOffset afterLoopOffset
	 */
	private static void AnalyzeIfAndIfElse(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
			int afterBodyLoopOffset, int afterListOffset, int breakOffset, int returnOffset, int testIndex,
			ConditionalBranchInstruction test) {
		int length = list.size();

		if (length == 0)
			return;

		int elseOffset = test.GetJumpOffset();
		// if (elseOffset == breakOffset) NE PLUS PRODUIRE D'INSTRUCTIONS
		// IF_CONTINUE ET IF_BREAK
		// return;

		if ((test.branch < 0) && 
			(beforeLoopEntryOffset < elseOffset) && 
			(elseOffset <= loopEntryOffset)	&& 
			(afterBodyLoopOffset == afterListOffset)) 
		{
			// L'instruction saute sur un debut de boucle et la liste termine
			// le block de la boucle.
			elseOffset = afterListOffset;
		}

		if ((elseOffset <= test.offset) || 
			((afterListOffset != -1) && (elseOffset > afterListOffset)))
			return;

		// Analyse classique des instructions 'if'
		int index = testIndex + 1;

		if (index < length) {
			// Non empty 'if'. Construct if block instructions
			List<Instruction> subList = new ArrayList<Instruction>();
			length = ExtrackBlock(list, subList, index, length, elseOffset);
			int subListLength = subList.size();

			if (subListLength == 0) {
				// Empty 'if'
				ComparisonInstructionAnalyzer.InverseComparison(test);
				list.set(testIndex, new FastTestList(FastConstants.IF_, test.offset, test.lineNumber, elseOffset
						- test.offset, test, null));
				return;
			}

			int beforeSubListOffset = test.offset;
			Instruction beforeElseBlock = subList.get(subListLength - 1);
			int minusJumpOffset = SearchMinusJumpOffset(
					subList, 0, subListLength, 
					test.offset, beforeElseBlock.offset);
			int lastListOffset = list.get(length - 1).offset;

			// TODO le bloc suivant est a revoir : produit une erreur dans
			// com.adventnet.snmp.ui.SasFileDialog:void
			// mouseDoubleClicked(MouseEvent e)
			if ((minusJumpOffset == -1)
					&& (subListLength > 1)
					&& (beforeElseBlock.opcode == ByteCodeConstants.RETURN)
					&& ((afterListOffset == -1) || (afterListOffset == returnOffset) || 
							ByteCodeUtil.JumpTo(
								method.getCode(),
								ByteCodeUtil.NextInstructionOffset(method.getCode(), lastListOffset), returnOffset))) {
				// Si la derniere instruction est un 'return' et si son
				// numero de ligne est inferieur a l'instruction precedente,
				// il s'agit d'une instruction synthetique ==> if-else
				if (subList.get(subListLength - 2).lineNumber > beforeElseBlock.lineNumber) {
					minusJumpOffset = (returnOffset == -1) ? lastListOffset + 1 : returnOffset;
				}
				// Sinon, si l'instruction 'return' a un numero de ligne
				// superieure au numero de ligne de l'instruction suivante,
				// elle est prise en compte aussi
				else if ((index < length) && (list.get(index).lineNumber < beforeElseBlock.lineNumber)) {
					minusJumpOffset = (returnOffset == -1) ? lastListOffset + 1 : returnOffset;
				}
			}

			if (minusJumpOffset != -1) 
			{
				if ((subListLength == 1) && 
					(beforeElseBlock.opcode == FastConstants.GOTO)) 
				{
					// Instruction 'if' suivi d'un bloc contenant un seul 'goto'
					// ==> Generation d'une instrcution 'break' ou 'continue'
					CreateBreakAndContinue(method, subList, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
							afterBodyLoopOffset, afterListOffset, breakOffset, returnOffset);

					ComparisonInstructionAnalyzer.InverseComparison(test);
					list.set(testIndex, new FastTestList(FastConstants.IF_, beforeElseBlock.offset, test.lineNumber,
							elseOffset - beforeElseBlock.offset, test, subList));
					return;
				}

				int afterIfElseOffset;

				if ((minusJumpOffset < test.offset) && 
					(beforeLoopEntryOffset < minusJumpOffset) && 
					(minusJumpOffset <= loopEntryOffset))
				{
					// Jump to loop entry ==> continue
					int positiveJumpOffset = SearchMinusJumpOffset(
							subList, 0, subListLength, -1, beforeElseBlock.offset);
					
					// S'il n'y a pas de saut positif ou si le saut mini positif 
					// est au dela de la fin de la liste (pour sauter vers le 
					// 'return' final par exemple) et si la liste courante termine 
					// la boucle courante
					if (((positiveJumpOffset == -1) || (positiveJumpOffset >= afterListOffset)) && 
						(afterBodyLoopOffset == afterListOffset)) 
					{					
						// Cas des instructions de saut n�gatif dans une boucle qui
						// participent tout de meme � une instruction if-else
						// L'instruction saute sur un debut de boucle et la liste
						// termine le block de la boucle.
						afterIfElseOffset = afterListOffset;
					} else {
						// If-else
						afterIfElseOffset = positiveJumpOffset;
					}
					
				} else {
					// If ou If-else
					afterIfElseOffset = minusJumpOffset;
				}

				if ((afterIfElseOffset > elseOffset)
						&& ((afterListOffset == -1) || (afterIfElseOffset <= afterListOffset) || 
								ByteCodeUtil.JumpTo(
									method.getCode(),
									ByteCodeUtil.NextInstructionOffset(method.getCode(), lastListOffset), afterIfElseOffset))) 
				{
					// If-else or If-elseif-...
					if (((beforeElseBlock.opcode == FastConstants.GOTO) && (((Goto) beforeElseBlock).GetJumpOffset() == minusJumpOffset))
							|| (beforeElseBlock.opcode == ByteCodeConstants.RETURN)) {
						// Remove 'goto' or 'return'
						subList.remove(subListLength - 1);
					}

					// Construct else block instructions
					List<Instruction> subElseList = new ArrayList<Instruction>();
					length = ExtrackBlock(list, subElseList, index, length, afterIfElseOffset);

					if (subElseList.size() > 0) {
						AnalyzeList(
							classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
							loopEntryOffset, afterBodyLoopOffset, beforeSubListOffset, afterIfElseOffset,
							breakOffset, returnOffset);

						beforeSubListOffset = beforeElseBlock.offset;

						AnalyzeList(classFile, method, subElseList, localVariables, offsetLabelSet,
							beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, beforeSubListOffset,
							afterIfElseOffset, breakOffset, returnOffset);

						int subElseListLength = subElseList.size();
						int lastIfElseOffset = (subElseListLength > 0) ? 
								subElseList.get(subElseListLength - 1).offset :
								beforeSubListOffset;

						ComparisonInstructionAnalyzer.InverseComparison(test);
						list.set(testIndex, new FastTest2Lists(
							FastConstants.IF_ELSE, lastIfElseOffset, test.lineNumber, 
							afterIfElseOffset - lastIfElseOffset, test, subList, subElseList));
						return;
					}
				}
			}

			// Simple 'if'
			AnalyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
					loopEntryOffset, afterBodyLoopOffset, beforeSubListOffset, elseOffset, breakOffset, returnOffset);

			ComparisonInstructionAnalyzer.InverseComparison(test);
			list.set(testIndex, new FastTestList(
					FastConstants.IF_, beforeElseBlock.offset, test.lineNumber, 
					elseOffset - beforeElseBlock.offset, test, subList));
		} else if (elseOffset == breakOffset) {
			// If-break
			list.set(testIndex, new FastInstruction(FastConstants.IF_BREAK, test.offset, test.lineNumber, test));
		} else {
			// Empty 'if'
			list.set(testIndex, new FastTestList(FastConstants.IF_, test.offset, test.lineNumber, elseOffset
					- test.offset, test, null));
		}
	}

	private static int ExtrackBlock(
			List<Instruction> list, List<Instruction> subList, 
			int index, int length, int endOffset) 
	{
		while ((index < length) && (list.get(index).offset < endOffset)) {
			subList.add(list.remove(index));
			length--;
		}

		return length;
	}

	/*
	 * debut de liste fin de liste | switchIndex | | | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
	 * beforeLoopEntryOffset afterLoopOffset
	 */
	private static void AnalyzeLookupSwitch(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
			int afterBodyLoopOffset, int afterListOffset, int returnOffset, int switchIndex, LookupSwitch ls) {
		int pairLength = ls.keys.length;
		FastSwitch.Pair[] pairs = new FastSwitch.Pair[pairLength + 1];

		// Construct list of pairs
		boolean defaultFlag = true;
		int pairIndex = 0;
		for (int i = 0; i < pairLength; i++) {
			if (defaultFlag && (ls.offsets[i] > ls.defaultOffset)) {
				pairs[pairIndex++] = new FastSwitch.Pair(true, 0, ls.offset + ls.defaultOffset);
				defaultFlag = false;
			}

			pairs[pairIndex++] = new FastSwitch.Pair(false, ls.keys[i], ls.offset + ls.offsets[i]);
		}

		if (defaultFlag)
			pairs[pairIndex++] = new FastSwitch.Pair(true, 0, ls.offset + ls.defaultOffset);

		// SWITCH or SWITCH_ENUM ?
		int switchOpcode = AnalyzeSwitchType(classFile, ls.key);

		// SWITCH or Eclipse SWITCH_STRING ?
		if ((classFile.getMajorVersion() >= 51) && (switchOpcode == FastConstants.SWITCH)
				&& (ls.key.opcode == ByteCodeConstants.ILOAD) && (switchIndex > 2)) {
			if (AnalyzeSwitchString(classFile, localVariables, list, switchIndex, ls, pairs)) {
				// Switch+String found.
				// Remove FastSwitch
				list.remove(--switchIndex);
				// Remove IStore
				list.remove(--switchIndex);
				// Remove AStore
				list.remove(--switchIndex);
				// Change opcode
				switchOpcode = FastConstants.SWITCH_STRING;
			}
		}

		AnalyzeSwitch(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
				afterBodyLoopOffset, afterListOffset, returnOffset, switchIndex, switchOpcode, ls.offset,
				ls.lineNumber, ls.key, pairs, pairLength);
	}

	private static int AnalyzeSwitchType(ClassFile classFile, Instruction i) 
	{
		if (i.opcode == ByteCodeConstants.ARRAYLOAD)
		{
			// switch(1.$SwitchMap$basic$data$TestEnum$enum1[e.ordinal()]) ?
			// switch(1.$SwitchMap$basic$data$TestEnum$enum1[request.getOperationType().ordinal()]) ?		
			ArrayLoadInstruction ali = (ArrayLoadInstruction)i;
			
			if (ali.indexref.opcode == ByteCodeConstants.INVOKEVIRTUAL) 
			{		
				if (ali.arrayref.opcode == ByteCodeConstants.GETSTATIC) 
				{
					GetStatic gs = (GetStatic) ali.arrayref;
	
					ConstantPool constants = classFile.getConstantPool();
					ConstantFieldref cfr = constants.getConstantFieldref(gs.index);
					ConstantNameAndType cnat = constants.getConstantNameAndType(cfr.name_and_type_index);
	
					if (classFile.getSwitchMaps().containsKey(cnat.name_index)) {
						Invokevirtual iv = (Invokevirtual) ali.indexref;
	
						if (iv.args.size() == 0) {
							ConstantMethodref cmr = constants.getConstantMethodref(iv.index);
							cnat = constants.getConstantNameAndType(cmr.name_and_type_index);
							
							if (StringConstants.ORDINAL_METHOD_NAME.equals(constants.getConstantUtf8(cnat.name_index))) {								
								// SWITCH_ENUM found
								return FastConstants.SWITCH_ENUM;
							}
						}
					}
				} 
				else if (ali.arrayref.opcode == ByteCodeConstants.INVOKESTATIC) 
				{
					Invokestatic is = (Invokestatic) ali.arrayref;
	
					if (is.args.size() == 0) {
						ConstantPool constants = classFile.getConstantPool();
						ConstantMethodref cmr = constants.getConstantMethodref(is.index);
						
						if (cmr.class_index == classFile.getThisClassIndex()) {
							ConstantNameAndType cnat = constants.getConstantNameAndType(cmr.name_and_type_index);
							if (classFile.getSwitchMaps().containsKey(cnat.name_index)) {
								Invokevirtual iv = (Invokevirtual) ali.indexref;
	
								if (iv.args.size() == 0) {
									cmr = constants.getConstantMethodref(iv.index);
									cnat = constants.getConstantNameAndType(cmr.name_and_type_index);
									
									if (StringConstants.ORDINAL_METHOD_NAME.equals(constants.getConstantUtf8(cnat.name_index))) {								
										// Eclipse SWITCH_ENUM found
										return FastConstants.SWITCH_ENUM;
									}
								}
							}
						}
					}
				}
			}
		}
		
		return FastConstants.SWITCH;
	}
	
	/*
	 * debut de liste fin de liste | switchIndex | | | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
	 * beforeLoopEntryOffset afterLoopOffset
	 */
	private static int AnalyzeTableSwitch(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
			int afterBodyLoopOffset, int afterListOffset, int returnOffset, int switchIndex, TableSwitch ts) {
		int pairLength = ts.offsets.length;
		FastSwitch.Pair[] pairs = new FastSwitch.Pair[pairLength + 1];

		// Construct list of pairs
		boolean defaultFlag = true;
		int pairIndex = 0;
		for (int i = 0; i < pairLength; i++) {
			if (defaultFlag && (ts.offsets[i] > ts.defaultOffset)) {
				pairs[pairIndex++] = new FastSwitch.Pair(true, 0, ts.offset + ts.defaultOffset);
				defaultFlag = false;
			}

			pairs[pairIndex++] = new FastSwitch.Pair(false, ts.low + i, ts.offset + ts.offsets[i]);
		}

		if (defaultFlag)
			pairs[pairIndex++] = new FastSwitch.Pair(true, 0, ts.offset + ts.defaultOffset);

		// SWITCH or Eclipse SWITCH_ENUM ?
		int switchOpcode = AnalyzeSwitchType(classFile, ts.key);

		// SWITCH or Eclipse SWITCH_STRING ?
		if ((classFile.getMajorVersion() >= 51) && (switchOpcode == FastConstants.SWITCH)
				&& (ts.key.opcode == ByteCodeConstants.ILOAD) && (switchIndex > 2)) {
			if (AnalyzeSwitchString(classFile, localVariables, list, switchIndex, ts, pairs)) {
				// Switch+String found.
				// Remove FastSwitch
				list.remove(--switchIndex);
				// Remove IStore
				list.remove(--switchIndex);
				// Remove AStore
				list.remove(--switchIndex);
				// Change opcode
				switchOpcode = FastConstants.SWITCH_STRING;
			}
		}

		AnalyzeSwitch(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
				afterBodyLoopOffset, afterListOffset, returnOffset, switchIndex, switchOpcode, ts.offset,
				ts.lineNumber, ts.key, pairs, pairLength);

		return switchIndex;
	}

	private static boolean AnalyzeSwitchString(ClassFile classFile, LocalVariables localVariables,
			List<Instruction> list, int switchIndex, Switch s, FastSwitch.Pair[] pairs) {
		Instruction instruction = list.get(switchIndex - 3);
		if ((instruction.opcode != FastConstants.ASTORE) || (instruction.lineNumber != s.key.lineNumber))
			return false;
		AStore astore = (AStore) instruction;

		instruction = list.get(switchIndex - 2);
		if ((instruction.opcode != FastConstants.ISTORE) || (instruction.lineNumber != astore.lineNumber))
			return false;

		instruction = list.get(switchIndex - 1);
		if ((instruction.opcode != FastConstants.SWITCH) || (instruction.lineNumber != astore.lineNumber))
			return false;

		FastSwitch previousSwitch = (FastSwitch) instruction;
		if (previousSwitch.test.opcode != FastConstants.INVOKEVIRTUAL)
			return false;

		Invokevirtual iv = (Invokevirtual) previousSwitch.test;

		if ((iv.objectref.opcode != FastConstants.ALOAD) || (iv.args.size() != 0))
			return false;

		ConstantPool constants = classFile.getConstantPool();
		ConstantMethodref cmr = constants.getConstantMethodref(iv.index);

		if (!cmr.getReturnedSignature().equals("I"))
			return false;

		String className = constants.getConstantClassName(cmr.class_index);
		if (!className.equals("java/lang/String"))
			return false;

		ConstantNameAndType cnat = constants.getConstantNameAndType(cmr.name_and_type_index);
		String descriptorName = constants.getConstantUtf8(cnat.descriptor_index);
		if (!descriptorName.equals("()I"))
			return false;

		String methodName = constants.getConstantUtf8(cnat.name_index);
		if (!methodName.equals("hashCode"))
			return false;

		Pair[] previousPairs = previousSwitch.pairs;
		int i = previousPairs.length;
		if (i == 0)
			return false;

		int tsKeyIloadIndex = ((ILoad) s.key).index;
		int previousSwitchAloadIndex = ((ALoad) iv.objectref).index;
		HashMap<Integer, Integer> stringIndexes = new HashMap<Integer, Integer>();

		while (i-- > 0) {
			Pair pair = previousPairs[i];
			if (pair.isDefault())
				continue;

			List<Instruction> instructions = pair.getInstructions();

			for (;;) {
				int length = instructions.size();
				if (length == 0)
					return false;

				instruction = instructions.get(0);

				/*
				 * if (instruction.opcode == FastConstants.IF_BREAK) { if
				 * (((length == 2) || (length == 3)) &&
				 * (AnalyzeSwitchStringTestInstructions( constants, cmr,
				 * tsKeyIloadIndex, previousSwitchAloadIndex, stringIndexes,
				 * ((FastInstruction)instruction).instruction,
				 * instructions.get(1), FastConstants.CMP_EQ))) { break; } else
				 * { return false; } } else
				 */if (instruction.opcode == FastConstants.IF_) {
					switch (length) {
					case 1:
						break;
					case 2:
						if (instructions.get(1).opcode == FastConstants.GOTO_BREAK)
							break;
					default:
						return false;
					}
					FastTestList ftl = (FastTestList) instruction;
					if ((ftl.instructions.size() == 1)
							&& AnalyzeSwitchStringTestInstructions(constants, cmr, tsKeyIloadIndex,
									previousSwitchAloadIndex, stringIndexes, ftl.test, ftl.instructions.get(0),
									FastConstants.CMP_NE)) {
						break;
					} else {
						return false;
					}
				} else if (instruction.opcode == FastConstants.IF_ELSE) {
					if (length != 1)
						return false;
					FastTest2Lists ft2l = (FastTest2Lists) instruction;
					if ((ft2l.instructions.size() == 1)
							&& AnalyzeSwitchStringTestInstructions(constants, cmr, tsKeyIloadIndex,
									previousSwitchAloadIndex, stringIndexes, ft2l.test, ft2l.instructions.get(0),
									FastConstants.CMP_NE)) {
						instructions = ft2l.instructions2;
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		}

		// First switch instruction for Switch+String found
		// Replace value of each pair
		i = pairs.length;

		while (i-- > 0) {
			Pair pair = pairs[i];
			if (pair.isDefault())
				continue;
			pair.setKey(stringIndexes.get(pair.getKey()));
		}

		// Remove synthetic local variable integer
		localVariables.removeLocalVariableWithIndexAndOffset(tsKeyIloadIndex, s.key.offset);
		// Remove synthetic local variable string
		localVariables.removeLocalVariableWithIndexAndOffset(astore.index, astore.offset);
		// Replace switch test
		s.key = astore.valueref;

		return true;
	}

	private static boolean AnalyzeSwitchStringTestInstructions(ConstantPool constants, ConstantMethodref cmr,
			int tsKeyIloadIndex, int previousSwitchAloadIndex, HashMap<Integer, Integer> stringIndexes,
			Instruction test, Instruction value, int cmp) {
		if (test.opcode != FastConstants.IF)
			return false;

		if (value.opcode != FastConstants.ISTORE)
			return false;

		IStore istore = (IStore) value;
		if (istore.index != tsKeyIloadIndex)
			return false;

		int opcode = istore.valueref.opcode;
		int index;

		if (opcode == FastConstants.BIPUSH)
			index = ((BIPush) istore.valueref).value;
		else if (opcode == FastConstants.ICONST)
			index = ((IConst) istore.valueref).value;
		else
			return false;

		IfInstruction ii = (IfInstruction) test;
		if ((ii.cmp != cmp) || (ii.value.opcode != FastConstants.INVOKEVIRTUAL))
			return false;

		Invokevirtual ivTest = (Invokevirtual) ii.value;

		if ((ivTest.args.size() != 1) || (ivTest.objectref.opcode != FastConstants.ALOAD)
				|| (((ALoad) ivTest.objectref).index != previousSwitchAloadIndex)
				|| (ivTest.args.get(0).opcode != FastConstants.LDC))
			return false;

		ConstantMethodref cmrTest = constants.getConstantMethodref(ivTest.index);
		if (cmr.class_index != cmrTest.class_index)
			return false;

		ConstantNameAndType cnatTest = constants.getConstantNameAndType(cmrTest.name_and_type_index);
		String descriptorNameTest = constants.getConstantUtf8(cnatTest.descriptor_index);
		if (!descriptorNameTest.equals("(Ljava/lang/Object;)Z"))
			return false;

		String methodNameTest = constants.getConstantUtf8(cnatTest.name_index);
		if (!methodNameTest.equals("equals"))
			return false;

		stringIndexes.put(index, Integer.valueOf(((Ldc) ivTest.args.get(0)).index));

		return true;
	}

	/*
	 * debut de liste fin de liste | switchIndex | | | | Liste ...
	 * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
	 * | beforeListOff. | | | Offsets | loopEntryOffset switchOffset
	 * endLoopOffset | beforeLoopEntryOffset afterLoopOffset
	 */
	private static void AnalyzeSwitch(ClassFile classFile, Method method, List<Instruction> list,
			LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
			int afterBodyLoopOffset, int afterListOffset, int returnOffset, int switchIndex, int switchOpcode,
			int switchOffset, int switchLineNumber, Instruction test, FastSwitch.Pair[] pairs, int pairLength) {
		int breakOffset = -1;

		// Order pairs by offset
		Arrays.sort(pairs);

		// Extract list of instructions for all pairs
		int lastSwitchOffset = switchOffset;
		int index = switchIndex + 1;

		if (index < list.size()) {
			// Switch non vide ou non en derniere position dans la serie
			// d'instructions
			for (int i = 0; i < pairLength; i++) 
			{
				ArrayList<Instruction> instructions = null;
				int beforeCaseOffset = lastSwitchOffset;
				int afterCaseOffset = pairs[i + 1].getOffset();

				while (index < list.size()) 
				{
					Instruction instruction = list.get(index);
					if (instruction.offset >= afterCaseOffset) 
					{
						if (instructions != null) 
						{
							int nbrInstrucrions = instructions.size();
							if (nbrInstrucrions > 0)
							{
								// Recherche de 'breakOffset'
								int breakOffsetTmp = SearchMinusJumpOffset(instructions, 0, nbrInstrucrions,
										beforeCaseOffset, lastSwitchOffset);
								if (breakOffsetTmp != -1) {
									if ((breakOffset == -1) || (breakOffset > breakOffsetTmp))
										breakOffset = breakOffsetTmp;
								}

								// Remplacement du dernier 'goto'
								instruction = instructions.get(nbrInstrucrions - 1);
								if (instruction.opcode == FastConstants.GOTO) {
									int lineNumber = instruction.lineNumber;
									
									if ((nbrInstrucrions <= 1) || (instructions.get(nbrInstrucrions-2).lineNumber == lineNumber))
										lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
									
									// Replace goto by break;
									instructions.set(nbrInstrucrions - 1, new FastInstruction(FastConstants.GOTO_BREAK,
											instruction.offset, lineNumber, null));
								}
							}
						}
						break;
					}

					if (instructions == null)
						instructions = new ArrayList<Instruction>();

					list.remove(index);
					instructions.add(instruction);
					lastSwitchOffset = instruction.offset;
				}

				pairs[i].setInstructions(instructions);
			}

			// Extract last block
			if (breakOffset != -1) {
				int afterSwitchOffset = (breakOffset >= switchOffset) ? breakOffset
						: list.get(list.size() - 1).offset + 1;

				// Reduction de 'afterSwitchOffset' via les 'Branch
				// Instructions'
				Instruction instruction;
				// Check previous instructions
				int i = switchIndex;
				while (i-- > 0) {
					instruction = list.get(i);

					switch (instruction.opcode) {
					case ByteCodeConstants.IF:
					case ByteCodeConstants.IFCMP:
					case ByteCodeConstants.IFXNULL:
					case ByteCodeConstants.GOTO:
					case FastConstants.SWITCH:
					case FastConstants.SWITCH_ENUM:
					case FastConstants.SWITCH_STRING:
						int jumpOffset = ((BranchInstruction) instruction).GetJumpOffset();
						if ((lastSwitchOffset < jumpOffset) && (jumpOffset < afterSwitchOffset))
							afterSwitchOffset = jumpOffset;
					}
				}
				// Check next instructions
				i = list.size();
				while (i-- > 0) {
					instruction = list.get(i);

					switch (instruction.opcode) {
					case ByteCodeConstants.IF:
					case ByteCodeConstants.IFCMP:
					case ByteCodeConstants.IFXNULL:
					case ByteCodeConstants.GOTO:
					case FastConstants.SWITCH:
					case FastConstants.SWITCH_ENUM:
					case FastConstants.SWITCH_STRING:
						int jumpOffset = ((BranchInstruction) instruction).GetJumpOffset();
						if ((lastSwitchOffset < jumpOffset) && (jumpOffset < afterSwitchOffset))
							afterSwitchOffset = jumpOffset;
					}

					if ((instruction.offset <= afterSwitchOffset) || (instruction.offset <= lastSwitchOffset))
						break;
				}

				// Extraction
				List<Instruction> instructions = null;

				while (index < list.size()) 
				{
					instruction = list.get(index);
					if (instruction.offset >= afterSwitchOffset) 
					{
						if (instructions != null) 
						{
							int nbrInstrucrions = instructions.size();
							if (nbrInstrucrions > 0) 
							{
								instruction = instructions.get(nbrInstrucrions - 1);
								if (instruction.opcode == FastConstants.GOTO) {
									int lineNumber = instruction.lineNumber;
									
									if ((nbrInstrucrions <= 1) || (instructions.get(nbrInstrucrions-2).lineNumber == lineNumber))
										lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
									
									// Replace goto by break;
									instructions.set(nbrInstrucrions - 1, new FastInstruction(FastConstants.GOTO_BREAK,
											instruction.offset, instruction.lineNumber, null));
								}
							}
						}
						break;
					}

					if (instructions == null)
						instructions = new ArrayList<Instruction>();

					list.remove(index);
					instructions.add(instruction);
					lastSwitchOffset = instruction.offset;
				}

				pairs[pairLength].setInstructions(instructions);
			}

			// Analyze instructions (recursive analyze)
			int beforeListOffset = test.offset;
			if (index < list.size())
				afterListOffset = list.get(index).offset;

			for (int i = 0; i <= pairLength; i++) 
			{
				FastSwitch.Pair pair = pairs[i];
				List<Instruction> instructions = pair.getInstructions();
				if (instructions != null) 
				{
					int nbrInstrucrions = instructions.size();
					if (nbrInstrucrions > 0) 
					{
						Instruction instruction = instructions.get(nbrInstrucrions - 1);
						if (instruction.opcode == FastConstants.GOTO_BREAK) 
						{
							instructions.remove(nbrInstrucrions - 1);
							AnalyzeList(classFile, method, instructions, localVariables, offsetLabelSet,
									beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, beforeListOffset,
									afterListOffset, breakOffset, returnOffset);
							instructions.add(instruction);
						} 
						else 
						{
							AnalyzeList(classFile, method, instructions, localVariables, offsetLabelSet,
									beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, beforeListOffset,
									afterListOffset, breakOffset, returnOffset);

							nbrInstrucrions = instructions.size();
							if (nbrInstrucrions > 0) 
							{
								instruction = instructions.get(nbrInstrucrions - 1);

								switch (instruction.opcode) 
								{
								case ByteCodeConstants.IF:
								case ByteCodeConstants.IFCMP:
								case ByteCodeConstants.IFXNULL:
								case FastConstants.IF_:
								case FastConstants.IF_ELSE:
								case ByteCodeConstants.GOTO:
								case FastConstants.SWITCH:
								case FastConstants.SWITCH_ENUM:
								case FastConstants.SWITCH_STRING:
									int jumpOffset = ((BranchInstruction) instruction).GetJumpOffset();
									if ((jumpOffset < switchOffset) || (lastSwitchOffset < jumpOffset)) {
										instructions.add(new FastInstruction(FastConstants.GOTO_BREAK,
												lastSwitchOffset + 1, Instruction.UNKNOWN_LINE_NUMBER, null));
									}
									break;
								}
							}
						}
						beforeListOffset = instruction.offset;
					}
				}
			}
		}

		// Create instruction
		int branch = (breakOffset == -1) ? 1 : (breakOffset - lastSwitchOffset);

		list.set(switchIndex, new FastSwitch(switchOpcode, lastSwitchOffset, switchLineNumber, branch, test, pairs));
	}

	private static void AddLabels(List<Instruction> list, IntSet offsetLabelSet) {
		for (int i = offsetLabelSet.size() - 1; i >= 0; --i) {
			SearchInstructionAndAddLabel(list, offsetLabelSet.get(i));
		}
	}

	/**
	 * @param list
	 * @param labelOffset
	 * @return false si aucune instruction ne correspond et true sinon.
	 */
	private static boolean SearchInstructionAndAddLabel(List<Instruction> list, int labelOffset) {
		int index = InstructionUtil.getIndexForOffset(list, labelOffset);

		if (index < 0)
			return false;

		boolean found = false;
		Instruction instruction = list.get(index);

		switch (instruction.opcode) {
		case FastConstants.INFINITE_LOOP: {
			List<Instruction> instructions = ((FastList) instruction).instructions;
			if (instructions != null)
				found = SearchInstructionAndAddLabel(instructions, labelOffset);
		}
			break;
		case FastConstants.WHILE:
		case FastConstants.DO_WHILE:
		case FastConstants.IF_: {
			FastTestList ftl = (FastTestList) instruction;
			if ((labelOffset >= ftl.test.offset) && (ftl.instructions != null))
				found = SearchInstructionAndAddLabel(ftl.instructions, labelOffset);
			}
			break;
		case FastConstants.SYNCHRONIZED: {
			FastSynchronized fs = (FastSynchronized) instruction;
			if ((labelOffset >= fs.monitor.offset) && (fs.instructions != null))
				found = SearchInstructionAndAddLabel(fs.instructions, labelOffset);
		}
			break;
		case FastConstants.FOR: {
			FastFor ff = (FastFor) instruction;
			if (((ff.init == null) || (labelOffset >= ff.init.offset)) && (ff.instructions != null))
				found = SearchInstructionAndAddLabel(ff.instructions, labelOffset);
		}
			break;
		case FastConstants.IF_ELSE: {
			FastTest2Lists ft2l = (FastTest2Lists) instruction;
			if (labelOffset >= ft2l.test.offset)
				found = SearchInstructionAndAddLabel(ft2l.instructions, labelOffset)
						|| SearchInstructionAndAddLabel(ft2l.instructions2, labelOffset);
		}
			break;
		case FastConstants.SWITCH:
		case FastConstants.SWITCH_ENUM:
		case FastConstants.SWITCH_STRING: {
			FastSwitch fs = (FastSwitch) instruction;
			if (labelOffset >= fs.test.offset) {
				FastSwitch.Pair[] pairs = fs.pairs;
				if (pairs != null)
					for (int i = pairs.length - 1; i >= 0 && !found; --i) {
						List<Instruction> instructions = pairs[i].getInstructions();
						if (instructions != null)
							found = SearchInstructionAndAddLabel(instructions, labelOffset);
					}
			}
		}
			break;
		case FastConstants.TRY: {
			FastTry ft = (FastTry) instruction;
			found = SearchInstructionAndAddLabel(ft.instructions, labelOffset);

			if (!found && (ft.catches != null))
				for (int i = ft.catches.size() - 1; i >= 0 && !found; --i) {
					found = SearchInstructionAndAddLabel(ft.catches.get(i).instructions, labelOffset);
				}

			if (!found && (ft.finallyInstructions != null))
				found = SearchInstructionAndAddLabel(ft.finallyInstructions, labelOffset);
		}
		}

		if (!found)
			list.set(index, new FastLabel(FastConstants.LABEL, labelOffset, instruction.lineNumber, instruction));

		return true;
	}
}
