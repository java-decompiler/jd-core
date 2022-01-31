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
package jd.core.process.layouter;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;
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
import jd.core.model.layout.block.BlockLayoutBlock;
import jd.core.model.layout.block.CaseBlockEndLayoutBlock;
import jd.core.model.layout.block.CaseBlockStartLayoutBlock;
import jd.core.model.layout.block.CaseEnumLayoutBlock;
import jd.core.model.layout.block.CaseLayoutBlock;
import jd.core.model.layout.block.DeclareLayoutBlock;
import jd.core.model.layout.block.FastCatchLayoutBlock;
import jd.core.model.layout.block.FragmentLayoutBlock;
import jd.core.model.layout.block.LayoutBlock;
import jd.core.model.layout.block.LayoutBlockConstants;
import jd.core.model.layout.block.OffsetLayoutBlock;
import jd.core.model.layout.block.SeparatorLayoutBlock;
import jd.core.model.layout.block.SingleStatementBlockEndLayoutBlock;
import jd.core.model.layout.block.SingleStatementBlockStartLayoutBlock;
import jd.core.model.layout.block.StatementsBlockEndLayoutBlock;
import jd.core.model.layout.block.StatementsBlockStartLayoutBlock;
import jd.core.model.layout.block.SwitchBlockEndLayoutBlock;
import jd.core.model.layout.block.SwitchBlockStartLayoutBlock;
import jd.core.preferences.Preferences;
import jd.core.process.layouter.visitor.InstructionSplitterVisitor;
import jd.core.process.layouter.visitor.InstructionsSplitterVisitor;
import jd.core.process.layouter.visitor.MaxLineNumberVisitor;

public class JavaSourceLayouter
{
    private final InstructionSplitterVisitor instructionSplitterVisitor;
    private final InstructionsSplitterVisitor instructionsSplitterVisitor;

    public JavaSourceLayouter()
    {
        this.instructionSplitterVisitor = new InstructionSplitterVisitor();
        this.instructionsSplitterVisitor = new InstructionsSplitterVisitor();
    }

