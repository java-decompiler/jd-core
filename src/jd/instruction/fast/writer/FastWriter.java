package jd.instruction.fast.writer;

import java.util.HashSet;
import java.util.List;

import jd.Preferences;
import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.LocalVariable;
import jd.classfile.Method;
import jd.classfile.constant.ConstantFieldref;
import jd.classfile.constant.ConstantMethodref;
import jd.classfile.constant.ConstantNameAndType;
import jd.classfile.constant.ConstantValue;
import jd.classfile.writer.ConstantValueWriter;
import jd.classfile.writer.SignatureWriter;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.instruction.bytecode.instruction.BranchInstruction;
import jd.instruction.bytecode.instruction.GetStatic;
import jd.instruction.bytecode.instruction.Goto;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.Invokestatic;
import jd.instruction.bytecode.instruction.Invokevirtual;
import jd.instruction.fast.FastConstants;
import jd.instruction.fast.instruction.FastFor;
import jd.instruction.fast.instruction.FastForEach;
import jd.instruction.fast.instruction.FastInstruction;
import jd.instruction.fast.instruction.FastLabel;
import jd.instruction.fast.instruction.FastList;
import jd.instruction.fast.instruction.FastSwitch;
import jd.instruction.fast.instruction.FastSynchronized;
import jd.instruction.fast.instruction.FastTest2Lists;
import jd.instruction.fast.instruction.FastTestList;
import jd.instruction.fast.instruction.FastTry;
import jd.instruction.fast.instruction.FastSwitch.Pair;
import jd.instruction.fast.instruction.FastTry.FastCatch;
import jd.instruction.fast.visitor.FastSourceWriterVisitor;
import jd.printer.Printer;
import jd.util.ReferenceMap;
import jd.util.StringUtil;



public class FastWriter
{
	public static void Write(
			HashSet<String> keywordSet, Preferences preferences, Printer spw, 
			ReferenceMap referenceMap, ClassFile classFile, Method method)
	{
		//spw.print(Printer.UNKNOWN_LINE_NUMBER, "// Fast:");
		//spw.endOfLineDebug();
		
		FastSourceWriterVisitor swv = 
			new FastSourceWriterVisitor(
				keywordSet, preferences, spw, referenceMap, 
				classFile, method.access_flags, method.getLocalVariables());
		
		WriteList(
			spw, swv, referenceMap, classFile, method, method.getFastNodes());
	}
	  
	private static void WriteList(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, List<Instruction> list)
	{		
		if (list != null)
		{
			int length = list.size();
	
			for (int i=0; i<length; i++)
			{
				WriteInstruction(
					spw, swv, referenceMap, classFile, 
					method, (Instruction)list.get(i));

				spw.endOfStatement();
			}
		}
	}
	  
