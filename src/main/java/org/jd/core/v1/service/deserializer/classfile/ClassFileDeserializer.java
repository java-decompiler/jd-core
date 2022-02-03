/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.deserializer.classfile;

import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodType;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.MethodParameter;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.ConstantPool;
import org.jd.core.v1.model.classfile.Field;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.Annotation;
import org.jd.core.v1.model.classfile.attribute.Annotations;
import org.jd.core.v1.model.classfile.attribute.Attribute;
import org.jd.core.v1.model.classfile.attribute.AttributeAnnotationDefault;
import org.jd.core.v1.model.classfile.attribute.AttributeBootstrapMethods;
import org.jd.core.v1.model.classfile.attribute.AttributeCode;
import org.jd.core.v1.model.classfile.attribute.AttributeConstantValue;
import org.jd.core.v1.model.classfile.attribute.AttributeDeprecated;
import org.jd.core.v1.model.classfile.attribute.AttributeElementValue;
import org.jd.core.v1.model.classfile.attribute.AttributeExceptions;
import org.jd.core.v1.model.classfile.attribute.AttributeInnerClasses;
import org.jd.core.v1.model.classfile.attribute.AttributeLineNumberTable;
import org.jd.core.v1.model.classfile.attribute.AttributeLocalVariableTable;
import org.jd.core.v1.model.classfile.attribute.AttributeLocalVariableTypeTable;
import org.jd.core.v1.model.classfile.attribute.AttributeMethodParameters;
import org.jd.core.v1.model.classfile.attribute.AttributeModule;
import org.jd.core.v1.model.classfile.attribute.AttributeModuleMainClass;
import org.jd.core.v1.model.classfile.attribute.AttributeModulePackages;
import org.jd.core.v1.model.classfile.attribute.AttributeParameterAnnotations;
import org.jd.core.v1.model.classfile.attribute.AttributeSignature;
import org.jd.core.v1.model.classfile.attribute.AttributeSourceFile;
import org.jd.core.v1.model.classfile.attribute.AttributeSynthetic;
import org.jd.core.v1.model.classfile.attribute.CodeException;
import org.jd.core.v1.model.classfile.attribute.ElementValueAnnotationValue;
import org.jd.core.v1.model.classfile.attribute.ElementValueArrayValue;
import org.jd.core.v1.model.classfile.attribute.ElementValueClassInfo;
import org.jd.core.v1.model.classfile.attribute.ElementValueEnumConstValue;
import org.jd.core.v1.model.classfile.attribute.ElementValuePrimitiveType;
import org.jd.core.v1.model.classfile.attribute.InnerClass;
import org.jd.core.v1.model.classfile.attribute.LocalVariable;
import org.jd.core.v1.model.classfile.attribute.LocalVariableType;
import org.jd.core.v1.model.classfile.attribute.ModuleInfo;
import org.jd.core.v1.model.classfile.attribute.PackageInfo;
import org.jd.core.v1.model.classfile.attribute.ServiceInfo;
import org.jd.core.v1.model.classfile.attribute.UnknownAttribute;
import org.jd.core.v1.model.classfile.constant.ConstantMemberRef;
import org.jd.core.v1.service.deserializer.classfile.attribute.InvalidAttributeLengthException;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.apache.bcel.Const.ACC_SYNTHETIC;
import static org.apache.bcel.Const.CONSTANT_Class;
import static org.apache.bcel.Const.CONSTANT_Double;
import static org.apache.bcel.Const.CONSTANT_Dynamic;
import static org.apache.bcel.Const.CONSTANT_Fieldref;
import static org.apache.bcel.Const.CONSTANT_Float;
import static org.apache.bcel.Const.CONSTANT_Integer;
import static org.apache.bcel.Const.CONSTANT_InterfaceMethodref;
import static org.apache.bcel.Const.CONSTANT_InvokeDynamic;
import static org.apache.bcel.Const.CONSTANT_Long;
import static org.apache.bcel.Const.CONSTANT_MethodHandle;
import static org.apache.bcel.Const.CONSTANT_MethodType;
import static org.apache.bcel.Const.CONSTANT_Methodref;
import static org.apache.bcel.Const.CONSTANT_Module;
import static org.apache.bcel.Const.CONSTANT_NameAndType;
import static org.apache.bcel.Const.CONSTANT_Package;
import static org.apache.bcel.Const.CONSTANT_String;
import static org.apache.bcel.Const.CONSTANT_Utf8;

import jd.core.CoreConstants;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;
import jd.core.process.deserializer.ClassFormatException;

public final class ClassFileDeserializer {
    private static final int[] EMPTY_INT_ARRAY = {};

    public ClassFile loadClassFile(Loader loader, String internalTypeName) throws IOException {
        ClassFile classFile = innerLoadClassFile(loader, internalTypeName);

        if (classFile == null) {
            throw new IllegalArgumentException("Class '" + internalTypeName + "' could not be loaded");
        }
        return classFile;
    }

