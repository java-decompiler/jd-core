package jd.core.model.classfile.attribute;


public class AttributeEnclosingMethod extends Attribute
{
	public final int class_index;
	public final int method_index;
	
	public AttributeEnclosingMethod(byte tag, int attribute_name_index, 
			                        int class_index, int method_index) 
	{
		super(tag, attribute_name_index);
		this.class_index = class_index;
		this.method_index = method_index;
	}
}
