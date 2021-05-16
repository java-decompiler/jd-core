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
package jd.core.process.writer;

import jd.core.loader.Loader;
import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.attribute.ElementValuePrimitiveType;
import jd.core.model.classfile.constant.ConstantValue;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.Printer;
import jd.core.util.StringUtil;

public class ElementValuePrimitiveTypeWriter 
{
	public static void Write(
		Loader loader, Printer printer, ReferenceMap referenceMap,
		ClassFile classFile, ElementValuePrimitiveType evpt)
	{
		ConstantPool constants = classFile.getConstantPool();
			
		if (evpt.type == 's')
		{
			String constValue = 
				constants.getConstantUtf8(evpt.const_value_index);
			String escapedString =
				StringUtil.EscapeStringAndAppendQuotationMark(constValue);
			printer.printString(escapedString, classFile.getThisClassName());
		}
		else
		{
			ConstantValue cv = constants.getConstantValue(
				evpt.const_value_index);
	    	ConstantValueWriter.Write(
	    		loader, printer, referenceMap, classFile, cv, evpt.type);
		}
	}
}
