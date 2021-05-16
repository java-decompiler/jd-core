package jd.loader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import jd.Constants;


public class DirectoryLoader implements Loader
{
	private String base;
	
	public DirectoryLoader(String base)
	{
		this.base = base;
	}
	
	public DataInputStream load(String internalPath)
		throws LoaderException
	{
		String path = 
			this.base + 
			internalPath.replace(
				Constants.INTERNAL_PACKAGE_SEPARATOR, File.separatorChar);
		
		try
		{
			return new DataInputStream(
						new BufferedInputStream(new FileInputStream(path)));
		}
		catch (FileNotFoundException e)
		{
			throw new LoaderException("'" + path + "'  not found.");
		}
	}

	public boolean canLoad(String internalPath) 
	{
		String path = 
			this.base + 
			internalPath.replace(
				Constants.INTERNAL_PACKAGE_SEPARATOR, File.separatorChar);
		
		File file = new File(path);

		return file.exists() && file.isFile();
	}
}
