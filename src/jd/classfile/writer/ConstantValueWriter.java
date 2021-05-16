package jd.classfile.writer;

import jd.Constants;
import jd.classfile.ConstantPool;
import jd.classfile.constant.ConstantDouble;
import jd.classfile.constant.ConstantFloat;
import jd.classfile.constant.ConstantInteger;
import jd.classfile.constant.ConstantLong;
import jd.classfile.constant.ConstantString;
import jd.classfile.constant.ConstantValue;
import jd.printer.Printer;
import jd.util.StringUtil;

public class ConstantValueWriter 
{	
	public static void Write(
		Printer spw, int lineNumber, ConstantPool constants, ConstantValue cv)
	{
		Write(spw, lineNumber, constants, cv, (byte)0);
	}
	
	public static void Write(
		Printer spw, int lineNumber, ConstantPool constants, 
		ConstantValue cv, byte constantIntegerType)
	{
		switch (cv.tag)
		{
		case Constants.CONSTANT_Double:
			double d = ((ConstantDouble)cv).bytes;
			
			if (d == Double.POSITIVE_INFINITY)
			{
				spw.print(lineNumber, "(1.0D / 0.0D)");
			}
			else if (d == Double.NEGATIVE_INFINITY)
			{
				spw.print(lineNumber, "(-1.0D / 0.0D)");				
			}
			else if (d == Double.NaN)
			{
				spw.print(lineNumber, "(0.0D / 0.0D)");								
			}
			else
			{
				spw.print(lineNumber, String.valueOf(d));
				spw.print(lineNumber, 'D');
			}
			break;
		case Constants.CONSTANT_Float:
			float f = ((ConstantFloat)cv).bytes;
			
			if (f == Float.POSITIVE_INFINITY)
			{
				spw.print(lineNumber, "(1.0F / 0.0F)");
			}
			else if (f == Float.NEGATIVE_INFINITY)
			{
				spw.print(lineNumber, "(-1.0F / 0.0F)");				
			}
			else if (f == Float.NaN)
			{
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "(0.0F / 0.0F)");								
			}
			else
			{
				spw.print(lineNumber, String.valueOf(f));
				spw.print(lineNumber, 'F');
			}
			break;
		case Constants.CONSTANT_Integer:
			switch (constantIntegerType)
			{
			case 'Z':
				spw.printKeyword(
					lineNumber, 
					(((ConstantInteger)cv).bytes == 0) ? "false" : "true");
				break;
			case 'C':
				String escapedString = StringUtil.EscapeCharAndAppendApostrophe(
					(char)((ConstantInteger)cv).bytes);
				spw.print(lineNumber, escapedString);
				break;
		    default:
		    	spw.print(
		    		lineNumber, String.valueOf(((ConstantInteger)cv).bytes));
			}
			break;
		case Constants.CONSTANT_Long:
			spw.print(lineNumber, String.valueOf(((ConstantLong)cv).bytes));
			spw.print(lineNumber, 'L');
			break;
		case Constants.CONSTANT_String:
			String s = constants.getConstantUtf8(
				((ConstantString)cv).string_index);
			String escapedString =
				StringUtil.EscapeStringAndAppendQuotationMark(s);
			spw.printString(lineNumber, escapedString);
			break;
		}
	}

	public static void WriteHexa(
		Printer spw, int lineNumber, ConstantPool constants, ConstantValue cv)
	{
		switch (cv.tag)
		{
		case Constants.CONSTANT_Integer:
			spw.print(lineNumber, "0x");
			spw.print(lineNumber, 
				Integer.toHexString( ((ConstantInteger)cv).bytes ).toUpperCase());
			break;
		case Constants.CONSTANT_Long:
			spw.print(lineNumber, "0x");
			spw.print(lineNumber, 
				Long.toHexString( ((ConstantLong)cv).bytes ).toUpperCase());
			break;
		default:
			Write(spw, lineNumber, constants, cv, (byte)0);
		}
	}
}