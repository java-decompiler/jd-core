/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.deserializer.classfile;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.ConstantPool;
import org.jd.core.v1.model.classfile.Field;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.*;
import org.jd.core.v1.model.classfile.constant.*;
import org.jd.core.v1.service.deserializer.classfile.attribute.InvalidAttributeLengthException;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;

import java.io.UTFDataFormatException;
import java.util.HashMap;
import java.util.Map;

import static org.jd.core.v1.model.classfile.Constants.ACC_SYNTHETIC;

public class ClassFileDeserializer {
    protected static final int[] EMPTY_INT_ARRAY = {};

    public ClassFile loadClassFile(Loader loader, String internalTypeName) throws LoaderException, UTFDataFormatException {
        ClassFile classFile = innerLoadClassFile(loader, internalTypeName);

        if (classFile == null) {
            throw new IllegalArgumentException("Class '" + internalTypeName + "' could not be loaded");
        }
        return classFile;
    }

    protected ClassFile innerLoadClassFile(Loader loader, String internalTypeName) throws LoaderException, UTFDataFormatException {
        if (!loader.canLoad(internalTypeName)) {
            return null;
        }

        byte[] data = loader.load(internalTypeName);

        if (data == null) {
            return null;
        }

        ClassFileReader reader = new ClassFileReader(data);

        // Load main type
        ClassFile classFile = loadClassFile(reader);

        // Load inner types
        AttributeInnerClasses aic = classFile.getAttribute("InnerClasses");

        if (aic != null) {
            DefaultList<ClassFile> innerClassFiles = new DefaultList<>();
            String innerTypePrefix = internalTypeName + '$';

            String innerTypeName;
            for (InnerClass ic : aic.getInnerClasses()) {
                innerTypeName = ic.getInnerTypeName();

                if (!internalTypeName.equals(innerTypeName) && (internalTypeName.equals(ic.getOuterTypeName()) || innerTypeName.startsWith(innerTypePrefix))) {
                    ClassFile innerClassFile = innerLoadClassFile(loader, innerTypeName);
                    int flags = ic.getInnerAccessFlags();
                    int length;

                    if (innerTypeName.startsWith(innerTypePrefix)) {
                        length = internalTypeName.length() + 1;
                    } else {
                        length = innerTypeName.indexOf('$') + 1;
                    }

                    if (Character.isDigit(innerTypeName.charAt(length))) {
                        flags |= ACC_SYNTHETIC;
                    }

                    if (innerClassFile == null) {
                        // Inner class not found. Create an empty one.
                        innerClassFile = new ClassFile(classFile.getMajorVersion(), classFile.getMinorVersion(), 0, innerTypeName, StringConstants.JAVA_LANG_OBJECT, null, null, null, null);
                    }

                    innerClassFile.setOuterClassFile(classFile);
                    innerClassFile.setAccessFlags(flags);
                    innerClassFiles.add(innerClassFile);
                }
            }

            if (!innerClassFiles.isEmpty()) {
                classFile.setInnerClassFiles(innerClassFiles);
            }
        }

        return classFile;
    }

    protected ClassFile loadClassFile(ClassFileReader reader) throws UTFDataFormatException {
        int magic = reader.readInt();

        if (magic != ClassFileReader.JAVA_MAGIC_NUMBER) {
            throw new ClassFileFormatException("Invalid CLASS file");
        }

        int minorVersion = reader.readUnsignedShort();
        int majorVersion = reader.readUnsignedShort();

        ConstantPool constants = new ConstantPool(loadConstants(reader));

        int accessFlags = reader.readUnsignedShort();
        int thisClassIndex = reader.readUnsignedShort();
        int superClassIndex = reader.readUnsignedShort();

        String internalTypeName = constants.getConstantTypeName(thisClassIndex);
        String superTypeName = (superClassIndex == 0) ? null : constants.getConstantTypeName(superClassIndex);
        String[] interfaceTypeNames = loadInterfaces(reader, constants);
        Field[] fields = loadFields(reader, constants);
        Method[] methods = loadMethods(reader, constants);
        Map<String, Attribute> attributes = loadAttributes(reader, constants);

        return new ClassFile(majorVersion, minorVersion, accessFlags, internalTypeName, superTypeName, interfaceTypeNames, fields, methods, attributes);
    }