	private static void WriteInstruction(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, Instruction instruction)
	{		
		switch (instruction.opcode)
		{
		case FastConstants.COMPLEXIF:
			WriteFastIf(spw, swv, (BranchInstruction)instruction);
			break;
		case FastConstants.WHILE:
			WriteWhileLoop(
					spw, swv, referenceMap, classFile, 
					method, (FastTestList)instruction);
			break;
		case FastConstants.DO_WHILE:
			WriteDoWhileLoop(
					spw, swv, referenceMap, classFile, 
					method, (FastTestList)instruction);
			break;
		case FastConstants.INFINITE_LOOP:
			WriteInfiniteLoop(
					spw, swv, referenceMap, classFile, 
					method, (FastList)instruction);
			break;
		case FastConstants.FOR:
			WriteForLoop(
					spw, swv, referenceMap, classFile, 
					method, (FastFor)instruction);
			break;
		case FastConstants.FOREACH:
			WriteForEachLoop(
					spw, swv, referenceMap, classFile, 
					method, (FastForEach)instruction);
			break;
		case FastConstants.IF_:
			WriteIf(
					spw, swv, referenceMap, classFile, 
					method, (FastTestList)instruction);
			break;
		case FastConstants.IF_ELSE:
			WriteIfElse(
					spw, swv, referenceMap, classFile, 
					method, (FastTest2Lists)instruction);
			break;
		case FastConstants.IF_CONTINUE:
		case FastConstants.IF_BREAK:
			WriteIfContinueOrBreak(spw, swv, (FastInstruction)instruction);
			break;
		case FastConstants.IF_LABELED_BREAK:
			WriteIfLabeledBreak(spw, swv, (FastInstruction)instruction);
			break;
		case FastConstants.GOTO_CONTINUE:
		case FastConstants.GOTO_BREAK:
			WriteGotoContinueOrBreak(spw, (FastInstruction)instruction);
			break;
		case FastConstants.GOTO_LABELED_BREAK:
			WriteGotoLabeledBreak(spw, (FastInstruction)instruction);
			break;
		// Normalement, ces instructions ont été traitées et remplacées par des 
		// instructions FastIf. Si elles apparaissent, c'est qu'il y a un pb.
		case FastConstants.IF:
		case FastConstants.IFCMP:
		case FastConstants.IFXNULL:
			WriteIfInstruction(spw, swv, (BranchInstruction)instruction);
			break;
		case FastConstants.SWITCH:
			WriteSwitch(
					spw, swv, referenceMap, classFile, 
					method, (FastSwitch)instruction);
				break;
		case FastConstants.SWITCH_STRING:
			WriteSwitchString(
				spw, swv, referenceMap, classFile, 
				method, (FastSwitch)instruction);
			break;
		case FastConstants.SWITCH_ENUM:
			WriteSwitchEnum(
					spw, swv, referenceMap, classFile, 
					method, (FastSwitch)instruction);
				break;
		case FastConstants.TRY:
			WriteTry(
				spw, swv, referenceMap, classFile, 
				method, (FastTry)instruction);
			break;
		case FastConstants.SYNCHRONIZED:
			WriteSynchronized(
				spw, swv, referenceMap, classFile, 
				method, (FastSynchronized)instruction);
			break;
		case FastConstants.LABEL:
			WriteLabel(
				spw, swv, referenceMap, classFile, 
				method, (FastLabel)instruction);
			break;
		default:
			int lineNumber = instruction.lineNumber;
				
			spw.printOffset(lineNumber, instruction.offset);
			swv.visit(instruction);
			spw.print(lineNumber, ';');
		}
	}
	
	private static void WriteWhileLoop(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastTestList list)
	{
		int lineNumber = list.test.lineNumber;
		
		spw.printOffset(list.lineNumber, list.offset);
		spw.print(lineNumber, "while (");
		swv.visit(list.test);
		spw.print(lineNumber, ")");	
		WriteSubList(
			spw, swv, referenceMap, classFile, method, list.instructions, true);
	}		

	private static void WriteDoWhileLoop(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastTestList list)
	{
		int lineNumber = list.lineNumber;
		
		spw.printKeyword(Printer.UNKNOWN_LINE_NUMBER, "do");
		WriteSubList(
			spw, swv, referenceMap, classFile, method, list.instructions, true);

		lineNumber = list.test.lineNumber;
		
		spw.print(lineNumber, "while (");
		swv.visit(list.test);
		spw.print(lineNumber, ");");
	}		

	private static void WriteInfiniteLoop(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastList list)
	{
		spw.printOffset(list.lineNumber, list.offset);
		spw.print(Printer.UNKNOWN_LINE_NUMBER, "while (true)");	
		WriteSubList(
			spw, swv, referenceMap, classFile, method, list.instructions, true);
	}	

	private static void WriteForLoop(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastFor list)
	{
		int lineNumber = list.lineNumber;
		
		spw.printOffset(list.lineNumber, list.offset);
		spw.print(lineNumber, "for (");	
		if (list.init != null)
		{
			swv.visit(list.init);
			lineNumber = list.init.lineNumber;
		}
		spw.print(lineNumber, "; ");	
		if (list.test != null)
		{
			swv.visit(list.test);
			lineNumber = list.test.lineNumber;
		}
		spw.print(lineNumber, "; ");	
		if (list.inc != null)
		{
			swv.visit(list.inc);
			lineNumber = list.inc.lineNumber;
		}
		spw.print(lineNumber, ")");	
		WriteSubList(
			spw, swv, referenceMap, classFile, method, list.instructions, true);		
	}
	
