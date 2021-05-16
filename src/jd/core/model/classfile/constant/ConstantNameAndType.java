package jd.core.model.classfile.constant;


public class ConstantNameAndType extends Constant 
{
	public int name_index;
	final public int descriptor_index;

	public ConstantNameAndType(byte tag, int name_index, int descriptor_index)
	{
		super(tag);
		this.name_index = name_index;
		this.descriptor_index = descriptor_index;
	}    
}
