package jd.core.process.analyzer.classfile.reconstructor;

import java.util.ArrayList;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.classfile.constant.ConstantString;
import jd.core.model.classfile.constant.ConstantValue;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;
import jd.core.model.instruction.bytecode.instruction.Ldc;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.reference.ReferenceMap;
import jd.core.process.analyzer.classfile.visitor.ReplaceGetStaticVisitor;
import jd.core.util.SignatureUtil;
import jd.core.util.StringConstants;


/*
 * Recontruction du mot cle '.class' depuis les instructions generees par le 
 * JDK 1.4 de SUN :
 * ...
 * ifnotnull( getstatic( current or outer class, 'class$...', Class ) )
 *  dupstore( invokestatic( current or outer class, 'class$', nom de la classe ) )
 *  putstatic( current class, 'class$...', Class, dupload )
 *  ternaryOpStore( dupload )
 *  goto 
 * ???( getstatic( class, 'class$...' ) )
 * ...
 */
public class DotClass14Reconstructor 
{
	public static void Reconstruct(
		ReferenceMap referenceMap, ClassFile classFile, List<Instruction> list)
	{
		int i = list.size();
		
		if  (i < 6)
			return;
		
		i -= 5;
		ConstantPool constants = classFile.getConstantPool();
		
		while (i-- > 0)
		{
			Instruction instruction = list.get(i);
			
			if (instruction.opcode != ByteCodeConstants.IFXNULL)
				continue;
			
			IfInstruction ii = (IfInstruction)instruction;
		
			if (ii.value.opcode != ByteCodeConstants.GETSTATIC)
				continue;
						
			int jumpOffset = ii.GetJumpOffset();
			
			instruction = list.get(i+1);
			
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

			instruction = list.get(i+2);
			
			if (instruction.opcode != ByteCodeConstants.PUTSTATIC)
				continue;
			
			PutStatic ps = (PutStatic)instruction;
			
			if ((ps.valueref.opcode != ByteCodeConstants.DUPLOAD) ||
				(ds.offset != ps.valueref.offset))
				continue;
			
			instruction = list.get(i+3);
			
			if (instruction.opcode != ByteCodeConstants.TERNARYOPSTORE)
				continue;
			
			TernaryOpStore tos = (TernaryOpStore)instruction;
			
			if ((tos.objectref.opcode != ByteCodeConstants.DUPLOAD) ||
				(ds.offset != tos.objectref.offset))
				continue;

			instruction = list.get(i+4);
			
			if (instruction.opcode != ByteCodeConstants.GOTO)
				continue;
			
			Goto g = (Goto)instruction;
			instruction = list.get(i+5);
			
			if ((g.offset >= jumpOffset) || (jumpOffset > instruction.offset))
				continue;
			
			GetStatic gs = (GetStatic)ii.value;			

			if (ps.index != gs.index)
				continue;
			
			ConstantFieldref cfr = constants.getConstantFieldref(gs.index);
			
			if (searchMatchingClassFile(cfr.class_index, classFile) == null)
				continue;
			
			ConstantNameAndType cnatField = constants.getConstantNameAndType(
					cfr.name_and_type_index);

			String descriptorField = 
				constants.getConstantUtf8(cnatField.descriptor_index);
			
			if (! descriptorField.equals(StringConstants.INTERNAL_CLASS_SIGNATURE))
				continue;
			
			String nameField = constants.getConstantUtf8(cnatField.name_index);

			if (!nameField.startsWith(StringConstants.CLASS_DOLLAR) && 
				!nameField.startsWith(StringConstants.ARRAY_DOLLAR))
				continue;
			
			ConstantMethodref cmr = 
				constants.getConstantMethodref(is.index);
			
			ClassFile matchingClassFile = 
				searchMatchingClassFile(cmr.class_index, classFile);
			if (matchingClassFile == null)
				continue;
			
			ConstantNameAndType cnatMethod = 
				constants.getConstantNameAndType(cmr.name_and_type_index);
			String nameMethod = 
				constants.getConstantUtf8(cnatMethod.name_index);
			
			if (! nameMethod.equals(StringConstants.CLASS_DOLLAR))
				continue;
			
			Ldc ldc = (Ldc)is.args.get(0);	
			ConstantValue cv = constants.getConstantValue(ldc.index);
			
			if (cv.tag != ConstantConstant.CONSTANT_String)
				continue;
			
			// Trouve !			
			ConstantString cs = (ConstantString)cv;
			String signature = constants.getConstantUtf8(cs.string_index);
			
			if (SignatureUtil.GetArrayDimensionCount(signature) == 0)
			{
				String internalName = signature.replace(
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
				ReplaceGetStaticVisitor visitor = 
					new ReplaceGetStaticVisitor(gs.index, ldc);
				
				visitor.visit(instruction);	
			}
			else
			{
				IConst iconst0 = new IConst(
					ByteCodeConstants.ICONST, ii.offset, 
					ii.lineNumber, 0);
				Instruction newArray;

				String signatureWithoutDimension = 
					SignatureUtil.CutArrayDimensionPrefix(signature);
				
				if (SignatureUtil.IsObjectSignature(signatureWithoutDimension))
				{
				    //  8: iconst_0
				    //  9: anewarray 62	java/lang/String
				    //  12: invokevirtual 64	java/lang/Object:getClass	()Ljava/lang/Class;
					String tmp = signatureWithoutDimension.replace(
						StringConstants.PACKAGE_SEPARATOR, 
						StringConstants.INTERNAL_PACKAGE_SEPARATOR);
					String internalName = tmp.substring(1, tmp.length()-1);	

					// Ajout du nom de la classe pour generer la liste des imports
					referenceMap.add(internalName);
					// Ajout du nom interne
					int index = constants.addConstantUtf8(internalName);
					// Ajout d'une nouvelle classe
					index = constants.addConstantClass(index);			

					newArray = new ANewArray(
						ByteCodeConstants.ANEWARRAY, ii.offset, 
						ii.lineNumber, index, iconst0);
				}
				else
				{
				    //  8: iconst_0
				    //  9: newarray byte
				    //  11: invokevirtual 62	java/lang/Object:getClass	()Ljava/lang/Class;
					newArray = new NewArray(
						ByteCodeConstants.NEWARRAY, ii.offset, ii.lineNumber, 
						SignatureUtil.GetTypeFromSignature(signatureWithoutDimension),
						iconst0);
				}
				
				// Ajout de la methode 'getClass'
				int methodNameIndex = constants.addConstantUtf8("getClass");
				int methodDescriptorIndex = 
					constants.addConstantUtf8("()Ljava/lang/Class;");
				int nameAndTypeIndex = constants.addConstantNameAndType(
					methodNameIndex, methodDescriptorIndex);
				int cmrIndex = constants.addConstantMethodref(
					constants.objectClassIndex, nameAndTypeIndex);	
				
				Invokevirtual iv = new Invokevirtual(
					ByteCodeConstants.INVOKEVIRTUAL, ii.offset, 
					ii.lineNumber, cmrIndex, newArray, 
					new ArrayList<Instruction>(0));
				
				// Remplacement de l'intruction GetStatic
				ReplaceGetStaticVisitor visitor = 
					new ReplaceGetStaticVisitor(gs.index, iv);
				
				visitor.visit(instruction);	
			}

			// Retrait de l'intruction Goto
			list.remove(i+4);
			// Retrait de l'intruction TernaryOpStore
			list.remove(i+3);
			// Retrait de l'intruction PutStatic
			list.remove(i+2);
			// Retrait de l'intruction DupStore
			list.remove(i+1);
			// Retrait de l'intruction IfNotNull
			list.remove(i);	
			
			if (matchingClassFile == classFile)
			{
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
				
				// Recherche de la methode statique et ajout de l'attribut SYNTHETIC
				Method[] methods = classFile.getMethods();
				j = methods.length;
				
				while (j-- > 0)
				{
					Method method = methods[j];
					
					if (method.name_index == cnatMethod.name_index)
					{
						method.access_flags |= ClassFileConstants.ACC_SYNTHETIC;
						break;
					}
				}
			}
			else
			{
				// Recherche de l'attribut statique et ajout de l'attribut SYNTHETIC
				ConstantPool matchingConstants = 
					matchingClassFile.getConstantPool();
				Field[] fields = matchingClassFile.getFields();
				int j = fields.length;
				
				
				while (j-- > 0)
				{
					Field field = fields[j];
					
					if (nameField.equals(
							matchingConstants.getConstantUtf8(field.name_index)))
					{
						field.access_flags |= ClassFileConstants.ACC_SYNTHETIC;
						break;
					}
				}

				// Recherche de la methode statique et ajout de l'attribut SYNTHETIC
				Method[] methods = matchingClassFile.getMethods();
				j = methods.length;
				
				while (j-- > 0)
				{
					Method method = methods[j];
					
					if (nameMethod.equals(
							matchingConstants.getConstantUtf8(method.name_index)))
					{
						method.access_flags |= ClassFileConstants.ACC_SYNTHETIC;
						break;
					}
				}				
			}
		}
	}
	
	private static ClassFile searchMatchingClassFile(
		int classIndex, ClassFile classFile)
	{
		if (classIndex == classFile.getThisClassIndex())
			return classFile;
		
		String className = 
			classFile.getConstantPool().getConstantClassName(classIndex);
		
		for (;;)
		{
			classFile = classFile.getOuterClass();
			
			if (classFile == null)
				return null;
			
			if (classFile.getThisClassName().equals(className))
				return classFile;
		}
	}
}
