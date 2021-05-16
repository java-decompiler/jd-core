package jd.classfile.constant;

public abstract class Constant 
{
	final public byte tag;
	
	protected Constant(byte tag) 
	{ 
		this.tag = tag; 
	}
}
