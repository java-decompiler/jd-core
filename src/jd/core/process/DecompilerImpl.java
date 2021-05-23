/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.process;

import java.util.ArrayList;

import jd.core.Decompiler;
import jd.core.loader.Loader;
import jd.core.loader.LoaderException;
import jd.core.model.classfile.ClassFile;
import jd.core.model.layout.block.LayoutBlock;
import jd.core.model.reference.ReferenceMap;
import jd.core.preferences.Preferences;
import jd.core.printer.Printer;
import jd.core.process.analyzer.classfile.ClassFileAnalyzer;
import jd.core.process.analyzer.classfile.ReferenceAnalyzer;
import jd.core.process.deserializer.ClassFileDeserializer;
import jd.core.process.layouter.ClassFileLayouter;
import jd.core.process.writer.ClassFileWriter;

public class DecompilerImpl implements Decompiler
{
	public void decompile(
			Preferences preferences, Loader loader, 
			Printer printer, String internalClassPath) 
		throws LoaderException 
	{
//long time0 = System.currentTimeMillis();

		// 1) Deserialisation
		ClassFile classFile = 
			ClassFileDeserializer.Deserialize(loader, internalClassPath);
		if (classFile == null)
			throw new LoaderException(
				"Can not deserialize '" + internalClassPath + "'.");

		// 2) Analyse du byte code
		ReferenceMap referenceMap = new ReferenceMap();			
		ClassFileAnalyzer.Analyze(referenceMap, classFile);
		
		// 3) Creation de la liste des references pour generer la liste des 
	    //    "import"
		ReferenceAnalyzer.Analyze(referenceMap, classFile);
		
		// 4) Mise en page du code source
		ArrayList<LayoutBlock> layoutBlockList = new ArrayList<LayoutBlock>(1024);
		int maxLineNumber =	ClassFileLayouter.Layout(
				preferences, referenceMap, classFile, layoutBlockList);
		
//System.out.println("layoutBlockList.size = " + layoutBlockList.size());
		
		// 5) Ecriture du code source 
		ClassFileWriter.Write(
			loader, printer, referenceMap, maxLineNumber, 
			classFile.getMajorVersion(), classFile.getMinorVersion(), 
			layoutBlockList);
		
//long time1 = System.currentTimeMillis();
//System.out.println("time = " + (time1-time0) + " ms");
	}
}
