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

import org.apache.bcel.classfile.Constant;
import org.jd.core.v1.api.loader.Loader;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.attribute.ElementValuePrimitiveType;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.Printer;
import jd.core.util.StringUtil;

public final class ElementValuePrimitiveTypeWriter
{
    private ElementValuePrimitiveTypeWriter() {
        super();
    }

    public static void write(
        Loader loader, Printer printer, ReferenceMap referenceMap,
        ClassFile classFile, ElementValuePrimitiveType evpt)
    {
        ConstantPool constants = classFile.getConstantPool();

        if (evpt.type() == 's')
        {
            String constValue =
                constants.getConstantUtf8(evpt.constValueIndex());
            String escapedString =
                StringUtil.escapeStringAndAppendQuotationMark(constValue);
            printer.printString(escapedString, classFile.getThisClassName());
        }
        else
        {
            Constant cv = constants.getConstantValue(
                evpt.constValueIndex());
            ConstantValueWriter.write(
                loader, printer, referenceMap, classFile, cv, evpt.type());
        }
    }
}
