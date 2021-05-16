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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.CodeException;
import jd.core.model.classfile.attribute.LineNumber;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ExceptionLoad;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.ReturnAddressLoad;
import jd.core.process.analyzer.instruction.bytecode.factory.InstructionFactory;
import jd.core.process.analyzer.instruction.bytecode.factory.InstructionFactoryConstants;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;
import jd.core.util.IntSet;
import jd.core.util.SignatureUtil;


public class InstructionListBuilder 
{
	private static CodeExceptionComparator COMPARATOR = 
		new CodeExceptionComparator();
	
	public static void Build(
			ClassFile classFile, Method method, 
			List<Instruction> list, 
			List<Instruction> listForAnalyze)
		throws Exception
	{
		byte[] code = method.getCode();
		
		if (code != null)
		{
			int offset = 0;
			
			try
			{
				final int length = code.length;
				
				// Declaration du tableau de sauts utile pour reconstruire les 
				// instructions de pre et post incrementation : si une 
				// instruction 'iinc' est une instruction vers laquelle on 
				// saute, elle ne sera pas agreg�e a l'instruction precedante 
				// ou suivante.			
				boolean[] jumps = new boolean[length];
				
				// Declaration du tableau des sauts vers les sous procedures 
				// (jsr ... ret). A chaque d�but de sous procedures, une pseudo 
				// adresse de retour doit etre inseree sur la pile.
				IntSet offsetSet = new IntSet();
				
				// Population des deux tableaux dans la meme passe.
				PopulateJumpsArrayAndSubProcOffsets(code, length, jumps, offsetSet);	
							
				// Initialisation de variables additionnelles pour le traitement 
				// des sous procedures.
				int[] subProcOffsets = offsetSet.toArray();
				int subProcOffsetsIndex = 0;
				int subProcOffset = 
					(subProcOffsets == null) ? -1 : subProcOffsets[0];
				
				// Declaration de variables additionnelles pour le traitement 
				// des blocs 'catch' et 'finally'.
				final Stack<Instruction> stack = new Stack<Instruction>();		
				final CodeException[] codeExceptions = method.getCodeExceptions();
				int codeExceptionsIndex = 0;
				int exceptionOffset;
				ConstantPool constants = classFile.getConstantPool();
							
				if (codeExceptions == null)
				{
					exceptionOffset = -1;
				}
				else
				{
					// Sort codeExceptions by handler_pc
					Arrays.sort(codeExceptions, COMPARATOR);
					exceptionOffset = codeExceptions[0].handler_pc;
				}
				
	            // Declaration de variables additionnelles pour le traitement 
				// des numeros de ligne
				LineNumber[] lineNumbers = method.getLineNumbers();
				int lineNumbersIndex = 0;				
				int lineNumber;
				int nextLineOffset;
				
				if (lineNumbers == null)
				{
					lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
					nextLineOffset = -1;
				}
				else
				{
					LineNumber ln = lineNumbers[lineNumbersIndex];
					lineNumber = ln.line_number;
					nextLineOffset = -1;
					
					int startPc = ln.start_pc;					
					while (++lineNumbersIndex < lineNumbers.length)
					{
						ln = lineNumbers[lineNumbersIndex];
						if (ln.start_pc != startPc)
						{
							nextLineOffset = ln.start_pc;
							break;
						}
						lineNumber = ln.line_number;
					}
				}
				
				// Boucle principale : agregation des instructions
				for (offset=0; offset<length; ++offset)
				{
					int opcode = code[offset] & 255;
					InstructionFactory factory = 
						InstructionFactoryConstants.FACTORIES[opcode];
					
					if (factory != null)
					{
						// Ajout de ExceptionLoad
						if (offset == exceptionOffset)
						{
							// Ajout d'une pseudo instruction de lecture 
							// d'exception en debut de bloc catch
							int catchType = 
								codeExceptions[codeExceptionsIndex].catch_type;
							int signatureIndex;
							
							if (catchType == 0)
							{
								signatureIndex = 0;
							}
							else
							{
								String catchClassName = SignatureUtil.CreateTypeName(
									constants.getConstantClassName(catchType)); 
								signatureIndex = constants.addConstantUtf8(
									catchClassName);
							}
	
							ExceptionLoad el = new ExceptionLoad(
									ByteCodeConstants.EXCEPTIONLOAD, 
									offset, lineNumber, signatureIndex);						
							stack.push(el);						
							listForAnalyze.add(el);
							
							// Search next exception offset
							int nextOffsetException;
							for (;;)
							{
								if (++codeExceptionsIndex >= codeExceptions.length)
								{
									nextOffsetException = -1;
									break;
								}
								
								nextOffsetException = 
									codeExceptions[codeExceptionsIndex].handler_pc;
								
								if (nextOffsetException != exceptionOffset)
									break;
							}						
							exceptionOffset = nextOffsetException;
						}
						
						// Ajout de ReturnAddressLoad
						if (offset == subProcOffset)
						{
							// Ajout d'une pseudo adresse de retour en debut de 
							// sous procedure. Lors de l'execution, cette 
							// adresse est normalement plac�e sur la pile par 
							// l'instruction JSR.
							stack.push(new ReturnAddressLoad(
									ByteCodeConstants.RETURNADDRESSLOAD, 
									offset, lineNumber));
									
							if (++subProcOffsetsIndex >= subProcOffsets.length)
								subProcOffset = -1;
							else
								subProcOffset = subProcOffsets[subProcOffsetsIndex];
						}
						
						// Traitement des numeros de ligne
						if (offset == nextLineOffset)
						{
							LineNumber ln = lineNumbers[lineNumbersIndex];
							lineNumber = ln.line_number;
							nextLineOffset = -1;
							
							int startPc = ln.start_pc;					
							while (++lineNumbersIndex < lineNumbers.length)
							{
								ln = lineNumbers[lineNumbersIndex];
								if (ln.start_pc != startPc)
								{
									nextLineOffset = ln.start_pc;
									break;
								}
								lineNumber = ln.line_number;
							}
						}
						
						// Generation d'instruction
						offset += factory.create(
							classFile, method, list, listForAnalyze, stack, 
							code, offset, lineNumber, jumps);
					}
					else
					{
						String msg = "No factory for " + 
								ByteCodeConstants.OPCODE_NAMES[opcode];
						System.err.println(msg);
						throw new Exception(msg);
					}
				}
			
				if (! stack.isEmpty())
				{
					final String className = classFile.getClassName();
					final String methodName =
						classFile.getConstantPool().getConstantUtf8(method.name_index);
					System.err.println(
						"'" + className + '.' + methodName + 
						"' build error: stack not empty. stack=" + stack);
				}
			}
			catch (Exception e)
			{
				// Bad byte code ... generate, for example, by Eclipse Java 
				// Compiler or Harmony:
			    // Byte code:
			    //   0: aload_0
			    //   1: invokevirtual 16	TryCatchFinallyClassForTest:before	()V
			    //   4: iconst_1
			    //   5: ireturn
			    //   6: astore_1       <----- Error: EmptyStackException
			    //   7: aload_0
			    //   8: invokevirtual 19	TryCatchFinallyClassForTest:inCatch1	()V
			    //   11: aload_0
			    //   12: invokevirtual 22	TryCatchFinallyClassForTest:after	()V
			    //   15: iconst_2
			    //   16: ireturn				
				throw new InstructionListException(classFile, method, offset, e);
			}
		}
	}
	
