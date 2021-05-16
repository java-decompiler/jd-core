package jd.core.model.classfile.constant;


public class ConstantDouble extends ConstantValue 
{
	final public double bytes;
	
	public ConstantDouble(byte tag, double bytes)
	{
		super(tag);
		this.bytes = bytes;
	}    
}
