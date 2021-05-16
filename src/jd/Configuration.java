package jd;

import java.io.File;

public class Configuration
{
	private String pathToClass = null;
	
	public Configuration(String[] args)
	{
		if (args.length > 0)
		{
			this.pathToClass = args[0].replace('/', File.separatorChar)
                                      .replace('\\', File.separatorChar);
		}
	}
	
	public boolean isValid()
	{
		return (this.pathToClass != null);
	}
	
	public String getPathToClass()
	{
		return this.pathToClass;
	}
}
