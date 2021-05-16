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
package jd.core.process.writer.visitor;

import java.util.HashSet;
import java.util.List;

import jd.core.loader.Loader;
import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.constant.Constant;
import jd.core.model.classfile.constant.ConstantClass;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.classfile.constant.ConstantUtf8;
import jd.core.model.classfile.constant.ConstantValue;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConstInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.ExceptionLoad;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.IInc;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.model.instruction.bytecode.instruction.Jsr;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
import jd.core.model.instruction.bytecode.instruction.LookupSwitch;
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.TableSwitch;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.InstructionPrinter;
import jd.core.process.writer.ConstantValueWriter;
import jd.core.process.writer.SignatureWriter;
import jd.core.util.SignatureUtil;
import jd.core.util.StringConstants;
import jd.core.util.StringUtil;
import jd.core.util.UtilConstants;


public class SourceWriterVisitor 
{
	protected Loader loader;
	protected InstructionPrinter printer;
	protected ReferenceMap referenceMap;
	protected HashSet<String> keywordSet;
	protected ConstantPool constants;
	protected LocalVariables localVariables;

	protected ClassFile classFile;
	protected int methodAccessFlags;
	protected int firstOffset;
	protected int lastOffset;
	protected int previousOffset;
	
	public SourceWriterVisitor(
		Loader loader, 
		InstructionPrinter printer, 
		ReferenceMap referenceMap,
		HashSet<String> keywordSet)
	{
		this.loader = loader;
		this.printer = printer;
		this.referenceMap = referenceMap;
		this.keywordSet = keywordSet;
	}
	
	/*
	 * Affichage de toutes les instructions avec 
	 *  - firstOffset <= offset 
	 *  - offset <= lastOffset
	 */
	public void init(
		ClassFile classFile, Method method, int firstOffset, int lastOffset)
	{
		this.classFile = classFile;
		this.firstOffset = firstOffset;
		this.lastOffset = lastOffset;
		this.previousOffset = 0;
		
		if ((classFile == null) || (method == null))
		{
			this.constants = null;
			this.methodAccessFlags = 0;
			this.localVariables = null;
		}
		else
		{
			this.constants = classFile.getConstantPool();
			this.methodAccessFlags = method.access_flags;
			this.localVariables = method.getLocalVariables();
		}
	}

	public int visit(Instruction instruction)
	{
		int lineNumber = instruction.lineNumber;
		
		if ((instruction.offset < this.firstOffset) || 
			(this.previousOffset > this.lastOffset))
			return lineNumber;
		
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			{
				lineNumber = visit(instruction, ((ArrayLength)instruction).arrayref);
				
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.print(lineNumber, '.');
					this.printer.printJavaWord("length");
				}
			}
			break;
		case ByteCodeConstants.ARRAYLOAD:
			{
				ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction; 
				lineNumber = writeArray(ali, ali.arrayref, ali.indexref);
			}
			break;
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			{
				ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
				lineNumber = writeArray(asi, asi.arrayref, asi.indexref);
				
				int nextOffset = this.previousOffset + 1;		
				if ((this.firstOffset <= nextOffset) && 
					(nextOffset <= this.lastOffset))
					this.printer.print(lineNumber, " = ");
				
				lineNumber = visit(asi, asi.valueref);
			}
			break;
		case ByteCodeConstants.ANEWARRAY:
			{
				ANewArray newArray = (ANewArray)instruction;
				Instruction dimension = newArray.dimension;
				
				String signature = constants.getConstantClassName(newArray.index);
				
				if (signature.charAt(0) != '[')
					signature = SignatureUtil.CreateTypeName(signature);
				
				String signatureWithoutArray = 
					SignatureUtil.CutArrayDimensionPrefix(signature);

				int nextOffset = this.previousOffset + 1;	
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.printKeyword(lineNumber, "new");
					this.printer.print(' ');
					
					SignatureWriter.WriteSignature(
						this.loader, this.printer, this.referenceMap, 
						this.classFile, signatureWithoutArray);
					
					this.printer.print(lineNumber, '[');
				}
				
				lineNumber = visit(dimension);

				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.print(lineNumber, ']');			
					
					int dimensionCount = 
						signature.length() - signatureWithoutArray.length();
						
