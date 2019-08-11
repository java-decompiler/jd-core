/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable;

import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ObjectTypeMaker;

import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_OBJECT;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_UNDEFINED_OBJECT;

public class ObjectLocalVariable extends AbstractLocalVariable {
    protected ObjectTypeMaker objectTypeMaker;
    protected Type type;

    public ObjectLocalVariable(ObjectTypeMaker objectTypeMaker, int index, int offset, Type type, String name) {
        super(index, offset, name);
        this.objectTypeMaker = objectTypeMaker;
        this.type = type;
    }

    public ObjectLocalVariable(ObjectTypeMaker objectTypeMaker, int index, int offset, Type type, String name, boolean declared) {
        this(objectTypeMaker, index, offset, type, name);
        this.declared = declared;
    }

    public ObjectLocalVariable(ObjectTypeMaker objectTypeMaker, int index, int offset, ObjectLocalVariable objectLocalVariable) {
        super(index, offset, null);
        this.objectTypeMaker = objectTypeMaker;
        this.type = objectLocalVariable.type;
    }

    @Override
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        if (!this.type.equals(type)) {
            this.type = type;
            fireChangeEvent();
        }
    }

    @Override
    public int getDimension() {
        return type.getDimension();
    }

    @Override
    public void accept(LocalVariableVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ObjectLocalVariable{");
        sb.append(type.getName());

        if (type.getDimension() > 0) {
            sb.append(new String(new char[type.getDimension()]).replaceAll("\0", "[]"));
        }

        sb.append(' ').append(name).append(", index=").append(index);

        if (next != null) {
            sb.append(", next=").append(next);
        }

        return sb.append("}").toString();
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        if (!type.isPrimitive()) {
            if ((type == TYPE_UNDEFINED_OBJECT) || (this.type == TYPE_UNDEFINED_OBJECT) || (this.type == TYPE_OBJECT) || this.type.equals(type)) {
                return true;
            } else if ((this.type.getDimension() == 0) && (type.getDimension() == 0) && this.type.isObject() && type.isObject()) {
                return objectTypeMaker.isAssignable((ObjectType) this.type, (ObjectType) type);
            }
        }

        return false;
    }

    public void typeOnRight(Type type) {
        if (type != TYPE_UNDEFINED_OBJECT) {
            if (this.type == TYPE_UNDEFINED_OBJECT) {
                this.type = type;
                fireChangeEvent();
            } else if ((this.type.getDimension() == 0) && (type.getDimension() == 0)) {
                assert !this.type.isPrimitive() && !type.isPrimitive() : "ObjectLocalVariable.typeOnRight(type) : unexpected type";

                if (this.type.isObject() && type.isObject()) {
                    ObjectType thisObjectType = (ObjectType)this.type;
                    ObjectType otherObjectType = (ObjectType)type;

                    if (thisObjectType.getInternalName().equals(otherObjectType.getInternalName())) {
                        if ((thisObjectType.getTypeArguments() == null) && (otherObjectType.getTypeArguments() != null)) {
                            // Keep type, update type arguments
                            this.type = otherObjectType;
                            fireChangeEvent();
                        }
                    } else if (objectTypeMaker.isAssignable(thisObjectType, otherObjectType)) {
                        // Assignable types
                        if ((thisObjectType.getTypeArguments() == null) && (otherObjectType.getTypeArguments() != null)) {
                            // Keep type, update type arguments
                            this.type = thisObjectType.createType(otherObjectType.getTypeArguments());
                            fireChangeEvent();
                        }
                    }
                }
            }
        }
    }

    public void typeOnLeft(Type type) {
        if (type != TYPE_UNDEFINED_OBJECT) {
            if (this.type == TYPE_UNDEFINED_OBJECT) {
                this.type = type;
                fireChangeEvent();
            } else if ((this.type.getDimension() == 0) && (type.getDimension() == 0)) {
                assert !this.type.isPrimitive() && !type.isPrimitive() : "unexpected type in ObjectLocalVariable.typeOnLeft(type)";

                if (this.type.isObject() && type.isObject()) {
                    ObjectType thisObjectType = (ObjectType)this.type;
                    ObjectType otherObjectType = (ObjectType)type;

                    if (thisObjectType.getInternalName().equals(otherObjectType.getInternalName())) {
                        if ((thisObjectType.getTypeArguments() == null) && (otherObjectType.getTypeArguments() != null)) {
                            // Keep type, update type arguments
                            this.type = otherObjectType;
                            fireChangeEvent();
                        }
                    } else if (objectTypeMaker.isAssignable(otherObjectType, thisObjectType)) {
                        // Assignable types
                        if ((thisObjectType.getTypeArguments() == null) && (otherObjectType.getTypeArguments() != null)) {
                            // Keep type, update type arguments
                            this.type = thisObjectType.createType(otherObjectType.getTypeArguments());
                            fireChangeEvent();
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isAssignableFrom(AbstractLocalVariable variable) {
        return isAssignableFrom(variable.getType());
    }

    public void variableOnRight(AbstractLocalVariable variable) {
        addVariableOnRight(variable);
        typeOnRight(variable.getType());
    }

    public void variableOnLeft(AbstractLocalVariable variable) {
        addVariableOnLeft(variable);
        typeOnLeft(variable.getType());
    }
}
