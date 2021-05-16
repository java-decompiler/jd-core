package jd.core.loader;

import java.io.DataInputStream;



public interface Loader 
{
	public DataInputStream load(String internalPath) throws LoaderException;
	
	public boolean canLoad(String internalPath);
}
