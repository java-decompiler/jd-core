package jd.core.util;


public class InvalidParameterException extends RuntimeException
{
	private static final long serialVersionUID = -3407799517256621265L;

	public InvalidParameterException() 
	{ 
		super(); 
	}
	
	public InvalidParameterException(String s) 
	{ 
		super(s); 
	}
}