	private static void PopulateJumpsArrayAndSubProcOffsets(
			byte[] code, int length, boolean[] jumps, IntSet offsetSet)
	{
		for (int offset=0; offset<length; ++offset)
		{
			int jumpOffset;
			int opcode = code[offset] & 255;
			
			switch (ByteCodeConstants.NO_OF_OPERANDS[opcode])
			{
			case 0:
				break;
			case 2:
				switch (opcode)
				{
				case ByteCodeConstants.IFEQ: 
				case ByteCodeConstants.IFNE:
				case ByteCodeConstants.IFLT: 
				case ByteCodeConstants.IFGE:
				case ByteCodeConstants.IFGT: 
				case ByteCodeConstants.IFLE:

				case ByteCodeConstants.IF_ICMPEQ: 
				case ByteCodeConstants.IF_ICMPNE:
				case ByteCodeConstants.IF_ICMPLT: 
				case ByteCodeConstants.IF_ICMPGE:
				case ByteCodeConstants.IF_ICMPGT: 
				case ByteCodeConstants.IF_ICMPLE:	
					
				case ByteCodeConstants.IF_ACMPEQ:
				case ByteCodeConstants.IF_ACMPNE:
					
				case ByteCodeConstants.IFNONNULL:
				case ByteCodeConstants.IFNULL:
					
				case ByteCodeConstants.GOTO:
					jumpOffset = offset + 
										(short)( ((code[++offset] & 255) << 8) | 
							                      (code[++offset] & 255) );
					jumps[jumpOffset] = true;
					break;
				case ByteCodeConstants.JSR: 
					jumpOffset = offset + 
									(short)( ((code[++offset] & 255) << 8) | 
						                      (code[++offset] & 255) );
					offsetSet.add(jumpOffset);
					break;
				default:
					offset += 2;	
				}
				break;
				
			case 4:
				switch (opcode)
				{
				case ByteCodeConstants.GOTO_W:
					jumpOffset = offset + 
					                    ((code[++offset] & 255) << 24) | 
							            ((code[++offset] & 255) << 16) |
					                    ((code[++offset] & 255) << 8 ) |  
					                     (code[++offset] & 255);
					jumps[jumpOffset] = true;
					break;
				case ByteCodeConstants.JSR_W:
					jumpOffset = offset + 
				                     ((code[++offset] & 255) << 24) | 
						             ((code[++offset] & 255) << 16) |
				                     ((code[++offset] & 255) << 8 ) |  
				                      (code[++offset] & 255);
					offsetSet.add(jumpOffset);
					break;
				default:
					offset += 4;
				}
				break;				
			default:
				switch (opcode)
				{
				case ByteCodeConstants.TABLESWITCH:
					offset = ByteCodeUtil.NextTableSwitchOffset(code, offset);
					break;
				case ByteCodeConstants.LOOKUPSWITCH:
					offset = ByteCodeUtil.NextLookupSwitchOffset(code, offset);
					break;
				case ByteCodeConstants.WIDE:
					offset = ByteCodeUtil.NextWideOffset(code, offset);
					break;
				default:
					offset += ByteCodeConstants.NO_OF_OPERANDS[opcode];
				}
			}
		}
	}
	
	private static class CodeExceptionComparator 
		implements Comparator<CodeException>
	{
		public int compare(CodeException ce1, CodeException ce2)
		{
			if (ce1.handler_pc != ce2.handler_pc)
				return ce1.handler_pc - ce2.handler_pc;
			
			if (ce1.end_pc != ce2.end_pc)
				return ce1.end_pc - ce2.end_pc;
			
			if (ce1.start_pc != ce2.start_pc)
				return ce1.start_pc - ce2.start_pc;
			
			return ce1.index - ce2.index;
		}
	}
}
