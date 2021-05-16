package jd.core.process.analyzer.instruction.fast;

import java.util.List;

import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Return;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastList;
import jd.core.model.instruction.fast.instruction.FastSwitch;
import jd.core.model.instruction.fast.instruction.FastTest2Lists;
import jd.core.model.instruction.fast.instruction.FastTry;


/*
 * Le numero de ligne des instructions 'return' genere par les compilateurs
 * sont faux et perturbe l'affichage des sources
 */
public class ReturnLineNumberAnalyzer
{
	public static void Check(Method method)
	{	
		List<Instruction> list = method.getFastNodes();
		int length = list.size();
		
		if (length > 1)
		{
			int afterListLineNumber = list.get(length-1).lineNumber;
			
			if (afterListLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
			{
				RecursiveCheck(list , afterListLineNumber);
			}
		}
	}

	private static void RecursiveCheck(
		List<Instruction> list, int afterListLineNumber)
	{
		int index = list.size();
		
		// Appels recursifs
		while (index-- > 0)
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
			case FastConstants.SYNCHRONIZED:
				{
					List<Instruction> instructions = 
							((FastList)instruction).instructions;
					if (instructions != null)
						RecursiveCheck(instructions, afterListLineNumber);
				}
				break;					
			case FastConstants.IF_ELSE:
				{
					FastTest2Lists ft2l = (FastTest2Lists)instruction;
					RecursiveCheck(ft2l.instructions, afterListLineNumber);				
					RecursiveCheck(ft2l.instructions2, afterListLineNumber);
				}
				break;	
			case FastConstants.SWITCH:
			case FastConstants.SWITCH_ENUM:
			case FastConstants.SWITCH_STRING:
				{
					FastSwitch.Pair[] pairs = ((FastSwitch)instruction).pairs;
					if (pairs != null)
						for (int i=pairs.length-1; i>=0; --i)
						{
							List<Instruction> instructions = pairs[i].getInstructions();					
							if (instructions != null)
							{
								RecursiveCheck(instructions, afterListLineNumber);
								if (instructions.size() > 0)
								{
									afterListLineNumber = 
										instructions.get(0).lineNumber;
								}
							}
						}
				}
				break;				
			case FastConstants.TRY:
				{
					FastTry ft = (FastTry)instruction;
					
					if (ft.finallyInstructions != null)
					{
						RecursiveCheck(ft.finallyInstructions, afterListLineNumber);
						if (ft.finallyInstructions.size() > 0)
						{
							afterListLineNumber = 
								ft.finallyInstructions.get(0).lineNumber;
						}
					}
					
					if (ft.catches != null)
					{
						for (int i=ft.catches.size()-1; i>=0; --i)
						{
							List<Instruction> catchInstructions = 
								ft.catches.get(i).instructions;
							RecursiveCheck(
								catchInstructions, afterListLineNumber);
							if (catchInstructions.size() > 0)
							{
								afterListLineNumber = 
									catchInstructions.get(0).lineNumber;
							}
						}
					}
					
					RecursiveCheck(ft.instructions, afterListLineNumber);					
				}
				break;
			case FastConstants.RETURN:
				{
					Return r = (Return)instruction;
					if (r.lineNumber > afterListLineNumber)
						r.lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
				}
				break;
			}	
			
			afterListLineNumber = instruction.lineNumber;
		}
	}
}
