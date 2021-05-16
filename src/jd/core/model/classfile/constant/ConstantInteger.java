package jd.core.model.classfile.constant;


public class ConstantInteger extends ConstantValue 
{
	final public int bytes;
	
	public ConstantInteger(byte tag, int bytes)
	{
		super(tag);
		this.bytes = bytes;
	}    
}
