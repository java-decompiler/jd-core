package jd.core.model.classfile;

import java.util.ArrayList;
import java.util.List;

import jd.core.model.classfile.constant.Constant;
import jd.core.model.classfile.constant.ConstantClass;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantInteger;
import jd.core.model.classfile.constant.ConstantInterfaceMethodref;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.classfile.constant.ConstantUtf8;
import jd.core.model.classfile.constant.ConstantValue;
import jd.core.util.IndexToIndexMap;
import jd.core.util.InvalidParameterException;
import jd.core.util.StringConstants;
import jd.core.util.StringToIndexMap;


public class ConstantPool 
{
	private ArrayList<Constant> listOfConstants;
	private StringToIndexMap constantUtf8ToIndex;
	private IndexToIndexMap constantClassToIndex;
	
	public final int instanceConstructorIndex;
	public final int classConstructorIndex;
	public final int internalDeprecatedSignatureIndex;	
	public final int toStringIndex;
	public final int valueOfIndex;
	public final int appendIndex;
	
	public final int objectClassIndex;
	
	public final int objectClassNameIndex;
	public final int stringClassNameIndex;
	public final int stringBufferClassNameIndex;
	public final int stringBuilderClassNameIndex;
	
	public final int objectSignatureIndex;
	
	public final int thisLocalVariableNameIndex;
	
	public final int annotationDefaultAttributeNameIndex;
	public final int codeAttributeNameIndex;
	public final int constantValueAttributeNameIndex;
	public final int deprecatedAttributeNameIndex;
	public final int enclosingMethodAttributeNameIndex;
	public final int exceptionsAttributeNameIndex;
	public final int innerClassesAttributeNameIndex;
	public final int lineNumberTableAttributeNameIndex;
	public final int localVariableTableAttributeNameIndex;
	public final int localVariableTypeTableAttributeNameIndex;
	public final int runtimeInvisibleAnnotationsAttributeNameIndex;
	public final int runtimeVisibleAnnotationsAttributeNameIndex;
	public final int runtimeInvisibleParameterAnnotationsAttributeNameIndex;
	public final int runtimeVisibleParameterAnnotationsAttributeNameIndex;
	public final int signatureAttributeNameIndex;
	public final int sourceFileAttributeNameIndex;
	public final int syntheticAttributeNameIndex;



