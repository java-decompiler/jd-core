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
import org.jd.core.v1.model.classfile.attribute.CodeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.AStore;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
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
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;
import jd.core.process.analyzer.instruction.fast.visitor.CheckLocalVariableUsedVisitor;
import jd.core.process.analyzer.instruction.fast.visitor.FastCompareInstructionVisitor;
import jd.core.process.analyzer.util.InstructionUtil;
import jd.core.util.IntSet;
import jd.core.util.UtilConstants;

/** Aglomeration des informations 'CodeException'. */
public final class FastCodeExceptionAnalyzer
{
    private FastCodeExceptionAnalyzer() {
    }

    public static List<FastCodeExcepcion> aggregateCodeExceptions(
            Method method, List<Instruction> list)
    {
        CodeException[] arrayOfCodeException = method.getCodeExceptions();

        if (arrayOfCodeException == null || arrayOfCodeException.length == 0) {
            return null;
        }

        // Aggregation des 'finally' et des 'catch' executant le même bloc
        List<FastAggregatedCodeExcepcion> fastAggregatedCodeExceptions =
                new ArrayList<>(
                        arrayOfCodeException.length);
        populateListOfFastAggregatedCodeException(
                method, list, fastAggregatedCodeExceptions);

        int length = fastAggregatedCodeExceptions.size();
        List<FastCodeExcepcion> fastCodeExceptions =
                new ArrayList<>(length);

        // Aggregation des blocs 'finally' aux blocs 'catch'
        // Add first
        fastCodeExceptions.add(newFastCodeException(
                list, fastAggregatedCodeExceptions.get(0)));

        FastAggregatedCodeExcepcion fastAggregatedCodeException;
        // Add or update
        for (int i=1; i<length; ++i)
        {
            fastAggregatedCodeException = fastAggregatedCodeExceptions.get(i);

            // Update 'FastCodeException' for 'codeException'
            if (!updateFastCodeException(
                    fastCodeExceptions, fastAggregatedCodeException)) {
                // Not found -> Add new entry
                fastCodeExceptions.add(newFastCodeException(
                        list, fastAggregatedCodeException));
            }
        }

        // Sort by 1)tryFromOffset 2)maxOffset 3)tryToOffset
        // Necessaire pour le calcul de 'afterOffset' des structures try-catch
        // par 'ComputeAfterOffset'
        Collections.sort(fastCodeExceptions);

        FastCodeExcepcion fce1;
        FastCodeExcepcion fce2;
        // Aggregation des blocs 'catch'
        // Reduce of FastCodeException after UpdateFastCodeException(...)
        for (int i=fastCodeExceptions.size()-1; i>=1; --i)
        {
            fce1 = fastCodeExceptions.get(i);
            fce2 = fastCodeExceptions.get(i-1);

            if (fce1.getTryFromOffset() == fce2.getTryFromOffset() &&
                    fce1.getTryToOffset() == fce2.getTryToOffset() &&
                    fce1.hasSynchronizedFlag() == fce2.hasSynchronizedFlag() &&
                    (fce1.getAfterOffset() == UtilConstants.INVALID_OFFSET || fce1.getAfterOffset() > fce2.maxOffset) &&
                    (fce2.getAfterOffset() == UtilConstants.INVALID_OFFSET || fce2.getAfterOffset() > fce1.maxOffset))
            {
                // Append catches
                fce2.getCatches().addAll(fce1.getCatches());
                Collections.sort(fce2.getCatches());
                // Append finally
                if (fce2.nbrFinally == 0)
                {
                    fce2.setFinallyFromOffset(fce1.getFinallyFromOffset());
                    fce2.nbrFinally        = fce1.nbrFinally;
                }
                // Update 'maxOffset'
                if (fce2.maxOffset < fce1.maxOffset) {
                    fce2.maxOffset = fce1.maxOffset;
                }
                // Update 'afterOffset'
                if (fce2.getAfterOffset() == UtilConstants.INVALID_OFFSET ||
                        fce1.getAfterOffset() != UtilConstants.INVALID_OFFSET &&
                        fce1.getAfterOffset() < fce2.getAfterOffset()) {
                    fce2.setAfterOffset(fce1.getAfterOffset());
                }
                // Remove last FastCodeException
                fastCodeExceptions.remove(i);
            }
        }

        // Search 'switch' instructions, sort case offset
        List<int[]> switchCaseOffsets = searchSwitchCaseOffsets(list);

        FastCodeExcepcion fce;
        for (int i=fastCodeExceptions.size()-1; i>=0; --i)
        {
            fce = fastCodeExceptions.get(i);

            // Determine type
            defineType(list, fce);

            if (fce.getType() == FastConstants.TYPE_UNDEFINED) {
                System.err.println("Undefined type catch");
            }

            // Compute afterOffset
            computeAfterOffset(
                    method, list, switchCaseOffsets, fastCodeExceptions, fce, i);

            length = list.size();
            if (fce.getAfterOffset() == UtilConstants.INVALID_OFFSET && length > 0)
            {
                Instruction lastInstruction = list.get(length-1);
                fce.setAfterOffset(lastInstruction.getOffset());

                if (lastInstruction.getOpcode() != Const.RETURN &&
                        lastInstruction.getOpcode() != ByteCodeConstants.XRETURN) {
                    // Set afterOffset to a virtual instruction after list.
                    fce.setAfterOffset(fce.getAfterOffset() + 1);
                }
            }
        }

        // Sort by 1)tryFromOffset 2)maxOffset 3)tryToOffset
        Collections.sort(fastCodeExceptions);

        return fastCodeExceptions;
    }

    private static void populateListOfFastAggregatedCodeException(
            Method method, List<Instruction> list,
            List<FastAggregatedCodeExcepcion> fastAggregatedCodeExceptions)
    {
        int length = method.getCode().length;
        if (length == 0) {
            return;
        }

        FastAggregatedCodeExcepcion[] array =
                new FastAggregatedCodeExcepcion[length];

        CodeException[] arrayOfCodeException = method.getCodeExceptions();
        length = arrayOfCodeException.length;
        CodeException codeException;
        for (int i=0; i<length; i++)
        {
            codeException = arrayOfCodeException[i];

            if (array[codeException.handlerPc()] == null)
            {
                FastAggregatedCodeExcepcion face =
                        new FastAggregatedCodeExcepcion(codeException.index(), codeException.startPc(), codeException.endPc(),
                                codeException.handlerPc(), codeException.catchType());
                fastAggregatedCodeExceptions.add(face);
                array[codeException.handlerPc()] = face;
            }
            else
            {
                FastAggregatedCodeExcepcion face = array[codeException.handlerPc()];
                // ATTENTION: la modification de 'endPc' implique la
                //            reecriture de 'defineType(...) !!
                if (face.getCatchType() == 0)
                {
                    face.nbrFinally++;
                } else // Ce type d'exception a-t-il deja ete ajoute ?
                    if (isNotAlreadyStored(face, codeException.catchType()))
                    {
                        // Non
                        if (face.otherCatchTypes == null) {
                            face.otherCatchTypes = new int[length];
                        }
                        face.otherCatchTypes[i] = codeException.catchType();
                    }
            }
        }

        int i = fastAggregatedCodeExceptions.size();
        FastAggregatedCodeExcepcion face;
        while (i-- > 0)
        {
            face = fastAggregatedCodeExceptions.get(i);

            if (face.getCatchType() == 0 && isASynchronizedBlock(list, face))
            {
                face.synchronizedFlag = true;
            }
        }
    }

    private static boolean isNotAlreadyStored(
            FastAggregatedCodeExcepcion face, int catchType)
    {
        if (face.getCatchType() == catchType) {
            return false;
        }

        if (face.otherCatchTypes != null)
        {
            int i = face.otherCatchTypes.length;

            while (i-- > 0)
            {
                if (face.otherCatchTypes[i] == catchType) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isASynchronizedBlock(
            List<Instruction> list, FastAggregatedCodeExcepcion face)
    {
        int index = InstructionUtil.getIndexForOffset(list, face.getStartPC());

        if (index == -1) {
            return false;
        }

        if (list.get(index).getOpcode() == Const.MONITOREXIT)
        {
            // Cas particulier Jikes 1.2.2
            return true;
        }

        if (index < 1) {
            return false;
        }

        /* Recherche si le bloc finally contient une instruction
         * monitorexit ayant le même index que l'instruction
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

        if (instruction.getOpcode() != Const.MONITORENTER) {
            return false;
        }

        int varMonitorIndex;
        MonitorEnter monitorEnter = (MonitorEnter)instruction;
        switch (monitorEnter.getObjectref().getOpcode())
        {
        case Const.ALOAD:
        {
            if (index < 2) {
                return false;
            }
            instruction = list.get(index-2);
            if (instruction.getOpcode() != Const.ASTORE) {
                return false;
            }
            AStore astore = (AStore)instruction;
            varMonitorIndex = astore.getIndex();
        }
        break;
        case ByteCodeConstants.ASSIGNMENT:
        {
            AssignmentInstruction ai =
                    (AssignmentInstruction)monitorEnter.getObjectref();
            if (ai.getValue1().getOpcode() != Const.ALOAD) {
                return false;
            }
            ALoad aload = (ALoad)ai.getValue1();
            varMonitorIndex = aload.getIndex();
        }
        break;
        default:
            return false;
        }

        boolean checkMonitorExit = false;
        int length = list.size();
        index = InstructionUtil.getIndexForOffset(list, face.getHandlerPC());

        while (index < length)
        {
            instruction = list.get(index);
            index++;

            if (instruction.getOpcode() == Const.MONITOREXIT) {
                checkMonitorExit = true;
                MonitorExit monitorExit = (MonitorExit)instruction;

                if (monitorExit.getObjectref().getOpcode() == Const.ALOAD &&
                        ((ALoad)monitorExit.getObjectref()).getIndex() == varMonitorIndex) {
                    return true;
                }
            } else if (instruction.getOpcode() == Const.RETURN
                    || instruction.getOpcode() == ByteCodeConstants.XRETURN
                    || instruction.getOpcode() == Const.ATHROW) {
                return false;
            }
        }
        // Si l'expression ci-dessous est vraie, aucune instruction 'MonitorExit' n'a ete trouvée. Cas de la
        // double instruction 'synchronized' imbriquée pour le JDK 1.1.8
        return !checkMonitorExit && index == length;
    }

    private static boolean updateFastCodeException(
            List<FastCodeExcepcion> fastCodeExceptions,
            FastAggregatedCodeExcepcion fastAggregatedCodeException)
    {
        int length = fastCodeExceptions.size();

        if (fastAggregatedCodeException.getCatchType() == 0)
        {
            // Finally

            // Same start and end offsets
            for (int i=0; i<length; ++i)
            {
                FastCodeExcepcion fce = fastCodeExceptions.get(i);

                if (fce.getFinallyFromOffset() == UtilConstants.INVALID_OFFSET &&
                        fastAggregatedCodeException.getStartPC() == fce.getTryFromOffset() &&
                        fastAggregatedCodeException.getEndPC() == fce.getTryToOffset() &&
                        fastAggregatedCodeException.getHandlerPC() > fce.maxOffset &&
                        !fastAggregatedCodeException.synchronizedFlag && (fce.getAfterOffset() == UtilConstants.INVALID_OFFSET ||
                        fastAggregatedCodeException.getEndPC() < fce.getAfterOffset() &&
                        fastAggregatedCodeException.getHandlerPC() < fce.getAfterOffset()))
                {
                    fce.maxOffset = fastAggregatedCodeException.getHandlerPC();
                    fce.setFinallyFromOffset(fastAggregatedCodeException.getHandlerPC());
                    fce.nbrFinally += fastAggregatedCodeException.nbrFinally;
                    return true;
                }
            }

            FastCodeExcepcion fce;
            // Old algo
            for (int i=0; i<length; ++i)
            {
                fce = fastCodeExceptions.get(i);

                if (fce.getFinallyFromOffset() == UtilConstants.INVALID_OFFSET &&
                        fastAggregatedCodeException.getStartPC() == fce.getTryFromOffset() &&
                        fastAggregatedCodeException.getEndPC() >= fce.getTryToOffset() &&
                        fastAggregatedCodeException.getHandlerPC() > fce.maxOffset &&
                        !fastAggregatedCodeException.synchronizedFlag && (fce.getAfterOffset() == UtilConstants.INVALID_OFFSET ||
                        fastAggregatedCodeException.getEndPC() < fce.getAfterOffset() &&
                        fastAggregatedCodeException.getHandlerPC() < fce.getAfterOffset()))
                {
                    fce.maxOffset = fastAggregatedCodeException.getHandlerPC();
                    fce.setFinallyFromOffset(fastAggregatedCodeException.getHandlerPC());
                    fce.nbrFinally += fastAggregatedCodeException.nbrFinally;
                    return true;
                }
                /* Mis en commentaire a cause d'erreurs pour le jdk1.5.0 dans
                 * TryCatchFinallyClass.complexMethodTryCatchCatchFinally()
                 *
                 * else if ((fce.catches != null) &&
                         (fce.afterOffset == fastAggregatedCodeException.endPc))
                {
                    fce.finallyFromOffset = fastAggregatedCodeException.handlerPc;
                    fce.nbrFinally += fastAggregatedCodeException.nbrFinally;
                    return true;
                } */
            }
        }

        return false;
    }

    private static FastCodeExcepcion newFastCodeException(
            List<Instruction> list, FastAggregatedCodeExcepcion fastCodeException)
    {
        FastCodeExcepcion fce = new FastCodeExcepcion(
                fastCodeException.getStartPC(),
                fastCodeException.getEndPC(),
                fastCodeException.getHandlerPC(),
                fastCodeException.synchronizedFlag);

        if (fastCodeException.getCatchType() == 0)
        {
            fce.setFinallyFromOffset(fastCodeException.getHandlerPC());
            fce.nbrFinally += fastCodeException.nbrFinally;
        }
        else
        {
            fce.getCatches().add(new FastCodeExceptionCatch(
                    fastCodeException.getCatchType(),
                    fastCodeException.otherCatchTypes,
                    fastCodeException.getHandlerPC()));
        }

        // Approximation a affinée par la méthode 'ComputeAfterOffset'
        fce.setAfterOffset(searchAfterOffset(list, fastCodeException.getHandlerPC()));

        return fce;
    }

    /** Recherche l'offset après le bloc try-catch-finally. */
    private static int searchAfterOffset(List<Instruction> list, int offset)
    {
        // Search instruction at 'offset'
        int index = InstructionUtil.getIndexForOffset(list, offset);

        if (index <= 0) {
            return offset;
        }

        index--;
        // Search previous 'goto' instruction
        Instruction i = list.get(index);
        switch (i.getOpcode())
        {
        case Const.GOTO:
            int branch = ((Goto)i).getBranch();
            if (branch < 0) {
                return UtilConstants.INVALID_OFFSET;
            }
            int jumpOffset = i.getOffset() + branch;
            index = InstructionUtil.getIndexForOffset(list, jumpOffset);
            if (index <= 0) {
                return UtilConstants.INVALID_OFFSET;
            }
            i = list.get(index);
            if (i.getOpcode() != Const.JSR) {
                return jumpOffset;
            }
            branch = ((Jsr)i).getBranch();
            if (branch > 0) {
                return i.getOffset() + branch;
            }
            return jumpOffset+1;

        case Const.RET:
            // Particularite de la structure try-catch-finally du JDK 1.1.8:
            // une sous routine termine le bloc precedent 'offset'.
            // Strategie : recheche de l'instruction goto, sautant après
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
                if (list.get(index).getOpcode() == Const.ATHROW &&
                        list.get(index-1).getOpcode() == Const.JSR &&
                        list.get(index-2).getOpcode() == Const.ASTORE &&
                        list.get(index-3).getOpcode() == Const.GOTO)
                {
                    Goto g = (Goto)list.get(index-3);
                    return g.getJumpOffset();
                }
            }
            // intended fall through
        default:
            return UtilConstants.INVALID_OFFSET;
        }
    }

