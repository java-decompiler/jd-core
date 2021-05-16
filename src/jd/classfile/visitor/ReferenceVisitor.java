package jd.classfile.visitor;

import java.util.List;

import jd.Constants;
import jd.classfile.ConstantPool;
import jd.classfile.analyzer.SignatureAnalyzer;
import jd.classfile.constant.Constant;
import jd.classfile.constant.ConstantClass;
import jd.classfile.constant.ConstantFieldref;
import jd.classfile.constant.ConstantMethodref;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.ANewArray;
import jd.instruction.bytecode.instruction.AThrow;
import jd.instruction.bytecode.instruction.ArrayLength;
import jd.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.instruction.bytecode.instruction.AssertInstruction;
import jd.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.instruction.bytecode.instruction.CheckCast;
import jd.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.instruction.bytecode.instruction.ConvertInstruction;
import jd.instruction.bytecode.instruction.DupStore;
import jd.instruction.bytecode.instruction.GetField;
import jd.instruction.bytecode.instruction.IfCmp;
import jd.instruction.bytecode.instruction.IfInstruction;
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
import jd.instruction.bytecode.instruction.New;
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
import jd.instruction.fast.instruction.FastSwitch.Pair;
import jd.instruction.fast.instruction.FastTry.FastCatch;
import jd.util.ReferenceMap;


public class ReferenceVisitor 
{
	private ConstantPool constants;
	private ReferenceMap referenceMap;
	
	public ReferenceVisitor(ConstantPool constants, ReferenceMap referenceMap)
	{
		this.constants = constants;
		this.referenceMap = referenceMap;
	}
	
