package jd.core.process.analyzer.classfile.visitor;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.constant.Constant;
import jd.core.model.classfile.constant.ConstantClass;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantMethodref;
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
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.LookupSwitch;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.model.instruction.bytecode.instruction.MonitorExit;
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.New;
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
import jd.core.model.instruction.fast.instruction.FastSwitch.Pair;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;
import jd.core.model.reference.ReferenceMap;
import jd.core.util.SignatureUtil;


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
		if (instruction == null)
			return;
		
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
		case ByteCodeConstants.IMPLICITCONVERT:
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
		case ByteCodeConstants.INVOKENEW:
			{
				InvokeInstruction ii = (InvokeInstruction)instruction;
				ConstantMethodref cmr = constants.getConstantMethodref(ii.index);
				internalName = constants.getConstantClassName(cmr.class_index);
				addReference(internalName);					
				visit(ii.args);
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
			visit(ff.init);	
			visit(ff.inc);	
		case FastConstants.WHILE:
		case FastConstants.DO_WHILE:
		case FastConstants.IF_:
			FastTestList ftl = (FastTestList)instruction;
			visit(ftl.test);	
		case FastConstants.INFINITE_LOOP:
			{
				List<Instruction> instructions = 
						((FastList)instruction).instructions;
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
					visit(instructions);
				}
			}
			break;
		case FastConstants.DECLARE:
			{
				FastDeclaration fd = (FastDeclaration)instruction;
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
				visit(fla.instruction);
			}
			break;
		case ByteCodeConstants.LDC:
			{
				IndexInstruction indexInstruction = (IndexInstruction)instruction;
				Constant cst = constants.get(indexInstruction.index);
				
				if (cst.tag == ConstantConstant.CONSTANT_Class)
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
		if (instructions != null)
		{
			for (int i=instructions.size()-1; i>=0; --i)
				visit(instructions.get(i));
		}
	}
	
	private void visitCheckCastAndMultiANewArray(int index)
	{
		Constant c = constants.get(index);
		
		if (c.tag == ConstantConstant.CONSTANT_Class)
		{
			addReference(
				constants.getConstantUtf8(((ConstantClass)c).name_index));
		}
	}
	
	private void addReference(String signature)
	{
		if (signature.charAt(0) == '[')
		{
			signature = SignatureUtil.CutArrayDimensionPrefix(signature);
			
			if (signature.charAt(0) == 'L')
				referenceMap.add(SignatureUtil.GetInnerName(signature));
		}
		else
		{
			referenceMap.add(signature);				
		}		
	}
}
