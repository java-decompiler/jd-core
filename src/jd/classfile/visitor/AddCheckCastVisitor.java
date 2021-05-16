package jd.classfile.visitor;

import java.util.List;

import jd.Constants;
import jd.classfile.ConstantPool;
import jd.classfile.LocalVariable;
import jd.classfile.LocalVariables;
import jd.classfile.analyzer.SignatureAnalyzer;
import jd.classfile.constant.ConstantFieldref;
import jd.classfile.constant.ConstantMethodref;
import jd.classfile.constant.ConstantNameAndType;
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
import jd.instruction.bytecode.instruction.InstanceOf;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.InvokeInstruction;
import jd.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.instruction.bytecode.instruction.LoadInstruction;
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


/*
 * Ajout de 'cast' sur les instructions 'throw', 'astore', 'invokeXXX', 
 * 'putfield' et 'putstatic'
 */
public class AddCheckCastVisitor 
{
	private ConstantPool constants;
	private LocalVariables localVariables;
	private LocalVariable localVariable;

	public AddCheckCastVisitor(
			ConstantPool constants, LocalVariables localVariables, 
			LocalVariable localVariable)
	{
		this.constants = constants;
		this.localVariables = localVariables;
		this.localVariable = localVariable;
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
			visit(((ArrayStoreInstruction)instruction).valueref);
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
				if (match(aThrow.value))
				{
					LoadInstruction li = (LoadInstruction)aThrow.value;
					LocalVariable lv = 
						this.localVariables.getLocalVariableWithIndexAndOffset(
								li.index, li.offset);
					int signatureIndex = 
						constants.addConstantUtf8(Constants.INTERNAL_OBJECT_SIGNATURE);
					if (lv.signature_index == signatureIndex)
					{
						// Add Throwable cast
						int nameIndex = 
							this.constants.addConstantUtf8(Constants.INTERNAL_THROWABLE_CLASS_NAME);
						int classIndex = this.constants.addConstantClass(nameIndex);
						if (this.constants.objectClassIndex != classIndex)
							aThrow.value = newInstruction(classIndex, aThrow.value);
					}				
				}
				else
					visit(aThrow.value);
			}
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
		case ByteCodeConstants.ISTORE:
			visit(((StoreInstruction)instruction).valueref);
			break;
		case ByteCodeConstants.ASTORE:
			{
				StoreInstruction storeInstruction = (StoreInstruction)instruction;
				if (match(storeInstruction.valueref))
				{
					LocalVariable lv = 
						this.localVariables.getLocalVariableWithIndexAndOffset(
								storeInstruction.index, storeInstruction.offset);
					if (lv.signature_index > 0)
					{
						// AStore est associé à une variable correctment typée
						String signature = 
							this.constants.getConstantUtf8(lv.signature_index);
						String internalName = 
							SignatureAnalyzer.GetInnerName(signature);
						int nameIndex = 
							this.constants.addConstantUtf8(internalName);
						int classIndex = 
							this.constants.addConstantClass(nameIndex);
						if (this.constants.objectClassIndex != classIndex)
							storeInstruction.valueref = 
								newInstruction(classIndex, storeInstruction.valueref);
					}
				}
				else
					visit(storeInstruction.valueref);
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
			{
				InvokeNoStaticInstruction insi = 
					(InvokeNoStaticInstruction)instruction;
				if (match(insi.objectref))
				{
					ConstantMethodref cmr = constants.getConstantMethodref(insi.index);
					if (this.constants.objectClassIndex != cmr.class_index)
						insi.objectref = newInstruction(cmr.class_index, insi.objectref);
				}
				else
					visit(insi.objectref);
			}
		case ByteCodeConstants.INVOKESTATIC:
			{
				List<Instruction> list = ((InvokeInstruction)instruction).args;
				List<String> types = ((InvokeInstruction)instruction)
					.getListOfParameterSignatures(this.constants);
				
				for (int i=list.size()-1; i>=0; --i)
				{
					Instruction arg = list.get(i);
					if (match(arg))
					{
						String type = types.get(i);
						
						if ((type != null) && (type.charAt(0) == 'L'))
						{
							String internalName = 
								SignatureAnalyzer.GetInnerName(type);					
							int nameIndex = 
								this.constants.addConstantUtf8(internalName);
							int classIndex = 
								this.constants.addConstantClass(nameIndex);
							if (this.constants.objectClassIndex != classIndex)
								list.set(i, newInstruction(classIndex, arg));
						}
					}
					else
						visit(arg);
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
		case ByteCodeConstants.GETFIELD:
			{
				GetField getField = (GetField)instruction;
				if (match(getField.objectref))
				{
					ConstantFieldref cfr = 
						this.constants.getConstantFieldref(getField.index);
					if (this.constants.objectClassIndex != cfr.class_index)
						getField.objectref = 
							newInstruction(cfr.class_index, getField.objectref);
				}
				else
					visit(getField.objectref);
			}
			break;	
		case ByteCodeConstants.PUTFIELD:
			{
				PutField putField = (PutField)instruction;
				if (match(putField.objectref))
				{
					ConstantFieldref cfr = 
						this.constants.getConstantFieldref(putField.index);
					if (this.constants.objectClassIndex != cfr.class_index)
						putField.objectref = 
							newInstruction(cfr.class_index, putField.objectref);
				}
				else
					visit(putField.objectref);
				if (match(putField.valueref))
				{
					ConstantFieldref cfr = constants.getConstantFieldref(putField.index);
					ConstantNameAndType cnat = 
						constants.getConstantNameAndType(cfr.name_and_type_index);
					String signature = this.constants.getConstantUtf8(cnat.descriptor_index);
					
					if (! Constants.INTERNAL_OBJECT_SIGNATURE.equals(signature))
					{
						String internalName = 
							SignatureAnalyzer.GetInnerName(signature);
						int nameIndex = 
							this.constants.addConstantUtf8(internalName);
						int classIndex = 
							this.constants.addConstantClass(nameIndex);
						if (this.constants.objectClassIndex != cfr.class_index)
							putField.valueref = 
								newInstruction(classIndex, putField.valueref);
					}
				}
				else
					visit(putField.valueref);
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			{
				PutStatic putStatic = (PutStatic)instruction;
				if (match(putStatic.valueref))
				{
					ConstantFieldref cfr = constants.getConstantFieldref(putStatic.index);
					ConstantNameAndType cnat = 
						constants.getConstantNameAndType(cfr.name_and_type_index);
					String signature = this.constants.getConstantUtf8(cnat.descriptor_index);
					
					if (! Constants.INTERNAL_OBJECT_SIGNATURE.equals(signature))
					{
						String internalName = 
							SignatureAnalyzer.GetInnerName(signature);
						int nameIndex = 
							this.constants.addConstantUtf8(internalName);
						int classIndex = 
							this.constants.addConstantClass(nameIndex);
						if (this.constants.objectClassIndex != cfr.class_index)
							putStatic.valueref = 
								newInstruction(classIndex, putStatic.valueref);
					}
				}
				else
					visit(putStatic.valueref);
			}
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
		case ByteCodeConstants.INVOKENEW:
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
		case ByteCodeConstants.IINC:			
		case ByteCodeConstants.PREINC:
		case ByteCodeConstants.POSTINC:		
			break;
		default:
			System.err.println(
					"Can not add cast in " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
	}

	private boolean match(Instruction i)
	{
		if (i.opcode == ByteCodeConstants.ALOAD)
		{
			LoadInstruction li = (LoadInstruction)i;			
			if (li.index == this.localVariable.index)
			{
				LocalVariable lv = 
					this.localVariables.getLocalVariableWithIndexAndOffset(
							li.index, li.offset);				
				return lv == this.localVariable;
			}
		}
			
		return false;
	}
	
	private Instruction newInstruction(int classIndex, Instruction i)
	{
		return new CheckCast(
			ByteCodeConstants.CHECKCAST, i.offset, i.lineNumber, classIndex, i);
	}
}
