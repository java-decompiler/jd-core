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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.accessor.Accessor;
import jd.core.model.classfile.accessor.AccessorConstants;
import jd.core.model.classfile.accessor.GetStaticAccessor;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
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
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
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

/*
 * Replace 'EntitlementFunctionLibrary.access$000()' 
 * par 'EntitlementFunctionLibrary.kernelId'
 */
public class OuterGetStaticVisitor 
{
	protected Map<String, ClassFile> innerClassesMap;
	protected ConstantPool constants;
	
	public OuterGetStaticVisitor(
		HashMap<String, ClassFile> innerClassesMap, ConstantPool constants)
	{
		this.innerClassesMap = innerClassesMap;
		this.constants = constants;
	}
	
	public void visit(Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			{
				ArrayLength al = (ArrayLength)instruction;
				Accessor a = match(al.arrayref);
				if (a != null)
					al.arrayref = newInstruction(al.arrayref, a);
				else
					visit(al.arrayref);
			}
			break;
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			{
				ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
				Accessor a = match(asi.arrayref);
				if (a != null)
					asi.arrayref = newInstruction(asi.arrayref, a);
				else
					visit(asi.arrayref);
				a = match(asi.indexref);
				if (a != null)
					asi.indexref = newInstruction(asi.indexref, a);
				else
					visit(asi.indexref);
				a = match(asi.valueref);
				if (a != null)
					asi.valueref = newInstruction(asi.valueref, a);
				else
					visit(asi.valueref);
			}
			break;
		case ByteCodeConstants.ASSERT:
			{
				AssertInstruction ai = (AssertInstruction)instruction;
				Accessor a = match(ai.test);
				if (a != null)
					ai.test = newInstruction(ai.test, a);
				else
					visit(ai.test);
				if (ai.msg != null)
				{
					a = match(ai.msg);
					if (a != null)
						ai.msg = newInstruction(ai.msg, a);
					else
						visit(ai.msg);	
				}
			}
			break;
		case ByteCodeConstants.ATHROW:
			{
				AThrow aThrow = (AThrow)instruction;
				Accessor a = match(aThrow.value);
				if (a != null)
					aThrow.value = newInstruction(aThrow.value, a);
				else
					visit(aThrow.value);
			}
			break;
		case ByteCodeConstants.UNARYOP:
			{
				UnaryOperatorInstruction uoi = (UnaryOperatorInstruction)instruction;
				Accessor a = match(uoi.value);
				if (a != null)
					uoi.value = newInstruction(uoi.value, a);
				else
					visit(uoi.value);
			}
			break;
		case ByteCodeConstants.BINARYOP:
			{
				BinaryOperatorInstruction boi = (BinaryOperatorInstruction)instruction;
				Accessor a = match(boi.value1);
				if (a != null)
					boi.value1 = newInstruction(boi.value1, a);
				else
					visit(boi.value1);
				a = match(boi.value2);
				if (a != null)
					boi.value2 = newInstruction(boi.value2, a);
				else
					visit(boi.value2);
			}
			break;
		case ByteCodeConstants.CHECKCAST:
			{
				CheckCast checkCast = (CheckCast)instruction;
				Accessor a = match(checkCast.objectref);
				if (a != null)
					checkCast.objectref = newInstruction(checkCast.objectref, a);
				else
					visit(checkCast.objectref);
			}
			break;
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			{
				StoreInstruction storeInstruction = (StoreInstruction)instruction;
				Accessor a = match(storeInstruction.valueref);
				if (a != null)
					storeInstruction.valueref = newInstruction(storeInstruction.valueref, a);
				else
					visit(storeInstruction.valueref);
			}
			break;
		case ByteCodeConstants.DUPSTORE:
			{
				DupStore dupStore = (DupStore)instruction;
				Accessor a = match(dupStore.objectref);
				if (a != null)
					dupStore.objectref = newInstruction(dupStore.objectref, a);
				else
					visit(dupStore.objectref);
			}
			break;
		case ByteCodeConstants.CONVERT:
		case ByteCodeConstants.IMPLICITCONVERT:
			{
				ConvertInstruction ci = (ConvertInstruction)instruction;
				Accessor a = match(ci.value);
				if (a != null)
					ci.value = newInstruction(ci.value, a);
				else
					visit(ci.value);
			}
			break;
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ifCmp = (IfCmp)instruction;
				Accessor a = match(ifCmp.value1);
				if (a != null)
					ifCmp.value1 = newInstruction(ifCmp.value1, a);
				else
					visit(ifCmp.value1);
				a = match(ifCmp.value2);
				if (a != null)
					ifCmp.value2 = newInstruction(ifCmp.value2, a);
				else
					visit(ifCmp.value2);
			}
			break;
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFXNULL:
			{
				IfInstruction iff = (IfInstruction)instruction;
				Accessor a = match(iff.value);
				if (a != null)
					iff.value = newInstruction(iff.value, a);
				else
					visit(iff.value);
			}
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
			{
				InstanceOf instanceOf = (InstanceOf)instruction;
				Accessor a = match(instanceOf.objectref);
				if (a != null)
					instanceOf.objectref = newInstruction(instanceOf.objectref, a);
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
				Accessor a = match(insi.objectref);
				if (a != null)
					insi.objectref = newInstruction(insi.objectref, a);
				else
					visit(insi.objectref);
			}
		case ByteCodeConstants.INVOKESTATIC:
		case ByteCodeConstants.INVOKENEW:
			{
				List<Instruction> list = ((InvokeInstruction)instruction).args;
				for (int i=list.size()-1; i>=0; --i)
				{
					Accessor a = match(list.get(i));
					if (a != null)
						list.set(i, newInstruction(list.get(i), a));
					else
						visit(list.get(i));
				}
			}
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
			{
				LookupSwitch ls = (LookupSwitch)instruction;
				Accessor a = match(ls.key);
				if (a != null)
					ls.key = newInstruction(ls.key, a);
				else
					visit(ls.key);
			}
			break;			
		case ByteCodeConstants.MONITORENTER:
			{
				MonitorEnter monitorEnter = (MonitorEnter)instruction;
				Accessor a = match(monitorEnter.objectref);
				if (a != null)
					monitorEnter.objectref = newInstruction(monitorEnter.objectref, a);
				else
					visit(monitorEnter.objectref);
			}
			break;
		case ByteCodeConstants.MONITOREXIT:
			{
				MonitorExit monitorExit = (MonitorExit)instruction;
				Accessor a = match(monitorExit.objectref);
				if (a != null)
					monitorExit.objectref = newInstruction(monitorExit.objectref, a);
				else
					visit(monitorExit.objectref);
			}
			break;
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				Instruction[] dimensions = ((MultiANewArray)instruction).dimensions;
				for (int i=dimensions.length-1; i>=0; --i)
				{
					Accessor a = match(dimensions[i]);
					if (a != null)
						dimensions[i] = newInstruction(dimensions[i], a);
					else
						visit(dimensions[i]);
				}
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			{
				NewArray newArray = (NewArray)instruction;
				Accessor a = match(newArray.dimension);
				if (a != null)
					newArray.dimension = newInstruction(newArray.dimension, a);
				else
					visit(newArray.dimension);
			}
			break;
		case ByteCodeConstants.ANEWARRAY:
			{
				ANewArray aNewArray = (ANewArray)instruction;
				Accessor a = match(aNewArray.dimension);
				if (a != null)
					aNewArray.dimension = newInstruction(aNewArray.dimension, a);
				else
					visit(aNewArray.dimension);
			}
			break;
		case ByteCodeConstants.POP:
			{
				Pop pop = (Pop)instruction;
				Accessor a = match(pop.objectref);
				if (a != null)
					pop.objectref = newInstruction(pop.objectref, a);
				else
					visit(pop.objectref);
			}
			break;
		case ByteCodeConstants.PUTFIELD:
			{
				PutField putField = (PutField)instruction;
				Accessor a = match(putField.objectref);
				if (a != null)
					putField.objectref = newInstruction(putField.objectref, a);
				else
					visit(putField.objectref);
				a = match(putField.valueref);
				if (a != null)
					putField.valueref = newInstruction(putField.valueref, a);
				else
					visit(putField.valueref);
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			{
				PutStatic putStatic = (PutStatic)instruction;
				Accessor a = match(putStatic.valueref);
				if (a != null)
					putStatic.valueref = newInstruction(putStatic.valueref, a);
				else
					visit(putStatic.valueref);
			}
			break;
		case ByteCodeConstants.XRETURN:
			{
				ReturnInstruction ri = (ReturnInstruction)instruction;
				Accessor a = match(ri.valueref);
				if (a != null)
					ri.valueref = newInstruction(ri.valueref, a);
				else
					visit(ri.valueref);
			}
			break;			
		case ByteCodeConstants.TABLESWITCH:
			{
				TableSwitch ts = (TableSwitch)instruction;
				Accessor a = match(ts.key);
				if (a != null)
					ts.key = newInstruction(ts.key, a);
				else
					visit(ts.key);
			}
			break;			
		case ByteCodeConstants.TERNARYOPSTORE:
			{
				TernaryOpStore tos = (TernaryOpStore)instruction;
				Accessor a = match(tos.objectref);
				if (a != null)
					tos.objectref = newInstruction(tos.objectref, a);
				else
					visit(tos.objectref);
			}
			break;		
		case ByteCodeConstants.TERNARYOP:	
			{
				TernaryOperator to = (TernaryOperator)instruction;
				Accessor a = match(to.test);
				if (a != null)
					to.test = newInstruction(to.test, a);
				else
					visit(to.test);
				a = match(to.value1);
				if (a != null)
					to.value1 = newInstruction(to.value1, a);
				else
					visit(to.value1);
				a = match(to.value2);
				if (a != null)
					to.value2 = newInstruction(to.value2, a);
				else
					visit(to.value2);
			}
			break;	
		case ByteCodeConstants.ASSIGNMENT:
			{
				AssignmentInstruction ai = (AssignmentInstruction)instruction;
				Accessor a = match(ai.value1);
				if (a != null)
					ai.value1 = newInstruction(ai.value1, a);
				else
					visit(ai.value1);
				a = match(ai.value2);
				if (a != null)
					ai.value2 = newInstruction(ai.value2, a);
				else
					visit(ai.value2);
			}
			break;
		case ByteCodeConstants.ARRAYLOAD:
			{
				ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
				Accessor a = match(ali.arrayref);
				if (a != null)
					ali.arrayref = newInstruction(ali.arrayref, a);
				else
					visit(ali.arrayref);
				a = match(ali.indexref);
				if (a != null)
					ali.indexref = newInstruction(ali.indexref, a);
				else
					visit(ali.indexref);
			}
			break;
		case ByteCodeConstants.PREINC:			
		case ByteCodeConstants.POSTINC:		
			{
				IncInstruction ii = (IncInstruction)instruction;
				Accessor a = match(ii.value);
				if (a != null)
					ii.value = newInstruction(ii.value, a);
				else
					visit(ii.value);
			}
			break;
		case ByteCodeConstants.GETFIELD:
			{
				GetField gf = (GetField)instruction;
				Accessor a = match(gf.objectref);
				if (a != null)
					gf.objectref = newInstruction(gf.objectref, a);
				else
					visit(gf.objectref);
			}
			break;
		case ByteCodeConstants.INITARRAY:
		case ByteCodeConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iai = (InitArrayInstruction)instruction;
				Accessor a = match(iai.newArray);
				if (a != null)
					iai.newArray = newInstruction(iai.newArray, a);
				else
					visit(iai.newArray);
				if (iai.values != null)
					visit(iai.values);
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
					"Can not replace accessor in " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
	}
	
