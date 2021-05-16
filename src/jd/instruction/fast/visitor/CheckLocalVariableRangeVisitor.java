package jd.instruction.fast.visitor;

import java.util.List;

import jd.classfile.LocalVariable;
import jd.classfile.LocalVariables;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.ANewArray;
import jd.instruction.bytecode.instruction.AThrow;
import jd.instruction.bytecode.instruction.ArrayLength;
import jd.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.instruction.bytecode.instruction.AssignmentInstruction;
import jd.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.instruction.bytecode.instruction.CheckCast;
import jd.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.instruction.bytecode.instruction.ConvertInstruction;
import jd.instruction.bytecode.instruction.DupStore;
import jd.instruction.bytecode.instruction.GetField;
import jd.instruction.bytecode.instruction.IfCmp;
import jd.instruction.bytecode.instruction.IfInstruction;
import jd.instruction.bytecode.instruction.IncInstruction;
import jd.instruction.bytecode.instruction.IndexInstruction;
import jd.instruction.bytecode.instruction.InitArrayInstruction;
import jd.instruction.bytecode.instruction.InstanceOf;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.InvokeInstruction;
import jd.instruction.bytecode.instruction.InvokeNew;
import jd.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.instruction.bytecode.instruction.LookupSwitch;
import jd.instruction.bytecode.instruction.MonitorEnter;
import jd.instruction.bytecode.instruction.MonitorExit;
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
import jd.instruction.fast.instruction.FastDeclaration;
import jd.instruction.fast.instruction.FastFor;
import jd.instruction.fast.instruction.FastForEach;
import jd.instruction.fast.instruction.FastInstruction;
import jd.instruction.fast.instruction.FastLabel;
import jd.instruction.fast.instruction.FastList;
import jd.instruction.fast.instruction.FastSwitch;
import jd.instruction.fast.instruction.FastSynchronized;
import jd.instruction.fast.instruction.FastTest2Lists;
import jd.instruction.fast.instruction.FastTestList;
import jd.instruction.fast.instruction.FastTry;
import jd.instruction.fast.instruction.FastTry.FastCatch;

/**
 * Retourne vrai si une variable locale est referencee et dont le domaine de 
 * definition est inclu a [start, end].
 * 
 * CLASSE NON UTILISEE
 */
public class CheckLocalVariableRangeVisitor 
{
	private LocalVariables localVariables;
	private int start;
	private int end;
	private boolean localVariableUsed;
	private boolean externalBlockLocalVariableUsed;

