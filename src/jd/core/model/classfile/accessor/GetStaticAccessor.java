package jd.core.model.classfile.accessor;


public class GetStaticAccessor extends Accessor 
{
	final public String className;
	final public String fieldName;
	final public String fieldDescriptor;
	
	public GetStaticAccessor(
		byte tag, String className, String fieldName, String fieldDescriptor)
	{
		super(tag);
		this.className = className;
		this.fieldName = fieldName;
		this.fieldDescriptor = fieldDescriptor;
	}
}
