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

import org.jd.core.v1.api.loader.Loader;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.attribute.ElementValue;
import jd.core.model.classfile.attribute.ElementValueAnnotationValue;
import jd.core.model.classfile.attribute.ElementValueArrayValue;
import jd.core.model.classfile.attribute.ElementValueClassInfo;
import jd.core.model.classfile.attribute.ElementValueContants;
import jd.core.model.classfile.attribute.ElementValueEnumConstValue;
import jd.core.model.classfile.attribute.ElementValuePrimitiveType;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.Printer;
import jd.core.util.SignatureUtil;

public final class ElementValueWriter
{
    private ElementValueWriter() {
        super();
    }

    public static void writeElementValue(
        Loader loader, Printer printer, ReferenceMap referenceMap,
        ClassFile classFile, ElementValue ev)
    {
        ConstantPool constants = classFile.getConstantPool();

        switch (ev.tag())
        {
        case ElementValueContants.EV_PRIMITIVE_TYPE:
            ElementValuePrimitiveType evpt = (ElementValuePrimitiveType)ev;
            ElementValuePrimitiveTypeWriter.write(
                loader, printer, referenceMap, classFile, evpt);
            break;

        case ElementValueContants.EV_CLASS_INFO:
            ElementValueClassInfo evci = (ElementValueClassInfo)ev;
            String signature =
                constants.getConstantUtf8(evci.classInfoIndex());
            SignatureWriter.writeSignature(
                loader, printer, referenceMap, classFile, signature);
            printer.print('.');
            printer.printKeyword("class");
            break;

        case ElementValueContants.EV_ANNOTATION_VALUE:
            ElementValueAnnotationValue evav = (ElementValueAnnotationValue)ev;
            AnnotationWriter.writeAnnotation(
                loader, printer, referenceMap,
                classFile, evav.annotationValue());
            break;

        case ElementValueContants.EV_ARRAY_VALUE:
            ElementValueArrayValue evarv = (ElementValueArrayValue)ev;
            ElementValue[] values = evarv.values();
            printer.print('{');

            if (values != null && values.length > 0)
            {
                writeElementValue(
                    loader, printer, referenceMap, classFile, values[0]);
                for (int i=1; i<values.length; i++)
                {
                    printer.print(", ");
                    writeElementValue(
                        loader, printer, referenceMap, classFile, values[i]);
                }
            }
            printer.print('}');
            break;

        case ElementValueContants.EV_ENUM_CONST_VALUE:
            ElementValueEnumConstValue evecv = (ElementValueEnumConstValue)ev;
            signature = constants.getConstantUtf8(evecv.typeNameIndex());
            String constName = constants.getConstantUtf8(evecv.constNameIndex());
            String internalClassName = SignatureUtil.getInternalName(signature);

            SignatureWriter.writeSignature(
                loader, printer, referenceMap, classFile, signature);

            printer.print('.');
            printer.printStaticField(
                internalClassName, constName,
                signature, classFile.getThisClassName());
        }
    }
}
