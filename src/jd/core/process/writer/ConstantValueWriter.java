package jd.core.process.writer;

import jd.core.loader.Loader;
import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.model.classfile.constant.ConstantDouble;
import jd.core.model.classfile.constant.ConstantFloat;
import jd.core.model.classfile.constant.ConstantInteger;
import jd.core.model.classfile.constant.ConstantLong;
import jd.core.model.classfile.constant.ConstantString;
import jd.core.model.classfile.constant.ConstantValue;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.Printer;
import jd.core.util.StringUtil;

public class ConstantValueWriter 
{	
	public static void Write(
		Loader loader, Printer printer, ReferenceMap referenceMap,
		ClassFile classFile, ConstantValue cv)
	{
		Write(loader, printer, referenceMap, classFile, cv, (byte)0);
	}
	
	public static void Write(
		Loader loader, Printer printer, ReferenceMap referenceMap,
		ClassFile classFile, ConstantValue cv, byte constantIntegerType)
	{
		ConstantPool constants = classFile.getConstantPool();

		switch (cv.tag)
		{
		case ConstantConstant.CONSTANT_Double:
			{
				double d = ((ConstantDouble)cv).bytes;
				
				if (d == Double.POSITIVE_INFINITY)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Double", "POSITIVE_INFINITY", "D");
				}
				else if (d == Double.NEGATIVE_INFINITY)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Double", "NEGATIVE_INFINITY", "D");
				}
				else if (d == Double.NaN)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Double", "NaN", "D");
				}
				else if (d == Double.MAX_VALUE)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Double", "MAX_VALUE", "D");
				}
				/* else if (d == Double.MIN_NORMAL)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Double", "MIN_NORMAL", "D");
				} */
				else if (d == Double.MIN_VALUE)
				{
					Write(
						loader, printer, referenceMap, classFile,
						"java/lang/Double", "MIN_VALUE", "D");
				}
				else
				{
					// TODO Conversion de la valeur en constante ?
					String value = String.valueOf(d);
					if (value.indexOf('.') == -1)
						value += ".0";
					printer.printNumeric(value + 'D');
				}
			}
			break;
		case ConstantConstant.CONSTANT_Float:
			{
				float value = ((ConstantFloat)cv).bytes;
				
				if (value == Float.POSITIVE_INFINITY)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Float", "POSITIVE_INFINITY", "F");
				}
				else if (value == Float.NEGATIVE_INFINITY)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Float", "NEGATIVE_INFINITY", "F");
				}
				else if (value == Float.NaN)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Float", "NaN", "F");
				}
				else if (value == Float.MAX_VALUE)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Float", "MAX_VALUE", "F");
				}
				/* else if (value == Float.MIN_NORMAL)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Float", "MIN_NORMAL", "F");
				} */
				else if (value == Float.MIN_VALUE)
				{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Float", "MIN_VALUE", "F");
				}
				else
				{
					// TODO Conversion de la valeur en constante ?
					String s = String.valueOf(value);
					if (s.indexOf('.') == -1)
						s += ".0";
					printer.printNumeric(s + 'F');
				}
			}
			break;
		case ConstantConstant.CONSTANT_Integer:
			{
		    	int value = ((ConstantInteger)cv).bytes;
		    	
				switch (constantIntegerType)
				{
				case 'Z':
					{
						printer.printKeyword((value == 0) ? "false" : "true");
					}
					break;
				case 'C':
					{
						String escapedString = StringUtil.EscapeCharAndAppendApostrophe((char)value);
						String scopeInternalName = classFile.getThisClassName();
						printer.printString(escapedString, scopeInternalName);
					}
					break;
			    default:
				    {
				    	if (value == Integer.MIN_VALUE)
				    	{
							Write(
								loader, printer, referenceMap, classFile, 
								"java/lang/Integer", "MIN_VALUE", "I");
				    	}
				    	else if (value == Integer.MAX_VALUE)
				    	{
							Write(
								loader, printer, referenceMap, classFile, 
								"java/lang/Integer", "MAX_VALUE", "I");
				    	}
				    	else
				    	{
					    	printer.printNumeric(String.valueOf(value));
				    	}
				    }
				}
			}
			break;
		case ConstantConstant.CONSTANT_Long:
			{
				long value = ((ConstantLong)cv).bytes;
				
		    	if (value == Long.MIN_VALUE)
		    	{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Long", "MIN_VALUE", "J");
		    	}
		    	else if (value == Long.MAX_VALUE)
		    	{
					Write(
						loader, printer, referenceMap, classFile, 
						"java/lang/Long", "MAX_VALUE", "J");
		    	}
		    	else
		    	{
		    		printer.printNumeric(String.valueOf(value) + 'L');
		    	}
			}
			break;
		case ConstantConstant.CONSTANT_String:
			{
				String s = constants.getConstantUtf8(
					((ConstantString)cv).string_index);
				String escapedString =
					StringUtil.EscapeStringAndAppendQuotationMark(s);
				String scopeInternalName = classFile.getThisClassName();
				printer.printString(escapedString, scopeInternalName);
			}
			break;
		}
	}

	private static void Write(
		Loader loader, Printer printer, ReferenceMap referenceMap,
		ClassFile classFile, String internalTypeName, 
		String name, String descriptor)
	{
		String className = SignatureWriter.InternalClassNameToClassName(
			loader, referenceMap, classFile, internalTypeName);
		String scopeInternalName = classFile.getThisClassName();
		printer.printType(internalTypeName, className, scopeInternalName);
		printer.print('.');
		printer.printStaticField(internalTypeName, name, descriptor, scopeInternalName);		
	}
	
	public static void WriteHexa(
		Loader loader, Printer printer, ReferenceMap referenceMap,
		ClassFile classFile, ConstantValue cv)
	{
		switch (cv.tag)
		{
		case ConstantConstant.CONSTANT_Integer:
			printer.printNumeric(
				"0x" + Integer.toHexString( ((ConstantInteger)cv).bytes ).toUpperCase());
			break;
		case ConstantConstant.CONSTANT_Long:
			printer.printNumeric(
				"0x" + Long.toHexString( ((ConstantLong)cv).bytes ).toUpperCase());
			break;
		default:
			Write(loader, printer, referenceMap, classFile, cv, (byte)0);
		}
	}
}