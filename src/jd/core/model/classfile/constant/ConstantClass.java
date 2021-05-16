package jd.core.model.classfile.constant;


public class ConstantClass extends Constant 
{
	final public int name_index;
	
	public ConstantClass(byte tag, int name_index)
	{
		super(tag);
		this.name_index = name_index;
	}
}
