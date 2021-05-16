package jd.core.model.classfile.constant;


public class ConstantInterfaceMethodref extends ConstantMethodref 
{
	public ConstantInterfaceMethodref(byte tag,
			                          int class_index,
			                          int name_and_type_index)
	{
		super(tag, class_index, name_and_type_index);
	}    
}
