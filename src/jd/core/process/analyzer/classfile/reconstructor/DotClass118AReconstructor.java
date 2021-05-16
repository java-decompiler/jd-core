/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
import jd.core.process.analyzer.classfile.visitor.ReplaceDupLoadVisitor;
import jd.core.util.SignatureUtil;
import jd.core.util.StringConstants;


/*
 * Recontruction du mot cle '.class' depuis les instructions generees par le 
 * JDK 1.1.8 de SUN :
 * ...
 * ifnotnull( getstatic( current class, 'class$...', Class ) )
 *  ternaryopstore( getstatic( class, 'class$...' ) )
 *  goto 
 *  dupstore( invokestatic( current class, 'class$', nom de la classe ) )
 *  putstatic( current class, 'class$...', Class, dupload ) )
 *  ??? ( dupload )
 * ...
 */
public class DotClass118AReconstructor 
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
						
			GetStatic gs = (GetStatic)ii.value;			

			int jumpOffset = ii.GetJumpOffset();
			
			instruction = list.get(i+1);
			
			if (instruction.opcode != ByteCodeConstants.TERNARYOPSTORE)
				continue;
			
			TernaryOpStore tos = (TernaryOpStore)instruction;
			
			if ((tos.objectref.opcode != ByteCodeConstants.GETSTATIC) ||
				(gs.index != ((GetStatic)tos.objectref).index))
				continue;

			instruction = list.get(i+2);
		
			if (instruction.opcode != ByteCodeConstants.GOTO)
				continue;
			
			Goto g = (Goto)instruction;
			
			instruction = list.get(i+3);
			
			if (instruction.opcode != ByteCodeConstants.DUPSTORE)
				continue;
			
			if ((g.offset >= jumpOffset) || (jumpOffset > instruction.offset))
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
			ConstantNameAndType cnatMethod = 
				constants.getConstantNameAndType(cmr.name_and_type_index);
			String nameMethod = constants.getConstantUtf8(cnatMethod.name_index);
			
			if (! nameMethod.equals(StringConstants.CLASS_DOLLAR))
				continue;
			
			Ldc ldc = (Ldc)instruction;	
			ConstantValue cv = constants.getConstantValue(ldc.index);
			
			if (cv.tag != ConstantConstant.CONSTANT_String)
				continue;
			
			instruction = list.get(i+4);
			
			if (instruction.opcode != ByteCodeConstants.PUTSTATIC)
				continue;

			PutStatic ps = (PutStatic)instruction;
			
			if ((ps.valueref.opcode != ByteCodeConstants.DUPLOAD) ||
				(ds.offset != ps.valueref.offset))
				continue;
			
			ConstantFieldref cfr = constants.getConstantFieldref(gs.index);
			ConstantNameAndType cnatField = constants.getConstantNameAndType(
				cfr.name_and_type_index);
			String signatureField = 
				constants.getConstantUtf8(cnatField.descriptor_index);
			
			if (! signatureField.equals(StringConstants.INTERNAL_CLASS_SIGNATURE))
				continue;
			
			String nameField = constants.getConstantUtf8(cnatField.name_index);

			if (nameField.startsWith(StringConstants.CLASS_DOLLAR))
			{
				// motif 'x.class' classique trouvé !	
				// Substitution par une constante de type 'ClassConstant'
				ConstantString cs = (ConstantString)cv;
				String signature = constants.getConstantUtf8(cs.string_index);
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
				ReplaceDupLoadVisitor visitor = new ReplaceDupLoadVisitor(ds, ldc);
				
				for (int j=i+5; j<list.size(); j++)
				{
					visitor.visit(list.get(j));
					if (visitor.getParentFound() != null)
						break;
				}
			}
			else if (nameField.startsWith(StringConstants.ARRAY_DOLLAR))
			{
				// motif 'x[].class' trouvé !	
				// Substitution par l'expression 'new x[0].getClass()'
				ConstantString cs = (ConstantString)cv;
				String signature = constants.getConstantUtf8(cs.string_index);
				String signatureWithoutDimension = 
						SignatureUtil.CutArrayDimensionPrefix(signature);
				
				IConst iconst0 = new IConst(
					ByteCodeConstants.ICONST, ii.offset, 
					ii.lineNumber, 0);
				Instruction newArray;
			
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
				
				// Ajout de la méthode 'getClass'
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
				
				// Remplacement de l'intruction 
				ReplaceDupLoadVisitor visitor = new ReplaceDupLoadVisitor(ds, iv);
				
				for (int j=i+5; j<list.size(); j++)
				{
					visitor.visit(list.get(j));
					if (visitor.getParentFound() != null)
						break;
				}
			}
			else
			{
				continue;								
			}
						
			// Retrait de l'intruction PutStatic
			list.remove(i+4);
			// Retrait de l'intruction DupStore
			list.remove(i+3);
			// Retrait de l'intruction Goto 
			list.remove(i+2);
			// Retrait de l'intruction TernaryOpStore
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
			
			// Recherche de la méthode statique et ajout de l'attribut SYNTHETIC
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
	}
}
