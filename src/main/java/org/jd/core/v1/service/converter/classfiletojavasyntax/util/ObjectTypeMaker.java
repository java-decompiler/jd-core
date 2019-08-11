/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.classfile.constant.ConstantClass;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.deserializer.classfile.ClassFileFormatException;
import org.jd.core.v1.service.deserializer.classfile.ClassFileReader;

import java.util.HashMap;

import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_UNDEFINED_OBJECT;

public class ObjectTypeMaker {
    protected static final HashMap<String, ObjectType> OBJECT_TYPE_CACHE = new HashMap<>();
    protected static final HashMap<String, ObjectType> OBJECT_PRIMITIVE_TYPE_CACHE = new HashMap<>();

    static {
        OBJECT_TYPE_CACHE.put(ObjectType.TYPE_CLASS.getInternalName(),             ObjectType.TYPE_CLASS);
        OBJECT_TYPE_CACHE.put(ObjectType.TYPE_OBJECT.getInternalName(),            ObjectType.TYPE_OBJECT);
        OBJECT_TYPE_CACHE.put(ObjectType.TYPE_STRING.getInternalName(),            ObjectType.TYPE_STRING);
        OBJECT_TYPE_CACHE.put(ObjectType.TYPE_THROWABLE.getInternalName(),         ObjectType.TYPE_THROWABLE);
        OBJECT_TYPE_CACHE.put(ObjectType.TYPE_EXCEPTION.getInternalName(),         ObjectType.TYPE_EXCEPTION);
        OBJECT_TYPE_CACHE.put(ObjectType.TYPE_RUNTIME_EXCEPTION.getInternalName(), ObjectType.TYPE_RUNTIME_EXCEPTION);
        OBJECT_TYPE_CACHE.put(ObjectType.TYPE_SYSTEM.getInternalName(),            ObjectType.TYPE_SYSTEM);
        OBJECT_TYPE_CACHE.put(ObjectType.TYPE_STRING_BUILDER.getInternalName(),    ObjectType.TYPE_STRING_BUILDER);
        OBJECT_TYPE_CACHE.put(ObjectType.TYPE_STRING_BUFFER.getInternalName(),     ObjectType.TYPE_STRING_BUFFER);
        OBJECT_TYPE_CACHE.put(ObjectType.TYPE_THREAD.getInternalName(),            ObjectType.TYPE_THREAD);

        OBJECT_PRIMITIVE_TYPE_CACHE.put(ObjectType.TYPE_PRIMITIVE_BOOLEAN.getInternalName(), ObjectType.TYPE_PRIMITIVE_BOOLEAN);
        OBJECT_PRIMITIVE_TYPE_CACHE.put(ObjectType.TYPE_PRIMITIVE_BYTE.getInternalName(),    ObjectType.TYPE_PRIMITIVE_BYTE);
        OBJECT_PRIMITIVE_TYPE_CACHE.put(ObjectType.TYPE_PRIMITIVE_CHAR.getInternalName(),    ObjectType.TYPE_PRIMITIVE_CHAR);
        OBJECT_PRIMITIVE_TYPE_CACHE.put(ObjectType.TYPE_PRIMITIVE_DOUBLE.getInternalName(),  ObjectType.TYPE_PRIMITIVE_DOUBLE);
        OBJECT_PRIMITIVE_TYPE_CACHE.put(ObjectType.TYPE_PRIMITIVE_FLOAT.getInternalName(),   ObjectType.TYPE_PRIMITIVE_FLOAT);
        OBJECT_PRIMITIVE_TYPE_CACHE.put(ObjectType.TYPE_PRIMITIVE_INT.getInternalName(),     ObjectType.TYPE_PRIMITIVE_INT);
        OBJECT_PRIMITIVE_TYPE_CACHE.put(ObjectType.TYPE_PRIMITIVE_LONG.getInternalName(),    ObjectType.TYPE_PRIMITIVE_LONG);
        OBJECT_PRIMITIVE_TYPE_CACHE.put(ObjectType.TYPE_PRIMITIVE_SHORT.getInternalName(),   ObjectType.TYPE_PRIMITIVE_SHORT);
        OBJECT_PRIMITIVE_TYPE_CACHE.put(ObjectType.TYPE_PRIMITIVE_VOID.getInternalName(),    ObjectType.TYPE_PRIMITIVE_VOID);
    }

    protected HashMap<String, ObjectType> descriptorToObjectTypeCache = new HashMap<>(1024);
    protected HashMap<String, ObjectType> internalTypeNameToObjectTypeCache = new HashMap<>(1024);
    protected HashMap<String, String[]> hierarchy = new HashMap<>(1024);
    protected Loader loader;

