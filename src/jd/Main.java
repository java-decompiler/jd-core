package jd;

import java.io.PrintWriter;

import jd.loader.DirectoryLoader;
import jd.loader.LoaderException;
import jd.printer.Printer;
import jd.printer.SimpleFlushPrinter;
import jd.util.ClassFileUtil;



public class Main 
{
	private static Configuration configuration;

	/**
	 * @param args	Path to java class
	 */
	public static void main(String[] args) 
	{
		configuration = new Configuration(args);
		if (! configuration.isValid())
		{
			System.out.println("usage: ...");
			return;
		}
		
		String pathToClass = configuration.getPathToClass();
		if (pathToClass == null)
			return;
		
		String directoryPath = ClassFileUtil.ExtractDirectoryPath(pathToClass);
		if (directoryPath == null)
			return;
		
		String internalPath = 
			ClassFileUtil.ExtractInternalPath(directoryPath, pathToClass);
		if (internalPath == null)
			return;
		
		Preferences preferences = new Preferences();
		DirectoryLoader loader = 
			new DirectoryLoader(directoryPath);
		Printer printer = new SimpleFlushPrinter(new PrintWriter(System.out));
		//Printer printer = new SimplePrinter(new PrintWriter(System.out));
		//Printer printer = new LineNumberPrinter(new PrintWriter(System.out));
		
		try 
		{
			Decompiler.Decompile(preferences, loader, printer, internalPath);
		} 
		catch (LoaderException e) 
		{
			e.printStackTrace();
		}
		
		printer.flush();
		printer.close();
	}
}
