package jd.core.model.classfile.constant;

public abstract class Constant 
{
	final public byte tag;
	
	protected Constant(byte tag) 
	{ 
		this.tag = tag; 
	}
}
