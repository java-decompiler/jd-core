package jd.core.model.classfile;

import jd.core.model.classfile.attribute.Attribute;


public class FieldOrMethod extends Base
{
	public int name_index;
	final public int descriptor_index;

	public FieldOrMethod(int access_flags, int name_index, 
			             int descriptor_index, Attribute[] attributes)
	{
		super(access_flags, attributes);
		
		this.name_index = name_index;
		this.descriptor_index = descriptor_index;
	}

	public Attribute[] getAttributes()
	{
		return this.attributes;
	}
}
