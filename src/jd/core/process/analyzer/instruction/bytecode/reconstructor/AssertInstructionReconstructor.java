package jd.core.process.analyzer.instruction.bytecode.reconstructor;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;


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
			
			if (! fieldName.equals("$assertionsDisabled"))
				continue;
			
			InvokeNew in = (InvokeNew)athrow.value;
			ConstantMethodref cmr = 
				constants.getConstantMethodref(in.index);	
			String className = constants.getConstantClassName(cmr.class_index);
			
			if (! className.equals("java/lang/AssertionError"))
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
