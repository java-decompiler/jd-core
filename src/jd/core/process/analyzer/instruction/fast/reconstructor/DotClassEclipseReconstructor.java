package jd.core.process.analyzer.instruction.fast.reconstructor;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.classfile.constant.ConstantString;
import jd.core.model.classfile.constant.ConstantValue;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.model.instruction.bytecode.instruction.Ldc;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;
import jd.core.model.reference.ReferenceMap;
import jd.core.process.analyzer.classfile.visitor.ReplaceDupLoadVisitor;
import jd.core.util.StringConstants;


/*
 * Recontruction du mot cle '.class' depuis les instructions generees par le 
 * compilateur d'Eclipse :
 * ...
 * ifnotnull( getstatic( current class, 'class$...', Class ) )
 *  try
 *  {
 *   dupstore( invokestatic( 'Class', 'forName', nom de la classe ) )
 *   putstatic( current class, 'class$...', Class, dupload )
 *  }
 *  catch (Ljava/lang/ClassNotFoundException;)
 *  {
 *    athrow ...
 *  }
 * ???( getstatic( class, 'class$...' ) )
 * ...
 */
public class DotClassEclipseReconstructor 
{
	public static void Reconstruct(
		ReferenceMap referenceMap, ClassFile classFile, List<Instruction> list)
	{
		int i = list.size();
		
		if  (i < 3)
			return;
		
		i -= 2;
		ConstantPool constants = classFile.getConstantPool();
		
		while (i-- > 0)
		{
			Instruction instruction = list.get(i);
			
			if (instruction.opcode != FastConstants.IFXNULL)
				continue;
					
			IfInstruction ii = (IfInstruction)instruction;
			
			if (ii.value.opcode != ByteCodeConstants.GETSTATIC)
				continue;
						
			int jumpOffset = ii.GetJumpOffset();
						
			instruction = list.get(i+1);
			
			if (instruction.opcode != FastConstants.TRY)
				continue;
		
			FastTry ft = (FastTry)instruction;
			
			if ((ft.catches.size() != 1) || (ft.finallyInstructions != null) ||
				(ft.instructions.size() != 2))
				continue;
			
			FastCatch fc = ft.catches.get(0);
			
			if ((fc.instructions.size() != 1) || 
				(fc.otherExceptionTypeIndexes != null))
					continue;
			
			instruction = list.get(i+2);
			
			if ((ft.offset >= jumpOffset) || (jumpOffset > instruction.offset))
				continue;
			
			GetStatic gs = (GetStatic)ii.value;			

			ConstantFieldref cfr = constants.getConstantFieldref(gs.index);
			
			if (cfr.class_index != classFile.getThisClassIndex())
				continue;
			
			ConstantNameAndType cnatField = constants.getConstantNameAndType(
					cfr.name_and_type_index);

			String signature = 
				constants.getConstantUtf8(cnatField.descriptor_index);
			
			if (! StringConstants.INTERNAL_CLASS_SIGNATURE.equals(signature))
				continue;
			
			String name = constants.getConstantUtf8(cnatField.name_index);

			if (! name.startsWith(StringConstants.CLASS_DOLLAR))
				continue;

			instruction = ft.instructions.get(0);
			
			if (instruction.opcode != ByteCodeConstants.DUPSTORE)
				continue;
			
			DupStore ds = (DupStore)instruction;
			
			if (ds.objectref.opcode != ByteCodeConstants.INVOKESTATIC)
				continue;
			
			Invokestatic is = (Invokestatic)ds.objectref;
			
			if (is.args.size() != 1)
				continue;

			instruction = is.args.get(0);
			
			if (instruction.opcode != ByteCodeConstants.LDC)
				continue;			

			ConstantMethodref cmr = 
				constants.getConstantMethodref(is.index);
			
			name = constants.getConstantClassName(cmr.class_index);
			
			if (! name.equals(StringConstants.INTERNAL_CLASS_CLASS_NAME))
				continue;
			
			ConstantNameAndType cnatMethod = 
				constants.getConstantNameAndType(cmr.name_and_type_index);
			name = constants.getConstantUtf8(cnatMethod.name_index);
			
			if (! name.equals(StringConstants.FORNAME_METHOD_NAME))
				continue;
			
			Ldc ldc = (Ldc)instruction;			
			ConstantValue cv = constants.getConstantValue(ldc.index);
			
			if (cv.tag != ConstantConstant.CONSTANT_String)
				continue;		
			
			instruction = ft.instructions.get(1);
			
			if (instruction.opcode != ByteCodeConstants.PUTSTATIC)
				continue;
			
			PutStatic ps = (PutStatic)instruction;
			
			if ((ps.index != gs.index) || 
				(ps.valueref.opcode != ByteCodeConstants.DUPLOAD) ||
				(ps.valueref.offset != ds.offset))
				continue;
			
			String exceptionName = 
				constants.getConstantClassName(fc.exceptionTypeIndex);
			
			if (! exceptionName.equals(StringConstants.INTERNAL_CLASSNOTFOUNDEXCEPTION_SIGNATURE))
				continue;
				
			if (fc.instructions.get(0).opcode != ByteCodeConstants.ATHROW)
				continue;
			
			// Trouve !	
			ConstantString cs = (ConstantString)cv;
			String className = constants.getConstantUtf8(cs.string_index);
			String internalName = className.replace(
				StringConstants.PACKAGE_SEPARATOR, 
				StringConstants.INTERNAL_PACKAGE_SEPARATOR);
			
			referenceMap.add(internalName);
			
			// Ajout du nom interne
			int index = constants.addConstantUtf8(internalName);
			// Ajout d'une nouvelle classe
			index = constants.addConstantClass(index);			
			ldc = new Ldc(
				ByteCodeConstants.LDC, ii.offset, 
				ii.lineNumber, index);
			
			// Remplacement de l'intruction GetStatic par l'instruction Ldc
			ReplaceDupLoadVisitor visitor = new ReplaceDupLoadVisitor(ds, ldc);
			
			visitor.visit(list.get(i+2));
			
			// Retrait de l'intruction FastTry
			list.remove(i+1);
			// Retrait de l'intruction IfNotNull
			list.remove(i);	
			
			// Recherche de l'attribut statique et ajout de l'attribut SYNTHETIC
			Field[] fields = classFile.getFields();
			int j = fields.length;
			
			while (j-- > 0)
			{
				Field field = fields[j];
				
				if (field.name_index == cnatField.name_index)
				{
					field.access_flags |= ClassFileConstants.ACC_SYNTHETIC;
					break;
				}
			}
		}
	}
}
