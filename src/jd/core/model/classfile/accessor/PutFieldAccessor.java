package jd.core.model.classfile.accessor;


public class PutFieldAccessor extends Accessor 
{
	final public String className;
	final public String fieldName;
	final public String fieldDescriptor;
	
	public PutFieldAccessor(
		byte tag, String className, String fieldName, String fieldDescriptor)
	{
		super(tag);
		this.className = className;
		this.fieldName = fieldName;
		this.fieldDescriptor = fieldDescriptor;
	}
}
