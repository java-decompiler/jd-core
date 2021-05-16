package jd.instruction.bytecode.visitor;

import java.util.HashSet;
import java.util.List;

import jd.Constants;
import jd.Preferences;
import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.LocalVariable;
import jd.classfile.LocalVariables;
import jd.classfile.Method;
import jd.classfile.analyzer.SignatureAnalyzer;
import jd.classfile.constant.Constant;
import jd.classfile.constant.ConstantClass;
import jd.classfile.constant.ConstantFieldref;
import jd.classfile.constant.ConstantMethodref;
import jd.classfile.constant.ConstantNameAndType;
import jd.classfile.constant.ConstantUtf8;
import jd.classfile.constant.ConstantValue;
import jd.classfile.writer.ClassFileWriter;
import jd.classfile.writer.ConstantValueWriter;
import jd.classfile.writer.SignatureWriter;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.ALoad;
import jd.instruction.bytecode.instruction.ANewArray;
import jd.instruction.bytecode.instruction.AThrow;
import jd.instruction.bytecode.instruction.ArrayLength;
import jd.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.instruction.bytecode.instruction.AssertInstruction;
import jd.instruction.bytecode.instruction.AssignmentInstruction;
import jd.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.instruction.bytecode.instruction.CheckCast;
import jd.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.instruction.bytecode.instruction.ConstInstruction;
import jd.instruction.bytecode.instruction.ConvertInstruction;
import jd.instruction.bytecode.instruction.DupLoad;
import jd.instruction.bytecode.instruction.DupStore;
import jd.instruction.bytecode.instruction.ExceptionLoad;
import jd.instruction.bytecode.instruction.GetField;
import jd.instruction.bytecode.instruction.GetStatic;
import jd.instruction.bytecode.instruction.Goto;
import jd.instruction.bytecode.instruction.IConst;
import jd.instruction.bytecode.instruction.IInc;
import jd.instruction.bytecode.instruction.IfCmp;
import jd.instruction.bytecode.instruction.IfInstruction;
import jd.instruction.bytecode.instruction.IncInstruction;
import jd.instruction.bytecode.instruction.IndexInstruction;
import jd.instruction.bytecode.instruction.InitArrayInstruction;
import jd.instruction.bytecode.instruction.InstanceOf;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.InvokeNew;
import jd.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.instruction.bytecode.instruction.Invokestatic;
import jd.instruction.bytecode.instruction.Jsr;
import jd.instruction.bytecode.instruction.LoadInstruction;
import jd.instruction.bytecode.instruction.LookupSwitch;
import jd.instruction.bytecode.instruction.MultiANewArray;
import jd.instruction.bytecode.instruction.NewArray;
import jd.instruction.bytecode.instruction.Pop;
import jd.instruction.bytecode.instruction.PutField;
import jd.instruction.bytecode.instruction.PutStatic;
import jd.instruction.bytecode.instruction.ReturnInstruction;
import jd.instruction.bytecode.instruction.StoreInstruction;
import jd.instruction.bytecode.instruction.TableSwitch;
import jd.instruction.bytecode.instruction.TernaryOpStore;
import jd.instruction.bytecode.instruction.TernaryOperator;
import jd.instruction.bytecode.instruction.UnaryOperatorInstruction;
import jd.instruction.fast.FastConstants;
import jd.printer.Printer;
import jd.util.ReferenceMap;
import jd.util.StringUtil;



public class ByteCodeSourceWriterVisitor 
{
	protected HashSet<String> keywordSet;
	protected Preferences preferences;
	protected Printer spw;
	protected ReferenceMap referenceMap;
	protected ConstantPool constants;
	protected LocalVariables localVariables;

	protected ClassFile classFile;
	protected int methodAccessFlags;
	
	public ByteCodeSourceWriterVisitor(
			HashSet<String> keywordSet, Preferences preferences, Printer spw, 
			ReferenceMap referenceMap, ClassFile classFile, 
			int methodAccessFlags, LocalVariables localVariables)
	{
		this.keywordSet = keywordSet;
		this.preferences = preferences;
		this.spw = spw;
		this.referenceMap = referenceMap;
		this.classFile = classFile;
		this.methodAccessFlags = methodAccessFlags;
		
		if (classFile != null)
		{
			this.constants = classFile.getConstantPool();
			this.localVariables = localVariables;
		}
		else
		{
			this.constants = null;
			this.localVariables = null;
		}
	}
	