    public ObjectTypeMaker(Loader loader) {
        this.loader = loader;
    }

    public ObjectType makeFromDescriptor(String descriptor) {
        ObjectType ot = descriptorToObjectTypeCache.get(descriptor);

        if (ot == null) {
            if (descriptor.charAt(0) == '[') {
                int dimension = 1;

                while (descriptor.charAt(dimension) == '[') {
                    dimension++;
                }

                ot = (ObjectType)makeFromDescriptorWithoutBracket(descriptor.substring(dimension)).createType(dimension);
            } else {
                ot = makeFromDescriptorWithoutBracket(descriptor);
            }

            descriptorToObjectTypeCache.put(descriptor, ot);
        }

        return ot;
    }

    private ObjectType makeFromDescriptorWithoutBracket(String descriptor) {
        ObjectType ot = OBJECT_PRIMITIVE_TYPE_CACHE.get(descriptor);

        if (ot == null) {
            ot = makeFromInternalTypeName(descriptor.substring(1, descriptor.length()-1));
        }

        return ot;
    }

    public ObjectType makeFromInternalTypeName(String internalTypeName) {
        assert (internalTypeName != null) && !internalTypeName.endsWith(";") : "ObjectTypeMaker.makeFromInternalTypeName(internalTypeName) : invalid internalTypeName";

        ObjectType ot = OBJECT_TYPE_CACHE.get(internalTypeName);

        if (ot == null) {
            ot = internalTypeNameToObjectTypeCache.get(internalTypeName);
        }

        if (ot == null) {
            // Search class file with loader, first
            ot = loadFromLoader(internalTypeName);

            if (ot == null) {
                // File not found with the loader -> Try to load class with system class loader
                ot = loadFromClassLoader(internalTypeName);
            }

            if (ot == null) {
                // File not found with the system class loader -> Create type just from 'internalTypeName'
                ot = create(internalTypeName);
            }
        }

        return ot;
    }

    private ObjectType loadFromLoader(String internalTypeName) {
        try {
            ObjectType ot = internalTypeNameToObjectTypeCache.get(internalTypeName);

            if ((ot == null) && loader.canLoad(internalTypeName)) {
                String outerTypeName = getOuterTypeName(internalTypeName);

                if (outerTypeName == null) {
                    int lastSlash = internalTypeName.lastIndexOf('/');
                    String qualifiedName = internalTypeName.replace('/', '.');
                    String name = qualifiedName.substring(lastSlash + 1);

                    ot = new ObjectType(internalTypeName, qualifiedName, name);
                } else {
                    ObjectType outerOT = loadFromLoader(outerTypeName);
                    int index;

                    assert outerOT != null;

                    if (internalTypeName.length() > outerTypeName.length() + 1) {
                        index = outerTypeName.length();
                    } else {
                        index = internalTypeName.lastIndexOf('$');
                    }

                    String innerName = internalTypeName.substring(index + 1);

                    if (Character.isDigit(innerName.charAt(0))) {
                        ot = new InnerObjectType(internalTypeName, null, extractLocalClassName(innerName), outerOT);
                    } else {
                        String qualifiedName = outerOT.getQualifiedName() + '.' + innerName;
                        ot = new InnerObjectType(internalTypeName, qualifiedName, innerName, outerOT);
                    }
                }

                internalTypeNameToObjectTypeCache.put(internalTypeName, ot);
            }

            return ot;
        } catch (Exception ignore) {
            // Class file not found by the system class loader, or invalid class file
            return null;
        }
    }

    private ObjectType loadFromClassLoader(String internalTypeName) {
        ObjectType ot = internalTypeNameToObjectTypeCache.get(internalTypeName);

        if (ot == null) {
            try {
                Class clazz = getClass().getClassLoader().loadClass(internalTypeName.replace('/', '.'));
                String qualifiedName = clazz.getCanonicalName();
                String name = clazz.getSimpleName();

                if (clazz.isMemberClass()) {
                    String outerInternalTypeName;

                    if (name.isEmpty()) {
                        // Anonymous type
                        outerInternalTypeName = internalTypeName.substring(0, internalTypeName.lastIndexOf('$'));
                    } else {
                        // Inner type
                        outerInternalTypeName = internalTypeName.substring(0, internalTypeName.length()-name.length()-1);
                    }

                    ObjectType outerSot = loadFromClassLoader(outerInternalTypeName);

                    ot = new InnerObjectType(internalTypeName, qualifiedName, name, outerSot);
                } else {
                    ot = new ObjectType(internalTypeName, qualifiedName, name);
                }
            } catch (ClassNotFoundException ignore) {
            }

            internalTypeNameToObjectTypeCache.put(internalTypeName, ot);
        }

        return ot;
    }