	public void visit(List<Instruction> instructions)
	{
		for (int index=instructions.size()-1; index>=0; --index)
		{
			Instruction i = instructions.get(index);
			Accessor a = match(i);
			
			if (a != null)
				instructions.set(index, newInstruction(i, a));
			else
				visit(i);
		}
	}

	protected Accessor match(Instruction i)
	{
		if (i.opcode != ByteCodeConstants.INVOKESTATIC)		
			return null; 
		
		Invokestatic is = (Invokestatic)i;
		ConstantMethodref cmr = 
			constants.getConstantMethodref(is.index);
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cmr.name_and_type_index);
		String descriptor = 
			constants.getConstantUtf8(cnat.descriptor_index);

		// Zero parameter ?
		if (descriptor.charAt(1) != ')')
			return null;

		String className = constants.getConstantClassName(cmr.class_index);		
		ClassFile classFile = this.innerClassesMap.get(className);
		if (classFile == null)
			return null;
		
		String name = 
			constants.getConstantUtf8(cnat.name_index);
		
		Accessor accessor = classFile.getAccessor(name, descriptor);
		
		if ((accessor == null) ||
			(accessor.tag != AccessorConstants.ACCESSOR_GETSTATIC))
			return null;
		
		return (GetStaticAccessor)accessor;
	}
	
	protected Instruction newInstruction(Instruction i, Accessor a)
	{
		GetStaticAccessor gsa = (GetStaticAccessor)a;
		
		int nameIndex = this.constants.addConstantUtf8(gsa.fieldName);
		int descriptorIndex = 
			this.constants.addConstantUtf8(gsa.fieldDescriptor);
		int cnatIndex = 
			this.constants.addConstantNameAndType(nameIndex, descriptorIndex);
		
		int classNameIndex = this.constants.addConstantUtf8(gsa.className);	
		int classIndex = this.constants.addConstantClass(classNameIndex);
		
		int cfrIndex = 
			constants.addConstantFieldref(classIndex, cnatIndex);
		
		return new GetStatic(
			ByteCodeConstants.GETSTATIC, i.offset, i.lineNumber, cfrIndex);
	}
}
