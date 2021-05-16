package jd.classfile.writer;

import jd.classfile.ConstantPool;
import jd.classfile.attribute.ElementValuePrimitiveType;
import jd.classfile.constant.ConstantValue;
import jd.printer.Printer;
import jd.util.StringUtil;

public class ElementValuePrimitiveTypeWriter 
{
	public static void Write(
			Printer spw, int lineNumber, ConstantPool constants, 
			ElementValuePrimitiveType evpt)
	{
		if (evpt.type == 's')
		{
			String constValue = 
				constants.getConstantUtf8(evpt.const_value_index);
			String escapedString =
				StringUtil.EscapeStringAndAppendQuotationMark(constValue);
			spw.print(lineNumber, escapedString);
		}
		else
		{
			ConstantValue cv = constants.getConstantValue(
				evpt.const_value_index);
	    	ConstantValueWriter.Write(
	    		spw, lineNumber, constants, cv, evpt.type);
		}
	}
}
