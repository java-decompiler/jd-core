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
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
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
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;
import jd.core.util.StringConstants;



public class ReplaceStringBuxxxerVisitor 
{	
	private ConstantPool constants;
	
	public ReplaceStringBuxxxerVisitor(ConstantPool constants)
	{
		this.constants = constants;
	}
	
	public void visit(Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			{
				ArrayLength al = (ArrayLength)instruction;
				Instruction i = match(al.arrayref);
				if (i == null)
					visit(al.arrayref);
				else
					al.arrayref = i;
			}
			break;
		case ByteCodeConstants.ARRAYLOAD:
			{
				ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
				Instruction i = match(ali.arrayref);
				if (i == null)
					visit(ali.arrayref);
				else
					ali.arrayref = i;
				
				i = match(ali.indexref);
				if (i == null)
					visit(ali.indexref);
				else
					ali.indexref = i;
			}
			break;
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			{
				ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
				Instruction i = match(asi.arrayref);
				if (i == null)
					visit(asi.arrayref);
				else
					asi.arrayref = i;
				
				i = match(asi.indexref);
				if (i == null)
					visit(asi.indexref);
				else
					asi.indexref = i;
				
				i = match(asi.valueref);
				if (i == null)
					visit(asi.valueref);
				else
					asi.valueref = i;
			}
			break;
		case ByteCodeConstants.ASSERT:
			{
				AssertInstruction ai = (AssertInstruction)instruction;
				Instruction i = match(ai.test);
				if (i == null)
					visit(ai.test);
				else
					ai.test = i;
				if (ai.msg != null)
				{
					i = match(ai.msg);
					if (i == null)
						visit(ai.msg);
					else
						ai.msg = i;
				}
			}
			break;
		case ByteCodeConstants.ASSIGNMENT:
			{
				AssignmentInstruction ai = (AssignmentInstruction)instruction;
				Instruction i = match(ai.value1);
				if (i == null)
					visit(ai.value1);
				else
					ai.value1 = i;

				i = match(ai.value2);
				if (i == null)
					visit(ai.value2);
				else
					ai.value2 = i;
			}
			break;
		case ByteCodeConstants.ATHROW:
			{
				AThrow aThrow = (AThrow)instruction;
				visit(aThrow.value);
			}
			break;
		case ByteCodeConstants.BINARYOP:
			{
				BinaryOperatorInstruction boi = 
					(BinaryOperatorInstruction)instruction;
				Instruction i = match(boi.value1);
				if (i == null)
					visit(boi.value1);
				else
					boi.value1 = i;
				
				i = match(boi.value2);
				if (i == null)
					visit(boi.value2);
				else
					boi.value2 = i;
			}
			break;
		case ByteCodeConstants.UNARYOP:
			{
				UnaryOperatorInstruction uoi = 
					(UnaryOperatorInstruction)instruction;
				Instruction i = match(uoi.value);
				if (i == null)
					visit(uoi.value);
				else
					uoi.value = i;
			}
			break;		
		case ByteCodeConstants.DUPSTORE:
			{
				DupStore dupStore = (DupStore)instruction;
				Instruction i = match(dupStore.objectref);
				if (i == null)
					visit(dupStore.objectref);
				else
					dupStore.objectref = i;	
			}
			break;
		case ByteCodeConstants.CHECKCAST:
			{
				CheckCast cc = (CheckCast)instruction;
				Instruction i = match(cc.objectref);
				if (i == null)
					visit(cc.objectref);
				else
					cc.objectref = i;
			}
			break;
		case ByteCodeConstants.CONVERT:
		case ByteCodeConstants.IMPLICITCONVERT:
			{
				ConvertInstruction ci = (ConvertInstruction)instruction;
				Instruction i = match(ci.value);
				if (i == null)
					visit(ci.value);
				else
					ci.value = i;
			}
			break;
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFXNULL:
			{
				IfInstruction ifInstruction = (IfInstruction)instruction;
				Instruction i = match(ifInstruction.value);
				if (i == null)
					visit(ifInstruction.value);
				else
					ifInstruction.value = i;
			}
			break;
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ifCmpInstruction = (IfCmp)instruction;
				Instruction i = match(ifCmpInstruction.value1);
				if (i == null)
					visit(ifCmpInstruction.value1);
				else
					ifCmpInstruction.value1 = i;

				i = match(ifCmpInstruction.value2);
				if (i == null)
					visit(ifCmpInstruction.value2);
				else
					ifCmpInstruction.value2 = i;
			}
			break;			
		case ByteCodeConstants.INSTANCEOF:
			{
				InstanceOf instanceOf = (InstanceOf)instruction;
				Instruction i = match(instanceOf.objectref);
				if (i == null)
					visit(instanceOf.objectref);
				else
					instanceOf.objectref = i;
			}
			break;
		case ByteCodeConstants.COMPLEXIF:
			{
				ComplexConditionalBranchInstruction complexIf = (ComplexConditionalBranchInstruction)instruction;
				List<Instruction> branchList = complexIf.instructions;
				for (int i=branchList.size()-1; i>=0; --i)
				{
					visit(branchList.get(i));
				}
			}
			break;
		case ByteCodeConstants.GETFIELD:
			{
				GetField getField = (GetField)instruction;
				Instruction i = match(getField.objectref);
				if (i == null)
					visit(getField.objectref);
				else
					getField.objectref = i;
			}
			break;
		case ByteCodeConstants.INVOKEVIRTUAL:
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
			{
				InvokeNoStaticInstruction insi = 
					(InvokeNoStaticInstruction)instruction;
				Instruction i = match(insi.objectref);
				if (i == null)
					visit(insi.objectref);
				else
					insi.objectref = i;
				replaceInArgs(insi.args);
			}
			break;	
		case ByteCodeConstants.INVOKESTATIC:
		case ByteCodeConstants.INVOKENEW:
			replaceInArgs(((InvokeInstruction)instruction).args);
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
			{
				LookupSwitch lookupSwitch = (LookupSwitch)instruction;
				Instruction i = match(lookupSwitch.key);
				if (i == null)
					visit(lookupSwitch.key);
				else
					lookupSwitch.key = i;
			}
			break;
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				MultiANewArray multiANewArray = (MultiANewArray)instruction;
				Instruction[] dimensions = multiANewArray.dimensions;
				Instruction ins;