    protected Constant[] loadConstants(ClassFileReader reader) throws UTFDataFormatException {
        int count = reader.readUnsignedShort();

        if (count == 0) {
            return null;
        }

        Constant[] constants = new Constant[count];

        int tag;
        for (int i=1; i<count; i++) {
            tag = reader.readByte();

            switch (tag) {
                case 1:
                    constants[i] = new ConstantUtf8(reader.readUTF8());
                    break;
                case 3:
                    constants[i] = new ConstantInteger(reader.readInt());
                    break;
                case 4:
                    constants[i] = new ConstantFloat(reader.readFloat());
                    break;
                case 5:
                    constants[i] = new ConstantLong(reader.readLong());
                i++;
                    break;
                case 6:
                    constants[i] = new ConstantDouble(reader.readDouble());
                i++;
                    break;
                case 7: case 19: case 20:
                    constants[i] = new ConstantClass(reader.readUnsignedShort());
                    break;
                case 8:
                    constants[i] = new ConstantString(reader.readUnsignedShort());
                    break;
                case 9: case 10: case 11: case 17: case 18:
                    constants[i] = new ConstantMemberRef(reader.readUnsignedShort(), reader.readUnsignedShort());
                    break;
                case 12:
                    constants[i] = new ConstantNameAndType(reader.readUnsignedShort(), reader.readUnsignedShort());
                    break;
                case 15:
                    constants[i] = new ConstantMethodHandle(reader.readByte(), reader.readUnsignedShort());
                    break;
                case 16:
                    constants[i] = new ConstantMethodType(reader.readUnsignedShort());
                    break;
                default:
                    throw new ClassFileFormatException("Invalid constant pool entry");
            }
        }

        return constants;
    }

    protected String[] loadInterfaces(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        String[] interfaceTypeNames = new String[count];

        int index;
        for (int i=0; i<count; i++) {
            index = reader.readUnsignedShort();
            interfaceTypeNames[i] = constants.getConstantTypeName(index);
        }

        return interfaceTypeNames;
    }

    protected Field[] loadFields(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        Field[] fields = new Field[count];

        int accessFlags;
        int nameIndex;
        int descriptorIndex;
        Map<String, Attribute> attributes;
        String name;
        String descriptor;
        for (int i=0; i<count; i++) {
            accessFlags = reader.readUnsignedShort();
            nameIndex = reader.readUnsignedShort();
            descriptorIndex = reader.readUnsignedShort();
            attributes = loadAttributes(reader, constants);

            name = constants.getConstantUtf8(nameIndex);
            descriptor = constants.getConstantUtf8(descriptorIndex);

            fields[i] = new Field(accessFlags, name, descriptor, attributes);
        }

        return fields;
    }

    protected Method[] loadMethods(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        Method[] methods = new Method[count];

        int accessFlags;
        int nameIndex;
        int descriptorIndex;
        Map<String, Attribute> attributes;
        String name;
        String descriptor;
        for (int i=0; i<count; i++) {
            accessFlags = reader.readUnsignedShort();
            nameIndex = reader.readUnsignedShort();
            descriptorIndex = reader.readUnsignedShort();
            attributes = loadAttributes(reader, constants);

            name = constants.getConstantUtf8(nameIndex);
            descriptor = constants.getConstantUtf8(descriptorIndex);

            methods[i] = new Method(accessFlags, name, descriptor, attributes, constants);
        }

        return methods;
    }

