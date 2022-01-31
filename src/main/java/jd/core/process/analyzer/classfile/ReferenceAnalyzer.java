/**
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
 */
package jd.core.process.analyzer.classfile;

import org.apache.bcel.Const;
import org.jd.core.v1.model.classfile.attribute.CodeException;
import org.jd.core.v1.util.StringConstants;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.Annotation;
import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeRuntimeAnnotations;
import jd.core.model.classfile.attribute.AttributeRuntimeParameterAnnotations;
import jd.core.model.classfile.attribute.AttributeSignature;
import jd.core.model.classfile.attribute.ElementValue;
import jd.core.model.classfile.attribute.ElementValueAnnotationValue;
import jd.core.model.classfile.attribute.ElementValueArrayValue;
import jd.core.model.classfile.attribute.ElementValueClassInfo;
import jd.core.model.classfile.attribute.ElementValueContants;
import jd.core.model.classfile.attribute.ElementValueEnumConstValue;
import jd.core.model.classfile.attribute.ElementValuePair;
import jd.core.model.classfile.attribute.ParameterAnnotations;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.reference.Reference;
import jd.core.model.reference.ReferenceMap;
import jd.core.process.analyzer.classfile.visitor.ReferenceVisitor;

public final class ReferenceAnalyzer
{
    private ReferenceAnalyzer() {
    }
        public static void analyze(
        ReferenceMap referenceMap, ClassFile classFile)
    {
        countReferences(referenceMap, classFile);
        reduceReferences(referenceMap, classFile);
    }

    private static void countReferences(
            ReferenceMap referenceMap, ClassFile classFile)
    {
        // Class
        referenceMap.add(classFile.getThisClassName());

        AttributeSignature as = classFile.getAttributeSignature();
        if (as == null)
        {
            // Super class
            if (classFile.getSuperClassIndex() != 0) {
                referenceMap.add(classFile.getSuperClassName());
            }
            // Interfaces
            int[] interfaces = classFile.getInterfaces();
            if (interfaces != null)
            {
                String internalInterfaceName;
                for(int i=interfaces.length-1; i>=0; --i)
                {
                    internalInterfaceName = classFile.getConstantPool().getConstantClassName(interfaces[i]);
                    referenceMap.add(internalInterfaceName);
                }
            }
        }
        else
        {
            String signature =
                classFile.getConstantPool().getConstantUtf8(as.getSignatureIndex());
            SignatureAnalyzer.analyzeClassSignature(referenceMap, signature);
        }

        // Class annotations
        countReferencesInAttributes(
            referenceMap, classFile.getConstantPool(), classFile.getAttributes());

        // Inner classes
        List<ClassFile> innerClassFiles = classFile.getInnerClassFiles();
        if (innerClassFiles != null) {
            for (int i=innerClassFiles.size()-1; i>=0; --i) {
                countReferences(referenceMap, innerClassFiles.get(i));
            }
        }

        ReferenceVisitor visitor =
            new ReferenceVisitor(classFile.getConstantPool(), referenceMap);

        // Fields
        countReferencesInFields(referenceMap, visitor, classFile);

        // Methods
        countReferencesInMethods(referenceMap, visitor, classFile);
    }

    private static void countReferencesInAttributes(
            ReferenceMap referenceMap, ConstantPool constants,
            Attribute[] attributes)
    {
        if (attributes != null)
        {
            for (int i=attributes.length-1; i>=0; --i) {
                if (attributes[i].getTag() == Const.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS
                 || attributes[i].getTag() == Const.ATTR_RUNTIME_VISIBLE_ANNOTATIONS) {
                    Annotation[] annotations =
                        ((AttributeRuntimeAnnotations)attributes[i])
                        .getAnnotations();
                    for (int j=annotations.length-1; j>=0; --j) {
                        countAnnotationReference(referenceMap, constants, annotations[j]);
                    }
                } else if (attributes[i].getTag() == Const.ATTR_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS
                        || attributes[i].getTag() == Const.ATTR_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS) {
                    ParameterAnnotations[] parameterAnnotations =
                        ((AttributeRuntimeParameterAnnotations)
                                attributes[i]).getParameterAnnotations();
                    countParameterAnnotationsReference(
                            referenceMap, constants, parameterAnnotations);
                }
            }
        }
    }