    private ObjectType create(String internalTypeName) {
        int lastSlash = internalTypeName.lastIndexOf('/');
        int lastDollar = internalTypeName.lastIndexOf('$');
        ObjectType ot;

        if (lastSlash < lastDollar) {
            String outerTypeName = internalTypeName.substring(0, lastDollar);
            ObjectType outerSot = create(outerTypeName);
            String innerName = internalTypeName.substring(outerTypeName.length() + 1);

            if (innerName.isEmpty()) {
                String qualifiedName = internalTypeName.replace('/', '.');
                String name = qualifiedName.substring(lastSlash + 1);
                ot = new ObjectType(internalTypeName, qualifiedName, name);
            } else if (Character.isDigit(innerName.charAt(0))) {
                ot = new InnerObjectType(internalTypeName, null, extractLocalClassName(innerName), outerSot);
            } else {
                String qualifiedName = outerSot.getQualifiedName() + '.' + innerName;
                ot = new InnerObjectType(internalTypeName, qualifiedName, innerName, outerSot);
            }
        } else {
            String qualifiedName = internalTypeName.replace('/', '.');
            String name = qualifiedName.substring(lastSlash + 1);
            ot = new ObjectType(internalTypeName, qualifiedName, name);
        }

        internalTypeNameToObjectTypeCache.put(internalTypeName, ot);

        return ot;
    }