    protected Map<String, Attribute> loadAttributes(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        Map<String, Attribute> attributes = new HashMap<>();

        int attributeNameIndex;
        int attributeLength;
        Constant constant;
        String name;
        for (int i=0; i<count; i++) {
            attributeNameIndex = reader.readUnsignedShort();
            attributeLength = reader.readInt();

            constant = constants.getConstant(attributeNameIndex);

            if (constant.getTag() != Constant.CONSTANT_UTF8) {
                throw new ClassFileFormatException("Invalid attributes");
            }
            name = ((ConstantUtf8)constant).getValue();
            switch (name) {
                case "AnnotationDefault":
                    attributes.put(name, new AttributeAnnotationDefault(loadElementValue(reader, constants)));
                    break;
                case "BootstrapMethods":
                    attributes.put(name, new AttributeBootstrapMethods(loadBootstrapMethods(reader)));
                    break;
                case "Code":
                    attributes.put(name, new AttributeCode(
                            reader.readUnsignedShort(), reader.readUnsignedShort(),
                            loadCode(reader), loadCodeExceptions(reader), loadAttributes(reader, constants)));
                    break;
                case "ConstantValue":
                    if (attributeLength != 2) {
                        throw new InvalidAttributeLengthException();
                    }
                    attributes.put(name, new AttributeConstantValue(loadConstantValue(reader, constants)));
                    break;
                case "Deprecated":
                    if (attributeLength != 0) {
                        throw new InvalidAttributeLengthException();
                    }
                    attributes.put(name, new AttributeDeprecated());
                    break;
                case "Exceptions":
                    attributes.put(name, new AttributeExceptions(loadExceptionTypeNames(reader, constants)));
                    break;
                case "InnerClasses":
                    attributes.put(name, new AttributeInnerClasses(loadInnerClasses(reader, constants)));
                    break;
                case "LocalVariableTable":
                    LocalVariable[] localVariables = loadLocalVariables(reader, constants);
                    if (localVariables != null) {
                        attributes.put(name, new AttributeLocalVariableTable(localVariables));
                    }
                    break;
                case "LocalVariableTypeTable":
                    attributes.put(name, new AttributeLocalVariableTypeTable(loadLocalVariableTypes(reader, constants)));
                    break;
                case "LineNumberTable":
                    attributes.put(name, new AttributeLineNumberTable(loadLineNumbers(reader)));
                    break;
                case "MethodParameters":
                    attributes.put(name, new AttributeMethodParameters(loadParameters(reader, constants)));
                    break;
                case "Module":
                    attributes.put(name, new AttributeModule(
                            constants.getConstantTypeName(reader.readUnsignedShort()),
                            reader.readUnsignedShort(),
                            constants.getConstantUtf8(reader.readUnsignedShort()),
                            loadModuleInfos(reader, constants),
                            loadPackageInfos(reader, constants),
                            loadPackageInfos(reader, constants),
                            loadConstantClassNames(reader, constants),
                            loadServiceInfos(reader, constants)));
                    break;
                case "ModulePackages":
                    attributes.put(name, new AttributeModulePackages(loadConstantClassNames(reader, constants)));
                    break;
                case "ModuleMainClass":
                    attributes.put(name, new AttributeModuleMainClass(constants.getConstant(reader.readUnsignedShort())));
                    break;
                case StringConstants.RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_NAME:
                case StringConstants.RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME:
                    Annotation[] annotations = loadAnnotations(reader, constants);
                    if (annotations != null) {
                        attributes.put(name, new Annotations(annotations));
                    }
                    break;
                case "RuntimeInvisibleParameterAnnotations":
                case "RuntimeVisibleParameterAnnotations":
                    attributes.put(name, new AttributeParameterAnnotations(loadParameterAnnotations(reader, constants)));
                    break;
                case StringConstants.SIGNATURE_ATTRIBUTE_NAME:
                    if (attributeLength != 2) {
                        throw new InvalidAttributeLengthException();
                    }
                    attributes.put(name, new AttributeSignature(constants.getConstantUtf8(reader.readUnsignedShort())));
                    break;
                case "SourceFile":
                    if (attributeLength != 2) {
                        throw new InvalidAttributeLengthException();
                    }
                    attributes.put(name, new AttributeSourceFile(constants.getConstantUtf8(reader.readUnsignedShort())));
                    break;
                case "Synthetic":
                    if (attributeLength != 0) {
                        throw new InvalidAttributeLengthException();
                    }
                    attributes.put(name, new AttributeSynthetic());
                    break;
                default:
                    attributes.put(name, new UnknownAttribute());
                    reader.skip(attributeLength);
            }
        }

        return attributes;
    }

    protected AttributeElementValue loadElementValue(ClassFileReader reader, ConstantPool constants) {
        int type = reader.readByte();

        switch (type) {
            case 'B': case 'D': case 'F':
            case 'I': case 'J': case 'S':
            case 'Z': case 'C': case 's':
                int constValueIndex = reader.readUnsignedShort();
                ConstantValue constValue = (ConstantValue)constants.getConstant(constValueIndex);
                return new ElementValuePrimitiveType(type, constValue);
            case 'e':
                int descriptorIndex = reader.readUnsignedShort();
                String descriptor = constants.getConstantUtf8(descriptorIndex);
                int constNameIndex = reader.readUnsignedShort();
                String constName = constants.getConstantUtf8(constNameIndex);
                return new ElementValueEnumConstValue(descriptor, constName);
            case 'c':
                int classInfoIndex = reader.readUnsignedShort();
                String classInfo = constants.getConstantUtf8(classInfoIndex);
                return new ElementValueClassInfo(classInfo);
            case '@':
                int typeIndex = reader.readUnsignedShort();
                descriptor = constants.getConstantUtf8(typeIndex);
                return new ElementValueAnnotationValue(new Annotation(descriptor, loadElementValuePairs(reader, constants)));
            case '[':
                return new ElementValueArrayValue(loadElementValues(reader, constants));
            default:
                throw new ClassFileFormatException("Invalid element value type: " + type);
        }
    }

