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
package jd.core.process.analyzer.instruction.bytecode;

import org.apache.bcel.Const;

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
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;

/**
 * Aggrege les instructions 'if'
 */
public final class ComparisonInstructionAnalyzer
{
    private ComparisonInstructionAnalyzer() {
        super();
    }

    /*
     *                            début de liste        fin de liste
     *                            |                                |
     * Liste    ... --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ...
     */
    public static void aggregate(List<Instruction> list)
    {
        int afterOffest = -1;
        int index = list.size();

        while (index-- > 0)
        {
            Instruction instruction = list.get(index);

            if (ByteCodeUtil.isIfInstruction(instruction.getOpcode(), false) && index > 0)
            {
                Instruction prevI = list.get(index-1);

                if (ByteCodeUtil.isIfOrGotoInstruction(prevI.getOpcode(), false))
                {
                    BranchInstruction bi     = (BranchInstruction)instruction;
                    BranchInstruction prevBi = (BranchInstruction)prevI;

                    int prevBiJumpOffset = prevBi.getJumpOffset();

                    // Le 2eme if appartient-il au même bloc que le 1er ?
                            if (prevBiJumpOffset == bi.getJumpOffset() ||
                                prevBi.getBranch() > 0 && prevBiJumpOffset <= afterOffest)
                    {
                        // Oui
                        // Test complexe : plusieurs instructions byte-code de test
                        index = analyzeIfInstructions(
                            list, index, bi, afterOffest);
                    }
                }
            }

            afterOffest = instruction.getOffset();
        }
    }

    /*
     *                            début de liste        fin de liste
     *                            |               index            |
     *                            |                   |            |
     * Liste    ... --|----|---|==0===1===2===3===4===5===6==7=...=n---|--| ...
     *                                            if  if  ?
     *                                            |   |   |
     * Offsets                                    |   |   afterLastBiOffset
     * Instruction                    (if) prevLast   last (if)
     */
    private static int analyzeIfInstructions(
            List<Instruction> list, int index,
            BranchInstruction lastBi, int afterOffest)
    {
        int arrayLength = list.get(list.size()-1).getOffset();
        boolean[] offsetToPreviousGotoFlag = new boolean[arrayLength];
        boolean[] inversedTernaryOpLogic = new boolean[arrayLength];

        // Recherche de l'indexe de la premiere instruction 'if' du bloc et
        // initialisation de 'offsetToPreviousGotoFlag'
        int firstIndex = searchFirstIndex(
            list, index, lastBi, afterOffest,
            offsetToPreviousGotoFlag, inversedTernaryOpLogic);

        firstIndex = reduceFirstIndex(list, firstIndex, index);

        if (firstIndex < index)
        {
            // Extraction des instructions de test formant un bloc
            List<Instruction> branchInstructions =
                new ArrayList<>(index - firstIndex + 1);

            branchInstructions.add(lastBi);
            while (index > firstIndex) {
                branchInstructions.add(list.remove(--index));
            }

            Collections.reverse(branchInstructions);
            list.set(index, createIfInstructions(
                offsetToPreviousGotoFlag, inversedTernaryOpLogic,
                branchInstructions, lastBi));
        }

        return index;
    }

    private static int reduceFirstIndex(
        List<Instruction> list, int firstIndex, int lastIndex)
    {
        int firstOffset = firstIndex == 0 ? 0 : list.get(firstIndex-1).getOffset();
        int newFirstOffset = firstOffset;
        int lastOffset = list.get(lastIndex).getOffset();

        // Reduce 'firstIndex' with previous instructions
        int index = firstIndex;
        while (index-- > 0)
        {
            Instruction i = list.get(index);

            if (ByteCodeUtil.isIfOrGotoInstruction(i.getOpcode(), false))
            {
                int jumpOffset = ((BranchInstruction)i).getJumpOffset();
                if (newFirstOffset < jumpOffset && jumpOffset <= lastOffset) {
                    newFirstOffset = jumpOffset;
                }
            }
        }

        // Reduce 'firstIndex' with next instructions
        index = list.size();
        while (--index > lastIndex)
        {
            Instruction i = list.get(index);

            if (ByteCodeUtil.isIfOrGotoInstruction(i.getOpcode(), false))
            {
                int jumpOffset = ((BranchInstruction)i).getJumpOffset();
                if (newFirstOffset < jumpOffset && jumpOffset <= lastOffset) {
                    newFirstOffset = jumpOffset;
                }
            }
        }

        // Search index associated with 'firstOffset'
        if (newFirstOffset != firstOffset)
        {
            for (index=firstIndex; index<=lastIndex; index++)
            {
                Instruction i = list.get(index);
                if (i.getOffset() > newFirstOffset)
                {
                    firstIndex = index;
                    break;
                }
            }
        }

        return firstIndex;
    }