    public boolean isAssignable(ObjectType parent, ObjectType child) {
        if (parent == TYPE_UNDEFINED_OBJECT) {
            return true;
        } else if (parent.getDimension() > 0) {
            return (parent.getDimension() == child.getDimension()) && parent.getInternalName().equals(child.getInternalName());
        } else {
            String parentInternalName = parent.getInternalName();
            String childInternalName = child.getInternalName();

            if (parentInternalName.equals(childInternalName) || parentInternalName.equals("java/lang/Object"))
                return true;

            return recursiveIsAssignable(parentInternalName, childInternalName);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean recursiveIsAssignable(String parentInternalName, String childInternalName) {
        if (childInternalName.equals("java/lang/Object"))
            return false;

        String[] superClassAndInterfaceNames = hierarchy.get(childInternalName);

        if (superClassAndInterfaceNames == null) {
            try {
                if (loader.canLoad(childInternalName)) {
                    loadFromLoader(childInternalName);
                    superClassAndInterfaceNames = hierarchy.get(childInternalName);
                } else {
                    Class childClazz = getClass().getClassLoader().loadClass(childInternalName.replace('/', '.'));
                    Class parentClazz = getClass().getClassLoader().loadClass(parentInternalName.replace('/', '.'));
                    return parentClazz.isAssignableFrom(childClazz);
                }
            } catch (Exception ignore) {
                return false;
            }
        }

        if (superClassAndInterfaceNames != null) {
            for (String name : superClassAndInterfaceNames) {
                if (parentInternalName.equals(name))
                    return true;
            }

            for (String name : superClassAndInterfaceNames) {
                if (recursiveIsAssignable(parentInternalName, name))
                    return true;
            }
        }

        return false;
    }

    private String getOuterTypeName(String internalTypeName) throws Exception {
        return loadTypeAndApplyFunction(internalTypeName, null, searchOuterTypeName);
    }

    private Object[] constants = new Object[1000];

    @FunctionalInterface
    private interface Function {
        String apply(ClassFileReader reader, String typeName, String innerTypeName) throws Exception;
    }

    private Function checkInnerTypeName = (reader, typeName, innerTypeName) -> {
        int count = reader.readUnsignedShort();

        for(int i=0; i < count; i++) {
            int innerTypeIndex = reader.readUnsignedShort();

            // Skip 'outerTypeIndex', 'innerNameIndex' & innerAccessFlags'
            reader.skip(3 * 2);

            ConstantClass cc = (ConstantClass)constants[innerTypeIndex];
            String innerInternalTypeName = (String)constants[cc.getNameIndex()];

            if (innerTypeName.equals(innerInternalTypeName)) {
                return typeName;
            }
        }

        return null;
    };

    private Function searchOuterTypeName = (reader, typeName, innerTypeName) -> {
        int count = reader.readUnsignedShort();

        for(int i=0; i < count; i++) {
            int innerTypeIndex = reader.readUnsignedShort();
            int outerTypeIndex = reader.readUnsignedShort();

            // Skip 'innerNameIndex' & innerAccessFlags'
            reader.skip(2 * 2);

            ConstantClass cc = (ConstantClass)constants[innerTypeIndex];
            innerTypeName = (String)constants[cc.getNameIndex()];

            if (innerTypeName.equals(typeName)) {
                if (outerTypeIndex == 0) {
                    // Synthetic inner class -> Search outer class
                    int lastDollar = typeName.lastIndexOf('$');

                    if (lastDollar != -1) {
                        String outerTypeName = typeName.substring(0, lastDollar);
                        return loadTypeAndApplyFunction(outerTypeName, typeName, checkInnerTypeName);
                    }

                    return null;
                } else {
                    // Return 'outerTypeName'
                    cc = (ConstantClass)constants[outerTypeIndex];
                    return (String)constants[cc.getNameIndex()];
                }
            }
        }

        return null;
    };

    private String loadTypeAndApplyFunction(String typeName, String innerTypeName, Function function) throws Exception {
        byte[] data = loader.load(typeName);

        if (data == null) {
            return null;
        } else {
            ClassFileReader reader = new ClassFileReader(data);

            int magic = reader.readInt();

            if (magic != ClassFileReader.JAVA_MAGIC_NUMBER)
                throw new ClassFileFormatException("Invalid CLASS file");

            // Skip 'minorVersion', 'majorVersion'
            reader.skip(2 * 2);

            Object[] constants = loadConstants(reader);

            // Skip 'accessFlags' & 'thisClassIndex'
            reader.skip(2 * 2);

            // Load super class name
            int superClassIndex = reader.readUnsignedShort();
            String superClassName;

            if (superClassIndex == 0) {
                superClassName = null;
            } else {
                ConstantClass cc = (ConstantClass)constants[superClassIndex];
                superClassName = (String)constants[cc.getNameIndex()];
            }

            // Load interface blackListNames
            int count = reader.readUnsignedShort();
            String[] superClassAndInterfaceNames = new String[count + 1];

            superClassAndInterfaceNames[0] = superClassName;

            for (int i = 1; i <= count; i++) {
                int interfaceIndex = reader.readUnsignedShort();
                ConstantClass cc = (ConstantClass)constants[interfaceIndex];
                superClassAndInterfaceNames[i] = (String)constants[cc.getNameIndex()];
            }

            hierarchy.put(typeName, superClassAndInterfaceNames);

            // Skip fields
            count = reader.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                // skip 'accessFlags', 'nameIndex', 'signatureIndex'
                reader.skip(3 * 2);
                skipAttributes(reader);
            }

            // Skip methods
            count = reader.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                // skip 'accessFlags', 'nameIndex', 'signatureIndex'
                reader.skip(3 * 2);
                skipAttributes(reader);
            }

            // Load attributes
            count = reader.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                int attributeNameIndex = reader.readUnsignedShort();
                int attributeLength = reader.readInt();

                String name = (String)constants[attributeNameIndex];

                if ("InnerClasses".equals(name)) {
                    return function.apply(reader, typeName, innerTypeName);
                } else {
                    reader.skip(attributeLength);
                }
            }

            return null;
        }
    }

    private Object[] loadConstants(ClassFileReader reader) throws Exception {
        int count = reader.readUnsignedShort();

        if (count == 0)
            return null;

        if (constants.length < count) {
            constants = new Object[count];
        }

        for (int i=1; i<count; i++) {
            int tag = reader.readByte();

            switch (tag) {
                case 1:
                    constants[i] = reader.readUTF8();
                    break;
                case 7:
                    constants[i] = new ConstantClass(reader.readUnsignedShort());
                    break;
                case 8: case 16: case 19: case 20:
                    reader.skip(2);
                    break;
                case 15:
                    reader.skip(3);
                    break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                    reader.skip(4);
                    break;
                case 5: case 6:
                    reader.skip(8);
                    i++;
                    break;
                default:
                    throw new ClassFileFormatException("Invalid constant pool entry");
            }
        }

        return constants;
    }

    private static void skipAttributes(ClassFileReader reader) {
        int count = reader.readUnsignedShort();

        for (int i = 0; i < count; i++) {
            // skip 'attributeNameIndex'
            reader.skip(2);

            int attributeLength = reader.readInt();

            reader.skip(attributeLength);
        }
    }

    private static String extractLocalClassName(String name) {
        if (Character.isDigit(name.charAt(0))) {
            int i = 0, len = name.length();

            while ((i < len) && Character.isDigit(name.charAt(i))) {
                i++;
            }

            return (i == len) ? null : name.substring(i);
        }

        return name;
    }
}
