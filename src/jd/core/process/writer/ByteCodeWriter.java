package jd.core.process.writer;

import jd.core.loader.Loader;
import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.CodeException;
import jd.core.model.classfile.attribute.LineNumber;
import jd.core.model.classfile.constant.Constant;
import jd.core.model.classfile.constant.ConstantClass;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.Printer;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;


public class ByteCodeWriter
{
	private static final String CORRUPTED_CONSTANT_POOL = 
		"Corrupted_Constant_Pool";
	
	public static void Write(
		Loader loader, Printer printer, ReferenceMap referenceMap, 
		ClassFile classFile, Method method)
	{
		// Ecriture du byte code
		byte[] code = method.getCode();
		
		if (code != null)
		{
			int    length = code.length;
			int    ioperande = 0;
			short  soperande = 0;
			
			printer.startOfComment();
			
			ConstantPool constants = classFile.getConstantPool();

			printer.print("// Byte code:");
			
			for (int index=0; index<length; ++index)
			{
				int offset = index;
				int opcode = code[index] & 255;
				
				printer.endOfLine();
				printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
				printer.print("//   ");	
				printer.print(offset);	
				printer.print(": ");	
				printer.print(					
					ByteCodeConstants.OPCODE_NAMES[opcode]);	
				
				switch (ByteCodeConstants.NO_OF_OPERANDS[opcode])
				{
				case 1:
					printer.print(" ");
					switch (opcode)
					{
					case ByteCodeConstants.NEWARRAY:
						printer.print(							
							ByteCodeConstants.TYPE_NAMES[code[++index] & 16]);
						break;
					default:
						printer.print(code[++index]);
					}
					break;
				case 2:
					printer.print(" ");
					switch (opcode)
					{
					case ByteCodeConstants.IINC:
						printer.print(code[++index]);
						printer.print(" ");
						printer.print(code[++index]);
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
							printer.print('+');
						printer.print(soperande);
						printer.print(" -> ");
						printer.print(
							index + soperande - 2);
						break;
						
					case ByteCodeConstants.PUTSTATIC: 
					case ByteCodeConstants.PUTFIELD:
					case ByteCodeConstants.GETSTATIC:
					case ByteCodeConstants.OUTERTHIS:
					case ByteCodeConstants.GETFIELD:
						ioperande = ((code[++index] & 255) << 8) | 
			            			(code[++index] & 255);
						printer.print(ioperande);
						printer.print("\t");
						String fieldName = 
							GetConstantFieldName(constants, ioperande);
						
						if (fieldName == null)
						{
							printer.startOfError();
							printer.print(CORRUPTED_CONSTANT_POOL);
							printer.endOfError();
						}
						else
						{
							printer.print(fieldName);
						}						
						break;
						
					case ByteCodeConstants.INVOKESTATIC:
					case ByteCodeConstants.INVOKESPECIAL:
					case ByteCodeConstants.INVOKEVIRTUAL:
						ioperande = ((code[++index] & 255) << 8) | 
            						(code[++index] & 255);
						printer.print(ioperande);
						printer.print("\t");
						String methodName = 
							GetConstantMethodName(constants, ioperande);
									
						if (methodName == null)
						{
							printer.startOfError();
							printer.print(CORRUPTED_CONSTANT_POOL);
							printer.endOfError();
						}
						else
						{
							printer.print(methodName);
						}		
						break;
						
					case ByteCodeConstants.NEW:
					case ByteCodeConstants.ANEWARRAY:
					case ByteCodeConstants.CHECKCAST:
						ioperande = ((code[++index] & 255) << 8) | 
									(code[++index] & 255);
						printer.print(ioperande);
						printer.print("\t");
						
						Constant c = constants.get(ioperande);
						
						if (c.tag == ConstantConstant.CONSTANT_Class)
						{
							ConstantClass cc = (ConstantClass)c;
							printer.print(
								constants.getConstantUtf8(cc.name_index));
						}
						else
						{
							printer.print(CORRUPTED_CONSTANT_POOL);
						}
						break;						
						
					default:
						ioperande = ((code[++index] & 255) << 8) | 
						            (code[++index] & 255);
						printer.print(ioperande);
					}
					break;
				default:
					switch (opcode)
					{
					case ByteCodeConstants.MULTIANEWARRAY:
						printer.print(" ");
						printer.print(							
							((code[++index] & 255) << 8) | (code[++index] & 255));
						printer.print(" ");
						printer.print(code[++index]);
						break;
					case ByteCodeConstants.INVOKEINTERFACE:
						printer.print(" ");
						printer.print(							
							((code[++index] & 255) << 8) | (code[++index] & 255));
						printer.print(" ");
						printer.print(code[++index]);
						printer.print(" ");
						printer.print(code[++index]);
						break;
					case ByteCodeConstants.TABLESWITCH:
						// Skip padding
						index = ((index+4) & 0xFFFC) - 1;
						
						printer.print("\tdefault:+");
						
						int jump = ((code[++index] & 255) << 24) | 
									  ((code[++index] & 255) << 16) |
						              ((code[++index] & 255) << 8 ) |  
						              (code[++index] & 255);
						
						printer.print(jump);
						printer.print("->");
						printer.print(offset + jump);
						
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
							printer.print(", ");
							printer.print(value);
							printer.print(":+");
							
							jump = ((code[++index] & 255) << 24) | 
									  ((code[++index] & 255) << 16) |
						              ((code[++index] & 255) << 8 ) |  
						              (code[++index] & 255);
							
							printer.print(jump);
							printer.print("->");
							printer.print(offset + jump);
						}
						break;
					case ByteCodeConstants.LOOKUPSWITCH:
						// Skip padding
						index = ((index+4) & 0xFFFC) - 1;
						
						printer.print("\tdefault:+");
						
						jump = ((code[++index] & 255) << 24) | 
								  ((code[++index] & 255) << 16) |
					              ((code[++index] & 255) << 8 ) |  
					              (code[++index] & 255);

						printer.print(jump);
						printer.print("->");
						printer.print(offset + jump);

						int npairs = ((code[++index] & 255) << 24) | 
				           			 ((code[++index] & 255) << 16) |
		                             ((code[++index] & 255) << 8 ) |  
		                             (code[++index] & 255);						

						for (int i=0; i<npairs; i++)
						{
							printer.print(", ");
							printer.print(
								
								((code[++index] & 255) << 24) | 
								((code[++index] & 255) << 16) |
						        ((code[++index] & 255) << 8 ) |  
						        (code[++index] & 255));
							printer.print(":+");
							
							jump = ((code[++index] & 255) << 24) | 
									  ((code[++index] & 255) << 16) |
						              ((code[++index] & 255) << 8 ) |  
						              (code[++index] & 255);

							printer.print(jump);
							printer.print("->");
							printer.print(offset + jump);
						}
						break;
					case ByteCodeConstants.WIDE:
						index = ByteCodeUtil.NextWideOffset(code, index);						
						break;
					default:
						for (int j=ByteCodeConstants.NO_OF_OPERANDS[opcode]; j>0; --j)
						{
							printer.print(" ");
							printer.print(code[++index]);		
						}
					}
				}
			}

			WriteAttributeNumberTables(printer, method);	
			WriteAttributeLocalVariableTables(
				loader, printer, referenceMap, classFile, method);
			WriteCodeExceptions(printer, referenceMap, classFile, method);
			
			printer.endOfComment();
		}
	}
	
	private static void WriteAttributeNumberTables(
			Printer printer, Method method)
	{
		// Ecriture de la table des numeros de ligne
		LineNumber[] lineNumbers = method.getLineNumbers();
		if (lineNumbers != null)
		{			
			printer.endOfLine();
			printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
			printer.print("// Line number table:");
			
			for (int i=0; i<lineNumbers.length; i++)
			{
				printer.endOfLine();
				printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
				printer.print("//   Java source line #");					
				printer.print(lineNumbers[i].line_number);					
				printer.print("\t-> byte code offset #");					
				printer.print(lineNumbers[i].start_pc);					
			}
		}
	}
	
	private static void WriteAttributeLocalVariableTables(
			Loader loader, Printer printer, ReferenceMap referenceMap, 
			ClassFile classFile, Method method)
	{
		// Ecriture de la table des variables locales
		LocalVariables localVariables = method.getLocalVariables();
		if (localVariables != null)
		{
			int length = localVariables.size();
			
			printer.endOfLine();
			printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
			printer.print("// Local variable table:");
			printer.endOfLine();
			printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
			printer.print("//   start\tlength\tslot\tname\tsignature");
			
			ConstantPool constants = classFile.getConstantPool();
				
			for (int i=0; i<length; i++)
			{
				LocalVariable lv = localVariables.getLocalVariableAt(i);
					
				if (lv != null)
				{
					printer.endOfLine();
					printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
					printer.print("//   ");	
					printer.print(lv.start_pc);	
					printer.print("\t");	
					printer.print(lv.length);	
					printer.print("\t");	
					printer.print(lv.index);	
					printer.print("\t");
					
					if (lv.name_index > 0)
					{
						printer.print(constants.getConstantUtf8(lv.name_index));
					}
					else
					{
						printer.print("???");
					}
					
					printer.print("\t");
					
					if (lv.signature_index > 0)
					{
						SignatureWriter.WriteSignature(
							loader, printer, referenceMap, 
							classFile, constants.getConstantUtf8(lv.signature_index));
					}
					else
					{
						printer.print("???");
					}
				}
			}
		}
	}
		
	private static void WriteCodeExceptions(
			Printer printer, ReferenceMap referenceMap, 
			ClassFile classFile, Method method)
	{
		// Ecriture de la table des exceptions
		CodeException[] codeExceptions = method.getCodeExceptions();
		if ((codeExceptions != null) && (codeExceptions.length > 0))
		{
			printer.endOfLine();
			printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
			printer.print("// Exception table:");
			printer.endOfLine();	
			printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
			printer.print("//   from\tto\ttarget\ttype");
			
			for (int i=0; i<codeExceptions.length; i++)
			{
				printer.endOfLine();	
				printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
				printer.print("//   ");	
				printer.print(codeExceptions[i].start_pc);	
				printer.print("\t");	
				printer.print(codeExceptions[i].end_pc);	
				printer.print("\t");	
				printer.print(codeExceptions[i].handler_pc);	
				printer.print("\t");	
				
				if (codeExceptions[i].catch_type == 0)
					printer.print("finally");
				else
					printer.print(
						
						classFile.getConstantPool().getConstantClassName(
								codeExceptions[i].catch_type));
			}
		}
	}

	private static String GetConstantFieldName(
			ConstantPool constants, int index)
	{				
		ConstantFieldref cfr;
		Constant c = constants.get(index);
		
		switch (c.tag)
		{
		case ConstantConstant.CONSTANT_Fieldref:
			cfr = (ConstantFieldref)c;
			break;
		default:
			return null;
		}
		
		ConstantClass cc;
		c = constants.get(cfr.class_index);
		
		switch (c.tag)
		{
		case ConstantConstant.CONSTANT_Class:
			cc = (ConstantClass)c;
			break;
		default:
			return null;
		}
		
		String classPath = constants.getConstantUtf8(cc.name_index);
		
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
		ConstantMethodref cfr;
		Constant c = constants.get(index);
		
		switch (c.tag)
		{
		case ConstantConstant.CONSTANT_Methodref:
		case ConstantConstant.CONSTANT_InterfaceMethodref:
			cfr = (ConstantMethodref)c;
			break;
		default:
			return null;
		}
		
		ConstantClass cc;
		c = constants.get(cfr.class_index);
		
		switch (c.tag)
		{
		case ConstantConstant.CONSTANT_Class:
			cc = (ConstantClass)c;
			break;
		default:
			return null;
		}
		
		String classPath = constants.getConstantUtf8(cc.name_index);
				
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cfr.name_and_type_index);

		String fieldName = constants.getConstantUtf8(cnat.name_index);
		String fieldDescriptor = 
			constants.getConstantUtf8(cnat.descriptor_index);
		
		return classPath + ':' + fieldName + "\t" + fieldDescriptor;
	}
}