    private static void countAnnotationReference(
            ReferenceMap referenceMap, ConstantPool constants,
            Annotation annotation)
    {
        String typeName = constants.getConstantUtf8(annotation.typeIndex());
        SignatureAnalyzer.analyzeSimpleSignature(referenceMap, typeName);

        ElementValuePair[] elementValuePairs =
            annotation.elementValuePairs();
        if (elementValuePairs != null)
        {
            for (int j=elementValuePairs.length-1; j>=0; --j) {
                countElementValue(
                    referenceMap, constants, elementValuePairs[j].elementValue());
            }
        }
    }

    private static void countParameterAnnotationsReference(
            ReferenceMap referenceMap, ConstantPool constants,
            ParameterAnnotations[] parameterAnnotations)
    {
        if (parameterAnnotations != null)
        {
            Annotation[] annotations;
            for (int i=parameterAnnotations.length-1; i>=0; --i)
            {
                annotations = parameterAnnotations[i].annotations();
                if (annotations != null)
                {
                    for (int j=annotations.length-1; j>=0; --j) {
                        countAnnotationReference(
                            referenceMap, constants, annotations[j]);
                    }
                }
            }
        }
    }

    private static void countElementValue(
            ReferenceMap referenceMap, ConstantPool constants, ElementValue ev)
    {
        String signature;
        ElementValueClassInfo evci;

        switch (ev.tag())
        {
        case ElementValueContants.EV_CLASS_INFO:
            {
                evci = (ElementValueClassInfo)ev;
                signature = constants.getConstantUtf8(evci.classInfoIndex());
                SignatureAnalyzer.analyzeSimpleSignature(referenceMap, signature);
            }
            break;
        case ElementValueContants.EV_ANNOTATION_VALUE:
            {
                ElementValueAnnotationValue evanv = (ElementValueAnnotationValue)ev;
                countAnnotationReference(
                        referenceMap, constants, evanv.annotationValue());
            }
            break;
        case ElementValueContants.EV_ARRAY_VALUE:
            {
                ElementValueArrayValue evarv = (ElementValueArrayValue)ev;
                ElementValue[] values = evarv.values();

                if (values != null)
                {
                    for (int i=values.length-1; i>=0; --i) {
                        if (values[i].tag() == ElementValueContants.EV_CLASS_INFO)
                        {
                            evci = (ElementValueClassInfo)values[i];
                            signature =
                                constants.getConstantUtf8(evci.classInfoIndex());
                            SignatureAnalyzer.analyzeSimpleSignature(referenceMap, signature);
                        }
                    }
                }
            }
            break;
        case ElementValueContants.EV_ENUM_CONST_VALUE:
            {
                ElementValueEnumConstValue evecv = (ElementValueEnumConstValue)ev;
                signature = constants.getConstantUtf8(evecv.typeNameIndex());
                SignatureAnalyzer.analyzeSimpleSignature(referenceMap, signature);
            }
            break;
        }
    }

    private static void countReferencesInFields(
            ReferenceMap referenceMap, ReferenceVisitor visitor,
            ClassFile classFile)
    {
        Field[] fields = classFile.getFields();

        if (fields == null) {
            return;
        }

        Field field;
        AttributeSignature as;
        String signature;
        for (int i=fields.length-1; i>=0; --i)
        {
            field = fields[i];

            if ((field.getAccessFlags() & Const.ACC_SYNTHETIC) != 0) {
                continue;
            }

            countReferencesInAttributes(
                    referenceMap, classFile.getConstantPool(), field.getAttributes());

            as = field.getAttributeSignature();
            signature = classFile.getConstantPool().getConstantUtf8(
                as==null ? field.getDescriptorIndex() : as.getSignatureIndex());
            SignatureAnalyzer.analyzeSimpleSignature(referenceMap, signature);

            if (field.getValueAndMethod() != null) {
                visitor.visit(field.getValueAndMethod().value());
            }
        }
    }

