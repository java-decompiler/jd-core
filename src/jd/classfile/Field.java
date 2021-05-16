package jd.classfile;

import jd.Constants;
import jd.classfile.attribute.Attribute;
import jd.classfile.attribute.AttributeConstantValue;
import jd.classfile.constant.ConstantValue;
import jd.instruction.bytecode.instruction.Instruction;


public class Field extends FieldOrMethod
{
	private ValueAndLocalVariables valueAndLocalVariables = null;
	
	public Field(int access_flags, int name_index, int descriptor_index, 
			     Attribute[] attributes)
	{
		super(access_flags, name_index, descriptor_index, attributes);
	}
	
	public ConstantValue getConstantValue(ConstantPool constants) 
	{
		if (this.attributes != null)
			for(int i=0; i<this.attributes.length; i++)
				if (this.attributes[i].tag == Constants.ATTR_CONSTANT_VALUE)
				{
					AttributeConstantValue acv = (AttributeConstantValue)this.attributes[i];
					return constants.getConstantValue(acv.constantvalue_index);
				}

		return null;
	}

	public ValueAndLocalVariables getValueAndLocalVariables() 
		{	return valueAndLocalVariables; }
	
	public void setValueAndLocalVariables(
		Instruction value, LocalVariables localVariables) 
	{ 
		this.valueAndLocalVariables = 
			new ValueAndLocalVariables(value, localVariables); 
	}
	
	public static class ValueAndLocalVariables
	{
		private Instruction value;
		private LocalVariables localVariables;
		
		ValueAndLocalVariables(Instruction value, LocalVariables localVariables)
		{
			this.value = value;
			this.localVariables = localVariables;
		}

		public LocalVariables getLocalVariables() 
		{
			return localVariables;
		}

		public Instruction getValue() 
		{
			return value;
		}		
	}
}
