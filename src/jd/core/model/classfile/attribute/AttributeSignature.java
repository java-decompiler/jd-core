package jd.core.model.classfile.attribute;


public class AttributeSignature extends Attribute
{
	public final int signature_index;
	
	public AttributeSignature(
			byte tag, int attribute_name_index, int signature_index) 
	{
		super(tag, attribute_name_index);
		this.signature_index = signature_index;
	}
}
