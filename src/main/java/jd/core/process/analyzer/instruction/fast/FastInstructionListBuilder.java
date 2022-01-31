/**
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
 */
package jd.core.process.analyzer.instruction.fast;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.AttributeSignature;
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
import jd.core.process.analyzer.instruction.fast.FastCodeExceptionAnalyzer.FastCodeExcepcion;
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

/**
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
public final class FastInstructionListBuilder {
    private FastInstructionListBuilder() {
    }
    /** Declaration constants. */
    private static final boolean DECLARED = true;
    private static final boolean NOT_DECLARED = false;

    /**
     * début de liste fin de liste | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ...
     */
    public static void build(ReferenceMap referenceMap, ClassFile classFile, Method method, List<Instruction> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        // Agregation des déclarations CodeException
        List<FastCodeExcepcion> lfce = FastCodeExceptionAnalyzer.aggregateCodeExceptions(method, list);

        // Initialyze delaclation flags
        LocalVariables localVariables = method.getLocalVariables();
        if (localVariables != null) {
            initDelcarationFlags(localVariables);
        }

        // Initialisation de l'ensemle des offsets d'etiquette
        IntSet offsetLabelSet = new IntSet();

        // Initialisation de 'returnOffset' ...
        int returnOffset = -1;
        if (!list.isEmpty()) {
            Instruction instruction = list.get(list.size() - 1);
            if (instruction.getOpcode() == Const.RETURN) {
                returnOffset = instruction.getOffset();
            }
        }

        // Recursive call
        if (lfce != null) {
            FastCodeExcepcion fce;
            for (int i = lfce.size() - 1; i >= 0; --i) {
                fce = lfce.get(i);
                if (fce.hasSynchronizedFlag()) {
                    createSynchronizedBlock(referenceMap, classFile, list, localVariables, fce);
                } else {
                    createFastTry(referenceMap, classFile, list, localVariables, fce, returnOffset);
                }
            }
        }

        executeReconstructors(referenceMap, classFile, list, localVariables);

        analyzeList(classFile, method, list, localVariables, offsetLabelSet, -1, -1, -1, -1, -1, -1, returnOffset);

        if (localVariables != null) {
            localVariables.removeUselessLocalVariables();
        }

        manageRedeclaredVariables(list);

        // Add labels
        if (!offsetLabelSet.isEmpty()) {
            addLabels(list, offsetLabelSet);
        }
    }

    private static void manageRedeclaredVariables(List<Instruction> list) {
        manageRedeclaredVariables(new HashSet<>(), new HashSet<>(), list);
    }

    /**
     * Remove re-declarations of unassigned local variables
     * Convert re-declarations of assigned local variables into assignments
     * Attempt to manage a simplified scope of local variables.
     */
    private static void manageRedeclaredVariables(Set<FastDeclaration> outsideDeclarations, Set<FastDeclaration> insideDeclarations, List<Instruction> instructions) {
        Instruction instruction;
        List<List<Instruction>> blocks;
        for (int i = 0; i < instructions.size(); i++) {
            instruction = instructions.get(i);
            if (instruction instanceof FastDeclaration) {// to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                FastDeclaration declaration = (FastDeclaration) instruction;
                if (insideDeclarations.contains(declaration) || outsideDeclarations.contains(declaration)) {
                    if (declaration.getInstruction() == null) {
                        // remove re-declaration if no assignment
                        instructions.remove(i);
                        i--;
                    } else if (declaration.getInstruction() instanceof StoreInstruction) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                        StoreInstruction si = (StoreInstruction) declaration.getInstruction();
                        // if variable is assigned, turn re-declaration into assignment
                        instructions.set(i, si);
                    }
                } else {
                    insideDeclarations.add(declaration);
                }
            }
            blocks = getBlocks(instruction);
            if (!blocks.isEmpty()) {
                /* each block has access to the previously declared local variables :
                 * - outsideDeclarations contains the previously declared local variables from every parent block
                 * - insideDeclarations contains the previously declared variables from the current block
                 * as we enter a new block, the current block becomes the parent block, and the new block
                 * becomes the current block, inheriting from a merged set of variables that contain both inside
                 * declarations from the parent, and outside declarations from all other ancestors,
                 * whilst starting with a brand new set of variables for its own declarations
                 */
                Set<FastDeclaration> mergedDeclarations = mergeSets(outsideDeclarations, insideDeclarations);
                for (List<Instruction> block : blocks) {
                    manageRedeclaredVariables(new HashSet<>(mergedDeclarations), new HashSet<>(), block);
                }
            }
        }
    }

    private static List<List<Instruction>> getBlocks(Instruction instruction) {
        if (instruction instanceof FastTest2Lists) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            FastTest2Lists fastTest2Lists = (FastTest2Lists) instruction;
            return Arrays.asList(fastTest2Lists.getInstructions(), fastTest2Lists.getInstructions2());
        }
        if (instruction instanceof FastTry) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            FastTry fastTry = (FastTry) instruction;
            List<List<Instruction>> instructions = new ArrayList<>();
            instructions.add(fastTry.getInstructions());
            for (FastCatch fastCatch : fastTry.getCatches()) {
                instructions.add(fastCatch.instructions());
            }
            if (fastTry.getFinallyInstructions() != null) {
                instructions.add(fastTry.getFinallyInstructions());
            }
            return instructions;
        }
        return Collections.emptyList();
    }

    private static <T> Set<T> mergeSets(Set<T> a, Set<T> b) {
        return Stream.concat(a.stream(), b.stream()).collect(Collectors.toSet());
    }

    private static void initDelcarationFlags(LocalVariables localVariables) {
        int nbrOfLocalVariables = localVariables.size();
        int indexOfFirstLocalVariable = localVariables.getIndexOfFirstLocalVariable();

        for (int i = 0; i < indexOfFirstLocalVariable && i < nbrOfLocalVariables; ++i) {
            localVariables.getLocalVariableAt(i).setDeclarationFlag(DECLARED);
        }

        LocalVariable lv;
        for (int i = indexOfFirstLocalVariable; i < nbrOfLocalVariables; ++i) {
            lv = localVariables.getLocalVariableAt(i);
            lv.setDeclarationFlag(lv.isExceptionOrReturnAddress() ? DECLARED : NOT_DECLARED);
        }
    }

    private static void createSynchronizedBlock(ReferenceMap referenceMap, ClassFile classFile, List<Instruction> list,
            LocalVariables localVariables, FastCodeExcepcion fce) {
        int index = InstructionUtil.getIndexForOffset(list, fce.getTryFromOffset());
        Instruction instruction = list.get(index);
        int synchronizedBlockJumpOffset = -1;

        if (fce.getType() == FastConstants.TYPE_118_FINALLY) {
            // Retrait de la sous procédure allant de "monitorexit" à  "ret"
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
            // 18: astore 4 <~~~~~ entrée de la sous procecure ('jsr')
            // 20: aload_3
            // 21: monitorexit
            // 22: ret 4 <-----

            // Save 'index'
            int tryFromIndex = index;

            // Search offset of sub procedure entry
            index = InstructionUtil.getIndexForOffset(list, fce.getFinallyFromOffset());
            int subProcedureOffset = list.get(index + 2).getOffset();

            int jumpOffset;
            // Remove 'jsr' instructions
            while (index-- > tryFromIndex) {
                instruction = list.get(index);
                if (instruction.getOpcode() != Const.JSR) {
                    continue;
                }

                jumpOffset = ((Jsr) instruction).getJumpOffset();
                list.remove(index);

                if (jumpOffset == subProcedureOffset) {
                    break;
                }
            }

            // Remove instructions of finally block
            int finallyFromOffset = fce.getFinallyFromOffset();
            index = InstructionUtil.getIndexForOffset(list, fce.getAfterOffset());
            if (index == -1) {
                index = list.size() - 1;
                while (list.get(index).getOffset() >= finallyFromOffset) {
                    list.remove(index);
                    index--;
                }
            } else if (index > 0) {
                index--;
                while (list.get(index).getOffset() >= finallyFromOffset) {
                    list.remove(index);
                    index--;
                }
            }

            // Extract try blocks
            List<Instruction> instructions = new ArrayList<>();
            if (index > 0) {
                int tryFromOffset = fce.getTryFromOffset();

                while (list.get(index).getOffset() >= tryFromOffset) {
                    instructions.add(list.remove(index));
                    index--;
                }
            }

            int fastSynchronizedOffset;

            if (!instructions.isEmpty()) {
                Instruction lastInstruction = instructions.get(0);
                fastSynchronizedOffset = lastInstruction.getOffset();
            } else {
                fastSynchronizedOffset = -1;
            }

            synchronizedBlockJumpOffset = searchMinusJumpOffset(instructions, 0, instructions.size(),
                    fce.getTryFromOffset(), fce.getAfterOffset());
            Collections.reverse(instructions);

            // Analyze lists of instructions
            executeReconstructors(referenceMap, classFile, instructions, localVariables);

            // Remove 'monitorenter (localTestSynchronize1 = xxx)'
            MonitorEnter menter = (MonitorEnter) list.remove(index);
            index--;
            int fastSynchronizedLineNumber = menter.getLineNumber();

            // Remove local variable for monitor
            if (menter.getObjectref().getOpcode() != Const.ALOAD) {
                throw new UnexpectedInstructionException();
            }
            int varMonitorIndex = ((IndexInstruction) menter.getObjectref()).getIndex();
            localVariables.removeLocalVariableWithIndexAndOffset(varMonitorIndex, menter.getOffset());

            // Search monitor
            AStore astore = (AStore) list.get(index);
            Instruction monitor = astore.getValueref();

            int branch = 1;
            if (fastSynchronizedOffset != -1 && synchronizedBlockJumpOffset != -1) {
                branch = synchronizedBlockJumpOffset - fastSynchronizedOffset;
            }

            FastSynchronized fastSynchronized = new FastSynchronized(FastConstants.SYNCHRONIZED,
                    fastSynchronizedOffset, fastSynchronizedLineNumber, branch, instructions);
            fastSynchronized.setMonitor(monitor);

            // Replace 'astore localTestSynchronize1'
            list.set(index, fastSynchronized);
        } else if (fce.getType() == FastConstants.TYPE_118_SYNCHRONIZED_DOUBLE) {
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

            // Extract try blocks
            List<Instruction> instructions = new ArrayList<>();
            instruction = list.remove(index);
            int fastSynchronizedOffset = instruction.getOffset();
            instructions.add(instruction);

            synchronizedBlockJumpOffset = searchMinusJumpOffset(instructions, 0, instructions.size(),
                    fce.getTryFromOffset(), fce.getAfterOffset());

            // Remove 'monitorenter'
            MonitorEnter menter = (MonitorEnter) list.remove(index - 1);

            // Search monitor
            AStore astore = (AStore) list.get(index - 2);
            Instruction monitor = astore.getValueref();

            // Remove local variable for monitor
            int varMonitorIndex = astore.getIndex();
            localVariables.removeLocalVariableWithIndexAndOffset(varMonitorIndex, menter.getOffset());

            int branch = 1;
            if (synchronizedBlockJumpOffset != -1) {
                branch = synchronizedBlockJumpOffset - fastSynchronizedOffset;
            }

            FastSynchronized fastSynchronized = new FastSynchronized(FastConstants.SYNCHRONIZED,
                    fastSynchronizedOffset, menter.getLineNumber(), branch, instructions);
            fastSynchronized.setMonitor(monitor);

            // Replace 'astore localTestSynchronize1'
            list.set(index - 2, fastSynchronized);
        } else if (instruction.getOpcode() == Const.MONITOREXIT) {
            if (list.get(--index).getOpcode() == Const.MONITORENTER) {
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
                if (me.getObjectref().getOpcode() == ByteCodeConstants.ASSIGNMENT) {
                    AssignmentInstruction ai = (AssignmentInstruction) me.getObjectref();
                    if (ai.getValue1() instanceof AStore) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                        AStore astore = (AStore) ai.getValue1();
                        // Remove local variable for monitor
                        localVariables.removeLocalVariableWithIndexAndOffset(astore.getIndex(), astore.getOffset());
                    }
                    if (ai.getValue1() instanceof ALoad) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                        ALoad aload = (ALoad) ai.getValue1();
                        // Remove local variable for monitor
                        localVariables.removeLocalVariableWithIndexAndOffset(aload.getIndex(), aload.getOffset());
                    }
                    monitor = ai.getValue2();
                    // Remove 'monitorexit' instruction
                    list.remove(index);
                } else {
                    // Remove 'monitorexit' instruction
                    list.remove(index);
                    index--;
                    // Remove 'astore'
                    AStore astore = (AStore) list.remove(index);
                    monitor = astore.getValueref();
                    // Remove local variable for monitor
                    localVariables.removeLocalVariableWithIndexAndOffset(astore.getIndex(), astore.getOffset());
                }

                List<Instruction> instructions = new ArrayList<>();
                Instruction gi = list.remove(index);

                if (gi.getOpcode() != Const.GOTO || ((Goto) gi).getJumpOffset() != fce.getAfterOffset()) {
                    instructions.add(gi);
                }

                // Remove 'localObject = finally' instruction
                if (list.get(index).getOpcode() == Const.ASTORE) {
                    list.remove(index);
                }
                // Remove 'monitorexit' instruction
                Instruction monitorexit = list.remove(index);

                // Analyze lists of instructions
                executeReconstructors(referenceMap, classFile, instructions, localVariables);

                FastSynchronized fastSynchronized = new FastSynchronized(FastConstants.SYNCHRONIZED,
                        monitorexit.getOffset(), instruction.getLineNumber(), 1, instructions);
                fastSynchronized.setMonitor(monitor);

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

                monitor = switch (instruction.getOpcode()) {
                case Const.ASTORE -> {
                    menter = (MonitorEnter) list.remove(index);
                    AStore astore = (AStore) instruction;
                    varMonitorIndex = astore.getIndex();
                    yield astore.getValueref();
                }
                case Const.MONITORENTER -> {
                    menter = (MonitorEnter) instruction;
                    AssignmentInstruction ai = (AssignmentInstruction) menter.getObjectref();
                    AStore astore = (AStore) ai.getValue1();
                    varMonitorIndex = astore.getIndex();
                    yield ai.getValue2();
                }
                default -> throw new UnexpectedInstructionException();
                };

                // Remove local variable for monitor
                localVariables.removeLocalVariableWithIndexAndOffset(varMonitorIndex, menter.getOffset());

                List<Instruction> instructions = new ArrayList<>();
                do {
                    instruction = list.get(index);

                    if (instruction.getOpcode() == Const.MONITOREXIT) {
                        MonitorExit mexit = (MonitorExit) instruction;
                        if (mexit.getObjectref().getOpcode() == Const.ALOAD) {
                            LoadInstruction li = (LoadInstruction) mexit.getObjectref();
                            if (li.getIndex() == varMonitorIndex) {
                                break;
                            }
                        }
                    }

                    instructions.add(list.remove(index));
                } while (true);

                if (index + 1 < list.size() && list.get(index + 1).getOpcode() == ByteCodeConstants.XRETURN) {
                    // Si l'instruction retournée possède un offset inférieur à 
                    // celui de l'instruction 'monitorexit', l'instruction
                    // 'return' est ajoute au bloc synchronise.
                    Instruction monitorexit = list.get(index);
                    Instruction value = ((ReturnInstruction) list.get(index + 1)).getValueref();

                    if (monitorexit.getOffset() > value.getOffset()) {
                        instructions.add(list.remove(index + 1));
                    }
                }

                // Analyze lists of instructions
                executeReconstructors(referenceMap, classFile, instructions, localVariables);

                synchronizedBlockJumpOffset = searchMinusJumpOffset(instructions, 0, instructions.size(),
                        fce.getTryFromOffset(), fce.getAfterOffset());

                int branch = 1;
                if (synchronizedBlockJumpOffset != -1) {
                    branch = synchronizedBlockJumpOffset - instruction.getOffset();
                }

                FastSynchronized fastSynchronized = new FastSynchronized(FastConstants.SYNCHRONIZED,
                        instruction.getOffset(), menter.getLineNumber(), branch, instructions);
                fastSynchronized.setMonitor(monitor);

                // Replace 'monitorexit localTestSynchronize1'
                list.set(index, fastSynchronized);
            }
        } else {
            // Cas général
            if (fce.getAfterOffset() > list.get(list.size() - 1).getOffset()) {
                index = list.size();
            } else {
                index = InstructionUtil.getIndexForOffset(list, fce.getAfterOffset());
            }
            index--;
            int lastOffset = list.get(index).getOffset();

            // Remove instructions of finally block
            Instruction i = null;
            int finallyFromOffset = fce.getFinallyFromOffset();
            while (list.get(index).getOffset() >= finallyFromOffset) {
                i = list.remove(index);
                index--;
            }

            // Store last 'AStore' to delete last "throw' instruction later
            int exceptionLoadIndex = -1;
            if (i != null && i.getOpcode() == Const.ASTORE) {
                AStore astore = (AStore) i;
                if (astore.getValueref().getOpcode() == ByteCodeConstants.EXCEPTIONLOAD) {
                    exceptionLoadIndex = astore.getIndex();
                }
            }

            // Extract try blocks
            List<Instruction> instructions = new ArrayList<>();
            i = null;
            if (index > 0) {
                int tryFromOffset = fce.getTryFromOffset();
                i = list.get(index);

                if (i.getOffset() >= tryFromOffset) {
                    instructions.add(i);

                    while (index-- > 0) {
                        i = list.get(index);
                        if (i.getOffset() < tryFromOffset) {
                            break;
                        }
                        list.remove(index + 1);
                        instructions.add(i);
                    }
                    list.set(index + 1, null);
                }
            }

            Instruction lastInstruction;

            synchronizedBlockJumpOffset = searchMinusJumpOffset(instructions, 0, instructions.size(),
                    fce.getTryFromOffset(), fce.getAfterOffset());
            Collections.reverse(instructions);

            int lineNumber;

            if (i == null) {
                lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
            } else {
                lineNumber = i.getLineNumber();
            }

            // Reduce lists of instructions
            int length = instructions.size();

            // Get local variable index for monitor
            int monitorLocalVariableIndex = getMonitorLocalVariableIndex(list, index);

            if (length > 0) {
                // Remove 'Goto' or jump 'return' & set 'afterListOffset'
                lastInstruction = instructions.get(length - 1);
                if (lastInstruction.getOpcode() == Const.GOTO) {
                    length--;
                    instructions.remove(length);
                } else if (lastInstruction.getOpcode() == ByteCodeConstants.XRETURN) {
                    length--;
                }

                // Remove all MonitorExit instructions
                removeAllMonitorExitInstructions(instructions, length, monitorLocalVariableIndex);

                // Remove last "throw finally" instructions
                int lastIndex = list.size() - 1;
                i = list.get(lastIndex);
                if (i != null && i.getOpcode() == Const.ATHROW) {
                    AThrow at = (AThrow) list.get(lastIndex);
                    if (at.getValue().getOpcode() == ByteCodeConstants.EXCEPTIONLOAD) {
                        ExceptionLoad el = (ExceptionLoad) at.getValue();
                        if (el.getExceptionNameIndex() == 0) {
                            list.remove(lastIndex);
                        }
                    } else if (at.getValue().getOpcode() == Const.ALOAD) {
                        ALoad aload = (ALoad) at.getValue();
                        if (aload.getIndex() == exceptionLoadIndex) {
                            list.remove(lastIndex);
                        }
                    }
                }
            }

            // Remove local variable for monitor
            if (monitorLocalVariableIndex != -1) {
                MonitorEnter menter = (MonitorEnter) list.get(index);
                localVariables.removeLocalVariableWithIndexAndOffset(monitorLocalVariableIndex, menter.getOffset());
            }

            int branch = 1;
            if (synchronizedBlockJumpOffset != -1) {
                branch = synchronizedBlockJumpOffset - lastOffset;
            }

            FastSynchronized fastSynchronized = new FastSynchronized(FastConstants.SYNCHRONIZED, lastOffset,
                    lineNumber, branch, instructions);

            // Analyze lists of instructions
            executeReconstructors(referenceMap, classFile, instructions, localVariables);

            // Store new FastTry instruction
            list.set(index + 1, fastSynchronized);

            // Extract monitor
            fastSynchronized.setMonitor(formatAndExtractMonitor(list, index));
        }
    }

    private static Instruction formatAndExtractMonitor(List<Instruction> list, int index) {
        // Remove "monitorenter localTestSynchronize1"
        MonitorEnter menter = (MonitorEnter) list.remove(index);
        index--;
        switch (menter.getObjectref().getOpcode()) {
        case ByteCodeConstants.ASSIGNMENT:
            return ((AssignmentInstruction) menter.getObjectref()).getValue2();
        case ByteCodeConstants.DUPLOAD:
            // Remove Astore(DupLoad)
            list.remove(index);
            index--;
            // Remove DupStore(...)
            DupStore dupstore = (DupStore) list.remove(index);
            return dupstore.getObjectref();
        case Const.ALOAD:
            AStore astore = (AStore) list.remove(index);
            return astore.getValueref();
        default:
            return null;
        }
    }

    private static void removeAllMonitorExitInstructions(List<Instruction> instructions, int length,
            int monitorLocalVariableIndex) {
        int index = length;

        Instruction instruction;
        while (index-- > 0) {
            instruction = instructions.get(index);
            switch (instruction.getOpcode()) {
            case Const.MONITOREXIT:
                MonitorExit mexit = (MonitorExit) instruction;
                if (mexit.getObjectref().getOpcode() == Const.ALOAD) {
                    int aloadIndex = ((ALoad) mexit.getObjectref()).getIndex();
                    if (aloadIndex == monitorLocalVariableIndex) {
                        instructions.remove(index);
                    }
                }
                break;
            case FastConstants.TRY:
                FastTry ft = (FastTry) instruction;
                removeAllMonitorExitInstructions(ft.getInstructions(), ft.getInstructions().size(), monitorLocalVariableIndex);
                int i = ft.getCatches().size();
                FastCatch fc;
                while (i-- > 0) {
                    fc = ft.getCatches().get(i);
                    removeAllMonitorExitInstructions(fc.instructions(), fc.instructions().size(), monitorLocalVariableIndex);
                }
                if (ft.getFinallyInstructions() != null) {
                    removeAllMonitorExitInstructions(ft.getFinallyInstructions(), ft.getFinallyInstructions().size(),
                            monitorLocalVariableIndex);
                }
                break;
            case FastConstants.SYNCHRONIZED:
                FastSynchronized fsy = (FastSynchronized) instruction;
                removeAllMonitorExitInstructions(fsy.getInstructions(), fsy.getInstructions().size(), monitorLocalVariableIndex);
                break;
            default:
                break;
            }
        }
    }

    private static int getMonitorLocalVariableIndex(List<Instruction> list, int index) {
        MonitorEnter menter = (MonitorEnter) list.get(index);
        switch (menter.getObjectref().getOpcode()) {
        case ByteCodeConstants.DUPLOAD:
            return ((AStore) list.get(index - 1)).getIndex();
        case Const.ALOAD:
            return ((ALoad) menter.getObjectref()).getIndex();
        case ByteCodeConstants.ASSIGNMENT:
            Instruction i = ((AssignmentInstruction) menter.getObjectref()).getValue1();
            if (i.getOpcode() == Const.ALOAD) {
                return ((ALoad) i).getIndex();
            }
            // intended fall through
        default:
            return -1;
        }
    }

    private static void createFastTry(ReferenceMap referenceMap, ClassFile classFile,
            List<Instruction> list, LocalVariables localVariables, FastCodeExcepcion fce, int returnOffset) {
        int afterListOffset = fce.getAfterOffset();
        int tryJumpOffset = -1;
        int lastIndex = list.size() - 1;
        int index;

        if (afterListOffset == -1 || afterListOffset > list.get(lastIndex).getOffset()) {
            index = lastIndex;
        } else {
            index = InstructionUtil.getIndexForOffset(list, afterListOffset);
            assert index != -1;
            --index;
        }

        int lastOffset = list.get(index).getOffset();
        // /30-12-2012///int lastOffset = fce.tryToOffset;

        // Extract finally block
        List<Instruction> finallyInstructions = null;
        if (fce.getFinallyFromOffset() > 0) {
            int finallyFromOffset = fce.getFinallyFromOffset();
            finallyInstructions = new ArrayList<>();

            while (list.get(index).getOffset() >= finallyFromOffset) {
                finallyInstructions.add(list.remove(index));
                index--;
            }

            if (finallyInstructions.isEmpty()) {
                throw new IllegalStateException("Unexpected structure for finally block");
            }

            Collections.reverse(finallyInstructions);
            //////////////////////////////////afterListOffset = finallyInstructions.get(0).offset;

            // Calcul de l'offset le plus haut pour le block 'try'
            int firstOffset = finallyInstructions.get(0).getOffset();
            int minimalJumpOffset = searchMinusJumpOffset(
                    finallyInstructions, 0, finallyInstructions.size(),
                    firstOffset, afterListOffset);

            afterListOffset = firstOffset;

            if (minimalJumpOffset != -1 && afterListOffset > minimalJumpOffset) {
                afterListOffset = minimalJumpOffset;
            }
        }

        // Extract catch blocks
        List<FastCatch> catches = null;
        if (fce.getCatches() != null)
        {
            int i = fce.getCatches().size();
            catches = new ArrayList<>(i);

            FastCodeExceptionCatch fcec;
            int fromOffset;
            List<Instruction> instructions;
            int instructionsLength;
            Instruction lastInstruction;
            int tryJumpOffsetTmp;
            ExceptionLoad el;
            int offset;
            int firstOffset;
            int minimalJumpOffset;
            while (i-- > 0) {
                fcec = fce.getCatches().get(i);
                fromOffset = fcec.getFromOffset();
                instructions = new ArrayList<>();

                while (list.get(index).getOffset() >= fromOffset) {
                    instructions.add(list.remove(index));
                    if (index == 0) {
                        break;
                    }
                    index--;
                }

                instructionsLength = instructions.size();

                if (instructionsLength <= 0) {
                    throw new IllegalStateException("Empty catch block");
                }
                lastInstruction = instructions.get(0);
                tryJumpOffsetTmp = searchMinusJumpOffset(instructions, 0, instructionsLength,
                        fce.getTryFromOffset(), fce.getAfterOffset());
                if (tryJumpOffsetTmp != -1 && (tryJumpOffset == -1 || tryJumpOffset > tryJumpOffsetTmp)) {
                    tryJumpOffset = tryJumpOffsetTmp;
                }
                Collections.reverse(instructions);
                // Search exception type and local variables index
                el = searchExceptionLoadInstruction(instructions);
                if (el == null) {
                    throw new UnexpectedInstructionException();
                }
                offset = lastInstruction.getOffset();
                catches.add(0, new FastCatch(el.getOffset(), fcec.getType(), fcec.getOtherTypes(),
                    el.getIndex(), instructions));
                // Calcul de l'offset le plus haut pour le block 'try'
                firstOffset = instructions.get(0).getOffset();
                minimalJumpOffset = searchMinusJumpOffset(
                        instructions, 0, instructions.size(),
                        firstOffset, offset);
                if (afterListOffset > firstOffset) {
                    afterListOffset = firstOffset;
                }
                if (minimalJumpOffset != -1 && afterListOffset > minimalJumpOffset) {
                    afterListOffset = minimalJumpOffset;
                }
            }
        }

        // Extract try blocks
        List<Instruction> tryInstructions = new ArrayList<>();

        if (fce.getTryToOffset() < afterListOffset) {
            index = FastCodeExceptionAnalyzer.computeTryToIndex(list, fce, index, afterListOffset);
        }

        int tryFromOffset = fce.getTryFromOffset();
        Instruction i = list.get(index);

        if (i.getOffset() >= tryFromOffset) {
            tryInstructions.add(i);

            while (index-- > 0) {
                i = list.get(index);
                if (i.getOffset() < tryFromOffset) {
                    break;
                }
                list.remove(index + 1);
                tryInstructions.add(i);
            }
            list.set(index + 1, null);
        }

        int tryJumpOffsetTmp = searchMinusJumpOffset(tryInstructions, 0, tryInstructions.size(), fce.getTryFromOffset(),
                fce.getTryToOffset());
        if (tryJumpOffsetTmp != -1 && (tryJumpOffset == -1 || tryJumpOffset > tryJumpOffsetTmp)) {
            tryJumpOffset = tryJumpOffsetTmp;
        }

        Collections.reverse(tryInstructions);

        int lineNumber = tryInstructions.get(0).getLineNumber();

        if (tryJumpOffset == -1) {
            tryJumpOffset = lastOffset + 1;
        }

        FastTry fastTry = new FastTry(FastConstants.TRY, lastOffset, lineNumber, tryJumpOffset - lastOffset,
                tryInstructions, catches, finallyInstructions);

        // Reduce lists of instructions
        FastCodeExceptionAnalyzer.formatFastTry(localVariables, fce, fastTry, returnOffset);

        // Analyze lists of instructions
        executeReconstructors(referenceMap, classFile, tryInstructions, localVariables);

        if (catches != null)
        {
            int length = catches.size();

            FastCatch fc;
            List<Instruction> catchInstructions;
            for (int j = 0; j < length; ++j) {
                fc = catches.get(j);
                catchInstructions = fc.instructions();
                executeReconstructors(referenceMap, classFile, catchInstructions, localVariables);
            }
        }

        if (finallyInstructions != null) {
            executeReconstructors(referenceMap, classFile, finallyInstructions, localVariables);
        }

        // Store new FastTry instruction
        list.set(index + 1, fastTry);
    }

    private static ExceptionLoad searchExceptionLoadInstruction(List<Instruction> instructions) {
        int length = instructions.size();

        Instruction instruction;
        for (int i = 0; i < length; i++) {
            instruction = SearchInstructionByOpcodeVisitor.visit(instructions.get(i),
                    ByteCodeConstants.EXCEPTIONLOAD);

            if (instruction != null) {
                return (ExceptionLoad) instruction;
            }
        }

        return null;
    }

    /**
     * début de liste fin de liste | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
     * beforeLoopEntryOffset afterLoopOffset
     */
    private static void executeReconstructors(ReferenceMap referenceMap, ClassFile classFile, List<Instruction> list,
            LocalVariables localVariables) {
        // Reconstruction des blocs synchronisés vide
        EmptySynchronizedBlockReconstructor.reconstruct(localVariables, list);
        // Recontruction du mot clé '.class' pour le JDK 1.1.8 - B
        DotClass118BReconstructor.reconstruct(referenceMap, classFile, list);
        // Recontruction du mot clé '.class' pour le compilateur d'Eclipse
        DotClassEclipseReconstructor.reconstruct(referenceMap, classFile, list);
        // Transformation de l'ensemble 'if-break' en simple 'if'
        // A executer avant 'ComparisonInstructionAnalyzer'
        IfGotoToIfReconstructor.reconstruct(list);
        // Aggregation des instructions 'if'
        // A executer après 'AssignmentInstructionReconstructor',
        // 'IfGotoToIfReconstructor'
        // A executer avant 'TernaryOpReconstructor'
        ComparisonInstructionAnalyzer.aggregate(list);
        // Recontruction des instructions 'assert'. Cette operation doit être
        // executee après 'ComparisonInstructionAnalyzer'.
        AssertInstructionReconstructor.reconstruct(classFile, list);
        // Create ternary operator before analisys of local variables.
        // A executer après 'ComparisonInstructionAnalyzer'
        TernaryOpReconstructor.reconstruct(list);
        // Recontruction des initialisations de tableaux
        // Cette operation doit être executee après
        // 'AssignmentInstructionReconstructor'.
        InitArrayInstructionReconstructor.reconstruct(list);
        // Recontruction des operations binaires d'assignement
        AssignmentOperatorReconstructor.reconstruct(list);
        // Retrait des instructions DupLoads & DupStore associés à 
        // une constante ou un attribut.
        RemoveDupConstantsAttributes.reconstruct(list);
    }

    /** Remove 'goto' jumping on next instruction. */
    private static void removeNoJumpGotoInstruction(List<Instruction> list, int afterListOffset) {
        int index = list.size();

        if (index == 0) {
            return;
        }

        index--;
        Instruction instruction = list.get(index);
        int lastInstructionOffset = instruction.getOffset();

        if (instruction.getOpcode() == Const.GOTO) {
            int branch = ((Goto) instruction).getBranch();
            if (branch >= 0 && instruction.getOffset() + branch <= afterListOffset) {
                list.remove(index);
            }
        }

        while (index-- > 0) {
            instruction = list.get(index);

            if (instruction.getOpcode() == Const.GOTO) {
                int branch = ((Goto) instruction).getBranch();
                if (branch >= 0 && instruction.getOffset() + branch <= lastInstructionOffset) {
                    list.remove(index);
                }
            }

            lastInstructionOffset = instruction.getOffset();
        }
    }

    /**
     * Effacement de instruction 'return' inutile sauf celle en fin de méthode
     * necessaire a 'InitInstanceFieldsReconstructor".
     */
    private static void removeSyntheticReturn(List<Instruction> list, int afterListOffset, int returnOffset) {
        if (afterListOffset == returnOffset) {
            int index = list.size();

            if (index == 1) {
                index--;
                removeSyntheticReturn(list, index);
            } else if (index-- > 1 && list.get(index).getLineNumber() < list.get(index - 1).getLineNumber()) {
                removeSyntheticReturn(list, index);
            }
        }
    }

    private static void removeSyntheticReturn(List<Instruction> list, int index) {
        int iOpCode = list.get(index).getOpcode();
        if (iOpCode == Const.RETURN) {
            list.remove(index);
        } else if (iOpCode == FastConstants.LABEL) {
            FastLabel fl = (FastLabel) list.get(index);
            if (fl.getInstruction().getOpcode() == Const.RETURN) {
                fl.setInstruction(null);
            }
        }
    }

    private static void addCastInstructionOnReturn(
        ClassFile classFile, Method method, List<Instruction> list)
    {
        ConstantPool constants = classFile.getConstantPool();
        LocalVariables localVariables = method.getLocalVariables();

        AttributeSignature as = method.getAttributeSignature();
        int signatureIndex = as == null ?
                method.getDescriptorIndex() : as.getSignatureIndex();
        String signature = constants.getConstantUtf8(signatureIndex);
        String methodReturnedSignature =
                SignatureUtil.getMethodReturnedSignature(signature);

        int index = list.size();

        Instruction instruction;
        while (index-- > 0)
        {
            instruction = list.get(index);

            if (instruction.getOpcode() == ByteCodeConstants.XRETURN)
            {
                ReturnInstruction ri = (ReturnInstruction)instruction;
                String returnedSignature =
                    ri.getValueref().getReturnedSignature(constants, localVariables);

                if (StringConstants.INTERNAL_OBJECT_SIGNATURE.equals(returnedSignature) && ! StringConstants.INTERNAL_OBJECT_SIGNATURE.equals(methodReturnedSignature))
                {
                    signatureIndex = constants.addConstantUtf8(methodReturnedSignature);

                    if (ri.getValueref().getOpcode() == Const.CHECKCAST)
                    {
                        ((CheckCast)ri.getValueref()).setIndex(signatureIndex);
                    }
                    else
                    {
                        ri.setValueref(new CheckCast(
                            Const.CHECKCAST, ri.getValueref().getOffset(),
                            ri.getValueref().getLineNumber(), signatureIndex, ri.getValueref()));
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
                        signature = SignatureUtil.getInnerName(methodReturnedSignature);
                        signatureIndex = constants.addConstantUtf8(signature);
                        int classIndex = constants.addConstantClass(signatureIndex);
                        ri.valueref = new CheckCast(
                            Const.CHECKCAST, ri.valueref.offset,
                            ri.valueref.lineNumber, classIndex, ri.valueref);
                    }
                }*/
            }
        }
    }

    /**
     * début de liste fin de liste | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
     * beforeLoopEntryOffset afterLoopOffset
     *
     *
     * beforeLoopEntryOffset & loopEntryOffset: utile pour la génération
     * d'instructions 'continue' beforeListOffset: utile pour la génération de
     * déclarations de variable endLoopOffset & afterLoopOffset: utile pour la
     * génération d'instructions 'break' afterListOffset: utile pour la
     * génération d'instructions 'if-else' = lastBodyWhileLoop.offset
     *
     * WHILE instruction avant boucle | goto | beforeSubListOffset instructions
     * | instruction | beforeLoopEntryOffset if à  saut négatif |
     * loopEntryOffset, endLoopOffset, afterListOffset instruction après boucle
     * | afterLoopOffset
     *
     * DO_WHILE instruction avant boucle | beforeListOffset instructions |
     * instruction | beforeLoopEntryOffset if à  saut négatif | loopEntryOffset,
     * endLoopOffset, afterListOffset instruction après boucle | afterLoopOffset
     *
     * FOR instruction avant boucle | goto | beforeListOffset instructions |
     * instruction | beforeLoopEntryOffset iinc | loopEntryOffset,
     * afterListOffset if à  saut négatif | endLoopOffset instruction après
     * boucle | afterLoopOffset
     *
     *
     * INFINITE_LOOP instruction avant boucle | beforeListOffset instructions |
     * instruction | beforeLoopEntryOffset goto à  saut négatif |
     * loopEntryOffset, endLoopOffset, afterListOffset instruction après boucle
     * | afterLoopOffset
     */
    private static void analyzeList(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
            int afterBodyLoopOffset, int beforeListOffset, int afterListOffset, int breakOffset, int returnOffset)
    {
        // Create loops
        createLoops(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
                beforeListOffset, afterListOffset, returnOffset);

        // Create switch
        createSwitch(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
                afterBodyLoopOffset, afterListOffset, returnOffset);

        analyzeTryAndSynchronized(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset,
                loopEntryOffset, afterBodyLoopOffset, beforeListOffset, afterListOffset, breakOffset, returnOffset);

        // Recontruction de la sequence 'return (b1 == 1);' après la
        // determination des types de variable
        // A executer après 'ComparisonInstructionAnalyzer'
        TernaryOpInReturnReconstructor.reconstruct(list);

        // Create labeled 'break'
        // Cet appel permettait de reduire le nombre d'imbrication des 'if' en
        // Augmentant le nombre de 'break' et 'continue'.
         createContinue(
         list, beforeLoopEntryOffset, loopEntryOffset, returnOffset);

        // Create if and if-else
        createIfElse(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
                afterBodyLoopOffset, afterListOffset, breakOffset, returnOffset);

        // Remove 'goto' instruction jumping to next instruction
        removeNopGoto(list);

        // // Compacte les instructions 'store' suivies d'instruction 'return'
        // // A executer avant l'ajout des déclarations.
        // StoreReturnAnalyzer.Cleanup(list, localVariables);

        // Add local variable déclarations
        addDeclarations(list, localVariables, beforeListOffset);

        // Remove 'goto' jumping on next instruction
        // A VALIDER A LONG TERME.
        // MODIFICATION AJOUTER SUITE A UNE MAUVAISE RECONSTRUCTION
        // DES BLOCS tyr-catch GENERES PAR LE JDK 1.1.8.
        // SI CELA PERTURBE LA RECONSTRUCTION DES INSTRUCTIONS if,
        // 1) MODIFIER LES SAUTS DES INSTRUCTIONS goto DANS FormatCatch
        // 2) DEPLACER CETTE METHODE APRES L'APPEL A
        // FastInstructionListBuilder.Build(...)
        removeNoJumpGotoInstruction(list, afterListOffset);

        // Create labeled 'break'
        createBreakAndContinue(method, list, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
                afterBodyLoopOffset, afterListOffset, breakOffset, returnOffset);

        // Retrait des instructions DupStore associées à  une seule
        // instruction DupLoad
        SingleDupLoadAnalyzer.cleanup(list);

        // Remove synthetic 'return'
        removeSyntheticReturn(list, afterListOffset, returnOffset);

        // Add cast instruction on return
        addCastInstructionOnReturn(classFile, method, list);
    }

    private static void analyzeTryAndSynchronized(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
            int afterBodyLoopOffset, int beforeListOffset, int afterListOffset, int breakOffset, int returnOffset) {
        int index = list.size();

        Instruction instruction;
        while (index-- > 0) {
            instruction = list.get(index);

            switch (instruction.getOpcode()) {
            case FastConstants.TRY: {
                FastTry ft = (FastTry) instruction;
                int tmpBeforeListOffset = index > 0 ? list.get(index - 1).getOffset() : beforeListOffset;

                // Try block
                analyzeList(classFile, method, ft.getInstructions(), localVariables, offsetLabelSet, beforeLoopEntryOffset,
                        loopEntryOffset, afterBodyLoopOffset, tmpBeforeListOffset, afterListOffset, breakOffset,
                        returnOffset);

                // Catch blocks
                int length = ft.getCatches().size();
                for (int i = 0; i < length; i++) {
                    analyzeList(classFile, method, ft.getCatches().get(i).instructions(), localVariables, offsetLabelSet,
                            beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, tmpBeforeListOffset,
                            afterListOffset, breakOffset, returnOffset);
                }

                // Finally block
                if (ft.getFinallyInstructions() != null) {
                    analyzeList(classFile, method, ft.getFinallyInstructions(), localVariables, offsetLabelSet,
                            beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, tmpBeforeListOffset,
                            afterListOffset, breakOffset, returnOffset);
                }
            }
                break;
            case FastConstants.SYNCHRONIZED: {
                FastSynchronized fs = (FastSynchronized) instruction;
                int tmpBeforeListOffset = index > 0 ? list.get(index - 1).getOffset() : beforeListOffset;

                analyzeList(classFile, method, fs.getInstructions(), localVariables, offsetLabelSet, beforeLoopEntryOffset,
                        loopEntryOffset, afterBodyLoopOffset, tmpBeforeListOffset, afterListOffset, breakOffset,
                        returnOffset);
            }
                break;
            case Const.MONITORENTER,
                 Const.MONITOREXIT: {
                // Effacement des instructions 'monitor*' pour les cas
                // exceptionnels des blocs synchronises vide.
                list.remove(index);
            }
                break;
            }

            afterListOffset = instruction.getOffset();
        }
    }

    private static void removeNopGoto(List<Instruction> list) {
        int length = list.size();

        if (length > 1) {
            int nextOffset = list.get(length - 1).getOffset();

            Instruction instruction;
            for (int index = length - 2; index >= 0; --index) {
                instruction = list.get(index);

                if (instruction.getOpcode() == Const.GOTO) {
                    Goto gi = (Goto) instruction;

                    if (gi.getBranch() >= 0 && gi.getJumpOffset() <= nextOffset) {
                        list.remove(index);
                    }
                }

                nextOffset = instruction.getOffset();
            }
        }
    }

    /**
     * Strategie : 1) Les instructions 'store' et 'for' sont passées en revue.
     * Si elles referencent une variables locales non encore déclarée et dont la
     * portée est incluse à  la liste, une declaration est insérée. 2) Le tableau
     * des variables locales est passé en revue. Pour toutes variables locales
     * non encore déclarées et dont la portée est incluse à  la liste courante,
     * on declare les variables en début de bloc.
     */
    private static void addDeclarations(List<Instruction> list, LocalVariables localVariables, int beforeListOffset) {
        int length = list.size();

        if (length > 0) {
            // 1) Ajout de declaration sur les instructions 'store' et 'for'
            StoreInstruction si;
            LocalVariable lv;

            int lastOffset = list.get(length - 1).getOffset();

            Instruction instruction;
            for (int i = 0; i < length; i++)
            {
                instruction = list.get(i);

                if (instruction.getOpcode() == Const.ASTORE
                 || instruction.getOpcode() == Const.ISTORE
                 || instruction.getOpcode() == ByteCodeConstants.STORE) {
                    si = (StoreInstruction) instruction;
                    lv = localVariables.getLocalVariableWithIndexAndOffset(si.getIndex(), si.getOffset());
                    if (lv != null && lv.hasDeclarationFlag() == NOT_DECLARED) {
                        ReturnInstruction returnInstruction = findReturnInstructionForStore(list, length, i, si);
                        if (returnInstruction == null || returnInstruction.getLineNumber() != si.getLineNumber()) {
                            if (beforeListOffset < lv.getStartPc()
                                    && lv.getStartPc() + lv.getLength() - 1 <= lastOffset) {
                                list.set(i, new FastDeclaration(FastConstants.DECLARE, si.getOffset(), si.getLineNumber(), lv, si));
                                lv.setDeclarationFlag(DECLARED);
                                updateNewAndInitArrayInstruction(si);
                            }
                        } else {
                            // compact store / return
                            returnInstruction.setValueref(si.getValueref());
                            // remove store instruction
                            list.remove(i);
                            i--;
                            length--;
                            // flag variable to be removed later
                            lv.setToBeRemoved(true);
                        }
                    }
                } else if (instruction.getOpcode() == FastConstants.FOR) {
                    FastFor ff = (FastFor) instruction;
                    if (ff.getInit() != null && (ff.getInit().getOpcode() == Const.ASTORE || ff.getInit().getOpcode() == Const.ISTORE
                            || ff.getInit().getOpcode() == ByteCodeConstants.STORE)) {
                        si = (StoreInstruction) ff.getInit();
                        lv = localVariables.getLocalVariableWithIndexAndOffset(si.getIndex(), si.getOffset());
                        if (lv != null && lv.hasDeclarationFlag() == NOT_DECLARED
                                && beforeListOffset < lv.getStartPc() && lv.getStartPc() + lv.getLength() - 1 <= lastOffset) {
                            ff.setInit(new FastDeclaration(FastConstants.DECLARE, si.getOffset(), si.getLineNumber(), lv, si));
                            lv.setDeclarationFlag(DECLARED);
                            updateNewAndInitArrayInstruction(si);
                        }
                    }
                }
            }

            // 2) Ajout de declaration pour toutes variables non encore
            // déclarées
            // TODO A affiner. Exemple:
            // 128: String message; <--- Erreur de positionnement. La
            // déclaration se limite à  l'instruction
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
            final int lvLength = localVariables == null ? 0 : localVariables.size();
            for (int i = 0; i < lvLength; i++) {
                lv = localVariables.getLocalVariableAt(i);
                if (lv.hasDeclarationFlag() == NOT_DECLARED && !lv.isToBeRemoved() && beforeListOffset < lv.getStartPc()
                        && lv.getStartPc() + lv.getLength() - 1 <= lastOffset) {
                    int indexForNewDeclaration = InstructionUtil.getIndexForOffset(list, lv.getStartPc());
                    if (indexForNewDeclaration == -1) {
                        // 'startPc' offset not found
                        indexForNewDeclaration = 0;
                    }
                    list.add(indexForNewDeclaration, new FastDeclaration(FastConstants.DECLARE, lv.getStartPc(),
                            Instruction.UNKNOWN_LINE_NUMBER, lv, null));
                    lv.setDeclarationFlag(DECLARED);
                }
            }
        }
    }

    private static ReturnInstruction findReturnInstructionForStore(List<Instruction> list, int length, int i, StoreInstruction si) {
        if (i + 1 < length) {
            Instruction next = list.get(i + 1);
            // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            if (next instanceof ReturnInstruction
                    && si.getValueref() instanceof IndexInstruction
                    && ((ReturnInstruction) next).getValueref() instanceof IndexInstruction
                    && ((IndexInstruction) ((ReturnInstruction) next).getValueref()).getIndex() == si.getIndex()) {
                return (ReturnInstruction) next;
            }
        }
        return null;
    }

    private static void updateNewAndInitArrayInstruction(Instruction instruction) {
        if (instruction.getOpcode() == Const.ASTORE) {
            Instruction valueref = ((StoreInstruction) instruction).getValueref();
            if (valueref.getOpcode() == ByteCodeConstants.NEWANDINITARRAY) {
                valueref.setOpcode(ByteCodeConstants.INITARRAY);
            }
        }
    }


    private static void createContinue(List<Instruction> list, int beforeLoopEntryOffset, int loopEntryOffset,
            int returnOffset) {
        int length = list.size();
        for (int index = 0; index < length; index++) {
            Instruction instruction = list.get(index);
            if (ByteCodeUtil.isIfInstruction(instruction.getOpcode(), true)) {
                BranchInstruction bi = (BranchInstruction) instruction;
                int jumpOffset = bi.getJumpOffset();
                if (jumpOffset == returnOffset) {
                    if (index + 1 < length) {
                        Instruction nextInstruction = list.get(index + 1);
                        // Si il n'y a pas assez de place pour une sequence
                        // 'if' + 'return', un simple 'if' sera cree.
                        if (bi.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER
                                && bi.getLineNumber() + 1 == nextInstruction.getLineNumber()) {
                            continue;
                        }
                    }
                    List<Instruction> instructions = new ArrayList<>(1);
                    instructions
                            .add(new Return(Const.RETURN, bi.getOffset(), Instruction.UNKNOWN_LINE_NUMBER));
                    list.set(index, new FastTestList(FastConstants.IF_SIMPLE, bi.getOffset(),
                            bi.getLineNumber(), jumpOffset - bi.getOffset(), bi, instructions));
                } else if (beforeLoopEntryOffset < jumpOffset && jumpOffset <= loopEntryOffset) {
                    if (index + 1 < length) {
                        Instruction nextInstruction = list.get(index + 1);
                        // Si il n'y a pas assez de place pour une sequence
                        // 'if' + 'continue', un simple 'if' sera cree.
                        if (bi.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER && index + 1 < length
                                && bi.getLineNumber() + 1 == nextInstruction.getLineNumber())
                         {
                            continue;
                        // Si l'instruction de test est suivie d'une seule instruction
                        // 'return', la sequence 'if' + 'continue' n'est pas construite.
                        }
                        if (nextInstruction.getOpcode() == Const.RETURN
                                || nextInstruction.getOpcode() == ByteCodeConstants.XRETURN) {
                            continue;
                        }
                    }
                    list.set(index, new FastInstruction(FastConstants.IF_CONTINUE, bi.getOffset(),
                            bi.getLineNumber(), bi));
                }
            }
        }
    }

    private static void createBreakAndContinue(Method method, List<Instruction> list, IntSet offsetLabelSet,
            int beforeLoopEntryOffset, int loopEntryOffset, int afterBodyLoopOffset, int afterListOffset,
            int breakOffset, int returnOffset) {
        int length = list.size();

        Instruction instruction;
        for (int index = 0; index < length; index++)
        {
            instruction = list.get(index);

            if (ByteCodeUtil.isIfInstruction(instruction.getOpcode(), true))
            {
                BranchInstruction bi = (BranchInstruction) instruction;
                int jumpOffset = bi.getJumpOffset();

                if (beforeLoopEntryOffset < jumpOffset && jumpOffset <= loopEntryOffset) {
                    list.set(index, new FastInstruction(
                        FastConstants.IF_CONTINUE, bi.getOffset(), bi.getLineNumber(), bi));
                } else if (ByteCodeUtil.jumpTo(method.getCode(), breakOffset, jumpOffset)) {
                    list.set(index, new FastInstruction(
                        FastConstants.IF_BREAK, bi.getOffset(), bi.getLineNumber(), bi));
                } else // Si la méthode retourne 'void' et si l'instruction
                // saute un goto qui saut sur un goto ... qui saute
                // sur 'returnOffset', générer 'if-return'.
                if (ByteCodeUtil.jumpTo(method.getCode(), jumpOffset, returnOffset)) {
                    List<Instruction> instructions = new ArrayList<>(1);
                    instructions.add(new Return(Const.RETURN, bi.getOffset(),
                            Instruction.UNKNOWN_LINE_NUMBER));
                    list.set(index, new FastTestList(FastConstants.IF_SIMPLE, bi.getOffset(), bi.getLineNumber(), jumpOffset
                            - bi.getOffset(), bi, instructions));
                } else {
                    // Si l'instruction saute vers un '?return' simple,
                    // duplication de l'instruction cible pour éviter la
                    // génération d'une instruction *_LABELED_BREAK.
                    byte[] code = method.getCode();

                    // Reconnaissance bas niveau de la sequence
                    // '?load_?' suivie de '?return' en fin de méthode.
                    if (code.length == jumpOffset+2)
                    {
                        LoadInstruction load = duplicateLoadInstruction(
                            code[jumpOffset] & 255, bi.getOffset(),
                            Instruction.UNKNOWN_LINE_NUMBER);
                        if (load != null)
                        {
                            ReturnInstruction ri = duplicateReturnInstruction(
                                code[jumpOffset+1] & 255, bi.getOffset(),
                                Instruction.UNKNOWN_LINE_NUMBER, load);
                            if (ri != null)
                            {
                                List<Instruction> instructions = new ArrayList<>(1);
                                instructions.add(ri);
                                list.set(index, new FastTestList(
                                    FastConstants.IF_SIMPLE, bi.getOffset(), bi.getLineNumber(),
                                    jumpOffset-bi.getOffset(), bi, instructions));
                                continue;
                            }
                        }
                    }

                    offsetLabelSet.add(jumpOffset);
                    list.set(index, new FastInstruction(
                        FastConstants.IF_LABELED_BREAK, bi.getOffset(), bi.getLineNumber(), bi));
                }
            }
            else if (instruction.getOpcode() == Const.GOTO)
            {
                Goto g = (Goto) instruction;
                int jumpOffset = g.getJumpOffset();
                int lineNumber = g.getLineNumber();

                if (index == 0 || list.get(index-1).getLineNumber() == lineNumber) {
                    lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
                }

                if (beforeLoopEntryOffset < jumpOffset && jumpOffset <= loopEntryOffset) {
                    // L'instruction 'goto' saute vers le début de la boucle
                    if (afterListOffset == afterBodyLoopOffset && index + 1 == length) {
                        // L'instruction 'goto' est la derniere instruction
                        // a s'executer dans la boucle. Elle ne sert a rien.
                        list.remove(index);
                    } else {
                        // Creation d'une instruction 'continue'
                        list.set(index, new FastInstruction(
                            FastConstants.GOTO_CONTINUE, g.getOffset(), lineNumber, null));
                    }
                } else if (ByteCodeUtil.jumpTo(method.getCode(), breakOffset, jumpOffset)) {
                    list.set(index, new FastInstruction(
                        FastConstants.GOTO_BREAK, g.getOffset(), lineNumber, null));
                } else // Si la méthode retourne 'void' et si l'instruction
                // saute un goto qui saut sur un goto ... qui saute
                // sur 'returnOffset', générer 'return'.
                if (ByteCodeUtil.jumpTo(method.getCode(), jumpOffset, returnOffset)) {
                    list.set(index, new Return(
                        Const.RETURN, g.getOffset(), lineNumber));
                } else {
                    // Si l'instruction saute vers un '?return' simple,
                    // duplication de l'instruction cible pour éviter la
                    // génération d'une instruction *_LABELED_BREAK.
                    byte[] code = method.getCode();

                    // Reconnaissance bas niveau de la sequence
                    // '?load_?' suivie de '?return' en fin de méthode.
                    if (code.length == jumpOffset+2)
                    {
                        LoadInstruction load = duplicateLoadInstruction(
                            code[jumpOffset] & 255, g.getOffset(), lineNumber);
                        if (load != null)
                        {
                            ReturnInstruction ri = duplicateReturnInstruction(
                                code[jumpOffset+1] & 255, g.getOffset(), lineNumber, load);
                            if (ri != null)
                            {
                                // Si l'instruction precedente est un
                                // '?store' sur la même variable et si
                                // elle a le même numéro de ligne
                                // => aggregation
                                if (index > 0)
                                {
                                    instruction = list.get(index-1);

                                    if (load.getLineNumber() == instruction.getLineNumber() &&
                                        Const.ISTORE <= instruction.getOpcode() &&
                                        instruction.getOpcode() <= Const.ASTORE_3 &&
                                        load.getIndex() == ((StoreInstruction)instruction).getIndex())
                                    {
                                        StoreInstruction si = (StoreInstruction)instruction;
                                        ri.setValueref(si.getValueref());
                                        index--;
                                        list.remove(index);
                                        length--;
                                    }
                                }

                                list.set(index, ri);
                                continue;
                            }
                        }
                    }

                    offsetLabelSet.add(jumpOffset);
                    list.set(index, new FastInstruction(
                        FastConstants.GOTO_LABELED_BREAK,
                        g.getOffset(), lineNumber, g));
                }
            }
        }
    }

    private static LoadInstruction duplicateLoadInstruction(
        int opcode, int offset, int lineNumber)
    {
        switch (opcode)
        {
        case Const.ILOAD:
            return new ILoad(Const.ILOAD, offset, lineNumber, 0);
        case Const.LLOAD:
            return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, 0, "J");
        case Const.FLOAD:
            return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, 0, "F");
        case Const.DLOAD:
            return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, 0, "D");
        case Const.ALOAD:
            return new ALoad(Const.ALOAD, offset, lineNumber, 0);
        case Const.ILOAD_0,
             Const.ILOAD_1,
             Const.ILOAD_2,
             Const.ILOAD_3:
            return new ILoad(Const.ILOAD, offset, lineNumber, opcode-Const.ILOAD_0);
        case Const.LLOAD_0,
             Const.LLOAD_1,
             Const.LLOAD_2,
             Const.LLOAD_3:
            return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, opcode-Const.LLOAD_0, "J");
        case Const.FLOAD_0,
             Const.FLOAD_1,
             Const.FLOAD_2,
             Const.FLOAD_3:
            return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, opcode-Const.FLOAD_0, "F");
        case Const.DLOAD_0,
             Const.DLOAD_1,
             Const.DLOAD_2,
             Const.DLOAD_3:
            return new LoadInstruction(ByteCodeConstants.LOAD, offset, lineNumber, opcode-Const.DLOAD_0, "D");
        case Const.ALOAD_0,
             Const.ALOAD_1,
             Const.ALOAD_2,
             Const.ALOAD_3:
            return new ALoad(Const.ALOAD, offset, lineNumber, opcode-Const.ALOAD_0);
        default:
            return null;
        }
    }

    private static ReturnInstruction duplicateReturnInstruction(
        int opcode, int offset, int lineNumber, Instruction instruction)
    {
        if (opcode == Const.IRETURN
         || opcode == Const.LRETURN
         || opcode == Const.FRETURN
         || opcode == Const.DRETURN
         || opcode == Const.ARETURN) {
            return new ReturnInstruction(ByteCodeConstants.XRETURN, offset, lineNumber, instruction);
        }
        return null;
    }

    private static int unoptimizeIfElseInLoop(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeListOffset, int afterListOffset,
            int returnOffset, int offset, int jumpOffset, int index) {
        int firstLoopInstructionIndex = InstructionUtil.getIndexForOffset(list, jumpOffset);
        if (firstLoopInstructionIndex != -1) {
            int length = list.size();
            if (index + 1 < length) {
                int afterLoopInstructionOffset = list.get(index + 1).getOffset();

                // Changement du calcul du saut : on considere que
                // l'instruction vers laquelle le saut négatif pointe.
                // int afterLoopJumpOffset = SearchMinusJumpOffset(
                // list, firstLoopInstructionIndex, index,
                // jumpOffset-1, afterLoopInstructionOffset);
                int afterLoopJumpOffset;
                Instruction firstLoopInstruction = list.get(firstLoopInstructionIndex);

                if (ByteCodeUtil.isIfOrGotoInstruction(firstLoopInstruction.getOpcode(), true)
                 || firstLoopInstruction.getOpcode() == FastConstants.TRY
                 || firstLoopInstruction.getOpcode() == FastConstants.SYNCHRONIZED) {
                    BranchInstruction bi = (BranchInstruction) firstLoopInstruction;
                    afterLoopJumpOffset = bi.getJumpOffset();
                } else {
                    afterLoopJumpOffset = -1;
                }

                if (afterLoopJumpOffset > afterLoopInstructionOffset) {
                    int afterLoopInstructionIndex = InstructionUtil.getIndexForOffset(list, afterLoopJumpOffset);

                    if (afterLoopInstructionIndex == -1 && afterLoopJumpOffset <= afterListOffset) {
                        afterLoopInstructionIndex = length;
                    }

                    if (afterLoopInstructionIndex != -1) {
                        int lastInstructionoffset = list.get(afterLoopInstructionIndex - 1).getOffset();

                        if (// Check previous instructions
                        InstructionUtil.checkNoJumpToInterval(list, 0, firstLoopInstructionIndex, offset,
                                lastInstructionoffset) &&
                        // Check next instructions
                                InstructionUtil.checkNoJumpToInterval(list, afterLoopInstructionIndex, list.size(),
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
                            // Attention: le goto genere a le même offset que
                            // l'instruction precedente.
                            Goto newGi = new Goto(Const.GOTO, lastInstruction.getOffset(),
                                    Instruction.UNKNOWN_LINE_NUMBER, jumpOffset - lastInstruction.getOffset());
                            list.add(afterLoopInstructionIndex, newGi);

                            return analyzeBackGoto(classFile, method, list, localVariables, offsetLabelSet,
                                    beforeListOffset, afterLoopJumpOffset, returnOffset, afterLoopInstructionIndex,
                                    newGi, jumpOffset);
                        }
                    }
                }
            }
        }

        return -1;
    }

    private static int unoptimizeIfiniteLoop(ClassFile classFile, Method method, List<Instruction> list,
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

        if (jumpIndex + 1 >= length) {
            return -1;
        }

        Instruction instruction = list.get(jumpIndex + 1);

        if (instruction.getOpcode() != Const.GOTO) {
            return -1;
        }

        int afterGotoOffset = jumpIndex + 2 >= length ? afterListOffset : list.get(jumpIndex + 2).getOffset();

        Goto g = (Goto) instruction;
        int jumpGotoOffset = g.getJumpOffset();

        if (g.getOffset() >= jumpGotoOffset || jumpGotoOffset > afterGotoOffset) {
            return -1;
        }

        // Motif de code trouvé
        int newGotoOffset = g.getOffset() + 1;

        // 1) Modification de l'offset de saut
        bi.setJumpOffset(newGotoOffset);

        // 2) Ajout d'une nouvelle instruction 'goto'
        Goto newGoto = new Goto(Const.GOTO, newGotoOffset, Instruction.UNKNOWN_LINE_NUMBER, jumpOffset
                - newGotoOffset);
        list.add(jumpIndex + 2, newGoto);

        return analyzeBackGoto(classFile, method, list, localVariables, offsetLabelSet, beforeListOffset,
                jumpGotoOffset, returnOffset, jumpIndex + 2, newGoto, jumpOffset);
    }

    /**
     * début de liste fin de liste | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
     * beforeLoopEntryOffset afterLoopOffset
     */
    private static void createLoops(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
            int beforeListOffset, int afterListOffset, int returnOffset) {
        // Unoptimize loop in loop
        int index = list.size();

        while (index-- > 0) {
            Instruction instruction = list.get(index);

            if (ByteCodeUtil.isIfOrGotoInstruction(instruction.getOpcode(), true)
              && unoptimizeLoopInLoop(list, beforeListOffset, index, instruction)) {
                index++;
            }
        }

        // Create loops
        index = list.size();

        Instruction instruction;
        while (index-- > 0) {
            instruction = list.get(index);

            switch (instruction.getOpcode()) {
            case ByteCodeConstants.IF,
                 ByteCodeConstants.IFCMP,
                 ByteCodeConstants.IFXNULL,
                 ByteCodeConstants.COMPLEXIF: {
                BranchInstruction bi = (BranchInstruction) instruction;
                if (bi.getBranch() < 0) {
                    int jumpOffset = bi.getJumpOffset();

                    if (beforeListOffset < jumpOffset
                            && (beforeLoopEntryOffset >= jumpOffset || jumpOffset > loopEntryOffset)) {
                        int newIndex = unoptimizeIfElseInLoop(classFile, method, list, localVariables, offsetLabelSet,
                                beforeListOffset, afterListOffset, returnOffset, bi.getOffset(), jumpOffset, index);

                        if (newIndex == -1) {
                            newIndex = unoptimizeIfiniteLoop(classFile, method, list, localVariables, offsetLabelSet,
                                    beforeListOffset, afterListOffset, returnOffset, bi, jumpOffset, index);
                        }

                        if (newIndex == -1) {
                            index = analyzeBackIf(classFile, method, list, localVariables, offsetLabelSet,
                                    beforeListOffset, returnOffset, index, bi);
                        } else {
                            index = newIndex;
                        }
                    }
                }
            }
                break;
            case Const.GOTO: {
                Goto gi = (Goto) instruction;
                if (gi.getBranch() < 0) {
                    int jumpOffset = gi.getJumpOffset();

                    if (beforeListOffset < jumpOffset
                            && (beforeLoopEntryOffset >= jumpOffset || jumpOffset > loopEntryOffset)) {
                        int newIndex = unoptimizeIfElseInLoop(classFile, method, list, localVariables, offsetLabelSet,
                                beforeListOffset, afterListOffset, returnOffset, gi.getOffset(), jumpOffset, index);

                        if (newIndex == -1) {
                            index = analyzeBackGoto(classFile, method, list, localVariables, offsetLabelSet,
                                    beforeListOffset, gi.getOffset(), returnOffset, index, gi, jumpOffset);
                        } else {
                            index = newIndex;
                        }
                    }
                }
            }
                break;
            case FastConstants.TRY,
                 FastConstants.SYNCHRONIZED: {
                FastList fl = (FastList) instruction;
                if (!fl.getInstructions().isEmpty()) {
                    int previousOffset = index > 0 ? list.get(index - 1).getOffset() : beforeListOffset;
                    int jumpOffset = fl.getJumpOffset();

                    if (jumpOffset != -1 && previousOffset >= jumpOffset && beforeListOffset < jumpOffset
                            && (beforeLoopEntryOffset >= jumpOffset || jumpOffset > loopEntryOffset)) {
                        fl.setBranch(1);
                        int afterSubListOffset = index + 1 < list.size() ? list.get(index + 1).getOffset()
                                : afterListOffset;
                        index = analyzeBackGoto(classFile, method, list, localVariables, offsetLabelSet,
                                beforeListOffset, afterSubListOffset, returnOffset, index, fl, jumpOffset);
                    }
                }
            }
                break;
            }
        }
    }

    private static boolean unoptimizeLoopInLoop(List<Instruction> list, int beforeListOffset, int index,
            Instruction instruction) {
        // Retrait de l'optimisation des boucles dans les boucles c.a.d. rajout
        // de l'instruction 'goto' supprimée.
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
        BranchInstruction bi = (BranchInstruction) instruction;
        if (bi.getBranch() >= 0) {
            return false;
        }

        int jumpOffset = bi.getJumpOffset();
        if (jumpOffset <= beforeListOffset) {
            return false;
        }

        int indexBi = index;

        // Recherche de l'instruction cible et verification qu'aucune
        // instruction switch dans l'intervale ne saute pas a l'exterieur
        // de l'intervale.
        for (;;) {
            if (index == 0) {
                return false;
            }

            index--;
            instruction = list.get(index);

            if (instruction.getOffset() <= jumpOffset) {
                break;
            }

            if (instruction.getOpcode() == Const.LOOKUPSWITCH || instruction.getOpcode() == Const.TABLESWITCH) {
                Switch s = (Switch) instruction;
                if (s.getOffset() + s.getDefaultOffset() > bi.getOffset()) {
                    return false;
                }
                int j = s.getOffsets().length;
                while (j-- > 0) {
                    if (s.getOffset() + s.getOffset(j) > bi.getOffset()) {
                        return false;
                    }
                }
            }
        }

        instruction = list.get(index + 1);

        if (bi == instruction) {
            return false;
        }

        if (ByteCodeUtil.isIfInstruction(instruction.getOpcode(), true)) {
            BranchInstruction bi2 = (BranchInstruction) instruction;

            if (bi2.getBranch() >= 0) {
                return false;
            }

            // Verification qu'aucune instruction switch definie avant
            // l'intervale ne saute dans l'intervale.
            for (int i = 0; i < index; i++) {
                instruction = list.get(i);

                if (instruction.getOpcode() == Const.LOOKUPSWITCH || instruction.getOpcode() == Const.TABLESWITCH) {
                    Switch s = (Switch) instruction;
                    if (s.getOffset() + s.getDefaultOffset() > bi2.getOffset()) {
                        return false;
                    }
                    int j = s.getOffsets().length;
                    while (j-- > 0) {
                        if (s.getOffset() + s.getOffset(j) > bi2.getOffset()) {
                            return false;
                        }
                    }
                }
            }

            // Unoptimize loop in loop
            int jumpOffset2 = bi2.getJumpOffset();

            // Recherche de l'instruction cible et verification qu'aucune
            // instruction switch dans l'intervale ne saute pas a l'exterieur
            // de l'intervale.
            for (;;) {
                if (index == 0) {
                    return false;
                }

                index--;
                instruction = list.get(index);

                if (instruction.getOffset() <= jumpOffset2) {
                    break;
                }

                if (instruction.getOpcode() == Const.LOOKUPSWITCH || instruction.getOpcode() == Const.TABLESWITCH) {
                    Switch s = (Switch) instruction;
                    if (s.getOffset() + s.getDefaultOffset() > bi.getOffset()) {
                        return false;
                    }
                    int j = s.getOffsets().length;
                    while (j-- > 0) {
                        if (s.getOffset() + s.getOffset(j) > bi.getOffset()) {
                            return false;
                        }
                    }
                }
            }

            Instruction target = list.get(index + 1);

            if (bi2 == target) {
                return false;
            }

            // Verification qu'aucune instruction switch definie avant
            // l'intervale ne saute dans l'intervale.
            for (int i = 0; i < index; i++) {
                instruction = list.get(i);

                if (instruction.getOpcode() == Const.LOOKUPSWITCH || instruction.getOpcode() == Const.TABLESWITCH) {
                    Switch s = (Switch) instruction;
                    if (s.getOffset() + s.getDefaultOffset() > bi2.getOffset()) {
                        return false;
                    }
                    int j = s.getOffsets().length;
                    while (j-- > 0) {
                        if (s.getOffset() + s.getOffset(j) > bi2.getOffset()) {
                            return false;
                        }
                    }
                }
            }

            if (bi.getOpcode() == Const.GOTO) {
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
                list.add(indexBi + 1, new Goto(Const.GOTO, bi.getOffset() + 1, Instruction.UNKNOWN_LINE_NUMBER,
                        jumpOffset2 - bi.getOffset() - 1));
                // 2) Modify branch offset of first loop
                bi2.setJumpOffset(bi.getOffset() + 1);
            } else // Original: Optimisation:
            // | |
            // ,----+ goto ,----+ goto
            // | ,--+ GOTO <-. | |
            // | | | <---. | | | <---.
            // | | | | | | | |
            // | '->+ if --' | | + if --'<--.
            // | | | | | |
            // '--->+ if ----' '--->+ if ------'
            // | |
            if (target.getOpcode() == Const.GOTO && ((Goto) target).getJumpOffset() == jumpOffset2) {
                // 'goto' exists
                // 1) Modify branch offset of first loop
                bi.setJumpOffset(jumpOffset2);
            } else {
                // Goto does not exist
                // 1) Create 'goto'
                list.add(index + 1, new Goto(Const.GOTO, jumpOffset2 - 1,
                        Instruction.UNKNOWN_LINE_NUMBER, jumpOffset - jumpOffset2 + 1));
                // 2) Modify branch offset of first loop
                bi.setJumpOffset(jumpOffset2 - 1);
                return true;
            }
        }

        return false;
    }

    /**
     * début de liste fin de liste | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
     * beforeLoopEntryOffset afterLoopOffset
     */
    private static void createIfElse(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
            int afterBodyLoopOffset, int afterListOffset, int breakOffset, int returnOffset) {
        // Create if and if-else
        int length = list.size();
        Instruction instruction;
        for (int index = 0; index < length; index++) {
            instruction = list.get(index);

            if (ByteCodeUtil.isIfInstruction(instruction.getOpcode(), true)) {
                analyzeIfAndIfElse(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset,
                        loopEntryOffset, afterBodyLoopOffset, afterListOffset, breakOffset, returnOffset, index,
                        (ConditionalBranchInstruction) instruction);
                length = list.size();
            }
        }
    }

    /**
     * début de liste fin de liste | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
     * beforeLoopEntryOffset afterLoopOffset
     */
    private static void createSwitch(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
            int afterBodyLoopOffset, int afterListOffset, int returnOffset) {
        Instruction instruction;
        // Create switch
        for (int index = 0; index < list.size(); index++) {
            instruction = list.get(index);

            if (instruction.getOpcode() == Const.LOOKUPSWITCH) {
                analyzeLookupSwitch(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset,
                        loopEntryOffset, afterBodyLoopOffset, afterListOffset, returnOffset, index,
                        (LookupSwitch) instruction);
            } else if (instruction.getOpcode() == Const.TABLESWITCH) {
                index = analyzeTableSwitch(classFile, method, list, localVariables, offsetLabelSet,
                        beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, afterListOffset, returnOffset,
                        index, (TableSwitch) instruction);
            }
        }
    }

    private static void removeLocalVariable(Method method, IndexInstruction ii)
    {
        LocalVariable lv = method.getLocalVariables().searchLocalVariableWithIndexAndOffset(ii.getIndex(), ii.getOffset());

        if (lv != null && ii.getOffset() == lv.getStartPc()) {
            method.getLocalVariables().removeLocalVariableWithIndexAndOffset(ii.getIndex(), ii.getOffset());
        }
    }

    /**
     * début de liste fin de liste | testIndex | | | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
     * beforeLoopEntryOffset afterLoopOffset
     *
     * Pour les boucles 'for', beforeLoopEntryOffset & loopEntryOffset encadrent
     * l'instruction d'incrementation.
     */
    private static int analyzeBackIf(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeListOffset, int returnOffset,
            int testIndex, Instruction test) {
        int index = testIndex - 1;
        List<Instruction> subList = new ArrayList<>();
        int firstOffset = ((BranchInstruction) test).getJumpOffset();

        int beforeLoopEntryOffset = index >= 0 ? list.get(index).getOffset() : beforeListOffset;

        // Move body of loop in a new list
        while (index >= 0 && list.get(index).getOffset() >= firstOffset) {
            subList.add(list.remove(index));
            index--;
        }

        int subListLength = subList.size();

        // Search escape offset
        if (index >= 0) {
            beforeListOffset = list.get(index).getOffset();
        }
        int breakOffset = searchMinusJumpOffset(subList, 0, subListLength, beforeListOffset, test.getOffset());

        // Search jump instruction before 'while' loop
        Instruction jumpInstructionBeforeLoop = null;

        if (index >= 0) {
            int i = index + 1;

            Instruction instruction;
            while (i-- > 0) {
                instruction = list.get(i);

                if (ByteCodeUtil.isIfOrGotoInstruction(instruction.getOpcode(), true)
                 || instruction.getOpcode() == FastConstants.TRY
                 || instruction.getOpcode() == FastConstants.SYNCHRONIZED) {
                    BranchInstruction bi = (BranchInstruction) instruction;
                    int offset = bi.getJumpOffset();
                    int lastBodyOffset = !subList.isEmpty() ? subList.get(0).getOffset() : bi.getOffset();

                    if (lastBodyOffset < offset && offset <= test.getOffset()) {
                        jumpInstructionBeforeLoop = bi;
                        i = 0; // Fin de boucle
                    }
                }
            }
        }

        if (jumpInstructionBeforeLoop != null)
        {
            // Remove 'goto' before 'while' loop
            if (jumpInstructionBeforeLoop.getOpcode() == Const.GOTO) {
                list.remove(index);
                index--;
            }

            Instruction beforeLoop = index >= 0 && index < list.size() ? list.get(index) : null;

            Instruction lastBodyLoop = null;
            Instruction beforeLastBodyLoop = null;

            if (subListLength > 0) {
                lastBodyLoop = subList.get(0);

                if (subListLength > 1) {
                    beforeLastBodyLoop = subList.get(1);

                    // Vérification qu'aucune instruction ne saute entre
                    // 'lastBodyLoop' et 'test'
                    if (!InstructionUtil.checkNoJumpToInterval(subList, 0, subListLength, lastBodyLoop.getOffset(),
                            test.getOffset())) {
                        // 'lastBodyLoop' ne peut pas être l'instruction
                        // d'incrementation d'une boucle 'for'
                        lastBodyLoop = null;
                        beforeLastBodyLoop = null;
                    }
                }
            }

            // if instruction before while loop affect same variable
            // last instruction of loop, create For loop.
            int typeLoop = getLoopType(beforeLoop, test, beforeLastBodyLoop, lastBodyLoop);

            switch (typeLoop) {
            case 2: // while (test)
                if (subListLength > 0) {
                    Collections.reverse(subList);
                    analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
                            test.getOffset(), test.getOffset(), jumpInstructionBeforeLoop.getOffset(), test.getOffset(), breakOffset,
                            returnOffset);
                }

                int branch = 1;
                if (breakOffset != -1) {
                    branch = breakOffset - test.getOffset();
                }

                index++;
                list.set(index, new FastTestList(
                    FastConstants.WHILE, test.getOffset(), test.getLineNumber(),
                    branch, test, subList));
                break;
            case 3: // for (beforeLoop; test;)
                // Remove initialisation instruction before sublist
                list.remove(index);

                if (subListLength > 0) {
                    Collections.reverse(subList);
                    analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
                            test.getOffset(), test.getOffset(), jumpInstructionBeforeLoop.getOffset(), test.getOffset(), breakOffset,
                            returnOffset);
                }

                createForLoopCase1(classFile, method, list, index, beforeLoop, test, subList, breakOffset);
                break;
            case 6: // for (; test; lastBodyLoop)
                if (subListLength > 1) {
                    Collections.reverse(subList);
                    subListLength--;
                    // Remove incrementation instruction
                    subList.remove(subListLength);
                    if (beforeLastBodyLoop != null && lastBodyLoop != null) {
                        analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLastBodyLoop.getOffset(),
                            lastBodyLoop.getOffset(), lastBodyLoop.getOffset(), jumpInstructionBeforeLoop.getOffset(),
                            lastBodyLoop.getOffset(), breakOffset, returnOffset);
                    }
                    branch = 1;
                    if (breakOffset != -1) {
                        branch = breakOffset - test.getOffset();
                    }

                    index++;
                    list.set(index, new FastFor(FastConstants.FOR, test.getOffset(), test.getLineNumber(), branch, null, test,
                            lastBodyLoop, subList));
                } else {
                    if (subListLength == 1) {
                        analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
                                test.getOffset(), test.getOffset(), jumpInstructionBeforeLoop.getOffset(), test.getOffset(), breakOffset,
                                returnOffset);
                    }

                    branch = 1;
                    if (breakOffset != -1) {
                        branch = breakOffset - test.getOffset();
                    }

                    index++;
                    list.set(index, new FastTestList(FastConstants.WHILE, test.getOffset(), test.getLineNumber(), branch, test,
                            subList));
                }
                break;
            case 7: // for (beforeLoop; test; lastBodyLoop)
                if (subListLength > 0) {
                    // Remove initialisation instruction before sublist
                    list.remove(index);

                    Collections.reverse(subList);
                    subListLength--;
                    // Remove incrementation instruction
                    subList.remove(subListLength);

                    if (subListLength > 0 && beforeLastBodyLoop != null && lastBodyLoop != null) {
                        analyzeList(classFile, method, subList, localVariables, offsetLabelSet,
                                beforeLastBodyLoop.getOffset(), lastBodyLoop.getOffset(), lastBodyLoop.getOffset(),
                                jumpInstructionBeforeLoop.getOffset(), lastBodyLoop.getOffset(), breakOffset, returnOffset);
                    }
                }

                index = createForLoopCase3(classFile, method, list, index, beforeLoop, test, lastBodyLoop, subList,
                        breakOffset);
                break;
            default:
                throw new UnexpectedElementException("AnalyzeBackIf");
            }
        } else if (subListLength > 0) {
            Collections.reverse(subList);
            analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
                    test.getOffset(), test.getOffset(), beforeListOffset, test.getOffset(), breakOffset, returnOffset);

            int branch = 1;
            if (breakOffset != -1) {
                branch = breakOffset - test.getOffset();
            }

            index++;
            list.set(index, new FastTestList(FastConstants.DO_WHILE, test.getOffset(),
                    Instruction.UNKNOWN_LINE_NUMBER, branch, test, subList));
        } else {
            index++;
            // 'do-while' avec une liste d'instructions vide devient un
            // 'while'.
            list.set(index, new FastTestList(FastConstants.WHILE, test.getOffset(), test.getLineNumber(), 1, test, null));
        }

        return index;
    }

    private static int searchMinusJumpOffset(
            List<Instruction> list,
            int fromIndex, int toIndex,
            int beforeListOffset, int lastListOffset)
    {
        int breakOffset = -1;
        int index = toIndex;

        Instruction instruction;
        while (index-- > fromIndex) {
            instruction = list.get(index);

            switch (instruction.getOpcode()) {
            case Const.GOTO,
                 ByteCodeConstants.IF,
                 ByteCodeConstants.IFCMP,
                 ByteCodeConstants.IFXNULL,
                 ByteCodeConstants.COMPLEXIF:
                BranchInstruction bi = (BranchInstruction) instruction;
                int jumpOffset = bi.getJumpOffset();

                if (jumpOffset != -1 && (jumpOffset <= beforeListOffset || lastListOffset < jumpOffset) && (breakOffset == -1 || breakOffset > jumpOffset)) {
                    breakOffset = jumpOffset;
                }
                break;
            case FastConstants.FOR,
                 FastConstants.FOREACH,
                 FastConstants.WHILE,
                 FastConstants.DO_WHILE,
                 FastConstants.SYNCHRONIZED:
                FastList fl = (FastList) instruction;
                List<Instruction> instructions = fl.getInstructions();
                if (instructions != null)
                {
                    jumpOffset = searchMinusJumpOffset(instructions, 0, instructions.size(), beforeListOffset,
                            lastListOffset);

                    if (jumpOffset != -1 && (jumpOffset <= beforeListOffset || lastListOffset < jumpOffset) && (breakOffset == -1 || breakOffset > jumpOffset)) {
                        breakOffset = jumpOffset;
                    }
                }
                break;
            case FastConstants.TRY:
                FastTry ft = (FastTry) instruction;

                jumpOffset = ft.getJumpOffset();

                if (jumpOffset != -1 && (jumpOffset <= beforeListOffset || lastListOffset < jumpOffset) && (breakOffset == -1 || breakOffset > jumpOffset)) {
                    breakOffset = jumpOffset;
                }

                // Try block
                instructions = ft.getInstructions();
                jumpOffset = searchMinusJumpOffset(instructions, 0, instructions.size(), beforeListOffset,
                        lastListOffset);

                if (jumpOffset != -1 && (jumpOffset <= beforeListOffset || lastListOffset < jumpOffset) && (breakOffset == -1 || breakOffset > jumpOffset)) {
                    breakOffset = jumpOffset;
                }

                // Catch blocks
                int i = ft.getCatches().size();
                while (i-- > 0)
                {
                    List<Instruction> catchInstructions = ft.getCatches().get(i).instructions();
                    jumpOffset = searchMinusJumpOffset(catchInstructions, 0, catchInstructions.size(),
                            beforeListOffset, lastListOffset);

                    if (jumpOffset != -1 && (jumpOffset <= beforeListOffset || lastListOffset < jumpOffset) && (breakOffset == -1 || breakOffset > jumpOffset)) {
                        breakOffset = jumpOffset;
                    }
                }

                // Finally block
                if (ft.getFinallyInstructions() != null)
                {
                    List<Instruction> finallyInstructions = ft.getFinallyInstructions();
                    jumpOffset = searchMinusJumpOffset(finallyInstructions, 0, finallyInstructions.size(),
                            beforeListOffset, lastListOffset);

                    if (jumpOffset != -1 && (jumpOffset <= beforeListOffset || lastListOffset < jumpOffset) && (breakOffset == -1 || breakOffset > jumpOffset)) {
                        breakOffset = jumpOffset;
                    }
                }
                break;
            case FastConstants.SWITCH,
                 FastConstants.SWITCH_ENUM,
                 FastConstants.SWITCH_STRING:
                FastSwitch fs = (FastSwitch) instruction;

                jumpOffset = fs.getJumpOffset();

                if (jumpOffset != -1 && (jumpOffset <= beforeListOffset || lastListOffset < jumpOffset) && (breakOffset == -1 || breakOffset > jumpOffset)) {
                    breakOffset = jumpOffset;
                }

                i = fs.getPairs().length;
                while (i-- > 0)
                {
                    List<Instruction> caseInstructions =
                        fs.getPair(i).getInstructions();
                    if (caseInstructions != null)
                    {
                        jumpOffset = searchMinusJumpOffset(caseInstructions, 0, caseInstructions.size(),
                                beforeListOffset, lastListOffset);

                        if (jumpOffset != -1 && (jumpOffset <= beforeListOffset || lastListOffset < jumpOffset) && (breakOffset == -1 || breakOffset > jumpOffset)) {
                            breakOffset = jumpOffset;
                        }
                    }
                }
                break;
            }
        }

        return breakOffset;
    }

    private static int getMaxOffset(Instruction beforeWhileLoop, Instruction test) {
        return beforeWhileLoop.getOffset() > test.getOffset() ? beforeWhileLoop.getOffset() : test.getOffset();
    }

    private static int getMaxOffset(Instruction beforeWhileLoop, Instruction test, Instruction lastBodyWhileLoop) {
        int offset = getMaxOffset(beforeWhileLoop, test);

        return offset > lastBodyWhileLoop.getOffset() ? offset : lastBodyWhileLoop.getOffset();
    }

    private static Instruction createForEachVariableInstruction(Instruction i) {
        switch (i.getOpcode()) {
        case FastConstants.DECLARE:
            ((FastDeclaration) i).setInstruction(null);
            return i;
        case Const.ASTORE:
            return new ALoad(Const.ALOAD, i.getOffset(), i.getLineNumber(), ((AStore) i).getIndex());
        case Const.ISTORE:
            return new ILoad(Const.ILOAD, i.getOffset(), i.getLineNumber(), ((IStore) i).getIndex());
        case ByteCodeConstants.STORE:
            return new LoadInstruction(ByteCodeConstants.LOAD, i.getOffset(), i.getLineNumber(), ((StoreInstruction) i).getIndex(),
                    ((StoreInstruction) i).getReturnedSignature(null, null));
        default:
            return i;
        }
    }

    private static void createForLoopCase1(ClassFile classFile, Method method, List<Instruction> list,
            int beforeWhileLoopIndex, Instruction beforeWhileLoop, Instruction test, List<Instruction> subList,
            int breakOffset) {
        int forLoopOffset = getMaxOffset(beforeWhileLoop, test);

        int branch = 1;
        if (breakOffset != -1) {
            branch = breakOffset - forLoopOffset;
        }

        // Is a for-each pattern ?
        if (isAForEachIteratorPattern(classFile, method, beforeWhileLoop, test, subList)) {
            Instruction variable = createForEachVariableInstruction(subList.remove(0));

            InvokeNoStaticInstruction insi = (InvokeNoStaticInstruction) ((AStore) beforeWhileLoop).getValueref();
            Instruction values = insi.getObjectref();

            // Remove iterator local variable
            removeLocalVariable(method, (StoreInstruction) beforeWhileLoop);

            list.set(beforeWhileLoopIndex, new FastForEach(FastConstants.FOREACH, forLoopOffset,
                    beforeWhileLoop.getLineNumber(), branch, variable, values, subList));
        } else {
            list.set(beforeWhileLoopIndex, new FastFor(FastConstants.FOR, forLoopOffset, beforeWhileLoop.getLineNumber(),
                    branch, beforeWhileLoop, test, null, subList));
        }
    }

    private static int createForLoopCase3(ClassFile classFile, Method method, List<Instruction> list,
            int beforeWhileLoopIndex, Instruction beforeWhileLoop, Instruction test, Instruction lastBodyWhileLoop,
            List<Instruction> subList, int breakOffset) {
        int forLoopOffset = getMaxOffset(beforeWhileLoop, test, lastBodyWhileLoop);

        int branch = 1;
        if (breakOffset != -1) {
            branch = breakOffset - forLoopOffset;
        }

        // Is a for-each pattern ?
        switch (getForEachArrayPatternType(classFile, beforeWhileLoop, test, lastBodyWhileLoop, list,
                beforeWhileLoopIndex, subList)) {
        case 1: // SUN 1.5
        {
            Instruction variable = createForEachVariableInstruction(subList.remove(0));

            beforeWhileLoopIndex--;
            StoreInstruction beforeBeforeWhileLoop = (StoreInstruction) list.remove(beforeWhileLoopIndex);
            AssignmentInstruction ai = (AssignmentInstruction) ((ArrayLength) beforeBeforeWhileLoop.getValueref()).getArrayref();
            Instruction values = ai.getValue2();

            // Remove length local variable
            removeLocalVariable(method, beforeBeforeWhileLoop);
            // Remove index local variable
            removeLocalVariable(method, (StoreInstruction) beforeWhileLoop);
            // Remove array tmp local variable
            removeLocalVariable(method, (AStore) ai.getValue1());

            list.set(beforeWhileLoopIndex, new FastForEach(FastConstants.FOREACH, forLoopOffset, variable.getLineNumber(),
                    branch, variable, values, subList));
        }
            break;
        case 2: // SUN 1.6
        {
            Instruction variable = createForEachVariableInstruction(subList.remove(0));

            beforeWhileLoopIndex--;
            StoreInstruction beforeBeforeWhileLoop = (StoreInstruction) list.remove(beforeWhileLoopIndex);

            beforeWhileLoopIndex--;
            StoreInstruction beforeBeforeBeforeWhileLoop = (StoreInstruction) list.remove(beforeWhileLoopIndex);
            Instruction values = beforeBeforeBeforeWhileLoop.getValueref();

            // Remove length local variable
            removeLocalVariable(method, beforeBeforeWhileLoop);
            // Remove index local variable
            removeLocalVariable(method, (StoreInstruction) beforeWhileLoop);
            // Remove array tmp local variable
            removeLocalVariable(method, beforeBeforeBeforeWhileLoop);

            list.set(beforeWhileLoopIndex, new FastForEach(FastConstants.FOREACH, forLoopOffset, variable.getLineNumber(),
                    branch, variable, values, subList));
        }
            break;
        case 3: // IBM
        {
            Instruction variable = createForEachVariableInstruction(subList.remove(0));

            beforeWhileLoopIndex--;
            StoreInstruction siIndex = (StoreInstruction) list.remove(beforeWhileLoopIndex);

            beforeWhileLoopIndex--;
            StoreInstruction siTmpArray = (StoreInstruction) list.remove(beforeWhileLoopIndex);
            Instruction values = siTmpArray.getValueref();

            // Remove length local variable
            removeLocalVariable(method, (StoreInstruction) beforeWhileLoop);
            // Remove index local variable
            removeLocalVariable(method, siIndex);
            // Remove array tmp local variable
            removeLocalVariable(method, siTmpArray);

            list.set(beforeWhileLoopIndex, new FastForEach(FastConstants.FOREACH, forLoopOffset, variable.getLineNumber(),
                    branch, variable, values, subList));
        }
            break;
        default: {
            list.set(beforeWhileLoopIndex, new FastFor(FastConstants.FOR, forLoopOffset, beforeWhileLoop.getLineNumber(),
                    branch, beforeWhileLoop, test, lastBodyWhileLoop, subList));
        }
        }

        return beforeWhileLoopIndex;
    }

    /**
     * Pattern: 7: List strings = new ArrayList(); 44: for (Iterator
     * localIterator = strings.iterator(); localIterator.hasNext(); ) { 29:
     * String s = (String)localIterator.next(); 34: System.out.println(s); }
     */
    private static boolean isAForEachIteratorPattern(ClassFile classFile, Method method, Instruction init,
            Instruction test, List<Instruction> subList) {
        // Tests: (Java 5 or later) + (Not empty sub list)
        if (classFile.getMajorVersion() < 49 || subList.isEmpty()) {
            return false;
        }

        Instruction firstInstruction = subList.get(0);

        // Test: Same line number
        // Test 'init' instruction: Iterator localIterator = strings.iterator()
        if (test.getLineNumber() != firstInstruction.getLineNumber() || init.getOpcode() != Const.ASTORE) {
            return false;
        }
        AStore astoreIterator = (AStore) init;
        if (astoreIterator.getValueref().getOpcode() != Const.INVOKEINTERFACE
                && astoreIterator.getValueref().getOpcode() != Const.INVOKEVIRTUAL) {
            return false;
        }
        LocalVariable lv = method.getLocalVariables().getLocalVariableWithIndexAndOffset(astoreIterator.getIndex(),
                astoreIterator.getOffset());
        if (lv == null || lv.getSignatureIndex() == 0) {
            return false;
        }
        ConstantPool constants = classFile.getConstantPool();
        InvokeNoStaticInstruction insi = (InvokeNoStaticInstruction) astoreIterator.getValueref();
        ConstantMethodref cmr = constants.getConstantMethodref(insi.getIndex());
        ConstantNameAndType cnat = constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
        String iteratorMethodName = constants.getConstantUtf8(cnat.getNameIndex());
        if (!"iterator".equals(iteratorMethodName)) {
            return false;
        }
        String iteratorMethodDescriptor = constants.getConstantUtf8(cnat.getSignatureIndex());
        // Test 'test' instruction: localIterator.hasNext()
        if (!"()Ljava/util/Iterator;".equals(iteratorMethodDescriptor) || test.getOpcode() != ByteCodeConstants.IF) {
            return false;
        }
        IfInstruction ifi = (IfInstruction) test;
        if (ifi.getValue().getOpcode() != Const.INVOKEINTERFACE) {
            return false;
        }
        insi = (InvokeNoStaticInstruction) ifi.getValue();
        if (insi.getObjectref().getOpcode() != Const.ALOAD
                || ((ALoad) insi.getObjectref()).getIndex() != astoreIterator.getIndex()) {
            return false;
        }
        cmr = constants.getConstantMethodref(insi.getIndex());
        cnat = constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
        String hasNextMethodName = constants.getConstantUtf8(cnat.getNameIndex());
        if (!"hasNext".equals(hasNextMethodName)) {
            return false;
        }
        String hasNextMethodDescriptor = constants.getConstantUtf8(cnat.getSignatureIndex());
        // Test first instruction: String s = (String)localIterator.next()
        if (!"()Z".equals(hasNextMethodDescriptor) || firstInstruction.getOpcode() != FastConstants.DECLARE) {
            return false;
        }
        FastDeclaration declaration = (FastDeclaration) firstInstruction;
        if (declaration.getInstruction() == null || declaration.getInstruction().getOpcode() != Const.ASTORE) {
            return false;
        }
        AStore astoreVariable = (AStore) declaration.getInstruction();

        if (astoreVariable.getValueref().getOpcode() == Const.CHECKCAST)
        {
            // Une instruction Cast est utilisée si le type de l'interation
            // n'est pas Object.
            CheckCast cc = (CheckCast) astoreVariable.getValueref();
            if (cc.getObjectref().getOpcode() != Const.INVOKEINTERFACE) {
                return false;
            }
            insi = (InvokeNoStaticInstruction) cc.getObjectref();
        }
        else
        {
            if (astoreVariable.getValueref().getOpcode() != Const.INVOKEINTERFACE) {
                return false;
            }
            insi = (InvokeNoStaticInstruction)astoreVariable.getValueref();
        }

        if (insi.getObjectref().getOpcode() != Const.ALOAD
                || ((ALoad) insi.getObjectref()).getIndex() != astoreIterator.getIndex()) {
            return false;
        }
        cmr = constants.getConstantMethodref(insi.getIndex());
        cnat = constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
        String nextMethodName = constants.getConstantUtf8(cnat.getNameIndex());
        if (!"next".equals(nextMethodName)) {
            return false;
        }
        String nextMethodDescriptor = constants.getConstantUtf8(cnat.getSignatureIndex());
        return "()Ljava/lang/Object;".equals(nextMethodDescriptor);
    }

    /**
     * Pattern SUN 1.5: 14: String[] strings = { "a", "b" }; 20: int j =
     * (arrayOfString1 = strings).length; 48: for (int i = 0; i < j; ++i) { 33:
     * String s = arrayOfString1[i]; 38: System.out.println(s); }
     *
     * Return 0: No pattern 1: Pattern SUN 1.5
     */
    private static int getForEachArraySun15PatternType(Instruction init, Instruction test, Instruction inc,
            Instruction firstInstruction, StoreInstruction siLength) {
        // Test before 'for' instruction: j = (arrayOfString1 = strings).length;
        ArrayLength al = (ArrayLength) siLength.getValueref();
        if (al.getArrayref().getOpcode() != ByteCodeConstants.ASSIGNMENT) {
            return 0;
        }
        AssignmentInstruction ai = (AssignmentInstruction) al.getArrayref();
        if (!"=".equals(ai.getOperator()) || ai.getValue1().getOpcode() != Const.ASTORE) {
            return 0;
        }
        StoreInstruction siTmpArray = (StoreInstruction) ai.getValue1();

        // Test 'init' instruction: int i = 0
        if (init.getOpcode() != Const.ISTORE) {
            return 0;
        }
        StoreInstruction siIndex = (StoreInstruction) init;
        if (siIndex.getValueref().getOpcode() != ByteCodeConstants.ICONST) {
            return 0;
        }
        IConst iconst = (IConst) siIndex.getValueref();
        // Test 'test' instruction: i < j
        if (iconst.getValue() != 0 || !"I".equals(iconst.getSignature()) || test.getOpcode() != ByteCodeConstants.IFCMP) {
            return 0;
        }
        IfCmp ifcmp = (IfCmp) test;
        // Test 'inc' instruction: ++i
        if (ifcmp.getValue1().getOpcode() != Const.ILOAD || ifcmp.getValue2().getOpcode() != Const.ILOAD
                || ((ILoad) ifcmp.getValue1()).getIndex() != siIndex.getIndex()
                || ((ILoad) ifcmp.getValue2()).getIndex() != siLength.getIndex()
                || inc.getOpcode() != Const.IINC || ((IInc) inc).getIndex() != siIndex.getIndex()
                || ((IInc) inc).getCount() != 1) {
            return 0;
        }

        // Test first instruction: String s = arrayOfString1[i];
        if (firstInstruction.getOpcode() == FastConstants.DECLARE) {
            FastDeclaration declaration = (FastDeclaration) firstInstruction;
            if (declaration.getInstruction() == null) {
                return 0;
            }
            firstInstruction = declaration.getInstruction();
        }
        if (firstInstruction.getOpcode() != ByteCodeConstants.STORE
         && firstInstruction.getOpcode() != Const.ASTORE
         && firstInstruction.getOpcode() != Const.ISTORE) {
            return 0;
        }
        StoreInstruction siVariable = (StoreInstruction) firstInstruction;
        if (siVariable.getValueref().getOpcode() != ByteCodeConstants.ARRAYLOAD) {
            return 0;
        }
        ArrayLoadInstruction ali = (ArrayLoadInstruction) siVariable.getValueref();
        if (ali.getArrayref().getOpcode() != Const.ALOAD || ali.getIndexref().getOpcode() != Const.ILOAD
                || ((ALoad) ali.getArrayref()).getIndex() != siTmpArray.getIndex()
                || ((ILoad) ali.getIndexref()).getIndex() != siIndex.getIndex()) {
            return 0;
        }

        return 1;
    }

    /**
     * Pattern SUN 1.6: String[] arr$ = { "a", "b" }; int len$ = arr$.length;
     * for(int i$ = 0; i$ < len$; i$++) { String s = arr$[i$];
     * System.out.println(s); }
     *
     * Return 0: No pattern 2: Pattern SUN 1.6
     */
    private static int getForEachArraySun16PatternType(Instruction init, Instruction test, Instruction inc,
            Instruction firstInstruction, StoreInstruction siLength, Instruction beforeBeforeForInstruction) {
        // Test before 'for' instruction: len$ = arr$.length;
        ArrayLength al = (ArrayLength) siLength.getValueref();
        // Test before before 'for' instruction: arr$ = ...;
        if (al.getArrayref().getOpcode() != Const.ALOAD || beforeBeforeForInstruction.getOpcode() != Const.ASTORE) {
            return 0;
        }
        StoreInstruction siTmpArray = (StoreInstruction) beforeBeforeForInstruction;
        // Test 'init' instruction: int i = 0
        if (siTmpArray.getIndex() != ((IndexInstruction) al.getArrayref()).getIndex() || init.getOpcode() != Const.ISTORE) {
            return 0;
        }
        StoreInstruction siIndex = (StoreInstruction) init;
        if (siIndex.getValueref().getOpcode() != ByteCodeConstants.ICONST) {
            return 0;
        }
        IConst iconst = (IConst) siIndex.getValueref();
        // Test 'test' instruction: i < j
        if (iconst.getValue() != 0 || !"I".equals(iconst.getSignature()) || test.getOpcode() != ByteCodeConstants.IFCMP) {
            return 0;
        }
        IfCmp ifcmp = (IfCmp) test;
        // Test 'inc' instruction: ++i
        if (ifcmp.getValue1().getOpcode() != Const.ILOAD || ifcmp.getValue2().getOpcode() != Const.ILOAD
                || ((ILoad) ifcmp.getValue1()).getIndex() != siIndex.getIndex()
                || ((ILoad) ifcmp.getValue2()).getIndex() != siLength.getIndex()
                || inc.getOpcode() != Const.IINC || ((IInc) inc).getIndex() != siIndex.getIndex()
                || ((IInc) inc).getCount() != 1) {
            return 0;
        }

        // Test first instruction: String s = arrayOfString1[i];
        if (firstInstruction.getOpcode() == FastConstants.DECLARE) {
            FastDeclaration declaration = (FastDeclaration) firstInstruction;
            if (declaration.getInstruction() == null) {
                return 0;
            }
            firstInstruction = declaration.getInstruction();
        }
        if (firstInstruction.getOpcode() != ByteCodeConstants.STORE
         && firstInstruction.getOpcode() != Const.ASTORE
         && firstInstruction.getOpcode() != Const.ISTORE) {
            return 0;
        }
        StoreInstruction siVariable = (StoreInstruction) firstInstruction;
        if (siVariable.getValueref().getOpcode() != ByteCodeConstants.ARRAYLOAD) {
            return 0;
        }
        ArrayLoadInstruction ali = (ArrayLoadInstruction) siVariable.getValueref();
        if (ali.getArrayref().getOpcode() != Const.ALOAD || ali.getIndexref().getOpcode() != Const.ILOAD
                || ((ALoad) ali.getArrayref()).getIndex() != siTmpArray.getIndex()
                || ((ILoad) ali.getIndexref()).getIndex() != siIndex.getIndex()) {
            return 0;
        }

        return 2;
    }

    /**
     * Pattern IBM: 81: Object localObject = args; 84: GUIMap guiMap = 0; 116:
     * for (GUIMap localGUIMap1 = localObject.length; guiMap < localGUIMap1;
     * ++guiMap) { 99: String arg = localObject[guiMap]; 106:
     * System.out.println(arg); }
     *
     * Return 0: No pattern 3: Pattern IBM
     */
    private static int getForEachArrayIbmPatternType(Instruction init, Instruction test,
            Instruction inc, List<Instruction> list, int beforeWhileLoopIndex, Instruction firstInstruction,
            StoreInstruction siIndex) {
        // Test before 'for' instruction: guiMap = 0;
        IConst icont = (IConst) siIndex.getValueref();
        // Test before before 'for' instruction: Object localObject = args;
        if (icont.getValue() != 0 || beforeWhileLoopIndex < 2) {
            return 0;
        }
        Instruction beforeBeforeForInstruction = list.get(beforeWhileLoopIndex - 2);
        // Test: Same line number
        if (test.getLineNumber() != beforeBeforeForInstruction.getLineNumber() || beforeBeforeForInstruction.getOpcode() != Const.ASTORE) {
            return 0;
        }
        StoreInstruction siTmpArray = (StoreInstruction) beforeBeforeForInstruction;

        // Test 'init' instruction: localGUIMap1 = localObject.length
        if (init.getOpcode() != Const.ISTORE) {
            return 0;
        }
        StoreInstruction siLength = (StoreInstruction) init;
        if (siLength.getValueref().getOpcode() != Const.ARRAYLENGTH) {
            return 0;
        }
        ArrayLength al = (ArrayLength) siLength.getValueref();
        // Test 'test' instruction: guiMap < localGUIMap1
        if (al.getArrayref().getOpcode() != Const.ALOAD || ((ALoad) al.getArrayref()).getIndex() != siTmpArray.getIndex()
                || test.getOpcode() != ByteCodeConstants.IFCMP) {
            return 0;
        }
        IfCmp ifcmp = (IfCmp) test;
        // Test 'inc' instruction: ++i
        // Test first instruction: String arg = localObject[guiMap];
        if (ifcmp.getValue1().getOpcode() != Const.ILOAD || ifcmp.getValue2().getOpcode() != Const.ILOAD
                || ((ILoad) ifcmp.getValue1()).getIndex() != siIndex.getIndex()
                || ((ILoad) ifcmp.getValue2()).getIndex() != siLength.getIndex()
                || inc.getOpcode() != Const.IINC || ((IInc) inc).getIndex() != siIndex.getIndex()
                || ((IInc) inc).getCount() != 1 || firstInstruction.getOpcode() != FastConstants.DECLARE) {
            return 0;
        }
        FastDeclaration declaration = (FastDeclaration) firstInstruction;
        if (declaration.getInstruction() == null || declaration.getInstruction().getOpcode() != ByteCodeConstants.STORE
                && declaration.getInstruction().getOpcode() != Const.ASTORE
                && declaration.getInstruction().getOpcode() != Const.ISTORE) {
            return 0;
        }
        StoreInstruction siVariable = (StoreInstruction) declaration.getInstruction();
        if (siVariable.getValueref().getOpcode() != ByteCodeConstants.ARRAYLOAD) {
            return 0;
        }
        ArrayLoadInstruction ali = (ArrayLoadInstruction) siVariable.getValueref();
        if (ali.getArrayref().getOpcode() != Const.ALOAD || ali.getIndexref().getOpcode() != Const.ILOAD
                || ((ALoad) ali.getArrayref()).getIndex() != siTmpArray.getIndex()
                || ((ILoad) ali.getIndexref()).getIndex() != siIndex.getIndex()) {
            return 0;
        }

        return 3;
    }

    /**
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
    private static int getForEachArrayPatternType(ClassFile classFile, Instruction init, Instruction test,
            Instruction inc, List<Instruction> list, int beforeWhileLoopIndex, List<Instruction> subList) {
        // Tests: (Java 5 or later) + (Not empty sub list)
        if (classFile.getMajorVersion() < 49 || beforeWhileLoopIndex == 0 || subList.isEmpty()) {
            return 0;
        }

        Instruction firstInstruction = subList.get(0);

        // Test: Same line number
        if (test.getLineNumber() != firstInstruction.getLineNumber()) {
            return 0;
        }

        Instruction beforeForInstruction = list.get(beforeWhileLoopIndex - 1);

        // Test: Same line number
        // Test before 'for' instruction:
        // SUN 1.5: j = (arrayOfString1 = strings).length;
        // SUN 1.6: len$ = arr$.length;
        // IBM : guiMap = 0;
        if (test.getLineNumber() != beforeForInstruction.getLineNumber() || beforeForInstruction.getOpcode() != Const.ISTORE) {
            return 0;
        }
        StoreInstruction si = (StoreInstruction) beforeForInstruction;
        if (si.getValueref().getOpcode() == Const.ARRAYLENGTH) {
            ArrayLength al = (ArrayLength) si.getValueref();
            if (al.getArrayref().getOpcode() == ByteCodeConstants.ASSIGNMENT) {
                return getForEachArraySun15PatternType(init, test, inc, firstInstruction, si);
            }
            if (beforeWhileLoopIndex > 1) {
                Instruction beforeBeforeForInstruction = list.get(beforeWhileLoopIndex - 2);
                return getForEachArraySun16PatternType(init, test, inc, firstInstruction, si,
                        beforeBeforeForInstruction);
            }
        }

        if (si.getValueref().getOpcode() == ByteCodeConstants.ICONST) {
            return getForEachArrayIbmPatternType(init, test, inc, list, beforeWhileLoopIndex,
                    firstInstruction, si);
        }

        return 0;
    }

    /**
     * Type de boucle infinie: 0: for (;;) 1: for (beforeLoop; ;) 2: while
     * (test) 3: for (beforeLoop; test;) 4: for (; ; lastBodyLoop) 5: for
     * (beforeLoop; ; lastBodyLoop) 6: for (; test; lastBodyLoop) 7: for
     * (beforeLoop; test; lastBodyLoop)
     */
    private static int getLoopType(Instruction beforeLoop, Instruction test, Instruction beforeLastBodyLoop,
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
                }
                // Cas possibles : 0, 4
                return beforeLastBodyLoop != null && beforeLastBodyLoop.getLineNumber() > lastBodyLoop.getLineNumber() ? 4
                        : 0;
            }
            /* 2: while (test) 6: for (; test; lastBodyLoop) */
            // Cas possibles : 0, 2, 4, 6
            if (lastBodyLoop != null && test.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER) {
                return test.getLineNumber() == lastBodyLoop.getLineNumber() ? 6 : 2;
            }
            // Cas possibles : 0, 2
            return 2;
        }
        if (beforeLoop.getOpcode() == ByteCodeConstants.ASSIGNMENT) {
            beforeLoop = ((AssignmentInstruction) beforeLoop).getValue1();
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
            }
            if (lastBodyLoop.getOpcode() == ByteCodeConstants.ASSIGNMENT) {
                lastBodyLoop = ((AssignmentInstruction) lastBodyLoop).getValue1();
            }
            // Cas possibles : 0, 1, 4, 5
            if (beforeLoop.getLineNumber() == Instruction.UNKNOWN_LINE_NUMBER) {
                // beforeLoop & lastBodyLoop sont-elles des instructions
                // d'affectation ou d'incrementation ?
                // (a|d|f|i|l|s)store ou iinc ?
                return checkBeforeLoopAndLastBodyLoop(beforeLoop, lastBodyLoop) ? 5 : 0;
            }
            if (beforeLoop.getLineNumber() == lastBodyLoop.getLineNumber()) {
                return 5;
            }
            return beforeLastBodyLoop != null &&
                    beforeLastBodyLoop.getLineNumber() > lastBodyLoop.getLineNumber() ? 4 : 0;
        }
        if (lastBodyLoop == null) {
            // Cas possibles : 2, 3
            /* 2: while (test) 3: for (beforeLoop; test;) */
            if (beforeLoop.getLineNumber() == Instruction.UNKNOWN_LINE_NUMBER) {
                return 2;
            }
            return beforeLoop.getLineNumber() == test.getLineNumber() ? 3 : 2;
        }
        if (lastBodyLoop.getOpcode() == ByteCodeConstants.ASSIGNMENT) {
            lastBodyLoop = ((AssignmentInstruction) lastBodyLoop).getValue1();
        }
        // Cas possibles : 0, 1, 2, 3, 4, 5, 6, 7
        if (beforeLoop.getLineNumber() == Instruction.UNKNOWN_LINE_NUMBER) {
            // beforeLoop & lastBodyLoop sont-elles des instructions
            // d'affectation ou d'incrementation ?
            // (a|d|f|i|l|s)store ou iinc ?
            /* 2: while (test) 7: for (beforeLoop; test; lastBodyLoop) */
            return checkBeforeLoopAndLastBodyLoop(beforeLoop, lastBodyLoop) ? 7 : 2;
        }
        if (beforeLastBodyLoop == null) {
            if (beforeLoop.getLineNumber() == test.getLineNumber()) {
                // Cas possibles : 3, 7
                /* 3: for (beforeLoop; test;) 7: for (beforeLoop; test; lastBodyLoop) */
                return beforeLoop.getLineNumber() == lastBodyLoop.getLineNumber() ? 7 : 3;
            }
            // Cas possibles : 2, 6
            /* 2: while (test) 6: for (; test; lastBodyLoop) */
            return test.getLineNumber() == lastBodyLoop.getLineNumber() ? 6 : 2;
        }
        if (beforeLastBodyLoop.getLineNumber() < lastBodyLoop.getLineNumber()) {
            // Cas possibles : 2, 3
            /* 2: while (test) 3: for (beforeLoop; test;) */
            return beforeLoop.getLineNumber() == test.getLineNumber() ? 3 : 2;
        }
        // Cas possibles : 6, 7
        /* 6: for (; test; lastBodyLoop) 7: for (beforeLoop; test; lastBodyLoop) */
        if (beforeLoop.getLineNumber() == test.getLineNumber()) {
            return 7;
        }
        return checkBeforeLoopAndLastBodyLoop(beforeLoop, lastBodyLoop) ? 7 : 6;
    }

    private static boolean checkBeforeLoopAndLastBodyLoop(Instruction beforeLoop, Instruction lastBodyLoop) {
        if (beforeLoop.getOpcode() == ByteCodeConstants.LOAD
         || beforeLoop.getOpcode() == ByteCodeConstants.STORE
         || beforeLoop.getOpcode() == Const.ALOAD
         || beforeLoop.getOpcode() == Const.ASTORE
         || beforeLoop.getOpcode() == Const.GETSTATIC
         || beforeLoop.getOpcode() == Const.PUTSTATIC
         || beforeLoop.getOpcode() == Const.GETFIELD
         || beforeLoop.getOpcode() == Const.PUTFIELD) {
            if (lastBodyLoop.getOpcode() == ByteCodeConstants.LOAD
             || lastBodyLoop.getOpcode() == ByteCodeConstants.STORE
             || lastBodyLoop.getOpcode() == Const.ALOAD
             || lastBodyLoop.getOpcode() == Const.ASTORE
             || lastBodyLoop.getOpcode() == Const.GETSTATIC
             || lastBodyLoop.getOpcode() == Const.PUTSTATIC
             || lastBodyLoop.getOpcode() == Const.GETFIELD
             || lastBodyLoop.getOpcode() == Const.PUTFIELD) {
                return ((IndexInstruction) beforeLoop).getIndex() == ((IndexInstruction) lastBodyLoop).getIndex();
            }
        } else if (beforeLoop.getOpcode() == Const.ISTORE && (beforeLoop.getOpcode() == lastBodyLoop.getOpcode() || lastBodyLoop.getOpcode() == Const.IINC)) {
            return ((IndexInstruction) beforeLoop).getIndex() == ((IndexInstruction) lastBodyLoop).getIndex();
        }

        return false;
    }

    /**
     * début de liste fin de liste | gotoIndex | | | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
     * beforeLoopEntryOffset afterLoopOffset
     */
    private static int analyzeBackGoto(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeListOffset, int afterSubListOffset,
            int returnOffset, int jumpInstructionIndex, Instruction jumpInstruction, int firstOffset) {
        List<Instruction> subList = new ArrayList<>();
        int index = jumpInstructionIndex - 1;

        if (jumpInstruction.getOpcode() == FastConstants.TRY || jumpInstruction.getOpcode() == FastConstants.SYNCHRONIZED) {
            subList.add(list.get(jumpInstructionIndex));
            list.set(jumpInstructionIndex, null);
        }

        while (index >= 0 && list.get(index).getOffset() >= firstOffset) {
            subList.add(list.remove(index));
            index--;
        }

        int subListLength = subList.size();

        if (subListLength > 0)
        {
            Instruction beforeLoop = index >= 0 ? list.get(index) : null;
            if (beforeLoop != null) {
                beforeListOffset = beforeLoop.getOffset();
            }
            Instruction instruction = subList.get(subListLength - 1);

            // Search escape offset
            int breakOffset = searchMinusJumpOffset(subList, 0, subListLength, beforeListOffset, jumpInstruction.getOffset());

            // Search test instruction
            BranchInstruction test = null;

            if (ByteCodeUtil.isIfInstruction(instruction.getOpcode(), true)) {
                BranchInstruction bi = (BranchInstruction) instruction;
                if (bi.getJumpOffset() == breakOffset) {
                    test = bi;
                }
            }

            Instruction lastBodyLoop = null;
            Instruction beforeLastBodyLoop = null;

            if (subListLength > 0) {
                lastBodyLoop = subList.get(0);

                if (lastBodyLoop == test) {
                    lastBodyLoop = null;
                } else if (subListLength > 1) {
                    beforeLastBodyLoop = subList.get(1);
                    if (beforeLastBodyLoop == test) {
                        beforeLastBodyLoop = null;
                    }

                    // Vérification qu'aucune instruction ne saute entre
                    // 'lastBodyLoop' et 'jumpInstruction'
                    if (!InstructionUtil.checkNoJumpToInterval(subList, 0, subListLength, lastBodyLoop.getOffset(),
                            jumpInstruction.getOffset()) || !InstructionUtil.checkNoJumpToInterval(subList, 0, subListLength, beforeListOffset,
                            firstOffset)) {
                        // 'lastBodyLoop' ne peut pas être l'instruction
                        // d'incrementation d'une boucle 'for'
                        lastBodyLoop = null;
                        beforeLastBodyLoop = null;
                    }
                }
            }

            int typeLoop = getLoopType(beforeLoop, test, beforeLastBodyLoop, lastBodyLoop);

            switch (typeLoop) {
            case 0: // for (;;)
            {
                Collections.reverse(subList);
                Instruction firstBodyLoop = subList.get(0);

                analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeListOffset,
                        firstBodyLoop.getOffset(), afterSubListOffset, beforeListOffset, afterSubListOffset, breakOffset,
                        returnOffset);

                int branch = 1;
                if (breakOffset != -1) {
                    branch = breakOffset - jumpInstruction.getOffset();
                }

                index++;
                list.set(index, new FastList(FastConstants.INFINITE_LOOP, jumpInstruction.getOffset(),
                        Instruction.UNKNOWN_LINE_NUMBER, branch, subList));
            }
                break;
            case 1: // for (beforeLoop; ;)
            {
                Collections.reverse(subList);
                Instruction firstBodyLoop = subList.get(0);
                if (beforeLoop != null) {
                    analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoop.getOffset(),
                        firstBodyLoop.getOffset(), afterSubListOffset, beforeListOffset, afterSubListOffset, breakOffset,
                        returnOffset);
                }
                int branch = 1;
                if (breakOffset != -1) {
                    branch = breakOffset - jumpInstruction.getOffset();
                }

                index++;
                list.set(index, new FastList(FastConstants.INFINITE_LOOP, jumpInstruction.getOffset(),
                        Instruction.UNKNOWN_LINE_NUMBER, branch, subList));
            }
                break;
            case 2: // while (test)
            {
                subListLength--;
                // Remove test
                subList.remove(subListLength);

                if (subListLength > 0) {
                    Collections.reverse(subList);

                    int beforeTestOffset;

                    if (beforeLoop == null) {
                        beforeTestOffset = beforeListOffset;
                    } else {
                        beforeTestOffset = beforeLoop.getOffset();
                    }

                    if (test != null) {
                        analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeTestOffset,
                            test.getOffset(), afterSubListOffset, test.getOffset(), afterSubListOffset, breakOffset, returnOffset);
                    }
                }

                int branch = 1;
                if (breakOffset != -1) {
                    branch = breakOffset - jumpInstruction.getOffset();
                }

                // 'while'
                ComparisonInstructionAnalyzer.inverseComparison(test);
                index++;
                if (test != null) {
                    list.set(index, new FastTestList(FastConstants.WHILE, jumpInstruction.getOffset(), test.getLineNumber(),
                        branch, test, subList));
                }
            }
                break;
            case 3: // for (beforeLoop; test;)
            {
                // Remove initialisation instruction before sublist
                list.remove(index);
                subListLength--;
                // Remove test
                subList.remove(subListLength);

                if (subListLength > 0) {
                    Collections.reverse(subList);
                    if (test != null && beforeLoop != null) {
                        analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoop.getOffset(),
                            test.getOffset(), afterSubListOffset, test.getOffset(), afterSubListOffset, breakOffset, returnOffset);
                    }
                }

                ComparisonInstructionAnalyzer.inverseComparison(test);
                createForLoopCase1(classFile, method, list, index, beforeLoop, test, subList, breakOffset);
            }
                break;
            case 4: // for (; ; lastBodyLoop)
            {
                Collections.reverse(subList);
                subListLength--;
                // Remove incrementation instruction
                subList.remove(subListLength);

                if (subListLength > 0) {
                    subListLength--;
                    beforeLastBodyLoop = subList.get(subListLength);
                    if (lastBodyLoop != null) {
                        analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLastBodyLoop.getOffset(),
                            lastBodyLoop.getOffset(), lastBodyLoop.getOffset(), beforeListOffset, afterSubListOffset,
                            breakOffset, returnOffset);
                    }
                }

                int branch = 1;
                if (breakOffset != -1) {
                    branch = breakOffset - jumpInstruction.getOffset();
                }

                index++;
                if (lastBodyLoop != null) {
                    list.set(index, new FastFor(FastConstants.FOR, jumpInstruction.getOffset(), lastBodyLoop.getLineNumber(),
                        branch, null, null, lastBodyLoop, subList));
                }
            }
                break;
            case 5: // for (beforeLoop; ; lastBodyLoop)
                // Remove initialisation instruction before sublist
                list.remove(index);

                Collections.reverse(subList);
                subListLength--;
                // Remove incrementation instruction
                subList.remove(subListLength);

                if (subListLength > 0) {
                    subListLength--;
                    beforeLastBodyLoop = subList.get(subListLength);
                    if (lastBodyLoop != null) {
                        analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLastBodyLoop.getOffset(),
                            lastBodyLoop.getOffset(), lastBodyLoop.getOffset(), beforeListOffset, afterSubListOffset,
                            breakOffset, returnOffset);
                    }
                }

                int branch = 1;
                if (breakOffset != -1) {
                    branch = breakOffset - jumpInstruction.getOffset();
                }
                if (lastBodyLoop != null) {
                    list.set(index, new FastFor(FastConstants.FOR, jumpInstruction.getOffset(), lastBodyLoop.getLineNumber(), branch,
                        beforeLoop, null, lastBodyLoop, subList));
                }
                break;
            case 6: // for (; test; lastBodyLoop)
                subListLength--;
                // Remove test
                subList.remove(subListLength);

                if (subListLength > 1) {
                    Collections.reverse(subList);
                    subListLength--;
                    // Remove incrementation instruction
                    subList.remove(subListLength);

                    if (subListLength > 0 && lastBodyLoop != null && test != null) {
                        beforeLastBodyLoop = subList.get(subListLength - 1);

                        analyzeList(classFile, method, subList, localVariables, offsetLabelSet,
                                beforeLastBodyLoop.getOffset(), lastBodyLoop.getOffset(), lastBodyLoop.getOffset(), test.getOffset(),
                                afterSubListOffset, breakOffset, returnOffset);
                    }

                    branch = 1;
                    if (breakOffset != -1) {
                        branch = breakOffset - jumpInstruction.getOffset();
                    }

                    ComparisonInstructionAnalyzer.inverseComparison(test);
                    index++;
                    if (lastBodyLoop != null) {
                        list.set(index, new FastFor(FastConstants.FOR, jumpInstruction.getOffset(), lastBodyLoop.getLineNumber(),
                            branch, null, test, lastBodyLoop, subList));
                    }
                } else {
                    if (subListLength == 1) {
                        int beforeTestOffset;

                        if (beforeLoop == null) {
                            beforeTestOffset = beforeListOffset;
                        } else {
                            beforeTestOffset = beforeLoop.getOffset();
                        }
                        if (test != null && lastBodyLoop != null) {
                            analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeTestOffset,
                                test.getOffset(), lastBodyLoop.getOffset(), test.getOffset(), afterSubListOffset, breakOffset,
                                returnOffset);
                        }
                    }

                    branch = 1;
                    if (breakOffset != -1) {
                        branch = breakOffset - jumpInstruction.getOffset();
                    }

                    // 'while'
                    ComparisonInstructionAnalyzer.inverseComparison(test);
                    index++;
                    if (test != null) {
                        list.set(index, new FastTestList(FastConstants.WHILE, jumpInstruction.getOffset(), test.getLineNumber(),
                            branch, test, subList));
                    }
                }
                break;
            case 7: // for (beforeLoop; test; lastBodyLoop)
                // Remove initialisation instruction before sublist
                list.remove(index);
                subListLength--;
                // Remove test
                subList.remove(subListLength);

                Collections.reverse(subList);
                subListLength--;
                // Remove incrementation instruction
                subList.remove(subListLength);

                if (subListLength > 0 && lastBodyLoop != null && test != null) {
                    beforeLastBodyLoop = subList.get(subListLength - 1);

                    analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLastBodyLoop.getOffset(),
                            lastBodyLoop.getOffset(), lastBodyLoop.getOffset(), test.getOffset(), afterSubListOffset, breakOffset,
                            returnOffset);
                }

                ComparisonInstructionAnalyzer.inverseComparison(test);
                index = createForLoopCase3(classFile, method, list, index, beforeLoop, test, lastBodyLoop, subList,
                        breakOffset);
                break;
            }
        } else {
            index++;
            // Empty infinite loop
            list.set(index, new FastList(FastConstants.INFINITE_LOOP, jumpInstruction.getOffset(),
                    Instruction.UNKNOWN_LINE_NUMBER, 0, subList));
        }

        return index;
    }

    /**
     * début de liste fin de liste | testIndex | | | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
     * beforeLoopEntryOffset afterLoopOffset
     */
    private static void analyzeIfAndIfElse(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
            int afterBodyLoopOffset, int afterListOffset, int breakOffset, int returnOffset, int testIndex,
            ConditionalBranchInstruction test) {
        int length = list.size();

        if (length == 0) {
            return;
        }

        int elseOffset = test.getJumpOffset();
        // if (elseOffset == breakOffset) NE PLUS PRODUIRE D'INSTRUCTIONS
        // IF_CONTINUE ET IF_BREAK
        // return;

        if (test.getBranch() < 0 &&
            beforeLoopEntryOffset < elseOffset &&
            elseOffset <= loopEntryOffset    &&
            afterBodyLoopOffset == afterListOffset)
        {
            // L'instruction saute sur un début de boucle et la liste termine
            // le block de la boucle.
            elseOffset = afterListOffset;
        }

        if (elseOffset <= test.getOffset() ||
            afterListOffset != -1 && elseOffset > afterListOffset) {
            return;
        }

        // Analyse classique des instructions 'if'
        int index = testIndex + 1;

        if (index < length) {
            // Non empty 'if'. Construct if block instructions
            List<Instruction> subList = new ArrayList<>();
            length = extrackBlock(list, subList, index, length, elseOffset);
            int subListLength = subList.size();

            if (subListLength == 0) {
                // Empty 'if'
                ComparisonInstructionAnalyzer.inverseComparison(test);
                list.set(testIndex, new FastTestList(FastConstants.IF_SIMPLE, test.getOffset(), test.getLineNumber(), elseOffset
                        - test.getOffset(), test, null));
                return;
            }

            int beforeSubListOffset = test.getOffset();
            Instruction beforeElseBlock = subList.get(subListLength - 1);
            int minusJumpOffset = searchMinusJumpOffset(
                    subList, 0, subListLength,
                    test.getOffset(), beforeElseBlock.getOffset());
            int lastListOffset = list.get(length - 1).getOffset();

            if (minusJumpOffset == -1
                    && subListLength > 1
                    && beforeElseBlock.getOpcode() == Const.RETURN
                    && (afterListOffset == -1 || afterListOffset == returnOffset ||
                            ByteCodeUtil.jumpTo(
                                method.getCode(),
                                ByteCodeUtil.nextInstructionOffset(method.getCode(), lastListOffset), returnOffset)) && (subList.get(subListLength - 2).getLineNumber() > beforeElseBlock.getLineNumber() || index < length && list.get(index).getLineNumber() < beforeElseBlock.getLineNumber())) {
                // Si la derniere instruction est un 'return' et si son
                // numéro de ligne est inférieur à  l'instruction precedente,
                // il s'agit d'une instruction synthetique ==> if-else
                minusJumpOffset = returnOffset == -1 ? lastListOffset + 1 : returnOffset;
            }

            if (minusJumpOffset != -1)
            {
                if (subListLength == 1 &&
                    beforeElseBlock.getOpcode() == Const.GOTO)
                {
                    // Instruction 'if' suivi d'un bloc contenant un seul 'goto'
                    // ==> Generation d'une instrcution 'break' ou 'continue'
                    createBreakAndContinue(method, subList, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
                            afterBodyLoopOffset, afterListOffset, breakOffset, returnOffset);

                    ComparisonInstructionAnalyzer.inverseComparison(test);
                    list.set(testIndex, new FastTestList(FastConstants.IF_SIMPLE, beforeElseBlock.getOffset(), test.getLineNumber(),
                            elseOffset - beforeElseBlock.getOffset(), test, subList));
                    return;
                }

                int afterIfElseOffset;

                if (minusJumpOffset < test.getOffset() &&
                    beforeLoopEntryOffset < minusJumpOffset &&
                    minusJumpOffset <= loopEntryOffset)
                {
                    // Jump to loop entry ==> continue
                    int positiveJumpOffset = searchMinusJumpOffset(
                            subList, 0, subListLength, -1, beforeElseBlock.getOffset());

                    // S'il n'y a pas de saut positif ou si le saut mini positif
                    // est au dela de la fin de la liste (pour sauter vers le
                    // 'return' final par exemple) et si la liste courante termine
                    // la boucle courante
                    if ((positiveJumpOffset == -1 || positiveJumpOffset >= afterListOffset) &&
                        afterBodyLoopOffset == afterListOffset)
                    {
                        // Cas des instructions de saut négatif dans une boucle qui
                        // participent tout de même à  une instruction if-else
                        // L'instruction saute sur un début de boucle et la liste
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

                if (afterIfElseOffset > elseOffset
                        && (afterListOffset == -1 || afterIfElseOffset <= afterListOffset ||
                                ByteCodeUtil.jumpTo(
                                    method.getCode(),
                                    ByteCodeUtil.nextInstructionOffset(method.getCode(), lastListOffset), afterIfElseOffset)))
                {
                    // If-else or If-elseif-...
                    if (beforeElseBlock.getOpcode() == Const.GOTO && ((Goto) beforeElseBlock).getJumpOffset() == minusJumpOffset
                            || beforeElseBlock.getOpcode() == Const.RETURN) {
                        // Remove 'goto' or 'return'
                        subList.remove(subListLength - 1);
                    }

                    // Construct else block instructions
                    List<Instruction> subElseList = new ArrayList<>();
                    extrackBlock(list, subElseList, index, length, afterIfElseOffset);

                    if (!subElseList.isEmpty()) {
                        analyzeList(
                            classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
                            loopEntryOffset, afterBodyLoopOffset, beforeSubListOffset, afterIfElseOffset,
                            breakOffset, returnOffset);

                        beforeSubListOffset = beforeElseBlock.getOffset();

                        analyzeList(classFile, method, subElseList, localVariables, offsetLabelSet,
                            beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, beforeSubListOffset,
                            afterIfElseOffset, breakOffset, returnOffset);

                        int subElseListLength = subElseList.size();
                        int lastIfElseOffset = subElseListLength > 0 ?
                                subElseList.get(subElseListLength - 1).getOffset() :
                                beforeSubListOffset;

                        ComparisonInstructionAnalyzer.inverseComparison(test);
                        list.set(testIndex, new FastTest2Lists(
                            FastConstants.IF_ELSE, lastIfElseOffset, test.getLineNumber(),
                            afterIfElseOffset - lastIfElseOffset, test, subList, subElseList));
                        return;
                    }
                }
            }

            // Simple 'if'
            analyzeList(classFile, method, subList, localVariables, offsetLabelSet, beforeLoopEntryOffset,
                    loopEntryOffset, afterBodyLoopOffset, beforeSubListOffset, elseOffset, breakOffset, returnOffset);

            ComparisonInstructionAnalyzer.inverseComparison(test);
            list.set(testIndex, new FastTestList(
                    FastConstants.IF_SIMPLE, beforeElseBlock.getOffset(), test.getLineNumber(),
                    elseOffset - beforeElseBlock.getOffset(), test, subList));
        } else if (elseOffset == breakOffset) {
            // If-break
            list.set(testIndex, new FastInstruction(FastConstants.IF_BREAK, test.getOffset(), test.getLineNumber(), test));
        } else {
            // Empty 'if'
            list.set(testIndex, new FastTestList(FastConstants.IF_SIMPLE, test.getOffset(), test.getLineNumber(), elseOffset
                    - test.getOffset(), test, null));
        }
    }

    private static int extrackBlock(
            List<Instruction> list, List<Instruction> subList,
            int index, int length, int endOffset)
    {
        while (index < length && list.get(index).getOffset() < endOffset) {
            subList.add(list.remove(index));
            length--;
        }

        return length;
    }

    /**
     * début de liste fin de liste | switchIndex | | | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
     * beforeLoopEntryOffset afterLoopOffset
     */
    private static void analyzeLookupSwitch(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
            int afterBodyLoopOffset, int afterListOffset, int returnOffset, int switchIndex, LookupSwitch ls) {
        final int pairLength = ls.getKeys().length;
        Pair[] pairs = new Pair[pairLength + 1];

        // Construct list of pairs
        boolean defaultFlag = true;
        int pairIndex = 0;
        for (int i = 0; i < pairLength; i++) {
            if (defaultFlag && ls.getOffset(i) > ls.getDefaultOffset()) {
                pairs[pairIndex] = new Pair(true, 0, ls.getOffset() + ls.getDefaultOffset());
                pairIndex++;
                defaultFlag = false;
            }

            pairs[pairIndex] = new Pair(false, ls.getKey(i), ls.getOffset() + ls.getOffset(i));
            pairIndex++;
        }

        if (defaultFlag) {
            pairs[pairIndex] = new Pair(true, 0, ls.getOffset() + ls.getDefaultOffset());
        }

        // SWITCH or SWITCH_ENUM ?
        int switchOpcode = analyzeSwitchType(classFile, ls.getKey());

        // SWITCH or Eclipse SWITCH_STRING ?
        if (classFile.getMajorVersion() >= 51 && switchOpcode == FastConstants.SWITCH
                && ls.getKey().getOpcode() == Const.ILOAD && switchIndex > 2 && analyzeSwitchString(classFile, localVariables, list, switchIndex, ls, pairs)) {
            switchIndex--;
            // Switch+String found.
            // Remove FastSwitch
            list.remove(switchIndex);
            switchIndex--;
            // Remove IStore
            list.remove(switchIndex);
            switchIndex--;
            // Remove AStore
            list.remove(switchIndex);
            // Change opcode
            switchOpcode = FastConstants.SWITCH_STRING;
        }

        analyzeSwitch(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
                afterBodyLoopOffset, afterListOffset, returnOffset, switchIndex, switchOpcode, ls.getOffset(),
                ls.getLineNumber(), ls.getKey(), pairs, pairLength);
    }

    private static int analyzeSwitchType(ClassFile classFile, Instruction i)
    {
        if (i.getOpcode() == ByteCodeConstants.ARRAYLOAD)
        {
            // switch(1.$SwitchMap$basic$data$TestEnum$enum1[e.ordinal()]) ?
            // switch(1.$SwitchMap$basic$data$TestEnum$enum1[request.getOperationType().ordinal()]) ?
            ArrayLoadInstruction ali = (ArrayLoadInstruction)i;

            if (ali.getIndexref().getOpcode() == Const.INVOKEVIRTUAL)
            {
                if (ali.getArrayref().getOpcode() == Const.GETSTATIC)
                {
                    GetStatic gs = (GetStatic) ali.getArrayref();

                    ConstantPool constants = classFile.getConstantPool();
                    ConstantFieldref cfr = constants.getConstantFieldref(gs.getIndex());
                    ConstantNameAndType cnat = constants.getConstantNameAndType(cfr.getNameAndTypeIndex());

                    if (classFile.getSwitchMaps().containsKey(cnat.getNameIndex())) {
                        Invokevirtual iv = (Invokevirtual) ali.getIndexref();

                        if (iv.getArgs().isEmpty()) {
                            ConstantMethodref cmr = constants.getConstantMethodref(iv.getIndex());
                            cnat = constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

                            if (StringConstants.ORDINAL_METHOD_NAME.equals(constants.getConstantUtf8(cnat.getNameIndex()))) {
                                // SWITCH_ENUM found
                                return FastConstants.SWITCH_ENUM;
                            }
                        }
                    }
                }
                else if (ali.getArrayref().getOpcode() == Const.INVOKESTATIC)
                {
                    Invokestatic is = (Invokestatic) ali.getArrayref();

                    if (is.getArgs().isEmpty()) {
                        ConstantPool constants = classFile.getConstantPool();
                        ConstantMethodref cmr = constants.getConstantMethodref(is.getIndex());

                        if (cmr.getClassIndex() == classFile.getThisClassIndex()) {
                            ConstantNameAndType cnat = constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
                            if (classFile.getSwitchMaps().containsKey(cnat.getNameIndex())) {
                                Invokevirtual iv = (Invokevirtual) ali.getIndexref();

                                if (iv.getArgs().isEmpty()) {
                                    cmr = constants.getConstantMethodref(iv.getIndex());
                                    cnat = constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

                                    if (StringConstants.ORDINAL_METHOD_NAME.equals(constants.getConstantUtf8(cnat.getNameIndex()))) {
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

    /**
     * début de liste fin de liste | switchIndex | | | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * beforeListOffset | | Offsets | loopEntryOffset endLoopOffset |
     * beforeLoopEntryOffset afterLoopOffset
     */
    private static int analyzeTableSwitch(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
            int afterBodyLoopOffset, int afterListOffset, int returnOffset, int switchIndex, TableSwitch ts) {
        final int pairLength = ts.getOffsets().length;
        Pair[] pairs = new Pair[pairLength + 1];

        // Construct list of pairs
        boolean defaultFlag = true;
        int pairIndex = 0;
        for (int i = 0; i < pairLength; i++) {
            if (defaultFlag && ts.getOffset(i) > ts.getDefaultOffset()) {
                pairs[pairIndex] = new Pair(true, 0, ts.getOffset() + ts.getDefaultOffset());
                pairIndex++;
                defaultFlag = false;
            }

            pairs[pairIndex] = new Pair(false, ts.getLow() + i, ts.getOffset() + ts.getOffset(i));
            pairIndex++;
        }

        if (defaultFlag) {
            pairs[pairIndex] = new Pair(true, 0, ts.getOffset() + ts.getDefaultOffset());
        }

        // SWITCH or Eclipse SWITCH_ENUM ?
        int switchOpcode = analyzeSwitchType(classFile, ts.getKey());

        // SWITCH or Eclipse SWITCH_STRING ?
        if (classFile.getMajorVersion() >= 51 && switchOpcode == FastConstants.SWITCH
                && ts.getKey().getOpcode() == Const.ILOAD && switchIndex > 2 && analyzeSwitchString(classFile, localVariables, list, switchIndex, ts, pairs)) {
            switchIndex--;
            // Switch+String found.
            // Remove FastSwitch
            list.remove(switchIndex);
            switchIndex--;
            // Remove IStore
            list.remove(switchIndex);
            switchIndex--;
            // Remove AStore
            list.remove(switchIndex);
            // Change opcode
            switchOpcode = FastConstants.SWITCH_STRING;
        }

        analyzeSwitch(classFile, method, list, localVariables, offsetLabelSet, beforeLoopEntryOffset, loopEntryOffset,
                afterBodyLoopOffset, afterListOffset, returnOffset, switchIndex, switchOpcode, ts.getOffset(),
                ts.getLineNumber(), ts.getKey(), pairs, pairLength);

        return switchIndex;
    }

    private static boolean analyzeSwitchString(ClassFile classFile, LocalVariables localVariables,
            List<Instruction> list, int switchIndex, Switch s, Pair[] pairs) {
        Instruction instruction = list.get(switchIndex - 3);
        if (instruction.getOpcode() != Const.ASTORE || instruction.getLineNumber() != s.getKey().getLineNumber()) {
            return false;
        }
        AStore astore = (AStore) instruction;

        instruction = list.get(switchIndex - 2);
        if (instruction.getOpcode() != Const.ISTORE || instruction.getLineNumber() != astore.getLineNumber()) {
            return false;
        }

        instruction = list.get(switchIndex - 1);
        if (instruction.getOpcode() != FastConstants.SWITCH || instruction.getLineNumber() != astore.getLineNumber()) {
            return false;
        }

        FastSwitch previousSwitch = (FastSwitch) instruction;
        if (previousSwitch.getTest().getOpcode() != Const.INVOKEVIRTUAL) {
            return false;
        }

        Invokevirtual iv = (Invokevirtual) previousSwitch.getTest();

        if (iv.getObjectref().getOpcode() != Const.ALOAD || !iv.getArgs().isEmpty()) {
            return false;
        }

        ConstantPool constants = classFile.getConstantPool();
        ConstantMethodref cmr = constants.getConstantMethodref(iv.getIndex());

        if (!"I".equals(cmr.getReturnedSignature())) {
            return false;
        }

        String className = constants.getConstantClassName(cmr.getClassIndex());
        if (!StringConstants.JAVA_LANG_STRING.equals(className)) {
            return false;
        }

        ConstantNameAndType cnat = constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
        String descriptorName = constants.getConstantUtf8(cnat.getSignatureIndex());
        if (!"()I".equals(descriptorName)) {
            return false;
        }

        String methodName = constants.getConstantUtf8(cnat.getNameIndex());
        if (!"hashCode".equals(methodName)) {
            return false;
        }

        Pair[] previousPairs = previousSwitch.getPairs();
        int i = previousPairs.length;
        if (i == 0) {
            return false;
        }

        int tsKeyIloadIndex = ((ILoad) s.getKey()).getIndex();
        int previousSwitchAloadIndex = ((ALoad) iv.getObjectref()).getIndex();
        Map<Integer, Integer> stringIndexes = new HashMap<>();

        List<Instruction> instructions;
        int length;
        FastTest2Lists ft2l;
        while (i-- > 0) {
            Pair pair = previousPairs[i];
            if (pair.isDefault()) {
                continue;
            }

            instructions = pair.getInstructions();

            for (;;) {
                length = instructions.size();
                if (length == 0) {
                    return false;
                }

                instruction = instructions.get(0);

                /*
                 * if (instruction.opcode == FastConstants.IF_BREAK) { if
                 * (((length == 2) || (length == 3)) &&
                 * (AnalyzeSwitchStringTestInstructions( constants, cmr,
                 * tsKeyIloadIndex, previousSwitchAloadIndex, stringIndexes,
                 * ((FastInstruction)instruction).instruction,
                 * instructions.get(1), ByteCodeConstants.CMP_EQ))) { break; } else
                 * { return false; } } else
                 */if (instruction.getOpcode() == FastConstants.IF_SIMPLE) {
                    switch (length) {
                    case 1:
                        break;
                    case 2:
                        if (instructions.get(1).getOpcode() == FastConstants.GOTO_BREAK) {
                            break;
                        }
                        // intended fall through
                    default:
                        return false;
                    }
                    FastTestList ftl = (FastTestList) instruction;
                    if (ftl.getInstructions().size() == 1
                            && analyzeSwitchStringTestInstructions(constants, cmr, tsKeyIloadIndex,
                                    previousSwitchAloadIndex, stringIndexes, ftl.getTest(), ftl.getInstructions().get(0),
                                    ByteCodeConstants.CMP_NE)) {
                        break;
                    }
                    return false;
                }
                if (instruction.getOpcode() != FastConstants.IF_ELSE || length != 1) {
                    return false;
                }
                ft2l = (FastTest2Lists) instruction;
                if (ft2l.getInstructions().size() != 1 || !analyzeSwitchStringTestInstructions(constants, cmr, tsKeyIloadIndex,
                        previousSwitchAloadIndex, stringIndexes, ft2l.getTest(), ft2l.getInstructions().get(0),
                        ByteCodeConstants.CMP_NE)) {
                    return false;
                }
                instructions = ft2l.getInstructions2();
            }
        }

        // First switch instruction for Switch+String found
        // Replace value of each pair
        i = pairs.length;

        Pair pair;
        while (i-- > 0) {
            pair = pairs[i];
            if (pair.isDefault()) {
                continue;
            }
            pair.setKey(stringIndexes.get(pair.getKey()));
        }

        // Remove synthetic local variable integer
        localVariables.removeLocalVariableWithIndexAndOffset(tsKeyIloadIndex, s.getKey().getOffset());
        // Remove synthetic local variable string
        localVariables.removeLocalVariableWithIndexAndOffset(astore.getIndex(), astore.getOffset());
        // Replace switch test
        s.setKey(astore.getValueref());

        return true;
    }

    private static boolean analyzeSwitchStringTestInstructions(ConstantPool constants, ConstantMethodref cmr,
            int tsKeyIloadIndex, int previousSwitchAloadIndex, Map<Integer, Integer> stringIndexes,
            Instruction test, Instruction value, int cmp) {
        if (test.getOpcode() != ByteCodeConstants.IF || value.getOpcode() != Const.ISTORE) {
            return false;
        }

        IStore istore = (IStore) value;
        if (istore.getIndex() != tsKeyIloadIndex) {
            return false;
        }

        int opcode = istore.getValueref().getOpcode();
        int index;

        if (opcode == Const.BIPUSH) {
            index = ((BIPush) istore.getValueref()).getValue();
        } else if (opcode == ByteCodeConstants.ICONST) {
            index = ((IConst) istore.getValueref()).getValue();
        } else {
            return false;
        }

        IfInstruction ii = (IfInstruction) test;
        if (ii.getCmp() != cmp || ii.getValue().getOpcode() != Const.INVOKEVIRTUAL) {
            return false;
        }

        Invokevirtual ivTest = (Invokevirtual) ii.getValue();

        if (ivTest.getArgs().size() != 1 || ivTest.getObjectref().getOpcode() != Const.ALOAD
                || ((ALoad) ivTest.getObjectref()).getIndex() != previousSwitchAloadIndex
                || ivTest.getArgs().get(0).getOpcode() != Const.LDC) {
            return false;
        }

        ConstantMethodref cmrTest = constants.getConstantMethodref(ivTest.getIndex());
        if (cmr.getClassIndex() != cmrTest.getClassIndex()) {
            return false;
        }

        ConstantNameAndType cnatTest = constants.getConstantNameAndType(cmrTest.getNameAndTypeIndex());
        String descriptorNameTest = constants.getConstantUtf8(cnatTest.getSignatureIndex());
        if (!"(Ljava/lang/Object;)Z".equals(descriptorNameTest)) {
            return false;
        }

        String methodNameTest = constants.getConstantUtf8(cnatTest.getNameIndex());
        if (!"equals".equals(methodNameTest)) {
            return false;
        }

        stringIndexes.put(index, ((Ldc) ivTest.getArgs().get(0)).getIndex());

        return true;
    }

    /**
     * début de liste fin de liste | switchIndex | | | | Liste ...
     * --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ... | | | | | | |
     * | beforeListOff. | | | Offsets | loopEntryOffset switchOffset
     * endLoopOffset | beforeLoopEntryOffset afterLoopOffset
     */
    private static void analyzeSwitch(ClassFile classFile, Method method, List<Instruction> list,
            LocalVariables localVariables, IntSet offsetLabelSet, int beforeLoopEntryOffset, int loopEntryOffset,
            int afterBodyLoopOffset, int afterListOffset, int returnOffset, int switchIndex, int switchOpcode,
            int switchOffset, int switchLineNumber, Instruction test, Pair[] pairs, final int pairLength) {
        int breakOffset = -1;

        // Order pairs by offset
        Arrays.sort(pairs);

        // Extract list of instructions for all pairs
        int lastSwitchOffset = switchOffset;
        int index = switchIndex + 1;

        if (index < list.size()) {
            int beforeCaseOffset;
            int afterCaseOffset;
            // Switch non vide ou non en derniere position dans la serie
            // d'instructions
            for (int i = 0; i < pairLength; i++)
            {
                List<Instruction> instructions = null;
                beforeCaseOffset = lastSwitchOffset;
                afterCaseOffset = pairs[i + 1].getOffset();

                Instruction instruction;
                while (index < list.size())
                {
                    instruction = list.get(index);
                    if (instruction.getOffset() >= afterCaseOffset)
                    {
                        if (instructions != null)
                        {
                            int nbrInstructions = instructions.size();
                            if (nbrInstructions > 0)
                            {
                                // Recherche de 'breakOffset'
                                int breakOffsetTmp = searchMinusJumpOffset(instructions, 0, nbrInstructions,
                                        beforeCaseOffset, lastSwitchOffset);
                                if (breakOffsetTmp != -1 && (breakOffset == -1 || breakOffset > breakOffsetTmp)) {
                                    breakOffset = breakOffsetTmp;
                                }

                                // Remplacement du dernier 'goto'
                                instruction = instructions.get(nbrInstructions - 1);
                                if (instruction.getOpcode() == Const.GOTO) {
                                    int lineNumber = instruction.getLineNumber();

                                    if (nbrInstructions <= 1 || instructions.get(nbrInstructions-2).getLineNumber() == lineNumber) {
                                        lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
                                    }

                                    // Replace goto by break;
                                    instructions.set(nbrInstructions - 1, new FastInstruction(FastConstants.GOTO_BREAK,
                                            instruction.getOffset(), lineNumber, null));
                                }
                            }
                        }
                        break;
                    }

                    if (instructions == null) {
                        instructions = new ArrayList<>();
                    }

                    list.remove(index);
                    instructions.add(instruction);
                    lastSwitchOffset = instruction.getOffset();
                }

                pairs[i].setInstructions(instructions);
            }

            // Extract last block
            if (breakOffset != -1) {
                int afterSwitchOffset = breakOffset >= switchOffset ? breakOffset
                        : list.get(list.size() - 1).getOffset() + 1;

                // Reduction de 'afterSwitchOffset' via les 'Branch
                // Instructions'
                Instruction instruction;
                // Check previous instructions
                int i = switchIndex;
                while (i-- > 0) {
                    instruction = list.get(i);

                    if (ByteCodeUtil.isIfOrGotoInstruction(instruction.getOpcode(), true)
                     || instruction.getOpcode() == FastConstants.SWITCH
                     || instruction.getOpcode() == FastConstants.SWITCH_ENUM
                     || instruction.getOpcode() == FastConstants.SWITCH_STRING) {
                        int jumpOffset = ((BranchInstruction) instruction).getJumpOffset();
                        if (lastSwitchOffset < jumpOffset && jumpOffset < afterSwitchOffset) {
                            afterSwitchOffset = jumpOffset;
                        }
                    }
                }
                // Check next instructions
                i = list.size();
                while (i-- > 0) {
                    instruction = list.get(i);

                    if (ByteCodeUtil.isIfOrGotoInstruction(instruction.getOpcode(), true)
                     || instruction.getOpcode() == FastConstants.SWITCH
                     || instruction.getOpcode() == FastConstants.SWITCH_ENUM
                     || instruction.getOpcode() == FastConstants.SWITCH_STRING) {
                        int jumpOffset = ((BranchInstruction) instruction).getJumpOffset();
                        if (lastSwitchOffset < jumpOffset && jumpOffset < afterSwitchOffset) {
                            afterSwitchOffset = jumpOffset;
                        }
                    }

                    if (instruction.getOffset() <= afterSwitchOffset || instruction.getOffset() <= lastSwitchOffset) {
                        break;
                    }
                }

                // Extraction
                List<Instruction> instructions = null;

                while (index < list.size())
                {
                    instruction = list.get(index);
                    if (instruction.getOffset() >= afterSwitchOffset)
                    {
                        if (instructions != null)
                        {
                            int nbrInstructions = instructions.size();
                            if (nbrInstructions > 0)
                            {
                                instruction = instructions.get(nbrInstructions - 1);
                                if (instruction.getOpcode() == Const.GOTO) {
                                    // Replace goto by break;
                                    instructions.set(nbrInstructions - 1, new FastInstruction(FastConstants.GOTO_BREAK,
                                            instruction.getOffset(), instruction.getLineNumber(), null));
                                }
                            }
                        }
                        break;
                    }

                    if (instructions == null) {
                        instructions = new ArrayList<>();
                    }

                    list.remove(index);
                    instructions.add(instruction);
                    lastSwitchOffset = instruction.getOffset();
                }

                pairs[pairLength].setInstructions(instructions);
            }

            // Analyze instructions (recursive analyze)
            int beforeListOffset = test.getOffset();
            if (index < list.size()) {
                afterListOffset = list.get(index).getOffset();
            }

            Pair pair;
            List<Instruction> instructions;
            for (int i = 0; i <= pairLength; i++)
            {
                pair = pairs[i];
                instructions = pair.getInstructions();
                if (instructions != null)
                {
                    int nbrInstructions = instructions.size();
                    if (nbrInstructions > 0)
                    {
                        Instruction instruction = instructions.get(nbrInstructions - 1);
                        if (instruction.getOpcode() == FastConstants.GOTO_BREAK)
                        {
                            removeLastInstruction(instructions);
                            analyzeList(classFile, method, instructions, localVariables, offsetLabelSet,
                                    beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, beforeListOffset,
                                    afterListOffset, breakOffset, returnOffset);
                            instructions.add(instruction);
                        }
                        else
                        {
                            analyzeList(classFile, method, instructions, localVariables, offsetLabelSet,
                                    beforeLoopEntryOffset, loopEntryOffset, afterBodyLoopOffset, beforeListOffset,
                                    afterListOffset, breakOffset, returnOffset);

                            nbrInstructions = instructions.size();
                            if (nbrInstructions > 0)
                            {
                                instruction = instructions.get(nbrInstructions - 1);

                                if (ByteCodeUtil.isIfOrGotoInstruction(instruction.getOpcode(), true)
                                 || instruction.getOpcode() == FastConstants.IF_SIMPLE
                                 || instruction.getOpcode() == FastConstants.IF_ELSE
                                 || instruction.getOpcode() == FastConstants.SWITCH
                                 || instruction.getOpcode() == FastConstants.SWITCH_ENUM
                                 || instruction.getOpcode() == FastConstants.SWITCH_STRING) {
                                    int jumpOffset = ((BranchInstruction) instruction).getJumpOffset();
                                    if (jumpOffset < switchOffset || lastSwitchOffset < jumpOffset) {
                                        instructions.add(new FastInstruction(FastConstants.GOTO_BREAK,
                                                lastSwitchOffset + 1, Instruction.UNKNOWN_LINE_NUMBER, null));
                                    }
                                }
                            }
                        }
                        beforeListOffset = instruction.getOffset();
                    }
                }
            }
        }

        // Create instruction
        int branch = breakOffset == -1 ? 1 : breakOffset - lastSwitchOffset;

        list.set(switchIndex, new FastSwitch(switchOpcode, lastSwitchOffset, switchLineNumber, branch, test, pairs));
    }

    private static void removeLastInstruction(List<Instruction> instructions) {
        instructions.remove(instructions.size() - 1);
    }

    private static void addLabels(List<Instruction> list, IntSet offsetLabelSet) {
        for (int i = offsetLabelSet.size() - 1; i >= 0; --i) {
            searchInstructionAndAddLabel(list, offsetLabelSet.get(i));
        }
    }

    /**
     * @param list
     * @param labelOffset
     * @return false si aucune instruction ne correspond et true sinon.
     */
    private static boolean searchInstructionAndAddLabel(List<Instruction> list, int labelOffset) {
        int index = InstructionUtil.getIndexForOffset(list, labelOffset);

        if (index < 0) {
            return false;
        }

        boolean found = false;
        Instruction instruction = list.get(index);

        switch (instruction.getOpcode()) {
        case FastConstants.INFINITE_LOOP: {
            List<Instruction> instructions = ((FastList) instruction).getInstructions();
            if (instructions != null) {
                found = searchInstructionAndAddLabel(instructions, labelOffset);
            }
        }
            break;
        case FastConstants.WHILE,
             FastConstants.DO_WHILE,
             FastConstants.IF_SIMPLE: {
            FastTestList ftl = (FastTestList) instruction;
            if (labelOffset >= ftl.getTest().getOffset() && ftl.getInstructions() != null) {
                found = searchInstructionAndAddLabel(ftl.getInstructions(), labelOffset);
            }
            }
            break;
        case FastConstants.SYNCHRONIZED: {
            FastSynchronized fs = (FastSynchronized) instruction;
            if (labelOffset >= fs.getMonitor().getOffset() && fs.getInstructions() != null) {
                found = searchInstructionAndAddLabel(fs.getInstructions(), labelOffset);
            }
        }
            break;
        case FastConstants.FOR: {
            FastFor ff = (FastFor) instruction;
            if ((ff.getInit() == null || labelOffset >= ff.getInit().getOffset()) && ff.getInstructions() != null) {
                found = searchInstructionAndAddLabel(ff.getInstructions(), labelOffset);
            }
        }
            break;
        case FastConstants.IF_ELSE: {
            FastTest2Lists ft2l = (FastTest2Lists) instruction;
            if (labelOffset >= ft2l.getTest().getOffset()) {
                found = searchInstructionAndAddLabel(ft2l.getInstructions(), labelOffset)
                        || searchInstructionAndAddLabel(ft2l.getInstructions2(), labelOffset);
            }
        }
            break;
        case FastConstants.SWITCH,
             FastConstants.SWITCH_ENUM,
             FastConstants.SWITCH_STRING: {
            FastSwitch fs = (FastSwitch) instruction;
            if (labelOffset >= fs.getTest().getOffset()) {
                Pair[] pairs = fs.getPairs();
                if (pairs != null) {
                    List<Instruction> instructions;
                    for (int i = pairs.length - 1; i >= 0 && !found; --i) {
                        instructions = pairs[i].getInstructions();
                        if (instructions != null) {
                            found = searchInstructionAndAddLabel(instructions, labelOffset);
                        }
                    }
                }
            }
        }
            break;
        case FastConstants.TRY: {
            FastTry ft = (FastTry) instruction;
            found = searchInstructionAndAddLabel(ft.getInstructions(), labelOffset);

            if (!found && ft.getCatches() != null) {
                for (int i = ft.getCatches().size() - 1; i >= 0 && !found; --i) {
                    found = searchInstructionAndAddLabel(ft.getCatches().get(i).instructions(), labelOffset);
                }
            }

            if (!found && ft.getFinallyInstructions() != null) {
                found = searchInstructionAndAddLabel(ft.getFinallyInstructions(), labelOffset);
            }
        }
        }

        if (!found) {
            list.set(index, new FastLabel(FastConstants.LABEL, labelOffset, instruction.getLineNumber(), instruction));
        }

        return true;
    }
}