    private static List<int[]> searchSwitchCaseOffsets(
            List<Instruction> list)
    {
        List<int[]> switchCaseOffsets = new ArrayList<>();

        int i = list.size();
        Instruction instruction;
        while (i-- > 0)
        {
            instruction = list.get(i);

            if (instruction.getOpcode() == Const.TABLESWITCH || instruction.getOpcode() == Const.LOOKUPSWITCH) {
                Switch s = (Switch)instruction;
                int j = s.getOffsets().length;
                int[] offsets = new int[j+1];

                offsets[j] = s.getOffset() + s.getDefaultOffset();
                while (j-- > 0) {
                    offsets[j] = s.getOffset() + s.getOffset(j);
                }

                Arrays.sort(offsets);
                switchCaseOffsets.add(offsets);
            }
        }

        return switchCaseOffsets;
    }

    private static void defineType(
            List<Instruction> list, FastCodeExcepcion fastCodeException)
    {
        // Contains finally ?
        switch (fastCodeException.nbrFinally)
        {
        case 0:
            // No
            fastCodeException.setType(FastConstants.TYPE_CATCH);
            break;
        case 1:
            // 1.1.8, 1.3.1, 1.4.2 or eclipse 677
            // Yes, contains catch ?
            if (fastCodeException.getCatches() == null    ||
            fastCodeException.getCatches().isEmpty())
            {
                // No
                int index = InstructionUtil.getIndexForOffset(
                        list, fastCodeException.getFinallyFromOffset());
                if (index < 0) {
                    return;
                }

                // Search 'goto' instruction
                Instruction instruction = list.get(index-1);
                switch (instruction.getOpcode())
                {
                case Const.GOTO:
                    if (tryBlockContainsJsr(list, fastCodeException))
                    {
                        fastCodeException.setType(FastConstants.TYPE_118_FINALLY);
                    } else // Search previous 'goto' instruction
                        if (list.get(index-2).getOpcode() == Const.MONITOREXIT) {
                            fastCodeException.setType(FastConstants.TYPE_118_SYNCHRONIZED);
                        } else {
                            // TYPE_ECLIPSE_677_FINALLY or TYPE_118_FINALLY_2 ?
                            int jumpOffset = ((Goto)instruction).getJumpOffset();
                            instruction =
                                    InstructionUtil.getInstructionAt(list, jumpOffset);

                            if (instruction.getOpcode() == Const.JSR) {
                                fastCodeException.setType(FastConstants.TYPE_118_FINALLY_2);
                            } else {
                                fastCodeException.setType(FastConstants.TYPE_ECLIPSE_677_FINALLY);
                            }
                        }
                    break;
                case Const.RETURN,
                     ByteCodeConstants.XRETURN:
                    if (tryBlockContainsJsr(list, fastCodeException))
                    {
                        fastCodeException.setType(FastConstants.TYPE_118_FINALLY);
                    } else // Search previous 'return' instruction
                        if (list.get(index-2).getOpcode() == Const.MONITOREXIT) {
                            fastCodeException.setType(FastConstants.TYPE_118_SYNCHRONIZED);
                        } else {
                            // TYPE_ECLIPSE_677_FINALLY or TYPE_142 ?
                            Instruction firstFinallyInstruction = list.get(index+1);
                            int exceptionIndex = ((AStore)list.get(index)).getIndex();
                            int length = list.size();

                            // Search throw instruction
                            while (++index < length)
                            {
                                instruction = list.get(index);
                                if (instruction.getOpcode() == Const.ATHROW)
                                {
                                    AThrow athrow = (AThrow)instruction;
                                    if (athrow.getValue().getOpcode() == Const.ALOAD &&
                                            ((ALoad)athrow.getValue()).getIndex() == exceptionIndex) {
                                        break;
                                    }
                                }
                            }

                            if (++index >= length)
                            {
                                fastCodeException.setType(FastConstants.TYPE_142);
                            }
                            else
                            {
                                instruction = list.get(index);

                                fastCodeException.setType(instruction.getOpcode() != firstFinallyInstruction.getOpcode() ||
                                firstFinallyInstruction.getLineNumber() == Instruction.UNKNOWN_LINE_NUMBER ||
                                firstFinallyInstruction.getLineNumber() != instruction.getLineNumber() ?
                                        FastConstants.TYPE_142 :
                                            FastConstants.TYPE_ECLIPSE_677_FINALLY);
                            }
                        }
                    break;
                case Const.ATHROW:
                    // Search 'jsr' instruction after 'astore' instruction
                    if (list.get(index+1).getOpcode() == Const.JSR) {
                        fastCodeException.setType(FastConstants.TYPE_118_FINALLY_THROW);
                    } else if (list.get(index).getOpcode() ==
                            Const.MONITOREXIT) {
                        fastCodeException.setType(FastConstants.TYPE_118_FINALLY);
                    } else {
                        fastCodeException.setType(FastConstants.TYPE_142_FINALLY_THROW);
                    }
                    break;
                case Const.RET:
                    // Double synchronized blocks compiled with the JDK 1.1.8
                    fastCodeException.setType(FastConstants.TYPE_118_SYNCHRONIZED_DOUBLE);
                    break;
                }
            }
            else
            {
                // Yes, contains catch(s) & finally
                int index = InstructionUtil.getIndexForOffset(
                        list, fastCodeException.getCatches().get(0).getFromOffset());
                if (index < 0) {
                    return;
                }

                index--;
                // Search 'goto' instruction in try block
                Instruction instruction = list.get(index);
                if (instruction.getOpcode() == Const.GOTO)
                {
                    Goto g = (Goto)instruction;

                    index--;
                    // Search previous 'goto' instruction
                    instruction = list.get(index);
                    if (instruction.getOpcode() == Const.JSR)
                    {
                        fastCodeException.setType(FastConstants.TYPE_131_CATCH_FINALLY);
                    }
                    else
                    {
                        // Search jump 'goto' instruction
                        index = InstructionUtil.getIndexForOffset(
                                list, g.getJumpOffset());
                        instruction = list.get(index);

                        if (instruction.getOpcode() == Const.JSR)
                        {
                            fastCodeException.setType(FastConstants.TYPE_118_CATCH_FINALLY);
                        }
                        else
                        {
                            instruction = list.get(index - 1);

                            if (instruction.getOpcode() == Const.ATHROW) {
                                fastCodeException.setType(FastConstants.TYPE_ECLIPSE_677_CATCH_FINALLY);
                            } else {
                                fastCodeException.setType(FastConstants.TYPE_118_CATCH_FINALLY_2);
                            }
                        }
                    }
                } else if (instruction.getOpcode() == Const.RET)
                {
                    fastCodeException.setType(FastConstants.TYPE_118_CATCH_FINALLY);
                }
                else
                {
                    index--;
                    // Search previous instruction
                    instruction = list.get(index);
                    if (instruction.getOpcode() == Const.JSR)
                    {
                        fastCodeException.setType(FastConstants.TYPE_131_CATCH_FINALLY);
                    }
                }
            }
            break;
        default:
            // 1.3.1, 1.4.2, 1.5.0, jikes 1.2.2 or eclipse 677
            // Yes, contains catch ?
            if (fastCodeException.getCatches() == null    ||
            fastCodeException.getCatches().isEmpty())
            {
                // No, 1.4.2 or jikes 1.2.2 ?
                int index = InstructionUtil.getIndexForOffset(
                        list, fastCodeException.getTryToOffset());
                if (index < 0) {
                    return;
                }

                Instruction instruction = list.get(index);
                switch (instruction.getOpcode())
                {
                case Const.JSR:
                    fastCodeException.setType(FastConstants.TYPE_131_CATCH_FINALLY);
                    break;
                case Const.ATHROW:
                    fastCodeException.setType(FastConstants.TYPE_JIKES_122);
                    break;
                case Const.GOTO:
                    Goto g = (Goto)instruction;

                    // Search previous 'goto' instruction
                    instruction = InstructionUtil.getInstructionAt(
                            list, g.getJumpOffset());
                    if (instruction == null) {
                        return;
                    }

                    if (instruction.getOpcode() == Const.JSR &&
                            ((Jsr)instruction).getBranch() < 0)
                    {
                        fastCodeException.setType(FastConstants.TYPE_JIKES_122);
                    } else if (index > 0 && list.get(index-1).getOpcode() == Const.JSR) {
                        fastCodeException.setType(FastConstants.TYPE_131_CATCH_FINALLY);
                    } else {
                        fastCodeException.setType(FastConstants.TYPE_142);
                    }
                    break;
                case Const.POP:
                    defineTypeJikes122Or142(
                            list, fastCodeException, ((Pop)instruction).getObjectref(), index);
                    break;
                case Const.ASTORE:
                    defineTypeJikes122Or142(
                            list, fastCodeException, ((AStore)instruction).getValueref(), index);
                    break;
                case Const.RETURN,
                     ByteCodeConstants.XRETURN:
                    // 1.3.1, 1.4.2 or jikes 1.2.2 ?
                    if (index > 0 && list.get(index-1).getOpcode() == Const.JSR) {
                        fastCodeException.setType(FastConstants.TYPE_131_CATCH_FINALLY);
                    } else {
                        fastCodeException.setType(FastConstants.TYPE_142);
                    }
                    break;
                default:
                    fastCodeException.setType(FastConstants.TYPE_142);
                }
            }
            else
            {
                // Yes, contains catch(s) & multiple finally
                // Control que toutes les instructions 'goto' sautent sur la
                // même instruction.
                boolean uniqueJumpAddressFlag = true;
                int uniqueJumpAddress = -1;

                if (fastCodeException.getCatches() != null)
                {
                    FastCodeExceptionCatch fcec;
                    int index;
                    for (int i=fastCodeException.getCatches().size()-1; i>=0; --i)
                    {
                        fcec = fastCodeException.getCatches().get(i);
                        index = InstructionUtil.getIndexForOffset(
                                list, fcec.getFromOffset());
                        if (index != -1)
                        {
                            Instruction instruction = list.get(index-1);
                            if (instruction.getOpcode() == Const.GOTO)
                            {
                                int branch  = ((Goto)instruction).getBranch();
                                if (branch > 0)
                                {
                                    int jumpAddress = instruction.getOffset() + branch;
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
                        list, fastCodeException.getFinallyFromOffset());
                if (index < 0) {
                    return;
                }

                index--;
                Instruction instruction = list.get(index);

                if (uniqueJumpAddressFlag &&
                        instruction.getOpcode() == Const.GOTO)
                {
                    int branch  = ((Goto)instruction).getBranch();
                    if (branch > 0)
                    {
                        int jumpAddress = instruction.getOffset() + branch;
                        if (uniqueJumpAddress == -1) {
                            uniqueJumpAddress = jumpAddress;
                        } else if (uniqueJumpAddress != jumpAddress) {
                            uniqueJumpAddressFlag = false;
                        }
                    }
                }

                if (!uniqueJumpAddressFlag)
                {
                    fastCodeException.setType(FastConstants.TYPE_ECLIPSE_677_CATCH_FINALLY);
                    return;
                }

                index = InstructionUtil.getIndexForOffset(
                        list, fastCodeException.getTryToOffset());
                if (index < 0) {
                    return;
                }

                instruction = list.get(index);

                switch (instruction.getOpcode())
                {
                case Const.JSR:
                    fastCodeException.setType(FastConstants.TYPE_131_CATCH_FINALLY);
                    break;
                case Const.ATHROW:
                    fastCodeException.setType(FastConstants.TYPE_JIKES_122);
                    break;
                case Const.GOTO:
                    Goto g = (Goto)instruction;

                    // Search previous 'goto' instruction
                    instruction = InstructionUtil.getInstructionAt(
                            list, g.getJumpOffset());
                    if (instruction == null) {
                        return;
                    }

                    if (instruction.getOpcode() == Const.JSR &&
                            ((Jsr)instruction).getBranch() < 0)
                    {
                        fastCodeException.setType(FastConstants.TYPE_JIKES_122);
                    } else if (index > 0 && list.get(index-1).getOpcode() == Const.JSR) {
                        fastCodeException.setType(FastConstants.TYPE_131_CATCH_FINALLY);
                    } else {
                        fastCodeException.setType(FastConstants.TYPE_142);
                    }
                    break;
                case Const.POP:
                    defineTypeJikes122Or142(
                            list, fastCodeException, ((Pop)instruction).getObjectref(), index);
                    break;
                case Const.ASTORE:
                    defineTypeJikes122Or142(
                            list, fastCodeException, ((AStore)instruction).getValueref(), index);
                    break;
                case Const.RETURN,
                     ByteCodeConstants.XRETURN:
                    // 1.3.1, 1.4.2 or jikes 1.2.2 ?
                    instruction = InstructionUtil.getInstructionAt(
                            list, uniqueJumpAddress);
                    if (instruction != null &&
                            instruction.getOpcode() == Const.JSR &&
                            ((Jsr)instruction).getBranch() < 0) {
                        fastCodeException.setType(FastConstants.TYPE_JIKES_122);
                    } else if (index > 0 && list.get(index-1).getOpcode() == Const.JSR) {
                        fastCodeException.setType(FastConstants.TYPE_131_CATCH_FINALLY);
                    } else {
                        fastCodeException.setType(FastConstants.TYPE_142);
                    }
                    break;
                default:
                    // TYPE_ECLIPSE_677_FINALLY or TYPE_142 ?
                    index = InstructionUtil.getIndexForOffset(
                            list, fastCodeException.getFinallyFromOffset());
                    Instruction firstFinallyInstruction = list.get(index+1);

                    if (firstFinallyInstruction.getOpcode() != Const.ASTORE)
                    {
                        fastCodeException.setType(FastConstants.TYPE_142);
                    }
                    else
                    {
                        int exceptionIndex = ((AStore)list.get(index)).getIndex();
                        int length = list.size();

                        // Search throw instruction
                        while (++index < length)
                        {
                            instruction = list.get(index);
                            if (instruction.getOpcode() == Const.ATHROW)
                            {
                                AThrow athrow = (AThrow)instruction;
                                if (athrow.getValue().getOpcode() == Const.ALOAD &&
                                        ((ALoad)athrow.getValue()).getIndex() == exceptionIndex) {
                                    break;
                                }
                            }
                        }

                        if (++index >= length)
                        {
                            fastCodeException.setType(FastConstants.TYPE_142);
                        }
                        else
                        {
                            instruction = list.get(index);

                            fastCodeException.setType(instruction.getOpcode() != firstFinallyInstruction.getOpcode() ||
                            firstFinallyInstruction.getLineNumber() == Instruction.UNKNOWN_LINE_NUMBER ||
                            firstFinallyInstruction.getLineNumber() != instruction.getLineNumber() ?
                                    FastConstants.TYPE_142 :
                                        FastConstants.TYPE_ECLIPSE_677_CATCH_FINALLY);
                        }
                    }
                }
            }
        }
    }

    private static boolean tryBlockContainsJsr(
            List<Instruction> list, FastCodeExcepcion fastCodeException)
    {
        int index = InstructionUtil.getIndexForOffset(
                list, fastCodeException.getTryToOffset());

        if (index != -1)
        {
            int tryFromOffset = fastCodeException.getTryFromOffset();

            Instruction instruction;
            for (;;)
            {
                instruction = list.get(index);

                if (instruction.getOffset() <= tryFromOffset)
                {
                    break;
                }

                if (instruction.getOpcode() == Const.JSR && ((Jsr)instruction).getJumpOffset() >
                fastCodeException.getFinallyFromOffset())
                {
                    return true;
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

    private static void defineTypeJikes122Or142(
            List<Instruction> list, FastCodeExcepcion fastCodeException,
            Instruction instruction, int index)
    {
        if (instruction.getOpcode() == ByteCodeConstants.EXCEPTIONLOAD)
        {
            index--;
            instruction = list.get(index);

            if (instruction.getOpcode() == Const.GOTO)
            {
                int jumpAddress = ((Goto)instruction).getJumpOffset();

                instruction = InstructionUtil.getInstructionAt(list, jumpAddress);

                if (instruction != null &&
                        instruction.getOpcode() == Const.JSR)
                {
                    fastCodeException.setType(FastConstants.TYPE_JIKES_122);
                    return;
                }
            }
        }

        fastCodeException.setType(FastConstants.TYPE_142);
    }

    private static void computeAfterOffset(
            Method method, List<Instruction> list,
            List<int[]> switchCaseOffsets,
            List<FastCodeExcepcion> fastCodeExceptions,
            FastCodeExcepcion fastCodeException, int fastCodeExceptionIndex)
    {
        switch (fastCodeException.getType())
        {
        case FastConstants.TYPE_118_CATCH_FINALLY:
        {
            // Strategie : Trouver l'instruction suivant 'ret' de la sous
            // routine 'finally'.
            int index = InstructionUtil.getIndexForOffset(
                    list, fastCodeException.getAfterOffset());
            if (index < 0 || index >= list.size()) {
                return;
            }

            int length = list.size();
            IntSet offsetSet = new IntSet();
            int retCounter = 0;

            Instruction i;
            // Search 'ret' instruction
            // Permet de prendre en compte les sous routines imbriquées
            while (++index < length)
            {
                i = list.get(index);

                if (i.getOpcode() == Const.JSR) {
                    offsetSet.add(((Jsr)i).getJumpOffset());
                } else if (i.getOpcode() == Const.RET) {
                    if (offsetSet.size() == retCounter)
                    {
                        fastCodeException.setAfterOffset(i.getOffset() + 1);
                        return;
                    }
                    retCounter++;
                }
            }
        }
        break;
        case FastConstants.TYPE_118_CATCH_FINALLY_2:
        {
            Instruction instruction = InstructionUtil.getInstructionAt(
                    list, fastCodeException.getAfterOffset());
            if (instruction == null) {
                return;
            }

            fastCodeException.setAfterOffset(instruction.getOffset() + 1);
        }
        break;
        case FastConstants.TYPE_118_FINALLY_2:
        {
            int index = InstructionUtil.getIndexForOffset(
                    list, fastCodeException.getAfterOffset());
            if (index < 0 || index >= list.size()) {
                return;
            }

            index++;
            Instruction i = list.get(index);
            if (i.getOpcode() != Const.GOTO) {
                return;
            }

            fastCodeException.setAfterOffset(((Goto)i).getJumpOffset());
        }
        break;
        case FastConstants.TYPE_JIKES_122:
            // Le traitement suivant etait faux pour reconstruire la méthode
            // "basic.data.TestTryCatchFinally .methodTryFinally1()" compile
            // par "Eclipse Java Compiler v_677_R32x, 3.2.1 release".
            //            {
            //                int index = InstructionUtil.getIndexForOffset(
            //                        list, fastCodeException.afterOffset);
            //                if ((index < 0) || (index >= list.size()))
            //                    return;
            //                Instruction i = list.get(++index);
            //                fastCodeException.afterOffset = i.offset;
            //            }
            break;
        case FastConstants.TYPE_ECLIPSE_677_FINALLY:
        {
            int index = InstructionUtil.getIndexForOffset(
                    list, fastCodeException.getFinallyFromOffset());
            if (index < 0) {
                return;
            }

            int length = list.size();
            Instruction instruction = list.get(index);

            if (instruction.getOpcode() == Const.POP) {
                // Search the first throw instruction
                while (++index < length)
                {
                    instruction = list.get(index);
                    if (instruction.getOpcode() == Const.ATHROW)
                    {
                        fastCodeException.setAfterOffset(instruction.getOffset() + 1);
                        break;
                    }
                }
            } else if (instruction.getOpcode() == Const.ASTORE) {
                // L'un des deux cas les plus complexes :
                // - le bloc 'finally' est dupliqué deux fois.
                // - aucun 'goto' ne saute après le dernier bloc finally.
                // Methode de calcul de 'afterOffset' :
                // - compter le nombre d'instructions entre le début du 1er bloc
                //   'finally' et le saut du goto en fin de bloc 'try'.
                // - Ajouter ce nombre à  l'index de l'instruction vers laquelle
                //   saute le 'goto' precedent le 1er bloc 'finally'.
                int finallyStartIndex = index+1;
                int exceptionIndex = ((AStore)instruction).getIndex();

                // Search throw instruction
                while (++index < length)
                {
                    instruction = list.get(index);
                    if (instruction.getOpcode() == Const.ATHROW)
                    {
                        AThrow athrow = (AThrow)instruction;
                        if (athrow.getValue().getOpcode() == Const.ALOAD &&
                                ((ALoad)athrow.getValue()).getIndex() == exceptionIndex) {
                            break;
                        }
                    }
                }

                index += index - finallyStartIndex + 1;

                if (index < length) {
                    fastCodeException.setAfterOffset(list.get(index).getOffset());
                }
            }
        }
        break;
        case FastConstants.TYPE_ECLIPSE_677_CATCH_FINALLY:
        {
            // L'un des deux cas les plus complexes :
            // - le bloc 'finally' est dupliqué deux ou trois fois.
            // - aucun 'goto' ne saute après le dernier bloc finally.
            // Methode de calcul de 'afterOffset' :
            // - compter le nombre d'instructions entre le début du 1er bloc
            //   'finally' et le saut du goto en fin de bloc 'try'.
            // - Ajouter ce nombre à  l'index de l'instruction vers laquelle
            //   saute le 'goto' precedent le 1er bloc 'finally'.
            int index = InstructionUtil.getIndexForOffset(
                    list, fastCodeException.getFinallyFromOffset());
            if (index < 0) {
                return;
            }

            Instruction instruction = list.get(index);

            if (instruction.getOpcode() != Const.ASTORE) {
                return;
            }

            int finallyStartIndex = index+1;
            int exceptionIndex = ((AStore)instruction).getIndex();
            int length = list.size();

            // Search throw instruction
            while (++index < length)
            {
                instruction = list.get(index);
                if (instruction.getOpcode() == Const.ATHROW)
                {
                    AThrow athrow = (AThrow)instruction;
                    if (findAloadForAThrow(exceptionIndex, athrow))
                    {
                        break;
                    }
                }
            }

            int delta = index - finallyStartIndex;
            index += delta + 1;

            if (index < list.size()) {
                int afterOffset = list.get(index).getOffset();

                // Verification de la presence d'un bloc 'finally' pour les blocs
                // 'catch'.
                if (index < length &&
                        list.get(index).getOpcode() == Const.GOTO)
                {
                    Goto g = (Goto)list.get(index);
                    int jumpOffset = g.getJumpOffset();
                    int indexTmp = index + delta + 1;

                    if (indexTmp < length &&
                            list.get(indexTmp-1).getOffset() < jumpOffset &&
                            jumpOffset <= list.get(indexTmp).getOffset())
                    {
                        // Reduction de 'afterOffset' a l'aide des 'Branch Instructions'
                        afterOffset = reduceAfterOffsetWithBranchInstructions(
                                list, fastCodeException,
                                fastCodeException.getFinallyFromOffset(),
                                list.get(indexTmp).getOffset());

                        // Reduction de 'afterOffset' a l'aide des numéros de ligne
                        if (! fastCodeException.hasSynchronizedFlag())
                        {
                            afterOffset = reduceAfterOffsetWithLineNumbers(
                                    list, fastCodeException, afterOffset);
                        }

                        // Reduction de 'afterOffset' a l'aide des instructions de
                        // gestion des exceptions englobantes
                        afterOffset = reduceAfterOffsetWithExceptions(
                                fastCodeExceptions, fastCodeException.getTryFromOffset(),
                                fastCodeException.getFinallyFromOffset(), afterOffset);
                    }
                }

                fastCodeException.setAfterOffset(afterOffset);
            }
        }
        break;
        case FastConstants.TYPE_118_FINALLY:
        {
            // Re-estimation de la valeur de l'attribut 'afterOffset'.
            // Strategie : le bon offset, après le bloc 'try-finally', se
            // trouve après l'instruction 'ret' de la sous procedure du
            // bloc 'finally'.
            int index = InstructionUtil.getIndexForOffset(
                    list, fastCodeException.getFinallyFromOffset());
            if (index <= 0) {
                return;
            }

            int length = list.size();

            // Gestion des instructions JSR imbriquees
            int offsetOfJsrsLength = list.get(length-1).getOffset() + 1;
            boolean[] offsetOfJsrs = new boolean[offsetOfJsrsLength];
            int level = 0;

            Instruction i;
            while (++index < length)
            {
                i = list.get(index);

                if (offsetOfJsrs[i.getOffset()]) {
                    level++;
                }

                if (i.getOpcode() == Const.JSR)
                {
                    int jumpOffset = ((Jsr)i).getJumpOffset();
                    if (jumpOffset < offsetOfJsrsLength) {
                        offsetOfJsrs[jumpOffset] = true;
                    }
                }
                else if (i.getOpcode() == Const.RET)
                {
                    if (level <= 1)
                    {
                        fastCodeException.setAfterOffset(i.getOffset()+1);
                        break;
                    }
                    level--;
                }
            }
        }
        break;
        case FastConstants.TYPE_118_FINALLY_THROW,
             FastConstants.TYPE_131_CATCH_FINALLY:
        {
            int index = InstructionUtil.getIndexForOffset(
                    list, fastCodeException.getFinallyFromOffset());
            if (index <= 0) {
                return;
            }

            // Search last 'ret' instruction of the finally block
            int length = list.size();

            Instruction i;
            while (++index < length)
            {
                i = list.get(index);
                if (i.getOpcode() == Const.RET)
                {
                    fastCodeException.setAfterOffset(++index < length ?
                            list.get(index).getOffset() :
                                i.getOffset()+1);
                    break;
                }
            }
        }
        break;
        default:
        {
            int length = list.size();

            // Re-estimation de la valeur de l'attribut 'afterOffset'.
            // Strategie : parcours du bytecode jusqu'à  trouver une
            // instruction de saut vers la derniere instruction 'return',
            // ou une instruction 'athrow' ou une instruction de saut
            // négatif allant en deca du début du dernier block. Le parcours
            // du bytecode doit prendre en compte les sauts positifs.

            // Calcul de l'offset après la structure try-catch
            int afterOffset = fastCodeException.getAfterOffset();
            if (afterOffset == -1) {
                afterOffset = list.get(length-1).getOffset() + 1;
            }

            // Reduction de 'afterOffset' a l'aide des 'Branch Instructions'
            afterOffset = reduceAfterOffsetWithBranchInstructions(
                    list, fastCodeException, fastCodeException.maxOffset,
                    afterOffset);

            // Reduction de 'afterOffset' a l'aide des numéros de ligne
            if (! fastCodeException.hasSynchronizedFlag())
            {
                afterOffset = reduceAfterOffsetWithLineNumbers(
                        list, fastCodeException, afterOffset);
            }

            // Reduction de 'afterOffset' a l'aide des instructions 'switch'
            afterOffset = reduceAfterOffsetWithSwitchInstructions(
                    switchCaseOffsets, fastCodeException.getTryFromOffset(),
                    fastCodeException.maxOffset, afterOffset);

            // Reduction de 'afterOffset' a l'aide des instructions de gestion
            // des exceptions englobantes
            afterOffset =
                    reduceAfterOffsetWithExceptions(
                            fastCodeExceptions, fastCodeException.getTryFromOffset(),
                            fastCodeException.maxOffset, afterOffset);
            fastCodeException.setAfterOffset(afterOffset);

            // Recherche de la 1ere exception débutant après 'maxOffset'
            int tryFromOffset = Integer.MAX_VALUE;
            int tryIndex = fastCodeExceptionIndex + 1;
            while (tryIndex < fastCodeExceptions.size())
            {
                int tryFromOffsetTmp =
                        fastCodeExceptions.get(tryIndex).getTryFromOffset();
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
            Instruction instruction;
            while (index < length)
            {
                instruction = list.get(index);

                if (instruction.getOffset() >= afterOffset) {
                    break;
                }

                if (instruction.getOffset() > tryFromOffset)
                {
                    // Saut des blocs try-catch-finally
                    FastCodeExcepcion fce =
                            fastCodeExceptions.get(tryIndex);
                    int afterOffsetTmp = fce.getAfterOffset();

                    int tryFromOffsetTmp;
                    FastCodeExcepcion fceTmp;
                    // Recherche du plus grand offset de fin parmi toutes
                    // les exceptions débutant à  l'offset 'tryFromOffset'
                    for (;;)
                    {
                        tryIndex++;
                        if (tryIndex >= fastCodeExceptions.size())
                        {
                            tryFromOffset = Integer.MAX_VALUE;
                            break;
                        }
                        tryFromOffsetTmp = fastCodeExceptions.get(tryIndex).getTryFromOffset();
                        if (fce.getTryFromOffset() != tryFromOffsetTmp)
                        {
                            tryFromOffset = tryFromOffsetTmp;
                            break;
                        }
                        fceTmp = fastCodeExceptions.get(tryIndex);
                        if (afterOffsetTmp < fceTmp.getAfterOffset()) {
                            afterOffsetTmp = fceTmp.getAfterOffset();
                        }
                    }

                    while (index < length &&
                            list.get(index).getOffset() < afterOffsetTmp) {
                        index++;
                    }
                }
                else
                {
                    switch (instruction.getOpcode())
                    {
                    case Const.ATHROW,
                         Const.RETURN,
                         ByteCodeConstants.XRETURN:
                        // Verification que toutes les variables
                        // locales utilisees sont definies dans le
                        // bloc du dernier catch ou de finally
                        // OU que l'instruction participe a un
                        // operateur ternaire
                        if (CheckLocalVariableUsedVisitor.visit(
                                method.getLocalVariables(),
                                fastCodeException.maxOffset,
                                instruction) || checkTernaryOperator(list, index))
                        {
                            // => Instruction incluse au bloc
                            fastCodeException.setAfterOffset(instruction.getOffset()+1);
                        } else if (index+1 >= length)
                        {
                            // Derniere instruction de la liste
                            if (instruction.getOpcode() == Const.ATHROW)
                            {
                                // Dernier 'throw'
                                // => Instruction incluse au bloc
                                fastCodeException.setAfterOffset(instruction.getOffset()+1);
                            }
                            else
                            {
                                // Dernier 'return'
                                // => Instruction placee après le bloc
                                fastCodeException.setAfterOffset(instruction.getOffset());
                            }
                        }
                        else
                        {
                            // Une instruction du bloc 'try-catch-finally'
                            // saute-t-elle vers l'instuction qui suit
                            // cette instruction ?
                            int tryFromIndex =
                                    InstructionUtil.getIndexForOffset(
                                            list, fastCodeException.getTryFromOffset());
                            int beforeInstructionOffset = index==0 ?
                                    0 : list.get(index-1).getOffset();

                            if (InstructionUtil.checkNoJumpToInterval(
                                    list, tryFromIndex, maxIndex,
                                    beforeInstructionOffset, instruction.getOffset()))
                            {
                                // Aucune instruction du bloc
                                // 'try-catch-finally' ne saute vers
                                // cette instruction.
                                // => Instruction incluse au bloc
                                fastCodeException.setAfterOffset(instruction.getOffset()+1);
                            }
                            else
                            {
                                // Une instruction du bloc
                                // 'try-catch-finally' saute vers
                                // cette instruction.
                                // => Instruction placee après le bloc
                                fastCodeException.setAfterOffset(instruction.getOffset());
                            }
                        }
                        return;
                    case Const.GOTO,
                         ByteCodeConstants.IFCMP,
                         ByteCodeConstants.IF,
                         ByteCodeConstants.IFXNULL:
                        int jumpOffsetTmp;

                        if (instruction.getOpcode() == Const.GOTO)
                        {
                            jumpOffsetTmp =
                                    ((BranchInstruction)instruction).getJumpOffset();
                        }
                        else
                        {
                            // L'aggregation des instructions 'if' n'a pas
                            // encore ete executee. Recherche du plus petit
                            // offset de saut parmi toutes les instructions
                            // 'if' qui suivent.
                            index = ComparisonInstructionAnalyzer.getLastIndex(
                                    list, index);
                            BranchInstruction lastBi =
                                    (BranchInstruction)list.get(index);
                            jumpOffsetTmp = lastBi.getJumpOffset();
                        }

                        if (jumpOffsetTmp > instruction.getOffset())
                        {
                            // Saut positif
                            if (jumpOffsetTmp >= afterOffset) {
                                if (instruction.getOpcode() == Const.GOTO ||
                                        jumpOffsetTmp != afterOffset)
                                {
                                    // Une instruction du bloc 'try-catch-finally'
                                    // saute-t-elle vers cett instuction ?
                                    int tryFromIndex =
                                            InstructionUtil.getIndexForOffset(
                                                    list, fastCodeException.getTryFromOffset());
                                    int beforeInstructionOffset = index==0 ?
                                            0 : list.get(index-1).getOffset();

                                    if (InstructionUtil.checkNoJumpToInterval(
                                            list, tryFromIndex, maxIndex,
                                            beforeInstructionOffset, instruction.getOffset()))
                                    {
                                        // Aucune instruction du bloc
                                        // 'try-catch-finally' ne saute vers
                                        // cette instuction
                                        // => Instruction incluse au bloc
                                        fastCodeException.setAfterOffset(instruction.getOffset()+1);
                                    }
                                    else
                                    {
                                        // Une instruction du bloc
                                        // 'try-catch-finally' saute vers
                                        // cette instuction
                                        // => Instruction placée après le bloc
                                        fastCodeException.setAfterOffset(instruction.getOffset());
                                    }
                                }
                                //else
                                //{
                                // Si l'instruction est un saut conditionnel
                                // et si l'offset de saut est le même que 'afterOffset',
                                // alors l'instruction fait partie du dernier bloc.
                                //}
                                return;
                            }
                            while (++index < length)
                            {
                                if (list.get(index).getOffset() >= jumpOffsetTmp)
                                {
                                    --index;
                                    break;
                                }
                            }
                        }
                        else if (jumpOffsetTmp <= fastCodeException.getTryFromOffset())
                        {
                            // Saut négatif
                            if (index > 0 &&
                                    instruction.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER)
                            {
                                Instruction beforeInstruction = list.get(index-1);
                                if (instruction.getLineNumber() ==
                                        beforeInstruction.getLineNumber())
                                {
                                    // For instruction ?
                                    if (beforeInstruction.getOpcode() ==
                                            Const.ASTORE &&
                                            ((AStore)beforeInstruction).getValueref().getOpcode() ==
                                            ByteCodeConstants.EXCEPTIONLOAD || beforeInstruction.getOpcode() ==
                                            Const.POP &&
                                            ((Pop)beforeInstruction).getObjectref().getOpcode() ==
                                            ByteCodeConstants.EXCEPTIONLOAD)
                                    {
                                        // Non
                                        fastCodeException.setAfterOffset(instruction.getOffset());
                                    } else {
                                        // Oui
                                        fastCodeException.setAfterOffset(beforeInstruction.getOffset());
                                    }
                                    return;
                                }
                            }
                            fastCodeException.setAfterOffset(instruction.getOffset());
                            return;
                        }
                        break;
                    case Const.LOOKUPSWITCH,
                         Const.TABLESWITCH:
                        Switch s = (Switch)instruction;

                        // Search max offset
                        int maxOffset = s.getDefaultOffset();
                        int i = s.getOffsets().length;
                        while (i-- > 0)
                        {
                            int offset = s.getOffset(i);
                            if (maxOffset < offset) {
                                maxOffset = offset;
                            }
                        }

                        if (maxOffset < afterOffset)
                        {
                            while (++index < length)
                            {
                                if (list.get(index).getOffset() >= maxOffset)
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

    private static boolean checkTernaryOperator(List<Instruction> list, int index)
    {
        // Motif des operateurs ternaires :
        //  index-3) If instruction (IF || IFCMP || IFXNULL || COMPLEXIF)
        //  index-2) TernaryOpStore
        //  index-1) Goto
        //    index) (X)Return
        if (index > 2 &&
                list.get(index-1).getOpcode() == Const.GOTO &&
                list.get(index-2).getOpcode() == ByteCodeConstants.TERNARYOPSTORE)
        {
            Goto g = (Goto)list.get(index-1);
            int jumpOffset = g.getJumpOffset();
            int returnOffset = list.get(index).getOffset();
            if (g.getOffset() < jumpOffset && jumpOffset < returnOffset)
            {
                return true;
            }
        }

        return false;
    }

    private static int reduceAfterOffsetWithBranchInstructions(
            List<Instruction> list, FastCodeExcepcion fastCodeException,
            int firstOffset, int afterOffset)
    {
        Instruction instruction;

        // Check previous instructions
        int index = InstructionUtil.getIndexForOffset(
                list, fastCodeException.getTryFromOffset());

        if (index != -1)
        {
            while (index-- > 0)
            {
                instruction = list.get(index);

                if (ByteCodeUtil.isIfOrGotoInstruction(instruction.getOpcode(), false)) {
                    int jumpOffset = ((BranchInstruction)instruction).getJumpOffset();
                    if (firstOffset < jumpOffset && jumpOffset < afterOffset) {
                        afterOffset = jumpOffset;
                    }
                }
            }
        }

        // Check next instructions
        index = list.size();
        do
        {
            index--;
            instruction = list.get(index);

            if (ByteCodeUtil.isIfOrGotoInstruction(instruction.getOpcode(), false)) {
                int jumpOffset = ((BranchInstruction)instruction).getJumpOffset();
                if (firstOffset < jumpOffset && jumpOffset < afterOffset) {
                    afterOffset = jumpOffset;
                }
            }
        }
        while (instruction.getOffset() > afterOffset);

        return afterOffset;
    }

    private static int reduceAfterOffsetWithLineNumbers(
            List<Instruction> list, FastCodeExcepcion fastCodeException,
            int afterOffset)
    {
        int fromIndex = InstructionUtil.getIndexForOffset(
                list, fastCodeException.getTryFromOffset());
        int index = fromIndex;

        if (index != -1)
        {
            // Search first line number
            int length = list.size();
            int firstLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
            Instruction instruction;

            do
            {
                instruction = list.get(index);
                index++;

                if (instruction.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER)
                {
                    firstLineNumber = instruction.getLineNumber();
                    break;
                }
            }
            while (instruction.getOffset() < afterOffset && index < length);

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

                        if (instruction.getOffset() <= maxOffset || instruction.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER &&
                                instruction.getLineNumber() >= firstLineNumber)
                        {
                            break;
                        }

                        // L'instruction a un numéro de ligne inferieur aux
                        // instructions du bloc 'try'. A priori, elle doit être
                        // place après le bloc 'catch'.

                        // Est-ce une instruction de saut ? Si oui, est-ce que
                        // la placer hors du bloc 'catch' genererait deux points
                        // de sortie du bloc ?
                        if (instruction.getOpcode() == Const.GOTO)
                        {
                            int jumpOffset = ((Goto)instruction).getJumpOffset();

                            if (! InstructionUtil.checkNoJumpToInterval(
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
                        if (instruction.getOpcode() == Const.RETURN)
                        {
                            int maxIndex = InstructionUtil.getIndexForOffset(
                                    list, maxOffset);

                            if (list.get(maxIndex-1).getOpcode() == instruction.getOpcode())
                            {
                                break;
                            }
                        }

                        /*
                         * A QUOI SERT CE BLOC ? A QUEL CAS D'UTILISATION
                         * CORRESPOND T IL ?
                         * /
                        if (instruction.opcode != Const.IINC)
                        {
                            if (// Check previous instructions
                                InstructionUtil.CheckNoJumpToInterval(
                                    list,
                                    0, index,
                                    maxOffset, instruction.offset) &&
                                // Check next instructions
                                InstructionUtil.CheckNoJumpToInterval(
                                    list,
                                    index+1, length,
                                    maxOffset, instruction.offset))
                            {
                                break;
                            }
                        }
                        / */

                        afterOffset = instruction.getOffset();
                    }
                }
            }
        }

        return afterOffset;
    }

    private static int reduceAfterOffsetWithSwitchInstructions(
            List<int[]> switchCaseOffsets,
            int firstOffset, int lastOffset, int afterOffset)
    {
        int i = switchCaseOffsets.size();
        int[] offsets;
        int j;
        while (i-- > 0)
        {
            offsets = switchCaseOffsets.get(i);

            j = offsets.length;
            if (j > 1)
            {
                j--;
                int offset2 = offsets[j];

                int offset1;
                while (j-- > 0)
                {
                    offset1 = offsets[j];

                    if (offset1 != -1 &&
                            offset1 <= firstOffset && lastOffset < offset2 && (afterOffset == -1 || afterOffset > offset2)) {
                        afterOffset = offset2;
                    }

                    offset2 = offset1;
                }
            }
        }

        return afterOffset;
    }

    private static int reduceAfterOffsetWithExceptions(
            List<FastCodeExcepcion> fastCodeExceptions,
            int fromOffset, int maxOffset, int afterOffset)
    {
        int i = fastCodeExceptions.size();
        FastCodeExcepcion fastCodeException;
        int toOffset;
        while (i-- > 0)
        {
            fastCodeException = fastCodeExceptions.get(i);

            toOffset = fastCodeException.getFinallyFromOffset();

            if (fastCodeException.getCatches() != null)
            {
                int j = fastCodeException.getCatches().size();
                FastCodeExceptionCatch fcec;
                while (j-- > 0)
                {
                    fcec = fastCodeException.getCatches().get(j);

                    if (toOffset != -1 &&
                            fcec.getFromOffset() <= fromOffset &&
                            maxOffset < toOffset && (afterOffset == -1 || afterOffset > toOffset)) {
                        afterOffset = toOffset;
                    }

                    toOffset = fcec.getFromOffset();
                }
            }

            if (fastCodeException.getTryFromOffset() <= fromOffset &&
                    maxOffset < toOffset && (afterOffset == -1 || afterOffset > toOffset)) {
                afterOffset = toOffset;
            }
        }

        return afterOffset;
    }

    public static void formatFastTry(
            LocalVariables localVariables, FastCodeExcepcion fce,
            FastTry fastTry, int returnOffset)
    {
        switch (fce.getType())
        {
        case FastConstants.TYPE_CATCH:
            formatCatch(localVariables, fce, fastTry);
            break;
        case FastConstants.TYPE_118_FINALLY:
            format118Finally(localVariables, fce, fastTry);
            break;
        case FastConstants.TYPE_118_FINALLY_2:
            format118Finally2(fce, fastTry);
            break;
        case FastConstants.TYPE_118_FINALLY_THROW:
            format118FinallyThrow(fastTry);
            break;
        case FastConstants.TYPE_118_CATCH_FINALLY:
            format118CatchFinally(fce, fastTry);
            break;
        case FastConstants.TYPE_118_CATCH_FINALLY_2:
            format118CatchFinally2(fce, fastTry);
            break;
        case FastConstants.TYPE_131_CATCH_FINALLY:
            format131CatchFinally(localVariables, fce, fastTry);
            break;
        case FastConstants.TYPE_142:
            format142(localVariables, fce, fastTry);
            break;
        case FastConstants.TYPE_142_FINALLY_THROW:
            format142FinallyThrow(fastTry);
            break;
        case FastConstants.TYPE_JIKES_122:
            formatJikes122(localVariables, fce, fastTry, returnOffset);
            break;
        case FastConstants.TYPE_ECLIPSE_677_FINALLY:
            formatEclipse677Finally(fce, fastTry);
            break;
        case FastConstants.TYPE_ECLIPSE_677_CATCH_FINALLY:
            formatEclipse677CatchFinally(fce, fastTry, returnOffset);
        }
    }

    private static void formatCatch(
            LocalVariables localVariables, FastCodeExcepcion fce, FastTry fastTry)
    {
        List<Instruction> tryInstructions = fastTry.getInstructions();
        int jumpOffset = -1;

        // Remove last 'goto' instruction in try block
        if (!tryInstructions.isEmpty())
        {
            int lastIndex = tryInstructions.size() - 1;
            Instruction instruction = tryInstructions.get(lastIndex);

            if (instruction.getOpcode() == Const.GOTO)
            {
                int tmpJumpOffset = ((Goto)instruction).getJumpOffset();

                if (tmpJumpOffset < fce.getTryFromOffset() ||
                        instruction.getOffset() < tmpJumpOffset)
                {
                    jumpOffset = tmpJumpOffset;
                    fce.setTryToOffset(instruction.getOffset());
                    tryInstructions.remove(lastIndex);
                }
            }
        }

        // Remove JSR instruction in try block before 'return' instruction
        formatFastTryRemoveJsrInstructionAndCompactStoreReturn(
                tryInstructions, localVariables, Instruction.UNKNOWN_LINE_NUMBER);

        int i = fastTry.getCatches().size();
        List<Instruction> catchInstructions;
        while (i-- > 0)
        {
            catchInstructions = fastTry.getCatches().get(i).instructions();

            // Remove first catch instruction in each catch block
            if (formatCatchRemoveFirstCatchInstruction(catchInstructions.get(0))) {
                catchInstructions.remove(0);
            }

            // Remove last 'goto' instruction
            if (!catchInstructions.isEmpty())
            {
                int lastIndex = catchInstructions.size() - 1;
                Instruction instruction = catchInstructions.get(lastIndex);

                if (instruction.getOpcode() == Const.GOTO)
                {
                    int tmpJumpOffset = ((Goto)instruction).getJumpOffset();

                    if (tmpJumpOffset < fce.getTryFromOffset() ||
                            instruction.getOffset() < tmpJumpOffset)
                    {
                        if (jumpOffset == -1)
                        {
                            jumpOffset = tmpJumpOffset;
                            catchInstructions.remove(lastIndex);
                        }
                        else if (jumpOffset == tmpJumpOffset)
                        {
                            catchInstructions.remove(lastIndex);
                        }
                    }
                }

                // Remove JSR instruction in try block before 'return' instruction
                formatFastTryRemoveJsrInstructionAndCompactStoreReturn(
                        catchInstructions, localVariables,
                        Instruction.UNKNOWN_LINE_NUMBER);
            }
        }
    }

    private static boolean formatCatchRemoveFirstCatchInstruction(
            Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case Const.POP:
            return
                    ((Pop)instruction).getObjectref().getOpcode() ==
                    ByteCodeConstants.EXCEPTIONLOAD;

        case Const.ASTORE:
            return
                    ((AStore)instruction).getValueref().getOpcode() ==
                    ByteCodeConstants.EXCEPTIONLOAD;

        default:
            return false;
        }
    }

    private static void format118Finally(
            LocalVariables localVariables, FastCodeExcepcion fce, FastTry fastTry)
    {
        List<Instruction> tryInstructions = fastTry.getInstructions();
        int length = tryInstructions.size();

        // Remove last 'goto' instruction in try block
        if (tryInstructions.get(--length).getOpcode() == Const.GOTO)
        {
            Goto g = (Goto)tryInstructions.remove(length);
            fce.setTryToOffset(g.getOffset());
        }
        length--;
        // Remove last 'jsr' instruction in try block
        if (tryInstructions.get(length).getOpcode() != Const.JSR) {
            throw new UnexpectedInstructionException();
        }
        tryInstructions.remove(length);

        // Remove JSR instruction in try block before 'return' instruction
        int finallyInstructionsLineNumber =
                fastTry.getFinallyInstructions().get(0).getLineNumber();
        formatFastTryRemoveJsrInstructionAndCompactStoreReturn(
                tryInstructions, localVariables, finallyInstructionsLineNumber);

        format118FinallyThrow(fastTry);
    }

    private static void format118Finally2(
            FastCodeExcepcion fce, FastTry fastTry)
    {
        List<Instruction> tryInstructions = fastTry.getInstructions();
        int tryInstructionsLength = tryInstructions.size();

        // Remove last 'goto' instruction in try block
        if (tryInstructions.get(tryInstructionsLength-1).getOpcode() ==
                Const.GOTO)
        {
            tryInstructionsLength--;
            Goto g = (Goto)tryInstructions.remove(tryInstructionsLength);
            fce.setTryToOffset(g.getOffset());
        }

        List<Instruction> finallyInstructions = fastTry.getFinallyInstructions();
        int finallyInstructionsLength = finallyInstructions.size();

        // Update all offset of instructions 'goto' and 'ifxxx' if
        // (finallyInstructions.gt(0).offset) < (jump offset) &&
        // (jump offset) < (finallyInstructions.gt(5).offset)
        if (finallyInstructionsLength > 5)
        {
            int firstFinallyOffset = finallyInstructions.get(0).getOffset();
            int lastFinallyOffset = finallyInstructions.get(5).getOffset();

            Instruction instruction;
            int jumpOffset;
            while (tryInstructionsLength-- > 0)
            {
                instruction = tryInstructions.get(tryInstructionsLength);
                switch (instruction.getOpcode())
                {
                case ByteCodeConstants.IFCMP:
                {
                    jumpOffset = ((IfCmp)instruction).getJumpOffset();

                    if (firstFinallyOffset < jumpOffset &&
                            jumpOffset <= lastFinallyOffset) {
                        ((IfCmp)instruction).setBranch(firstFinallyOffset - instruction.getOffset());
                    }
                }
                break;
                case ByteCodeConstants.IF,
                     ByteCodeConstants.IFXNULL:
                {
                    jumpOffset =
                            ((IfInstruction)instruction).getJumpOffset();

                    if (firstFinallyOffset < jumpOffset &&
                            jumpOffset <= lastFinallyOffset) {
                        ((IfInstruction)instruction).setBranch(firstFinallyOffset - instruction.getOffset());
                    }
                }
                break;
                case ByteCodeConstants.COMPLEXIF:
                {
                    jumpOffset =
                            ((BranchInstruction)instruction).getJumpOffset();

                    if (firstFinallyOffset < jumpOffset &&
                            jumpOffset <= lastFinallyOffset) {
                        ((ComplexConditionalBranchInstruction)instruction).setBranch(firstFinallyOffset - instruction.getOffset());
                    }
                }
                break;
                case Const.GOTO:
                {
                    jumpOffset = ((Goto)instruction).getJumpOffset();

                    if (firstFinallyOffset < jumpOffset &&
                            jumpOffset <= lastFinallyOffset) {
                        ((Goto)instruction).setBranch(firstFinallyOffset - instruction.getOffset());
                    }
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

    private static void format118FinallyThrow(FastTry fastTry)
    {
        List<Instruction> finallyInstructions = fastTry.getFinallyInstructions();
        int length = finallyInstructions.size();

        length--;
        // Remove last 'ret' instruction in finally block
        Instruction i = finallyInstructions.get(length);
        if (i.getOpcode() != Const.RET) {
            throw new UnexpectedInstructionException();
        }
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

    private static void format118CatchFinally(
            FastCodeExcepcion fce, FastTry fastTry)
    {
        List<Instruction> tryInstructions = fastTry.getInstructions();
        int tryInstructionsLength = tryInstructions.size();

        // Remove last 'goto' instruction in try block
        if (tryInstructions.get(--tryInstructionsLength).getOpcode() ==
                Const.GOTO)
        {
            Goto g = (Goto)tryInstructions.remove(tryInstructionsLength);
            fce.setTryToOffset(g.getOffset());
        }

        // Format catch blocks
        int i = fastTry.getCatches().size()-1;
        if (i >= 0)
        {
            List<Instruction>  catchInstructions =
                    fastTry.getCatches().get(i).instructions();
            int catchInstructionsLength = catchInstructions.size();
            if (catchInstructionsLength > 0) {
                catchInstructionsLength--;
                int catchLastOpCode = catchInstructions.get(catchInstructionsLength).getOpcode();
                if (catchLastOpCode == Const.GOTO) {
                    // Remove 'goto' instruction in catch block
                    catchInstructions.remove(catchInstructionsLength);
                    catchInstructionsLength--;
                    // Remove 'jsr' instruction in catch block
                    catchInstructions.remove(catchInstructionsLength);
                } else if (catchLastOpCode == Const.RETURN || catchLastOpCode == ByteCodeConstants.XRETURN) {
                    catchInstructionsLength--;
                    // Remove 'jsr' instruction in catch block
                    catchInstructions.remove(catchInstructionsLength);
                        if (catchInstructionsLength > 0 &&
                            catchInstructions.get(catchInstructionsLength-1).getOpcode() == Const.ATHROW)
                    {
                        // Remove 'return' instruction after a 'throw' instruction
                        catchInstructions.remove(catchInstructionsLength);
                    }
                }
                    // Remove first catch instruction in each catch block
                catchInstructions.remove(0);
            }
            int catchLastOpCode;
            while (i-- > 0)
            {
                catchInstructions = fastTry.getCatches().get(i).instructions();
                catchInstructionsLength = catchInstructions.size();

                catchInstructionsLength--;
                catchLastOpCode = catchInstructions.get(catchInstructionsLength).getOpcode();
                if (catchLastOpCode == Const.GOTO) {
                    catchInstructions.remove(catchInstructionsLength);
                } else if (catchLastOpCode == Const.RETURN || catchLastOpCode == ByteCodeConstants.XRETURN) {
                    catchInstructionsLength--;
                    // Remove 'jsr' instruction in catch block
                    catchInstructions.remove(catchInstructionsLength);
                }

                // Remove first catch instruction in each catch block
                catchInstructions.remove(0);
            }
        }

        List<Instruction>  finallyInstructions = fastTry.getFinallyInstructions();
        int finallyInstructionsLength = finallyInstructions.size();

        finallyInstructionsLength--;
        // Remove last 'ret' instruction in finally block
        finallyInstructions.remove(finallyInstructionsLength);
        // Remove 'AStore ExceptionLoad' instruction in finally block
        finallyInstructions.remove(0);
        // Remove 'jsr' instruction in finally block
        finallyInstructions.remove(0);
        // Remove 'athrow' instruction in finally block
        finallyInstructions.remove(0);
        // Remove 'AStore ExceptionLoad' instruction in finally block
        finallyInstructions.remove(0);
    }

    private static void format118CatchFinally2(
            FastCodeExcepcion fce, FastTry fastTry)
    {
        List<Instruction> tryInstructions = fastTry.getInstructions();
        int tryInstructionsLength = tryInstructions.size();

        // Remove last 'goto' instruction in try block
        if (tryInstructions.get(--tryInstructionsLength).getOpcode() ==
                Const.GOTO)
        {
            Goto g = (Goto)tryInstructions.remove(tryInstructionsLength);
            fce.setTryToOffset(g.getOffset());
        }

        // Format catch blocks
        int i = fastTry.getCatches().size();
        List<Instruction> catchInstructions;
        int catchInstructionsLength;
        while (i-- > 0)
        {
            catchInstructions = fastTry.getCatches().get(i).instructions();
            catchInstructionsLength = catchInstructions.size();
            catchInstructions.remove(catchInstructionsLength - 1);
            // Remove first catch instruction in each catch block
            catchInstructions.remove(0);
        }

        // Remove 'Pop ExceptionLoad' instruction in finally block
        List<Instruction>  finallyInstructions = fastTry.getFinallyInstructions();
        finallyInstructions.remove(0);
    }

    /** Deux variantes existent. La sous procedure [finally] ne se trouve pas
     * toujours dans le block 'finally'.
     */
    private static void format131CatchFinally(
            LocalVariables localVariables, FastCodeExcepcion fce, FastTry fastTry)
    {
        List<Instruction> tryInstructions = fastTry.getInstructions();
        int length = tryInstructions.size();

        // Remove last 'goto' instruction in try block
        if (tryInstructions.get(--length).getOpcode() == Const.GOTO)
        {
            Goto g = (Goto)tryInstructions.remove(length);
            fce.setTryToOffset(g.getOffset());
        }
        // Remove JSR instruction in try block before 'return' instruction
        int finallyInstructionsLineNumber =
                fastTry.getFinallyInstructions().get(0).getLineNumber();
        int jumpOffset = formatFastTryRemoveJsrInstructionAndCompactStoreReturn(
                tryInstructions, localVariables, finallyInstructionsLineNumber);
        // Remove last 'jsr' instruction in try block
        length = tryInstructions.size();
        if (tryInstructions.get(--length).getOpcode() == Const.JSR)
        {
            Jsr jsr = (Jsr)tryInstructions.remove(length);
            jumpOffset = jsr.getJumpOffset();
        }
        if (jumpOffset == -1) {
            throw new UnexpectedInstructionException();
        }

        List<Instruction> finallyInstructions = fastTry.getFinallyInstructions();

        if (jumpOffset < finallyInstructions.get(0).getOffset())
        {
            // La sous procedure [finally] se trouve dans l'un des blocs 'catch'.

            // Recherche et extraction de la sous procedure
            int i = fastTry.getCatches().size();
            int index;
            while (i-- > 0)
            {
                List<Instruction> catchInstructions =
                        fastTry.getCatches().get(i).instructions();

                if (catchInstructions.isEmpty() ||
                        catchInstructions.get(0).getOffset() > jumpOffset) {
                    continue;
                }

                // Extract
                index = InstructionUtil.getIndexForOffset(catchInstructions, jumpOffset);
                finallyInstructions.clear();

                while (catchInstructions.get(index).getOpcode() != Const.RET) {
                    finallyInstructions.add(catchInstructions.remove(index));
                }
                if (catchInstructions.get(index).getOpcode() == Const.RET) {
                    finallyInstructions.add(catchInstructions.remove(index));
                }

                break;
            }

            // Format catch blocks
            i = fastTry.getCatches().size();
            List<Instruction> catchInstructions;
            while (i-- > 0)
            {
                catchInstructions = fastTry.getCatches().get(i).instructions();
                length = catchInstructions.size();

                // Remove last 'goto' instruction
                if (catchInstructions.get(--length).getOpcode() == Const.GOTO)
                {
                    catchInstructions.remove(length);
                }
                // Remove last 'jsr' instruction
                if (catchInstructions.get(--length).getOpcode() == Const.JSR) {
                    catchInstructions.remove(length);
                }
                // Remove JSR instruction in try block before 'return' instruction
                formatFastTryRemoveJsrInstructionAndCompactStoreReturn(
                        catchInstructions, localVariables,
                        finallyInstructionsLineNumber);
                // Remove first catch instruction in each catch block
                catchInstructions.remove(0);
            }

            // Format finally block
            length = finallyInstructions.size();

            length--;
            // Remove last 'ret' instruction in finally block
            finallyInstructions.remove(length);
        }
        else
        {
            // La sous procedure [finally] se trouve dans le bloc 'finally'.

            // Format catch blocks
            int i = fastTry.getCatches().size();
            List<Instruction> catchInstructions;
            while (i-- > 0)
            {
                catchInstructions = fastTry.getCatches().get(i).instructions();
                length = catchInstructions.size();

                // Remove last 'goto' instruction
                if (catchInstructions.get(--length).getOpcode() == Const.GOTO)
                {
                    catchInstructions.remove(length);
                }
                // Remove last 'jsr' instruction
                if (catchInstructions.get(--length).getOpcode() == Const.JSR) {
                    catchInstructions.remove(length);
                }
                // Remove JSR instruction in try block before 'return' instruction
                formatFastTryRemoveJsrInstructionAndCompactStoreReturn(
                        catchInstructions, localVariables,
                        finallyInstructionsLineNumber);
                // Remove first catch instruction in each catch block
                catchInstructions.remove(0);
            }

            // Format finally block
            length = finallyInstructions.size();

            length--;
            // Remove last 'ret' instruction in finally block
            finallyInstructions.remove(length);
            // Remove 'AStore ExceptionLoad' instruction in finally block
            finallyInstructions.remove(0);
            // Remove 'jsr' instruction in finally block
            finallyInstructions.remove(0);
            // Remove 'athrow' instruction in finally block
            finallyInstructions.remove(0);
        }
        // Remove 'AStore ReturnAddressLoad' instruction in finally block
        finallyInstructions.remove(0);
    }

    private static void format142(
            LocalVariables localVariables, FastCodeExcepcion fce, FastTry fastTry)
    {
        List<Instruction> finallyInstructions = fastTry.getFinallyInstructions();
        int finallyInstructionsSize = finallyInstructions.size();

        // Remove last 'athrow' instruction in finally block
        if (finallyInstructions.get(finallyInstructionsSize-1).getOpcode() ==
                Const.ATHROW)
        {
            finallyInstructions.remove(finallyInstructionsSize-1);
        }
        // Remove 'astore' or 'monitorexit' instruction in finally block
        int finallyFirstOpcode = finallyInstructions.get(0).getOpcode();
        if (finallyFirstOpcode == Const.ASTORE || finallyFirstOpcode == Const.POP) {
            finallyInstructions.remove(0);
        }
        finallyInstructionsSize = finallyInstructions.size();

        if (finallyInstructionsSize > 0)
        {
            FastCompareInstructionVisitor visitor =
                    new FastCompareInstructionVisitor();

            List<Instruction> tryInstructions = fastTry.getInstructions();
            int length = tryInstructions.size();

            int tryLastOpCode = tryInstructions.get(length-1).getOpcode();
            if (tryLastOpCode == Const.GOTO) {
                length--;
                // Remove last 'goto' instruction in try block
                Goto g = (Goto)tryInstructions.get(length);
                if (g.getBranch() > 0)
                {
                    tryInstructions.remove(length);
                    fce.setTryToOffset(g.getOffset());
                }
            }

            // Remove finally instructions in try block before 'return' instruction
            format142RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
                    localVariables, visitor, tryInstructions, finallyInstructions);

            if (fastTry.getCatches() != null)
            {
                // Format catch blocks
                int i = fastTry.getCatches().size();
                List<Instruction> catchInstructions;
                int catchLastOpCode;
                while (i-- > 0)
                {
                    catchInstructions = fastTry.getCatches().get(i).instructions();

                    length = catchInstructions.size();

                    catchLastOpCode = catchInstructions.get(length-1).getOpcode();
                    if (catchLastOpCode == Const.GOTO) {
                        length--;
                        // Remove last 'goto' instruction in try block
                        Goto g = (Goto)catchInstructions.get(length);
                        if (g.getBranch() > 0)
                        {
                            catchInstructions.remove(length);
                        }
                    }

                    // Remove finally instructions before 'return' instruction
                    format142RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
                            localVariables, visitor, catchInstructions, finallyInstructions);
                    // Remove first catch instruction in each catch block
                    if (!catchInstructions.isEmpty()) {
                        catchInstructions.remove(0);
                    }
                }
            }
        }
        if (fastTry.getCatches() != null && !fastTry.getCatches().isEmpty())
        {
            // Format catch blocks
            int i = fastTry.getCatches().size();
            List<Instruction> catchInstructions;
            while (i-- > 0)
            {
                catchInstructions = fastTry.getCatches().get(i).instructions();

                // Remove first catch instruction in each catch block
                if (!catchInstructions.isEmpty() && formatCatchRemoveFirstCatchInstruction(catchInstructions.get(0))) {
                    catchInstructions.remove(0);
                }
            }
        }
    }

    private static void format142RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
            LocalVariables localVariables,
            FastCompareInstructionVisitor visitor,
            List<Instruction> instructions,
            List<Instruction> finallyInstructions)
    {
        int index = instructions.size();
        int finallyInstructionsSize = finallyInstructions.size();
        int finallyInstructionsLineNumber = finallyInstructions.get(0).getLineNumber();

        boolean match = index >= finallyInstructionsSize && visitor.visit(
                instructions, finallyInstructions,
                index-finallyInstructionsSize, 0, finallyInstructionsSize);

        // Remove last finally instructions
        if (match)
        {
            for (int j=0; j<finallyInstructionsSize && index>0; index--,j++) {
                instructions.remove(index-1);
            }
        }

        Instruction instruction;
        while (index-- > 0)
        {
            instruction = instructions.get(index);

            switch (instruction.getOpcode())
            {
            case Const.RETURN,
                 Const.ATHROW:
            {
                match = index >= finallyInstructionsSize && visitor.visit(
                        instructions, finallyInstructions,
                        index-finallyInstructionsSize, 0, finallyInstructionsSize);

                if (match)
                {
                    Instruction instr;
                    // Remove finally instructions
                    for (int j=0; j<finallyInstructionsSize && index>0; index--,++j) {
                        instr = instructions.get(index-1);
                        if (instr.getLineNumber() >= finallyInstructionsLineNumber) {
                            instructions.remove(index-1);
                        }
                    }
                }

                if (instruction.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER &&
                        instruction.getLineNumber() >= finallyInstructionsLineNumber)
                {
                    instruction.setLineNumber(Instruction.UNKNOWN_LINE_NUMBER);
                }
            }
            break;
            case ByteCodeConstants.XRETURN:
            {
                match = index >= finallyInstructionsSize && visitor.visit(
                        instructions, finallyInstructions,
                        index-finallyInstructionsSize, 0, finallyInstructionsSize);

                if (match)
                {
                    // Remove finally instructions
                    for (int j=0; j<finallyInstructionsSize && index>0; index--,j++) {
                        instructions.remove(index-1);
                    }
                }

                // Compact AStore + Return
                ReturnInstruction ri = (ReturnInstruction)instruction;

                if (ri.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER)
                {
                    switch (ri.getValueref().getOpcode())
                    {
                    case Const.ALOAD:
                        if (instructions.get(index-1).getOpcode() == Const.ASTORE) {
                            index = compactStoreReturn(
                                    instructions, localVariables, ri,
                                    index, finallyInstructionsLineNumber);
                        }
                        break;
                    case ByteCodeConstants.LOAD:
                        if (instructions.get(index-1).getOpcode() == ByteCodeConstants.STORE) {
                            index = compactStoreReturn(
                                    instructions, localVariables, ri,
                                    index, finallyInstructionsLineNumber);
                        }
                        break;
                    case Const.ILOAD:
                        if (instructions.get(index-1).getOpcode() == Const.ISTORE) {
                            index = compactStoreReturn(
                                    instructions, localVariables, ri,
                                    index, finallyInstructionsLineNumber);
                        }
                        break;
                    }
                }
            }
            break;
            case FastConstants.TRY:
            {
                // Recursive calls
                FastTry ft = (FastTry)instruction;

                format142RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
                        localVariables, visitor,
                        ft.getInstructions(), finallyInstructions);

                if (ft.getCatches() != null)
                {
                    int i = ft.getCatches().size();
                    while (i-- > 0)
                    {
                        format142RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
                                localVariables, visitor,
                                ft.getCatches().get(i).instructions(), finallyInstructions);
                    }
                }

                if (ft.getFinallyInstructions() != null)
                {
                    format142RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
                            localVariables, visitor,
                            ft.getFinallyInstructions(), finallyInstructions);
                }
            }
            break;
            case FastConstants.SYNCHRONIZED:
            {
                // Recursive calls
                FastSynchronized fs = (FastSynchronized)instruction;

                format142RemoveFinallyInstructionsBeforeReturnAndCompactStoreReturn(
                        localVariables, visitor,
                        fs.getInstructions(), finallyInstructions);
            }
            break;
            }
        }
    }

    private static int compactStoreReturn(
            List<Instruction> instructions, LocalVariables localVariables,
            ReturnInstruction ri, int index, int finallyInstructionsLineNumber)
    {
        IndexInstruction load = (IndexInstruction)ri.getValueref();
        StoreInstruction store = (StoreInstruction)instructions.get(index-1);

        if (load.getIndex() == store.getIndex() &&
                (load.getLineNumber() <= store.getLineNumber() ||
                load.getLineNumber() >= finallyInstructionsLineNumber))
        {
            // TODO A ameliorer !!
            // Remove local variable
            LocalVariable lv = localVariables.
                    getLocalVariableWithIndexAndOffset(
                            store.getIndex(), store.getOffset());

            if (lv != null && lv.getStartPc() == store.getOffset() &&
                    lv.getStartPc() + lv.getLength() <= ri.getOffset()) {
                localVariables.
                removeLocalVariableWithIndexAndOffset(
                        store.getIndex(), store.getOffset());
            }
            // Replace returned instruction
            ri.setValueref(store.getValueref());
            if (ri.getLineNumber() > store.getLineNumber()) {
                ri.setLineNumber(store.getLineNumber());
            }
            index--;
            // Remove 'store' instruction
            instructions.remove(index);
        }

        return index;
    }

    private static void format142FinallyThrow(FastTry fastTry)
    {
        // Remove last 'athrow' instruction in finally block
        fastTry.getFinallyInstructions().remove(fastTry.getFinallyInstructions().size()-1);
        // Remove 'astore' instruction in finally block
        fastTry.getFinallyInstructions().remove(0);
    }

    private static void formatJikes122(
            LocalVariables localVariables, FastCodeExcepcion fce,
            FastTry fastTry, int returnOffset)
    {
        List<Instruction> tryInstructions = fastTry.getInstructions();
        int lastIndex = tryInstructions.size()-1;
        Instruction lastTryInstruction = tryInstructions.get(lastIndex);
        int lastTryInstructionOffset = lastTryInstruction.getOffset();

        // Remove last 'goto' instruction in try block
        if (tryInstructions.get(lastIndex).getOpcode() == Const.GOTO)
        {
            Goto g = (Goto)tryInstructions.remove(lastIndex);
            fce.setTryToOffset(g.getOffset());
        }
        // Remove Jsr instruction before return instructions
        int finallyInstructionsLineNumber;
        if (fastTry.getFinallyInstructions().isEmpty()) {
            finallyInstructionsLineNumber = -1;
        } else {
            finallyInstructionsLineNumber = fastTry.getFinallyInstructions().get(0).getLineNumber();
        }
        formatFastTryRemoveJsrInstructionAndCompactStoreReturn(
                tryInstructions, localVariables, finallyInstructionsLineNumber);

        // Format catch blocks
        int i = fastTry.getCatches().size();
        List<Instruction> catchInstructions;
        while (i-- > 0)
        {
            catchInstructions = fastTry.getCatches().get(i).instructions();
            lastIndex = catchInstructions.size()-1;

            // Remove last 'goto' instruction in try block
            if (catchInstructions.get(lastIndex).getOpcode() == Const.GOTO)
            {
                catchInstructions.remove(lastIndex);
            }
            // Remove Jsr instruction before return instructions
            if (finallyInstructionsLineNumber != -1) {
                formatFastTryRemoveJsrInstructionAndCompactStoreReturn(
                        catchInstructions, localVariables,
                        finallyInstructionsLineNumber);
            }
            // Change negative jump goto to return offset
            formatFastTryFormatNegativeJumpOffset(
                    catchInstructions, lastTryInstructionOffset, returnOffset);
            // Remove first catch instruction in each catch block
            catchInstructions.remove(0);
        }

        List<Instruction> finallyInstructions = fastTry.getFinallyInstructions();
        int length = finallyInstructions.size();

        // Remove last 'jsr' instruction in finally block
        if (!finallyInstructions.isEmpty()) {
            length--;
            finallyInstructions.remove(length);
        }
        // Remove last 'ret' or 'athrow' instruction in finally block
        if (!finallyInstructions.isEmpty()) {
            length--;
            finallyInstructions.remove(length);
        }
        // Remove 'AStore ExceptionLoad' instruction in finally block
        if (!finallyInstructions.isEmpty()) {
            finallyInstructions.remove(0);
        }
        // Remove 'jsr' instruction in finally block
        if (!finallyInstructions.isEmpty() && finallyInstructions.get(0).getOpcode() == Const.JSR) {
            finallyInstructions.remove(0);
        }
        // Remove 'athrow' instruction in finally block
        if (!finallyInstructions.isEmpty() && finallyInstructions.get(0).getOpcode() == Const.ATHROW) {
            finallyInstructions.remove(0);
        }
        // Remove 'astore' instruction in finally block
        if (!finallyInstructions.isEmpty() && finallyInstructions.get(0).getOpcode() == Const.ASTORE) {
            finallyInstructions.remove(0);
        }
    }

    private static int formatFastTryRemoveJsrInstructionAndCompactStoreReturn(
            List<Instruction> instructions, LocalVariables localVariables,
            int finallyInstructionsLineNumber)
    {
        int jumpOffset = UtilConstants.INVALID_OFFSET;
        int index = instructions.size();

        while (index-- > 1)
        {
            if (instructions.get(index).getOpcode() == Const.JSR)
            {
                // Remove Jsr instruction
                Jsr jsr = (Jsr)instructions.remove(index);
                jumpOffset = jsr.getJumpOffset();
            }
        }

        index = instructions.size();

        Instruction instruction;
        while (index-- > 1)
        {
            instruction = instructions.get(index);

            if (instruction.getOpcode() == ByteCodeConstants.XRETURN)
            {
                // Compact AStore + Return
                ReturnInstruction ri = (ReturnInstruction)instruction;

                if (ri.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER)
                {
                    switch (ri.getValueref().getOpcode())
                    {
                    case Const.ALOAD:
                        if (instructions.get(index-1).getOpcode() == Const.ASTORE) {
                            index = compactStoreReturn(
                                    instructions, localVariables, ri,
                                    index, finallyInstructionsLineNumber);
                        }
                        break;
                    case ByteCodeConstants.LOAD:
                        if (instructions.get(index-1).getOpcode() == ByteCodeConstants.STORE) {
                            index = compactStoreReturn(
                                    instructions, localVariables, ri,
                                    index, finallyInstructionsLineNumber);
                        }
                        break;
                    case Const.ILOAD:
                        if (instructions.get(index-1).getOpcode() == Const.ISTORE) {
                            index = compactStoreReturn(
                                    instructions, localVariables, ri,
                                    index, finallyInstructionsLineNumber);
                        }
                        break;
                    }
                }
            }
        }

        return jumpOffset;
    }

    private static void formatFastTryFormatNegativeJumpOffset(
            List<Instruction> instructions,
            int lastTryInstructionOffset, int returnOffset)
    {
        int i = instructions.size();

        Instruction instruction;
        while (i-- > 0)
        {
            instruction = instructions.get(i);

            if (instruction.getOpcode() == Const.GOTO) {
                Goto g = (Goto)instruction;
                int jumpOffset = g.getJumpOffset();

                if (jumpOffset < lastTryInstructionOffset)
                {
                    // Change jump offset
                    g.setBranch(returnOffset - g.getOffset());
                }
            }
        }
    }

    private static void formatEclipse677Finally(
            FastCodeExcepcion fce, FastTry fastTry)
    {
        // Remove instructions in finally block
        List<Instruction> finallyInstructions = fastTry.getFinallyInstructions();

        Instruction instruction = finallyInstructions.get(0);

        if (instruction.getOpcode() == Const.POP) {
            // Remove 'pop' instruction in finally block
            finallyInstructions.remove(0);

            List<Instruction> tryInstructions = fastTry.getInstructions();
            int lastIndex = tryInstructions.size()-1;

            // Remove last 'goto' instruction in try block
            if (tryInstructions.get(lastIndex).getOpcode() == Const.GOTO)
            {
                Goto g = (Goto)tryInstructions.remove(lastIndex);
                fce.setTryToOffset(g.getOffset());
            }
        } else if (instruction.getOpcode() == Const.ASTORE) {
            int exceptionIndex = ((AStore)instruction).getIndex();
            int index = finallyInstructions.size();
            int athrowOffset = -1;
            int afterAthrowOffset = -1;

            // Search throw instruction
            while (index-- > 0)
            {
                instruction = finallyInstructions.get(index);
                if (instruction.getOpcode() == Const.ATHROW)
                {
                    AThrow athrow = (AThrow)instruction;
                    if (findAloadForAThrow(exceptionIndex, athrow))
                    {
                        // Remove last 'athrow' instruction in finally block
                        athrowOffset = instruction.getOffset();
                        finallyInstructions.remove(index);
                        break;
                    }
                }
                afterAthrowOffset = instruction.getOffset();
                finallyInstructions.remove(index);
            }

            if (!finallyInstructions.isEmpty()) {
                // Remove 'astore' instruction in finally block
                Instruction astore = finallyInstructions.remove(0);

                List<Instruction> tryInstructions = fastTry.getInstructions();
                int lastIndex = tryInstructions.size()-1;

                // Remove last 'goto' instruction in try block
                if (tryInstructions.get(lastIndex).getOpcode() == Const.GOTO)
                {
                    Goto g = (Goto)tryInstructions.remove(lastIndex);
                    fce.setTryToOffset(g.getOffset());
                }

                removeOutOfBoundsInstructions(fastTry, astore);
                // Remove finally instructions before 'return' instruction
                int finallyInstructionsSize = finallyInstructions.size();
                formatEclipse677FinallyRemoveFinallyInstructionsBeforeReturn(
                        tryInstructions, finallyInstructionsSize);

                // Format 'ifxxx' instruction jumping to finally block
                formatEclipse677FinallyFormatIfInstruction(
                        tryInstructions, athrowOffset, afterAthrowOffset, astore.getOffset());
            }
        }
    }

    private static void removeOutOfBoundsInstructions(FastTry fastTry, Instruction astore) {
        Instruction tryInstr;
        // Remove try instructions that are out of bounds and should be found in finally instructions
        for (Iterator<Instruction> tryIter = fastTry.getInstructions().iterator(); tryIter.hasNext();) {
            tryInstr = tryIter.next();
            if (tryInstr.getLineNumber() >= astore.getLineNumber()) {
                tryIter.remove();
            }
        }
        Instruction catchInstr;
        // Remove catch instructions that are out of bounds and should be found in finally instructions
        for (FastCatch fastCatch : fastTry.getCatches()) {
            for (Iterator<Instruction> catchIter = fastCatch.instructions().iterator(); catchIter.hasNext();) {
                catchInstr = catchIter.next();
                if (catchInstr.getLineNumber() >= astore.getLineNumber()) {
                    catchIter.remove();
                }
            }
        }
    }

    private static void formatEclipse677FinallyFormatIfInstruction(
            List<Instruction> instructions, int athrowOffset,
            int afterAthrowOffset, int afterTryOffset)
    {
        int i = instructions.size();

        Instruction instruction;
        while (i-- > 0)
        {
            instruction = instructions.get(i);

            if (instruction.getOpcode() == ByteCodeConstants.IF
             || instruction.getOpcode() == ByteCodeConstants.IFXNULL
             || instruction.getOpcode() == ByteCodeConstants.COMPLEXIF) {
                IfInstruction ifi = (IfInstruction)instruction;
                int jumpOffset = ifi.getJumpOffset();

                if (athrowOffset < jumpOffset && jumpOffset <= afterAthrowOffset)
                {
                    // Change jump offset
                    ifi.setBranch(afterTryOffset - ifi.getOffset());
                }
            }
        }
    }

    private static void formatEclipse677FinallyRemoveFinallyInstructionsBeforeReturn(
            List<Instruction> instructions, int finallyInstructionsSize)
    {
        int i = instructions.size();

        int iOpCode;
        while (i-- > 0)
        {
            iOpCode = instructions.get(i).getOpcode();
            if (iOpCode == Const.RETURN || iOpCode == ByteCodeConstants.XRETURN) {
                // Remove finally instructions
                for (int j=0; j<finallyInstructionsSize && i>0; i--,j++) {
                    instructions.remove(i-1);
                }
            }
        }
    }

    private static void formatEclipse677CatchFinally(
            FastCodeExcepcion fce, FastTry fastTry, int returnOffset)
    {
        // Remove instructions in finally block
        List<Instruction> finallyInstructions = fastTry.getFinallyInstructions();

        int exceptionIndex = ((AStore)finallyInstructions.get(0)).getIndex();
        int index = finallyInstructions.size();
        int athrowOffset = -1;
        int afterAthrowOffset = -1;

        Instruction instruction;
        // Search throw instruction
        while (index-- > 0)
        {
            instruction = finallyInstructions.get(index);
            if (instruction.getOpcode() == Const.ATHROW)
            {
                AThrow athrow = (AThrow)instruction;
                if (findAloadForAThrow(exceptionIndex, athrow))
                {
                    // Remove last 'athrow' instruction in finally block
                    athrowOffset = finallyInstructions.remove(index).getOffset();
                    break;
                }
            }
            afterAthrowOffset = instruction.getOffset();
            finallyInstructions.remove(index);
        }

        if (!finallyInstructions.isEmpty()) {
            // Remove 'astore' instruction in finally block
            Instruction astore = finallyInstructions.remove(0);

            List<Instruction> tryInstructions = fastTry.getInstructions();
            int lastIndex = tryInstructions.size()-1;
            Instruction lastTryInstruction = tryInstructions.get(lastIndex);
            int lastTryInstructionOffset = lastTryInstruction.getOffset();

            // Remove last 'goto' instruction in try block
            if (lastTryInstruction.getOpcode() == Const.GOTO)
            {
                Goto g = (Goto)tryInstructions.remove(lastIndex);
                fce.setTryToOffset(g.getOffset());
            }

            removeOutOfBoundsInstructions(fastTry, astore);

            // Remove finally instructions before 'return' instruction
            int finallyInstructionsSize = finallyInstructions.size();
            formatEclipse677FinallyRemoveFinallyInstructionsBeforeReturn(
                    tryInstructions, finallyInstructionsSize);

            // Format 'ifxxx' instruction jumping to finally block
            formatEclipse677FinallyFormatIfInstruction(
                    tryInstructions, athrowOffset,
                    afterAthrowOffset, lastTryInstructionOffset+1);

            // Format catch blocks
            int i = fastTry.getCatches().size();
            FastCatch fastCatch;
            List<Instruction> catchInstructions;
            Instruction lastInstruction;
            int lastInstructionOffset;
            while (i-- > 0)
            {
                fastCatch = fastTry.getCatches().get(i);
                catchInstructions = fastCatch.instructions();
                index = catchInstructions.size();

                lastInstruction = catchInstructions.get(index-1);
                lastInstructionOffset = lastInstruction.getOffset();

                if (lastInstruction.getOpcode() == Const.GOTO)
                {
                    index--;
                    // Remove last 'goto' instruction
                    Goto g = (Goto)catchInstructions.remove(index);
                    int jumpOffset = g.getJumpOffset();

                    if (jumpOffset > fastTry.getOffset())
                    {
                        // Remove finally block instructions
                        for (int j=finallyInstructionsSize; j>0; --j) {
                            index--;
                            catchInstructions.remove(index);
                        }
                    }
                }

                // Remove finally instructions before 'return' instruction
                formatEclipse677FinallyRemoveFinallyInstructionsBeforeReturn(
                        catchInstructions, finallyInstructionsSize);

                // Format 'ifxxx' instruction jumping to finally block
                formatEclipse677FinallyFormatIfInstruction(
                        catchInstructions, athrowOffset,
                        afterAthrowOffset, lastInstructionOffset+1);

                // Change negative jump goto to return offset
                formatFastTryFormatNegativeJumpOffset(
                        catchInstructions, lastTryInstructionOffset, returnOffset);

                // Remove first catch instruction in each catch block
                catchInstructions.remove(0);
            }
        }
    }

    private static boolean findAloadForAThrow(int exceptionIndex, AThrow athrow) {
        return athrow.getValue().getOpcode() == Const.ALOAD &&
                ((ALoad)athrow.getValue()).getIndex() == exceptionIndex
                || athrow.getValue().getOpcode() == Const.CHECKCAST &&
                ((CheckCast)athrow.getValue()).getObjectref().getOpcode() == Const.ALOAD &&
                ((ALoad)((CheckCast)athrow.getValue()).getObjectref()).getIndex() == exceptionIndex;
    }

    public static class FastCodeExcepcion
    implements Comparable<FastCodeExcepcion>
    {
        private final int tryFromOffset;
        private int tryToOffset;
        private final List<FastCodeExceptionCatch> catches;
        private int finallyFromOffset;
        private int nbrFinally;
        private int maxOffset;
        private int afterOffset;
        private int type;
        private final boolean synchronizedFlag;

        FastCodeExcepcion(
                int tryFromOffset, int tryToOffset,
                int maxOffset, boolean synchronizedFlag)
        {
            this.tryFromOffset = tryFromOffset;
            setTryToOffset(tryToOffset);
            this.catches = new ArrayList<>();
            setFinallyFromOffset(UtilConstants.INVALID_OFFSET);
            this.nbrFinally = 0;
            this.maxOffset = maxOffset;
            setAfterOffset(UtilConstants.INVALID_OFFSET);
            setType(FastConstants.TYPE_UNDEFINED);
            this.synchronizedFlag = synchronizedFlag;
        }

        @Override
        public int compareTo(FastCodeExcepcion other)
        {
            // Sort by 1)tryFromOffset 2)maxOffset 3)tryToOffset
            if (getTryFromOffset() != other.getTryFromOffset()) {
                return getTryFromOffset() - other.getTryFromOffset();
            }

            if (this.maxOffset != other.maxOffset) {
                return other.maxOffset - this.maxOffset;
            }

            return other.getTryToOffset() - getTryToOffset();
        }

        @Override
        public int hashCode() {
            return Objects.hash(tryFromOffset, tryToOffset, maxOffset);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return compareTo((FastCodeExcepcion) obj) == 0;
        }

        public int getTryFromOffset() {
            return tryFromOffset;
        }

        public List<FastCodeExceptionCatch> getCatches() {
            return catches;
        }

        public boolean hasSynchronizedFlag() {
            return synchronizedFlag;
        }

        public int getFinallyFromOffset() {
            return finallyFromOffset;
        }

        private void setFinallyFromOffset(int finallyFromOffset) {
            this.finallyFromOffset = finallyFromOffset;
        }

        public int getAfterOffset() {
            return afterOffset;
        }

        private void setAfterOffset(int afterOffset) {
            this.afterOffset = afterOffset;
        }

        public int getType() {
            return type;
        }

        private void setType(int type) {
            this.type = type;
        }

        public int getTryToOffset() {
            return tryToOffset;
        }

        private void setTryToOffset(int tryToOffset) {
            this.tryToOffset = tryToOffset;
        }
    }

    public static class FastCodeExceptionCatch
    implements Comparable<FastCodeExceptionCatch>
    {
        private final int type;
        private final int[] otherTypes;
        private final int fromOffset;

        public FastCodeExceptionCatch(
                int type, int[] otherCatchTypes, int fromOffset)
        {
            this.type = type;
            this.otherTypes = otherCatchTypes;
            this.fromOffset = fromOffset;
        }

        @Override
        public int compareTo(FastCodeExceptionCatch other)
        {
            return this.getFromOffset() - other.getFromOffset();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFromOffset());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return compareTo((FastCodeExceptionCatch) obj) == 0;
        }

        public int getFromOffset() {
            return fromOffset;
        }

        public int getType() {
            return type;
        }

        public int[] getOtherTypes() {
            return otherTypes;
        }
    }

    public static class FastAggregatedCodeExcepcion
    {
        private int[]               otherCatchTypes;
        private int                 nbrFinally;
        private boolean             synchronizedFlag;
        private final CodeException codeException;

        public FastAggregatedCodeExcepcion(int index, int startPc, int endPc, int handlerPc, int catchType)
        {
            this.codeException = new CodeException(index, startPc, endPc, handlerPc, catchType);
            this.otherCatchTypes = null;
            this.nbrFinally = catchType == 0 ? 1 : 0;
        }

        public int getCatchType() {
            return codeException.catchType();
        }

        public int getStartPC() {
            return codeException.startPc();
        }

        public int getEndPC() {
            return codeException.endPc();
        }

        public int getHandlerPC() {
            return codeException.handlerPc();
        }
    }

    public static int computeTryToIndex(
            List<Instruction> instructions, FastCodeExcepcion fce,
            int lastIndex, int maxOffset)
    {
        // Parcours
        int beforeMaxOffset = fce.getTryFromOffset();
        int index = InstructionUtil.getIndexForOffset(
                instructions, fce.getTryFromOffset());

        Instruction instruction;
        while (index <= lastIndex)
        {
            instruction = instructions.get(index);

            if (instruction.getOffset() > maxOffset) {
                return index-1;
            }

            switch (instruction.getOpcode())
            {
            case Const.ATHROW,
                 Const.RETURN,
                 ByteCodeConstants.XRETURN:
            {
                if (instruction.getOffset() >= beforeMaxOffset) {
                    return index;
                }    // Inclus au bloc 'try'
            }
            break;
            case Const.GOTO:
            {
                int jumpOffset = ((BranchInstruction)instruction).getJumpOffset();

                if (jumpOffset > instruction.getOffset())
                {
                    // Saut positif
                    if (jumpOffset < maxOffset)
                    {
                        // Saut dans les limites
                        if (beforeMaxOffset < jumpOffset) {
                            beforeMaxOffset = jumpOffset;
                        }
                    } else // Saut au-delà  des limites
                        if (instruction.getOffset() >= beforeMaxOffset) {
                            return index;
                        }    // Inclus au bloc 'try'
                } else // Saut au-delà  des limites
                    if (jumpOffset < fce.getTryFromOffset() && instruction.getOffset() >= beforeMaxOffset) {
                        return index;
                    }    // Inclus au bloc 'try'
            }
            break;
            case ByteCodeConstants.IFCMP,
                 ByteCodeConstants.IF,
                 ByteCodeConstants.IFXNULL:
            {
                // L'aggregation des instructions 'if' n'a pas
                // encore ete executee. Recherche du plus petit
                // offset de saut parmi toutes les instructions
                // 'if' qui suivent.
                index = ComparisonInstructionAnalyzer.getLastIndex(instructions, index);
                BranchInstruction lastBi = (BranchInstruction)instructions.get(index);
                int jumpOffset = lastBi.getJumpOffset();

                // Saut positif dans les limites
                if (jumpOffset > instruction.getOffset() && jumpOffset < maxOffset && beforeMaxOffset < jumpOffset) {
                    beforeMaxOffset = jumpOffset;
                }
                // else
                // {
                //     // Saut au-delà  des limites, 'break' ?
                // }
                // else
                // {
                //     // Saut négatif, 'continue' ?
                //}
            }
            break;
            case Const.LOOKUPSWITCH,
                 Const.TABLESWITCH:
            {
                Switch s = (Switch)instruction;

                // Search max offset
                int maxSitchOffset = s.getDefaultOffset();
                int i = s.getOffsets().length;
                int offset;
                while (i-- > 0)
                {
                    offset = s.getOffset(i);
                    if (maxSitchOffset < offset) {
                        maxSitchOffset = offset;
                    }
                }
                maxSitchOffset += s.getOffset();

                // Saut positif dans les limites
                if (maxSitchOffset > instruction.getOffset() && maxSitchOffset < maxOffset && beforeMaxOffset < maxSitchOffset) {
                    beforeMaxOffset = maxSitchOffset;
                }
                // else
                // {
                //     // Saut au-delà  des limites, 'break' ?
                // }
                // else
                // {
                //     // Saut négatif, 'continue' ?
                //}
                break;
            }
            }

            index++;
        }

        if (index == instructions.size()) {
            return index - 1;
        }

        return index;
    }
}