	public ConstantPool(Constant[] constants)
	{
		this.listOfConstants = new ArrayList<Constant>();
		this.constantUtf8ToIndex = new StringToIndexMap();
		this.constantClassToIndex = new IndexToIndexMap();
		
		for (int i=0; i<constants.length; i++)
		{
			Constant constant = constants[i];
			
			int index = this.listOfConstants.size();
			this.listOfConstants.add(constant);
			
			if (constant == null)
				continue;
			
			switch (constant.tag)
			{
			case ConstantConstant.CONSTANT_Utf8:				
				this.constantUtf8ToIndex.put(
						((ConstantUtf8)constant).bytes, index);
				break;
			case ConstantConstant.CONSTANT_Class:
				this.constantClassToIndex.put(
						((ConstantClass)constant).name_index, index);
			}
		}
		
		// Add instance constructor
		this.instanceConstructorIndex = 
			addConstantUtf8(StringConstants.INSTANCE_CONSTRUCTOR);
		
		// Add class constructor
		this.classConstructorIndex = 
			addConstantUtf8(StringConstants.CLASS_CONSTRUCTOR);
		
		// Add internal deprecated signature
		this.internalDeprecatedSignatureIndex = 
			addConstantUtf8(StringConstants.INTERNAL_DEPRECATED_SIGNATURE);
		
		// -- Add method names --------------------------------------------- //
		// Add 'toString'
		this.toStringIndex = addConstantUtf8(StringConstants.TOSTRING_METHOD_NAME);
		
		// Add 'valueOf'
		this.valueOfIndex = addConstantUtf8(StringConstants.VALUEOF_METHOD_NAME);
		
		// Add 'append'
		this.appendIndex = addConstantUtf8(StringConstants.APPEND_METHOD_NAME);
		
		// -- Add class names ---------------------------------------------- //
		// Add 'Object'
		this.objectClassNameIndex = 
			addConstantUtf8(StringConstants.INTERNAL_OBJECT_CLASS_NAME);
		this.objectClassIndex = 
			addConstantClass(this.objectClassNameIndex);
		this.objectSignatureIndex = 
			addConstantUtf8(StringConstants.INTERNAL_OBJECT_SIGNATURE);
		
		// Add 'String'
		this.stringClassNameIndex = 
			addConstantUtf8(StringConstants.INTERNAL_STRING_CLASS_NAME);
		
		// Add 'StringBuffer'
		this.stringBufferClassNameIndex = 
			addConstantUtf8(StringConstants.INTERNAL_STRINGBUFFER_CLASS_NAME);
		
		// Add 'StringBuilder'
		this.stringBuilderClassNameIndex = 
			addConstantUtf8(StringConstants.INTERNAL_STRINGBUILDER_CLASS_NAME);
		
		// Add 'this'
		this.thisLocalVariableNameIndex = 
			addConstantUtf8(StringConstants.THIS_LOCAL_VARIABLE_NAME);
		
		// -- Add attribute names ------------------------------------------ //
		this.annotationDefaultAttributeNameIndex = 
			addConstantUtf8(StringConstants.ANNOTATIONDEFAULT_ATTRIBUTE_NAME);
		
		this.codeAttributeNameIndex = 
			addConstantUtf8(StringConstants.CODE_ATTRIBUTE_NAME);
		
		this.constantValueAttributeNameIndex = 
			addConstantUtf8(StringConstants.CONSTANTVALUE_ATTRIBUTE_NAME);
		
		this.deprecatedAttributeNameIndex = 
			addConstantUtf8(StringConstants.DEPRECATED_ATTRIBUTE_NAME);
		
		this.enclosingMethodAttributeNameIndex = 
			addConstantUtf8(StringConstants.ENCLOSINGMETHOD_ATTRIBUTE_NAME);
		
		this.exceptionsAttributeNameIndex = 
			addConstantUtf8(StringConstants.EXCEPTIONS_ATTRIBUTE_NAME);
		
		this.innerClassesAttributeNameIndex = 
			addConstantUtf8(StringConstants.INNERCLASSES_ATTRIBUTE_NAME);
		
		this.lineNumberTableAttributeNameIndex = 
			addConstantUtf8(StringConstants.LINENUMBERTABLE_ATTRIBUTE_NAME);
		
		this.localVariableTableAttributeNameIndex = 
			addConstantUtf8(StringConstants.LOCALVARIABLETABLE_ATTRIBUTE_NAME);
		
		this.localVariableTypeTableAttributeNameIndex = 
			addConstantUtf8(StringConstants.LOCALVARIABLETYPETABLE_ATTRIBUTE_NAME);
		
		this.runtimeInvisibleAnnotationsAttributeNameIndex = 
			addConstantUtf8(StringConstants.RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_NAME);
		
		this.runtimeVisibleAnnotationsAttributeNameIndex = 
			addConstantUtf8(StringConstants.RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME);
		
		this.runtimeInvisibleParameterAnnotationsAttributeNameIndex = 
			addConstantUtf8(StringConstants.RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_NAME);
		
		this.runtimeVisibleParameterAnnotationsAttributeNameIndex = 
			addConstantUtf8(StringConstants.RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_NAME);
		
		this.signatureAttributeNameIndex = 
			addConstantUtf8(StringConstants.SIGNATURE_ATTRIBUTE_NAME);
		
		this.sourceFileAttributeNameIndex = 
			addConstantUtf8(StringConstants.SOURCEFILE_ATTRIBUTE_NAME);
		
		this.syntheticAttributeNameIndex = 
			addConstantUtf8(StringConstants.SYNTHETIC_ATTRIBUTE_NAME);
	
	}

	public Constant get(int i)
	{
		return this.listOfConstants.get(i);
	}
	
	public int size()
	{
		return this.listOfConstants.size();
	}
	
	// -- Constants -------------------------------------------------------- //
	
	public int addConstantUtf8(String s)
	{
		if (s == null)
			throw new InvalidParameterException("Constant string is null");
		
		assert !s.startsWith("L[");
		
		int index = this.constantUtf8ToIndex.get(s);
		
		if (index == -1)
		{
			ConstantUtf8 cutf8 = 
				new ConstantUtf8(ConstantConstant.CONSTANT_Utf8, s);
			index = this.listOfConstants.size();
			this.listOfConstants.add(cutf8);
			this.constantUtf8ToIndex.put(s, index);
		}
		
		return index;
	}
	