    private ClassFile innerLoadClassFile(Loader loader, String internalTypeName) throws IOException {
        if (!loader.canLoad(internalTypeName)) {
            return null;
        }

        byte[] data = loader.load(internalTypeName);

        if (data == null) {
            return null;
        }

        try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(data))) {

            // Load main type
            ClassFile classFile = loadClassFile(reader);

            // Load inner types
            AttributeInnerClasses aic = classFile.getAttribute("InnerClasses");

            if (aic != null) {
                DefaultList<ClassFile> innerClassFiles = new DefaultList<>();
                String innerTypePrefix = internalTypeName + '$';

                String innerTypeName;
                for (InnerClass ic : aic.getInnerClasses()) {
                    innerTypeName = ic.innerTypeName();

                    if (!internalTypeName.equals(innerTypeName) && (internalTypeName.equals(ic.outerTypeName()) || innerTypeName.startsWith(innerTypePrefix))) {
                        ClassFile innerClassFile = innerLoadClassFile(loader, innerTypeName);
                        int flags = ic.innerAccessFlags();
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
    }

    private ClassFile loadClassFile(DataInput reader) throws IOException {
        int magic = reader.readInt();

        if (magic != CoreConstants.JAVA_MAGIC_NUMBER) {
            throw new ClassFormatException("Invalid CLASS file");
        }

        int minorVersion = reader.readUnsignedShort();
        int majorVersion = reader.readUnsignedShort();

        ConstantPool constants = new ConstantPool(loadConstants(reader));

        int accessFlags = reader.readUnsignedShort();
        int thisClassIndex = reader.readUnsignedShort();
        int superClassIndex = reader.readUnsignedShort();

        String internalTypeName = constants.getConstantTypeName(thisClassIndex);
        String superTypeName = superClassIndex == 0 ? null : constants.getConstantTypeName(superClassIndex);
        String[] interfaceTypeNames = loadInterfaces(reader, constants);
        Field[] fields = loadFields(reader, constants);
        Method[] methods = loadMethods(reader, constants, internalTypeName);
        Map<String, Attribute> attributes = loadAttributes(reader, constants);

        return new ClassFile(majorVersion, minorVersion, accessFlags, internalTypeName, superTypeName, interfaceTypeNames, fields, methods, attributes);
    }

    private static Constant[] loadConstants(DataInput reader) throws IOException {
        int count = reader.readUnsignedShort();

        if (count == 0) {
            return null;
        }

        Constant[] constants = new Constant[count];

        int tag;
        for (int i=1; i<count; i++) {
            tag = reader.readByte();
            switch (tag) {
                case CONSTANT_Utf8:
                    constants[i] = new ConstantUtf8(reader.readUTF());
                    break;
                case CONSTANT_Integer:
                    constants[i] = new ConstantInteger(reader.readInt());
                    break;
                case CONSTANT_Float:
                    constants[i] = new ConstantFloat(reader.readFloat());
                    break;
                case CONSTANT_Long:
                    constants[i] = new ConstantLong(reader.readLong());
                    i++;
                    break;
                case CONSTANT_Double:
                    constants[i] = new ConstantDouble(reader.readDouble());
                    i++;
                    break;
                case CONSTANT_Class, CONSTANT_Module, CONSTANT_Package:
                    constants[i] = new ConstantClass(reader.readUnsignedShort());
                    break;
                case CONSTANT_String:
                    constants[i] = new ConstantString(reader.readUnsignedShort());
                    break;
                case CONSTANT_Fieldref, CONSTANT_Methodref, CONSTANT_InterfaceMethodref, CONSTANT_Dynamic, CONSTANT_InvokeDynamic:
                    constants[i] = new ConstantMemberRef(reader.readUnsignedShort(), reader.readUnsignedShort());
                    break;
                case CONSTANT_NameAndType:
                    constants[i] = new ConstantNameAndType(reader.readUnsignedShort(), reader.readUnsignedShort());
                    break;
                case CONSTANT_MethodHandle:
                    constants[i] = new ConstantMethodHandle(reader.readByte(), reader.readUnsignedShort());
                    break;
                case CONSTANT_MethodType:
                    constants[i] = new ConstantMethodType(reader.readUnsignedShort());
                    break;
                default:
                    throw new ClassFormatException("Invalid constant pool entry");
            }
        }

        return constants;
    }

    private static String[] loadInterfaces(DataInput reader, ConstantPool constants) throws IOException {
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

    private Field[] loadFields(DataInput reader, ConstantPool constants) throws IOException {
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

    private Method[] loadMethods(DataInput reader, ConstantPool constants, String className) throws IOException {
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

            methods[i] = new Method(accessFlags, name, descriptor, attributes, constants, className);
        }

        return methods;
    }

    private Map<String, Attribute> loadAttributes(DataInput reader, ConstantPool constants) throws IOException {
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

            if (!(constant instanceof ConstantUtf8)) {
                throw new ClassFormatException("Invalid attributes");
            }
            name = ((ConstantUtf8)constant).getBytes();
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
                    attributes.put(name, new AttributeMethodParameters(loadParameters(reader)));
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
                case StringConstants.RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_NAME, StringConstants.RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME:
                    Annotation[] annotations = loadAnnotations(reader, constants);
                    if (annotations != null) {
                        attributes.put(name, new Annotations(annotations));
                    }
                    break;
                case "RuntimeInvisibleParameterAnnotations", "RuntimeVisibleParameterAnnotations":
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
                    reader.skipBytes(attributeLength);
            }
        }

        return attributes;
    }

    private AttributeElementValue loadElementValue(DataInput reader, ConstantPool constants) throws IOException {
        int type = reader.readByte();

        switch (type) {
            case 'B', 'D', 'F', 'I', 'J', 'S', 'Z', 'C', 's':
                int constValueIndex = reader.readUnsignedShort();
                Constant constValue = constants.getConstant(constValueIndex);
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
                throw new ClassFormatException("Invalid element value type: " + type);
        }
    }

    private List<Entry<String, AttributeElementValue>> loadElementValuePairs(DataInput reader, ConstantPool constants) throws IOException {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        List<Entry<String, AttributeElementValue>> pairs = new ArrayList<>(count);

        int elementNameIndex;
        String elementName;
        for (int i=0; i < count; i++) {
            elementNameIndex = reader.readUnsignedShort();
            elementName = constants.getConstantUtf8(elementNameIndex);
            pairs.add(new SimpleEntry<>(elementName, loadElementValue(reader, constants)));
        }

        return pairs;
    }

    private AttributeElementValue[] loadElementValues(DataInput reader, ConstantPool constants) throws IOException {
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

    public static BootstrapMethod[] loadBootstrapMethods(DataInput reader) throws IOException {
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

    public static byte[] loadCode(DataInput reader) throws IOException {
        int codeLength = reader.readInt();
        if (codeLength == 0) {
            return null;
        }

        byte[] code = new byte[codeLength];
        reader.readFully(code);

        return ByteCodeUtil.cleanUpByteCode(code);
    }

    public static CodeException[] loadCodeExceptions(DataInput reader) throws IOException {
        int count = reader.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        CodeException[] codeExceptions = new CodeException[count];

        for (int i=0; i<count; i++) {
            codeExceptions[i] = new CodeException(i, reader.readUnsignedShort(),
                                                  reader.readUnsignedShort(),
                                                  reader.readUnsignedShort(),
                                                  reader.readUnsignedShort());
        }

        return codeExceptions;
    }

    private static Constant loadConstantValue(DataInput reader, ConstantPool constants) throws IOException {
        int constantValueIndex = reader.readUnsignedShort();

        return constants.getConstantValue(constantValueIndex);
    }

    private static String[] loadExceptionTypeNames(DataInput reader, ConstantPool constants) throws IOException {
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

    private static InnerClass[] loadInnerClasses(DataInput reader, ConstantPool constants) throws IOException {
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
            outerTypeName = outerTypeIndex == 0 ? null : constants.getConstantTypeName(outerTypeIndex);
            innerName = innerNameIndex == 0 ? null : constants.getConstantUtf8(innerNameIndex);

            innerClasses[i] = new InnerClass(innerTypeName, outerTypeName, innerName, innerAccessFlags);
        }

        return innerClasses;
    }

    private static LocalVariable[] loadLocalVariables(DataInput reader, ConstantPool constants) throws IOException {
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

    private static LocalVariableType[] loadLocalVariableTypes(DataInput reader, ConstantPool constants) throws IOException {
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

    public static LineNumber[] loadLineNumbers(DataInput reader) throws IOException {
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

    public static MethodParameter[] loadParameters(DataInput reader) throws IOException {
        int count = reader.readUnsignedByte();
        if (count == 0) {
            return null;
        }

        MethodParameter[] parameters = new MethodParameter[count];
        for (int i=0; i<count; i++) {
            parameters[i] = new MethodParameter();
            parameters[i].setNameIndex(reader.readUnsignedShort());
            parameters[i].setAccessFlags(reader.readUnsignedShort());
        }

        return parameters;
    }

    private static ModuleInfo[] loadModuleInfos(DataInput reader, ConstantPool constants) throws IOException {
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
            moduleVersion = moduleVersionIndex==0 ? null : constants.getConstantUtf8(moduleVersionIndex);

            moduleInfos[i] = new ModuleInfo(moduleInfoName, moduleFlag, moduleVersion);
        }

        return moduleInfos;
    }

    private static PackageInfo[] loadPackageInfos(DataInput reader, ConstantPool constants) throws IOException {
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

    private static String[] loadConstantClassNames(DataInput reader, ConstantPool constants) throws IOException {
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

    private static ServiceInfo[] loadServiceInfos(DataInput reader, ConstantPool constants) throws IOException {
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

    private Annotation[] loadAnnotations(DataInput reader, ConstantPool constants) throws IOException {
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

    private Annotations[] loadParameterAnnotations(DataInput reader, ConstantPool constants) throws IOException {
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
