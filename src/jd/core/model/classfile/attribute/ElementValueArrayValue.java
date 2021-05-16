package jd.core.model.classfile.attribute;


public class ElementValueArrayValue extends ElementValue
{
	final public ElementValue[] values;
	
	public ElementValueArrayValue(byte tag, ElementValue[] values) 
	{ 
		super(tag);
		this.values = values;
	}
}