    private static int searchFirstIndex(
        List<Instruction> list, int lastIndex,
        BranchInstruction lastBi, int afterOffest,
        boolean[] offsetToPreviousGotoFlag,
        boolean[] inversedTernaryOpLogic)
    {
        int index = lastIndex;
        int lastBiJumpOffset = lastBi.getJumpOffset();

        Instruction nextInstruction = lastBi;

        while (index-- > 0)
        {
            Instruction instruction = list.get(index);
            int opcode = instruction.getOpcode();

            if (ByteCodeUtil.isIfInstruction(opcode, false))
            {
                BranchInstruction bi = (BranchInstruction)instruction;
                int jumpOffset = bi.getJumpOffset();

                // L'instruction if courante appartient-elle au même bloc que le 1er ?
                if (jumpOffset == lastBiJumpOffset)
                {
                    if (bi.getBranch() > 0 &&
                        instruction.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER &&
                        nextInstruction.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER)
                    {
                        if (instruction.getLineNumber()+2 <= nextInstruction.getLineNumber())
                        {
                            // Amelioration par rapport a JAD : possibilite de
                            // construire deux instructions 'if' pourtant compatibles
                            break; // Non
                        }
                        // Est-ce que l'une des instructions suivantes a un
                        // numéro de ligne <= a instruction.lineNumber et <
                        // a nextInstruction.lineNumber
                        int length = list.size();
                        boolean instructionBetweenIf = false;

                        // ATTENTION: Fragment de code ralentissement grandement la decompilation
                        for (int i=lastIndex+1; i<length; i++)
                        {
                            Instruction ins = list.get(i);

                            if (ins.getOpcode() == Const.IINC)
                            {
                                int lineNumber = ins.getLineNumber();

                                if (lineNumber != Instruction.UNKNOWN_LINE_NUMBER &&
                                    instruction.getLineNumber() <= lineNumber &&
                                    lineNumber < nextInstruction.getLineNumber())
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
                else if (jumpOffset != lastBiJumpOffset &&
                         (bi.getBranch() <= 0 || jumpOffset > afterOffest))
                {
                    break; // Non
                }
            }
            else if (opcode == Const.GOTO)
            {
                Goto g = (Goto)instruction;
                int jumpOffset = g.getJumpOffset();

                // Ce 'goto' appartient-il au même bloc que le 1er 'if' ?
                if (jumpOffset != lastBiJumpOffset &&
                    (jumpOffset <= nextInstruction.getOffset() ||
                     jumpOffset > afterOffest))
                 {
                    break; // Non
                }

                // Recherche de l'offset de l'instruction avant le 'goto'
                if (index <= 0)
                {
                    break; // Non
                }
                Instruction lastInstructionValue1 = list.get(index-1);
                opcode = lastInstructionValue1.getOpcode();

                if (!ByteCodeUtil.isIfInstruction(opcode, false))
                {
                    break; // Non
                }

                int jumpOffsetValue1 =
                    ((BranchInstruction)lastInstructionValue1).getJumpOffset();

                if (g.getOffset() < jumpOffsetValue1 &&
                    jumpOffsetValue1 <= lastBi.getOffset())
                {
                    break; // Non
                }

                // offset de l'instruction avant le saut du goto
                Instruction lastInstructionValue2 = list.get(lastIndex);

                for (int jumpIndex=lastIndex-1; jumpIndex>index; jumpIndex--)
                {
                    Instruction jumpInstruction = list.get(jumpIndex);
                    if (jumpOffset > jumpInstruction.getOffset())
                    {
                        lastInstructionValue2 = jumpInstruction;
                        break;
                    }
                }

                opcode = lastInstructionValue2.getOpcode();

                if (!ByteCodeUtil.isIfInstruction(opcode, false))
                 {
                    break; // Non
                }

                int jumpOffsetValue2 =
                    ((BranchInstruction)lastInstructionValue2).getJumpOffset();

                if (jumpOffsetValue1 == jumpOffsetValue2)
                {
                    // Oui ! séquence dans le bon sens
                    int nextOffset = nextInstruction.getOffset();
                    for (int j=g.getOffset()+1; j<nextOffset; j++) {
                        offsetToPreviousGotoFlag[j] = true;
                    }
                }
                else if (jumpOffset == jumpOffsetValue2)
                {
                    // Oui ! séquence inversee : les offsets du Goto et du 1er
                    // sous-test sont inversés => il FAUT inverser le 1er test
                    int nextOffset = nextInstruction.getOffset();
                    for (int j=g.getOffset()+1; j<nextOffset; j++) {
                        offsetToPreviousGotoFlag[j] = true;
                    }

                    inversedTernaryOpLogic[g.getOffset()] = true;
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

    private static ComplexConditionalBranchInstruction createIfInstructions(
        boolean[] offsetToPreviousGotoFlag,
        boolean[] inversedTernaryOpLogic,
        List<Instruction> branchInstructions,
        BranchInstruction lastBi)
    {
        // Reconstruction des operateurs ternaires
        //  -> Elimination des instructions 'Goto'
        reconstructTernaryOperators(
            offsetToPreviousGotoFlag, inversedTernaryOpLogic,
            branchInstructions, lastBi);

        // Creation de l'instruction ComplexBranchList
        ComplexConditionalBranchInstruction cbl =
            assembleAndCreateIfInstructions(branchInstructions, lastBi);

        // Affectation des comparaisons    & des operateurs
        setOperator(cbl, lastBi, false);

        return cbl;
    }

    private static void reconstructTernaryOperators(
            boolean[] offsetToPreviousGotoFlag,
            boolean[] inversedTernaryOpLogic,
            List<Instruction> branchInstructions,
            BranchInstruction lastBi)
    {
        if (branchInstructions.size() <= 1) {
            return;
        }

        // Recherche des instructions 'if' sautant vers des instructions 'goto'
        // en commencant par la derniere instruction
        int index = branchInstructions.size()-1;
        int nextOffest = branchInstructions.get(index).getOffset();

        while (index-- > 0)
        {
            Instruction i = branchInstructions.get(index);

            if (ByteCodeUtil.isIfInstruction(i.getOpcode(), false))
            {
                BranchInstruction lastTernaryOpTestBi = (BranchInstruction)i;
                int lastTernaryOpTestBiJumpOffset =
                    lastTernaryOpTestBi.getJumpOffset();

                if (lastTernaryOpTestBiJumpOffset >= 0 &&
                    lastBi.getOffset() >= lastTernaryOpTestBiJumpOffset &&
                    offsetToPreviousGotoFlag[lastTernaryOpTestBiJumpOffset])
                {
                    // Extraction de la sous liste d'instructions constituant
                    // le test de l'operateur ternaire
                    List<Instruction> ternaryOpTestInstructions =
                        new ArrayList<>();
                    ternaryOpTestInstructions.add(lastTernaryOpTestBi);

                    while (index > 0)
                    {
                        Instruction ternaryOpTestInstruction =
                            branchInstructions.get(--index);

                        int opcode = ternaryOpTestInstruction.getOpcode();

                        if (!ByteCodeUtil.isIfOrGotoInstruction(opcode, false))
                        {
                            index++;
                            break;
                        }

                        BranchInstruction bi =
                            (BranchInstruction)ternaryOpTestInstruction;
                        int branchOffset = bi.getBranch();
                        int jumpOffset = bi.getOffset() + branchOffset;

                        // L'instruction if courante appartient-elle au même bloc que le 1er ?
                        if (jumpOffset != lastTernaryOpTestBiJumpOffset &&
                            (branchOffset <= 0 || jumpOffset > nextOffest))
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
                        test = createIfInstructions(
                            offsetToPreviousGotoFlag, inversedTernaryOpLogic,
                            ternaryOpTestInstructions, lastTernaryOpTestBi);
                    }
                    else
                    {
                        test = lastTernaryOpTestBi;
                    }
                    inverseComparison(test);

                    // Extraction de la sous liste d'instructions constituant
                    // la premiere valeur de l'operateur ternaire (instructions
                    // entre le test et l'instruction 'goto')
                    List<Instruction> ternaryOpValue1Instructions =
                        new ArrayList<>();

                    index++;

                    while (index < branchInstructions.size())
                    {
                        Instruction instruction =
                            branchInstructions.get(index);

                        if (instruction.getOffset() >= lastTernaryOpTestBiJumpOffset) {
                            break;
                        }

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
                        value1 = assembleAndCreateIfInstructions(
                            ternaryOpValue1Instructions, lastTernaryOpValueBi);
                    }
                    else
                    {
                        value1 = (BranchInstruction)ternaryOpValue1Instructions.get(
                            ternaryOpValue1Instructions.size()-1);
                    }

                    int gotoJumpOffset;

                    if (inversedTernaryOpLogic[g.getOffset()])
                    {
                        gotoJumpOffset = value1.getJumpOffset();
                        inverseComparison(value1);
                    }
                    else
                    {
                        gotoJumpOffset = g.getJumpOffset();
                    }

                    // Extraction de la sous liste d'instructions constituant
                    // la seconde valeur de l'operateur ternaire (instructions entre
                    // l'instruction 'goto' et la prochaine instruction 'goto' ou
                    // jusqu'au saut du test)
                    List<Instruction> ternaryOpValue2Instructions =
                        new ArrayList<>();

                    while (index < branchInstructions.size())
                    {
                        Instruction instruction =
                            branchInstructions.get(index);

                        if (instruction.getOpcode() == Const.GOTO ||
                            instruction.getOffset() >= gotoJumpOffset) {
                            break;
                        }

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
                        value2 = assembleAndCreateIfInstructions(
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
                        ByteCodeConstants.TERNARYOP, value2.getOffset(),
                        test.getLineNumber(), test, value1, value2);

                    List<Instruction> instructions =
                        new ArrayList<>(1);
                    instructions.add(to);

                    // Create complex if instruction
                    ComplexConditionalBranchInstruction cbl = new ComplexConditionalBranchInstruction(
                        ByteCodeConstants.COMPLEXIF, value2.getOffset(), test.getLineNumber(),
                        ByteCodeConstants.CMP_NONE, instructions,
                        value2.getBranch());

                    branchInstructions.set(index, cbl);
                }
            }

            nextOffest = i.getOffset();
        }
    }

    private static ComplexConditionalBranchInstruction assembleAndCreateIfInstructions(
            List<Instruction> branchInstructions,
            BranchInstruction lastBi)
    {
        int length = branchInstructions.size();
        int lastBiOffset  = lastBi.getOffset();

        // Search sub test block
        for (int i=0; i<length; ++i)
        {
            BranchInstruction branchInstruction =
                (BranchInstruction)branchInstructions.get(i);
            int jumpOffset = branchInstruction.getJumpOffset();

            if (branchInstruction.getBranch() > 0 && jumpOffset < lastBiOffset)
            {
                // Inner jump
                BranchInstruction subLastBi = lastBi;
                List<Instruction> subBranchInstructions =
                    new ArrayList<>();

                // Extract sub list
                subBranchInstructions.add(branchInstruction);
                i++;

                while (i < length)
                {
                    branchInstruction =
                        (BranchInstruction)branchInstructions.get(i);

                    if (branchInstruction.getOffset() >= jumpOffset) {
                        break;
                    }

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
                            assembleAndCreateIfInstructions(
                                    subBranchInstructions, subLastBi));
                }
            }
        }

        //
        analyzeLastTestBlock(branchInstructions);

        // First branch instruction line number
        int lineNumber = branchInstructions.get(0).getLineNumber();

        return new ComplexConditionalBranchInstruction(
            ByteCodeConstants.COMPLEXIF, lastBi.getOffset(), lineNumber,
            ByteCodeConstants.CMP_NONE, branchInstructions, lastBi.getBranch());
    }

    private static void analyzeLastTestBlock(
        List<Instruction> branchInstructions)
    {
        int length = branchInstructions.size();

        if (length > 1)
        {
            length--;
            BranchInstruction branchInstruction =
                (BranchInstruction)branchInstructions.get(0);
            int firstJumpOffset = branchInstruction.getJumpOffset();

            // Search sub list
            for (int i=1; i<length; ++i)
            {
                branchInstruction =
                    (BranchInstruction)branchInstructions.get(i);
                int jumpOffset = branchInstruction.getJumpOffset();

                if (firstJumpOffset != jumpOffset)
                {
                    BranchInstruction subLastBi = branchInstruction;
                    List<Instruction> subJumpInstructions =
                        new ArrayList<>(length);

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
                    analyzeLastTestBlock(subJumpInstructions);

                    // First branch instruction line number
                    int lineNumber = branchInstructions.get(0).getLineNumber();

                    branchInstructions.set(
                        --i, new ComplexConditionalBranchInstruction(
                            ByteCodeConstants.COMPLEXIF, subLastBi.getOffset(),
                            lineNumber, ByteCodeConstants.CMP_NONE,
                            subJumpInstructions, subLastBi.getBranch()));
                }
            }
        }
    }

    private static void setOperator(
        ComplexConditionalBranchInstruction cbl,
        BranchInstruction lastBi, boolean inverse)
    {
        List<Instruction> instructions = cbl.getInstructions();
        int lastIndex = instructions.size()-1;
        BranchInstruction firstBi = (BranchInstruction)instructions.get(0);

        if (firstBi.getJumpOffset() == lastBi.getJumpOffset())
        {
            cbl.setCmp(inverse ?
                ByteCodeConstants.CMP_AND : ByteCodeConstants.CMP_OR);

            for (int i=0; i<=lastIndex; ++i) {
                setOperator(instructions.get(i), inverse);
            }
        }
        else
        {
            cbl.setCmp(inverse ?
                ByteCodeConstants.CMP_OR : ByteCodeConstants.CMP_AND);

            // Inverse all comparaisons except last one
            boolean tmpInverse = !inverse;
            int i = 0;

            while (i < lastIndex) {
                setOperator(instructions.get(i++), tmpInverse);
            }

            setOperator(instructions.get(i), inverse);
        }
    }

    private static void setOperator(Instruction instruction, boolean inverse)
    {
        switch (instruction.getOpcode())
        {
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator to = (TernaryOperator)instruction;
                setOperator(to.getValue1(), inverse);
                setOperator(to.getValue2(), inverse);
            }
            break;
        case ByteCodeConstants.COMPLEXIF:
            {
                ComplexConditionalBranchInstruction cbl = (ComplexConditionalBranchInstruction)instruction;
                int length = cbl.getInstructions().size();

                if (length == 1)
                {
                    setOperator(cbl.getInstructions().get(0), inverse);
                }
                else if (length > 1)
                {
                    setOperator(
                        cbl,
                        (BranchInstruction)cbl.getInstructions().get(length-1),
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
                    cbi.setCmp(ByteCodeConstants.CMP_MAX_INDEX - cbi.getCmp());
                }
            }
            break;
        }
    }

    public static void inverseComparison(Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFCMP,
             ByteCodeConstants.IFXNULL:
            ConditionalBranchInstruction cbi =
                (ConditionalBranchInstruction)instruction;
            cbi.setCmp(ByteCodeConstants.CMP_MAX_INDEX - cbi.getCmp());
            break;
        case ByteCodeConstants.COMPLEXIF:
            ComplexConditionalBranchInstruction ccbi =
                (ComplexConditionalBranchInstruction)instruction;
            ccbi.setCmp(ByteCodeConstants.CMP_OR - ccbi.getCmp());
            for (int i=ccbi.getInstructions().size()-1; i>=0; --i) {
                inverseComparison(ccbi.getInstructions().get(i));
            }
            break;
        case ByteCodeConstants.TERNARYOP:
            TernaryOperator to = (TernaryOperator)instruction;
            inverseComparison(to.getValue1());
            inverseComparison(to.getValue2());
            break;
//        default:
//            System.out.println("debug");
        }
    }

    public static int getLastIndex(List<Instruction> list, int firstIndex)
    {
        int length = list.size();
        int index = firstIndex+1;

        // Recherche de la potentielle derniere instruction de saut
        while (index < length)
        {
            Instruction instruction = list.get(index);
            int opcode = instruction.getOpcode();

            if (!ByteCodeUtil.isIfOrGotoInstruction(opcode, false)) {
                break;
            }
            index++;
        }

        if (index-1 == firstIndex) {
            return firstIndex;
        }

        boolean[] dummy = new boolean[list.get(length-1).getOffset()];

        while (--index > firstIndex)
        {
            // Verification que la potentielle derniere instruction de saut a
            // comme premiere instruction l'instruction à  l'indexe 'firstIndex'
            // Recherche de l'indexe de la premiere instruction 'if' du bloc et
            // initialisation de 'offsetToPreviousGotoFlag'
            BranchInstruction lastBi = (BranchInstruction)list.get(index);
            int afterOffest = index+1 < length ? list.get(index+1).getOffset() : -1;

            int firstIndexTmp = searchFirstIndex(
                list, index, lastBi, afterOffest, dummy, dummy);

            firstIndexTmp = reduceFirstIndex(list, firstIndexTmp, index);

            if (firstIndex == firstIndexTmp)
            {
                // trouvé
                break;
            }
        }

        return index;
    }
}
