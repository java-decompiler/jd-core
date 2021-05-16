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
package jd.core.process.analyzer.classfile.visitor;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.constant.ConstantClass;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;
import jd.core.model.instruction.bytecode.instruction.LookupSwitch;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.model.instruction.bytecode.instruction.MonitorExit;
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.TableSwitch;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;

/*
 * Search :
		public int indexOf(int ch)
		public int indexOf(int ch, int fromIndex)
		public int lastIndexOf(int ch)
		public int lastIndexOf(int ch, int fromIndex)
 */
public class SetConstantTypeInStringIndexOfMethodsVisitor 
{
	protected ConstantPool constants;
	
	public SetConstantTypeInStringIndexOfMethodsVisitor(ConstantPool constants)
	{
		this.constants = constants;
	}
	
	public void visit(Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			visit(((ArrayLength)instruction).arrayref);
			break;
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			visit(((ArrayStoreInstruction)instruction).arrayref);
			break;
		case ByteCodeConstants.ASSERT:
			{
				AssertInstruction ai = (AssertInstruction)instruction;
				visit(ai.test);
				if (ai.msg != null)
					visit(ai.msg);
			}
			break;
		case ByteCodeConstants.ATHROW:
			visit(((AThrow)instruction).value);
			break;
		case ByteCodeConstants.UNARYOP:
			visit(((UnaryOperatorInstruction)instruction).value);
			break;
		case ByteCodeConstants.BINARYOP:
		case ByteCodeConstants.ASSIGNMENT:
			{
				BinaryOperatorInstruction boi = 
					(BinaryOperatorInstruction)instruction;
				visit(boi.value1);
				visit(boi.value2);
			}
			break;
		case ByteCodeConstants.CHECKCAST:
			visit(((CheckCast)instruction).objectref);
			break;
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			visit(((StoreInstruction)instruction).valueref);
			break;
		case ByteCodeConstants.DUPSTORE:
			visit(((DupStore)instruction).objectref);
			break;
		case ByteCodeConstants.CONVERT:
		case ByteCodeConstants.IMPLICITCONVERT:
			visit(((ConvertInstruction)instruction).value);
			break;
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ifCmp = (IfCmp)instruction;
				visit(ifCmp.value1);
				visit(ifCmp.value2);
			}
			break;
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFXNULL:
			visit(((IfInstruction)instruction).value);
			break;
		case ByteCodeConstants.COMPLEXIF:
			{
				List<Instruction> branchList = 
					((ComplexConditionalBranchInstruction)instruction).instructions;
				for (int i=branchList.size()-1; i>=0; --i)
					visit(branchList.get(i));
			}
			break;
		case ByteCodeConstants.INSTANCEOF:
			visit(((InstanceOf)instruction).objectref);
			break;
		case ByteCodeConstants.INVOKEVIRTUAL:
			{
				Invokevirtual iv = (Invokevirtual)instruction;
				ConstantMethodref cmr = 
					this.constants.getConstantMethodref(iv.index);
				ConstantClass cc = this.constants.getConstantClass(cmr.class_index);
				
				if (cc.name_index == this.constants.stringClassNameIndex)
				{
					int nbrOfParameters = iv.args.size();
				
					if ((1 <= nbrOfParameters) && (nbrOfParameters <= 2)) 
					{
						int opcode = iv.args.get(0).opcode;
						
						if (((opcode==ByteCodeConstants.BIPUSH) ||
							 (opcode==ByteCodeConstants.SIPUSH)) &&
							 cmr.getReturnedSignature().equals("I") &&
							 cmr.getListOfParameterSignatures().get(0).equals("I"))
						{
							ConstantNameAndType cnat = 
								this.constants.getConstantNameAndType(
									cmr.name_and_type_index);
							String name = 
								this.constants.getConstantUtf8(cnat.name_index);
							
							if ("indexOf".equals(name) || 
								"lastIndexOf".equals(name))
							{
								// Change constant type
								IConst ic = (IConst)iv.args.get(0);
								ic.setReturnedSignature("C");
								break;
							}
						}
					}
				}
			}
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
			visit(((InvokeNoStaticInstruction)instruction).objectref);
		case ByteCodeConstants.INVOKESTATIC:
		case ByteCodeConstants.INVOKENEW:
			{
				List<Instruction> list = ((InvokeInstruction)instruction).args;
				for (int i=list.size()-1; i>=0; --i)
					visit(list.get(i));
			}
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
			visit(((LookupSwitch)instruction).key);
			break;
		case ByteCodeConstants.MONITORENTER:
			visit(((MonitorEnter)instruction).objectref);
			break;
		case ByteCodeConstants.MONITOREXIT:
			visit(((MonitorExit)instruction).objectref);
			break;
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				Instruction[] dimensions = ((MultiANewArray)instruction).dimensions;
				for (int i=dimensions.length-1; i>=0; --i)
					visit(dimensions[i]);
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			visit(((NewArray)instruction).dimension);
			break;
		case ByteCodeConstants.ANEWARRAY:
			visit(((ANewArray)instruction).dimension);
			break;
		case ByteCodeConstants.POP:
			visit(((Pop)instruction).objectref);
			break;
		case ByteCodeConstants.PUTFIELD: 
			{
				PutField putField = (PutField)instruction;
				visit(putField.objectref);
				visit(putField.valueref);
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			visit(((PutStatic)instruction).valueref);
			break;
		case ByteCodeConstants.XRETURN:
			visit(((ReturnInstruction)instruction).valueref);
			break;
		case ByteCodeConstants.TABLESWITCH:
			visit(((TableSwitch)instruction).key);
			break;
		case ByteCodeConstants.TERNARYOPSTORE:
			visit(((TernaryOpStore)instruction).objectref);
			break;
		case ByteCodeConstants.PREINC:			
		case ByteCodeConstants.POSTINC:	
			visit(((IncInstruction)instruction).value);
			break;
		case ByteCodeConstants.GETFIELD:
			visit(((GetField)instruction).objectref);
			break;
		case ByteCodeConstants.INITARRAY:
		case ByteCodeConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iai = (InitArrayInstruction)instruction;
				visit(iai.newArray);
				if (iai.values != null)
					visit(iai.values);
			}
			break;
		case ByteCodeConstants.ACONST_NULL:
		case ByteCodeConstants.ARRAYLOAD:
		case ByteCodeConstants.LOAD:
		case ByteCodeConstants.ALOAD:
		case ByteCodeConstants.ILOAD:
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.LCONST:
		case ByteCodeConstants.FCONST:
		case ByteCodeConstants.DCONST:
		case ByteCodeConstants.DUPLOAD:
		case ByteCodeConstants.GETSTATIC:
		case ByteCodeConstants.OUTERTHIS:
		case ByteCodeConstants.GOTO:
		case ByteCodeConstants.IINC:			
		case ByteCodeConstants.JSR:			
		case ByteCodeConstants.LDC:
		case ByteCodeConstants.LDC2_W:
		case ByteCodeConstants.NEW:
		case ByteCodeConstants.NOP:
		case ByteCodeConstants.SIPUSH:
		case ByteCodeConstants.RET:
		case ByteCodeConstants.RETURN:
		case ByteCodeConstants.EXCEPTIONLOAD:
		case ByteCodeConstants.RETURNADDRESSLOAD:
			break;
		default:
			System.err.println(
					"Can not search String.indexOf in " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
	}

	public void visit(List<Instruction> instructions)
	{
		for (int i=instructions.size()-1; i>=0; --i)
			visit(instructions.get(i));
	}	
}
