package jd.core.process.analyzer.classfile.reconstructor;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.process.analyzer.classfile.visitor.SearchInstructionByOpcodeVisitor;


public class InitStaticFieldsReconstructor 
{
	public static void Reconstruct(ClassFile classFile)
	{
		Method method = classFile.getStaticMethod();
		if (method == null)
			return;
		
		Field[] fields = classFile.getFields();
		if (fields == null)
			return;
		
		List<Instruction> list = method.getFastNodes();	
		if (list == null)
			return;
		
		ConstantPool constants = classFile.getConstantPool();

		// Search field initialisation from the begining
		int indexInstruction = 0;
		int length = list.size();
		int indexField = 0;
		
		while (indexInstruction < length)
		{
			Instruction instruction = list.get(indexInstruction);
			
			if (instruction.opcode != ByteCodeConstants.PUTSTATIC)
				break;
			
			PutStatic putStatic = (PutStatic)instruction;
			ConstantFieldref cfr = constants.getConstantFieldref(putStatic.index);

			if (cfr.class_index != classFile.getThisClassIndex())
				break;
				
			ConstantNameAndType cnat = 
				constants.getConstantNameAndType(cfr.name_and_type_index);
			
			int lengthBeforeSubstitution = list.size();
			
			while (indexField < fields.length)
			{
				Field field = fields[indexField++];
				
				if (((field.access_flags & ClassFileConstants.ACC_STATIC) != 0) && 
					(cnat.descriptor_index == field.descriptor_index) &&
					(cnat.name_index == field.name_index))						
				{
					Instruction valueref = putStatic.valueref;
					
					if (SearchInstructionByOpcodeVisitor.visit(
							valueref, ByteCodeConstants.ALOAD) != null)
						break;
					if (SearchInstructionByOpcodeVisitor.visit(
							valueref, ByteCodeConstants.LOAD) != null)
						break;
					if (SearchInstructionByOpcodeVisitor.visit(
							valueref, ByteCodeConstants.ILOAD) != null)
						break;
					
					field.setValueAndMethod(valueref, method);	
					if (valueref.opcode == FastConstants.NEWANDINITARRAY)
						valueref.opcode = FastConstants.INITARRAY;
					list.remove(indexInstruction--);
					break;
				}
			}
					
			// La substitution a-t-elle ete faite ?
			if (lengthBeforeSubstitution == list.size())
			{
				// Non -> On arrete.
				break;
			}			
			
			indexInstruction++;
		}
		
		// Search field initialisation from the end
		indexInstruction = list.size();
		
		if (indexInstruction > 0)
		{
			// Saute la derniere instruction 'return'
			indexInstruction--;
			indexField = fields.length;
					
			while (indexInstruction-- > 0)
			{
				Instruction instruction = list.get(indexInstruction);
				
				if (instruction.opcode != ByteCodeConstants.PUTSTATIC)
					break;
				
				PutStatic putStatic = (PutStatic)instruction;
				ConstantFieldref cfr = constants.getConstantFieldref(putStatic.index);
	
				if (cfr.class_index != classFile.getThisClassIndex())
					break;
					
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cfr.name_and_type_index);
				
				int lengthBeforeSubstitution = list.size();
				
				while (indexField-- > 0)
				{
					Field field = fields[indexField];
					
					if (((field.access_flags & ClassFileConstants.ACC_STATIC) != 0) && 
						(cnat.descriptor_index == field.descriptor_index) &&
						(cnat.name_index == field.name_index))						
					{
						Instruction valueref = putStatic.valueref;

						if (SearchInstructionByOpcodeVisitor.visit(
								valueref, ByteCodeConstants.ALOAD) != null)
							break;
						if (SearchInstructionByOpcodeVisitor.visit(
								valueref, ByteCodeConstants.LOAD) != null)
							break;
						if (SearchInstructionByOpcodeVisitor.visit(
								valueref, ByteCodeConstants.ILOAD) != null)
							break;
						
						field.setValueAndMethod(valueref, method);	
						if (valueref.opcode == FastConstants.NEWANDINITARRAY)
							valueref.opcode = FastConstants.INITARRAY;
						list.remove(indexInstruction);
						break;
					}
				}
				
				// La substitution a-t-elle ete faite ?
				if (lengthBeforeSubstitution == list.size())
				{
					// Non -> On arrete.
					break;
				}
			}
		}
	}	
}