    public boolean createBlocks(
            Preferences preferences,
            List<LayoutBlock> layoutBlockList, ClassFile classFile,
            Method method, List<Instruction> list)
    {
        int length = list.size();
        boolean singleLine = false;

        Instruction instruction;
        for (int index=0; index<length; index++)
        {
            instruction = list.get(index);

            if (index > 0)
            {
                layoutBlockList.add(new SeparatorLayoutBlock(
                        LayoutBlockConstants.SEPARATOR_OF_STATEMENTS, 1));
            }

            switch (instruction.getOpcode())
            {
            case FastConstants.WHILE:
                createBlockForFastTestList(
                        preferences, LayoutBlockConstants.FRAGMENT_WHILE,
                        layoutBlockList, classFile, method,
                        (FastTestList)instruction, true);
                break;
            case FastConstants.DO_WHILE:
                createBlocksForDoWhileLoop(
                        preferences, layoutBlockList, classFile,
                        method, (FastTestList)instruction);
                break;
            case FastConstants.INFINITE_LOOP:
                createBlocksForInfiniteLoop(
                        preferences, layoutBlockList, classFile,
                        method, (FastList)instruction);
                break;
            case FastConstants.FOR:
                createBlocksForForLoop(
                        preferences, layoutBlockList, classFile,
                        method, (FastFor)instruction);
                break;
            case FastConstants.FOREACH:
                createBlockForFastForEach(
                        preferences, layoutBlockList, classFile,
                        method, (FastForEach)instruction);
                break;
            case FastConstants.IF_SIMPLE:
                createBlockForFastTestList(
                        preferences, LayoutBlockConstants.FRAGMENT_IF,
                        layoutBlockList, classFile, method,
                        (FastTestList)instruction, true);
                break;
            case FastConstants.IF_ELSE:
                FastTest2Lists ft2l = (FastTest2Lists)instruction;
                createBlocksForIfElse(
                        preferences, layoutBlockList, classFile, method,
                        ft2l, showSingleInstructionBlock(ft2l));
                break;
            case FastConstants.IF_CONTINUE,
                 FastConstants.IF_BREAK:
                createBlocksForIfContinueOrBreak(
                        preferences, layoutBlockList, classFile,
                        method, (FastInstruction)instruction);
                break;
            case FastConstants.IF_LABELED_BREAK:
                createBlocksForIfLabeledBreak(
                        preferences, layoutBlockList, classFile,
                        method, (FastInstruction)instruction);
                break;
            //            case FastConstants.GOTO_CONTINUE:
            //                CreateBlocksForGotoContinue(layoutBlockList);
            //            case FastConstants.GOTO_BREAK:
            //                CreateBlocksForGotoBreak(layoutBlockList);
            //                break;
            case FastConstants.GOTO_LABELED_BREAK:
                createBlocksForGotoLabeledBreak(
                        layoutBlockList, (FastInstruction)instruction);
                break;
            case FastConstants.SWITCH:
                createBlocksForSwitch(
                        preferences, layoutBlockList, classFile, method,
                        (FastSwitch)instruction, LayoutBlockConstants.FRAGMENT_CASE);
                break;
            case FastConstants.SWITCH_ENUM:
                createBlocksForSwitchEnum(
                        preferences, layoutBlockList, classFile, method,
                        (FastSwitch)instruction);
                break;
            case FastConstants.SWITCH_STRING:
                createBlocksForSwitch(
                        preferences, layoutBlockList, classFile, method,
                        (FastSwitch)instruction,
                        LayoutBlockConstants.FRAGMENT_CASE_STRING);
                break;
            case FastConstants.TRY:
                createBlocksForTry(
                        preferences, layoutBlockList, classFile,
                        method, (FastTry)instruction);
                break;
            case FastConstants.SYNCHRONIZED:
                createBlocksForSynchronized(
                        preferences, layoutBlockList, classFile,
                        method, (FastSynchronized)instruction);
                break;
            case FastConstants.LABEL:
                createBlocksForLabel(
                        preferences, layoutBlockList, classFile, method,
                        (FastLabel)instruction);
                break;
            case FastConstants.DECLARE:
                if (((FastDeclaration)instruction).getInstruction() == null)
                {
                    layoutBlockList.add(new DeclareLayoutBlock(
                            classFile, method, instruction));
                    break;
                }
                // intended fall through
            default:
                if (length == 1)
                {
                    int min = instruction.getLineNumber();
                    if (min != Instruction.UNKNOWN_LINE_NUMBER)
                    {
                        int max = MaxLineNumberVisitor.visit(instruction);
                        singleLine = min == max;
                    }
                }

                index = createBlockForInstructions(
                        preferences, layoutBlockList,
                        classFile, method, list, index);
                break;
            }
        }

        return singleLine;
    }

    private void createBlockForFastTestList(
            Preferences preferences,
            byte tag, List<LayoutBlock> layoutBlockList, ClassFile classFile,
            Method method, FastTestList ftl, boolean showSingleInstructionBlock)
    {
        layoutBlockList.add(new FragmentLayoutBlock(tag));

        createBlockForInstruction(
                preferences, layoutBlockList, classFile, method, ftl.getTest());

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

        createBlockForSubList(
                preferences, layoutBlockList, classFile, method,
                ftl.getInstructions(), showSingleInstructionBlock, 1);
    }

