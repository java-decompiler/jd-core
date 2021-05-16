package jd.classfile;

import java.util.ArrayList;
import java.util.List;

import jd.Constants;
import jd.classfile.constant.Constant;
import jd.classfile.constant.ConstantClass;
import jd.classfile.constant.ConstantFieldref;
import jd.classfile.constant.ConstantInteger;
import jd.classfile.constant.ConstantInterfaceMethodref;
import jd.classfile.constant.ConstantMethodref;
import jd.classfile.constant.ConstantNameAndType;
import jd.classfile.constant.ConstantUtf8;
import jd.classfile.constant.ConstantValue;
import jd.exception.InvalidParameterException;
import jd.util.IndexToIndexMap;
import jd.util.StringToIndexMap;


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
	public final int stringClassIndex;
	public final int stringBufferClassIndex;
	public final int stringBuilderClassIndex;
	
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
			case Constants.CONSTANT_Utf8:				
				this.constantUtf8ToIndex.put(
						((ConstantUtf8)constant).bytes, index);
				break;
			case Constants.CONSTANT_Class:
				this.constantClassToIndex.put(
						((ConstantClass)constant).name_index, index);
			}
		}
		
		// Add instance constructor
		this.instanceConstructorIndex = 
			addConstantUtf8(Constants.INSTANCE_CONSTRUCTOR);
		
		// Add class constructor
		this.classConstructorIndex = 
			addConstantUtf8(Constants.CLASS_CONSTRUCTOR);
		
		// Add internal deprecated signature
		this.internalDeprecatedSignatureIndex = 
			addConstantUtf8(Constants.INTERNAL_DEPRECATED_SIGNATURE);
		
		// -- Add method names --------------------------------------------- //
		// Add 'toString'
		this.toStringIndex = addConstantUtf8(Constants.TOSTRING_METHOD_NAME);
		
		// Add 'valueOf'
		this.valueOfIndex = addConstantUtf8(Constants.VALUEOF_METHOD_NAME);
		
		// Add 'append'
		this.appendIndex = addConstantUtf8(Constants.APPEND_METHOD_NAME);
		
		// -- Add class names ---------------------------------------------- //
		// Add 'Object'
		int signatureIndex = 
			addConstantUtf8(Constants.INTERNAL_OBJECT_CLASS_NAME);
		this.objectClassIndex = addConstantClass(signatureIndex);
		
		// Add 'String'
		signatureIndex = 
			addConstantUtf8(Constants.INTERNAL_STRING_CLASS_NAME);
		this.stringClassIndex = addConstantClass(signatureIndex);
		
		// Add 'StringBuffer'
		signatureIndex = 
			addConstantUtf8(Constants.INTERNAL_STRINGBUFFER_CLASS_NAME);
		this.stringBufferClassIndex = addConstantClass(signatureIndex);
		
		// Add 'StringBuilder'
		signatureIndex = 
			addConstantUtf8(Constants.INTERNAL_STRINGBUILDER_CLASS_NAME);
		this.stringBuilderClassIndex = addConstantClass(signatureIndex);
		
		// Add 'this'
		this.thisLocalVariableNameIndex = 
			addConstantUtf8(Constants.THIS_LOCAL_VARIABLE_NAME);
		
		// -- Add attribute names ------------------------------------------ //
		this.annotationDefaultAttributeNameIndex = 
			addConstantUtf8(Constants.ANNOTATIONDEFAULT_ATTRIBUTE_NAME);
		
		this.codeAttributeNameIndex = 
			addConstantUtf8(Constants.CODE_ATTRIBUTE_NAME);
		
		this.constantValueAttributeNameIndex = 
			addConstantUtf8(Constants.CONSTANTVALUE_ATTRIBUTE_NAME);
		
		this.deprecatedAttributeNameIndex = 
			addConstantUtf8(Constants.DEPRECATED_ATTRIBUTE_NAME);
		
		this.enclosingMethodAttributeNameIndex = 
			addConstantUtf8(Constants.ENCLOSINGMETHOD_ATTRIBUTE_NAME);
		
		this.exceptionsAttributeNameIndex = 
			addConstantUtf8(Constants.EXCEPTIONS_ATTRIBUTE_NAME);
		
		this.innerClassesAttributeNameIndex = 
			addConstantUtf8(Constants.INNERCLASSES_ATTRIBUTE_NAME);
		
		this.lineNumberTableAttributeNameIndex = 
			addConstantUtf8(Constants.LINENUMBERTABLE_ATTRIBUTE_NAME);
		
		this.localVariableTableAttributeNameIndex = 
			addConstantUtf8(Constants.LOCALVARIABLETABLE_ATTRIBUTE_NAME);
		
		this.runtimeInvisibleAnnotationsAttributeNameIndex = 
			addConstantUtf8(Constants.RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_NAME);
		
		this.runtimeVisibleAnnotationsAttributeNameIndex = 
			addConstantUtf8(Constants.RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME);
		
		this.runtimeInvisibleParameterAnnotationsAttributeNameIndex = 
			addConstantUtf8(Constants.RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_NAME);
		
		this.runtimeVisibleParameterAnnotationsAttributeNameIndex = 
			addConstantUtf8(Constants.RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_NAME);
		
		this.signatureAttributeNameIndex = 
			addConstantUtf8(Constants.SIGNATURE_ATTRIBUTE_NAME);
		
		this.sourceFileAttributeNameIndex = 
			addConstantUtf8(Constants.SOURCEFILE_ATTRIBUTE_NAME);
		
		this.syntheticAttributeNameIndex = 
			addConstantUtf8(Constants.SYNTHETIC_ATTRIBUTE_NAME);
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
		
		int index = this.constantUtf8ToIndex.get(s);
		
		if (index == -1)
		{
			ConstantUtf8 cutf8 = 
				new ConstantUtf8(Constants.CONSTANT_Utf8, s);
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
			(internalName.charAt(0) == 'L'))
			System.err.println("ConstantPool.addConstantClass: invalid name index");
		
		int index = this.constantClassToIndex.get(name_index);
		
		if (index == -1)
		{
			ConstantClass cc = 
				new ConstantClass(Constants.CONSTANT_Class, name_index);
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
				(constant.tag != Constants.CONSTANT_NameAndType))
				continue;
			
			ConstantNameAndType cnat = (ConstantNameAndType)constant;
			
			if ((cnat.name_index == name_index) && 
				(cnat.descriptor_index == descriptor_index))
				return index;
		}
		
		ConstantNameAndType cnat = new ConstantNameAndType(
			Constants.CONSTANT_NameAndType, name_index, descriptor_index);
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
				(constant.tag != Constants.CONSTANT_Fieldref))
				continue;
			
			ConstantFieldref cfr = (ConstantFieldref)constant;
			
			if ((cfr.class_index == class_index) && 
				(cfr.name_and_type_index == name_and_type_index))
				return index;
		}
		
		ConstantFieldref cfr = new ConstantFieldref(
			Constants.CONSTANT_Fieldref, class_index, name_and_type_index);
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
				(constant.tag != Constants.CONSTANT_Methodref))
				continue;
			
			ConstantMethodref cmr = (ConstantMethodref)constant;
			
			if ((cmr.class_index == class_index) && 
				(cmr.name_and_type_index == name_and_type_index))
				return index;
		}
		
		ConstantMethodref cfr = new ConstantMethodref(
			Constants.CONSTANT_Methodref, class_index, name_and_type_index, 
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