				for (int i=dimensions.length-1; i>=0; i--)
				{
					ins = match(dimensions[i]);
					if (ins == null)
						visit(dimensions[i]);
					else
						dimensions[i] = ins;
				}
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			{
				NewArray newArray = (NewArray)instruction;
				Instruction i = match(newArray.dimension);
				if (i == null)
					visit(newArray.dimension);
				else
					newArray.dimension = i;
			}
			break;
		case ByteCodeConstants.ANEWARRAY:
			{
				ANewArray newArray = (ANewArray)instruction;
				Instruction i = match(newArray.dimension);
				if (i == null)
					visit(newArray.dimension);
				else
					newArray.dimension = i;
			}
			break;
		case ByteCodeConstants.POP:
			visit(((Pop)instruction).objectref);
			break;
		case ByteCodeConstants.PUTFIELD:
			{
				PutField putField = (PutField)instruction;
				Instruction i = match(putField.objectref);
				if (i == null)
					visit(putField.objectref);
				else
					putField.objectref = i;

				i = match(putField.valueref);
				if (i == null)
					visit(putField.valueref);
				else
					putField.valueref = i;
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			{
				PutStatic putStatic = (PutStatic)instruction;
				Instruction i = match(putStatic.valueref);
				if (i == null)
					visit(putStatic.valueref);
				else
					putStatic.valueref = i;
			}
			break;
		case ByteCodeConstants.XRETURN:
			{
				ReturnInstruction returnInstruction = 
					(ReturnInstruction)instruction;
				Instruction i = match(returnInstruction.valueref);
				if (i == null)
					visit(returnInstruction.valueref);
				else
					returnInstruction.valueref = i;			
			}
			break;
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			{
				StoreInstruction storeInstruction =
					(StoreInstruction)instruction;
				Instruction i = match(storeInstruction.valueref);
				if (i == null)
					visit(storeInstruction.valueref);
				else
					storeInstruction.valueref = i;
			}
			break;
		case ByteCodeConstants.TABLESWITCH:
			{
				TableSwitch tableSwitch = (TableSwitch)instruction;
				Instruction i = match(tableSwitch.key);
				if (i == null)
					visit(tableSwitch.key);
				else
					tableSwitch.key = i;
			}
			break;
		case ByteCodeConstants.TERNARYOPSTORE:
			{
				TernaryOpStore tosInstruction = (TernaryOpStore)instruction;
				Instruction i = match(tosInstruction.objectref);
				if (i == null)
					visit(tosInstruction.objectref);
				else
					tosInstruction.objectref = i;
			}
			break;
		case ByteCodeConstants.TERNARYOP:					
			{		
				TernaryOperator to = (TernaryOperator)instruction;
				Instruction i = match(to.value1);
				if (i == null)
					visit(to.value1);
				else
					to.value1 = i;
				
				i = match(to.value2);
				if (i == null)
					visit(to.value2);
				else
					to.value2 = i;		
			}
			break;
		case ByteCodeConstants.MONITORENTER:
			{
				MonitorEnter meInstruction = (MonitorEnter)instruction;
				Instruction i = match(meInstruction.objectref);
				if (i == null)
					visit(meInstruction.objectref);
				else
					meInstruction.objectref = i;
			}
			break;
		case ByteCodeConstants.MONITOREXIT:			
			{
				MonitorExit meInstruction = (MonitorExit)instruction;
				Instruction i = match(meInstruction.objectref);
				if (i == null)
					visit(meInstruction.objectref);
				else
					meInstruction.objectref = i;
			}
			break;
		case ByteCodeConstants.INITARRAY:			
		case ByteCodeConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iaInstruction =
					(InitArrayInstruction)instruction;
				Instruction i = match(iaInstruction.newArray);
				if (i == null)
					visit(iaInstruction.newArray);
				else
					iaInstruction.newArray = i;
				
				for (int index=iaInstruction.values.size()-1; index>=0; --index)
				{
					i = match(iaInstruction.values.get(index));
					if (i == null)
						visit(iaInstruction.values.get(index));
					else
						iaInstruction.values.set(index, i);
				}
			}
			break;
		case ByteCodeConstants.ACONST_NULL:
		case ByteCodeConstants.DUPLOAD:
		case ByteCodeConstants.LDC:
		case ByteCodeConstants.LDC2_W:
		case ByteCodeConstants.NEW:
		case ByteCodeConstants.RETURN:
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.DCONST:
		case ByteCodeConstants.FCONST:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.LCONST:
		case ByteCodeConstants.IINC:			
		case ByteCodeConstants.PREINC:
		case ByteCodeConstants.POSTINC:
		case ByteCodeConstants.JSR:			
		case ByteCodeConstants.GETSTATIC:
		case ByteCodeConstants.OUTERTHIS:
		case ByteCodeConstants.SIPUSH:
		case ByteCodeConstants.LOAD:
		case ByteCodeConstants.ALOAD:
		case ByteCodeConstants.ILOAD:
		case ByteCodeConstants.GOTO:
		case ByteCodeConstants.EXCEPTIONLOAD:
		case ByteCodeConstants.RET:
		case ByteCodeConstants.RETURNADDRESSLOAD:
			break;
		default:
			System.err.println(
					"Can not replace StringBuxxxer in " + 
					instruction.getClass().getName() + " " + 
					instruction.opcode);
		}
	}


	private void replaceInArgs(List<Instruction> args)
	{
		if (args.size() > 0)
		{
			Instruction ins;
			
			for (int i=args.size()-1; i>=0; --i)
			{
				ins = match(args.get(i));
				if (ins == null)
					visit(args.get(i));
				else
					args.set(i, ins);
			}
		}
	}
	
	private Instruction match(Instruction i)
	{
		if (i.opcode == ByteCodeConstants.INVOKEVIRTUAL)
		{
			Invokevirtual iv = (Invokevirtual)i;
			ConstantMethodref cmr = this.constants.getConstantMethodref(iv.index);
			ConstantClass cc = this.constants.getConstantClass(cmr.class_index);
			
			if ((cc.name_index == constants.stringBufferClassNameIndex) ||
				(cc.name_index == constants.stringBuilderClassNameIndex))
			{
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cmr.name_and_type_index);
		
				if (cnat.name_index == constants.toStringIndex)
					return match(iv.objectref, cmr.class_index);
			}
		}
		
		return null;
	}

	private Instruction match(Instruction i, int classIndex)
	{
		if (i.opcode == ByteCodeConstants.INVOKEVIRTUAL)
		{
			InvokeNoStaticInstruction insi = (InvokeNoStaticInstruction)i;
			ConstantMethodref cmr = 
				this.constants.getConstantMethodref(insi.index);
			
			if (cmr.class_index == classIndex)
			{
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cmr.name_and_type_index);
		
				if ((cnat.name_index == this.constants.appendIndex) && 
					(insi.args.size() == 1))
				{
					Instruction result = match(insi.objectref, cmr.class_index);

					if (result == null)
					{
						return insi.args.get(0);
					}
					else
					{
						return new BinaryOperatorInstruction(
							ByteCodeConstants.BINARYOP, i.offset, i.lineNumber, 
							4,  StringConstants.INTERNAL_STRING_SIGNATURE, "+",
							result, insi.args.get(0));
					}
				}
			}
		}
		else if (i.opcode == ByteCodeConstants.INVOKENEW)
		{
			InvokeNew in = (InvokeNew)i;
			ConstantMethodref cmr = 
				this.constants.getConstantMethodref(in.index);			
			
			if ((cmr.class_index == classIndex) && (in.args.size() == 1))
			{
				Instruction arg0 = in.args.get(0);
				
				// Remove String.valueOf for String
				if (arg0.opcode == ByteCodeConstants.INVOKESTATIC)
				{
					Invokestatic is = (Invokestatic)arg0;
					cmr = this.constants.getConstantMethodref(is.index);
					ConstantClass cc = this.constants.getConstantClass(cmr.class_index);
					
					if (cc.name_index == this.constants.stringClassNameIndex)
					{
						ConstantNameAndType cnat = 
							this.constants.getConstantNameAndType(cmr.name_and_type_index);
				
						if ((cnat.name_index == this.constants.valueOfIndex) && 
						    (is.args.size() == 1))
							return is.args.get(0);
					}
				}
				
				return arg0;
			}
		}
		
		return null;
	}
}
