package jd.core;

import jd.core.loader.Loader;
import jd.core.loader.LoaderException;
import jd.core.preferences.Preferences;
import jd.core.printer.Printer;



public interface Decompiler 
{
	public void decompile(
			Preferences preferences, Loader loader, 
			Printer printer, String internalClassPath) 
		throws LoaderException;
}
