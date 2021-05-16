package jd.core.model.classfile.constant;


public class ConstantFloat extends ConstantValue 
{
	final public float bytes;
	
	public ConstantFloat(byte tag, float bytes)
	{
		super(tag);
		this.bytes = bytes;
	}    
}