    protected ElementValuePair[] loadElementValuePairs(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        ElementValuePair[] pairs = new ElementValuePair[count];

        int elementNameIndex;
        String elementName;
        for (int i=0; i < count; i++) {
            elementNameIndex = reader.readUnsignedShort();
            elementName = constants.getConstantUtf8(elementNameIndex);
            pairs[i] = new ElementValuePair(elementName, loadElementValue(reader, constants));
        }

        return pairs;
    }

    protected AttributeElementValue[] loadElementValues(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        AttributeElementValue[] values = new AttributeElementValue[count];

        for (int i=0; i<count; i++) {
            values[i] = loadElementValue(reader, constants);
        }

        return values;
    }

    protected BootstrapMethod[] loadBootstrapMethods(ClassFileReader reader) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        BootstrapMethod[] values = new BootstrapMethod[count];

        int bootstrapMethodRef;
        int numBootstrapArguments;
        int[] bootstrapArguments;
        for (int i=0; i<count; i++) {
            bootstrapMethodRef = reader.readUnsignedShort();
            numBootstrapArguments = reader.readUnsignedShort();
            if (numBootstrapArguments == 0) {
                bootstrapArguments = EMPTY_INT_ARRAY;
            } else {
                bootstrapArguments = new int[numBootstrapArguments];
                for (int j=0; j<numBootstrapArguments; j++) {
                    bootstrapArguments[j] = reader.readUnsignedShort();
                }
            }

            values[i] = new BootstrapMethod(bootstrapMethodRef, bootstrapArguments);
        }

