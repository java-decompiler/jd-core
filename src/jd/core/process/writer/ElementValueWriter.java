package jd.core.process.writer;

import jd.core.loader.Loader;
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

public class ElementValueWriter 
{
	public static void WriteElementValue(
		Loader loader, Printer printer, ReferenceMap referenceMap, 
		ClassFile classFile, ElementValue ev)
	{
		ConstantPool constants = classFile.getConstantPool();
		
		switch (ev.tag)
		{
		case ElementValueContants.EV_PRIMITIVE_TYPE:
			ElementValuePrimitiveType evpt = (ElementValuePrimitiveType)ev;
			ElementValuePrimitiveTypeWriter.Write(
				loader, printer, referenceMap, classFile, evpt);
			break;
			
		case ElementValueContants.EV_CLASS_INFO:
			ElementValueClassInfo evci = (ElementValueClassInfo)ev;
			String signature = 
				constants.getConstantUtf8(evci.class_info_index);
			SignatureWriter.WriteSignature(
				loader, printer, referenceMap, classFile, signature);
			printer.print('.');
			printer.printKeyword("class");
			break;
			
		case ElementValueContants.EV_ANNOTATION_VALUE:
			ElementValueAnnotationValue evav = (ElementValueAnnotationValue)ev;
			AnnotationWriter.WriteAnnotation(
				loader, printer, referenceMap, 
				classFile, evav.annotation_value);
			break;
			
		case ElementValueContants.EV_ARRAY_VALUE:
			ElementValueArrayValue evarv = (ElementValueArrayValue)ev;
			ElementValue[] values = evarv.values;
			printer.print('{');
	
			if ((values != null) && (values.length > 0))
			{
				WriteElementValue(
					loader, printer, referenceMap, classFile, values[0]);
				for (int i=1; i<values.length; i++)
				{
					printer.print(", ");
					WriteElementValue(
						loader, printer, referenceMap, classFile, values[i]);
				}
			}
			printer.print('}');
			break;
			
		case ElementValueContants.EV_ENUM_CONST_VALUE:
			ElementValueEnumConstValue evecv = (ElementValueEnumConstValue)ev;
			signature = constants.getConstantUtf8(evecv.type_name_index);
			String constName = constants.getConstantUtf8(evecv.const_name_index);
			String internalClassName = SignatureUtil.GetInternalName(signature);
			
			SignatureWriter.WriteSignature(
				loader, printer, referenceMap, classFile, signature);
			
			printer.print('.');
			printer.printStaticField(
				internalClassName, constName, 
				signature, classFile.getThisClassName());
		}
	}
}
