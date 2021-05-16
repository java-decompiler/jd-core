package jd.core.process.analyzer.instruction.fast;


public class UnexpectedElementException extends RuntimeException
{
	private static final long serialVersionUID = -3407799517256621265L;

	public UnexpectedElementException() 
	{ 
		super(); 
	}
	
	public UnexpectedElementException(String s) 
	{ 
		super(s); 
	}
}
