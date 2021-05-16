package jd.core.model.classfile.attribute;


public class AttributeConstantValue extends Attribute
{
	public final int constantvalue_index;
	
	public AttributeConstantValue(byte tag, 
			                      int attribute_name_index, 
			                      int constantvalue_index) 
	{
		super(tag, attribute_name_index);
		this.constantvalue_index = constantvalue_index;
	}
}