	private static void WriteForEachLoop(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastForEach list)
	{
		int lineNumber = list.lineNumber;
		
		spw.printOffset(list.lineNumber, list.offset);
		spw.print(lineNumber, "for (");	
		swv.visit(list.variable);
		lineNumber = list.variable.lineNumber;
		spw.print(lineNumber, " : ");	
		swv.visit(list.values);
		lineNumber = list.values.lineNumber;
		spw.print(lineNumber, ")");	
		WriteSubList(
			spw, swv, referenceMap, classFile, method, list.instructions, true);		
	}
	
	private static void WriteIf(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastTestList list)
	{
		spw.printOffset(list.lineNumber, list.offset);
		WriteIfWithoutOffset(
			spw, swv, referenceMap, classFile, method, list, true);
	}	

	private static void WriteIfWithoutOffset(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastTestList list,
			boolean showSingleInstructionBlock)
	{
		int lineNumber = list.test.lineNumber;
		
		spw.print(lineNumber, "if (");	
		swv.visit(list.test);
		spw.print(lineNumber, ")");
		WriteSubList(
			spw, swv, referenceMap, classFile, method, list.instructions, 
			showSingleInstructionBlock);
	}	

	// Show single instruction block ?
	private static boolean ShowSingleInstructionBlock(FastTest2Lists ifElse)
	{
		for (;;)
		{
			if (ifElse.instructions.size() >= 2)
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
					return ((FastTestList)instruction).instructions.size() < 2;
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
	
	private static void WriteIfElse(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastTest2Lists ifElse)
	{
		spw.printOffset(ifElse.lineNumber, ifElse.offset);
		WriteIfElseWithoutOffset(
			spw, swv, referenceMap, classFile, method, ifElse,
			ShowSingleInstructionBlock(ifElse));
	}	

	private static void WriteIfElseWithoutOffset(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastTest2Lists ifElse,
			boolean showSingleInstructionBlock)
	{
		int lineNumber = ifElse.test.lineNumber;
		
		spw.print(lineNumber, "if (");	
		swv.visit(ifElse.test);
		spw.print(lineNumber, ")");
		
		List<Instruction> instructions = ifElse.instructions;
		if (instructions.size() == 1)
		{
			switch (instructions.get(0).opcode) 
			{
			case FastConstants.IF_:
			case FastConstants.IF_ELSE:
				spw.startStatementBlock();
				WriteList(
					spw, swv, referenceMap, classFile, method, instructions);
				spw.endStatementBlock();
				break;
			default:
				WriteSubList(
					spw, swv, referenceMap, classFile, 
					method, instructions, showSingleInstructionBlock);
				break;
			}
		}
		else
		{
			WriteSubList(
				spw, swv, referenceMap, classFile, 
				method, instructions, showSingleInstructionBlock);
		}
		
		List<Instruction> instructions2 = ifElse.instructions2;
		if (instructions2.size() == 1)
		{
			Instruction instruction = instructions2.get(0);
			
			// Write 'else if'
			switch (instruction.opcode)
			{
			case FastConstants.IF_:
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "else ");
				WriteIfWithoutOffset(
						spw, swv, referenceMap, classFile, 
						method, (FastTestList)instruction,
						showSingleInstructionBlock);
				return;
			case FastConstants.IF_ELSE:
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "else ");
				WriteIfElseWithoutOffset(
						spw, swv, referenceMap, classFile, 
						method, (FastTest2Lists)instruction,
						showSingleInstructionBlock);
				return;		
			}
		}

		spw.print(Printer.UNKNOWN_LINE_NUMBER, "else");
		WriteSubList(
			spw, swv, referenceMap, classFile, method, instructions2, 
			showSingleInstructionBlock);
	}	

