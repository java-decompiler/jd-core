package jd.instruction.bytecode.writer;

import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.LocalVariable;
import jd.classfile.LocalVariables;
import jd.classfile.Method;
import jd.classfile.attribute.CodeException;
import jd.classfile.attribute.LineNumber;
import jd.classfile.constant.ConstantFieldref;
import jd.classfile.constant.ConstantMethodref;
import jd.classfile.constant.ConstantNameAndType;
import jd.classfile.writer.SignatureWriter;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.util.ByteCodeUtil;
import jd.printer.Printer;
import jd.util.ReferenceMap;


public class ByteCodeWriter
{
	public static void Write(
			Printer spw, ReferenceMap referenceMap, 
			ClassFile classFile, Method method)
	{
		// Ecriture du byte code
		byte[] code = method.getCode();
		if (code != null)
		{
			int    length = code.length;
			int    ioperande = 0;
			short  soperande = 0;
			
			spw.startComment();
			
			ConstantPool constants = classFile.getConstantPool();

			spw.print(Printer.UNKNOWN_LINE_NUMBER, "// Byte code:");
			spw.endOfLineComment();
			
			for (int index=0; index<length; ++index)
			{
				int offset = index;
				int opcode = code[index] & 255;
				
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "//  ");	
				spw.print(Printer.UNKNOWN_LINE_NUMBER, offset);	
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ": ");	
				spw.print(
					Printer.UNKNOWN_LINE_NUMBER, 
					ByteCodeConstants.OPCODE_NAMES[opcode]);	
				
				switch (ByteCodeConstants.NO_OF_OPERANDS[opcode])
				{
				case 1:
					spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");
					switch (opcode)
					{
					case ByteCodeConstants.NEWARRAY:
						spw.print(
							Printer.UNKNOWN_LINE_NUMBER, 
							ByteCodeConstants.TYPE_NAMES[code[++index] & 255]);
						break;
					default:
						spw.print(Printer.UNKNOWN_LINE_NUMBER, code[++index]);
					}
					break;
				case 2:
					spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");
					switch (opcode)
					{
					case ByteCodeConstants.IINC:
						spw.print(Printer.UNKNOWN_LINE_NUMBER, code[++index]);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");
						spw.print(Printer.UNKNOWN_LINE_NUMBER, code[++index]);
						break;
						
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
					case ByteCodeConstants.JSR:
						soperande = (short)( ((code[++index] & 255) << 8) | 
									         (code[++index] & 255) );
						if (soperande >= 0)
							spw.print(Printer.UNKNOWN_LINE_NUMBER, '+');
						spw.print(Printer.UNKNOWN_LINE_NUMBER, soperande);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, " -> ");
						spw.print(
							Printer.UNKNOWN_LINE_NUMBER, index + soperande - 2);
						break;
						
					case ByteCodeConstants.PUTSTATIC: 
					case ByteCodeConstants.PUTFIELD:
					case ByteCodeConstants.GETSTATIC:
					case ByteCodeConstants.OUTERTHIS:
					case ByteCodeConstants.GETFIELD:
						ioperande = ((code[++index] & 255) << 8) | 
			            			(code[++index] & 255);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, ioperande);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t");
						spw.print(
							Printer.UNKNOWN_LINE_NUMBER, 
							GetConstantFieldName(constants, ioperande));
						break;
						
					case ByteCodeConstants.INVOKESTATIC:
					case ByteCodeConstants.INVOKESPECIAL:
					case ByteCodeConstants.INVOKEVIRTUAL:
						ioperande = ((code[++index] & 255) << 8) | 
            						(code[++index] & 255);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, ioperande);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t");
						spw.print(
							Printer.UNKNOWN_LINE_NUMBER, 
							GetConstantMethodName(constants, ioperande));
						break;
						
