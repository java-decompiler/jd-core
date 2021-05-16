package jd.core.model.classfile.constant;


public class ConstantString extends ConstantValue 
{
	final public int string_index;
	
	public ConstantString(byte tag, int string_index)
	{
		super(tag);
		this.string_index = string_index;
	}    
}
