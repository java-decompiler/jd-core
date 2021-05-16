package jd.core.model.classfile.constant;


public class ConstantFieldref extends Constant 
{
	final public int class_index;
	final public int name_and_type_index;

	public ConstantFieldref(byte tag,
			                int class_index, 
			                int name_and_type_index)
	{
		super(tag);
		this.class_index = class_index;
		this.name_and_type_index = name_and_type_index;
	}
}
