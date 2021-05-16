package jd.core.model.classfile.constant;


public class ConstantUtf8 extends Constant 
{
	public String bytes;

	public ConstantUtf8(byte tag, String bytes)
	{
		super(tag);
		this.bytes = bytes;
	}    
}
