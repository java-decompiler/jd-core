/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.type;

import org.jd.core.v1.util.StringConstants;

import java.util.Map;

public class ObjectType implements Type {
    public static final ObjectType TYPE_BOOLEAN           = new ObjectType(StringConstants.JAVA_LANG_BOOLEAN, "java.lang.Boolean", "Boolean");
    public static final ObjectType TYPE_BYTE              = new ObjectType(StringConstants.JAVA_LANG_BYTE, "java.lang.Byte", "Byte");
    public static final ObjectType TYPE_CHARACTER         = new ObjectType(StringConstants.JAVA_LANG_CHARACTER, "java.lang.Character", "Character");
    public static final ObjectType TYPE_CLASS             = new ObjectType(StringConstants.JAVA_LANG_CLASS, "java.lang.Class", "Class");
    public static final ObjectType TYPE_CLASS_WILDCARD    = TYPE_CLASS.createType(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
    public static final ObjectType TYPE_DOUBLE            = new ObjectType(StringConstants.JAVA_LANG_DOUBLE, "java.lang.Double", "Double");
    public static final ObjectType TYPE_EXCEPTION         = new ObjectType(StringConstants.JAVA_LANG_EXCEPTION, "java.lang.Exception", "Exception");
    public static final ObjectType TYPE_FLOAT             = new ObjectType(StringConstants.JAVA_LANG_FLOAT, "java.lang.Float", "Float");
    public static final ObjectType TYPE_INTEGER           = new ObjectType(StringConstants.JAVA_LANG_INTEGER, "java.lang.Integer", "Integer");
    public static final ObjectType TYPE_ITERABLE          = new ObjectType(StringConstants.JAVA_LANG_ITERABLE, "java.lang.Iterable", "Iterable");
    public static final ObjectType TYPE_LONG              = new ObjectType(StringConstants.JAVA_LANG_LONG, "java.lang.Long", "Long");
    public static final ObjectType TYPE_MATH              = new ObjectType(StringConstants.JAVA_LANG_MATH, "java.lang.Math", "Math");
    public static final ObjectType TYPE_OBJECT            = new ObjectType(StringConstants.JAVA_LANG_OBJECT, "java.lang.Object", "Object");
    public static final ObjectType TYPE_RUNTIME_EXCEPTION = new ObjectType(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, "java.lang.RuntimeException", "RuntimeException");
    public static final ObjectType TYPE_SHORT             = new ObjectType(StringConstants.JAVA_LANG_SHORT, "java.lang.Short", "Short");
    public static final ObjectType TYPE_STRING            = new ObjectType(StringConstants.JAVA_LANG_STRING, "java.lang.String", "String");
    public static final ObjectType TYPE_STRING_BUFFER     = new ObjectType(StringConstants.JAVA_LANG_STRING_BUFFER, "java.lang.StringBuffer", "StringBuffer");
    public static final ObjectType TYPE_STRING_BUILDER    = new ObjectType(StringConstants.JAVA_LANG_STRING_BUILDER, "java.lang.StringBuilder", "StringBuilder");
    public static final ObjectType TYPE_SYSTEM            = new ObjectType(StringConstants.JAVA_LANG_SYSTEM, "java.lang.System", "System");
    public static final ObjectType TYPE_THREAD            = new ObjectType(StringConstants.JAVA_LANG_THREAD, "java.lang.Thread", "Thread");
    public static final ObjectType TYPE_THROWABLE         = new ObjectType(StringConstants.JAVA_LANG_THROWABLE, "java.lang.Throwable", "Throwable");

    public static final ObjectType TYPE_PRIMITIVE_BOOLEAN = new ObjectType("Z");
    public static final ObjectType TYPE_PRIMITIVE_BYTE    = new ObjectType("B");
    public static final ObjectType TYPE_PRIMITIVE_CHAR    = new ObjectType("C");
    public static final ObjectType TYPE_PRIMITIVE_DOUBLE  = new ObjectType("D");
    public static final ObjectType TYPE_PRIMITIVE_FLOAT   = new ObjectType("F");
    public static final ObjectType TYPE_PRIMITIVE_INT     = new ObjectType("I");
    public static final ObjectType TYPE_PRIMITIVE_LONG    = new ObjectType("J");
    public static final ObjectType TYPE_PRIMITIVE_SHORT   = new ObjectType("S");
    public static final ObjectType TYPE_PRIMITIVE_VOID    = new ObjectType("V");

    public static final ObjectType TYPE_UNDEFINED_OBJECT = new ObjectType(StringConstants.JAVA_LANG_OBJECT, "java.lang.Object", "Object") {
        @Override
        public String toString() { return "UndefinedObjectType"; }
    };

    protected String internalName;
    protected String qualifiedName;
    protected String name;

    protected BaseTypeArgument typeArguments;
    protected int dimension;
    protected String descriptor;

    public ObjectType(String internalName, String qualifiedName, String name) {
        this(internalName, qualifiedName, name, null, 0);
    }

    public ObjectType(String internalName, String qualifiedName, String name, int dimension) {
        this(internalName, qualifiedName, name, null, dimension);
    }

    public ObjectType(String internalName, String qualifiedName, String name, BaseTypeArgument typeArguments) {
        this(internalName, qualifiedName, name, typeArguments, 0);
    }

    public ObjectType(String internalName, String qualifiedName, String name, BaseTypeArgument typeArguments, int dimension) {
        this.internalName = internalName;
        this.qualifiedName = qualifiedName;
        this.name = name;
        this.typeArguments = typeArguments;
        this.dimension = dimension;
        this.descriptor = createDescriptor("L" + internalName + ';', dimension);

        if (internalName == null || internalName.endsWith(";")) {
            throw new IllegalArgumentException("internal name is null or ends with ;");
        }
    }

    public ObjectType(String primitiveDescriptor) {
        this(primitiveDescriptor, 0);
    }

    public ObjectType(String primitiveDescriptor, int dimension) {
        this.internalName = primitiveDescriptor;
        this.qualifiedName = this.name = PrimitiveType.getPrimitiveType(primitiveDescriptor.charAt(0)).getName();
        this.dimension = dimension;
        this.descriptor = createDescriptor(primitiveDescriptor, dimension);
    }

    protected static String createDescriptor(String descriptor, int dimension) {
        switch (dimension) {
            case 0:
                return descriptor;
            case 1:
                return "[" + descriptor;
            case 2:
                return "[[" + descriptor;
            default:
                return new String(new char[dimension]).replace('\0', '[') + descriptor;
        }
    }

    public ObjectType(ObjectType ot) {
        this.internalName = ot.getInternalName();
        this.qualifiedName = ot.getQualifiedName();
        this.name = ot.getName();
        this.typeArguments = ot.getTypeArguments();
        this.dimension = ot.getDimension();
        this.descriptor = ot.getDescriptor();
    }

    @Override
    public String getInternalName() {
        return internalName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public String getName() {
        return name;
    }

    public BaseTypeArgument getTypeArguments() {
        return typeArguments;
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public Type createType(int dimension) {
        if (dimension < 0) {
            throw new IllegalArgumentException("ObjectType.createType(dim) : create type with negative dimension");
        }

        if (this.dimension == dimension) {
            return this;
        }
        if (descriptor.charAt(descriptor.length()-1) == ';') {
            // Object type or array of object types
            return new ObjectType(internalName, qualifiedName, name, typeArguments, dimension);
        }
        // Array of primitive types
        if (dimension == 0) {
            return PrimitiveType.getPrimitiveType(descriptor.charAt(this.dimension));
        }
        return new ObjectType(internalName, dimension);
    }

    public ObjectType createType(BaseTypeArgument typeArguments) {
        if (this.typeArguments == typeArguments) {
            return this;
        }
        return new ObjectType(internalName, qualifiedName, name, typeArguments, dimension);
    }

    public boolean rawEquals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectType that = (ObjectType) o;

        return dimension == that.dimension && internalName.equals(that.internalName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectType that = (ObjectType) o;

        if (dimension != that.dimension || !internalName.equals(that.internalName)) {
            return false;
        }

        if (StringConstants.JAVA_LANG_CLASS.equals(internalName)) {
            boolean wildcard1 = typeArguments == null || typeArguments.getClass() == WildcardTypeArgument.class;
            boolean wildcard2 = that.typeArguments == null || that.typeArguments.getClass() == WildcardTypeArgument.class;

            if (wildcard1 && wildcard2) {
                return true;
            }
        }

        return typeArguments != null ? typeArguments.equals(that.typeArguments) : that.typeArguments == null;
    }

    @Override
    public int hashCode() {
        int result = 735485092 + internalName.hashCode();
        result = 31 * result + (typeArguments != null ? typeArguments.hashCode() : 0);
        return 31 * result + dimension;
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(TypeArgumentVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean isTypeArgumentAssignableFrom(Map<String, BaseType> typeBounds, BaseTypeArgument typeArgument) {
        if (typeArgument.getClass() == ObjectType.class || typeArgument.getClass() == InnerObjectType.class) {
            ObjectType ot = (ObjectType)typeArgument;

            if (dimension != ot.getDimension() || !internalName.equals(ot.getInternalName())) {
                return false;
            }

            if (ot.getTypeArguments() == null) {
                return typeArguments == null;
            }
            return typeArguments != null && typeArguments.isTypeArgumentAssignableFrom(typeBounds, ot.getTypeArguments());
        }

        if (typeArgument.getClass() == GenericType.class) {
            GenericType gt = (GenericType)typeArgument;
            BaseType bt = typeBounds.get(gt.getName());

            if (bt != null) {
                for (Type type : bt) {
                    if (dimension == type.getDimension() && (type.getClass() == ObjectType.class || type.getClass() == InnerObjectType.class)) {
                        ObjectType ot = (ObjectType) type;

                        if (internalName.equals(ot.getInternalName())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    protected boolean isTypeArgumentAssignableFrom(Map<String, BaseType> typeBounds, ObjectType objectType) {
        if (dimension != objectType.getDimension() || !internalName.equals(objectType.getInternalName())) {
            return false;
        }

        if (objectType.getTypeArguments() == null) {
            return typeArguments == null;
        }
        return typeArguments != null && typeArguments.isTypeArgumentAssignableFrom(typeBounds, objectType.getTypeArguments());
    }

    @Override
    public boolean isObjectType() {
        return true;
    }

    @Override
    public boolean isObjectTypeArgument() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ObjectType{");

        sb.append(internalName);

        if (typeArguments != null) {
            sb.append('<').append(typeArguments).append('>');
        }

        if (dimension > 0) {
            sb.append(", dimension=").append(dimension);
        }

        return sb.append('}').toString();
    }
}
