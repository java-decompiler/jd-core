package jd.loader;

import java.io.DataInputStream;

public interface Loader 
{
	public DataInputStream load(String internalPath) throws LoaderException;
	
	boolean canLoad(String internalPath);
}
