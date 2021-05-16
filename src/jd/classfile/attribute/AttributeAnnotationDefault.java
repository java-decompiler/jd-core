package jd.classfile.attribute;


public class AttributeAnnotationDefault extends Attribute
{
	//private ElementValue default_value;
	
	public AttributeAnnotationDefault(byte tag, 
			                          int attribute_name_index, 
			                          ElementValue default_value) 
	{
		super(tag, attribute_name_index);
		//this.default_value = default_value;
	}
}
