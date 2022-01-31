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

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantString;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.util.StringConstants;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.Printer;
import jd.core.util.StringUtil;

public final class ConstantValueWriter
{
    private ConstantValueWriter() {
        super();
    }

    public static void write(
        Loader loader, Printer printer, ReferenceMap referenceMap,
        ClassFile classFile, Constant cv)
    {
        write(loader, printer, referenceMap, classFile, cv, (byte)0);
    }

    public static void write(
        Loader loader, Printer printer, ReferenceMap referenceMap,
        ClassFile classFile, Constant cv, byte constantIntegerType)
    {
        ConstantPool constants = classFile.getConstantPool();

        switch (cv.getTag())
        {
          case Const.CONSTANT_Double:
            {
                double d = ((ConstantDouble)cv).getBytes();

                if (Double.compare(d, Double.POSITIVE_INFINITY) == 0)
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_DOUBLE, "POSITIVE_INFINITY", "D");
                }
                else if (Double.compare(d, Double.NEGATIVE_INFINITY) == 0)
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_DOUBLE, "NEGATIVE_INFINITY", "D");
                }
                else if (Double.isNaN(d))
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_DOUBLE, "NaN", "D");
                }
                else if (Double.compare(d, Double.MAX_VALUE) == 0)
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_DOUBLE, StringConstants.MAX_VALUE, "D");
                }
                /* else if (Double.compare(d, Double.MIN_NORMAL) == 0)
                {
                    Write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_DOUBLE, "MIN_NORMAL", "D");
                } */
                else if (Double.compare(d, Double.MIN_VALUE) == 0)
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_DOUBLE, StringConstants.MIN_VALUE, "D");
                }
                else
                {
                    // TODO Conversion de la valeur en constante ?
                    String value = String.valueOf(d);
                    if (value.indexOf('.') == -1) {
                        value += ".0";
                    }
                    printer.printNumeric(value + 'D');
                }
            }
            break;
          case Const.CONSTANT_Float:
            {
                float value = ((ConstantFloat)cv).getBytes();

                if (Float.compare(value, Float.POSITIVE_INFINITY) == 0)
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_FLOAT, "POSITIVE_INFINITY", "F");
                }
                else if (Float.compare(value, Float.NEGATIVE_INFINITY) == 0)
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_FLOAT, "NEGATIVE_INFINITY", "F");
                }
                else if (Float.isNaN(value))
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_FLOAT, "NaN", "F");
                }
                else if (Float.compare(value, Float.MAX_VALUE) == 0)
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_FLOAT, StringConstants.MAX_VALUE, "F");
                }
                /* else if (Float.compare(value, Float.MIN_NORMAL) == 0)
                {
                    Write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_FLOAT, "MIN_NORMAL", "F");
                } */
                else if (Float.compare(value, Float.MIN_VALUE) == 0)
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_FLOAT, StringConstants.MIN_VALUE, "F");
                }
                else
                {
                    // TODO Conversion de la valeur en constante ?
                    String s = String.valueOf(value);
                    if (s.indexOf('.') == -1) {
                        s += ".0";
                    }
                    printer.printNumeric(s + 'F');
                }
            }
            break;
          case Const.CONSTANT_Integer:
            {
                int value = ((ConstantInteger)cv).getBytes();

                switch (constantIntegerType)
                {
                case 'Z':
                    {
                        printer.printKeyword(value == 0 ? "false" : "true");
                    }
                    break;
                case 'C':
                    {
                        String escapedString = StringUtil.escapeCharAndAppendApostrophe((char)value);
                        String scopeInternalName = classFile.getThisClassName();
                        printer.printString(escapedString, scopeInternalName);
                    }
                    break;
                default:
                    {
                        if (value == Integer.MIN_VALUE)
                        {
                            write(
                                loader, printer, referenceMap, classFile,
                                StringConstants.JAVA_LANG_INTEGER, StringConstants.MIN_VALUE, "I");
                        }
                        else if (value == Integer.MAX_VALUE)
                        {
                            write(
                                loader, printer, referenceMap, classFile,
                                StringConstants.JAVA_LANG_INTEGER, StringConstants.MAX_VALUE, "I");
                        }
                        else
                        {
                            printer.printNumeric(String.valueOf(value));
                        }
                    }
                }
            }
            break;
          case Const.CONSTANT_Long:
            {
                long value = ((ConstantLong)cv).getBytes();

                if (value == Long.MIN_VALUE)
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_LONG, StringConstants.MIN_VALUE, "J");
                }
                else if (value == Long.MAX_VALUE)
                {
                    write(
                        loader, printer, referenceMap, classFile,
                        StringConstants.JAVA_LANG_LONG, StringConstants.MAX_VALUE, "J");
                }
                else
                {
                    printer.printNumeric(String.valueOf(value) + 'L');
                }
            }
            break;
          case Const.CONSTANT_String:
            {
                String s = constants.getConstantUtf8(
                    ((ConstantString)cv).getStringIndex());
                String escapedString =
                    StringUtil.escapeStringAndAppendQuotationMark(s);
                String scopeInternalName = classFile.getThisClassName();
                printer.printString(escapedString, scopeInternalName);
            }
            break;
        }
    }

    private static void write(
        Loader loader, Printer printer, ReferenceMap referenceMap,
        ClassFile classFile, String internalTypeName,
        String name, String descriptor)
    {
        String className = SignatureWriter.internalClassNameToClassName(
            loader, referenceMap, classFile, internalTypeName);
        String scopeInternalName = classFile.getThisClassName();
        printer.printType(internalTypeName, className, scopeInternalName);
        printer.print('.');
        printer.printStaticField(internalTypeName, name, descriptor, scopeInternalName);
    }

    public static void writeHexa(
        Loader loader, Printer printer, ReferenceMap referenceMap,
        ClassFile classFile, Constant cv)
    {
        switch (cv.getTag())
        {
        case Const.CONSTANT_Integer:
            printer.printNumeric(
                "0x" + Integer.toHexString( ((ConstantInteger)cv).getBytes() ).toUpperCase());
            break;
        case Const.CONSTANT_Long:
            printer.printNumeric(
                "0x" + Long.toHexString( ((ConstantLong)cv).getBytes() ).toUpperCase());
            break;
        default:
            write(loader, printer, referenceMap, classFile, cv, (byte)0);
        }
    }
}
