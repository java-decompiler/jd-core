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
package jd.core.process.layouter;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
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
import jd.core.model.instruction.fast.instruction.FastSynchronized;
import jd.core.model.instruction.fast.instruction.FastTest2Lists;
import jd.core.model.instruction.fast.instruction.FastTestList;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.instruction.fast.instruction.FastSwitch.Pair;
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
	InstructionSplitterVisitor instructionSplitterVisitor;
	InstructionsSplitterVisitor instructionsSplitterVisitor;
	
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
		
		for (int index=0; index<length; index++)
		{
			Instruction instruction = list.get(index);
			
			if (index > 0)
			{
				layoutBlockList.add(new SeparatorLayoutBlock(
					LayoutBlockConstants.SEPARATOR_OF_STATEMENTS, 1));
			}
			
			switch (instruction.opcode)
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
			case FastConstants.IF_:
				createBlockForFastTestList(
					preferences, LayoutBlockConstants.FRAGMENT_IF, 
					layoutBlockList, classFile, method, 
					(FastTestList)instruction, true);
				break;
			case FastConstants.IF_ELSE:
				FastTest2Lists ft2l = (FastTest2Lists)instruction;
				createBlocksForIfElse(
					preferences, layoutBlockList, classFile, method, 
					ft2l, ShowSingleInstructionBlock(ft2l));
				break;
			case FastConstants.IF_CONTINUE:
			case FastConstants.IF_BREAK:
				createBlocksForIfContinueOrBreak(
					preferences, layoutBlockList, classFile, 
					method, (FastInstruction)instruction);
				break;
			case FastConstants.IF_LABELED_BREAK:
				createBlocksForIfLabeledBreak(
					preferences, layoutBlockList, classFile, 
					method, (FastInstruction)instruction);
				break;
//			case FastConstants.GOTO_CONTINUE:
//				CreateBlocksForGotoContinue(layoutBlockList);
//			case FastConstants.GOTO_BREAK:
//				CreateBlocksForGotoBreak(layoutBlockList);
//				break;
			case FastConstants.GOTO_LABELED_BREAK:
				CreateBlocksForGotoLabeledBreak(
					layoutBlockList, classFile, method, 
					(FastInstruction)instruction);
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
				if (((FastDeclaration)instruction).instruction == null)
				{
					layoutBlockList.add(new DeclareLayoutBlock(
						classFile, method, instruction));
					break;
				}				
			default:
				if (length == 1)
				{		
					int min = instruction.lineNumber;					
					if (min != Instruction.UNKNOWN_LINE_NUMBER)
					{
						int max = MaxLineNumberVisitor.visit(instruction);
						singleLine = (min == max);
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
			preferences, layoutBlockList, classFile, method, ftl.test);
		
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));
			
		createBlockForSubList(
			preferences, layoutBlockList, classFile, method, 
			ftl.instructions, showSingleInstructionBlock, 1);
	}
		
	private void createBlocksForIfElse(
		Preferences preferences, 
		List<LayoutBlock> layoutBlockList, ClassFile classFile, Method method, 
		FastTest2Lists ft2l, boolean showSingleInstructionBlock)
	{
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_IF));
		
		createBlockForInstruction(
			preferences, layoutBlockList, classFile, method, ft2l.test);
		
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));
		
		List<Instruction> instructions = ft2l.instructions;
		
		if (instructions.size() == 1)
		{
			switch (instructions.get(0).opcode) 
			{
			case FastConstants.IF_:
			case FastConstants.IF_ELSE:
				createBlockForSubList(
					preferences, layoutBlockList, classFile, 
					method, instructions, false, 2);
				break;
			default:
				createBlockForSubList(
					preferences, layoutBlockList, classFile, method, 
					instructions, showSingleInstructionBlock, 2);
				break;
			}
		}
		else
		{
			createBlockForSubList(
				preferences, layoutBlockList, classFile, method, 
				instructions, showSingleInstructionBlock, 2);
		}
		
		List<Instruction> instructions2 = ft2l.instructions2;
		
		if (instructions2.size() == 1)
		{
			Instruction instruction = instructions2.get(0);
			
			// Write 'else if'
			switch (instruction.opcode)
			{
			case FastConstants.IF_:
				layoutBlockList.add(new FragmentLayoutBlock(
					LayoutBlockConstants.FRAGMENT_ELSE_SPACE));

				createBlockForFastTestList(
					preferences, LayoutBlockConstants.FRAGMENT_IF, layoutBlockList, 
					classFile, method, (FastTestList)instruction, 
					showSingleInstructionBlock);
				return;
			case FastConstants.IF_ELSE:
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

	// Show single instruction block ?
	private static boolean ShowSingleInstructionBlock(FastTest2Lists ifElse)
	{
		for (;;)
		{
			List<Instruction> instructions = ifElse.instructions;
			if ((instructions != null) && (instructions.size() >= 2))
				return false;
			
			int instructions2Size = ifElse.instructions2.size();
			
			if (instructions2Size == 0)
				return true;
			
			if (instructions2Size >= 2)
				return false;
			
			if (instructions2Size == 1)
			{
				Instruction instruction = ifElse.instructions2.get(0);
				
				if (instruction.opcode == FastConstants.IF_)
				{
					instructions = ((FastTestList)instruction).instructions;
					return (instructions == null) || (instructions.size() < 2);
				}
				else if (instruction.opcode == FastConstants.IF_ELSE)
				{
					ifElse = (FastTest2Lists)instruction;					
				}
				else
				{
					return true;
				}
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
			method, ftl.instructions, false, 1);
		
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_WHILE));

		createBlockForInstruction(
			preferences, layoutBlockList, classFile, method, ftl.test);
		
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
			method, fl.instructions, false, 1);
	}
	
	private void createBlocksForForLoop(
		Preferences preferences, List<LayoutBlock> layoutBlockList, 
		ClassFile classFile, Method method, FastFor ff)
	{
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_FOR));

		if (ff.init != null)
		{
			BlockLayoutBlock sblb = new BlockLayoutBlock(
					LayoutBlockConstants.FOR_BLOCK_START, 0);
				layoutBlockList.add(sblb);
				
			createBlockForInstruction(
				preferences, layoutBlockList, classFile, method, ff.init);

			BlockLayoutBlock eblb = new BlockLayoutBlock(
				LayoutBlockConstants.FOR_BLOCK_END, 0);
			sblb.other = eblb;
			eblb.other = sblb;
			layoutBlockList.add(eblb);
		}
		
		if (ff.test == null)
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
				preferences, layoutBlockList, classFile, method, ff.test);
			
			BlockLayoutBlock eblb = new BlockLayoutBlock(
				LayoutBlockConstants.FOR_BLOCK_END, 0);
			sblb.other = eblb;
			eblb.other = sblb;
			layoutBlockList.add(eblb);
		}
		
			
		if (ff.inc == null)
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
				preferences, layoutBlockList, classFile, method, ff.inc);
			
			BlockLayoutBlock eblb = new BlockLayoutBlock(
				LayoutBlockConstants.FOR_BLOCK_END, 0);
			sblb.other = eblb;
			eblb.other = sblb;
			layoutBlockList.add(eblb);
		}
		
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));
			
		createBlockForSubList(
			preferences, layoutBlockList, classFile, 
			method, ff.instructions, true, 1);
	}
	
	private void createBlockForFastForEach(
		Preferences preferences, List<LayoutBlock> layoutBlockList, 
		ClassFile classFile, Method method, FastForEach ffe)
	{
		layoutBlockList.add(new FragmentLayoutBlock(
				LayoutBlockConstants.FRAGMENT_FOR));
				
		createBlockForInstruction(
			preferences, layoutBlockList, classFile, method, ffe.variable);

		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_SPACE_COLON_SPACE));
				
		createBlockForInstruction(
			preferences, layoutBlockList, classFile, method, ffe.values);

		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));
		
		createBlockForSubList(
			preferences, layoutBlockList, classFile, 
			method, ffe.instructions, true, 1);
	}
	
	private void createBlocksForIfContinueOrBreak(
		Preferences preferences, List<LayoutBlock> layoutBlockList, 
		ClassFile classFile, Method method, FastInstruction fi)
	{
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_IF));
		
		createBlockForInstruction(
			preferences, layoutBlockList, classFile, method, fi.instruction);
		
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

		SingleStatementBlockStartLayoutBlock ssbslb = 
			new SingleStatementBlockStartLayoutBlock();
		layoutBlockList.add(ssbslb);
		
		if (fi.opcode == FastConstants.IF_CONTINUE)
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
		ssbslb.other = ssbelb;
		ssbelb.other = ssbslb;
		layoutBlockList.add(ssbelb);
	}
	
	private void createBlocksForIfLabeledBreak(
		Preferences preferences, List<LayoutBlock> layoutBlockList, 
		ClassFile classFile, Method method, FastInstruction fi)
	{
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_IF));
			
		createBlockForInstruction(
			preferences, layoutBlockList, classFile, method, fi.instruction);

		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));
		
		SingleStatementBlockStartLayoutBlock ssbslb = 
			new SingleStatementBlockStartLayoutBlock();
		layoutBlockList.add(ssbslb);
		
		BranchInstruction bi = (BranchInstruction)fi.instruction;
		
		layoutBlockList.add(new OffsetLayoutBlock(
			LayoutBlockConstants.FRAGMENT_LABELED_BREAK, 
			Instruction.UNKNOWN_LINE_NUMBER, 
			Instruction.UNKNOWN_LINE_NUMBER, 
			0, 0, 0, 
			bi.GetJumpOffset()));
		
		SingleStatementBlockEndLayoutBlock ssbelb =
			new SingleStatementBlockEndLayoutBlock(1);
		ssbslb.other = ssbelb;
		ssbelb.other = ssbslb;
		layoutBlockList.add(ssbelb);
	}
		
	private static void CreateBlocksForGotoLabeledBreak(
		List<LayoutBlock> layoutBlockList, ClassFile classFile, 
		Method method, FastInstruction fi)
	{
		BranchInstruction bi = (BranchInstruction)fi.instruction;

		layoutBlockList.add(new OffsetLayoutBlock(
			LayoutBlockConstants.FRAGMENT_LABELED_BREAK, 
			Instruction.UNKNOWN_LINE_NUMBER, 
			Instruction.UNKNOWN_LINE_NUMBER, 
			0, 0, 0, 
			bi.GetJumpOffset()));
	}
	
	private void createBlocksForSwitch(
		Preferences preferences, 
		List<LayoutBlock> layoutBlockList, ClassFile classFile, 
		Method method, FastSwitch fs, byte tagCase)
	{
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_SWITCH));
		
		createBlockForInstruction(
			preferences, layoutBlockList, classFile, method, fs.test);

		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

		BlockLayoutBlock sbslb = new SwitchBlockStartLayoutBlock();		
		layoutBlockList.add(sbslb);
		
		Pair[] pairs = fs.pairs;
		int length = pairs.length;
		int firstIndex = 0;
			
		for (int i=0; i<length; i++)
		{
			boolean last = (i == length-1);
			Pair pair = pairs[i];
			List<Instruction> instructions = pair.getInstructions();

			// Do'nt write default case on last position with empty 
			// instructions list.			
			if (pair.isDefault() && last)
			{
				if (instructions == null)
					break;
				if (instructions.size() == 0)
					break;
				if ((instructions.size() == 1) && 
					(instructions.get(0).opcode == FastConstants.GOTO_BREAK))
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
		sbslb.other = sbelb;
		sbelb.other = sbslb;
		layoutBlockList.add(sbelb);
	}
	
	private void createBlocksForSwitchEnum(
		Preferences preferences, 
		List<LayoutBlock> layoutBlockList, ClassFile classFile, 
		Method method, FastSwitch fs)
	{
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_SWITCH));

		Instruction test = fs.test;
		
		ConstantPool constants = classFile.getConstantPool();
		int switchMapKeyIndex = -1;
		
		if (test.opcode == ByteCodeConstants.ARRAYLOAD)
		{
			ArrayLoadInstruction ali = (ArrayLoadInstruction)test;
			ConstantNameAndType cnat;
			
			if (ali.arrayref.opcode == ByteCodeConstants.INVOKESTATIC)
			{
				// Dans le cas des instructions Switch+Enum d'Eclipse, la cl� de la map 
				// est l'indexe du nom de la m�thode 
				// "static int[] $SWITCH_TABLE$basic$data$TestEnum$enum1()".
				Invokestatic is = (Invokestatic)ali.arrayref;
				ConstantMethodref cmr = constants.getConstantMethodref(is.index);
				cnat = constants.getConstantNameAndType(cmr.name_and_type_index);
			}
			else if (ali.arrayref.opcode == ByteCodeConstants.GETSTATIC)
			{
				// Dans le cas des instructions Switch+Enum des autres compilateurs, 
				// la cl� de la map est l'indexe du nom de la classe interne 
				// "static class 1" contenant le tableau de correspondance 
				// "$SwitchMap$basic$data$TestEnum$enum1".		
				GetStatic gs = (GetStatic)ali.arrayref;
				ConstantFieldref cfr = constants.getConstantFieldref(gs.index);	
				cnat = constants.getConstantNameAndType(cfr.name_and_type_index);
			}
			else
			{
				throw new RuntimeException();
			}
			
			switchMapKeyIndex = cnat.name_index;

			Invokevirtual iv = (Invokevirtual)ali.indexref;
			
			createBlockForInstruction(
				preferences, layoutBlockList, classFile, method, iv.objectref);
		}
		
		if (switchMapKeyIndex == -1)
			throw new RuntimeException();

		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

		BlockLayoutBlock sbslb = new SwitchBlockStartLayoutBlock();		
		layoutBlockList.add(sbslb);
		
		Pair[] pairs = fs.pairs;
		int length = pairs.length;
		int firstIndex = 0;
			
		for (int i=0; i<length; i++)
		{
			boolean last = (i == length-1);
			Pair pair = pairs[i];
			List<Instruction> instructions = pair.getInstructions();

			// Do'nt write default case on last position with empty 
			// instructions list.			
			if (pair.isDefault() && last)
			{
				if (instructions == null)
					break;
				if (instructions.size() == 0)
					break;
				if ((instructions.size() == 1) && 
					(instructions.get(0).opcode == FastConstants.GOTO_BREAK))
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
		sbslb.other = sbelb;
		sbelb.other = sbslb;
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
			method, ft.instructions, false, 2);
		
		if (ft.catches != null)
		{
			int length = ft.catches.size();
			
			if (length > 0)
			{
				length--;
				
				// First catch blocks
				for (int i=0; i<length; ++i)
				{
					FastCatch fc = ft.catches.get(i);
					
					layoutBlockList.add(
						new FastCatchLayoutBlock(classFile, method, fc));
					
					createBlockForSubList(
						preferences, layoutBlockList, classFile, 
						method, fc.instructions, false, 2);
				}

				// Last catch block
				FastCatch fc = ft.catches.get(length);
				
				layoutBlockList.add(
					new FastCatchLayoutBlock(classFile, method, fc));
				
				int blockEndPreferedLineCount = 
						(ft.finallyInstructions == null) ? 1 : 2;
				
				createBlockForSubList(
					preferences, layoutBlockList, classFile, method, 
					fc.instructions, false, blockEndPreferedLineCount);
			}
		}
		
		if (ft.finallyInstructions != null)
		{
			layoutBlockList.add(new FragmentLayoutBlock(
				LayoutBlockConstants.FRAGMENT_FINALLY));
			
			createBlockForSubList(
				preferences, layoutBlockList, classFile, 
				method, ft.finallyInstructions, false, 1);
		}
	}
	
	private void createBlocksForSynchronized(
		Preferences preferences, List<LayoutBlock> layoutBlockList, 
		ClassFile classFile, Method method, FastSynchronized fs)
	{
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_SYNCHRONIZED));

		createBlockForInstruction(
			preferences, layoutBlockList, classFile, method, fs.monitor);
		
		layoutBlockList.add(new FragmentLayoutBlock(
			LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET));

		createBlockForSubList(
			preferences, layoutBlockList, classFile, 
			method, fs.instructions, false, 1);
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
			fl.offset));
		
		Instruction instruction = fl.instruction;
		
		if (instruction != null)
		{
			layoutBlockList.add(new SeparatorLayoutBlock(
				LayoutBlockConstants.SEPARATOR_OF_STATEMENTS, 1));

			switch (instruction.opcode)
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
			case FastConstants.IF_:
				createBlockForFastTestList(
					preferences, LayoutBlockConstants.FRAGMENT_IF, 
					layoutBlockList, classFile, method, 
					(FastTestList)instruction, true);
				break;
			case FastConstants.IF_ELSE:
				FastTest2Lists ft2l = (FastTest2Lists)instruction;
				createBlocksForIfElse(
					preferences, layoutBlockList, classFile, method, 
					ft2l, ShowSingleInstructionBlock(ft2l));
				break;
			case FastConstants.IF_CONTINUE:
			case FastConstants.IF_BREAK:
				createBlocksForIfContinueOrBreak(
					preferences, layoutBlockList, classFile, method, 
					(FastInstruction)instruction);
				break;
			case FastConstants.IF_LABELED_BREAK:
				createBlocksForIfLabeledBreak(
					preferences, layoutBlockList, classFile, 
					method, (FastInstruction)instruction);
				break;
//					case FastConstants.GOTO_CONTINUE:
//						CreateBlocksForGotoContinue(layoutBlockList);
//					case FastConstants.GOTO_BREAK:
//						CreateBlocksForGotoBreak(layoutBlockList);
//						break;
			case FastConstants.GOTO_LABELED_BREAK:
				CreateBlocksForGotoLabeledBreak(
					layoutBlockList, classFile, method, 
					(FastInstruction)instruction);
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
				if (((FastDeclaration)instruction).instruction == null)
				{
					layoutBlockList.add(new DeclareLayoutBlock(
						classFile, method, instruction));
					break;
				}				
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
	 * 				2 pour les premiers blocks,
	 * 				1 pour le dernier bloc
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
		if ((instructions == null) || (instructions.size() == 0))
		{
			StatementsBlockStartLayoutBlock sbslb = 
				new StatementsBlockStartLayoutBlock();
			sbslb.transformToStartEndBlock(0);
			layoutBlockList.add(sbslb);	
		}
		else
		{
			if (instructions.size() > 1)
				showSingleInstructionBlock = false;
			
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
			sbslb.other = sbelb;
			sbelb.other = sbslb;
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
		int index2 = SkipInstructions(list, index1);
				
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
	
	private static int SkipInstructions(List<Instruction> list, int index)
	{
		int length = list.size();
		
		while (++index < length)
		{
			Instruction instruction = list.get(index);
			
			switch (instruction.opcode)
			{
			case FastConstants.WHILE:
			case FastConstants.DO_WHILE:
			case FastConstants.INFINITE_LOOP:
			case FastConstants.FOR:
			case FastConstants.FOREACH:
			case FastConstants.IF_:
			case FastConstants.IF_ELSE:
			case FastConstants.IF_CONTINUE:
			case FastConstants.IF_BREAK:
			case FastConstants.IF_LABELED_BREAK:
			case FastConstants.GOTO_LABELED_BREAK:
			case FastConstants.SWITCH:
			case FastConstants.SWITCH_ENUM:
			case FastConstants.SWITCH_STRING:			
			case FastConstants.TRY:
			case FastConstants.SYNCHRONIZED:
			case FastConstants.LABEL:
				return index-1;
			case FastConstants.DECLARE:
				if (((FastDeclaration)instruction).instruction == null)
					return index-1;
			}
		}
		
		return length-1;
	}
}
