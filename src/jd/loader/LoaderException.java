package jd.loader;

public class LoaderException extends Exception 
{
	private static final long serialVersionUID = 9506606333927794L;

	public LoaderException() {}
	
	LoaderException(String msg) 
	{
		super(msg);
	}
}
