package jd.core.process.layouter.visitor;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
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
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.Switch;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;
import jd.core.util.StringConstants;


public abstract class BaseInstructionSplitterVisitor 
{
	protected ClassFile classFile;
	protected ConstantPool constants;
	
	public BaseInstructionSplitterVisitor() {}
	
	public void start(ClassFile classFile)
	{
		this.classFile = classFile;
		this.constants = (classFile == null) ? null : classFile.getConstantPool();
	}
	
	public void visit(Instruction instruction)
	{
		visit(null, instruction);
	}
	
	protected void visit(Instruction parent, Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			visit(instruction, ((ArrayLength)instruction).arrayref);
			break;
		case ByteCodeConstants.ARRAYLOAD:
			{
				ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction; 
				visit(instruction, ali.arrayref);
				visit(instruction, ali.indexref);
			}
			break;
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			{
				ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
				visit(instruction, asi.arrayref);
				visit(instruction, asi.indexref);
				visit(instruction, asi.valueref);
			}
			break;
		case ByteCodeConstants.ANEWARRAY:
			visit(instruction, ((ANewArray)instruction).dimension);
			break;
		case ByteCodeConstants.ASSERT:
			{
				AssertInstruction ai = (AssertInstruction)instruction;
				visit(instruction, ai.test);
				if (ai.msg != null)
					visit(instruction, ai.msg);
			}
			break;
		case ByteCodeConstants.ASSIGNMENT:
		case ByteCodeConstants.BINARYOP:
			{
				BinaryOperatorInstruction boi = 
					(BinaryOperatorInstruction)instruction;
				visit(instruction, boi.value1);
				visit(instruction, boi.value2);
			}
			break;
		case ByteCodeConstants.ATHROW:
			visit(instruction, ((AThrow)instruction).value);
			break;
		case ByteCodeConstants.UNARYOP:
			visit(instruction, ((UnaryOperatorInstruction)instruction).value);
			break;
		case ByteCodeConstants.CONVERT:
		case ByteCodeConstants.IMPLICITCONVERT:
			visit(instruction, ((ConvertInstruction)instruction).value);
			break;
		case ByteCodeConstants.CHECKCAST:
			visit(instruction, ((CheckCast)instruction).objectref);
			break;
		case FastConstants.DECLARE:
			{
				FastDeclaration fd = (FastDeclaration)instruction;
				if (fd.instruction != null)
					visit(instruction, fd.instruction);
			}
			break;
		case ByteCodeConstants.GETFIELD:
			visit(instruction, ((GetField)instruction).objectref);
			break;
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFXNULL:
			visit(instruction, ((IfInstruction)instruction).value);
			break;
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ic = (IfCmp)instruction;
				visit(instruction, ic.value1);
				visit(instruction, ic.value2);
			}
			break;
		case FastConstants.COMPLEXIF:
			{
				List<Instruction> branchList = 
					((ComplexConditionalBranchInstruction)instruction).instructions;
				int lenght = branchList.size();	
				for (int i=0; i<lenght; i++)
					visit(instruction, branchList.get(i));
			}
			break;
		case ByteCodeConstants.PREINC:	
		case ByteCodeConstants.POSTINC:			
			visit(instruction, ((IncInstruction)instruction).value);
			break;
		case ByteCodeConstants.INVOKENEW:
			{
				InvokeNew in = (InvokeNew)instruction;			
				List<Instruction> args = in.args;
				int lenght = args.size();	
				for (int i=0; i<lenght; i++)
					visit(instruction, args.get(i));
				
				ConstantMethodref cmr = 
					this.constants.getConstantMethodref(in.index);	
				String internalClassName = 
					this.constants.getConstantClassName(cmr.class_index);
				String prefix = 
					this.classFile.getThisClassName() + 
					StringConstants.INTERNAL_INNER_SEPARATOR;
				
				if (internalClassName.startsWith(prefix))
				{
					ClassFile innerClassFile = 
						this.classFile.getInnerClassFile(internalClassName);
	
					if ((innerClassFile != null) && 
						(innerClassFile.getInternalAnonymousClassName() != null))
					{
						// Anonymous new invoke
						visitAnonymousNewInvoke(
							(parent==null) ? in : parent, in, innerClassFile);
					}	
					//else 
					//{
						// Inner class new invoke		
					//}
				}
				//else
				//{
					// Normal new invoke
				//}
			}
			break;
		case ByteCodeConstants.INSTANCEOF:
			visit(instruction, ((InstanceOf)instruction).objectref);
			break;
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKEVIRTUAL:
		case ByteCodeConstants.INVOKESPECIAL:
			visit(instruction, ((InvokeNoStaticInstruction)instruction).objectref);
		case ByteCodeConstants.INVOKESTATIC:
			{
				List<Instruction> args = ((InvokeInstruction)instruction).args;
				int lenght = args.size();	
				for (int i=0; i<lenght; i++)
					visit(instruction, args.get(i));
			}
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
		case ByteCodeConstants.TABLESWITCH:
			visit(instruction, ((Switch)instruction).key);
			break;
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				Instruction[] dimensions = 
					((MultiANewArray)instruction).dimensions;
				int lenght = dimensions.length;	
				for (int i=0; i<lenght; i++)
					visit(instruction, dimensions[i]);	
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			visit(instruction, ((NewArray)instruction).dimension);
			break;
		case ByteCodeConstants.POP:
			visit(instruction, ((Pop)instruction).objectref);
			break;
		case ByteCodeConstants.PUTFIELD:
			{
				PutField putField = (PutField)instruction;
				visit(instruction, putField.objectref);
				visit(instruction, putField.valueref);
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			visit(instruction, ((PutStatic)instruction).valueref);
			break;
		case ByteCodeConstants.XRETURN:
			visit(instruction, ((ReturnInstruction)instruction).valueref);
			break;
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			visit(instruction, ((StoreInstruction)instruction).valueref);
			break;
		case ByteCodeConstants.TERNARYOPSTORE:
			visit(instruction, ((TernaryOpStore)instruction).objectref);
			break;
		case FastConstants.TERNARYOP:
			{
				TernaryOperator tp = (TernaryOperator)instruction;
				visit(instruction, tp.test);	
				visit(instruction, tp.value1);	
				visit(instruction, tp.value2);
			}
			break;
		case FastConstants.INITARRAY:
		case FastConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iai = (InitArrayInstruction)instruction;
				visit(instruction, iai.newArray);	
				List<Instruction> values = iai.values;
				int lenght = values.size();	
				for (int i=0; i<lenght; i++)
					visit(instruction, values.get(i));	
			}
			break;
		}
	}
	
	public abstract void visitAnonymousNewInvoke(
		Instruction parent, InvokeNew in, ClassFile innerClassFile);
}