					case ByteCodeConstants.NEW:
					case ByteCodeConstants.ANEWARRAY:
					case ByteCodeConstants.CHECKCAST:
						ioperande = ((code[++index] & 255) << 8) | 
									(code[++index] & 255);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, ioperande);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t");
						spw.print(
							Printer.UNKNOWN_LINE_NUMBER, 
							constants.getConstantClassName(ioperande));
						break;						
						
					default:
						ioperande = ((code[++index] & 255) << 8) | 
						            (code[++index] & 255);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, ioperande);
					}
					break;
				default:
					switch (opcode)
					{
					case ByteCodeConstants.MULTIANEWARRAY:
						spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");
						spw.print(
							Printer.UNKNOWN_LINE_NUMBER, 
							((code[++index] & 255) << 8) | (code[++index] & 255));
						spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");
						spw.print(Printer.UNKNOWN_LINE_NUMBER, code[++index]);
						break;
					case ByteCodeConstants.INVOKEINTERFACE:
						spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");
						spw.print(
							Printer.UNKNOWN_LINE_NUMBER, 
							((code[++index] & 255) << 8) | (code[++index] & 255));
						spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");
						spw.print(Printer.UNKNOWN_LINE_NUMBER, code[++index]);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");
						spw.print(Printer.UNKNOWN_LINE_NUMBER, code[++index]);
						break;
					case ByteCodeConstants.TABLESWITCH:
						// Skip padding
						index = ((index+4) & 0xFFFC) - 1;
						
						spw.print(Printer.UNKNOWN_LINE_NUMBER, "\tdefault:+");
						
						int jump = ((code[++index] & 255) << 24) | 
									  ((code[++index] & 255) << 16) |
						              ((code[++index] & 255) << 8 ) |  
						              (code[++index] & 255);
						
						spw.print(Printer.UNKNOWN_LINE_NUMBER, jump);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, "->");
						spw.print(Printer.UNKNOWN_LINE_NUMBER, offset + jump);
						
						int low =  ((code[++index] & 255) << 24) | 
						           ((code[++index] & 255) << 16) |
				                   ((code[++index] & 255) << 8 ) |  
				                   (code[++index] & 255);						
						int high = ((code[++index] & 255) << 24) | 
						           ((code[++index] & 255) << 16) |
				                   ((code[++index] & 255) << 8 ) |  
				                   (code[++index] & 255);

						for (int value=low; value<=high; value++)
						{
							spw.print(Printer.UNKNOWN_LINE_NUMBER, ", ");
							spw.print(Printer.UNKNOWN_LINE_NUMBER, value);
							spw.print(Printer.UNKNOWN_LINE_NUMBER, ":+");
							
							jump = ((code[++index] & 255) << 24) | 
									  ((code[++index] & 255) << 16) |
						              ((code[++index] & 255) << 8 ) |  
						              (code[++index] & 255);
							
							spw.print(Printer.UNKNOWN_LINE_NUMBER, jump);
							spw.print(Printer.UNKNOWN_LINE_NUMBER, "->");
							spw.print(Printer.UNKNOWN_LINE_NUMBER, offset + jump);
						}
						break;
					case ByteCodeConstants.LOOKUPSWITCH:
						// Skip padding
						index = ((index+4) & 0xFFFC) - 1;
						
						spw.print(Printer.UNKNOWN_LINE_NUMBER, "\tdefault:+");
						
						jump = ((code[++index] & 255) << 24) | 
								  ((code[++index] & 255) << 16) |
					              ((code[++index] & 255) << 8 ) |  
					              (code[++index] & 255);

						spw.print(Printer.UNKNOWN_LINE_NUMBER, jump);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, "->");
						spw.print(Printer.UNKNOWN_LINE_NUMBER, offset + jump);

						int npairs = ((code[++index] & 255) << 24) | 
				           			 ((code[++index] & 255) << 16) |
		                             ((code[++index] & 255) << 8 ) |  
		                             (code[++index] & 255);						

						for (int i=0; i<npairs; i++)
						{
							spw.print(Printer.UNKNOWN_LINE_NUMBER, ", ");
							spw.print(
								Printer.UNKNOWN_LINE_NUMBER, 
								((code[++index] & 255) << 24) | 
								((code[++index] & 255) << 16) |
						        ((code[++index] & 255) << 8 ) |  
						        (code[++index] & 255));
							spw.print(Printer.UNKNOWN_LINE_NUMBER, ":+");
							
							jump = ((code[++index] & 255) << 24) | 
									  ((code[++index] & 255) << 16) |
						              ((code[++index] & 255) << 8 ) |  
						              (code[++index] & 255);

							spw.print(Printer.UNKNOWN_LINE_NUMBER, jump);
							spw.print(Printer.UNKNOWN_LINE_NUMBER, "->");
							spw.print(Printer.UNKNOWN_LINE_NUMBER, offset + jump);
						}
						break;
					case ByteCodeConstants.WIDE:
						index = ByteCodeUtil.WideOffset(code, index);						
						break;
					default:
						for (int j=ByteCodeConstants.NO_OF_OPERANDS[opcode]; j>0; --j)
						{
							spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");
							spw.print(Printer.UNKNOWN_LINE_NUMBER, code[++index]);		
						}
					}
				}
				
				spw.endOfLineComment();
			}

			WriteAttributeNumberTables(spw, method);	
			WriteAttributeLocalVariableTables(
					spw, referenceMap, classFile, method);
			WriteCodeExceptions(spw, referenceMap, classFile, method);
			
			spw.endComment();
		}
	}
	
	private static void WriteAttributeNumberTables(
			Printer spw, Method method)
	{
		// Ecriture de la table des numeros de ligne
		LineNumber[] lineNumbers = method.getLineNumbers();
		if (lineNumbers != null)
		{
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "// Line number table:");
			spw.endOfLineComment();
			
			for (int i=0; i<lineNumbers.length; i++)
			{
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "//  Java source line #");					
				spw.print(Printer.UNKNOWN_LINE_NUMBER, lineNumbers[i].line_number);					
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t-> byte code line #");					
				spw.print(Printer.UNKNOWN_LINE_NUMBER, lineNumbers[i].start_pc);					
				spw.endOfLineComment();					
			}
		}
	}
	
	private static void WriteAttributeLocalVariableTables(
			Printer spw, ReferenceMap referenceMap, 
			ClassFile classFile, Method method)
	{
		// Ecriture de la table des variables locales
		LocalVariables localVariables = method.getLocalVariables();
		if (localVariables != null)
		{
			int length = localVariables.size();
			
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "// Local variable table:");
			spw.endOfLineComment();
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "//  start\tlength\tslot\tname\tsignature");
			spw.endOfLineComment();
			
			ConstantPool constants = classFile.getConstantPool();
				
			for (int i=0; i<length; i++)
			{
				LocalVariable lv = localVariables.getLocalVariableAt(i);
					
				if (lv != null)
				{
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "//  ");	
					spw.print(Printer.UNKNOWN_LINE_NUMBER, lv.start_pc);	
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t");	
					spw.print(Printer.UNKNOWN_LINE_NUMBER, lv.length);	
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t");	
					spw.print(Printer.UNKNOWN_LINE_NUMBER, lv.index);	
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t");	
					if (lv.name_index > 0)
						spw.print(Printer.UNKNOWN_LINE_NUMBER, constants.getConstantUtf8(lv.name_index));
					else
						spw.print(Printer.UNKNOWN_LINE_NUMBER, "???");
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t");	
					if (lv.signature_index > 0)
						SignatureWriter.WriteSimpleSignature(
							spw, Printer.UNKNOWN_LINE_NUMBER, referenceMap, 
							classFile, constants.getConstantUtf8(lv.signature_index));
					else
						spw.print(Printer.UNKNOWN_LINE_NUMBER, "???");
					spw.endOfLineComment();	
				}
			}
		}
	}
		
	private static void WriteCodeExceptions(
			Printer spw, ReferenceMap referenceMap, 
			ClassFile classFile, Method method)
	{
		// Ecriture de la table des exceptions
		CodeException[] codeExceptions = method.getCodeExceptions();
		if (codeExceptions != null)
		{
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "// Exception table:");
			spw.endOfLineComment();	
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "//  from\tto\ttarget\ttype");
			spw.endOfLineComment();	
			
			for (int i=0; i<codeExceptions.length; i++)
			{
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "//  ");	
				spw.print(Printer.UNKNOWN_LINE_NUMBER, codeExceptions[i].start_pc);	
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t");	
				spw.print(Printer.UNKNOWN_LINE_NUMBER, codeExceptions[i].end_pc);	
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t");	
				spw.print(Printer.UNKNOWN_LINE_NUMBER, codeExceptions[i].handler_pc);	
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "\t");	
				
				if (codeExceptions[i].catch_type == 0)
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "finally");
				else
					spw.print(
						Printer.UNKNOWN_LINE_NUMBER, 
						classFile.getConstantPool().getConstantClassName(
								codeExceptions[i].catch_type));
				spw.endOfLineComment();	
			}
		}
	}

	private static String GetConstantFieldName(
			ConstantPool constants, int index)
	{
		ConstantFieldref cfr = constants.getConstantFieldref(index);
		
		String classPath = constants.getConstantClassName(cfr.class_index);
		
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cfr.name_and_type_index);

		String fieldName = constants.getConstantUtf8(cnat.name_index);
		String fieldDescriptor = 
			constants.getConstantUtf8(cnat.descriptor_index);
		
		return classPath + ':' + fieldName + "\t" + fieldDescriptor;
	}

	private static String GetConstantMethodName(
			ConstantPool constants, int index)
	{
		ConstantMethodref cfr = constants.getConstantMethodref(index);
		
		String classPath = constants.getConstantClassName(cfr.class_index);
		
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cfr.name_and_type_index);

		String fieldName = constants.getConstantUtf8(cnat.name_index);
		String fieldDescriptor = 
			constants.getConstantUtf8(cnat.descriptor_index);
		
		return classPath + ':' + fieldName + "\t" + fieldDescriptor;
	}
}
