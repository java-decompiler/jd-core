package jd.instruction.fast.reconstructor;

import java.util.List;

import jd.Constants;
import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.Field;
import jd.classfile.constant.ConstantFieldref;
import jd.classfile.constant.ConstantMethodref;
import jd.classfile.constant.ConstantNameAndType;
import jd.classfile.constant.ConstantString;
import jd.classfile.constant.ConstantValue;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.AssignmentInstruction;
import jd.instruction.bytecode.instruction.DupStore;
import jd.instruction.bytecode.instruction.GetStatic;
import jd.instruction.bytecode.instruction.IfInstruction;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.Invokestatic;
import jd.instruction.bytecode.instruction.Ldc;
import jd.instruction.bytecode.instruction.Pop;
import jd.instruction.bytecode.instruction.TernaryOpStore;
import jd.instruction.fast.FastConstants;
import jd.instruction.fast.instruction.FastTry;
import jd.instruction.fast.visitor.ReplaceDupLoadVisitor;
import jd.util.ReferenceMap;


/*
 * Recontruction du mot cle '.class' depuis les instructions generees par le 
 * JDK 1.1.8 de IBM (?) :
 * ...
 * dupstore( getstatic( current class, 'class$...', Class ) )
 * ifnotnull( dupload )
 * {
 *   pop
 *   try
 *   {
 *     ternaryopstore( AssignmentInstruction( 
 *     		putstatic( dupload ),
 *     		invokestatic( current class, 'class$...', nom de la classe ) ) )
 *   }
 *   catch (ClassNotFoundException localClassNotFoundException1)
 *   {
 *     throw new NoClassDefFoundError(localClassNotFoundException1.getMessage());
 *   }
 * }
 * ??? ( dupload )
 * ...
 */
public class DotClass118BReconstructor 
{
	public static void Reconstruct(
		ReferenceMap referenceMap, ClassFile classFile, List<Instruction> list)
	{
		int i = list.size();
		
		if  (i < 5)
			return;
		
		i -= 4;
		ConstantPool constants = classFile.getConstantPool();
		
		while (i-- > 0)
		{
			Instruction instruction = list.get(i);
			
			if (instruction.opcode != ByteCodeConstants.DUPSTORE)
				continue;
			
			DupStore ds = (DupStore)instruction;
		
			if (ds.objectref.opcode != ByteCodeConstants.GETSTATIC)
				continue;
						
			GetStatic gs = (GetStatic)ds.objectref;

			ConstantFieldref cfr = constants.getConstantFieldref(gs.index);
			
			if (cfr.class_index != classFile.getThisClassIndex())
				continue;

			instruction = list.get(i+1);
			
			if (instruction.opcode != ByteCodeConstants.IFXNULL)
				continue;
			
			IfInstruction ii = (IfInstruction)instruction;
			
			if ((ii.value.opcode != ByteCodeConstants.DUPLOAD) ||
				(ds.offset != ii.value.offset))
				continue;

			instruction = list.get(i+2);
			
			if (instruction.opcode != ByteCodeConstants.POP)
				continue;
			
			Pop pop = (Pop)instruction;

			if ((pop.objectref.opcode != ByteCodeConstants.DUPLOAD) ||
				(ds.offset != pop.objectref.offset))
				continue;

			instruction = list.get(i+3);
			
			if (instruction.opcode != FastConstants.TRY)
				continue;
			
			FastTry ft = (FastTry)instruction;
			
			if ((ft.finallyInstructions != null) ||
				(ft.instructions.size() != 1) ||
				(ft.catches.size() != 1))
				continue;
			
			List<Instruction> catchInstructions = 
				ft.catches.get(0).instructions;
			
			if ((catchInstructions.size() != 1) || 
				(catchInstructions.get(0).opcode != ByteCodeConstants.ATHROW))
				continue;
			
			instruction = ft.instructions.get(0);
			
			if (instruction.opcode != ByteCodeConstants.TERNARYOPSTORE)
				continue;
			
			TernaryOpStore tos = (TernaryOpStore)instruction;
			
			if (tos.objectref.opcode != ByteCodeConstants.ASSIGNMENT)
				continue;
			
			AssignmentInstruction ai = (AssignmentInstruction)tos.objectref;
			
			if ((ai.value1.opcode != ByteCodeConstants.PUTSTATIC) ||
				(ai.value2.opcode != ByteCodeConstants.INVOKESTATIC))
				continue;
			
			Invokestatic is = (Invokestatic)ai.value2;
			
			if (is.args.size() != 1)
				continue;

			instruction = is.args.get(0);
			
			if (instruction.opcode != ByteCodeConstants.LDC)
				continue;			

			ConstantNameAndType cnatField = constants.getConstantNameAndType(
				cfr.name_and_type_index);

			String signature = 
				constants.getConstantUtf8(cnatField.descriptor_index);
			
			if (! Constants.INTERNAL_CLASS_SIGNATURE.equals(signature))
				continue;
			
			String nameField = constants.getConstantUtf8(cnatField.name_index);

			if (! nameField.startsWith(Constants.CLASS_DOLLAR))
				continue;						
			
			ConstantMethodref cmr = 
				constants.getConstantMethodref(is.index);
			
			String className = 
				constants.getConstantClassName(cmr.class_index);
			
			if (! className.equals(Constants.INTERNAL_CLASS_CLASS_NAME))
				continue;
			
			ConstantNameAndType cnatMethod = 
				constants.getConstantNameAndType(cmr.name_and_type_index);
			String nameMethod = 
				constants.getConstantUtf8(cnatMethod.name_index);
			
			if (! nameMethod.equals(Constants.FORNAME_METHOD_NAME))
				continue;
			
			Ldc ldc = (Ldc)instruction;	
			ConstantValue cv = constants.getConstantValue(ldc.index);
			
			if (cv.tag != Constants.CONSTANT_String)
				continue;
			
			// Trouve !		
			ConstantString cs = (ConstantString)cv;
			String dotClassName = constants.getConstantUtf8(cs.string_index);
			String internalName = dotClassName.replace(
				Constants.PACKAGE_SEPARATOR, 
				Constants.INTERNAL_PACKAGE_SEPARATOR);
			
			referenceMap.add(internalName);			
			
			// Ajout du nom interne
			int index = constants.addConstantUtf8(internalName);
			// Ajout d'une nouvelle classe
			index = constants.addConstantClass(index);			
			ldc = new Ldc(
				ByteCodeConstants.LDC, instruction.offset, 
				instruction.lineNumber, index);
			
			// Remplacement de l'intruction GetStatic par l'instruction Ldc
			ReplaceDupLoadVisitor visitor = new ReplaceDupLoadVisitor(ds, ldc);
			
			visitor.visit(list.get(i+4));

			// Retrait de l'intruction FastTry
			list.remove(i+3);
			// Retrait de l'intruction Pop 
			list.remove(i+2);
			// Retrait de l'intruction IfNotNull
			list.remove(i+1);
			// Retrait de l'intruction DupStore
			list.remove(i);	

			// Recherche de l'attribut statique et ajout de l'attribut SYNTHETIC
			Field[] fields = classFile.getFields();
			int j = fields.length;
			
			while (j-- > 0)
			{
				Field field = fields[j];
				
				if (field.name_index == cnatField.name_index)
				{
					field.access_flags |= Constants.ACC_SYNTHETIC;
					break;
				}
			}
		}
	}
}