	private static void WriteIfContinueOrBreak(
			Printer spw, FastSourceWriterVisitor swv, FastInstruction ifContinue)
	{
		int lineNumber = ifContinue.instruction.lineNumber;
		
		spw.printOffset(lineNumber, ifContinue.offset);
		spw.print(lineNumber, "if (");	
		swv.visit(ifContinue.instruction);
		spw.print(lineNumber, ")");
		
		spw.startSingleStatementBlock();
		if (ifContinue.opcode == FastConstants.IF_CONTINUE)
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "continue;");
		else
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "break;");
		spw.startSingleStatementBlock();
	}

	private static void WriteIfLabeledBreak(
		Printer spw, FastSourceWriterVisitor swv, FastInstruction ifContinue)
	{
		BranchInstruction bi = (BranchInstruction)ifContinue.instruction;
		
		int lineNumber = bi.lineNumber;
		
		spw.printOffset(lineNumber, ifContinue.offset);
		spw.print(lineNumber, "if (");	
		swv.visit(bi);
		spw.print(lineNumber, ")");
		
		spw.print(Printer.UNKNOWN_LINE_NUMBER, "break ");
		spw.print(Printer.UNKNOWN_LINE_NUMBER, FastConstants.LABEL_PREFIX);
		spw.print(Printer.UNKNOWN_LINE_NUMBER, bi.offset + bi.branch);
		spw.print(Printer.UNKNOWN_LINE_NUMBER, ';');
	}

	private static void WriteGotoContinueOrBreak(
		Printer spw, FastInstruction ifContinue)
	{
		spw.printOffset(ifContinue.lineNumber, ifContinue.offset);
		
		if (ifContinue.opcode == FastConstants.GOTO_CONTINUE) 
		{
			spw.print(ifContinue.lineNumber, "continue;");
		}
		else
		{
			spw.print(ifContinue.lineNumber, "break;");
		}
	}

	private static void WriteGotoLabeledBreak(
		Printer spw, FastInstruction ifContinue)
	{
		int lineNumber = ifContinue.lineNumber;
		
		spw.print(lineNumber, "break ");
		Goto g = (Goto)ifContinue.instruction;
		
		if (g != null)
		{	
			spw.print(lineNumber, FastConstants.LABEL_PREFIX);
			spw.print(lineNumber, g.GetJumpOffset());
		}
		
		spw.print(lineNumber, ';');
	}
	
	private static void WriteSubList(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, List<Instruction> subList,
			boolean showSingleInstructionBlock)
	{
		if (subList == null)
		{
			spw.print(Printer.UNKNOWN_LINE_NUMBER, ';');
		}
		else
		{
			switch (subList.size())
			{
			case 0:
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ';');
				break;
			case 1:
				if (showSingleInstructionBlock)
					spw.startSingleStatementBlock();
				else
					spw.startStatementBlock();
				WriteList(spw, swv, referenceMap, classFile, method, subList);
				if (showSingleInstructionBlock)
					spw.endSingleStatementBlock();
				else
					spw.endStatementBlock();
				break;
			default:
				spw.startStatementBlock();
				WriteList(spw, swv, referenceMap, classFile, method, subList);
				spw.endStatementBlock();
			}
		}
	}
	
	private static void WriteFastIf(
		Printer spw, FastSourceWriterVisitor swv, BranchInstruction bi)
	{
		int lineNumber = bi.lineNumber;
		
		spw.printOffset(lineNumber, bi.offset);
		spw.print(lineNumber, "if (");	
		swv.visit(bi);
		spw.print(lineNumber, ") goto ");
		spw.print(lineNumber, bi.GetJumpOffset());
		spw.print(lineNumber, ';');
	}	

	private static void WriteIfInstruction(
		Printer spw, FastSourceWriterVisitor swv, BranchInstruction bi)
	{
		int lineNumber = bi.lineNumber;
		
		spw.printOffset(lineNumber, bi.offset);
		spw.print(lineNumber, "if (");	
		swv.visit(bi);
		spw.print(lineNumber, ") goto ");
		spw.print(lineNumber, bi.GetJumpOffset());
		spw.print(lineNumber, ';');
	}	
	
	private static void WriteSwitch(
		Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
		ClassFile classFile, Method method, FastSwitch fs)
	{
		Instruction test = fs.test;
			
		int lineNumber = test.lineNumber;
		
		spw.printOffset(lineNumber, fs.offset);
		spw.print(lineNumber, "switch (");	
		swv.visit(test);
		spw.print(lineNumber, ')');
		spw.startStatementBlock();
		
		String signature = test.getReturnedSignature(
				classFile.getConstantPool(), method.getLocalVariables());
		char type = (signature == null) ? 'X' : signature.charAt(0);		
		
		Pair[] pairs = fs.pairs;
		int length = pairs.length;
			
		for (int i=0; i<length; i++)
		{
			Pair pair = pairs[i];
			
			spw.startCaseStatement();
			if (pair.isDefault())
			{
				// Dont write default case on last position with empty 
				// instructions list.
				if ((i == length-1) && (pair.getInstructions() == null))
					break;
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "default:");
			}
			else
			{
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "case ");		
				
				if (type == 'C')
				{
					String escapedString =
						StringUtil.EscapeCharAndAppendApostrophe((char)pair.getKey());
					spw.print(Printer.UNKNOWN_LINE_NUMBER, escapedString);
				}
				else
				{
					spw.print(Printer.UNKNOWN_LINE_NUMBER, pair.getKey());
				}
				
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ':');		
			}
			spw.endCaseStatement();

			List<Instruction> instructions = pair.getInstructions();
			if (instructions != null)
				WriteList(
					spw, swv, referenceMap, classFile, method, instructions);
		}

		spw.endStatementBlock();
	}
	
	private static void WriteSwitchString(
		Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
		ClassFile classFile, Method method, FastSwitch fs)
	{
		Instruction test = fs.test;
			
		int lineNumber = test.lineNumber;
		
		spw.printOffset(lineNumber, fs.offset);
		spw.print(lineNumber, "switch (");	
		swv.visit(test);
		spw.print(lineNumber, ')');
		spw.startStatementBlock();
		
		ConstantPool constants = classFile.getConstantPool();
		Pair[] pairs = fs.pairs;
		int length = pairs.length;
			
		for (int i=0; i<length; i++)
		{
			Pair pair = pairs[i];
			
			spw.startCaseStatement();
			if (pair.isDefault())
			{
				// Dont write default case on last position with empty 
				// instructions list.
				if ((i == length-1) && (pair.getInstructions() == null))
					break;
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "default:");
			}
			else
			{
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "case ");		

				ConstantValue cv = constants.getConstantValue(pair.getKey());
				ConstantValueWriter.Write(
					spw, Printer.UNKNOWN_LINE_NUMBER, constants, cv);
				
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ':');		
			}
			spw.endCaseStatement();

			List<Instruction> instructions = pair.getInstructions();
			if (instructions != null)
				WriteList(
					spw, swv, referenceMap, classFile, method, instructions);
		}

		spw.endStatementBlock();
	}
		
	private static void WriteSwitchEnum(
		Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
		ClassFile classFile, Method method, FastSwitch fs)
	{
		ConstantPool constants = classFile.getConstantPool();
		Instruction test = fs.test;
		int switchMapKeyIndex = -1;
			
		int lineNumber = test.lineNumber;
		
		spw.printOffset(lineNumber, fs.offset);
		spw.print(lineNumber, "switch (");	
		
		if (test.opcode == ByteCodeConstants.ARRAYLOAD)
		{
			ArrayLoadInstruction ali = (ArrayLoadInstruction)test;
			ConstantNameAndType cnat;
			
			if (ali.arrayref.opcode == ByteCodeConstants.INVOKESTATIC)
			{
				// Dans le cas des instructions Switch+Enum d'Eclipse, la clé de la map 
				// est l'indexe du nom de la méthode 
				// "static int[] $SWITCH_TABLE$basic$data$TestEnum$enum1()".
				Invokestatic is = (Invokestatic)ali.arrayref;
				ConstantMethodref cmr = constants.getConstantMethodref(is.index);
				cnat = constants.getConstantNameAndType(cmr.name_and_type_index);
			}
			else if (ali.arrayref.opcode == ByteCodeConstants.GETSTATIC)
			{
				// Dans le cas des instructions Switch+Enum des autres compilateurs, 
				// la clé de la map est l'indexe du nom de la classe interne 
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
			swv.visit(iv.objectref);			
		}
		
		spw.print(lineNumber, ')');
		
		List<Integer> switchMap = 
			classFile.getSwitchMaps().get(switchMapKeyIndex);
		
		if (switchMap != null)
		{
			spw.startStatementBlock();	
			
			Pair[] pairs = fs.pairs;
			int length = pairs.length;
				
			for (int i=0; i<length; i++)
			{
				Pair pair = pairs[i];
				
				spw.startCaseStatement();
				if (pair.isDefault())
				{
					// Dont write default case on last position with empty 
					// instructions list.
					if ((i == length-1) && (pair.getInstructions() == null))
						break;
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "default:");
				}
				else
				{
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "case ");		
					int key = pair.getKey();
					
					if ((0 < key) && (key <= switchMap.size()))
					{
						String value = 
							constants.getConstantUtf8(switchMap.get(key-1));
						spw.print(Printer.UNKNOWN_LINE_NUMBER, value);
					}
					else
					{
						spw.print(Printer.UNKNOWN_LINE_NUMBER, "???");
					}
					
					spw.print(Printer.UNKNOWN_LINE_NUMBER, ':');		
				}
				spw.endCaseStatement();
	
				List<Instruction> instructions = pair.getInstructions();
				if (instructions != null)
					WriteList(
						spw, swv, referenceMap, classFile, method, instructions);
			}
	
			spw.endStatementBlock();
		}
	}
	
	private static void WriteTry(
			Printer spw, FastSourceWriterVisitor swv, 
			ReferenceMap referenceMap, ClassFile classFile, 
			Method method, FastTry ft)
	{
		spw.printOffset(Printer.UNKNOWN_LINE_NUMBER, ft.offset);
		spw.print(Printer.UNKNOWN_LINE_NUMBER, "try");
		spw.startStatementBlock();
		WriteList(spw, swv, referenceMap, classFile, method, ft.instructions);
		spw.endStatementBlock();

		if (ft.catches != null)
		{
			ConstantPool constants = classFile.getConstantPool();
			int length = ft.catches.size();
			
			for (int i=0; i<length; ++i)
			{
				FastCatch fc = ft.catches.get(i);
				LocalVariable lv = method.getLocalVariables()
					.searchLocalVariableWithIndexAndOffset(
						fc.localVarIndex, fc.offset);
				
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "catch (");
				spw.print(
					Printer.UNKNOWN_LINE_NUMBER, 
					SignatureWriter.WriteSimpleSignature(
						referenceMap, classFile, 
						constants.getConstantUtf8(fc.exceptionNameIndex)));
				spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");
				spw.print(
					Printer.UNKNOWN_LINE_NUMBER, 
					constants.getConstantUtf8(lv.name_index));
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ")");
				spw.startStatementBlock();
				WriteList(
					spw, swv, referenceMap, classFile, method, fc.instructions);
				spw.endStatementBlock();
			}
		}
		
		if (ft.finallyInstructions != null)
		{
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "finally");
			spw.startStatementBlock();
			WriteList(
				spw, swv, referenceMap, classFile, 
				method, ft.finallyInstructions);
			spw.endStatementBlock();
		}
	}
	
	private static void WriteSynchronized(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastSynchronized fs)
	{
		int lineNumber = fs.monitor.lineNumber;
		
		spw.printOffset(lineNumber, fs.offset);
		spw.print(lineNumber, "synchronized (");	
		swv.visit(fs.monitor);
		spw.print(lineNumber, ')');
		spw.startStatementBlock();
		WriteList(spw, swv, referenceMap, classFile, method, fs.instructions);
		spw.endStatementBlock();
	}
	
	private static void WriteLabel(
			Printer spw, FastSourceWriterVisitor swv, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, FastLabel fl)
	{
		int lineNumber = fl.lineNumber;
		
		spw.printOffset(lineNumber, fl.offset);
		spw.print(lineNumber, FastConstants.LABEL_PREFIX);
		spw.print(lineNumber, fl.offset);
		spw.print(lineNumber, ':');
		spw.endOfStatement();
		
		if (fl.instruction != null)
		{
			WriteInstruction(
				spw, swv, referenceMap, classFile, method, fl.instruction);
		}
	}
}