    private static void countReferencesInMethods(
            ReferenceMap referenceMap, ReferenceVisitor visitor,
            ClassFile classFile)
    {
        Method[] methods = classFile.getMethods();

        if (methods == null) {
            return;
        }

        ConstantPool constants = classFile.getConstantPool();

        Method method;
        AttributeSignature as;
        String signature;
        int[] exceptionIndexes;
        ElementValue defaultAnnotationValue;
        LocalVariables localVariables;
        CodeException[] codeExceptions;
        for (int i=methods.length-1; i>=0; --i)
        {
            method = methods[i];

            if ((method.getAccessFlags() &
                 (Const.ACC_SYNTHETIC|Const.ACC_BRIDGE)) != 0 ||
                method.containsError()) {
                continue;
            }

            countReferencesInAttributes(
                referenceMap, classFile.getConstantPool(), method.getAttributes());

            // Signature
            as = method.getAttributeSignature();
            signature = constants.getConstantUtf8(
                    as==null ? method.getDescriptorIndex() : as.getSignatureIndex());
            SignatureAnalyzer.analyzeMethodSignature(referenceMap, signature);

            // Exceptions
            exceptionIndexes = method.getExceptionIndexes();
            if (exceptionIndexes != null) {
                for (int j=exceptionIndexes.length-1; j>=0; --j) {
                    referenceMap.add(
                        constants.getConstantClassName(exceptionIndexes[j]));
                }
            }

            // Default annotation method value
            defaultAnnotationValue = method.getDefaultAnnotationValue();
            if (defaultAnnotationValue != null) {
                countElementValue(
                    referenceMap, constants, defaultAnnotationValue);
            }

            // Local variables
            localVariables = method.getLocalVariables();
            if (localVariables != null) {
                countReferencesInLocalVariables(
                    referenceMap, constants, localVariables);
            }

            // Code exceptions
            codeExceptions = method.getCodeExceptions();
            if (codeExceptions != null) {
                countReferencesInCodeExceptions(
                    referenceMap, constants, codeExceptions);
            }

            // Code
            countReferencesInCode(visitor, method);
        }
    }

    private static void countReferencesInLocalVariables(
            ReferenceMap referenceMap, ConstantPool constants,
            LocalVariables localVariables)
    {
        LocalVariable lv;
        for (int i=localVariables.size()-1; i>=0; --i)
        {
            lv = localVariables.getLocalVariableAt(i);

            if (lv != null && lv.getSignatureIndex() > 0)
            {
                String signature =
                    constants.getConstantUtf8(lv.getSignatureIndex());
                SignatureAnalyzer.analyzeSimpleSignature(referenceMap, signature);
            }
        }
    }

    private static void countReferencesInCodeExceptions(
            ReferenceMap referenceMap, ConstantPool constants,
            CodeException[] codeExceptions)
    {
        CodeException ce;
        for (int i=codeExceptions.length-1; i>=0; --i)
        {
            ce = codeExceptions[i];

            if (ce.catchType() != 0)
            {
                String internalClassName =
                    constants.getConstantClassName(ce.catchType());
                referenceMap.add(internalClassName);
            }
        }
    }

    private static void countReferencesInCode(
        ReferenceVisitor visitor, Method method)
    {
        List<Instruction> instructions = method.getFastNodes();

        if (instructions != null)
        {
            for (int i=instructions.size()-1; i>=0; --i) {
                visitor.visit(instructions.get(i));
            }
        }
    }

    private static void reduceReferences(
        ReferenceMap referenceMap, ClassFile classFile)
    {
        Map<String, Boolean> multipleInternalClassName =
            new HashMap<>();

        Iterator<Reference> iterator = referenceMap.values().iterator();
        while (iterator.hasNext())
        {
            Reference reference = iterator.next();
            String internalName = reference.getInternalName();

            int index =
                internalName.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);
            String internalClassName =
                index != -1 ? internalName.substring(index+1) : internalName;

            if (multipleInternalClassName.containsKey(internalClassName)) {
                multipleInternalClassName.put(internalClassName, Boolean.TRUE);
            } else {
                multipleInternalClassName.put(internalClassName, Boolean.FALSE);
            }
        }

        iterator = referenceMap.values().iterator();
        Reference reference;
        String internalName;
        int index;
        String internalPackageName;
        String internalClassName;
        String internalPackageNameClassName;
        while (iterator.hasNext())
        {
            reference = iterator.next();
            internalName = reference.getInternalName();
            index = internalName.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);
            if (index != -1)
            {
                internalPackageName = internalName.substring(0, index);
                internalClassName = internalName.substring(index+1);
            }
            else
            {
                internalPackageName = "";
                internalClassName = internalName;
            }

            internalPackageNameClassName = classFile.getInternalPackageName() +
            StringConstants.INTERNAL_PACKAGE_SEPARATOR + internalClassName;

            if (!classFile.getInternalPackageName().equals(internalPackageName) &&
                Boolean.TRUE.equals(multipleInternalClassName.get(internalClassName)) ||
                referenceMap.contains(internalPackageNameClassName))
            {
                // Remove references with same name and different packages
                // or with a name of same package of current class
                iterator.remove();
            }
        }
    }
}
