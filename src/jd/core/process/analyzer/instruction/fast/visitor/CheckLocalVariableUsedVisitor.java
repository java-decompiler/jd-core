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
package jd.core.process.analyzer.instruction.fast.visitor;

import java.util.List;

import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
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
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;
import jd.core.model.instruction.fast.instruction.FastFor;
import jd.core.model.instruction.fast.instruction.FastForEach;
import jd.core.model.instruction.fast.instruction.FastInstruction;
import jd.core.model.instruction.fast.instruction.FastLabel;
import jd.core.model.instruction.fast.instruction.FastList;
import jd.core.model.instruction.fast.instruction.FastSwitch;
import jd.core.model.instruction.fast.instruction.FastSynchronized;
import jd.core.model.instruction.fast.instruction.FastTest2Lists;
import jd.core.model.instruction.fast.instruction.FastTestList;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;


public class CheckLocalVariableUsedVisitor 
{
	public static boolean Visit(
		LocalVariables localVariables, int maxOffset, Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			return Visit(
				localVariables, maxOffset, ((ArrayLength)instruction).arrayref);
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			{
				ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
				if (Visit(localVariables, maxOffset, asi.indexref))
					return true;
				return Visit(localVariables, maxOffset, asi.valueref);
			}
		case ByteCodeConstants.ATHROW:
			return Visit(localVariables, maxOffset, ((AThrow)instruction).value);
		case ByteCodeConstants.UNARYOP:
			return Visit(
				localVariables, maxOffset, 
				((UnaryOperatorInstruction)instruction).value);
		case ByteCodeConstants.BINARYOP:
			{
				BinaryOperatorInstruction boi = 
					(BinaryOperatorInstruction)instruction;
				if (Visit(localVariables, maxOffset, boi.value1))
					return true;
				return Visit(localVariables, maxOffset, boi.value2);
			}
		case ByteCodeConstants.CHECKCAST:
			return Visit(
				localVariables, maxOffset, ((CheckCast)instruction).objectref);
		case ByteCodeConstants.LOAD:
		case ByteCodeConstants.ALOAD:
		case ByteCodeConstants.ILOAD:
			{
				LoadInstruction li = (LoadInstruction)instruction;				
				LocalVariable lv = 
					localVariables.getLocalVariableWithIndexAndOffset(
						li.index, li.offset);
				return (lv != null) && (maxOffset <= lv.start_pc);
			}
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			{
				StoreInstruction si = (StoreInstruction)instruction;
				LocalVariable lv = 
					localVariables.getLocalVariableWithIndexAndOffset(
						si.index, si.offset);
				if ((lv != null) && (maxOffset <= lv.start_pc))
					return true;
				return Visit(localVariables, maxOffset, si.valueref);
			}
		case ByteCodeConstants.DUPSTORE:
			return Visit(
				localVariables, maxOffset, ((DupStore)instruction).objectref);
		case ByteCodeConstants.CONVERT:
		case ByteCodeConstants.IMPLICITCONVERT:
			return Visit(
				localVariables, maxOffset, 
				((ConvertInstruction)instruction).value);
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ifCmp = (IfCmp)instruction;
				if (Visit(localVariables, maxOffset, ifCmp.value1))
					return true;
				return Visit(localVariables, maxOffset, ifCmp.value2);
			}
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFXNULL:
			return Visit(
				localVariables, maxOffset, ((IfInstruction)instruction).value);
		case ByteCodeConstants.COMPLEXIF:
			{
				List<Instruction> branchList = 
					((ComplexConditionalBranchInstruction)instruction).instructions;
				for (int i=branchList.size()-1; i>=0; --i)
				{
					if (Visit(localVariables, maxOffset, branchList.get(i)))
						return true;
				}
				return false;
			}
		case ByteCodeConstants.INSTANCEOF:
			return Visit(
				localVariables, maxOffset, ((InstanceOf)instruction).objectref);
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
		case ByteCodeConstants.INVOKEVIRTUAL:
			if (Visit(
					localVariables, maxOffset, 
					((InvokeNoStaticInstruction)instruction).objectref))
				return true;
		case ByteCodeConstants.INVOKESTATIC:
			{
				List<Instruction> list = ((InvokeInstruction)instruction).args;
				for (int i=list.size()-1; i>=0; --i)
				{
					if (Visit(localVariables, maxOffset, list.get(i)))
						return true;
				}
				return false;
			}
		case ByteCodeConstants.INVOKENEW:
			{
				List<Instruction> list = ((InvokeNew)instruction).args;
				for (int i=list.size()-1; i>=0; --i)
				{
					if (Visit(localVariables, maxOffset, list.get(i)))
						return true;
				}
				return false;
			}
		case ByteCodeConstants.LOOKUPSWITCH:
			return Visit(
				localVariables, maxOffset, ((LookupSwitch)instruction).key);
		case ByteCodeConstants.MONITORENTER:
			return Visit(
				localVariables, maxOffset, 
				((MonitorEnter)instruction).objectref);
		case ByteCodeConstants.MONITOREXIT:
			return Visit(
				localVariables, maxOffset, 
				((MonitorExit)instruction).objectref);
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				Instruction[] dimensions = ((MultiANewArray)instruction).dimensions;
				for (int i=dimensions.length-1; i>=0; --i)
				{
					if (Visit(localVariables, maxOffset, dimensions[i]))
						return true;
				}
				return false;
			}
		case ByteCodeConstants.NEWARRAY:
			return Visit(
				localVariables, maxOffset, 
				((NewArray)instruction).dimension);
		case ByteCodeConstants.ANEWARRAY:
			return Visit(
				localVariables, maxOffset, 
				((ANewArray)instruction).dimension);
		case ByteCodeConstants.POP:
			return Visit(
				localVariables, maxOffset, 
				((Pop)instruction).objectref);
		case ByteCodeConstants.PUTFIELD:
			{
				PutField putField = (PutField)instruction;
				if (Visit(localVariables, maxOffset, putField.objectref))
					return true;
				return Visit(localVariables, maxOffset, putField.valueref);
			}
		case ByteCodeConstants.PUTSTATIC:
			return Visit(
				localVariables, maxOffset, 
				((PutStatic)instruction).valueref);
		case ByteCodeConstants.XRETURN:
			return Visit(
				localVariables, maxOffset, 
				((ReturnInstruction)instruction).valueref);
		case ByteCodeConstants.TABLESWITCH:
			return Visit(
				localVariables, maxOffset, 
				((TableSwitch)instruction).key);
		case ByteCodeConstants.TERNARYOPSTORE:
			return Visit(
				localVariables, maxOffset, 
				((TernaryOpStore)instruction).objectref);	
		case ByteCodeConstants.TERNARYOP:	
			{
				TernaryOperator to = (TernaryOperator)instruction;
				if (Visit(localVariables, maxOffset, to.value1))
					return true;	
				return Visit(localVariables, maxOffset, to.value2);
			}			
		case ByteCodeConstants.ASSIGNMENT:
			{
				AssignmentInstruction ai = (AssignmentInstruction)instruction;
				if (Visit(localVariables, maxOffset, ai.value1))
					return true;	
				return Visit(localVariables, maxOffset, ai.value2);
			}
		case ByteCodeConstants.ARRAYLOAD:
			{
				ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
				if (Visit(localVariables, maxOffset, ali.arrayref))
					return true;	
				return Visit(localVariables, maxOffset, ali.indexref);
			}
		case ByteCodeConstants.PREINC:			
		case ByteCodeConstants.POSTINC:			
			return Visit(
				localVariables, maxOffset, 
				((IncInstruction)instruction).value);
		case ByteCodeConstants.GETFIELD:
			return Visit(
				localVariables, maxOffset, 
				((GetField)instruction).objectref);
		case ByteCodeConstants.INITARRAY:
		case ByteCodeConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iai = (InitArrayInstruction)instruction;
				if (Visit(localVariables, maxOffset, iai.newArray))
					return true;	
				if (iai.values != null)
					if (visit(localVariables, maxOffset, iai.values))
						return true;	
				return false;	
			}
		case FastConstants.FOR:
			{
				FastFor ff = (FastFor)instruction;
				if (ff.init != null)
					if (Visit(localVariables, maxOffset, ff.init))
						return true;	
				if (ff.inc != null)
					if (Visit(localVariables, maxOffset, ff.inc))
						return true;	
				return false;	
			}
		case FastConstants.WHILE:
		case FastConstants.DO_WHILE:
		case FastConstants.IF_:
			{
				Instruction test = ((FastTestList)instruction).test;					
				if (test != null)
					if (Visit(localVariables, maxOffset, test))
						return true;	
				return false;	
			}
		case FastConstants.INFINITE_LOOP:
			{
				List<Instruction> instructions = 
						((FastList)instruction).instructions;
				if (instructions != null)
					return visit(localVariables, maxOffset, instructions);
				return false;	
			}			
		case FastConstants.FOREACH:
			{
				FastForEach ffe = (FastForEach)instruction;				
				if (Visit(localVariables, maxOffset, ffe.variable))
					return true;	
				if (Visit(localVariables, maxOffset, ffe.values))
					return true;	
				return visit(localVariables, maxOffset, ffe.instructions);
			}
		case FastConstants.IF_ELSE:
			{
				FastTest2Lists ft2l = (FastTest2Lists)instruction;
				if (Visit(localVariables, maxOffset, ft2l.test))
					return true;	
				if (visit(localVariables, maxOffset, ft2l.instructions))
					return true;	
				return visit(localVariables, maxOffset, ft2l.instructions2);
			}
		case FastConstants.IF_CONTINUE:
		case FastConstants.IF_BREAK:
		case FastConstants.IF_LABELED_BREAK:
		case FastConstants.GOTO_CONTINUE:
		case FastConstants.GOTO_BREAK:
		case FastConstants.GOTO_LABELED_BREAK:
			{
				FastInstruction fi = (FastInstruction)instruction;
				if (fi.instruction != null)
					if (Visit(localVariables, maxOffset, fi.instruction))
						return true;	
				return false;
			}
		case FastConstants.SWITCH:
		case FastConstants.SWITCH_ENUM:
		case FastConstants.SWITCH_STRING:
			{
				FastSwitch fs = (FastSwitch)instruction;
				if (Visit(localVariables, maxOffset, fs.test))
					return true;			
				FastSwitch.Pair[] pairs = fs.pairs;
				for (int i=pairs.length-1; i>=0; --i)
				{
					List<Instruction> instructions = pairs[i].getInstructions();
					if (instructions != null)
						if (visit(localVariables, maxOffset, instructions))
							return true;			
				}
				return false;
			}
		case FastConstants.TRY:
			{
				FastTry ft = (FastTry)instruction;
				if (visit(localVariables, maxOffset, ft.instructions))
					return true;						
				if (ft.finallyInstructions != null)
					if (visit(localVariables, maxOffset, ft.finallyInstructions))
						return true;						
				List<FastCatch> catchs = ft.catches;
				for (int i=catchs.size()-1; i>=0; --i)
					if (visit(localVariables, maxOffset, catchs.get(i).instructions))
						return true;			
				return false;
			}
		case FastConstants.SYNCHRONIZED:
			{
				FastSynchronized fsd = (FastSynchronized)instruction;
				if (Visit(localVariables, maxOffset, fsd.monitor))
					return true;				
				return visit(localVariables, maxOffset, fsd.instructions);
			}
		case FastConstants.LABEL:
			{
				FastLabel fl = (FastLabel)instruction;
				if (fl.instruction != null)
					if (Visit(localVariables, maxOffset, fl.instruction))
						return true;				
				return false;
			}
		case FastConstants.DECLARE:
			{
				FastDeclaration fd = (FastDeclaration)instruction;
				if (fd.instruction != null)				
					if (Visit(localVariables, maxOffset, fd.instruction))
						return true;				
				return false;
			}
		case ByteCodeConstants.GETSTATIC:
		case ByteCodeConstants.OUTERTHIS:
		case ByteCodeConstants.ACONST_NULL:
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.LCONST:
		case ByteCodeConstants.FCONST:
		case ByteCodeConstants.DCONST:
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
		case ByteCodeConstants.DUPLOAD:
			return false;
		default:
			System.err.println(
					"Can not find local variable used in " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
			return false;
		}
	}
	
	private static boolean visit(
		LocalVariables localVariables, int maxOffset, 
		List<Instruction> instructions)
	{
		for (int i=instructions.size()-1; i>=0; --i)
			if (Visit(localVariables, maxOffset, instructions.get(i)))
				return true;				
		return false;
	}
}
