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