	public int addConstantClass(int name_index)
	{
		String internalName = getConstantUtf8(name_index);
		if ((internalName == null) || 
			(internalName.length() == 0) || 
			(internalName.charAt(internalName.length()-1) == ';'))
			System.err.println("ConstantPool.addConstantClass: invalid name index");
		
		int index = this.constantClassToIndex.get(name_index);
		
		if (index == -1)
		{
			ConstantClass cc = 
				new ConstantClass(ConstantConstant.CONSTANT_Class, name_index);
			index = this.listOfConstants.size();
			this.listOfConstants.add(cc);
			this.constantClassToIndex.put(name_index, index);
		}
		
		return index;
	}
	
	public int addConstantNameAndType(int name_index, int descriptor_index)
	{
		int index = this.listOfConstants.size();
		
		while (--index > 0)
		{
			Constant constant = this.listOfConstants.get(index);
			
			if ((constant == null) ||
				(constant.tag != ConstantConstant.CONSTANT_NameAndType))
				continue;
			
			ConstantNameAndType cnat = (ConstantNameAndType)constant;
			
			if ((cnat.name_index == name_index) && 
				(cnat.descriptor_index == descriptor_index))
				return index;
		}
		
		ConstantNameAndType cnat = new ConstantNameAndType(
				ConstantConstant.CONSTANT_NameAndType, name_index, descriptor_index);
		index = this.listOfConstants.size();
		this.listOfConstants.add(cnat);
		
		return index;
	}
	
	public int addConstantFieldref(int class_index, int name_and_type_index)
	{
		int index = this.listOfConstants.size();
		
		while (--index > 0)
		{
			Constant constant = this.listOfConstants.get(index);
			
			if ((constant == null) ||
				(constant.tag != ConstantConstant.CONSTANT_Fieldref))
				continue;
			
			ConstantFieldref cfr = (ConstantFieldref)constant;
			
			if ((cfr.class_index == class_index) && 
				(cfr.name_and_type_index == name_and_type_index))
				return index;
		}
		
		ConstantFieldref cfr = new ConstantFieldref(
				ConstantConstant.CONSTANT_Fieldref, class_index, name_and_type_index);
		index = this.listOfConstants.size();
		this.listOfConstants.add(cfr);
		
		return index;
	}
	
	public int addConstantMethodref(int class_index, int name_and_type_index)
	{
		return addConstantMethodref(
			class_index, name_and_type_index, null, null);
	}
	
	public int addConstantMethodref(
		int class_index, int name_and_type_index,
		List<String> listOfParameterSignatures, String returnedSignature)
	{
		int index = this.listOfConstants.size();
		
		while (--index > 0)
		{
			Constant constant = this.listOfConstants.get(index);
			
			if ((constant == null) ||
				(constant.tag != ConstantConstant.CONSTANT_Methodref))
				continue;
			
			ConstantMethodref cmr = (ConstantMethodref)constant;
			
			if ((cmr.class_index == class_index) && 
				(cmr.name_and_type_index == name_and_type_index))
				return index;
		}
		
		ConstantMethodref cfr = new ConstantMethodref(
			ConstantConstant.CONSTANT_Methodref, class_index, name_and_type_index, 
			listOfParameterSignatures, returnedSignature);
		index = this.listOfConstants.size();
		this.listOfConstants.add(cfr);
		
		return index;
	}
	
	public String getConstantUtf8(int index)
	{
		ConstantUtf8 cutf8 = (ConstantUtf8)this.listOfConstants.get(index);
		return cutf8.bytes;
	}

	public String getConstantClassName(int index)
	{
		ConstantClass cc = (ConstantClass)this.listOfConstants.get(index);
		ConstantUtf8 cutf8 = (ConstantUtf8)this.listOfConstants.get(cc.name_index);
		return cutf8.bytes;
	}
	
	public ConstantClass getConstantClass(int index)
	{
		return (ConstantClass)this.listOfConstants.get(index);
	}
	
	public ConstantFieldref getConstantFieldref(int index)
	{
		return (ConstantFieldref)this.listOfConstants.get(index);
	}
	
	public ConstantNameAndType getConstantNameAndType(int index)
	{
		return (ConstantNameAndType)this.listOfConstants.get(index);
	}
	
	public ConstantMethodref getConstantMethodref(int index)
	{
		return (ConstantMethodref)this.listOfConstants.get(index);
	}
	
	public ConstantInterfaceMethodref getConstantInterfaceMethodref(int index)
	{
		return (ConstantInterfaceMethodref)this.listOfConstants.get(index);
	}
	
	public ConstantValue getConstantValue(int index)
	{
		return (ConstantValue)this.listOfConstants.get(index);
	}	
	
	public ConstantInteger getConstantInteger(int index)
	{
		return (ConstantInteger)this.listOfConstants.get(index);
	}
}
