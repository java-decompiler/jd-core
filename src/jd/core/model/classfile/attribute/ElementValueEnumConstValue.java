package jd.core.model.classfile.attribute;


public class ElementValueEnumConstValue extends ElementValue
{
	final public int type_name_index;
	final public int const_name_index;
	
	public ElementValueEnumConstValue(byte tag, 
			                          int type_name_index, 
			                          int const_name_index) 
	{
		super(tag);
		this.type_name_index = type_name_index;
		this.const_name_index = const_name_index;
	}
}
