package jd.core.model.classfile.attribute;


public class ElementValuePrimitiveType extends ElementValue
{
	/*
	 * type = {'B', 'D', 'F', 'I', 'J', 'S', 'Z', 'C', 's'}
	 */	
	public byte type;
	final public int const_value_index;

	public ElementValuePrimitiveType(byte tag, byte type, int const_value_index) 
	{
		super(tag);
		this.type = type;
		this.const_value_index = const_value_index;
	}
}