	public CheckLocalVariableRangeVisitor(
		LocalVariables localVariables, int start, int end)
	{
		this.localVariables = localVariables;
		this.start = start;
		this.end = end;
		this.localVariableUsed = false;
		this.externalBlockLocalVariableUsed = false;
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
			{
				ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
				visit(asi.arrayref); 
				visit(asi.indexref);
				visit(asi.valueref);
			}
			break;
		case ByteCodeConstants.ATHROW:
			visit(((AThrow)instruction).value);
			break;
		case ByteCodeConstants.UNARYOP:
			visit(((UnaryOperatorInstruction)instruction).value);
			break;
		case ByteCodeConstants.BINARYOP:
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
		case ByteCodeConstants.LOAD:
		case ByteCodeConstants.ALOAD:
		case ByteCodeConstants.ILOAD:
			{
				this.localVariableUsed = true;
				IndexInstruction ii = (IndexInstruction)instruction;
				LocalVariable lv = 
					this.localVariables.searchLocalVariableWithIndexAndOffset(
						ii.index, ii.offset);
				
				if (lv != null)
				{
					if ((lv.start_pc < this.start) ||
						(this.end < lv.start_pc+lv.length))
						this.externalBlockLocalVariableUsed = true;
				}
			}
			break;
		case ByteCodeConstants.DUPSTORE:
			visit(((DupStore)instruction).objectref);
			break;
		case ByteCodeConstants.CONVERT:
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
			visit(
				((ComplexConditionalBranchInstruction)instruction).instructions);
			break;
		case ByteCodeConstants.INSTANCEOF:
			visit(((InstanceOf)instruction).objectref);
			break;
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
		case ByteCodeConstants.INVOKEVIRTUAL:
			visit(((InvokeNoStaticInstruction)instruction).objectref);
		case ByteCodeConstants.INVOKESTATIC:
			visit(((InvokeInstruction)instruction).args);
			break;
		case ByteCodeConstants.INVOKENEW:
			visit(((InvokeNew)instruction).args);
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
		case ByteCodeConstants.TERNARYOP:	
			{
				TernaryOperator to = (TernaryOperator)instruction;
				visit(to.test);
				visit(to.value1); 
				visit(to.value2);
			}
			break;
		case ByteCodeConstants.ASSIGNMENT:
			{
				AssignmentInstruction ai = (AssignmentInstruction)instruction;
				visit(ai.value1);
				visit(ai.value2);
			}
			break;
		case ByteCodeConstants.ARRAYLOAD:
			{
				ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
				visit(ali.arrayref);
				visit(ali.indexref);
			}
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
		case FastConstants.FOR:
			{
				FastFor ff = (FastFor)instruction;
				if (ff.init != null) 
					visit(ff.init);
				if (ff.inc != null) 
					visit(ff.inc);
			}
		case FastConstants.WHILE:
		case FastConstants.DO_WHILE:
		case FastConstants.IF_:
			{
				FastTestList ftl = (FastTestList)instruction;
				if (ftl.test != null) 
					visit(ftl.test);
			}
		case FastConstants.INFINITE_LOOP:
			visit(((FastList)instruction).instructions);
			break;
		case FastConstants.FOREACH:
			{
				FastForEach ffe = (FastForEach)instruction;
				visit(ffe.variable);
				visit(ffe.values);
				visit(ffe.instructions);
			}
			break;
		case FastConstants.IF_ELSE:
			{
				FastTest2Lists ft2l = (FastTest2Lists)instruction;
				visit(ft2l.test);
				visit(ft2l.instructions);
				visit(ft2l.instructions2);
			}
			break;
		case FastConstants.IF_CONTINUE:
		case FastConstants.IF_BREAK:
		case FastConstants.IF_LABELED_BREAK:
		case FastConstants.GOTO_CONTINUE:
		case FastConstants.GOTO_BREAK:
		case FastConstants.GOTO_LABELED_BREAK:
			visit(((FastInstruction)instruction).instruction);
			break;
		case FastConstants.SWITCH:
		case FastConstants.SWITCH_ENUM:
		case FastConstants.SWITCH_STRING:
			{
				FastSwitch fs = (FastSwitch)instruction;
				visit(fs.test);
				FastSwitch.Pair[] pairs = fs.pairs;
				for (int i=pairs.length-1; i>=0; --i)
					visit(pairs[i].getInstructions());
			}
			break;
		case FastConstants.TRY:
			{
				FastTry ft = (FastTry)instruction;
				visit(ft.instructions);
				if (ft.finallyInstructions != null)
					visit(ft.finallyInstructions);
			
				List<FastCatch> catchs = ft.catches;
				for (int i=catchs.size()-1; i>=0; --i)
					visit(catchs.get(i).instructions);
			}
			break;
		case FastConstants.SYNCHRONIZED:
			{
				FastSynchronized fsd = (FastSynchronized)instruction;
				visit(fsd.monitor);
				visit(fsd.instructions);
			}
			break;
		case FastConstants.LABEL:
			visit(((FastLabel)instruction).instruction);
			break;
		case FastConstants.DECLARE:
			visit(((FastDeclaration)instruction).instruction);
			break;
		case ByteCodeConstants.ACONST_NULL:
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
	
	public boolean IsLocalVariableUsed()
	{
		return this.localVariableUsed;
	}
	
	public boolean IsExternalBlockLocalVariableUsed()
	{	
		return this.externalBlockLocalVariableUsed;
	}
}
