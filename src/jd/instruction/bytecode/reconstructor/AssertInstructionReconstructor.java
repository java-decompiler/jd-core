package jd.instruction.bytecode.reconstructor;

import java.util.List;

import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.constant.ConstantFieldref;
import jd.classfile.constant.ConstantNameAndType;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.AThrow;
import jd.instruction.bytecode.instruction.AssertInstruction;
import jd.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.instruction.bytecode.instruction.GetStatic;
import jd.instruction.bytecode.instruction.IfInstruction;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.InvokeNew;


/*
 * Recontruction des des instructions 'assert' depuis le motif :
 * ...
 * complexif( (!($assertionsDisabled)) && (test) )
 *  athrow( newinvoke( classindex="AssertionError", args=["msg"] ));
 * ...
 */
public class AssertInstructionReconstructor 
{
	public static void Reconstruct(ClassFile classFile, List<Instruction> list)
	{
		int index = list.size();
		if (index-- == 0)
			return;
				
		while (index-- > 1)		
		{
			Instruction instruction = list.get(index);
			
			if (instruction.opcode != ByteCodeConstants.ATHROW)
				continue;

			// AThrow trouve
			AThrow athrow = (AThrow)instruction;
			if (athrow.value.opcode != ByteCodeConstants.INVOKENEW)
				continue;

			instruction = list.get(index-1);
			if (instruction.opcode != ByteCodeConstants.COMPLEXIF)
				continue;
			
			// ComplexConditionalBranchInstruction trouve
			ComplexConditionalBranchInstruction cbl = 
				(ComplexConditionalBranchInstruction)instruction;
			int jumpOffset = cbl.GetJumpOffset();
			int lastOffset = list.get(index+1).offset;
			
			if ((athrow.offset >= jumpOffset) || (jumpOffset > lastOffset))
				continue;
			
			if ((cbl.cmp != 2) || (cbl.instructions.size() < 1))
				continue;
			
			instruction = cbl.instructions.get(0);
			if (instruction.opcode != ByteCodeConstants.IF)
				continue;
			
			IfInstruction if1 = (IfInstruction)instruction;
			if ((if1.cmp != 7) || (if1.value.opcode != ByteCodeConstants.GETSTATIC))
				continue;
			
			GetStatic gs = (GetStatic)if1.value;
			ConstantPool constants = classFile.getConstantPool();		
			ConstantFieldref cfr = constants.getConstantFieldref(gs.index);
			
			if (cfr.class_index != classFile.getThisClassIndex())
				continue;
			
			ConstantNameAndType cnat = 
				constants.getConstantNameAndType(cfr.name_and_type_index);		
			String fieldName = constants.getConstantUtf8(cnat.name_index);
			
			if (! "$assertionsDisabled".equals(fieldName))
				continue;
			
			InvokeNew in = (InvokeNew)athrow.value;
			String className = constants.getConstantClassName(in.classIndex);
			
			if (! "java/lang/AssertionError".equals(className))
				continue;
			
			// Remove first condition "!($assertionsDisabled)"
			cbl.instructions.remove(0);	

			Instruction msg = (in.args.size() == 0) ? null : in.args.get(0);
			list.remove(index--);
			
			list.set(index, new AssertInstruction(
				ByteCodeConstants.ASSERT, athrow.offset, 
				cbl.lineNumber, cbl, msg));
		}	
	}
}
