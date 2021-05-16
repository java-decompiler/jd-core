package jd.core.process.analyzer.classfile.visitor;

import java.util.List;

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


/*
 * Utilisé par TernaryOpReconstructor
 */
public class SearchInstructionByOffsetVisitor 
{
	public static Instruction visit(Instruction instruction, int offset)
	{
		if (instruction.offset == offset)
			return instruction;
		
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			return visit(((ArrayLength)instruction).arrayref, offset);
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			return visit(((ArrayStoreInstruction)instruction).arrayref, offset);
		case ByteCodeConstants.ASSERT:
			{
				AssertInstruction ai = (AssertInstruction)instruction;
				instruction = visit(ai.test, offset);
				if (instruction != null)
					return instruction;
				if (ai.msg == null)
					return null;
				return visit(ai.msg, offset);
			}
		case ByteCodeConstants.ATHROW:
			return visit(((AThrow)instruction).value, offset);
		case ByteCodeConstants.UNARYOP:
			return visit(((UnaryOperatorInstruction)instruction).value, offset);
		case ByteCodeConstants.BINARYOP:
		case ByteCodeConstants.ASSIGNMENT:
			{
				BinaryOperatorInstruction boi = 
					(BinaryOperatorInstruction)instruction;
				instruction = visit(boi.value1, offset);
				if (instruction != null)
					return instruction;
				return visit(boi.value2, offset);
			}
		case ByteCodeConstants.CHECKCAST:
			return visit(((CheckCast)instruction).objectref, offset);
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			return visit(((StoreInstruction)instruction).valueref, offset);
		case ByteCodeConstants.DUPSTORE:
			return visit(((DupStore)instruction).objectref, offset);
		case ByteCodeConstants.CONVERT:
		case ByteCodeConstants.IMPLICITCONVERT:
			return visit(((ConvertInstruction)instruction).value, offset);
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ifCmp = (IfCmp)instruction;
				instruction = visit(ifCmp.value1, offset);
				if (instruction != null)
					return instruction;
				return visit(ifCmp.value2, offset);
			}
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFXNULL:
			return visit(((IfInstruction)instruction).value, offset);
		case ByteCodeConstants.COMPLEXIF:
			{
				List<Instruction> branchList = 
					((ComplexConditionalBranchInstruction)instruction).instructions;
				for (int i=branchList.size()-1; i>=0; --i)
				{
					instruction = visit(branchList.get(i), offset);
					if (instruction != null)
						return instruction;
				}
			}
			break;
		case ByteCodeConstants.INSTANCEOF:
			return visit(((InstanceOf)instruction).objectref, offset);
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
		case ByteCodeConstants.INVOKEVIRTUAL:
			{
				Instruction result = visit(
					((InvokeNoStaticInstruction)instruction).objectref, offset);
				if (result != null)
					return result;
			}
		case ByteCodeConstants.INVOKESTATIC:
		case ByteCodeConstants.INVOKENEW:
			{
				List<Instruction> list = ((InvokeInstruction)instruction).args;
				for (int i=list.size()-1; i>=0; --i)
				{
					instruction = visit(list.get(i), offset);
					if (instruction != null)
						return instruction;
				}
			}
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
			return visit(((LookupSwitch)instruction).key, offset);
		case ByteCodeConstants.MONITORENTER:
			return visit(((MonitorEnter)instruction).objectref, offset);
		case ByteCodeConstants.MONITOREXIT:
			return visit(((MonitorExit)instruction).objectref, offset);
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				Instruction[] dimensions = ((MultiANewArray)instruction).dimensions;
				for (int i=dimensions.length-1; i>=0; --i)
				{
					instruction = visit(dimensions[i], offset);
					if (instruction != null)
						return instruction;
				}
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			return visit(((NewArray)instruction).dimension, offset);
		case ByteCodeConstants.ANEWARRAY:
			return visit(((ANewArray)instruction).dimension, offset);
		case ByteCodeConstants.POP:
			return visit(((Pop)instruction).objectref, offset);
		case ByteCodeConstants.PUTFIELD: 
			{
				PutField putField = (PutField)instruction;
				instruction = visit(putField.objectref, offset);
				if (instruction != null)
					return instruction;
				return visit(putField.valueref, offset);
			}
		case ByteCodeConstants.PUTSTATIC:
			return visit(((PutStatic)instruction).valueref, offset);
		case ByteCodeConstants.XRETURN:
			return visit(((ReturnInstruction)instruction).valueref, offset);
		case ByteCodeConstants.TABLESWITCH:
			return visit(((TableSwitch)instruction).key, offset);
		case ByteCodeConstants.TERNARYOPSTORE:
			return visit(((TernaryOpStore)instruction).objectref, offset);
		case ByteCodeConstants.PREINC:			
		case ByteCodeConstants.POSTINC:	
			return visit(((IncInstruction)instruction).value, offset);
		case ByteCodeConstants.GETFIELD:
			return visit(((GetField)instruction).objectref, offset);
		case ByteCodeConstants.INITARRAY:
		case ByteCodeConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iai = (InitArrayInstruction)instruction;
				instruction = visit(iai.newArray, offset);
				if (instruction != null)
					return instruction;
				if (iai.values != null)
					return visit(iai.values, offset);
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
					"Can not search instruction in " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
		
		return null;
	}

	private static Instruction visit(List<Instruction> instructions, int offset)
	{
		for (int i=instructions.size()-1; i>=0; --i)
		{
			Instruction instruction = visit(instructions.get(i), offset);
			if (instruction != null)
				return instruction;
		}
		
		return null;
	}
}
