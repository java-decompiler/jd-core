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
import jd.core.util.SignatureUtil;


/*
 * Elimine les doubles casts et ajoute des casts devant les constantes 
 * numeriques si necessaire.
 */
public class CheckCastAndConvertInstructionVisitor 
{
	private static void visit(ConstantPool constants, Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			visit(constants, ((ArrayLength)instruction).arrayref);
			break;
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			visit(constants, ((ArrayStoreInstruction)instruction).arrayref);
			break;
		case ByteCodeConstants.ASSERT:
			{
				AssertInstruction ai = (AssertInstruction)instruction;
				visit(constants, ai.test);
				if (ai.msg != null)
					visit(constants, ai.msg);
			}
			break;
		case ByteCodeConstants.ATHROW:
			visit(constants, ((AThrow)instruction).value);
			break;
		case ByteCodeConstants.UNARYOP:
			visit(constants, ((UnaryOperatorInstruction)instruction).value);
			break;
		case ByteCodeConstants.BINARYOP:
		case ByteCodeConstants.ASSIGNMENT:
			{
				BinaryOperatorInstruction boi = 
					(BinaryOperatorInstruction)instruction;
				visit(constants, boi.value1);
				visit(constants, boi.value2);
			}
			break;
		case ByteCodeConstants.CHECKCAST:
			{
				CheckCast cc = (CheckCast)instruction;	
				if (cc.objectref.opcode == ByteCodeConstants.CHECKCAST)
				{
					cc.objectref = ((CheckCast)cc.objectref).objectref;					
				}				
				visit(constants, cc.objectref);
			}
			break;
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			visit(constants, ((StoreInstruction)instruction).valueref);
			break;
		case ByteCodeConstants.DUPSTORE:
			visit(constants, ((DupStore)instruction).objectref);
			break;
		case ByteCodeConstants.CONVERT:
		case ByteCodeConstants.IMPLICITCONVERT:
			visit(constants, ((ConvertInstruction)instruction).value);
			break;
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ifCmp = (IfCmp)instruction;
				visit(constants, ifCmp.value1);
				visit(constants, ifCmp.value2);
			}
			break;
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFXNULL:
			visit(constants, ((IfInstruction)instruction).value);
			break;
		case ByteCodeConstants.COMPLEXIF:
			{
				List<Instruction> branchList = 
					((ComplexConditionalBranchInstruction)instruction).instructions;
				for (int i=branchList.size()-1; i>=0; --i)
				{
					visit(constants, branchList.get(i));
				}
			}
			break;
		case ByteCodeConstants.INSTANCEOF:
			visit(constants, ((InstanceOf)instruction).objectref);
			break;
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
		case ByteCodeConstants.INVOKEVIRTUAL:
			{
				visit(constants, ((InvokeNoStaticInstruction)instruction).objectref);
			}
		case ByteCodeConstants.INVOKESTATIC:
		case ByteCodeConstants.INVOKENEW:
			{
				List<String> parameterSignatures =
					((InvokeInstruction)instruction).
						getListOfParameterSignatures(constants);
				
				if (parameterSignatures != null)
				{
					List<Instruction> args = 
						((InvokeInstruction)instruction).args;				
					int i = parameterSignatures.size();
					int j = args.size();
					
					while ((i-- > 0) && (j-- > 0))
					{
						Instruction arg = args.get(j);
						
						switch (arg.opcode)
						{
						case ByteCodeConstants.SIPUSH:
						case ByteCodeConstants.BIPUSH:
						case ByteCodeConstants.ICONST:
							{						
								String argSignature = ((IConst)arg).getSignature();
								String parameterSignature = parameterSignatures.get(i);
								
								if (!parameterSignature.equals(argSignature))
								{
									// Types differents
									int argBitFields = 
											SignatureUtil.CreateArgOrReturnBitFields(argSignature);
									int paramBitFields = 
											SignatureUtil.CreateTypesBitField(parameterSignature);
									
									if ((argBitFields|paramBitFields) == 0)
									{
										// Ajout d'une instruction cast si les types
										// sont differents
								    	args.set(j, new ConvertInstruction(
								    		ByteCodeConstants.CONVERT, 
								    		arg.offset-1, arg.lineNumber, 
								    		arg, parameterSignature));
									}							    	
								}
								else
								{
									switch (parameterSignature.charAt(0))
								    {
								    case 'B':
								    case 'S':
								    	// Ajout d'une instruction cast pour les
								    	// parametres numeriques de type byte ou short
								    	args.set(j, new ConvertInstruction(
								    		ByteCodeConstants.CONVERT, 
								    		arg.offset-1, arg.lineNumber, 
								    		arg, parameterSignature));		    
								    	break;
								    default:
								    	visit(constants, arg);
								    }
								}
							}	
							break;
						default:	
							visit(constants, arg);
						}
					}
				}					
			}
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
			visit(constants, ((LookupSwitch)instruction).key);
			break;
		case ByteCodeConstants.MONITORENTER:
			visit(constants, ((MonitorEnter)instruction).objectref);
			break;
		case ByteCodeConstants.MONITOREXIT:
			visit(constants, ((MonitorExit)instruction).objectref);
			break;
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				Instruction[] dimensions = ((MultiANewArray)instruction).dimensions;
				for (int i=dimensions.length-1; i>=0; --i)
				{
					visit(constants, dimensions[i]);
				}
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			visit(constants, ((NewArray)instruction).dimension);
			break;
		case ByteCodeConstants.ANEWARRAY:
			visit(constants, ((ANewArray)instruction).dimension);
			break;
		case ByteCodeConstants.POP:
			visit(constants, ((Pop)instruction).objectref);
			break;
		case ByteCodeConstants.PUTFIELD: 
			{
				PutField putField = (PutField)instruction;
				visit(constants, putField.objectref);
				visit(constants, putField.valueref);
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			visit(constants, ((PutStatic)instruction).valueref);
			break;
		case ByteCodeConstants.XRETURN:
			visit(constants, ((ReturnInstruction)instruction).valueref);
			break;
		case ByteCodeConstants.TABLESWITCH:
			visit(constants, ((TableSwitch)instruction).key);
			break;
		case ByteCodeConstants.TERNARYOPSTORE:
			visit(constants, ((TernaryOpStore)instruction).objectref);
			break;
		case ByteCodeConstants.PREINC:			
		case ByteCodeConstants.POSTINC:	
			visit(constants, ((IncInstruction)instruction).value);
			break;
		case ByteCodeConstants.GETFIELD:
			visit(constants, ((GetField)instruction).objectref);
			break;
		case ByteCodeConstants.INITARRAY:
		case ByteCodeConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iai = (InitArrayInstruction)instruction;
				visit(constants, iai.newArray);
				if (iai.values != null)
					visit(constants, iai.values);
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
					"Can not check cast and convert instruction in " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
	}

	public static void visit(
		ConstantPool constants, List<Instruction> instructions)
	{
		for (int i=instructions.size()-1; i>=0; --i)
		{
			visit(constants, instructions.get(i));
		}
	}
}