	public void visit(Instruction instruction)
	{
		int lineNumber = instruction.lineNumber;
		
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			visit(instruction, ((ArrayLength)instruction).arrayref);
			spw.print(lineNumber, ".length");
			break;
		case ByteCodeConstants.ARRAYLOAD:
			{
				ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction; 
				writeArray(ali, ali.arrayref, ali.indexref);
			}
			break;
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			{
				ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
				writeArray(asi, asi.arrayref, asi.indexref);
				spw.print(lineNumber, " = ");
				visit(asi, asi.valueref);
			}
			break;
		case ByteCodeConstants.ANEWARRAY:
			{
				ANewArray newArray = (ANewArray)instruction;
				spw.print(lineNumber, "new ");
				
				String signature = constants.getConstantClassName(newArray.index);
				
				if (signature.charAt(0) != '[')
					signature = 'L' + signature + ';';
				
				String signatureWithoutArray = 
					SignatureAnalyzer.CutArrayDimensionPrefix(signature);

				int dimensionCount = 
					signature.length() - signatureWithoutArray.length();
				
				spw.print(lineNumber, 
					SignatureWriter.WriteSimpleSignature(
						this.referenceMap, classFile, signatureWithoutArray));
				spw.print(lineNumber, '[');
				Instruction dimension = newArray.dimension;
				visit(dimension);
				spw.print(dimension.lineNumber, ']');			
				
				for (int i=dimensionCount; i>0; --i)
					spw.print(dimension.lineNumber, "[]");
			}
			break;
		case ByteCodeConstants.ACONST_NULL:
			spw.print(lineNumber, "null");
			break;
		case ByteCodeConstants.ASSERT:
			{
				AssertInstruction ai = (AssertInstruction)instruction;
				spw.print(lineNumber, "assert ");
				visit(ai, ai.test);
				if (ai.msg != null)
				{
					spw.print(lineNumber, " : ");
					visit(ai, ai.msg);
				}
			}
			break;
		case ByteCodeConstants.ASSIGNMENT:
			writeAssignmentInstruction((AssignmentInstruction)instruction);
			break;
		case ByteCodeConstants.ATHROW:
			{
				AThrow athrow = (AThrow)instruction;
				spw.print(lineNumber, "throw ");
				visit(athrow, athrow.value);
			}
			break;
		case ByteCodeConstants.UNARYOP:
			{
				UnaryOperatorInstruction ioi = 
					(UnaryOperatorInstruction)instruction;
				spw.print(lineNumber, ioi.operator);
				visit(ioi, ioi.value);
			}
			break;
		case ByteCodeConstants.BINARYOP:
			writeBinaryOperatorInstruction(
					(BinaryOperatorInstruction)instruction);
			break;
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.SIPUSH:
		case ByteCodeConstants.ICONST:
			writeBIPush_SIPush_IConst((IConst)instruction);
			break;
		case ByteCodeConstants.LCONST:
			spw.print(lineNumber, ((ConstInstruction)instruction).value);
			spw.print(lineNumber, 'L');
			break;
		case ByteCodeConstants.FCONST:
			spw.print(lineNumber, ((ConstInstruction)instruction).value);
			spw.print(lineNumber, 'F');
			break;
		case ByteCodeConstants.DCONST:
			spw.print(lineNumber, ((ConstInstruction)instruction).value);
			spw.print(lineNumber, 'D');
			break;
		case ByteCodeConstants.CONVERT:
			writeConvertInstruction((ConvertInstruction)instruction);
			break;
		case ByteCodeConstants.CHECKCAST:
			{
				CheckCast checkCast = (CheckCast)instruction;
				spw.print(lineNumber, '(');
				
				String signature;
				Constant c = constants.get(checkCast.index);
				
				if (c.tag == Constants.CONSTANT_Utf8)
				{
					ConstantUtf8 cutf8 = (ConstantUtf8)c;
					signature = cutf8.bytes;
				}
				else
				{
					ConstantClass cc = (ConstantClass)c;
					signature = constants.getConstantUtf8(cc.name_index);
				}

				if (signature.charAt(0) != '[')
					signature = 'L' + signature + ';';					

				spw.print(
					lineNumber, 
					SignatureWriter.WriteSimpleSignature(
						this.referenceMap, classFile, signature));
				spw.print(lineNumber, ')');
				visit(checkCast, checkCast.objectref);	
			}
			break;
		case ByteCodeConstants.DUPSTORE:
			spw.print(lineNumber, Constants.TMP_LOCAL_VARIABLE_NAME);
			spw.print(lineNumber, instruction.offset);
			spw.print(lineNumber, "_");
			spw.print(lineNumber, ((DupStore)instruction).objectref.offset);
			spw.print(lineNumber, " = ");
			visit(instruction, ((DupStore)instruction).objectref);
			break;
		case ByteCodeConstants.DUPLOAD:
			spw.print(lineNumber, Constants.TMP_LOCAL_VARIABLE_NAME);
			spw.print(lineNumber, instruction.offset);
			spw.print(lineNumber, "_");
			spw.print(
				lineNumber, ((DupLoad)instruction).dupStore.objectref.offset);
			break;
		case ByteCodeConstants.GETFIELD:
			{
				GetField getField = (GetField)instruction;
				
				ConstantFieldref cfr = 
					constants.getConstantFieldref(getField.index);
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cfr.name_and_type_index);
				
				boolean displayPrefix = true;
					
				if ((this.preferences.displayPrefixThis == false) &&
					!this.localVariables.containsLocalVariableWithNameIndex(cnat.name_index))
				{
					switch (getField.objectref.opcode)
					{
					case ByteCodeConstants.ALOAD:
						if (((ALoad)getField.objectref).index == 0)
							displayPrefix = false;
						break;
					case ByteCodeConstants.OUTERTHIS:
						if (!needAPrefixForThisField(
								cnat.name_index, cnat.descriptor_index, 
								(GetStatic)getField.objectref))
							displayPrefix = false;
						break;
					}
				}
				
				if (displayPrefix)
				{
					visit(getField, getField.objectref);		
					spw.print(lineNumber, '.');
				}

				String fieldName = 
					constants.getConstantUtf8(cnat.name_index);
				if (this.keywordSet.contains(fieldName))
					fieldName = Constants.JD_FIELD_PREFIX + fieldName;
				spw.print(lineNumber, fieldName);
			}
			break;
		case ByteCodeConstants.GETSTATIC:
			writeGetStatic((GetStatic)instruction);
			break;
		case ByteCodeConstants.OUTERTHIS:
			writeOuterThis((GetStatic)instruction);
			break;
		case ByteCodeConstants.GOTO:
			{
				Goto gotoInstruction = (Goto)instruction;
				spw.print(lineNumber, "goto ");
				spw.print(lineNumber, gotoInstruction.GetJumpOffset());
			}
			break;
		case ByteCodeConstants.IF:
			{
				IfInstruction ifInstruction = (IfInstruction)instruction;
				spw.print(lineNumber, "if (");
				writeIfTest(ifInstruction);
				spw.print(lineNumber, ") goto ");
				spw.print(lineNumber, ifInstruction.GetJumpOffset());
			}
			break;
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ifCmpInstruction = (IfCmp)instruction;
				spw.print(lineNumber, "if (");
				writeIfCmpTest(ifCmpInstruction);
				spw.print(lineNumber, ") goto ");
				spw.print(lineNumber, ifCmpInstruction.GetJumpOffset());
			}
			break;
		case ByteCodeConstants.IFXNULL:
			{
				IfInstruction ifXNull = (IfInstruction)instruction;
				spw.print(lineNumber, "if (");
				writeIfXNullTest(ifXNull);
				spw.print(lineNumber, ") goto ");
				spw.print(lineNumber, ifXNull.GetJumpOffset());
			}
			break;
		case FastConstants.COMPLEXIF:
			{
				ComplexConditionalBranchInstruction ccbi = 
					(ComplexConditionalBranchInstruction)instruction;
				spw.print(lineNumber, "if (");
				writeComplexConditionalBranchInstructionTest(ccbi);
				spw.print(lineNumber, ") goto ");
				spw.print(lineNumber, ccbi.GetJumpOffset());		
			}
			break;
		case ByteCodeConstants.IINC:			
			writeIInc((IInc)instruction);
			break;
		case ByteCodeConstants.PREINC:			
			writePreInc((IncInstruction)instruction);
			break;
		case ByteCodeConstants.POSTINC:			
			writePostInc((IncInstruction)instruction);
			break;
		case ByteCodeConstants.INVOKENEW:
			writeInvokeNewInstruction((InvokeNew)instruction);
			break;
		case ByteCodeConstants.INSTANCEOF:
			{
				InstanceOf instanceOf = (InstanceOf)instruction;
				visit(instanceOf, instanceOf.objectref);
				lineNumber = instanceOf.objectref.lineNumber;
				spw.print(lineNumber, " instanceof ");
				// reference to a class, array, or interface
				String signature =
					constants.getConstantClassName(instanceOf.index);
				if (signature.charAt(0) == '[')
					spw.print(
						lineNumber, SignatureWriter.WriteSimpleSignature(
							this.referenceMap, classFile, signature));
				else
					spw.print(
						lineNumber, SignatureWriter.WriteSimpleSignature(
							this.referenceMap, classFile, 'L' + signature + ';'));
			}
			break;
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKEVIRTUAL:
			writeInvokeNoStaticInstruction((InvokeNoStaticInstruction)instruction);
			break;	
		case ByteCodeConstants.INVOKESPECIAL:
			writeInvokespecial((InvokeNoStaticInstruction)instruction);
			break;	
		case ByteCodeConstants.INVOKESTATIC:
			writeInvokestatic((Invokestatic)instruction);
			break;
		case ByteCodeConstants.JSR:
			spw.print(lineNumber, "jsr ");
			spw.print(lineNumber, (short)((Jsr)instruction).value);
			break;
		case ByteCodeConstants.LDC:
		case ByteCodeConstants.LDC2_W:
			writeLcdInstruction((IndexInstruction)instruction);
			break;
		case ByteCodeConstants.LOAD:
		case ByteCodeConstants.ALOAD:
		case ByteCodeConstants.ILOAD:
			writeLoadInstruction((LoadInstruction)instruction);
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
			{
				LookupSwitch lookupSwitch = (LookupSwitch)instruction;
				spw.print(lineNumber, "switch (");
				visit(lookupSwitch.key);
				spw.print(lookupSwitch.key.lineNumber, ')');
			}
			break;
		case ByteCodeConstants.TABLESWITCH:
			{
				TableSwitch tableSwitch = (TableSwitch)instruction;
				spw.print(lineNumber, "switch (");
				visit(tableSwitch.key);
				spw.print(tableSwitch.key.lineNumber, ')');
			}
			break;
		case ByteCodeConstants.MONITORENTER:
			spw.print(lineNumber, "monitorenter");
			break;
		case ByteCodeConstants.MONITOREXIT:
			spw.print(lineNumber, "monitorexit");
			break;
		case ByteCodeConstants.MULTIANEWARRAY:
			writeMultiANewArray((MultiANewArray)instruction);
			break;
		case ByteCodeConstants.NEW:
			spw.print(lineNumber, "new ");
			spw.print(
				lineNumber, constants.getConstantClassName(
					((IndexInstruction)instruction).index));
			break;
		case ByteCodeConstants.NEWARRAY:
			{
				NewArray newArray = (NewArray)instruction;
				spw.print(lineNumber, "new ");
				spw.print(
					lineNumber, SignatureWriter.WriteSimpleSignature(
						this.referenceMap, classFile, 
						SignatureAnalyzer.GetSignatureFromType(newArray.type)));
				spw.print(lineNumber, '[');
				Instruction dimension = newArray.dimension;
				visit(dimension);
				spw.print(dimension.lineNumber, ']');			
			}
			break;
		case ByteCodeConstants.POP:
			visit(instruction, ((Pop)instruction).objectref);
			break;
		case ByteCodeConstants.PUTFIELD:
			{
				PutField putField = (PutField)instruction;
				
				ConstantFieldref cfr = constants.getConstantFieldref(putField.index);
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cfr.name_and_type_index);
				
				boolean displayPrefix = true;
				
				if ((this.preferences.displayPrefixThis == false) &&
					!this.localVariables.containsLocalVariableWithNameIndex(cnat.name_index))
				{
					switch (putField.objectref.opcode)
					{
					case ByteCodeConstants.ALOAD:
						if (((ALoad)putField.objectref).index == 0)
							displayPrefix = false;
						break;
					case ByteCodeConstants.OUTERTHIS:
						if (!needAPrefixForThisField(
								cnat.name_index, cnat.descriptor_index, 
								(GetStatic)putField.objectref))
							displayPrefix = false;
						break;
					}
				}
				
				if (displayPrefix)
				{
					visit(putField, putField.objectref);
					lineNumber = putField.objectref.lineNumber;
					spw.print(lineNumber, '.');
				}
				
				String fieldName = 
					constants.getConstantUtf8(cnat.name_index);
				if (this.keywordSet.contains(fieldName))
					fieldName = Constants.JD_FIELD_PREFIX + fieldName;
				
				spw.print(lineNumber, fieldName);
				spw.print(lineNumber, " = ");
				visit(putField, putField.valueref);
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			writePutStatic((PutStatic)instruction);
			break;
		case ByteCodeConstants.RET:
			spw.print(lineNumber, "ret");
			break;
		case ByteCodeConstants.RETURN:
			spw.print(lineNumber, "return");
			break;
		case ByteCodeConstants.XRETURN:
			spw.print(lineNumber, "return ");
			visit(((ReturnInstruction)instruction).valueref);
			break;
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			writeStoreInstruction((StoreInstruction)instruction);
			break;
		case ByteCodeConstants.EXCEPTIONLOAD:
			writeExceptionLoad((ExceptionLoad)instruction);
			break;
		case ByteCodeConstants.RETURNADDRESSLOAD:
			spw.print(lineNumber, "returnAddress");
			break;
		case ByteCodeConstants.TERNARYOPSTORE:
			spw.print(lineNumber, "tmpTernaryOp");
			spw.print(lineNumber, " = ");
			visit(instruction, ((TernaryOpStore)instruction).objectref);
			break;
		case FastConstants.TERNARYOP:
			{
				TernaryOperator tp = (TernaryOperator)instruction;
				visit(tp.test);
				spw.print(tp.test.lineNumber, " ? ");		
				visit(tp, tp.value1);
				spw.print(tp.value1.lineNumber, " : ");		
				visit(tp, tp.value2);
			}
			break;
		case FastConstants.INITARRAY:
			WriteInitArrayInstruction((InitArrayInstruction)instruction);
			break;
		case FastConstants.NEWANDINITARRAY:
			WriteNewAndInitArrayInstruction((InitArrayInstruction)instruction);
			break;
		case ByteCodeConstants.NOP:
			break;
		default:
			System.err.println(
					"Can not write code for " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
	}
	
	protected void visit(Instruction parent, Instruction child)
	{
		if (parent.getPriority() < child.getPriority())
		{
			spw.print(child.lineNumber, '(');
			visit(child);
			spw.print(child.lineNumber, ')');			
		}
		else
		{
			visit(child);
		}
	}
	
	protected void visit(int parentPriority, Instruction child)
	{
		if (parentPriority < child.getPriority())
		{
			spw.print(child.lineNumber, '(');
			visit(child);
			spw.print(child.lineNumber, ')');			
		}
		else
		{
			visit(child);
		}
	}
	
	private boolean needAPrefixForThisField(
		int fieldNameIndex, int fieldDescriptorIndex, GetStatic getStatic)
	{
		if (this.classFile.getField(fieldNameIndex, fieldDescriptorIndex) != null)
		{
			// La classe courante contient un champ ayant le meme nom et la 
			// meme signature
			return true;
		}

		ConstantFieldref cfr = 
			this.constants.getConstantFieldref(getStatic.index);
		String getStaticOuterClassName = 
			this.constants.getConstantClassName(cfr.class_index);
		String fieldName = this.constants.getConstantUtf8(fieldNameIndex);
		String fieldDescriptor = 
			this.constants.getConstantUtf8(fieldDescriptorIndex);
		
		ClassFile outerClassFile = this.classFile.getOuterClass();
		
		while (outerClassFile != null)
		{
			String outerClassName = outerClassFile.getThisClassName();
			if (outerClassName.equals(getStaticOuterClassName))
				break;
			
			if (outerClassFile.getField(fieldName, fieldDescriptor) != null)
			{
				// La classe englobante courante contient un champ ayant le 
				// meme nom et la meme signature
				return true;
			}
			
			outerClassFile = outerClassFile.getOuterClass();
		}
		
		return false;
	}

	private void writeBIPush_SIPush_IConst(IConst iconst)
	{
		int value = iconst.value;
		String signature = iconst.getSignature();
			
		if ("C".equals(signature))
		{
			String escapedString =
				StringUtil.EscapeCharAndAppendApostrophe((char)value);
			spw.printString(iconst.lineNumber, escapedString);
		}
		else if ("Z".equals(signature))
		{			
			spw.printKeyword(iconst.lineNumber, (value == 0) ? "false" : "true");
		}
		else
		{
			spw.print(iconst.lineNumber, value);
		}	
	}
	
	private void writeArray(
			Instruction parent, Instruction arrayref, Instruction indexref)
	{
		visit(parent, arrayref);
		spw.print(arrayref.lineNumber, '[');
		visit(parent, indexref);
		spw.print(indexref.lineNumber, ']');
	}

	/* +, -, *, /, %, <<, >>, >>>, &, |, ^ */
	private void writeBinaryOperatorInstruction(BinaryOperatorInstruction boi)
	{
		int lineNumber = boi.value1.lineNumber;
		
		if (boi.operator.length() == 1)
		{
			switch (boi.operator.charAt(0))
			{
			case '&': case '|': case '^':
				{
					// Binary operators
					if (boi.getPriority() < boi.value1.getPriority())
					{
						spw.print(boi.value1.lineNumber, '(');
						writeBinaryOperatorParameterInHexaOrBoolean(boi.value1);
						spw.print(boi.value1.lineNumber, ')');			
					}
					else
					{
						writeBinaryOperatorParameterInHexaOrBoolean(boi.value1);
					}	
					
					spw.print(lineNumber, ' ');
					spw.print(lineNumber, boi.operator);
					spw.print(lineNumber, ' ');
					
					if (boi.getPriority() <= boi.value2.getPriority())
					{
						spw.print(boi.value2.lineNumber, '(');
						writeBinaryOperatorParameterInHexaOrBoolean(boi.value2);
						spw.print(boi.value2.lineNumber, ')');			
					}
					else
					{
						writeBinaryOperatorParameterInHexaOrBoolean(boi.value2);
					}	
					return;
				}
			}
		}
		
		// Other operators
		if (boi.getPriority() < boi.value1.getPriority())
		{
			spw.print(boi.value1.lineNumber, '(');
			visit(boi.value1);
			spw.print(boi.value1.lineNumber, ')');			
		}
		else
		{
			visit(boi.value1);
		}		

		spw.print(lineNumber, ' ');
		spw.print(lineNumber, boi.operator);
		spw.print(lineNumber, ' ');
		
		if (boi.getPriority() <= boi.value2.getPriority())
		{
			spw.print(boi.value2.lineNumber, '(');
			visit(boi.value2);
			spw.print(boi.value2.lineNumber, ')');			
		}
		else
		{
			visit(boi.value2);
		}		
	}
	
	private void writeBinaryOperatorParameterInHexaOrBoolean(Instruction value)
	{
		switch (value.opcode)
		{
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.SIPUSH:
			{
				IConst iconst = (IConst)value;
				if (iconst.signature.equals("Z"))
				{
					if (iconst.value == 0)
						spw.print(value.lineNumber, "false");
					else
						spw.print(value.lineNumber, "true");
				}
				else
				{
					spw.print(value.lineNumber, "0x");
					spw.print(value.lineNumber, 
						Integer.toHexString(iconst.value).toUpperCase());
				}
			}
			break;
		case ByteCodeConstants.LDC:
		case ByteCodeConstants.LDC2_W:
			Constant cst = constants.get( ((IndexInstruction)value).index );
			ConstantValueWriter.WriteHexa(
				spw, value.lineNumber, constants, (ConstantValue)cst);
			break;
		default:
			visit(value);
			break;
		}	
	}
	
	protected void writeIfTest(IfInstruction ifInstruction)
	{
		String signature = 
			ifInstruction.value.getReturnedSignature(constants, localVariables);
		
		if (signature.charAt(0) == 'Z')
		{
			switch (ifInstruction.cmp)
			{
			case ByteCodeConstants.CMP_EQ:
			case ByteCodeConstants.CMP_LE:
			case ByteCodeConstants.CMP_GE:
				spw.print(ifInstruction.lineNumber, "!");
			}

			visit(2, ifInstruction.value);					
				
//			visit(ifInstruction, ifInstruction.value);	
//			switch (ifInstruction.cmp)
//			{
//			case ByteCodeConstants.CMP_EQ:
//			case ByteCodeConstants.CMP_LE:
//			case ByteCodeConstants.CMP_GE:
//				spw.print(" == false");
//				break;
//			default:
//				spw.print(" == true");	
//			}
		}
		else
		{
			int lineNumber = ifInstruction.value.lineNumber;
			
			visit(6, ifInstruction.value);	
			spw.print(lineNumber, ' ');
			spw.print(lineNumber, ByteCodeConstants.CMP_NAMES[ifInstruction.cmp]);
			spw.print(lineNumber, " 0");
		}
	}

	protected void writeIfCmpTest(IfCmp ifCmpInstruction)
	{
		int lineNumber = ifCmpInstruction.value1.lineNumber;
		visit(6, ifCmpInstruction.value1);
		spw.print(lineNumber, ' ');
		spw.print(lineNumber, ByteCodeConstants.CMP_NAMES[ifCmpInstruction.cmp]);
		spw.print(lineNumber, ' ');
		visit(6, ifCmpInstruction.value2);
	}

	protected void writeIfXNullTest(IfInstruction ifXNull)
	{
		int lineNumber = ifXNull.value.lineNumber;
		visit(6, ifXNull.value);
		spw.print(lineNumber, ' ');
		spw.print(lineNumber, ByteCodeConstants.CMP_NAMES[ifXNull.cmp]);
		spw.print(lineNumber, " null");
	}
	
	protected void writeComplexConditionalBranchInstructionTest(
		ComplexConditionalBranchInstruction ccbi)
	{
		List<Instruction> branchList = ccbi.instructions;
		int lenght = branchList.size();

		if (lenght > 1)
		{
			String operator = 
				(ccbi.cmp == FastConstants.CMP_AND) ? " && " : " || ";
			
			Instruction instruction = branchList.get(0);
			int lineNumber = instruction.lineNumber;
			
			spw.print(lineNumber, '(');
			visit(instruction);
			spw.print(lineNumber, ')');

			for (int i=1; i<lenght; i++)
			{
				spw.print(lineNumber, operator);

				instruction = branchList.get(i);
				lineNumber = instruction.lineNumber;
				
				spw.print(lineNumber, '(');
				visit(instruction);
				spw.print(lineNumber, ')');
			}
		}
		else if (lenght > 0)
		{
			visit(branchList.get(0));
		}
	}

	private void writeIInc(IInc iinc)
	{
		String lvName = null;
		
		LocalVariable lv = 
			this.localVariables.getLocalVariableWithIndexAndOffset(
					iinc.index, iinc.offset);
		
		if (lv != null)
		{
			int lvNameIndex = lv.name_index;
			if (lvNameIndex > 0)
				lvName = constants.getConstantUtf8(lvNameIndex);
		}
		
		if (lvName == null)
		{
			lvName = "???";
			//new RuntimeException("local variable not found")
			//	.printStackTrace();
		}
		
		int lineNumber = iinc.lineNumber;
		
		switch (iinc.count)
		{
		case -1:
			spw.print(lineNumber, lvName);	
			spw.print(lineNumber, "--");
			break;
		case 1:
			spw.print(lineNumber, lvName);
			spw.print(lineNumber, "++");
			break;
		default:
			spw.print(lineNumber, lvName);
			if (iinc.count >= 0)
			{
				spw.print(lineNumber, " += ");
				spw.print(lineNumber, iinc.count);				
			}
			else
			{
				spw.print(lineNumber, " -= ");
				spw.print(lineNumber, -iinc.count);				
			}
		}
	}

	private void writePreInc(IncInstruction ii)
	{	
		int lineNumber = ii.lineNumber;
		
		switch (ii.count)
		{
		case -1:
			spw.print(lineNumber, "--");
			visit(ii.value);			
			break;
		case 1:
			spw.print(lineNumber, "++");
			visit(ii.value);
			break;
		default:
			visit(ii.value);				
			if (ii.count >= 0)
			{
				spw.print(lineNumber, " += ");
				spw.print(lineNumber, ii.count);				
			}
			else
			{
				spw.print(lineNumber, " -= ");
				spw.print(lineNumber, -ii.count);				
			}
		}
	}
		
	private void writePostInc(IncInstruction ii)
	{
		switch (ii.count)
		{
		case -1:
			visit(ii.value);			
			spw.print(ii.lineNumber, "--");
			break;
		case 1:
			visit(ii.value);
			spw.print(ii.lineNumber, "++");
			break;
		default:
			new RuntimeException("PostInc with value=" + ii.count)
				.printStackTrace();
		}
	}

	private void writeInvokeNewInstruction(InvokeNew in)
	{		
		String internalClassName = constants.getConstantClassName(in.classIndex);
		String prefix = 
			this.classFile.getThisClassName() + Constants.INTERNAL_INNER_SEPARATOR;
		ClassFile innerClassFile;
		
		if (internalClassName.startsWith(prefix))
			innerClassFile = this.classFile.getInnerClassFile(internalClassName);
		else
			innerClassFile = null;
		
		int lineNumber = in.lineNumber;
		
		spw.print(lineNumber, "new ");

		if (innerClassFile == null)
		{
			// Normal new invoke
			spw.print(lineNumber, SignatureWriter.WriteSimpleSignature(
					this.referenceMap, this.classFile, 
					'L' + internalClassName + ';'));
			writeArgs(in.lineNumber, 0, in.args);			
		}
		else if (innerClassFile.getInternalAnonymousClassName() == null)
		{
			// Inner class new invoke
			spw.print(lineNumber, SignatureWriter.WriteSimpleSignature(
					this.referenceMap, this.classFile, 
					'L' + internalClassName + ';'));
			int firstIndex = 
				((innerClassFile.access_flags & Constants.ACC_STATIC) != 0) ? 0 : 1;
			writeArgs(in.lineNumber, firstIndex, in.args);			
		}
		else 
		{
			// Anonymous new invoke
			spw.print(lineNumber, SignatureWriter.WriteSimpleSignature(
					this.referenceMap, this.classFile, 
					innerClassFile.getInternalAnonymousClassName()));
			int firstIndex = 
				((this.methodAccessFlags & Constants.ACC_STATIC) != 0) ? 0 : 1;
			writeArgs(in.lineNumber, firstIndex, in.args);
			
			ClassFileWriter.WriteBody(
				preferences, spw, referenceMap, innerClassFile);
		}		
	}
	
	private void writeInvokeNoStaticInstruction(InvokeNoStaticInstruction insi)
	{
		ConstantMethodref cmr = constants.getConstantMethodref(insi.index);
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cmr.name_and_type_index);
		boolean thisInvoke = false;
		
		if ((insi.objectref.opcode == ByteCodeConstants.ALOAD) &&
			(((ALoad)insi.objectref).index == 0))
		{
			ALoad aload = (ALoad)insi.objectref;
			LocalVariable lv = 
				this.localVariables.getLocalVariableWithIndexAndOffset(
						aload.index, aload.offset);
			
			if (lv != null)
			{
				String name = this.constants.getConstantUtf8(lv.name_index);				
				if (Constants.THIS_LOCAL_VARIABLE_NAME.equals(name))
					thisInvoke = true;				
			}
		}
		
		if (thisInvoke)
		{
			String methodName = constants.getConstantUtf8(cnat.name_index);
			if (this.keywordSet.contains(methodName))
				methodName = Constants.JD_METHOD_PREFIX + methodName;
			// Methode de la classe courante : elimination du prefix 'this.'				
			spw.print(insi.lineNumber, methodName);
		}	
		else
		{
			int lineNumber = insi.objectref.lineNumber;
				
			if (this.preferences.displayPrefixThis ||
				(insi.objectref.opcode != ByteCodeConstants.OUTERTHIS) ||
				needAPrefixForThisMethod(
					cnat.name_index, cnat.descriptor_index, 
					(GetStatic)insi.objectref))
			{
				visit(insi, insi.objectref);
				spw.print(lineNumber, '.');			
			}
			
			String methodName = constants.getConstantUtf8(cnat.name_index);
			if (this.keywordSet.contains(methodName))
				methodName = Constants.JD_METHOD_PREFIX + methodName;
			
			spw.print(lineNumber, methodName);
		}

		writeArgs(insi.lineNumber, 0, insi.args);
	}
	
	private boolean needAPrefixForThisMethod(
		int methodNameIndex, int methodDescriptorIndex, GetStatic getStatic)
	{
		if (this.classFile.getMethod(methodNameIndex, methodDescriptorIndex) != null)
		{
			// La classe courante contient une method ayant le meme nom et la 
			// meme signature
			return true;
		}

		ConstantFieldref cfr = 
			this.constants.getConstantFieldref(getStatic.index);
		String getStaticOuterClassName = 
			this.constants.getConstantClassName(cfr.class_index);
		String methodName = this.constants.getConstantUtf8(methodNameIndex);
		String methodDescriptor = 
			this.constants.getConstantUtf8(methodDescriptorIndex);
		
		ClassFile outerClassFile = this.classFile.getOuterClass();
		
		while (outerClassFile != null)
		{
			String outerClassName = outerClassFile.getThisClassName();
			if (outerClassName.equals(getStaticOuterClassName))
				break;
			
			if (outerClassFile.getMethod(methodName, methodDescriptor) != null)
			{
				// La classe englobante courante contient une method ayant le 
				// meme nom et la meme signature
				return true;
			}
			
			outerClassFile = outerClassFile.getOuterClass();
		}
		
		return false;
	}
	
	private void writeInvokespecial(InvokeNoStaticInstruction insi)
	{
		ConstantMethodref cmr = constants.getConstantMethodref(insi.index);
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cmr.name_and_type_index);
		boolean thisInvoke = false;
		
		if ((insi.objectref.opcode == ByteCodeConstants.ALOAD) &&
			(((ALoad)insi.objectref).index == 0))
		{
			ALoad aload = (ALoad)insi.objectref;
			LocalVariable lv = 
				this.localVariables.getLocalVariableWithIndexAndOffset(
						aload.index, aload.offset);
			
			if (lv != null)
			{
				String name = this.constants.getConstantUtf8(lv.name_index);				
				if (Constants.THIS_LOCAL_VARIABLE_NAME.equals(name))
					thisInvoke = true;				
			}
		}
		
		if (thisInvoke)
		{
			int lineNumber = insi.lineNumber;
			
			// Appel d'un constructeur?
			if (cnat.name_index == constants.instanceConstructorIndex)
			{
				if (cmr.class_index == classFile.getThisClassIndex())
				{
					// Appel d'un constructeur de la classe courante
					spw.print(lineNumber, "this");
					
					if ((this.classFile.access_flags & Constants.ACC_ENUM) == 0)
					{
						if (this.classFile.isAInnerClass() &&
							((this.classFile.access_flags & Constants.ACC_STATIC) == 0))
						{
							// inner class: firstIndex=1
							writeArgs(insi.lineNumber, 1, insi.args);
						}
						else
						{
							// class: firstIndex=0
							// static inner class: firstIndex=0
							writeArgs(insi.lineNumber, 0, insi.args);							
						}
					}
					else
					{
						// enum: firstIndex=2
						// static inner enum: firstIndex=2
						writeArgs(insi.lineNumber, 2, insi.args);	
					}
				}
				else
				{
					// Appel d'un constructeur de la classe mere
					spw.print(lineNumber, "super");
					
					if (this.classFile.isAInnerClass())
					{
						// inner class: firstIndex=1
						writeArgs(insi.lineNumber, 1, insi.args);
					}
					else
					{
						// class: firstIndex=0
						writeArgs(insi.lineNumber, 0, insi.args);
					}
				}
			}
			else
			{
				// Appel a une methode privee?
				Method method = this.classFile.getMethod(
					cnat.name_index, cnat.descriptor_index);
				
				if ((method == null) || 
					((method.access_flags & Constants.ACC_PRIVATE) == 0))
				{
					// Methode de la classe mere
					spw.print(lineNumber, "super");
					spw.print(lineNumber, '.');		
				}
				//else
				//{
				//	// Methode de la classe courante : elimination du prefix 'this.'	
				//}

				String methodName = constants.getConstantUtf8(cnat.name_index);
				if (this.keywordSet.contains(methodName))
					methodName = Constants.JD_METHOD_PREFIX + methodName;				
				spw.print(lineNumber, methodName);
				writeArgs(insi.lineNumber, 0, insi.args);
			}
		}
		else
		{
			int lineNumber = insi.objectref.lineNumber;
			
			visit(insi, insi.objectref);
			spw.print(lineNumber, '.');			

			String methodName = constants.getConstantUtf8(cnat.name_index);
			if (this.keywordSet.contains(methodName))
				methodName = Constants.JD_METHOD_PREFIX + methodName;
			
			spw.print(lineNumber, methodName);
			writeArgs(insi.lineNumber, 0, insi.args);
		}
	}
	
	private void writeInvokestatic(Invokestatic invokestatic)
	{
		int lineNumber = invokestatic.lineNumber;
		
		ConstantMethodref cmr = 
			constants.getConstantMethodref(invokestatic.index);
		
		if (classFile.getThisClassIndex() != cmr.class_index)
		{
			String signature = SignatureWriter.WriteSimpleSignature(
					this.referenceMap, classFile, 
					'L' + constants.getConstantClassName(cmr.class_index) + ';');
			
			if (signature.length() > 0)
			{
				spw.print(lineNumber, signature);
				spw.print(lineNumber, '.');
			}
		}				
		
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cmr.name_and_type_index);
		
		String methodName = constants.getConstantUtf8(cnat.name_index);
		if (this.keywordSet.contains(methodName))
			methodName = Constants.JD_METHOD_PREFIX + methodName;
		
		spw.print(lineNumber, methodName);		
		writeArgs(invokestatic.lineNumber, 0, invokestatic.args);
	}
	
	private void writeArgs(
		int lineNumber, int firstIndex, List<Instruction> args)
	{
		final int length = args.size();

		if (length > firstIndex)
		{
			spw.print(lineNumber, '(');

			Instruction instruction = args.get(firstIndex);
			lineNumber = instruction.lineNumber;
			
			visit(instruction);
			
			for (int i=firstIndex+1; i<length; i++)
			{
				spw.print(lineNumber, ", ");
				
				instruction = args.get(i);
				lineNumber = instruction.lineNumber;
				
				visit(instruction);
			}

			spw.print(lineNumber, ')');
		}
		else
		{
			spw.print(lineNumber, "()");			
		}
	}
	
	private void writeGetStatic(GetStatic getStatic)
	{
		int lineNumber = getStatic.lineNumber;
		
		ConstantFieldref cfr = 
			constants.getConstantFieldref(getStatic.index);
		
		if (cfr.class_index != classFile.getThisClassIndex())
		{
			String signature = SignatureWriter.WriteSimpleSignature(
				this.referenceMap, classFile, 
				'L' + constants.getConstantClassName(cfr.class_index) + ';');
			
			if (signature.length() > 0)
			{
				spw.print(lineNumber, signature);
				spw.print(lineNumber, '.');
			}
		}
		
		ConstantNameAndType cnat = constants.getConstantNameAndType(
				cfr.name_and_type_index);
		spw.print(lineNumber, constants.getConstantUtf8(cnat.name_index));
	}

	private void writeOuterThis(GetStatic getStatic)
	{
		int lineNumber = getStatic.lineNumber;
		
		ConstantFieldref cfr = 
			constants.getConstantFieldref(getStatic.index);
		
		if (cfr.class_index != classFile.getThisClassIndex())
		{
			String signature = SignatureWriter.WriteSimpleSignature(
				this.referenceMap, classFile, 
				'L' + constants.getConstantClassName(cfr.class_index) + ';');
			
			if (signature.length() > 0)
			{
				spw.print(lineNumber, signature);
				spw.print(lineNumber, '.');
			}
		}
		
		ConstantNameAndType cnat = constants.getConstantNameAndType(
				cfr.name_and_type_index);
		spw.print(lineNumber, constants.getConstantUtf8(cnat.name_index));
	}
	
	private void writeLcdInstruction(IndexInstruction ii)
	{
		int lineNumber = ii.lineNumber;
		
		// Dans les specs, LDC pointe vers une constante du pool. Lors de la
		// declaration d'enumeration, le byte code de la methode 
		// 'Enum.valueOf(Class<T> enumType, String name)' contient une
		// instruction LDC pointant un objet de type 'ConstantClass'.
		Constant cst = constants.get(ii.index);
		
		if (cst.tag == Constants.CONSTANT_Class)
		{
			// Exception a la regle
			ConstantClass cc = (ConstantClass)cst;
			String signature = 
				'L' + constants.getConstantUtf8(cc.name_index) + ';'; 
			
			spw.print(lineNumber, SignatureWriter.WriteSimpleSignature(
				this.referenceMap, classFile, signature));
			spw.print(lineNumber, ".class");
		}
		else
		{
			// Cas général
	    	ConstantValueWriter.Write(
	    		spw, lineNumber, constants, (ConstantValue)cst);
		}
	}
	
	private void writeLoadInstruction(LoadInstruction loadInstruction)
	{
		int lineNumber = loadInstruction.lineNumber;
		
		LocalVariable lv = this.localVariables.getLocalVariableWithIndexAndOffset(
				loadInstruction.index, loadInstruction.offset);
		
		if ((lv == null) || (lv.name_index <= 0))
		{
			// Error
			spw.print(lineNumber, "???");
		}
		else
		{
			int nameIndex = lv.name_index;

			if (nameIndex == -1)
			{
				// Error
				spw.print(lineNumber, "???");
			}
			else if (nameIndex == this.constants.thisLocalVariableNameIndex)
			{
				// Keyword
				spw.print(lineNumber, constants.getConstantUtf8(lv.name_index));
			}
			else
			{
				spw.print(lineNumber, constants.getConstantUtf8(lv.name_index));
			}
		}
	}
	
	private void writeMultiANewArray(MultiANewArray multiANewArray)
	{
		int lineNumber = multiANewArray.lineNumber;
		
		spw.print(lineNumber, "new ");
		
		String signature = 
			constants.getConstantClassName(multiANewArray.index);

		spw.print(lineNumber, SignatureWriter.WriteSimpleSignature(
				this.referenceMap, classFile, 
				SignatureAnalyzer.CutArrayDimensionPrefix(signature)));
		
		Instruction[] dimensions = multiANewArray.dimensions;
		for (int i=dimensions.length-1; i>=0; i--)
		{
			spw.print(lineNumber, '[');
			
			Instruction instruction = dimensions[i];
			lineNumber = instruction.lineNumber;
			visit(instruction);
			spw.print(lineNumber, ']');			
		}

		// Affichage des dimensions sans taille
		int dimensionCount = SignatureAnalyzer.GetArrayDimensionCount(signature);
		for (int i=dimensions.length; i<dimensionCount; i++)
			spw.print(lineNumber, "[]");
	}

	
	private void writePutStatic(PutStatic putStatic)
	{
		int lineNumber = putStatic.lineNumber;
		
		ConstantFieldref cfr = constants.getConstantFieldref(putStatic.index);
		
		if (cfr.class_index != classFile.getThisClassIndex())
		{
			String signature = SignatureWriter.WriteSimpleSignature(
				this.referenceMap, classFile, 
				'L' + constants.getConstantClassName(cfr.class_index) + ';');
			
			if (signature.length() > 0)
			{
				spw.print(lineNumber, signature);
				spw.print(lineNumber, '.');
			}
		}
		
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cfr.name_and_type_index);
		spw.print(lineNumber, constants.getConstantUtf8(cnat.name_index));
		spw.print(lineNumber, " = ");
		// Est-il necessaire de parenthéser l'expression ?
		// visit(putStatic, putStatic.valueref);
		visit(putStatic.valueref);
	}

	private void writeStoreInstruction(StoreInstruction storeInstruction)
	{
		int lineNumber = storeInstruction.lineNumber;
		
		LocalVariable lv = this.localVariables.getLocalVariableWithIndexAndOffset(
				storeInstruction.index, storeInstruction.offset);
		
		if ((lv == null) || (lv.name_index <= 0))
			spw.print(lineNumber, "???");		
		else
			spw.print(lineNumber, constants.getConstantUtf8(lv.name_index));
		
		spw.print(lineNumber, " = ");
		// Est-il necessaire de parenthéser l'expression ?
		// visit(storeInstruction, storeInstruction.valueref);
		visit(storeInstruction.valueref);
	}

	private void writeExceptionLoad(ExceptionLoad exceptionLoad)
	{		
		int lineNumber = exceptionLoad.lineNumber;
		
		if (exceptionLoad.exceptionNameIndex == 0)
		{
			spw.print(lineNumber, "finally");
		}
		else
		{
			LocalVariable lv = 
				this.localVariables.getLocalVariableWithIndexAndOffset(
					exceptionLoad.index, exceptionLoad.offset);
			
			if ((lv == null) || (lv.name_index == 0))
				spw.print(lineNumber, "???");
			else
				spw.print(lineNumber, constants.getConstantUtf8(lv.name_index));
		}
	}
	
	private void writeAssignmentInstruction(AssignmentInstruction ai)
	{
		int lineNumber = ai.lineNumber;
		
		ConstantFieldref cfr;
		ConstantNameAndType cnat;
		
		switch (ai.value1.opcode)
		{
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
		case ByteCodeConstants.STORE:
			StoreInstruction storeInstruction = (StoreInstruction)ai.value1;
			LocalVariable lv = this.localVariables.getLocalVariableWithIndexAndOffset(
					storeInstruction.index, storeInstruction.offset);
			
			if ((lv != null) && (lv.name_index > 0))
				spw.print(
					storeInstruction.lineNumber, constants.getConstantUtf8(lv.name_index));
			else
				spw.print(storeInstruction.lineNumber, "???");
			break;
		case ByteCodeConstants.ARRAYSTORE:
			ArrayStoreInstruction asi = (ArrayStoreInstruction)ai.value1;
			writeArray(asi, asi.arrayref, asi.indexref);
			break;
		case ByteCodeConstants.PUTFIELD:
			PutField putField = (PutField)ai.value1;
			cfr = constants.getConstantFieldref(putField.index);
			cnat = constants.getConstantNameAndType(cfr.name_and_type_index);

			lineNumber = putField.objectref.lineNumber;
			
			visit(putField, putField.objectref);
			spw.print(lineNumber, '.');	
			spw.print(lineNumber, constants.getConstantUtf8(cnat.name_index));
			break;
		case ByteCodeConstants.PUTSTATIC:
			PutStatic putStatic = (PutStatic)ai.value1;
			cfr = constants.getConstantFieldref(putStatic.index);
			cnat = constants.getConstantNameAndType(cfr.name_and_type_index);
			
			lineNumber = putStatic.lineNumber;
			
			String signature = SignatureWriter.WriteSimpleSignature(
				this.referenceMap, classFile, 
				'L' + constants.getConstantClassName(cfr.class_index) + ';');
			if (signature.length() > 0)
			{
				spw.print(lineNumber, signature);
				spw.print(lineNumber, '.');
			}
			spw.print(lineNumber, constants.getConstantUtf8(cnat.name_index));
			break;
		case ByteCodeConstants.DSTORE:
		case ByteCodeConstants.FSTORE:
		case ByteCodeConstants.LSTORE:
			new RuntimeException("instruction inattendue").printStackTrace();
		default:
			visit(ai.value1);	
		}
		
		spw.print(lineNumber, ' ');
		spw.print(lineNumber, ai.operator);
		spw.print(lineNumber, ' ');
		
		visit(ai, ai.value2);	
	}

	private void writeConvertInstruction(ConvertInstruction instruction)
	{
		int lineNumber = instruction.lineNumber;
		
		switch (instruction.signature.charAt(0))
		{
		case 'C':
			spw.print(lineNumber, "(char)");
			break;
		case 'B':
			spw.print(lineNumber, "(byte)");
			break;
		case 'S':
			spw.print(lineNumber, "(short)");
			break;
		case 'I':
			spw.print(lineNumber, "(int)");
			break;
		case 'L':
			spw.print(lineNumber, "(long)");
			break;
		case 'F':
			spw.print(lineNumber, "(float)");
			break;
		case 'D':
			spw.print(lineNumber, "(double)");
			break;
		}

		visit(instruction, instruction.value);
	}	

	/*
	 * Affichage des initialisations de tableaux associees aux instructions
	 * 'NEWARRAY' et 'ANEWARRAY' dans les affectations '?Store' et passees
	 * en parametres.
	 */
	private void WriteInitArrayInstruction(InitArrayInstruction iai)
	{		
		int lineNumber = iai.lineNumber;
		
		// Affichage des valeurs
		spw.print(lineNumber, "{ ");
		
		List<Instruction> values = iai.values;
		final int length = values.size();

		if (length > 0)
		{
			Instruction instruction = values.get(0);
			lineNumber = instruction.lineNumber;
			visit(instruction);
			
			for (int i=1; i<length; i++)
			{
				spw.print(lineNumber, ", ");				
				instruction = values.get(i);
				lineNumber = instruction.lineNumber;				
				visit(instruction);
			}
		}

		spw.print(lineNumber, " }");
	}
	
	/*
	 * Affichage des initialisations de tableaux associees aux instructions
	 * 'NEWARRAY' et 'ANEWARRAY' dans les affectations '?Store' et passees
	 * en parametres.
	 */
	private void WriteNewAndInitArrayInstruction(InitArrayInstruction iai)
	{		
		int lineNumber = iai.lineNumber;
		
		// Affichage de l'instruction 'new'
		spw.print(lineNumber, "new ");
		
		switch (iai.newArray.opcode)
		{
		case ByteCodeConstants.NEWARRAY:
			NewArray na = (NewArray)iai.newArray;
			spw.print(lineNumber, SignatureWriter.WriteSimpleSignature(
					this.referenceMap, classFile, 
					SignatureAnalyzer.GetSignatureFromType(na.type)));
			break;
		case ByteCodeConstants.ANEWARRAY:
			ANewArray ana = (ANewArray)iai.newArray;
			String signature = constants.getConstantClassName(ana.index);
			
			if (signature.charAt(0) != '[')
				signature = 'L' + signature + ';';
			
			spw.print(lineNumber, SignatureWriter.WriteSimpleSignature(
					this.referenceMap, classFile, signature));
			break;
		}

		spw.print(lineNumber, "[] ");
		
		WriteInitArrayInstruction(iai);
	}
}
