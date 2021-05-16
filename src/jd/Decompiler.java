package jd;

import jd.classfile.ClassFile;
import jd.classfile.analyzer.ClassFileAnalyzer;
import jd.classfile.analyzer.ReferenceAnalyzer;
import jd.classfile.writer.ClassFileWriter;
import jd.deserializer.ClassFileDeserializer;
import jd.loader.Loader;
import jd.loader.LoaderException;
import jd.printer.Printer;
import jd.util.ReferenceMap;

public class Decompiler 
{
	public static void Decompile(
			Preferences preferences, Loader loader, 
			Printer printer, String internalPath) 
		throws LoaderException 
	{
long time0 = System.currentTimeMillis();
		// 1) Deserialisation
		ClassFile classFile = 
			ClassFileDeserializer.Deserialize(loader, internalPath);

		if (classFile == null)
		{
			System.err.println(
					"Can not deserialize '" + internalPath + "'.");
		}
		else
		{
			// 2) Analyse byte code
			ReferenceMap referenceMap = new ReferenceMap();			
			ClassFileAnalyzer.Analyze(referenceMap, classFile);
			
			// 3) Creation de la liste des references pour generer la liste des 
		    //    "import"
			ReferenceAnalyzer.Analyze(referenceMap, classFile);		
			
			// 4) Write source code
			ClassFileWriter.Write(
				preferences, printer, referenceMap, classFile);
		}
long time1 = System.currentTimeMillis();
System.out.println("time = " + (time1-time0) + " ms");
	}
}
