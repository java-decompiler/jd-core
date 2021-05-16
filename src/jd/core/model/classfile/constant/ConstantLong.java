package jd.core.model.classfile.constant;


public class ConstantLong extends ConstantValue 
{
	final public long bytes;
	
	public ConstantLong(byte tag, long bytes)
	{
		super(tag);
		this.bytes = bytes;
	}    
}
