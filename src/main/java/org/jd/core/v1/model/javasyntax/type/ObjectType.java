/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

public class ObjectType implements Type {
    public static final ObjectType TYPE_BOOLEAN          = new ObjectType("java/lang/Boolean", "java.lang.Boolean", "Boolean");
    public static final ObjectType TYPE_BYTE             = new ObjectType("java/lang/Byte", "java.lang.Byte", "Byte");
    public static final ObjectType TYPE_CHARACTER        = new ObjectType("java/lang/Character", "java.lang.Character", "Character");
    public static final ObjectType TYPE_CLASS            = new ObjectType("java/lang/Class", "java.lang.Class", "Class");
    public static final ObjectType TYPE_DOUBLE           = new ObjectType("java/lang/Double", "java.lang.Double", "Double");
    public static final ObjectType TYPE_FLOAT            = new ObjectType("java/lang/Float", "java.lang.Float", "Float");
    public static final ObjectType TYPE_INTEGER          = new ObjectType("java/lang/Integer", "java.lang.Integer", "Integer");
    public static final ObjectType TYPE_MATH             = new ObjectType("java/lang/Math", "java.lang.Math", "Math");
    public static final ObjectType TYPE_OBJECT           = new ObjectType("java/lang/Object", "java.lang.Object", "Object");
    public static final ObjectType TYPE_SHORT            = new ObjectType("java/lang/Short", "java.lang.Short", "Short");
    public static final ObjectType TYPE_STRING           = new ObjectType("java/lang/String", "java.lang.String", "String");
    public static final ObjectType TYPE_THROWABLE        = new ObjectType("java/lang/Throwable", "java.lang.Throwable", "Throwable");

    public static final ObjectType TYPE_UNDEFINED_OBJECT = new ObjectType("java/lang/Object", "java.lang.Object", "Object") {
        @Override public String toString() { return "UndefinedObjectType"; }
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

        assert (internalName != null) && !internalName.endsWith(";");

        switch (dimension) {
            case 0:
                this.descriptor = "L" + internalName + ';';
                break;
            case 1:
                this.descriptor = "[L" + internalName + ';';
                break;
            case 2:
                this.descriptor = "[[L" + internalName + ';';
                break;
            default:
                this.descriptor = new String(new char[dimension]).replaceAll("\0", "[") + 'L' + internalName + ';';
                break;
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

    public String getInternalName() {
        return internalName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getName() {
        return name;
    }

    public BaseTypeArgument getTypeArguments() {
        return typeArguments;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public Type createType(int dimension) {
        assert dimension >= 0;
        if (this.dimension == dimension)
            return this;
        else
            return new ObjectType(internalName, qualifiedName, name, typeArguments, dimension);
    }

    public ObjectType createType(BaseTypeArgument typeArguments) {
        return new ObjectType(internalName, qualifiedName, name, typeArguments, dimension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectType that = (ObjectType) o;

        if (dimension != that.dimension) return false;
        if (!internalName.equals(that.internalName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = internalName.hashCode();
        result = 31 * result + dimension;
        return result;
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean isObject() {
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