    private void createBlocksForIfElse(
            Preferences preferences,
            List<LayoutBlock> layoutBlockList, ClassFile classFile, Method method,
            FastTest2Lists ft2l, boolean showSingleInstructionBlock)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_IF));

        createBlockForInstruction(
                preferences, layoutBlockList, classFile, method, ft2l.getTest());

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

        List<Instruction> instructions = ft2l.getInstructions();

        if (instructions.size() == 1 && (instructions.get(0).getOpcode() == FastConstants.IF_SIMPLE || instructions.get(0).getOpcode() == FastConstants.IF_ELSE)) {
            createBlockForSubList(
                    preferences, layoutBlockList, classFile,
                    method, instructions, false, 2);
        } else {
            createBlockForSubList(
                    preferences, layoutBlockList, classFile, method,
                    instructions, showSingleInstructionBlock, 2);
        }

        List<Instruction> instructions2 = ft2l.getInstructions2();

        if (instructions2.size() == 1)
        {
            Instruction instruction = instructions2.get(0);

            // Write 'else if'
            if (instruction.getOpcode() == FastConstants.IF_SIMPLE) {
                layoutBlockList.add(new FragmentLayoutBlock(
                        LayoutBlockConstants.FRAGMENT_ELSE_SPACE));

                createBlockForFastTestList(
                        preferences, LayoutBlockConstants.FRAGMENT_IF, layoutBlockList,
                        classFile, method, (FastTestList)instruction,
                        showSingleInstructionBlock);
                return;
            }
            if (instruction.getOpcode() == FastConstants.IF_ELSE) {
                layoutBlockList.add(new FragmentLayoutBlock(
                        LayoutBlockConstants.FRAGMENT_ELSE_SPACE));

                createBlocksForIfElse(
                        preferences, layoutBlockList, classFile, method,
                        (FastTest2Lists)instruction, showSingleInstructionBlock);
                return;
            }
        }

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_ELSE));

        createBlockForSubList(
                preferences, layoutBlockList, classFile, method,
                instructions2, showSingleInstructionBlock, 1);
    }

    /** Show single instruction block ? */
    private static boolean showSingleInstructionBlock(FastTest2Lists ifElse)
    {
        List<Instruction> instructions;
        int instructions2Size;
        for (;;)
        {
            instructions = ifElse.getInstructions();
            if (instructions != null && instructions.size() >= 2) {
                return false;
            }

            instructions2Size = ifElse.getInstructions2().size();

            if (instructions2Size == 0) {
                return true;
            }

            if (instructions2Size >= 2) {
                return false;
            }

            if (instructions2Size == 1)
            {
                Instruction instruction = ifElse.getInstructions2().get(0);

                if (instruction.getOpcode() == FastConstants.IF_SIMPLE)
                {
                    instructions = ((FastTestList)instruction).getInstructions();
                    return instructions == null || instructions.size() < 2;
                }
                if (instruction.getOpcode() != FastConstants.IF_ELSE) {
                    return true;
                }
                ifElse = (FastTest2Lists)instruction;
            }
        }
    }

    private void createBlocksForDoWhileLoop(
            Preferences preferences, List<LayoutBlock> layoutBlockList,
            ClassFile classFile, Method method, FastTestList ftl)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_DO));

        createBlockForSubList(
                preferences, layoutBlockList, classFile,
                method, ftl.getInstructions(), false, 1);

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_WHILE));

        createBlockForInstruction(
                preferences, layoutBlockList, classFile, method, ftl.getTest());

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET_SEMICOLON));
    }

    private void createBlocksForInfiniteLoop(
            Preferences preferences,
            List<LayoutBlock> layoutBlockList, ClassFile classFile,
            Method method, FastList fl)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_INFINITE_LOOP));

        createBlockForSubList(
                preferences, layoutBlockList, classFile,
                method, fl.getInstructions(), false, 1);
    }

    private void createBlocksForForLoop(
            Preferences preferences, List<LayoutBlock> layoutBlockList,
            ClassFile classFile, Method method, FastFor ff)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_FOR));

        if (ff.getInit() != null)
        {
            BlockLayoutBlock sblb = new BlockLayoutBlock(
                    LayoutBlockConstants.FOR_BLOCK_START, 0);
            layoutBlockList.add(sblb);

            createBlockForInstruction(
                    preferences, layoutBlockList, classFile, method, ff.getInit());

            BlockLayoutBlock eblb = new BlockLayoutBlock(
                    LayoutBlockConstants.FOR_BLOCK_END, 0);
            sblb.setOther(eblb);
            eblb.setOther(sblb);
            layoutBlockList.add(eblb);
        }

        if (ff.getTest() == null)
        {
            layoutBlockList.add(new FragmentLayoutBlock(
                    LayoutBlockConstants.FRAGMENT_SEMICOLON));
        }
        else
        {
            layoutBlockList.add(new FragmentLayoutBlock(
                    LayoutBlockConstants.FRAGMENT_SEMICOLON_SPACE));

            BlockLayoutBlock sblb = new BlockLayoutBlock(
                    LayoutBlockConstants.FOR_BLOCK_START,
                    0, LayoutBlockConstants.UNLIMITED_LINE_COUNT, 0);
            layoutBlockList.add(sblb);

            createBlockForInstruction(
                    preferences, layoutBlockList, classFile, method, ff.getTest());

            BlockLayoutBlock eblb = new BlockLayoutBlock(
                    LayoutBlockConstants.FOR_BLOCK_END, 0);
            sblb.setOther(eblb);
            eblb.setOther(sblb);
            layoutBlockList.add(eblb);
        }

        if (ff.getInc() == null)
        {
            layoutBlockList.add(new FragmentLayoutBlock(
                    LayoutBlockConstants.FRAGMENT_SEMICOLON));
        }
        else
        {
            layoutBlockList.add(new FragmentLayoutBlock(
                    LayoutBlockConstants.FRAGMENT_SEMICOLON_SPACE));

            BlockLayoutBlock sblb = new BlockLayoutBlock(
                    LayoutBlockConstants.FOR_BLOCK_START,
                    0, LayoutBlockConstants.UNLIMITED_LINE_COUNT, 0);
            layoutBlockList.add(sblb);

            createBlockForInstruction(
                    preferences, layoutBlockList, classFile, method, ff.getInc());

            BlockLayoutBlock eblb = new BlockLayoutBlock(
                    LayoutBlockConstants.FOR_BLOCK_END, 0);
            sblb.setOther(eblb);
            eblb.setOther(sblb);
            layoutBlockList.add(eblb);
        }

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

        createBlockForSubList(
                preferences, layoutBlockList, classFile,
                method, ff.getInstructions(), true, 1);
    }

    private void createBlockForFastForEach(
            Preferences preferences, List<LayoutBlock> layoutBlockList,
            ClassFile classFile, Method method, FastForEach ffe)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_FOR));

        createBlockForInstruction(
                preferences, layoutBlockList, classFile, method, ffe.getVariable());

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_SPACE_COLON_SPACE));

        createBlockForInstruction(
                preferences, layoutBlockList, classFile, method, ffe.getValues());

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

        createBlockForSubList(
                preferences, layoutBlockList, classFile,
                method, ffe.getInstructions(), true, 1);
    }

    private void createBlocksForIfContinueOrBreak(
            Preferences preferences, List<LayoutBlock> layoutBlockList,
            ClassFile classFile, Method method, FastInstruction fi)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_IF));

        createBlockForInstruction(
                preferences, layoutBlockList, classFile, method, fi.getInstruction());

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

        SingleStatementBlockStartLayoutBlock ssbslb =
                new SingleStatementBlockStartLayoutBlock();
        layoutBlockList.add(ssbslb);

        if (fi.getOpcode() == FastConstants.IF_CONTINUE)
        {
            layoutBlockList.add(new FragmentLayoutBlock(
                    LayoutBlockConstants.FRAGMENT_CONTINUE));
        }
        else
        {
            layoutBlockList.add(new FragmentLayoutBlock(
                    LayoutBlockConstants.FRAGMENT_BREAK));
        }

        SingleStatementBlockEndLayoutBlock ssbelb =
                new SingleStatementBlockEndLayoutBlock(1);
        ssbslb.setOther(ssbelb);
        ssbelb.setOther(ssbslb);
        layoutBlockList.add(ssbelb);
    }

    private void createBlocksForIfLabeledBreak(
            Preferences preferences, List<LayoutBlock> layoutBlockList,
            ClassFile classFile, Method method, FastInstruction fi)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_IF));

        createBlockForInstruction(
                preferences, layoutBlockList, classFile, method, fi.getInstruction());

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

        SingleStatementBlockStartLayoutBlock ssbslb =
                new SingleStatementBlockStartLayoutBlock();
        layoutBlockList.add(ssbslb);

        BranchInstruction bi = (BranchInstruction)fi.getInstruction();

        layoutBlockList.add(new OffsetLayoutBlock(
                LayoutBlockConstants.FRAGMENT_LABELED_BREAK,
                Instruction.UNKNOWN_LINE_NUMBER,
                Instruction.UNKNOWN_LINE_NUMBER,
                0, 0, 0,
                bi.getJumpOffset()));

        SingleStatementBlockEndLayoutBlock ssbelb =
                new SingleStatementBlockEndLayoutBlock(1);
        ssbslb.setOther(ssbelb);
        ssbelb.setOther(ssbslb);
        layoutBlockList.add(ssbelb);
    }

    private static void createBlocksForGotoLabeledBreak(
            List<LayoutBlock> layoutBlockList, FastInstruction fi)
    {
        BranchInstruction bi = (BranchInstruction)fi.getInstruction();

        layoutBlockList.add(new OffsetLayoutBlock(
                LayoutBlockConstants.FRAGMENT_LABELED_BREAK,
                Instruction.UNKNOWN_LINE_NUMBER,
                Instruction.UNKNOWN_LINE_NUMBER,
                0, 0, 0,
                bi.getJumpOffset()));
    }

    private void createBlocksForSwitch(
            Preferences preferences,
            List<LayoutBlock> layoutBlockList, ClassFile classFile,
            Method method, FastSwitch fs, byte tagCase)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_SWITCH));

        createBlockForInstruction(
                preferences, layoutBlockList, classFile, method, fs.getTest());

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

        BlockLayoutBlock sbslb = new SwitchBlockStartLayoutBlock();
        layoutBlockList.add(sbslb);

        Pair[] pairs = fs.getPairs();
        int length = pairs.length;
        int firstIndex = 0;

        boolean last;
        Pair pair;
        List<Instruction> instructions;
        for (int i=0; i<length; i++)
        {
            last = i == length-1;
            pair = pairs[i];
            instructions = pair.getInstructions();

            // Do'nt write default case on last position with empty
            // instructions list.
            if (pair.isDefault() && last && (instructions == null || instructions.isEmpty() || instructions.size() == 1 &&
                    instructions.get(0).getOpcode() == FastConstants.GOTO_BREAK)) {
                break;
            }

            if (instructions != null)
            {
                layoutBlockList.add(new CaseLayoutBlock(
                        tagCase, classFile, method, fs, firstIndex, i));
                firstIndex = i+1;

                layoutBlockList.add(new CaseBlockStartLayoutBlock());
                createBlocks(
                        preferences, layoutBlockList, classFile, method, instructions);
                layoutBlockList.add(new CaseBlockEndLayoutBlock());
            }
        }

        BlockLayoutBlock sbelb = new SwitchBlockEndLayoutBlock();
        sbslb.setOther(sbelb);
        sbelb.setOther(sbslb);
        layoutBlockList.add(sbelb);
    }

    private void createBlocksForSwitchEnum(
            Preferences preferences,
            List<LayoutBlock> layoutBlockList, ClassFile classFile,
            Method method, FastSwitch fs)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_SWITCH));

        Instruction test = fs.getTest();

        ConstantPool constants = classFile.getConstantPool();
        int switchMapKeyIndex = -1;

        if (test.getOpcode() == ByteCodeConstants.ARRAYLOAD)
        {
            ArrayLoadInstruction ali = (ArrayLoadInstruction)test;
            ConstantNameAndType cnat;

            if (ali.getArrayref().getOpcode() == Const.INVOKESTATIC)
            {
                // Dans le cas des instructions Switch+Enum d'Eclipse, la clé de la map
                // est l'indexe du nom de la méthode
                // "static int[] $SWITCH_TABLE$basic$data$TestEnum$enum1()".
                Invokestatic is = (Invokestatic)ali.getArrayref();
                ConstantMethodref cmr = constants.getConstantMethodref(is.getIndex());
                cnat = constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
            }
            else if (ali.getArrayref().getOpcode() == Const.GETSTATIC)
            {
                // Dans le cas des instructions Switch+Enum des autres compilateurs,
                // la clé de la map est l'indexe du nom de la classe interne
                // "static class 1" contenant le tableau de correspondance
                // "$SwitchMap$basic$data$TestEnum$enum1".
                GetStatic gs = (GetStatic)ali.getArrayref();
                ConstantFieldref cfr = constants.getConstantFieldref(gs.getIndex());
                cnat = constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
            }
            else
            {
                throw new IllegalStateException();
            }

            switchMapKeyIndex = cnat.getNameIndex();

            Invokevirtual iv = (Invokevirtual)ali.getIndexref();

            createBlockForInstruction(
                    preferences, layoutBlockList, classFile, method, iv.getObjectref());
        }

        if (switchMapKeyIndex == -1) {
            throw new IllegalStateException();
        }

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

        BlockLayoutBlock sbslb = new SwitchBlockStartLayoutBlock();
        layoutBlockList.add(sbslb);

        Pair[] pairs = fs.getPairs();
        int length = pairs.length;
        int firstIndex = 0;

        boolean last;
        Pair pair;
        List<Instruction> instructions;
        for (int i=0; i<length; i++)
        {
            last = i == length-1;
            pair = pairs[i];
            instructions = pair.getInstructions();

            // Do'nt write default case on last position with empty
            // instructions list.
            if (pair.isDefault() && last && (instructions == null || instructions.isEmpty() || instructions.size() == 1 &&
                    instructions.get(0).getOpcode() == FastConstants.GOTO_BREAK)) {
                break;
            }

            if (instructions != null)
            {
                layoutBlockList.add(new CaseEnumLayoutBlock(
                        classFile, method, fs, firstIndex, i, switchMapKeyIndex));
                firstIndex = i+1;

                layoutBlockList.add(new CaseBlockStartLayoutBlock());
                createBlocks(
                        preferences, layoutBlockList, classFile, method, instructions);
                layoutBlockList.add(new CaseBlockEndLayoutBlock());
            }
        }

        BlockLayoutBlock sbelb = new SwitchBlockEndLayoutBlock();
        sbslb.setOther(sbelb);
        sbelb.setOther(sbslb);
        layoutBlockList.add(sbelb);
    }

    private void createBlocksForTry(
            Preferences preferences, List<LayoutBlock> layoutBlockList,
            ClassFile classFile, Method method, FastTry ft)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_TRY));

        createBlockForSubList(
                preferences, layoutBlockList, classFile,
                method, ft.getInstructions(), false, 2);

        if (ft.getCatches() != null)
        {
            int length = ft.getCatches().size();

            if (length > 0)
            {
                length--;

                // First catch blocks
                for (int i=0; i<length; ++i)
                {
                    FastCatch fc = ft.getCatches().get(i);

                    layoutBlockList.add(
                            new FastCatchLayoutBlock(classFile, method, fc));

                    createBlockForSubList(
                            preferences, layoutBlockList, classFile,
                            method, fc.instructions(), false, 2);
                }

                // Last catch block
                FastCatch fc = ft.getCatches().get(length);

                layoutBlockList.add(
                        new FastCatchLayoutBlock(classFile, method, fc));

                int blockEndPreferedLineCount =
                        ft.getFinallyInstructions() == null ? 1 : 2;

                createBlockForSubList(
                        preferences, layoutBlockList, classFile, method,
                        fc.instructions(), false, blockEndPreferedLineCount);
            }
        }

        if (ft.getFinallyInstructions() != null)
        {
            layoutBlockList.add(new FragmentLayoutBlock(
                    LayoutBlockConstants.FRAGMENT_FINALLY));

            createBlockForSubList(
                    preferences, layoutBlockList, classFile,
                    method, ft.getFinallyInstructions(), false, 1);
        }
    }

    private void createBlocksForSynchronized(
            Preferences preferences, List<LayoutBlock> layoutBlockList,
            ClassFile classFile, Method method, FastSynchronized fs)
    {
        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_SYNCHRONIZED));

        createBlockForInstruction(
                preferences, layoutBlockList, classFile, method, fs.getMonitor());

        layoutBlockList.add(new FragmentLayoutBlock(
                LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

        createBlockForSubList(
                preferences, layoutBlockList, classFile,
                method, fs.getInstructions(), false, 1);
    }

    private void createBlocksForLabel(
            Preferences preferences,
            List<LayoutBlock> layoutBlockList, ClassFile classFile,
            Method method, FastLabel fl)
    {
        layoutBlockList.add(new OffsetLayoutBlock(
                LayoutBlockConstants.STATEMENT_LABEL,
                Instruction.UNKNOWN_LINE_NUMBER,
                Instruction.UNKNOWN_LINE_NUMBER,
                0, 0, 0,
                fl.getOffset()));

        Instruction instruction = fl.getInstruction();

        if (instruction != null)
        {
            layoutBlockList.add(new SeparatorLayoutBlock(
                    LayoutBlockConstants.SEPARATOR_OF_STATEMENTS, 1));

            switch (instruction.getOpcode())
            {
            case FastConstants.WHILE:
                createBlockForFastTestList(
                        preferences, LayoutBlockConstants.FRAGMENT_WHILE,
                        layoutBlockList, classFile, method,
                        (FastTestList)instruction, true);
                break;
            case FastConstants.DO_WHILE:
                createBlocksForDoWhileLoop(
                        preferences, layoutBlockList, classFile, method,
                        (FastTestList)instruction);
                break;
            case FastConstants.INFINITE_LOOP:
                createBlocksForInfiniteLoop(
                        preferences, layoutBlockList, classFile,
                        method, (FastList)instruction);
                break;
            case FastConstants.FOR:
                createBlocksForForLoop(
                        preferences, layoutBlockList, classFile,
                        method, (FastFor)instruction);
                break;
            case FastConstants.FOREACH:
                createBlockForFastForEach(
                        preferences, layoutBlockList, classFile,
                        method, (FastForEach)instruction);
                break;
            case FastConstants.IF_SIMPLE:
                createBlockForFastTestList(
                        preferences, LayoutBlockConstants.FRAGMENT_IF,
                        layoutBlockList, classFile, method,
                        (FastTestList)instruction, true);
                break;
            case FastConstants.IF_ELSE:
                FastTest2Lists ft2l = (FastTest2Lists)instruction;
                createBlocksForIfElse(
                        preferences, layoutBlockList, classFile, method,
                        ft2l, showSingleInstructionBlock(ft2l));
                break;
            case FastConstants.IF_CONTINUE,
                 FastConstants.IF_BREAK:
                createBlocksForIfContinueOrBreak(
                        preferences, layoutBlockList, classFile, method,
                        (FastInstruction)instruction);
                break;
            case FastConstants.IF_LABELED_BREAK:
                createBlocksForIfLabeledBreak(
                        preferences, layoutBlockList, classFile,
                        method, (FastInstruction)instruction);
                break;
                //                    case FastConstants.GOTO_CONTINUE:
                    //                        CreateBlocksForGotoContinue(layoutBlockList);
                    //                    case FastConstants.GOTO_BREAK:
                        //                        CreateBlocksForGotoBreak(layoutBlockList);
                        //                        break;
                    case FastConstants.GOTO_LABELED_BREAK:
                        createBlocksForGotoLabeledBreak(
                                layoutBlockList, (FastInstruction)instruction);
                        break;
                    case FastConstants.SWITCH:
                        createBlocksForSwitch(
                                preferences, layoutBlockList, classFile, method,
                                (FastSwitch)instruction, LayoutBlockConstants.FRAGMENT_CASE);
                        break;
                    case FastConstants.SWITCH_ENUM:
                        createBlocksForSwitchEnum(
                                preferences, layoutBlockList, classFile, method,
                                (FastSwitch)instruction);
                        break;
                    case FastConstants.SWITCH_STRING:
                        createBlocksForSwitch(
                                preferences, layoutBlockList, classFile, method,
                                (FastSwitch)instruction,
                                LayoutBlockConstants.FRAGMENT_CASE_STRING);
                        break;
                    case FastConstants.TRY:
                        createBlocksForTry(
                                preferences, layoutBlockList, classFile,
                                method, (FastTry)instruction);
                        break;
                    case FastConstants.SYNCHRONIZED:
                        createBlocksForSynchronized(
                                preferences, layoutBlockList, classFile,
                                method, (FastSynchronized)instruction);
                        break;
                    case FastConstants.LABEL:
                        createBlocksForLabel(
                                preferences, layoutBlockList, classFile, method,
                                (FastLabel)instruction);
                        break;
                    case FastConstants.DECLARE:
                        if (((FastDeclaration)instruction).getInstruction() == null)
                        {
                            layoutBlockList.add(new DeclareLayoutBlock(
                                    classFile, method, instruction));
                            break;
                        }
                        // intended fall through
                    default:
                    {
                        createBlockForInstruction(
                                preferences, layoutBlockList, classFile,
                                method, instruction);
                        layoutBlockList.add(new FragmentLayoutBlock(
                                LayoutBlockConstants.FRAGMENT_SEMICOLON));
                    }
            }
        }
    }

    /**
     * @param layoutBlockList
     * @param classFile
     * @param method
     * @param instructions
     * @param showSingleInstructionBlock
     * @param blockEndPreferedLineCount
     *                 2 pour les premiers blocks,
     *                 1 pour le dernier bloc
     */
    private void createBlockForSubList(
            Preferences preferences,
            List<LayoutBlock> layoutBlockList,
            ClassFile classFile,
            Method method,
            List<Instruction> instructions,
            boolean showSingleInstructionBlock,
            int blockEndPreferedLineCount)
    {
        if (instructions == null || instructions.isEmpty())
        {
            StatementsBlockStartLayoutBlock sbslb =
                    new StatementsBlockStartLayoutBlock();
            sbslb.transformToStartEndBlock(0);
            layoutBlockList.add(sbslb);
        }
        else
        {
            if (instructions.size() > 1) {
                showSingleInstructionBlock = false;
            }

            BlockLayoutBlock sbslb =
                    showSingleInstructionBlock ?
                            new SingleStatementBlockStartLayoutBlock() :
                                new StatementsBlockStartLayoutBlock();
            layoutBlockList.add(sbslb);

            createBlocks(
                    preferences, layoutBlockList, classFile, method, instructions);

            BlockLayoutBlock sbelb =
                    showSingleInstructionBlock ?
                            new SingleStatementBlockEndLayoutBlock(0+1 /* TENTATIVE blockEndPreferedLineCount */) :
                                new StatementsBlockEndLayoutBlock(blockEndPreferedLineCount);
            sbslb.setOther(sbelb);
            sbelb.setOther(sbslb);
            layoutBlockList.add(sbelb);
        }
    }

    private void createBlockForInstruction(
            Preferences preferences,
            List<LayoutBlock> layoutBlockList, ClassFile classFile,
            Method method, Instruction instruction)
    {
        this.instructionSplitterVisitor.start(
                preferences, layoutBlockList, classFile, method, instruction);
        this.instructionSplitterVisitor.visit(instruction);
        this.instructionSplitterVisitor.end();
    }

    private int createBlockForInstructions(
            Preferences preferences,
            List<LayoutBlock> layoutBlockList, ClassFile classFile,
            Method method, List<Instruction> list, int index1)
    {
        int index2 = skipInstructions(list, index1);

        instructionsSplitterVisitor.start(
                preferences, layoutBlockList, classFile, method, list, index1);

        for (int index=index1; index<=index2; index++)
        {
            instructionsSplitterVisitor.setIndex2(index);
            instructionsSplitterVisitor.visit(list.get(index));
        }

        instructionsSplitterVisitor.end();

        return index2;
    }

    private static int skipInstructions(List<Instruction> list, int index)
    {
        int length = list.size();

        Instruction instruction;
        while (++index < length)
        {
            instruction = list.get(index);

            if (instruction.getOpcode() == FastConstants.WHILE
             || instruction.getOpcode() == FastConstants.DO_WHILE
             || instruction.getOpcode() == FastConstants.INFINITE_LOOP
             || instruction.getOpcode() == FastConstants.FOR
             || instruction.getOpcode() == FastConstants.FOREACH
             || instruction.getOpcode() == FastConstants.IF_SIMPLE
             || instruction.getOpcode() == FastConstants.IF_ELSE
             || instruction.getOpcode() == FastConstants.IF_CONTINUE
             || instruction.getOpcode() == FastConstants.IF_BREAK
             || instruction.getOpcode() == FastConstants.IF_LABELED_BREAK
             || instruction.getOpcode() == FastConstants.GOTO_LABELED_BREAK
             || instruction.getOpcode() == FastConstants.SWITCH
             || instruction.getOpcode() == FastConstants.SWITCH_ENUM
             || instruction.getOpcode() == FastConstants.SWITCH_STRING
             || instruction.getOpcode() == FastConstants.TRY
             || instruction.getOpcode() == FastConstants.SYNCHRONIZED
             || instruction.getOpcode() == FastConstants.LABEL
             || instruction.getOpcode() == FastConstants.DECLARE
             && ((FastDeclaration)instruction).getInstruction() == null) {
                return index-1;
            }
        }

        return length-1;
    }
}
