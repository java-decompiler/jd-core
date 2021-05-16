package jd.core.process.analyzer.instruction.fast.visitor;

import java.util.List;

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
import jd.core.model.instruction.bytecode.instruction.DupLoad;
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


public class CountDupLoadVisitor 
{
	private DupStore dupStore;
	private int counter;
	
	public CountDupLoadVisitor()
	{
		init(null);
	}
	
	public void init(DupStore dupStore)
	{
		this.dupStore = dupStore;
		this.counter = 0;
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
				BinaryOperatorInstruction boi = (BinaryOperatorInstruction)instruction;
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
				{
					visit(branchList.get(i));
				}
			}
			break;
		case ByteCodeConstants.INSTANCEOF:
			visit(((InstanceOf)instruction).objectref);
			break;
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
		case ByteCodeConstants.INVOKEVIRTUAL:
			visit(((InvokeNoStaticInstruction)instruction).objectref);
		case ByteCodeConstants.INVOKESTATIC:
			{
				List<Instruction> list = ((InvokeInstruction)instruction).args;
				for (int i=list.size()-1; i>=0; --i)
				{
					visit(list.get(i));
				}
			}
			break;
		case ByteCodeConstants.INVOKENEW:
			{
				List<Instruction> list = ((InvokeNew)instruction).args;
				for (int i=list.size()-1; i>=0; --i)
				{
					visit(list.get(i));
				}
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
				{
					visit(dimensions[i]);
				}
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
		case ByteCodeConstants.DUPLOAD:
			{
				if (((DupLoad)instruction).dupStore == this.dupStore)
					this.counter++;
			}
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
				Instruction test = ((FastTestList)instruction).test;					
				if (test != null)
					visit(test);
			}
		case FastConstants.INFINITE_LOOP:
			{
				List<Instruction> instructions = 
					((FastList)instruction).instructions;
				if (instructions != null)
					visit(instructions);
			}
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
			{
				FastInstruction fi = (FastInstruction)instruction;
				if (fi.instruction != null)
					visit(fi.instruction);
			}
			break;
		case FastConstants.SWITCH:
		case FastConstants.SWITCH_ENUM:
		case FastConstants.SWITCH_STRING:
			{
				FastSwitch fs = (FastSwitch)instruction;
				visit(fs.test);			
				FastSwitch.Pair[] pairs = fs.pairs;
				for (int i=pairs.length-1; i>=0; --i)
				{
					List<Instruction> instructions = pairs[i].getInstructions();
					if (instructions != null)
						visit(instructions);
				}
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
			{
				FastLabel fl = (FastLabel)instruction;
				if (fl.instruction != null)
					visit(fl.instruction);
			}
			break;
		case FastConstants.DECLARE:
			{
				FastDeclaration fd = (FastDeclaration)instruction;
				if (fd.instruction != null)				
					visit(fd.instruction);
			}
			break;
		case ByteCodeConstants.GETSTATIC:
		case ByteCodeConstants.OUTERTHIS:
		case ByteCodeConstants.ACONST_NULL:
		case ByteCodeConstants.LOAD:
		case ByteCodeConstants.ALOAD:
		case ByteCodeConstants.ILOAD:
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
			break;
		default:
			System.err.println(
					"Can not count DupLoad in " + 
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
	 * @return le dernier parent sur lequel une substitution a été faite
	 */
	public int getCounter() 
	{
		return this.counter;
	}
}