					for (int i=dimensionCount; i>0; --i)
						this.printer.print(lineNumber, "[]");
				}
			}
			break;
		case ByteCodeConstants.ACONST_NULL:
			{
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
					this.printer.printKeyword(lineNumber, "null");
			}
			break;
		case ByteCodeConstants.ASSERT:
			{
				AssertInstruction ai = (AssertInstruction)instruction;
				
				int nextOffset = this.previousOffset + 1;	
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.printKeyword(lineNumber, "assert");
					this.printer.print(' ');
				}
				
				lineNumber = visit(ai, ai.test);

				if (ai.msg != null)
				{
					nextOffset = this.previousOffset + 1;	
					
					if ((this.firstOffset <= this.previousOffset) && 
						(ai.msg.offset <= this.lastOffset))
						this.printer.print(lineNumber, " : ");

					lineNumber = visit(ai, ai.msg);
				}
			}
			break;
		case ByteCodeConstants.ASSIGNMENT:
			lineNumber = writeAssignmentInstruction(
				(AssignmentInstruction)instruction);
			break;
		case ByteCodeConstants.ATHROW:
			{
				AThrow athrow = (AThrow)instruction;			
				int nextOffset = this.previousOffset + 1;	
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.printKeyword(lineNumber, "throw");
					this.printer.print(' ');
				}
				
				lineNumber = visit(athrow, athrow.value);
			}
			break;
		case ByteCodeConstants.UNARYOP:
			{
				UnaryOperatorInstruction ioi = 
					(UnaryOperatorInstruction)instruction;
				
				int nextOffset = this.previousOffset + 1;	
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
					this.printer.print(lineNumber, ioi.operator);
				
				lineNumber = visit(ioi, ioi.value);
			}
			break;
		case ByteCodeConstants.BINARYOP:
			lineNumber = writeBinaryOperatorInstruction(
				(BinaryOperatorInstruction)instruction);
			break;
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.SIPUSH:
		case ByteCodeConstants.ICONST:
			lineNumber = writeBIPush_SIPush_IConst((IConst)instruction);
			break;
		case ByteCodeConstants.LCONST:
			if ((this.firstOffset <= this.previousOffset) &&
				(instruction.offset <= this.lastOffset))
			{
				this.printer.printNumeric(lineNumber, 
					String.valueOf(((ConstInstruction)instruction).value) + 'L');
			}
			break;
		case ByteCodeConstants.FCONST:
			if ((this.firstOffset <= this.previousOffset) && 
				(instruction.offset <= this.lastOffset))
			{
				String value = 
					String.valueOf(((ConstInstruction)instruction).value);
				if (value.indexOf('.') == -1)
					value += ".0";
				this.printer.printNumeric(lineNumber, value + 'F');
			}
			break;
		case ByteCodeConstants.DCONST:
			if ((this.firstOffset <= this.previousOffset) && 
				(instruction.offset <= this.lastOffset))
			{
				String value = 
					String.valueOf(((ConstInstruction)instruction).value);
				if (value.indexOf('.') == -1)
					value += ".0";
				this.printer.printNumeric(lineNumber, value + 'D');
			}
			break;
		case ByteCodeConstants.CONVERT:
			lineNumber = writeConvertInstruction(
				(ConvertInstruction)instruction);
			break;
		case ByteCodeConstants.IMPLICITCONVERT:
			lineNumber = visit(((ConvertInstruction)instruction).value);
			break;
		case ByteCodeConstants.CHECKCAST:
			{
				CheckCast checkCast = (CheckCast)instruction;
				
				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.print(lineNumber, '(');
					
					String signature;
					Constant c = constants.get(checkCast.index);
					
					if (c.tag == ConstantConstant.CONSTANT_Utf8)
					{
						ConstantUtf8 cutf8 = (ConstantUtf8)c;
						signature = cutf8.bytes;
					}
					else
					{
						ConstantClass cc = (ConstantClass)c;
						signature = constants.getConstantUtf8(cc.name_index);
						if (signature.charAt(0) != '[')
							signature = SignatureUtil.CreateTypeName(signature);					
					}	
	
					SignatureWriter.WriteSignature(
						this.loader, this.printer, this.referenceMap, 
						this.classFile, signature);
	
					this.printer.print(')');
				}
				
				lineNumber = visit(checkCast, checkCast.objectref);	
			}
			break;
		case FastConstants.DECLARE:
			lineNumber = writeDeclaration((FastDeclaration)instruction);
			break;
		case ByteCodeConstants.DUPSTORE:
			{
				DupStore dupStore = (DupStore)instruction;
				
				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.print(
						lineNumber, StringConstants.TMP_LOCAL_VARIABLE_NAME);
					this.printer.print(instruction.offset);
					this.printer.print('_');
					this.printer.print(
						((DupStore)instruction).objectref.offset);
					this.printer.print(" = ");
				}
				
				lineNumber = visit(instruction, dupStore.objectref);
			}
			break;
		case ByteCodeConstants.DUPLOAD:
			{
				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.print(
						lineNumber, StringConstants.TMP_LOCAL_VARIABLE_NAME);
					this.printer.print(instruction.offset);
					this.printer.print('_');
					this.printer.print(
						((DupLoad)instruction).dupStore.objectref.offset);
				}
			}
			break;
		case FastConstants.ENUMVALUE:
			lineNumber = writeEnumValueInstruction((InvokeNew)instruction);
			break;
		case ByteCodeConstants.GETFIELD:
			writeGetField((GetField)instruction);
			break;
		case ByteCodeConstants.GETSTATIC:
			lineNumber = writeGetStatic((GetStatic)instruction);
			break;
		case ByteCodeConstants.OUTERTHIS:
			lineNumber = writeOuterThis((GetStatic)instruction);
			break;
		case ByteCodeConstants.GOTO:
			{			
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					Goto gotoInstruction = (Goto)instruction;
					this.printer.printKeyword(lineNumber, "goto");
					this.printer.print(' ');
					this.printer.print(
						lineNumber, gotoInstruction.GetJumpOffset());
				}
			}
			break;
		case FastConstants.GOTO_CONTINUE:
			if ((this.firstOffset <= this.previousOffset) && 
				(instruction.offset <= this.lastOffset))
			{
				this.printer.printKeyword(lineNumber, "continue");
			}				
			break;
		case FastConstants.GOTO_BREAK:
			if ((this.firstOffset <= this.previousOffset) && 
				(instruction.offset <= this.lastOffset))
			{
				this.printer.printKeyword(lineNumber, "break");
			}								
			break;
		case ByteCodeConstants.IF:
			lineNumber = writeIfTest((IfInstruction)instruction);
			break;
		case ByteCodeConstants.IFCMP:
			lineNumber = writeIfCmpTest((IfCmp)instruction);
			break;
		case ByteCodeConstants.IFXNULL:
			lineNumber = writeIfXNullTest((IfInstruction)instruction);
			break;
		case FastConstants.COMPLEXIF:
			lineNumber = writeComplexConditionalBranchInstructionTest(
				(ComplexConditionalBranchInstruction)instruction);
			break;
		case ByteCodeConstants.IINC:			
			lineNumber = writeIInc((IInc)instruction);
			break;
		case ByteCodeConstants.PREINC:			
			lineNumber = writePreInc((IncInstruction)instruction);
			break;
		case ByteCodeConstants.POSTINC:			
			lineNumber = writePostInc((IncInstruction)instruction);
			break;
		case ByteCodeConstants.INVOKENEW:
			lineNumber = writeInvokeNewInstruction((InvokeNew)instruction);
			break;
		case ByteCodeConstants.INSTANCEOF:
			{
				InstanceOf instanceOf = (InstanceOf)instruction;
				
				lineNumber = visit(instanceOf, instanceOf.objectref);
				
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.print(lineNumber, ' ');
					this.printer.printKeyword("instanceof");
					this.printer.print(' ');
					
					// reference to a class, array, or interface
					String signature =
						constants.getConstantClassName(instanceOf.index);

					if (signature.charAt(0) != '[')
						signature = SignatureUtil.CreateTypeName(signature);

					SignatureWriter.WriteSignature(
						this.loader, this.printer, this.referenceMap, 
						this.classFile, signature);
				}
			}
			break;
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKEVIRTUAL:
			lineNumber = writeInvokeNoStaticInstruction(
				(InvokeNoStaticInstruction)instruction);
			break;
		case ByteCodeConstants.INVOKESPECIAL:
			lineNumber = writeInvokespecial(
				(InvokeNoStaticInstruction)instruction);
			break;
		case ByteCodeConstants.INVOKESTATIC:
			lineNumber = writeInvokestatic((Invokestatic)instruction);
			break;
		case ByteCodeConstants.JSR:
			{
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.printKeyword(lineNumber, "jsr");
					this.printer.print(' ');
					this.printer.print((short)((Jsr)instruction).branch);
				}
			}
			break;
		case ByteCodeConstants.LDC:
		case ByteCodeConstants.LDC2_W:
			lineNumber = writeLcdInstruction((IndexInstruction)instruction);
			break;
		case ByteCodeConstants.LOAD:
		case ByteCodeConstants.ALOAD:
		case ByteCodeConstants.ILOAD:
			lineNumber = writeLoadInstruction((LoadInstruction)instruction);
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
			{
				LookupSwitch lookupSwitch = (LookupSwitch)instruction;

				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.printKeyword(lineNumber, "switch");
					this.printer.print(" (");
				}
				
				lineNumber = visit(lookupSwitch.key);

				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.print(lineNumber, ')');
				}
			}
			break;
		case ByteCodeConstants.TABLESWITCH:
			{
				TableSwitch tableSwitch = (TableSwitch)instruction;

				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.printKeyword(lineNumber, "switch");
					this.printer.print(" (");
				}
				
				lineNumber = visit(tableSwitch.key);
				
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.print(lineNumber, ')');
				}
			}
			break;
		case ByteCodeConstants.MONITORENTER:
			{
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.startOfError();
					this.printer.printKeyword(lineNumber, "monitorenter");
					this.printer.endOfError();
				}
			}
			break;
		case ByteCodeConstants.MONITOREXIT:
			{
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.startOfError();
					this.printer.printKeyword(lineNumber, "monitorexit");
					this.printer.endOfError();
				}
			}
			break;
		case ByteCodeConstants.MULTIANEWARRAY:
			lineNumber = writeMultiANewArray((MultiANewArray)instruction);
			break;
		case ByteCodeConstants.NEW:
			{
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.printKeyword(lineNumber, "new");
					this.printer.print(' ');
					this.printer.print(
						lineNumber, constants.getConstantClassName(
							((IndexInstruction)instruction).index));
				}
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			{
				NewArray newArray = (NewArray)instruction;			
				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.printKeyword(lineNumber, "new");
					this.printer.print(' ');
					SignatureWriter.WriteSignature(
						this.loader, this.printer, 
						this.referenceMap, this.classFile, 
						SignatureUtil.GetSignatureFromType(newArray.type));
					this.printer.print(lineNumber, '[');
				}
				
				lineNumber = visit(newArray.dimension);
				
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
					this.printer.print(lineNumber, ']');		
			}
			break;
		case ByteCodeConstants.POP:
			lineNumber = visit(instruction, ((Pop)instruction).objectref);
			break;
		case ByteCodeConstants.PUTFIELD:
			{
				PutField putField = (PutField)instruction;
				
				ConstantFieldref cfr = constants.getConstantFieldref(putField.index);
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cfr.name_and_type_index);
				
				boolean displayPrefix = false;
				
				if (this.localVariables.containsLocalVariableWithNameIndex(cnat.name_index))
				{
					switch (putField.objectref.opcode)
					{
					case ByteCodeConstants.ALOAD:
						if (((ALoad)putField.objectref).index == 0)
							displayPrefix = true;
						break;
					case ByteCodeConstants.OUTERTHIS:
						if (!needAPrefixForThisField(
								cnat.name_index, cnat.descriptor_index, 
								(GetStatic)putField.objectref))
							displayPrefix = true;
						break;
					}
				}
				
				if ((this.firstOffset <= this.previousOffset) && 
					(putField.objectref.offset <= this.lastOffset))
				{
					if (displayPrefix == false)
					{
						this.printer.addNewLinesAndPrefix(lineNumber);
						this.printer.startOfOptionalPrefix();
					}
					
					lineNumber = visit(putField, putField.objectref);
					this.printer.print(lineNumber, '.');
	
					if (displayPrefix == false)
					{
						this.printer.endOfOptionalPrefix();
					}
				}
				
				String fieldName = 
					constants.getConstantUtf8(cnat.name_index);
				if (this.keywordSet.contains(fieldName))
					fieldName = StringConstants.JD_FIELD_PREFIX + fieldName;
				
				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					String internalClassName = 
						this.constants.getConstantClassName(cfr.class_index);		
					String descriptor = 
						this.constants.getConstantUtf8(cnat.descriptor_index);
					this.printer.printField(
						lineNumber, internalClassName, fieldName, 
						descriptor, this.classFile.getThisClassName());			
					this.printer.print(" = ");
				}
				
				lineNumber = visit(putField, putField.valueref);
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			lineNumber = writePutStatic((PutStatic)instruction);
			break;
		case ByteCodeConstants.RET:
			{
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.startOfError();
					this.printer.printKeyword(lineNumber, "ret");
					this.printer.endOfError();
				}
			}
			break;
		case ByteCodeConstants.RETURN:
			{
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
					this.printer.printKeyword(lineNumber, "return");
			}
			break;
		case ByteCodeConstants.XRETURN:
			{
				ReturnInstruction ri = (ReturnInstruction)instruction;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.printKeyword(ri.lineNumber, "return");
					this.printer.print(' ');
				}
				
				lineNumber = visit(ri.valueref);
			}
			break;
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			lineNumber = writeStoreInstruction((StoreInstruction)instruction);
			break;
		case ByteCodeConstants.EXCEPTIONLOAD:
			lineNumber = writeExceptionLoad((ExceptionLoad)instruction);
			break;
		case ByteCodeConstants.RETURNADDRESSLOAD:
			{
				if ((this.firstOffset <= this.previousOffset) && 
					(instruction.offset <= this.lastOffset))
				{
					this.printer.startOfError();
					this.printer.printKeyword(lineNumber, "returnAddress");
					this.printer.endOfError();
				}
			}
			break;
		case ByteCodeConstants.TERNARYOPSTORE:
			{
				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.startOfError();
					this.printer.print(lineNumber, "tmpTernaryOp");
					this.printer.print(lineNumber, " = ");
					this.printer.endOfError();
				}

				lineNumber = visit(
					instruction, ((TernaryOpStore)instruction).objectref);
			}
			break;
		case FastConstants.TERNARYOP:
			{
				TernaryOperator tp = (TernaryOperator)instruction;
				
				lineNumber = visit(tp.test);
				
				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
					this.printer.print(lineNumber, " ? ");
				
				lineNumber = visit(tp, tp.value1);
				
				nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
					this.printer.print(lineNumber, " : ");
				
				lineNumber = visit(tp, tp.value2);
			}
			break;
		case FastConstants.INITARRAY:
			lineNumber = WriteInitArrayInstruction(
				(InitArrayInstruction)instruction);
			break;
		case FastConstants.NEWANDINITARRAY:
			lineNumber = WriteNewAndInitArrayInstruction(
				(InitArrayInstruction)instruction);
			break;
		case ByteCodeConstants.NOP:
			break;
		default:
			System.err.println(
					"Can not write code for " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
		
		this.previousOffset = instruction.offset;
		
		return lineNumber;
	}
	
	protected int visit(Instruction parent, Instruction child)
	{
		return visit(parent.getPriority(), child);
	}
	
	protected int visit(int parentPriority, Instruction child)
	{
		if (parentPriority < child.getPriority())
		{
			int nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(child.lineNumber, '(');
			
			int lineNumber = visit(child);
			
			nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(lineNumber, ')');			
			
			return lineNumber;
		}
		else
		{
			return visit(child);
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

	private int writeBIPush_SIPush_IConst(IConst iconst)
	{
		int lineNumber = iconst.lineNumber;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(iconst.offset <= this.lastOffset))
		{
			int value = iconst.value;
			String signature = iconst.getSignature();
			
			if ("S".equals(signature))
			{
		    	if (((short)value) == Short.MIN_VALUE)
		    	{
		    		writeBIPush_SIPush_IConst(
		    			lineNumber, "java/lang/Short", "MIN_VALUE", "S");
		    	}
		    	else if (((short)value) == Short.MAX_VALUE)
		    	{
		    		writeBIPush_SIPush_IConst(
		    			lineNumber, "java/lang/Short", "MAX_VALUE", "S");
		    	}
		    	else
		    	{
					this.printer.printNumeric(lineNumber, String.valueOf(value));
		    	}				
			}
			else if ("B".equals(signature))
			{
		    	if (value == Byte.MIN_VALUE)
		    	{
		    		writeBIPush_SIPush_IConst(
		    			lineNumber, "java/lang/Byte", "MIN_VALUE", "B");
		    	}
		    	else if (value == Byte.MAX_VALUE)
		    	{
		    		writeBIPush_SIPush_IConst(
		    			lineNumber, "java/lang/Byte", "MAX_VALUE", "B");
		    	}
		    	else
		    	{
					this.printer.printNumeric(lineNumber, String.valueOf(value));
		    	}
			}		
			else if ("C".equals(signature))
			{
				String escapedString =
					StringUtil.EscapeCharAndAppendApostrophe((char)value);
				String scopeInternalName =  this.classFile.getThisClassName();
				this.printer.printString(
					lineNumber, escapedString, scopeInternalName);
			}
			else if ("Z".equals(signature))
			{			
				this.printer.printKeyword(
					lineNumber, (value == 0) ? "false" : "true");
			}
			else
			{
				this.printer.printNumeric(lineNumber, String.valueOf(value));
			}	
		}
		
		return lineNumber;
	}
	
	private void writeBIPush_SIPush_IConst(
		int lineNumber, String internalTypeName, String name, String descriptor)
	{
		String className = SignatureWriter.InternalClassNameToClassName(
			this.loader, this.referenceMap, this.classFile, internalTypeName);
		String scopeInternalName = this.classFile.getThisClassName();
		this.printer.printType(
			lineNumber, internalTypeName, className, scopeInternalName);
		this.printer.print(lineNumber, '.');
		this.printer.printStaticField(
			lineNumber, internalTypeName, name, descriptor, scopeInternalName);		
	}	
	
	private int writeArray(
		Instruction parent, Instruction arrayref, Instruction indexref)
	{
		int lineNumber = visit(parent, arrayref);
		
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
			this.printer.print(lineNumber, '[');
		
		lineNumber = visit(parent, indexref);
		
		if ((this.firstOffset <= this.previousOffset) && 
			(parent.offset <= this.lastOffset))
			this.printer.print(lineNumber, ']');
		
		return lineNumber;
	}

	/* +, -, *, /, %, <<, >>, >>>, &, |, ^ */
	private int writeBinaryOperatorInstruction(BinaryOperatorInstruction boi)
	{
		int lineNumber = boi.value1.lineNumber;
		
		if (boi.operator.length() == 1)
		{
			switch (boi.operator.charAt(0))
			{
			case '&': case '|': case '^':
				{
					// Binary operators
					lineNumber = 
						writeBinaryOperatorParameterInHexaOrBoolean(boi, boi.value1);
					
					int nextOffset = this.previousOffset + 1;
					
					if ((this.firstOffset <= this.previousOffset) && 
						(nextOffset <= this.lastOffset))
					{
						this.printer.print(lineNumber, ' ');
						this.printer.print(lineNumber, boi.operator);
						this.printer.print(lineNumber, ' ');
					}

					return 
						writeBinaryOperatorParameterInHexaOrBoolean(boi, boi.value2);	
				}
			}
		}
		
		// Other operators
		lineNumber = visit(boi, boi.value1);		

		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
		{
			this.printer.print(lineNumber, ' ');
			this.printer.print(lineNumber, boi.operator);
			this.printer.print(lineNumber, ' ');
		}
		
		if (boi.getPriority() <= boi.value2.getPriority())
		{
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(lineNumber, '(');
			
			lineNumber = visit(boi.value2);
			
			if ((this.firstOffset <= this.previousOffset) && 
				(boi.offset <= this.lastOffset))
				this.printer.print(lineNumber, ')');	
			
			return lineNumber;		
		}
		else
		{
			return visit(boi.value2);
		}		
	}
	
	protected int writeBinaryOperatorParameterInHexaOrBoolean(
		Instruction parent, Instruction child)
	{
		if (parent.getPriority() < child.getPriority())
		{
			int nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(child.lineNumber, '(');
		
			int lineNumber = writeBinaryOperatorParameterInHexaOrBoolean(child);
			
			nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(lineNumber, ')');	
			
			return lineNumber;
		}
		else
		{
			return writeBinaryOperatorParameterInHexaOrBoolean(child);
		}
	}
	
	private int writeBinaryOperatorParameterInHexaOrBoolean(Instruction value)
	{
		int lineNumber = value.lineNumber;
				
		if ((this.firstOffset <= this.previousOffset) && 
			(value.offset <= this.lastOffset))
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
							this.printer.printKeyword(lineNumber, "false");
						else
							this.printer.printKeyword(lineNumber, "true");
					}
					else
					{
						this.printer.printNumeric(
							lineNumber, 
							"0x" + Integer.toHexString(iconst.value).toUpperCase());
					}
				}
				break;
			case ByteCodeConstants.LDC:
			case ByteCodeConstants.LDC2_W:
				this.printer.addNewLinesAndPrefix(lineNumber);
				Constant cst = constants.get( ((IndexInstruction)value).index );
				ConstantValueWriter.WriteHexa(
					this.loader, this.printer, this.referenceMap, 
					this.classFile, (ConstantValue)cst);
				break;
			default:
				lineNumber = visit(value);
				break;
			}	
		}
		
		return lineNumber;		
	}
	
	protected int writeIfTest(IfInstruction ifInstruction)
	{
		String signature = 
			ifInstruction.value.getReturnedSignature(constants, localVariables);
		
		if ((signature != null) && (signature.charAt(0) == 'Z'))
		{
			switch (ifInstruction.cmp)
			{
			case ByteCodeConstants.CMP_EQ:
			case ByteCodeConstants.CMP_LE:
			case ByteCodeConstants.CMP_GE:
				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
					this.printer.print(ifInstruction.lineNumber, "!");
			}

			return visit(2, ifInstruction.value);					
				
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
			int lineNumber = visit(6, ifInstruction.value);	
			
			if ((this.firstOffset <= this.previousOffset) && 
				(ifInstruction.offset <= this.lastOffset))
			{
				this.printer.print(' ');
				this.printer.print(ByteCodeConstants.CMP_NAMES[ifInstruction.cmp]);
				this.printer.print(' ');
				this.printer.printNumeric("0");
			}
			
			return lineNumber;
		}
	}

	protected int writeIfCmpTest(IfCmp ifCmpInstruction)
	{
		int lineNumber = visit(6, ifCmpInstruction.value1);
		
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
		{
			this.printer.print(lineNumber, ' ');
			this.printer.print(ByteCodeConstants.CMP_NAMES[ifCmpInstruction.cmp]);
			this.printer.print(' ');
		}
		
		return visit(6, ifCmpInstruction.value2);
	}

	protected int writeIfXNullTest(IfInstruction ifXNull)
	{
		int lineNumber = visit(6, ifXNull.value);
		
		if ((this.firstOffset <= this.previousOffset) && 
			(ifXNull.offset <= this.lastOffset))
		{
			this.printer.print(lineNumber, ' ');
			this.printer.print(ByteCodeConstants.CMP_NAMES[ifXNull.cmp]);
			this.printer.print(' ');
			this.printer.printKeyword("null");
		}
		
		return lineNumber;
	}
	
	protected int writeComplexConditionalBranchInstructionTest(
		ComplexConditionalBranchInstruction ccbi)
	{
		List<Instruction> branchList = ccbi.instructions;
		int lenght = branchList.size();

		if (lenght > 1)
		{
			String operator = 
				(ccbi.cmp==FastConstants.CMP_AND) ? " && " : " || ";
			Instruction instruction = branchList.get(0);
			int lineNumber = instruction.lineNumber;
			
			int nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(lineNumber, '(');
			
			lineNumber = visit(instruction);
			
			nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(lineNumber, ')');

			for (int i=1; i<lenght; i++)
			{
				instruction = branchList.get(i);
				
				nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.print(lineNumber, operator);
					this.printer.print(instruction.lineNumber, '(');
				}
				
				lineNumber = visit(instruction);
								
				if ((this.firstOffset <= this.previousOffset) && 
					(ccbi.offset <= this.lastOffset))
					this.printer.print(lineNumber, ')');
			}
			
			return lineNumber;
		}
		else if (lenght > 0)
		{
			return visit(branchList.get(0));
		}
		else
		{
			return Instruction.UNKNOWN_LINE_NUMBER;
		}
	}

	private int writeIInc(IInc iinc)
	{
		int lineNumber = iinc.lineNumber;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(iinc.offset <= this.lastOffset))
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
				//new RuntimeException("local variable not found")
				//	.printStackTrace();
				this.printer.startOfError();
				this.printer.print(lineNumber, "???");
				this.printer.endOfError();
			}
			else
			{
				this.printer.print(lineNumber, lvName);				
			}
			
			switch (iinc.count)
			{
			case -1:
				this.printer.print(lineNumber, "--");
				break;
			case 1:
				this.printer.print(lineNumber, "++");
				break;
			default:
				if (iinc.count >= 0)
				{
					this.printer.print(lineNumber, " += ");
					this.printer.printNumeric(lineNumber, String.valueOf(iinc.count));				
				}
				else
				{
					this.printer.print(lineNumber, " -= ");
					this.printer.printNumeric(lineNumber, String.valueOf(-iinc.count));				
				}
			}
		}
		
		return lineNumber;
	}

	private int writePreInc(IncInstruction ii)
	{	
		int lineNumber = ii.lineNumber;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(ii.offset <= this.lastOffset))
		{
			switch (ii.count)
			{
			case -1:
				this.printer.print(lineNumber, "--");
				lineNumber = visit(ii.value);	
				break;
			case 1:
				this.printer.print(lineNumber, "++");
				lineNumber = visit(ii.value);
				break;
			default:
				lineNumber = visit(ii.value);	
				
				if (ii.count >= 0)
				{
					this.printer.print(lineNumber, " += ");
					this.printer.printNumeric(lineNumber, String.valueOf(ii.count));				
				}
				else
				{
					this.printer.print(lineNumber, " -= ");
					this.printer.printNumeric(lineNumber, String.valueOf(-ii.count));				
				}
				break;
			}
		}
		
		return lineNumber;
	}
		
	private int writePostInc(IncInstruction ii)
	{
		int lineNumber = ii.lineNumber;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(ii.offset <= this.lastOffset))
		{
			switch (ii.count)
			{
			case -1:
				lineNumber = visit(ii.value);			
				this.printer.print(lineNumber, "--");
				break;
			case 1:
				lineNumber = visit(ii.value);
				this.printer.print(lineNumber, "++");
				break;
			default:
				new RuntimeException("PostInc with value=" + ii.count)
					.printStackTrace();
			}
		}
		
		return lineNumber;
	}

	private int writeInvokeNewInstruction(InvokeNew in)
	{		
		ConstantMethodref cmr = this.constants.getConstantMethodref(in.index);	
		String internalClassName = 
			this.constants.getConstantClassName(cmr.class_index);
		String prefix = 
			this.classFile.getThisClassName() + 
			StringConstants.INTERNAL_INNER_SEPARATOR;
		ClassFile innerClassFile;
		
		if (internalClassName.startsWith(prefix))
			innerClassFile = this.classFile.getInnerClassFile(internalClassName);
		else
			innerClassFile = null;
		
		int lineNumber = in.lineNumber;
		int firstIndex;
		int length = in.args.size();

		ConstantNameAndType cnat = 
			this.constants.getConstantNameAndType(cmr.name_and_type_index);
		String constructorDescriptor = 
			this.constants.getConstantUtf8(cnat.descriptor_index);
		
		if (innerClassFile == null)
		{
			// Normal new invoke
			firstIndex = 0;	
		}
		else if (innerClassFile.getInternalAnonymousClassName() == null)
		{
			// Inner class new invoke
			firstIndex = computeFirstIndex(innerClassFile.access_flags, in);
		}
		else 
		{
			// Anonymous new invoke		
			firstIndex = computeFirstIndex(this.methodAccessFlags, in);
			// Search parameter count of super constructor
			String constructorName = 
				this.constants.getConstantUtf8(cnat.name_index);
			Method constructor =
				innerClassFile.getMethod(constructorName, constructorDescriptor);
			if (constructor != null)
			{
				length = 
					firstIndex + constructor.getSuperConstructorParameterCount();				
				assert length <= in.args.size();
			}		
		}

		if (this.firstOffset <= this.previousOffset)
		{			
			this.printer.printKeyword(lineNumber, "new");
			this.printer.print(' ');
	
			if (innerClassFile == null)
			{
				// Normal new invoke
				SignatureWriter.WriteConstructor(
					this.loader, this.printer, this.referenceMap, 
					this.classFile, 
					SignatureUtil.CreateTypeName(internalClassName),
					constructorDescriptor);
				//writeArgs(in.lineNumber, 0, in.args);			
			}
			else if (innerClassFile.getInternalAnonymousClassName() == null)
			{
				// Inner class new invoke
				SignatureWriter.WriteConstructor(
					this.loader, this.printer, this.referenceMap, 
					this.classFile, 
					SignatureUtil.CreateTypeName(internalClassName),
					constructorDescriptor);			
			}
			else 
			{
				// Anonymous new invoke
				SignatureWriter.WriteConstructor(
					this.loader, this.printer, this.referenceMap, this.classFile, 
					SignatureUtil.CreateTypeName(innerClassFile.getInternalAnonymousClassName()),
					constructorDescriptor);
			}		
		}

		return writeArgs(in.lineNumber, firstIndex, length, in.args);
	}
	
	private int computeFirstIndex(int accessFlags, InvokeNew in)
	{
		if (((accessFlags & ClassFileConstants.ACC_STATIC) == 0) &&
			(in.args.size() > 0))
		{
			Instruction arg0 = in.args.get(0);
			if ((arg0.opcode == ByteCodeConstants.ALOAD) && 
				(((ALoad)arg0).index == 0))
			{
				return 1;
			}
			else
			{
				return 0;	
			}
		}
		else
		{
			return 0;	
		}		
	}
	
	private int writeEnumValueInstruction(InvokeNew in)
	{	
		int lineNumber = in.lineNumber;

		ConstantFieldref cfr = 
			constants.getConstantFieldref(in.enumValueFieldRefIndex);		
		ConstantNameAndType cnat = constants.getConstantNameAndType(
			cfr.name_and_type_index);
		
		String internalClassName = classFile.getThisClassName();
		String name = constants.getConstantUtf8(cnat.name_index);
		String descriptor = constants.getConstantUtf8(cnat.descriptor_index);
		
		this.printer.addNewLinesAndPrefix(lineNumber);
		this.printer.printStaticFieldDeclaration(
			internalClassName, name, descriptor);
		
		if (in.args.size() > 2)
			lineNumber = writeArgs(lineNumber, 2, in.args.size(), in.args);
		
		return lineNumber;
	}

	private int writeGetField(GetField getField)
	{		
		int lineNumber = getField.lineNumber;	
		ConstantFieldref cfr = 
			constants.getConstantFieldref(getField.index);
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cfr.name_and_type_index);
		Field field = 
			this.classFile.getField(cnat.name_index, cnat.descriptor_index);
		
		if ((field != null) && 
			(field.outerMethodLocalVariableNameIndex != UtilConstants.INVALID_INDEX))
		{
			// Specificite des classes anonymes : affichage du nom du champs de
			// la methode englobante plutot que le nom du champs
			if ((this.firstOffset <= this.previousOffset) && 
				(getField.offset <= this.lastOffset))
			{
				String internalClassName = 
					this.constants.getConstantClassName(cfr.class_index);		
				String fieldName = this.constants.getConstantUtf8(
					field.outerMethodLocalVariableNameIndex);
				if (this.keywordSet.contains(fieldName))
					fieldName = StringConstants.JD_FIELD_PREFIX + fieldName;
				String descriptor = 
					this.constants.getConstantUtf8(cnat.descriptor_index);
				this.printer.printField(
					lineNumber, internalClassName, fieldName, 
					descriptor, this.classFile.getThisClassName());				
			}
		}
		else
		{
			// Cas normal
			boolean displayPrefix = false;
				
			if (this.localVariables.containsLocalVariableWithNameIndex(cnat.name_index))
			{
				switch (getField.objectref.opcode)
				{
				case ByteCodeConstants.ALOAD:
					if (((ALoad)getField.objectref).index == 0)
						displayPrefix = true;
					break;
				case ByteCodeConstants.OUTERTHIS:
					if (!needAPrefixForThisField(
							cnat.name_index, cnat.descriptor_index, 
							(GetStatic)getField.objectref))
						displayPrefix = true;
					break;
				}
			}
			
			if ((this.firstOffset <= this.previousOffset) && 
				(getField.objectref.offset <= this.lastOffset))
			{
				if (displayPrefix == false)
				{
					this.printer.addNewLinesAndPrefix(lineNumber);
					this.printer.startOfOptionalPrefix();
				}
				
				lineNumber = visit(getField, getField.objectref);		
				this.printer.print(lineNumber, '.');
				
				if (displayPrefix == false)
				{
					this.printer.endOfOptionalPrefix();
				}
			}
			
			if ((this.firstOffset <= this.previousOffset) && 
				(getField.offset <= this.lastOffset))
			{
				String internalClassName = 
					this.constants.getConstantClassName(cfr.class_index);		
				String fieldName = 
					this.constants.getConstantUtf8(cnat.name_index);
				if (this.keywordSet.contains(fieldName))
					fieldName = StringConstants.JD_FIELD_PREFIX + fieldName;
				String descriptor = 
					this.constants.getConstantUtf8(cnat.descriptor_index);
				this.printer.printField(
					lineNumber, internalClassName, fieldName, 
					descriptor, this.classFile.getThisClassName());							
			}
		}
		
		return lineNumber;
	}
		
	private int writeInvokeNoStaticInstruction(InvokeNoStaticInstruction insi)
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
				if (StringConstants.THIS_LOCAL_VARIABLE_NAME.equals(name))
					thisInvoke = true;				
			}
		}
		
		if (thisInvoke)
		{
			int nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
			{
				String internalClassName = 
					this.constants.getConstantClassName(cmr.class_index);
				String methodName = constants.getConstantUtf8(cnat.name_index);
				if (this.keywordSet.contains(methodName))
					methodName = StringConstants.JD_METHOD_PREFIX + methodName;
				String descriptor = 
					this.constants.getConstantUtf8(cnat.descriptor_index);
				// Methode de la classe courante : elimination du prefix 'this.'				
				this.printer.printMethod(
					insi.lineNumber, internalClassName, methodName, 
					descriptor, this.classFile.getThisClassName());
			}
		}	
		else
		{
			boolean displayPrefix = 
				((insi.objectref.opcode != ByteCodeConstants.OUTERTHIS) ||
				needAPrefixForThisMethod(
					cnat.name_index, cnat.descriptor_index, 
					(GetStatic)insi.objectref));
			
			int lineNumber = insi.objectref.lineNumber;	
			
			if (displayPrefix == false)
			{
				this.printer.addNewLinesAndPrefix(lineNumber);
				this.printer.startOfOptionalPrefix();
			}
			
			visit(insi, insi.objectref);

			int nextOffset = this.previousOffset + 1;
			lineNumber = insi.lineNumber;			
		
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
			{
				this.printer.print(lineNumber, '.');
			}
			
			if (displayPrefix == false)
			{
				this.printer.endOfOptionalPrefix();
			}
			
			nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
			{
				String internalClassName = 
					this.constants.getConstantClassName(cmr.class_index);
				String methodName = constants.getConstantUtf8(cnat.name_index);
				if (this.keywordSet.contains(methodName))
					methodName = StringConstants.JD_METHOD_PREFIX + methodName;
				String descriptor = 
					this.constants.getConstantUtf8(cnat.descriptor_index);
				this.printer.printMethod(
					lineNumber, internalClassName, methodName, 
					descriptor, this.classFile.getThisClassName());
			}
		}

		return writeArgs(insi.lineNumber, 0, insi.args.size(), insi.args);
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
	
	private int writeInvokespecial(InvokeNoStaticInstruction insi)
	{
		ConstantMethodref cmr = constants.getConstantMethodref(insi.index);
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cmr.name_and_type_index);
		boolean thisInvoke = false;
		int firstIndex;
			
		if ((insi.objectref.opcode == ByteCodeConstants.ALOAD) &&
			(((ALoad)insi.objectref).index == 0))
		{
			ALoad aload = (ALoad)insi.objectref;
			LocalVariable lv = 
				this.localVariables.getLocalVariableWithIndexAndOffset(
						aload.index, aload.offset);
			
			if ((lv != null) && 
				(lv.name_index == this.constants.thisLocalVariableNameIndex))
			{
				thisInvoke = true;				
			}
		}
		
		if (thisInvoke)
		{
			// Appel d'un constructeur?
			if (cnat.name_index == constants.instanceConstructorIndex)
			{
				if (cmr.class_index == classFile.getThisClassIndex())
				{
					// Appel d'un constructeur de la classe courante					
					if ((this.classFile.access_flags & ClassFileConstants.ACC_ENUM) == 0)
					{
						if (this.classFile.isAInnerClass() &&
							((this.classFile.access_flags & ClassFileConstants.ACC_STATIC) == 0))
						{
							// inner class: firstIndex=1
							firstIndex = 1;
						}
						else
						{
							// class: firstIndex=0
							// static inner class: firstIndex=0	
							firstIndex = 0;
						}
					}
					else
					{
						// enum: firstIndex=2
						// static inner enum: firstIndex=2
						firstIndex = 2;
					}
				}
				else
				{
					// Appel d'un constructeur de la classe mere
					if (this.classFile.isAInnerClass())
					{
						// inner class: firstIndex=1
						firstIndex = 1;
					}
					else
					{
						// class: firstIndex=0
						firstIndex = 0;
					}
				}
			}
			else
			{
				// Appel a une methode privee?
				firstIndex = 0;
			}
		}
		else
		{
			firstIndex = 0;
		}
		
		if (thisInvoke)
		{
			int nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
			{
				int lineNumber = insi.lineNumber;
				
				// Appel d'un constructeur?
				if (cnat.name_index == constants.instanceConstructorIndex)
				{
					if (cmr.class_index == classFile.getThisClassIndex())
					{
						// Appel d'un constructeur de la classe courante
						this.printer.printKeyword(lineNumber, "this");
					}
					else
					{
						// Appel d'un constructeur de la classe mere
						this.printer.printKeyword(lineNumber, "super");
					}
				}
				else
				{
					// Appel a une methode privee?
					Method method = this.classFile.getMethod(
						cnat.name_index, cnat.descriptor_index);
					
					if ((method == null) || 
						((method.access_flags & ClassFileConstants.ACC_PRIVATE) == 0))
					{
						// Methode de la classe mere
						this.printer.printKeyword(lineNumber, "super");
						this.printer.print(lineNumber, '.');		
					}
					//else
					//{
					//	// Methode de la classe courante : elimination du prefix 'this.'	
					//}
	
					String internalClassName = 
						this.constants.getConstantClassName(cmr.class_index);
					String methodName = constants.getConstantUtf8(cnat.name_index);
					if (this.keywordSet.contains(methodName))
						methodName = StringConstants.JD_METHOD_PREFIX + methodName;	
					String descriptor = 
						this.constants.getConstantUtf8(cnat.descriptor_index);
					this.printer.printMethod(
						lineNumber, internalClassName, methodName, 
						descriptor, this.classFile.getThisClassName());
				}
			}
		}
		else
		{
			int lineNumber = insi.lineNumber;
					
			visit(insi, insi.objectref);
			
			int nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
			{
				this.printer.print(lineNumber, '.');			

				String internalClassName = 
					this.constants.getConstantClassName(cmr.class_index);
				String methodName = constants.getConstantUtf8(cnat.name_index);
				if (this.keywordSet.contains(methodName))
					methodName = StringConstants.JD_METHOD_PREFIX + methodName;
				String descriptor = 
					this.constants.getConstantUtf8(cnat.descriptor_index);
				
				this.printer.printMethod(
					internalClassName, methodName, 
					descriptor, this.classFile.getThisClassName());
			}
		}
		
		return writeArgs(
			insi.lineNumber, firstIndex, insi.args.size(), insi.args);
	}
	
	private int writeInvokestatic(Invokestatic invokestatic)
	{
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
		{
			int lineNumber = invokestatic.lineNumber;
			
			ConstantMethodref cmr = 
				constants.getConstantMethodref(invokestatic.index);
			
			String internalClassName = 
				this.constants.getConstantClassName(cmr.class_index);
					
			if (classFile.getThisClassIndex() != cmr.class_index)
			{
				this.printer.addNewLinesAndPrefix(lineNumber);
				int length = SignatureWriter.WriteSignature(
					this.loader, this.printer, 
					this.referenceMap, this.classFile, 
					SignatureUtil.CreateTypeName(constants.getConstantClassName(cmr.class_index)));
				
				if (length > 0)
				{
					this.printer.print('.');
				}
			}				
			
			ConstantNameAndType cnat = 
				constants.getConstantNameAndType(cmr.name_and_type_index);
			
			String methodName = constants.getConstantUtf8(cnat.name_index);
			if (this.keywordSet.contains(methodName))
				methodName = StringConstants.JD_METHOD_PREFIX + methodName;
			String descriptor = 
					this.constants.getConstantUtf8(cnat.descriptor_index);	
			
			this.printer.printStaticMethod(
				lineNumber, internalClassName, methodName, descriptor, 
				this.classFile.getThisClassName());
		}
		
		return writeArgs(
			invokestatic.lineNumber, 0, 
			invokestatic.args.size(), invokestatic.args);
	}
	
	private int writeArgs(
		int lineNumber, int firstIndex, int length, List<Instruction> args)
	{
		if (length > firstIndex)
		{
			int nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(lineNumber, '(');

			lineNumber = visit(args.get(firstIndex));
			
			for (int i=firstIndex+1; i<length; i++)
			{
				nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
					this.printer.print(lineNumber, ", ");
				
				lineNumber = visit(args.get(i));
			}
			
			nextOffset = this.previousOffset + 1;	
			
			if ((this.firstOffset <= this.previousOffset) &&
				(nextOffset <= this.lastOffset))
				this.printer.print(lineNumber, ')');
		}
		else
		{
			int nextOffset = this.previousOffset + 1;	
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(lineNumber, "()");			
		}
		
		return lineNumber;
	}
	
	private int writeGetStatic(GetStatic getStatic)
	{
		int lineNumber = getStatic.lineNumber;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(getStatic.offset <= this.lastOffset))
		{
			ConstantFieldref cfr = 
				constants.getConstantFieldref(getStatic.index);
			String internalClassName = 
				constants.getConstantClassName(cfr.class_index);
					
			if (cfr.class_index != classFile.getThisClassIndex())
			{
				this.printer.addNewLinesAndPrefix(lineNumber);
				
				String className = SignatureUtil.CreateTypeName(internalClassName);
				
				int length = SignatureWriter.WriteSignature(
					this.loader, this.printer, 
					this.referenceMap, this.classFile, className);
				
				if (length > 0)
				{
					this.printer.print(lineNumber, '.');
				}
			}
			
			ConstantNameAndType cnat = constants.getConstantNameAndType(
				cfr.name_and_type_index);		
			String descriptor = constants.getConstantUtf8(cnat.descriptor_index);	
			String constName = constants.getConstantUtf8(cnat.name_index);
					
			this.printer.printStaticField(
				lineNumber, internalClassName, constName, 
				descriptor, this.classFile.getThisClassName());		
		}
		
		return lineNumber;
	}

	private int writeOuterThis(GetStatic getStatic)
	{
		int lineNumber = getStatic.lineNumber;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(getStatic.offset <= this.lastOffset))
		{
			ConstantFieldref cfr = 
				constants.getConstantFieldref(getStatic.index);
			
			if (cfr.class_index != classFile.getThisClassIndex())
			{
				this.printer.addNewLinesAndPrefix(lineNumber);
				int length = SignatureWriter.WriteSignature(
					this.loader, this.printer, 
					this.referenceMap, this.classFile, 
					SignatureUtil.CreateTypeName(constants.getConstantClassName(cfr.class_index)));
				
				if (length > 0)
				{
					this.printer.print(lineNumber, '.');
				}
			}
			
			ConstantNameAndType cnat = constants.getConstantNameAndType(
				cfr.name_and_type_index);			
			this.printer.printKeyword(
				lineNumber, constants.getConstantUtf8(cnat.name_index));
		}
		
		return lineNumber;
	}
	
	private int writeLcdInstruction(IndexInstruction ii)
	{
		int lineNumber = ii.lineNumber;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(ii.offset <= this.lastOffset))
		{
			// Dans les specs, LDC pointe vers une constante du pool. Lors de la
			// declaration d'enumeration, le byte code de la methode 
			// 'Enum.valueOf(Class<T> enumType, String name)' contient une
			// instruction LDC pointant un objet de type 'ConstantClass'.
			Constant cst = constants.get(ii.index);
			
			if (cst.tag == ConstantConstant.CONSTANT_Class)
			{
				// Exception a la regle
				ConstantClass cc = (ConstantClass)cst;
				String signature = SignatureUtil.CreateTypeName(
					constants.getConstantUtf8(cc.name_index)); 
				
				this.printer.addNewLinesAndPrefix(lineNumber);
				SignatureWriter.WriteSignature(
					this.loader, this.printer, this.referenceMap, 
					this.classFile, signature);
				this.printer.print('.');
				this.printer.printKeyword("class");
			}
			else
			{
				// Cas g�n�ral
				this.printer.addNewLinesAndPrefix(lineNumber);
		    	ConstantValueWriter.Write(
		    		this.loader, this.printer, this.referenceMap, 
		    		this.classFile, (ConstantValue)cst);
			}
		}
		
		return lineNumber;
	}
	
	private int writeLoadInstruction(LoadInstruction loadInstruction)
	{
		int lineNumber = loadInstruction.lineNumber;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(loadInstruction.offset <= this.lastOffset))
		{
			LocalVariable lv = 
				this.localVariables.getLocalVariableWithIndexAndOffset(
					loadInstruction.index, loadInstruction.offset);
			
			if ((lv == null) || (lv.name_index <= 0))
			{
				// Error
				this.printer.startOfError();
				this.printer.print(lineNumber, "???");
				this.printer.endOfError();
			}
			else
			{
				int nameIndex = lv.name_index;
	
				if (nameIndex == -1)
				{
					// Error
					this.printer.startOfError();
					this.printer.print(lineNumber, "???");
					this.printer.endOfError();
				}
				else if (nameIndex == this.constants.thisLocalVariableNameIndex)
				{
					this.printer.printKeyword(
						lineNumber, constants.getConstantUtf8(lv.name_index));
				}
				else
				{
					this.printer.print(
						lineNumber, constants.getConstantUtf8(lv.name_index));
				}
			}
		}
		
		return lineNumber;
	}
	
	private int writeMultiANewArray(MultiANewArray multiANewArray)
	{
		int lineNumber = multiANewArray.lineNumber;
		
		String signature = constants.getConstantClassName(multiANewArray.index);
	
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
		{
			this.printer.printKeyword(lineNumber, "new");
			this.printer.print(' ');
		
			SignatureWriter.WriteSignature(
				this.loader, this.printer, this.referenceMap, this.classFile, 
				SignatureUtil.CutArrayDimensionPrefix(signature));
		}
		
		Instruction[] dimensions = multiANewArray.dimensions;
		
		for (int i=dimensions.length-1; i>=0; i--)
		{
			nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(lineNumber, '[');
			
			lineNumber = visit(dimensions[i]);
			
			nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
				this.printer.print(lineNumber, ']');			
		}

		// Affichage des dimensions sans taille
		nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
		{
			int dimensionCount = SignatureUtil.GetArrayDimensionCount(signature);
			for (int i=dimensions.length; i<dimensionCount; i++)
				this.printer.print(lineNumber, "[]");
		}
		
		return lineNumber;
	}

	
	private int writePutStatic(PutStatic putStatic)
	{
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
		{
			int lineNumber = putStatic.lineNumber;
			
			ConstantFieldref cfr = constants.getConstantFieldref(putStatic.index);
			
			if (cfr.class_index != classFile.getThisClassIndex())
			{
				this.printer.addNewLinesAndPrefix(lineNumber);
				
				String signature = SignatureUtil.CreateTypeName(
					this.constants.getConstantClassName(cfr.class_index));
				
				int length = SignatureWriter.WriteSignature(
					this.loader, this.printer, 
					this.referenceMap, this.classFile, signature);
				
				if (length > 0)
				{
					this.printer.print(lineNumber, '.');
				}
			}
			
			ConstantNameAndType cnat = 
				constants.getConstantNameAndType(cfr.name_and_type_index);
			String descriptor = constants.getConstantUtf8(cnat.descriptor_index);			
			String internalClassName = SignatureUtil.GetInternalName(descriptor);
			String constName = constants.getConstantUtf8(cnat.name_index);
					
			this.printer.printStaticField(
				lineNumber, internalClassName, constName, 
				descriptor, this.classFile.getThisClassName());			
			
			this.printer.print(lineNumber, " = ");
		}
		
		// Est-il necessaire de parenth�ser l'expression ?
		// visit(putStatic, putStatic.valueref);
		return visit(putStatic.valueref);
	}

	private int writeStoreInstruction(StoreInstruction storeInstruction)
	{
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
		{
			int lineNumber = storeInstruction.lineNumber;
			
			LocalVariable lv = 
				this.localVariables.getLocalVariableWithIndexAndOffset(
					storeInstruction.index, storeInstruction.offset);
			
			if ((lv == null) || (lv.name_index <= 0))
			{
				this.printer.startOfError();
				this.printer.print(lineNumber, "???");
				this.printer.endOfError();
			}
			else
			{
				this.printer.print(
					lineNumber, constants.getConstantUtf8(lv.name_index));
			}
			
			this.printer.print(lineNumber, " = ");
		}
		
		// Est-il necessaire de parenth�ser l'expression ?
		// visit(storeInstruction, storeInstruction.valueref);
		return visit(storeInstruction.valueref);
	}

	private int writeExceptionLoad(ExceptionLoad exceptionLoad)
	{		
		int lineNumber = exceptionLoad.lineNumber;		
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
		{
			if (exceptionLoad.exceptionNameIndex == 0)
			{
				this.printer.printKeyword(lineNumber, "finally");
			}
			else
			{
				LocalVariable lv = 
					this.localVariables.getLocalVariableWithIndexAndOffset(
						exceptionLoad.index, exceptionLoad.offset);
				
				if ((lv == null) || (lv.name_index == 0))
				{
					this.printer.startOfError();
					this.printer.print(lineNumber, "???");
					this.printer.endOfError();
				}
				else
				{
					this.printer.print(
						lineNumber, constants.getConstantUtf8(lv.name_index));
				}
			}
		}
		
		return lineNumber;
	}
	
	private int writeAssignmentInstruction(AssignmentInstruction ai)
	{
		int lineNumber = ai.lineNumber;
		int previousOffsetBackup = this.previousOffset;
		
		visit(ai.value1);	

		this.previousOffset = previousOffsetBackup;		
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
		{
			this.printer.print(lineNumber, ' ');
			this.printer.print(lineNumber, ai.operator);
			this.printer.print(lineNumber, ' ');
		}
		
		/* +=, -=, *=, /=, %=, <<=, >>=, >>>=, &=, |=, ^= */
		if (ai.operator.length() > 0)
		{
			switch (ai.operator.charAt(0))
			{
			case '&': case '|': case '^':
				// Binary operators
				return writeBinaryOperatorParameterInHexaOrBoolean(ai, ai.value2);
			}
		}
		
		return visit(ai, ai.value2);	
	}

	private int writeConvertInstruction(ConvertInstruction instruction)
	{
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) &&
			(nextOffset <= this.lastOffset))
		{
			int lineNumber = instruction.lineNumber;
			
			switch (instruction.signature.charAt(0))
			{
			case 'C':
				this.printer.print(lineNumber, '(');
				this.printer.printKeyword("char");
				this.printer.print(')');
				break;
			case 'B':
				this.printer.print(lineNumber, '(');
				this.printer.printKeyword("byte");
				this.printer.print(')');
				break;
			case 'S':
				this.printer.print(lineNumber, '(');
				this.printer.printKeyword("short");
				this.printer.print(')');
				break;
			case 'I':
				this.printer.print(lineNumber, '(');
				this.printer.printKeyword("int");
				this.printer.print(')');
				break;
			case 'L':
				this.printer.print(lineNumber, '(');
				this.printer.printKeyword("long");
				this.printer.print(')');
				break;
			case 'F':
				this.printer.print(lineNumber, '(');
				this.printer.printKeyword("float");
				this.printer.print(')');
				break;
			case 'D':
				this.printer.print(lineNumber, '(');
				this.printer.printKeyword("double");
				this.printer.print(')');
				break;
			}
		}
		
		return visit(instruction, instruction.value);
	}	

	private int writeDeclaration(FastDeclaration fd)
	{
		int lineNumber = fd.lineNumber;
		
		LocalVariable lv = 
			localVariables.getLocalVariableWithIndexAndOffset(
				fd.index, fd.offset);
		
		if (lv == null)
		{
			if (fd.instruction == null)
			{
				int nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
				{
					this.printer.startOfError();
					this.printer.print(lineNumber, "???");
					this.printer.endOfError();
				}
			}
			else
			{
				lineNumber = visit(fd.instruction);
			}
		}
		else
		{
			int nextOffset = this.previousOffset + 1;
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
			{
				this.printer.addNewLinesAndPrefix(lineNumber);
				String signature = 
					this.constants.getConstantUtf8(lv.signature_index);				
				String internalName = 
						SignatureUtil.GetInternalName(signature);				
				ClassFile innerClassFile = 
					this.classFile.getInnerClassFile(internalName);

				if (lv.finalFlag)
				{
					this.printer.printKeyword("final");					
					this.printer.print(' ');
				}
				
				if ((innerClassFile != null) &&
					(innerClassFile.getInternalAnonymousClassName() != null))
				{
					String internalAnonymousClassSignature = 
							SignatureUtil.CreateTypeName(innerClassFile.getInternalAnonymousClassName());					
					SignatureWriter.WriteSignature(
						this.loader, this.printer, this.referenceMap, 
						this.classFile, internalAnonymousClassSignature);
				}
				else
				{
					SignatureWriter.WriteSignature(
						this.loader, this.printer, this.referenceMap, 
						this.classFile, signature);
				}
				
				this.printer.print(' ');
			}
			
			if (fd.instruction == null)
			{
				nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
					this.printer.print(
						lineNumber, constants.getConstantUtf8(lv.name_index));
			}
			else
			{
				lineNumber = visit(fd.instruction);
			}
		}
		
		return lineNumber;
	}
	
	/*
	 * Affichage des initialisations de tableaux associees aux instructions
	 * 'NEWARRAY' et 'ANEWARRAY' dans les affectations '?Store' et passees
	 * en parametres.
	 */
	private int WriteInitArrayInstruction(InitArrayInstruction iai)
	{		
		int lineNumber = iai.lineNumber;
		
		// Affichage des valeurs
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
			this.printer.print(lineNumber, "{");
		
		List<Instruction> values = iai.values;
		final int length = values.size();

		if (length > 0)
		{
			Instruction instruction = values.get(0);
			
			if ((this.firstOffset <= this.previousOffset) && 
				(nextOffset <= this.lastOffset))
			{		
				if (lineNumber == instruction.lineNumber)
					this.printer.print(" ");	
			}
			
			lineNumber = visit(instruction);
			
			for (int i=1; i<length; i++)
			{
				nextOffset = this.previousOffset + 1;
				
				if ((this.firstOffset <= this.previousOffset) && 
					(nextOffset <= this.lastOffset))
					this.printer.print(lineNumber, ", ");	

				lineNumber = visit(values.get(i));
			}
		}

		nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
			this.printer.print(lineNumber, " }");
		
		return lineNumber;
	}
	
	/*
	 * Affichage des initialisations de tableaux associees aux instructions
	 * 'NEWARRAY' et 'ANEWARRAY' dans les affectations '?Store' et passees
	 * en parametres.
	 */
	private int WriteNewAndInitArrayInstruction(InitArrayInstruction iai)
	{		
		int nextOffset = this.previousOffset + 1;
		
		if ((this.firstOffset <= this.previousOffset) && 
			(nextOffset <= this.lastOffset))
		{
			int lineNumber = iai.lineNumber;
		
			// Affichage de l'instruction 'new'
			this.printer.printKeyword(lineNumber, "new");
			this.printer.print(' ');
			
			switch (iai.newArray.opcode)
			{
			case ByteCodeConstants.NEWARRAY:
				NewArray na = (NewArray)iai.newArray;
				SignatureWriter.WriteSignature(
					this.loader, this.printer, 
					this.referenceMap, this.classFile, 
					SignatureUtil.GetSignatureFromType(na.type));
				break;
			case ByteCodeConstants.ANEWARRAY:
				ANewArray ana = (ANewArray)iai.newArray;
				String signature = constants.getConstantClassName(ana.index);
				
				if (signature.charAt(0) != '[')
					signature = SignatureUtil.CreateTypeName(signature);
				
				SignatureWriter.WriteSignature(
					this.loader, this.printer, this.referenceMap, 
					this.classFile, signature);
				break;
			}
	
			this.printer.print(lineNumber, "[] ");
		}
		
		return WriteInitArrayInstruction(iai);
	}
}
