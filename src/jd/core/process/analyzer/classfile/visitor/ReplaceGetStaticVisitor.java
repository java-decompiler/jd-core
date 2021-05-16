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

import jd.core.model.instruction.bytecode.ByteCodeConstants;
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
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
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
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;


public class ReplaceGetStaticVisitor 
{
	private int index;
	private Instruction newInstruction;
	private Instruction parentFound;

	public ReplaceGetStaticVisitor(int index, Instruction newInstruction)
	{
		this.index = index;
		this.newInstruction = newInstruction;
		this.parentFound = null;
	}
	
	public void visit(Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			{
				ArrayLength al = (ArrayLength)instruction;
				if (match(al, al.arrayref))
					al.arrayref = this.newInstruction;
				else
					visit(al.arrayref);
			}
			break;
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			{
				ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
				if (match(asi, asi.arrayref))
				{
					asi.arrayref = this.newInstruction;
				}
				else
				{
					visit(asi.arrayref);
					
					if (this.parentFound == null)
					{
						if (match(asi, asi.indexref))
						{
							asi.indexref = this.newInstruction;
						}
						else
						{
							visit(asi.indexref);
							
							if (this.parentFound == null)
							{
								if (match(asi, asi.valueref))
									asi.valueref = this.newInstruction;
								else
									visit(asi.valueref);
							}
						}
					}
				}
			}
			break;
		case ByteCodeConstants.ASSERT:
			{
				AssertInstruction ai = (AssertInstruction)instruction;
				if (match(ai, ai.test))
				{
					ai.test = this.newInstruction;
				}
				else
				{
					visit(ai.test);
				
					if ((this.parentFound == null) && (ai.msg != null))
					{
						if (match(ai, ai.msg))
							ai.msg = this.newInstruction;
						else
							visit(ai.msg);
					}
				}				
			}
			break;
		case ByteCodeConstants.ATHROW:
			{
				AThrow aThrow = (AThrow)instruction;
				if (match(aThrow, aThrow.value))
					aThrow.value = this.newInstruction;
				else
					visit(aThrow.value);
			}
			break;
		case ByteCodeConstants.UNARYOP:
			{
				UnaryOperatorInstruction uoi = (UnaryOperatorInstruction)instruction;
				if (match(uoi, uoi.value))
					uoi.value = this.newInstruction;
				else
					visit(uoi.value);
			}
			break;
		case ByteCodeConstants.BINARYOP:
			{
				BinaryOperatorInstruction boi = (BinaryOperatorInstruction)instruction;
				if (match(boi, boi.value1))
				{
					boi.value1 = this.newInstruction;
				}
				else
				{
					visit(boi.value1);
				
					if (this.parentFound == null)
					{
						if (match(boi, boi.value2))
							boi.value2 = this.newInstruction;
						else
							visit(boi.value2);
					}
				}
			}
			break;
		case ByteCodeConstants.CHECKCAST:
			{
				CheckCast checkCast = (CheckCast)instruction;
				if (match(checkCast, checkCast.objectref))
					checkCast.objectref = this.newInstruction;
				else
					visit(checkCast.objectref);
			}
			break;
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			{
				StoreInstruction storeInstruction = (StoreInstruction)instruction;
				if (match(storeInstruction, storeInstruction.valueref))
					storeInstruction.valueref = this.newInstruction;
				else
					visit(storeInstruction.valueref);
			}
			break;
		case ByteCodeConstants.DUPSTORE:
			{
				DupStore dupStore = (DupStore)instruction;
				if (match(dupStore, dupStore.objectref))
					dupStore.objectref = this.newInstruction;
				else
					visit(dupStore.objectref);
			}
			break;
		case ByteCodeConstants.CONVERT:
		case ByteCodeConstants.IMPLICITCONVERT:
			{
				ConvertInstruction ci = (ConvertInstruction)instruction;
				if (match(ci, ci.value))
					ci.value = this.newInstruction;
				else
					visit(ci.value);
			}
			break;
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ifCmp = (IfCmp)instruction;
				if (match(ifCmp, ifCmp.value1))
				{
					ifCmp.value1 = this.newInstruction;
				}
				else
				{
					visit(ifCmp.value1);
					
					if (this.parentFound == null)
					{
						if (match(ifCmp, ifCmp.value2))
							ifCmp.value2 = this.newInstruction;
						else
							visit(ifCmp.value2);
					}
				}
			}
			break;
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFXNULL:
			{
				IfInstruction iff = (IfInstruction)instruction;
				if (match(iff, iff.value))
					iff.value = this.newInstruction;
				else
					visit(iff.value);
			}
			break;			
		case ByteCodeConstants.COMPLEXIF:
			{
				List<Instruction> branchList = 
					((ComplexConditionalBranchInstruction)instruction).instructions;
				for (int i=branchList.size()-1; (i>=0) && (this.parentFound == null); --i)
				{
					visit(branchList.get(i));
				}
			}
			break;
		case ByteCodeConstants.INSTANCEOF:
			{
				InstanceOf instanceOf = (InstanceOf)instruction;
				if (match(instanceOf, instanceOf.objectref))
					instanceOf.objectref = this.newInstruction;
				else
					visit(instanceOf.objectref);
			}
			break;
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
		case ByteCodeConstants.INVOKEVIRTUAL:
			{
				InvokeNoStaticInstruction insi = 
					(InvokeNoStaticInstruction)instruction;
				if (match(insi, insi.objectref))
					insi.objectref = this.newInstruction;
				else
					visit(insi.objectref);
			}
		case ByteCodeConstants.INVOKESTATIC:
		case ByteCodeConstants.INVOKENEW:
			{
				List<Instruction> list = ((InvokeInstruction)instruction).args;
				for (int i=list.size()-1; (i>=0) && (this.parentFound == null); --i)
				{
					if (match(instruction, list.get(i)))
						list.set(i, this.newInstruction);
					else
						visit(list.get(i));
				}
			}
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
			{
				LookupSwitch ls = (LookupSwitch)instruction;
				if (match(ls, ls.key))
					ls.key = this.newInstruction;
				else
					visit(ls.key);
			}
			break;			
		case ByteCodeConstants.MONITORENTER:
			{
				MonitorEnter monitorEnter = (MonitorEnter)instruction;
				if (match(monitorEnter, monitorEnter.objectref))
					monitorEnter.objectref = this.newInstruction;
				else
					visit(monitorEnter.objectref);
			}
			break;
		case ByteCodeConstants.MONITOREXIT:
			{
				MonitorExit monitorExit = (MonitorExit)instruction;
				if (match(monitorExit, monitorExit.objectref))
					monitorExit.objectref = this.newInstruction;
				else
					visit(monitorExit.objectref);
			}
			break;
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				Instruction[] dimensions = ((MultiANewArray)instruction).dimensions;
				for (int i=dimensions.length-1; (i>=0) && (this.parentFound == null); --i)
				{
					if (match(instruction, dimensions[i]))
						dimensions[i] = this.newInstruction;
					else
						visit(dimensions[i]);
				}
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			{
				NewArray newArray = (NewArray)instruction;
				if (match(newArray, newArray.dimension))
					newArray.dimension = this.newInstruction;
				else
					visit(newArray.dimension);
			}
			break;
		case ByteCodeConstants.ANEWARRAY:
			{
				ANewArray aNewArray = (ANewArray)instruction;
				if (match(aNewArray, aNewArray.dimension))
					aNewArray.dimension = this.newInstruction;
				else
					visit(aNewArray.dimension);
			}
			break;
		case ByteCodeConstants.POP:
			{
				Pop pop = (Pop)instruction;
				if (match(pop, pop.objectref))
					pop.objectref = this.newInstruction;
				else
					visit(pop.objectref);
			}
			break;
		case ByteCodeConstants.PUTFIELD:
			{
				PutField putField = (PutField)instruction;
				if (match(putField, putField.objectref))
				{
					putField.objectref = this.newInstruction;
				}
				else
				{
					visit(putField.objectref);
					
					if (this.parentFound == null)
					{
						if (match(putField, putField.valueref))
							putField.valueref = this.newInstruction;
						else
							visit(putField.valueref);
					}
				}
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			{
				PutStatic putStatic = (PutStatic)instruction;
				if (match(putStatic, putStatic.valueref))
					putStatic.valueref = this.newInstruction;
				else
					visit(putStatic.valueref);
			}
			break;
		case ByteCodeConstants.XRETURN:
			{
				ReturnInstruction ri = (ReturnInstruction)instruction;
				if (match(ri, ri.valueref))
					ri.valueref = this.newInstruction;
				else
					visit(ri.valueref);
			}
			break;			
		case ByteCodeConstants.TABLESWITCH:
			{
				TableSwitch ts = (TableSwitch)instruction;
				if (match(ts, ts.key))
					ts.key = this.newInstruction;
				else
					visit(ts.key);
			}
			break;			
		case ByteCodeConstants.TERNARYOPSTORE:
			{
				TernaryOpStore tos = (TernaryOpStore)instruction;
				if (match(tos, tos.objectref))
					tos.objectref = this.newInstruction;
				else
					visit(tos.objectref);	
			}
			break;		
		case ByteCodeConstants.TERNARYOP:	
			{
				TernaryOperator to = (TernaryOperator)instruction;
				if (match(to, to.test))
				{
					to.test = this.newInstruction;
				}
				else
				{
					visit(to.test);

					if (this.parentFound == null)
					{
						if (match(to, to.value1))
						{
							to.value1 = this.newInstruction;
						}
						else
						{
							visit(to.value1);
			
							if (this.parentFound == null)
							{
								if (match(to, to.value2))
									to.value2 = this.newInstruction;
								else
									visit(to.value2);
							}
						}
					}
				}
			}
			break;	
		case ByteCodeConstants.ASSIGNMENT:
			{
				AssignmentInstruction ai = (AssignmentInstruction)instruction;
				if (match(ai, ai.value1))
				{
					ai.value1 = this.newInstruction;
				}
				else
				{
					visit(ai.value1);
	
					if (this.parentFound == null)
					{
						if (match(ai, ai.value2))
							ai.value2 = this.newInstruction;
						else
							visit(ai.value2);
					}
				}
			}
			break;
		case ByteCodeConstants.ARRAYLOAD:
			{
				ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
				if (match(ali, ali.arrayref))
				{
					ali.arrayref = this.newInstruction;
				}
				else
				{
					visit(ali.arrayref);
	
					if (this.parentFound == null)
					{
						if (match(ali, ali.indexref))
							ali.indexref = this.newInstruction;
						else
							visit(ali.indexref);
					}
				}
			}
			break;
		case ByteCodeConstants.PREINC:			
		case ByteCodeConstants.POSTINC:		
			{
				IncInstruction ii = (IncInstruction)instruction;
				if (match(ii, ii.value))
					ii.value = this.newInstruction;
				else
					visit(ii.value);
			}
			break;
		case ByteCodeConstants.GETFIELD:
			{
				GetField gf = (GetField)instruction;
				if (match(gf, gf.objectref))
					gf.objectref = this.newInstruction;
				else
					visit(gf.objectref);
			}
			break;
		case ByteCodeConstants.INITARRAY:
		case ByteCodeConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iai = (InitArrayInstruction)instruction;
				if (match(iai, iai.newArray))
				{
					iai.newArray = this.newInstruction;
				}
				else
				{
					visit(iai.newArray);
					
					if ((this.parentFound == null) && (iai.values != null))
						visit(iai.values);
				}
			}
			break;
		case ByteCodeConstants.ACONST_NULL:
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
					"Can not replace DupLoad in " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
	}
	
	private void visit(List<Instruction> instructions)
	{
		for (int i=instructions.size()-1; i>=0; --i)
			visit(instructions.get(i));
	}
	
	/**
	 * @return le dernier parent sur lequel une substitution a �t� faite
	 */
	public Instruction getParentFound() 
	{
		return this.parentFound;
	}

	private boolean match(Instruction parent, Instruction i)
	{
		if ((i.opcode == ByteCodeConstants.GETSTATIC) && 
			(((GetStatic)i).index == this.index))
		{
			this.parentFound = parent;
			return true;
		}
		
		return false;
	}
}
