package jd.core.model.classfile.attribute;


public class ElementValueClassInfo extends ElementValue
{
	final public int class_info_index;
	
	public ElementValueClassInfo(byte tag, int class_info_index) 
	{ 
		super(tag);
		this.class_info_index = class_info_index;
	}
}
