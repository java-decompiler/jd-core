package jd.instruction.fast.reconstructor;

import java.util.List;

import jd.Constants;
import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.Field;
import jd.classfile.Method;
import jd.classfile.constant.ConstantFieldref;
import jd.classfile.constant.ConstantNameAndType;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.PutStatic;
import jd.instruction.fast.FastConstants;


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
		int indexField = 0;
		
		for (int indexInstruction=0; indexInstruction<list.size(); ++indexInstruction)
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
			
			while (indexField < fields.length)
			{
				Field field = fields[indexField++];
				
				if (((field.access_flags & Constants.ACC_STATIC) != 0) && 
					(cnat.descriptor_index == field.descriptor_index) &&
					(cnat.name_index == field.name_index))						
				{
					Instruction valueref = putStatic.valueref;
					field.setValueAndLocalVariables(
						valueref, method.getLocalVariables());	
					if (valueref.opcode == FastConstants.NEWANDINITARRAY)
						valueref.opcode = FastConstants.INITARRAY;
					list.remove(indexInstruction--);
					break;
				}
			}
		}
	}	
}