        return values;
    }

    protected byte[] loadCode(ClassFileReader reader) {
        int code_length = reader.readInt();
        if (code_length == 0) {
            return null;
        }

        byte[] code = new byte[code_length];
        reader.readFully(code);

        return code;
    }

    protected CodeException[] loadCodeExceptions(ClassFileReader reader) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        CodeException[] codeExceptions = new CodeException[count];

        for (int i=0; i<count; i++) {
            codeExceptions[i] = new CodeException(i,
                    reader.readUnsignedShort(),
                    reader.readUnsignedShort(),
                    reader.readUnsignedShort(),
                    reader.readUnsignedShort());
        }

        return codeExceptions;
    }

    protected ConstantValue loadConstantValue(ClassFileReader reader, ConstantPool constants) {
        int constantValueIndex = reader.readUnsignedShort();

        return constants.getConstantValue(constantValueIndex);
    }

    protected String[] loadExceptionTypeNames(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        String[] exceptionTypeNames = new String[count];

        int exceptionClassIndex;
        for (int i=0; i < count; i++) {
            exceptionClassIndex = reader.readUnsignedShort();
            exceptionTypeNames[i] = constants.getConstantTypeName(exceptionClassIndex);
        }

        return exceptionTypeNames;
    }

    protected InnerClass[] loadInnerClasses(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        InnerClass[] innerClasses = new InnerClass[count];

        int innerTypeIndex;
        int outerTypeIndex;
        int innerNameIndex;
        int innerAccessFlags;
        String innerTypeName;
        String outerTypeName;
        String innerName;
        for (int i=0; i < count; i++) {
            innerTypeIndex = reader.readUnsignedShort();
            outerTypeIndex = reader.readUnsignedShort();
            innerNameIndex = reader.readUnsignedShort();
            innerAccessFlags = reader.readUnsignedShort();

            innerTypeName = constants.getConstantTypeName(innerTypeIndex);
            outerTypeName = (outerTypeIndex == 0) ? null : constants.getConstantTypeName(outerTypeIndex);
            innerName = (innerNameIndex == 0) ? null : constants.getConstantUtf8(innerNameIndex);

            innerClasses[i] = new InnerClass(innerTypeName, outerTypeName, innerName, innerAccessFlags);
        }

        return innerClasses;
    }

    protected LocalVariable[] loadLocalVariables(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        LocalVariable[] localVariables = new LocalVariable[count];

        int startPc;
        int length;
        int nameIndex;
        int descriptorIndex;
        int index;
        String name;
        String descriptor;
        for (int i=0; i<count; i++) {
            startPc = reader.readUnsignedShort();
            length = reader.readUnsignedShort();
            nameIndex = reader.readUnsignedShort();
            descriptorIndex = reader.readUnsignedShort();
            index = reader.readUnsignedShort();

            name = constants.getConstantUtf8(nameIndex);
            descriptor = constants.getConstantUtf8(descriptorIndex);

            localVariables[i] = new LocalVariable(startPc, length, name, descriptor, index);
        }

        return localVariables;
    }

    protected LocalVariableType[] loadLocalVariableTypes(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        LocalVariableType[] localVariables = new LocalVariableType[count];

        int startPc;
        int length;
        int nameIndex;
        int descriptorIndex;
        int index;
        String name;
        String descriptor;
        for (int i=0; i<count; i++) {
            startPc = reader.readUnsignedShort();
            length = reader.readUnsignedShort();
            nameIndex = reader.readUnsignedShort();
            descriptorIndex = reader.readUnsignedShort();
            index = reader.readUnsignedShort();

            name = constants.getConstantUtf8(nameIndex);
            descriptor = constants.getConstantUtf8(descriptorIndex);

            localVariables[i] = new LocalVariableType(startPc, length, name, descriptor, index);
        }

        return localVariables;
    }

    protected LineNumber[] loadLineNumbers(ClassFileReader reader) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        LineNumber[] lineNumbers = new LineNumber[count];

        for (int i=0; i<count; i++) {
            lineNumbers[i] = new LineNumber(reader.readUnsignedShort(), reader.readUnsignedShort());
        }

        return lineNumbers;
    }

    protected MethodParameter[] loadParameters(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedByte();
        if (count == 0) {
            return null;
        }

        MethodParameter[] parameters = new MethodParameter[count];

        int nameIndex;
        String name;
        for (int i=0; i<count; i++) {
            nameIndex = reader.readUnsignedShort();

            name = constants.getConstantUtf8(nameIndex);

            parameters[i] = new MethodParameter(name, reader.readUnsignedShort());
        }

        return parameters;
    }

    protected ModuleInfo[] loadModuleInfos(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        ModuleInfo[] moduleInfos = new ModuleInfo[count];

        int moduleInfoIndex;
        int moduleFlag;
        int moduleVersionIndex;
        String moduleInfoName;
        String moduleVersion;
        for (int i=0; i<count; i++) {
            moduleInfoIndex = reader.readUnsignedShort();
            moduleFlag = reader.readUnsignedShort();
            moduleVersionIndex = reader.readUnsignedShort();

            moduleInfoName = constants.getConstantTypeName(moduleInfoIndex);
            moduleVersion = (moduleVersionIndex==0) ? null : constants.getConstantUtf8(moduleVersionIndex);

            moduleInfos[i] = new ModuleInfo(moduleInfoName, moduleFlag, moduleVersion);
        }

        return moduleInfos;
    }

    protected PackageInfo[] loadPackageInfos(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        PackageInfo[] packageInfos = new PackageInfo[count];

        int packageInfoIndex;
        int packageFlag;
        String packageInfoName;
        for (int i=0; i<count; i++) {
            packageInfoIndex = reader.readUnsignedShort();
            packageFlag = reader.readUnsignedShort();

            packageInfoName = constants.getConstantTypeName(packageInfoIndex);

            packageInfos[i] = new PackageInfo(packageInfoName, packageFlag, loadConstantClassNames(reader, constants));
        }

        return packageInfos;
    }

    protected String[] loadConstantClassNames(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        String[] names = new String[count];

        for (int i=0; i<count; i++) {
            names[i] = constants.getConstantTypeName(reader.readUnsignedShort());
        }

        return names;
    }

    protected ServiceInfo[] loadServiceInfos(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        ServiceInfo[] services = new ServiceInfo[count];

        for (int i=0; i<count; i++) {
            services[i] = new ServiceInfo(
                    constants.getConstantTypeName(reader.readUnsignedShort()),
                    loadConstantClassNames(reader, constants));
        }

        return services;
    }

    protected Annotation[] loadAnnotations(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        Annotation[] annotations = new Annotation[count];

        int descriptorIndex;
        String descriptor;
        for (int i=0; i<count; i++) {
            descriptorIndex = reader.readUnsignedShort();
            descriptor = constants.getConstantUtf8(descriptorIndex);
            annotations[i] = new Annotation(descriptor, loadElementValuePairs(reader, constants));
        }

        return annotations;
    }

    protected Annotations[] loadParameterAnnotations(ClassFileReader reader, ConstantPool constants) {
        int count = reader.readUnsignedByte();
        if (count == 0) {
            return null;
        }

        Annotations[] parameterAnnotations = new Annotations[count];

        Annotation[] annotations;
        for (int i=0; i < count; i++) {
            annotations = loadAnnotations(reader, constants);
            if (annotations != null) {
                parameterAnnotations[i] = new Annotations(annotations);
            }
        }

        return parameterAnnotations;
    }
}
