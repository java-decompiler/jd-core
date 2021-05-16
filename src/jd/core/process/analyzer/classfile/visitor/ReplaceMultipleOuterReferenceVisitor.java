package jd.core.process.analyzer.classfile.visitor;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.util.SignatureUtil;


/*
 * Replace 'this.this$3.this$2.this$1.this$0.xxx' by 'TestInnerClass.this.xxx'
 */
public class ReplaceMultipleOuterReferenceVisitor 
	extends ReplaceOuterAccessorVisitor
{
	public ReplaceMultipleOuterReferenceVisitor(ClassFile classFile)
	{
		super(classFile);
	}
	
	protected ClassFile match(Instruction instruction)
	{
		if (instruction.opcode != ByteCodeConstants.GETFIELD)
			return null;
		
		GetField gf = (GetField)instruction;
		
		switch (gf.objectref.opcode) 
		{
		case ByteCodeConstants.ALOAD:
			{
				ALoad aload = (ALoad)gf.objectref;			
				if (aload.index != 0)
					return null;
				Field field = this.classFile.getOuterThisField();
				if (field == null)
					return null;
				ConstantPool constants = classFile.getConstantPool();
				ConstantFieldref cfr = 
					constants.getConstantFieldref(gf.index);
				if (cfr.class_index != classFile.getThisClassIndex())
					return null;
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cfr.name_and_type_index);	
				if ((field.name_index != cnat.name_index) || 
					(field.descriptor_index != cnat.descriptor_index))
					return null;
				return this.classFile.getOuterClass();
			}
		case ByteCodeConstants.OUTERTHIS:
			{
				ConstantPool constants = this.classFile.getConstantPool();
				GetStatic gs = (GetStatic)gf.objectref;
				ConstantFieldref cfr = 
					constants.getConstantFieldref(gs.index);
				String className = 
					constants.getConstantClassName(cfr.class_index);
				ClassFile outerClass = this.classFile.getOuterClass();
				
				while (outerClass != null)
				{
					if (outerClass.getThisClassName().equals(className))
					{
						Field outerField = outerClass.getOuterThisField();
							
						if (outerField == null)
							return null;

						cfr = constants.getConstantFieldref(gf.index);
						ConstantNameAndType cnat = 
							constants.getConstantNameAndType(cfr.name_and_type_index);							
						String fieldName = 
							constants.getConstantUtf8(cnat.name_index);	
					
						ConstantPool outerConstants = 
							outerClass.getConstantPool();
						String outerFieldName = 
							outerConstants.getConstantUtf8(outerField.name_index);	
						
						if (!fieldName.equals(outerFieldName))
							return null;
						
						String fieldDescriptor = 
							constants.getConstantUtf8(cnat.descriptor_index);	
						String outerFieldDescriptor = 
							outerConstants.getConstantUtf8(outerField.descriptor_index);	
						
						if (!fieldDescriptor.equals(outerFieldDescriptor))
							return null;
						
						return outerClass.getOuterClass();
					}
					
					outerClass = outerClass.getOuterClass();
				}

				return null;
			}
		case ByteCodeConstants.GETFIELD:
			{
				ConstantPool constants = this.classFile.getConstantPool();
				ConstantFieldref cfr = 
					constants.getConstantFieldref(gf.index);				
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cfr.name_and_type_index);
				String descriptorName = 
					constants.getConstantUtf8(cnat.descriptor_index);	
				if (!SignatureUtil.IsObjectSignature(descriptorName))
					return null;
				
				ClassFile matchedClassFile = match(gf.objectref);
				if ((matchedClassFile == null) || 
					!matchedClassFile.isAInnerClass())
					return null;
				
				Field matchedField = matchedClassFile.getOuterThisField();
				if (matchedField == null)
					return null;
				
				String className = 
					constants.getConstantClassName(cfr.class_index);	
				
				if (!className.equals(matchedClassFile.getThisClassName()))
					return null;
				
				String fieldName = constants.getConstantUtf8(cnat.name_index);
								
				ConstantPool matchedConstants = matchedClassFile.getConstantPool();
				String matchedFieldName = 
					matchedConstants.getConstantUtf8(matchedField.name_index);

				if (! fieldName.equals(matchedFieldName))
					return null;
				
				String matchedDescriptorName = 
					matchedConstants.getConstantUtf8(matchedField.descriptor_index);	

				if (! descriptorName.equals(matchedDescriptorName))
					return null;
				
				return matchedClassFile.getOuterClass();
			}
		default:
			return null;	
		}
	}
}