	public void visit(Instruction instruction)
	{
		String internalName;
		
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			{
				ArrayLength al = (ArrayLength)instruction;
				visit(al.arrayref);
			}
			break;
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			{
				ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
				visit(asi.valueref);
			}
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
			{
				AThrow aThrow = (AThrow)instruction;
				visit(aThrow.value);
			}
			break;
		case ByteCodeConstants.UNARYOP:
			{
				UnaryOperatorInstruction uoi = (UnaryOperatorInstruction)instruction;
				visit(uoi.value);
			}
			break;
		case ByteCodeConstants.BINARYOP:
		case ByteCodeConstants.ASSIGNMENT:	
			{
				BinaryOperatorInstruction boi = (BinaryOperatorInstruction)instruction;
				visit(boi.value1);
				visit(boi.value2);
			}
			break;
		case ByteCodeConstants.CHECKCAST:
			{
				CheckCast checkCast = (CheckCast)instruction;	
				visitCheckCastAndMultiANewArray(checkCast.index);
				visit(checkCast.objectref);
			}
			break;
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ISTORE:
			{
				StoreInstruction storeInstruction = (StoreInstruction)instruction;
				visit(storeInstruction.valueref);
			}
			break;
		case ByteCodeConstants.ASTORE:
			{
				StoreInstruction storeInstruction = (StoreInstruction)instruction;
				visit(storeInstruction.valueref);
			}
			break;
		case ByteCodeConstants.DUPSTORE:
			{
				DupStore dupStore = (DupStore)instruction;
				visit(dupStore.objectref);
			}
			break;
		case ByteCodeConstants.CONVERT:
			{
				ConvertInstruction ci = (ConvertInstruction)instruction;
				visit(ci.value);
			}
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
			{
				IfInstruction iff = (IfInstruction)instruction;
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
				visitCheckCastAndMultiANewArray(instanceOf.index);
				visit(instanceOf.objectref);
			}
			break;
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
		case ByteCodeConstants.INVOKEVIRTUAL:
			InvokeNoStaticInstruction insi = 
				(InvokeNoStaticInstruction)instruction;
				visit(insi.objectref);
		case ByteCodeConstants.INVOKESTATIC:
			{
				InvokeInstruction ii = (InvokeInstruction)instruction;
				ConstantMethodref cmr = constants.getConstantMethodref(ii.index);
				internalName = constants.getConstantClassName(cmr.class_index);
				String innerName = SignatureAnalyzer.GetInnerName(internalName);
				addReference(innerName);					
				visit(ii.args);
			}
			break;			
		case ByteCodeConstants.INVOKENEW:
			{
				InvokeNew in = (InvokeNew)instruction;
				internalName = constants.getConstantClassName(in.classIndex);
				addReference(internalName);	
				visit(in.args);
			}
			break;			
		case ByteCodeConstants.LOOKUPSWITCH:
			{
				LookupSwitch ls = (LookupSwitch)instruction;
				visit(ls.key);
			}
			break;			
		case ByteCodeConstants.MONITORENTER:
			{
				MonitorEnter monitorEnter = (MonitorEnter)instruction;
				visit(monitorEnter.objectref);
			}
			break;
		case ByteCodeConstants.MONITOREXIT:
			{
				MonitorExit monitorExit = (MonitorExit)instruction;
				visit(monitorExit.objectref);
			}
			break;
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				MultiANewArray multiANewArray = (MultiANewArray)instruction;	
				visitCheckCastAndMultiANewArray(multiANewArray.index);
				Instruction[] dimensions = multiANewArray.dimensions;
				for (int i=dimensions.length-1; i>=0; --i)
					visit(dimensions[i]);
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			{
				NewArray newArray = (NewArray)instruction;
				visit(newArray.dimension);
			}
			break;
		case ByteCodeConstants.NEW:
			{
				New aNew = (New)instruction;
				addReference(this.constants.getConstantClassName(aNew.index));
			}
			break;
		case ByteCodeConstants.ANEWARRAY:
			{
				ANewArray aNewArray = (ANewArray)instruction;	
				addReference(
					this.constants.getConstantClassName(aNewArray.index));				
				visit(aNewArray.dimension);
			}
			break;
		case ByteCodeConstants.POP:
			{
				Pop pop = (Pop)instruction;
				visit(pop.objectref);
			}
			break;
		case ByteCodeConstants.PUTFIELD:
			{
				PutField putField = (PutField)instruction;
				visit(putField.objectref);
				visit(putField.valueref);
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			{
				PutStatic putStatic = (PutStatic)instruction;
				visit(putStatic.valueref);
			}
			break;
		case ByteCodeConstants.XRETURN:
			{
				ReturnInstruction ri = (ReturnInstruction)instruction;
				visit(ri.valueref);
			}
			break;			
		case ByteCodeConstants.TABLESWITCH:
			{
				TableSwitch ts = (TableSwitch)instruction;
				visit(ts.key);
			}
			break;			
		case ByteCodeConstants.TERNARYOPSTORE:
			{
				TernaryOpStore tos = (TernaryOpStore)instruction;
				visit(tos.objectref);
			}
			break;			
		case ByteCodeConstants.TERNARYOP:	
			{
				TernaryOperator to = (TernaryOperator)instruction;
				visit(to.test);	
				visit(to.value1);	
				visit(to.value2);
			}
			break;
		case ByteCodeConstants.GETFIELD:
			GetField getField = (GetField)instruction;
			visit(getField.objectref);
		case ByteCodeConstants.GETSTATIC:
		case ByteCodeConstants.OUTERTHIS:
			{
				IndexInstruction indexInstruction = (IndexInstruction)instruction;
				ConstantFieldref cfr = 
					constants.getConstantFieldref(indexInstruction.index);
				internalName = constants.getConstantClassName(cfr.class_index);
				addReference(internalName);
			}
			break;		
		case ByteCodeConstants.INITARRAY:
		case ByteCodeConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iai = (InitArrayInstruction)instruction;
				visit(iai.newArray);			
				for (int index=iai.values.size()-1; index>=0; --index)
					visit(iai.values.get(index));
			}
			break;			
		case FastConstants.FOR:
			FastFor ff = (FastFor)instruction;	
			if (ff.init != null)
				visit(ff.init);	
			if (ff.inc != null)
				visit(ff.inc);	
		case FastConstants.WHILE:
		case FastConstants.DO_WHILE:
		case FastConstants.IF_:
			FastTestList ftl = (FastTestList)instruction;
			if (ftl.test != null)
				visit(ftl.test);	
		case FastConstants.INFINITE_LOOP:
			{
				FastList fl = (FastList)instruction;	
				if (fl.instructions != null)
					visit(fl.instructions);
			}
			break;		
		case FastConstants.FOREACH:
			{
				FastForEach ffe = (FastForEach)instruction;	
				visit(ffe.variable);
				visit(ffe.values);				
				if (ffe.instructions != null)
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
				Pair[] pairs = fs.pairs;
				for (int i=pairs.length-1; i>=0; --i)
				{
					List<Instruction> instructions = pairs[i].getInstructions();
					if (instructions != null)
						visit(instructions);
				}
			}
			break;
		case FastConstants.DECLARE:
			{
				FastDeclaration fd = (FastDeclaration)instruction;
				if (fd.instruction != null)
					visit(fd.instruction);
			}
			break;
		case FastConstants.TRY:
			{
				FastTry ft = (FastTry)instruction;
				visit(ft.instructions);
				List<FastCatch> catches = ft.catches;
				for (int i=catches.size()-1; i>=0; --i)
					visit(catches.get(i).instructions);
				if (ft.finallyInstructions != null)
					visit(ft.finallyInstructions);
			}
			break;
		case FastConstants.SYNCHRONIZED:
			{
				FastSynchronized fsy = (FastSynchronized)instruction;
				visit(fsy.monitor);
				visit(fsy.instructions);
			}
			break;
		case FastConstants.LABEL:
			{
				FastLabel fla = (FastLabel)instruction;
				if (fla.instruction != null)
					visit(fla.instruction);
			}
			break;
		case ByteCodeConstants.LDC:
			{
				IndexInstruction indexInstruction = (IndexInstruction)instruction;
				Constant cst = constants.get(indexInstruction.index);
				
				if (cst.tag == Constants.CONSTANT_Class)
				{
					ConstantClass cc = (ConstantClass)cst;
					internalName = constants.getConstantUtf8(cc.name_index); 
					addReference(internalName);
				}				
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
		case ByteCodeConstants.GOTO:
		case ByteCodeConstants.IINC:			
		case ByteCodeConstants.PREINC:			
		case ByteCodeConstants.POSTINC:	
		case ByteCodeConstants.JSR:			
		case ByteCodeConstants.LDC2_W:
		case ByteCodeConstants.NOP:
		case ByteCodeConstants.SIPUSH:
		case ByteCodeConstants.RET:
		case ByteCodeConstants.RETURN:
		case ByteCodeConstants.EXCEPTIONLOAD:
		case ByteCodeConstants.RETURNADDRESSLOAD:
			break;
		default:
			System.err.println(
					"Can not count reference in " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
	}
	
	private void visit(List<Instruction> instructions)
	{
		for (int i=instructions.size()-1; i>=0; --i)
			visit(instructions.get(i));
	}
	
	private void visitCheckCastAndMultiANewArray(int index)
	{
		Constant c = constants.get(index);
		
		if (c.tag == Constants.CONSTANT_Class)
		{
			addReference(
				constants.getConstantUtf8(((ConstantClass)c).name_index));
		}
	}
	
	private void addReference(String signature)
	{
		if (signature.charAt(0) == '[')
		{
			signature = SignatureAnalyzer.CutArrayDimensionPrefix(signature);
			
			if (signature.charAt(0) == 'L')
				referenceMap.add(SignatureAnalyzer.GetInnerName(signature));
		}
		else
		{
			referenceMap.add(signature);				
		}		
	}
}
