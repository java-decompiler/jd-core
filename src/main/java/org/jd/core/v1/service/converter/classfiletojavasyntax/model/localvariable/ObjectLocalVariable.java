/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable;

import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ObjectTypeMaker;

import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_UNDEFINED_OBJECT;

public class ObjectLocalVariable extends AbstractLocalVariable {
    protected ObjectTypeMaker objectTypeMaker;
    protected ObjectType fromType;
    protected ObjectType toType;
    protected Type       arrayType;

    public ObjectLocalVariable(ObjectTypeMaker objectTypeMaker, int index, int offset, ObjectType type, String name) {
        super(index, offset, name, type.getDimension());
        this.objectTypeMaker = objectTypeMaker;

        if (dimension == 0) {
            this.fromType = this.toType = type;
        } else {
            this.arrayType = type;
        }
    }

    public ObjectLocalVariable(ObjectTypeMaker objectTypeMaker, int index, int offset, Type type, String name) {
        super(index, offset, name, type.getDimension());
        this.objectTypeMaker = objectTypeMaker;

        if (dimension == 0) {
            this.fromType = this.toType = (ObjectType)type;
        } else {
            this.arrayType = type;
        }
    }

    public ObjectLocalVariable(ObjectTypeMaker objectTypeMaker, int index, int offset, ObjectType type, String name, boolean declared) {
        super(index, offset, name, type.getDimension(), declared);
        this.objectTypeMaker = objectTypeMaker;

        if (dimension == 0) {
            this.fromType = this.toType = type;
        } else {
            this.arrayType = type;
        }
    }

    public ObjectLocalVariable(ObjectTypeMaker objectTypeMaker, int index, int offset, Type type) {
        super(index, offset, type.getDimension());
        this.objectTypeMaker = objectTypeMaker;

        if (dimension == 0) {
            this.fromType = this.toType = (ObjectType)type;
        } else {
            this.arrayType = type;
        }
    }

    public ObjectLocalVariable(ObjectTypeMaker objectTypeMaker, int index, int offset, ObjectLocalVariable objectLocalVariable) {
        super(index, offset, objectLocalVariable.getDimension());
        this.objectTypeMaker = objectTypeMaker;
        this.toType = objectLocalVariable.toType;
        this.arrayType = objectLocalVariable.arrayType;
    }

    @Override
    public Type getType() {
        return (dimension == 0) ? toType : arrayType;
    }

    @Override
    public boolean isAssignable(AbstractLocalVariable other) {
        if (other.getClass() == ObjectLocalVariable.class) {
            if (toType == TYPE_UNDEFINED_OBJECT) {
                return true;
            } else if (dimension == other.getDimension()) {
                ObjectLocalVariable olv = (ObjectLocalVariable) other;

                if (dimension == 0) {
                    if (olv.fromType == null) {
                        return isAssignable(olv.toType);
                    } else {
                        return isAssignable(olv.fromType);
                    }
                } else {
                    return arrayType.equals(olv.arrayType);
                }
            }
        }

        return false;
    }

    @Override
    public boolean isAssignable(Type otherType) {
        if (otherType == TYPE_UNDEFINED_OBJECT) {
            return true;
        }

        if (toType == TYPE_UNDEFINED_OBJECT) {
            return true;
        } else if (dimension == otherType.getDimension()) {
            if (dimension == 0) {
                if (otherType.isObject()) {
                    return objectTypeMaker.isAssignable(toType, (ObjectType)otherType);
                }
            } else {
                return arrayType.equals(otherType);
            }
        }

        return false;
    }

    @Override
    public void leftReduce(AbstractLocalVariable other) {
        if (other.getClass() == ObjectLocalVariable.class) {
            Type otherToType = ((ObjectLocalVariable) other).toType;

            if ((otherToType != null) && (otherToType != TYPE_UNDEFINED_OBJECT)) {
                Type otherType = ((ObjectLocalVariable) other).fromType;

                if (otherType == null) {
                    otherType = otherToType;
                }

                if (toType == TYPE_UNDEFINED_OBJECT) {
                    leftReduce(otherType);
                } else if ((dimension == 0) && (dimension == other.getDimension())) {
                    leftReduce(otherType);
                }
            }
        }
    }

    @Override
    public void leftReduce(Type otherType) {
        if (otherType != TYPE_UNDEFINED_OBJECT) {
            if (toType == TYPE_UNDEFINED_OBJECT) {
                if (otherType.getDimension() == 0) {
                    if (otherType.isObject()) {
                        fromType = toType = (ObjectType)otherType;
                    }
                } else {
                    dimension = otherType.getDimension();
                    arrayType = otherType;
                }
            } else if ((dimension == 0) && (otherType.getDimension() == 0) && otherType.isObject()) {
                ObjectType otherObjectType = (ObjectType) otherType;

                if (!toType.getInternalName().equals(otherObjectType.getInternalName()) && objectTypeMaker.isAssignable(otherObjectType, toType)) {
                    if (toType.getTypeArguments() == null) {
                        toType = otherObjectType;
                    } else if (otherObjectType.getTypeArguments() == null) {
                        toType = otherObjectType.createType(toType.getTypeArguments());
                    } else if (toType.getTypeArguments().equals(otherObjectType.getTypeArguments())) {
                        toType = otherObjectType;
                    }
                }
            }
        }
    }

    @Override
    public void rightReduce(AbstractLocalVariable other) {
        if ((dimension == 0) && (other.getClass() == ObjectLocalVariable.class)) {
            ObjectLocalVariable olv = (ObjectLocalVariable)other;

            if (toType == TYPE_UNDEFINED_OBJECT) {
                dimension = olv.dimension;
                fromType = olv.fromType;
                toType = olv.toType;
            } else {
                assert (olv.getDimension() == 0);

                rightReduce(olv.toType);
            }
        }
    }

    @Override
    public void rightReduce(Type otherType) {
        if (dimension == 0) {
            if (otherType.isObject()) {
                if ((otherType != ObjectType.TYPE_OBJECT) && (otherType != TYPE_UNDEFINED_OBJECT)) {
                    if (toType == TYPE_UNDEFINED_OBJECT) {
                        dimension = otherType.getDimension();

                        if (dimension == 0) {
                            fromType = null;
                            toType = (ObjectType) otherType;
                        } else {
                            arrayType = otherType;
                        }
                    } else if (!otherType.equals(ObjectType.TYPE_OBJECT)) {
                        assert (otherType.getDimension() == 0) && otherType.isObject();

                        ObjectType otherObjectType = (ObjectType) otherType;

                        if (fromType == null) {
                            if (toType.getTypeArguments() == null) {
                                fromType = otherObjectType;
                            } else if (otherObjectType.getTypeArguments() == null) {
                                fromType = otherObjectType.createType(toType.getTypeArguments());
                            } else if (toType.getTypeArguments().equals(otherObjectType.getTypeArguments())) {
                                fromType = otherObjectType;
                            }
                        } else if (!fromType.getInternalName().equals(otherObjectType.getInternalName()) && objectTypeMaker.isAssignable(fromType, otherObjectType)) {
                            if (fromType.getTypeArguments() == null) {
                                fromType = otherObjectType;
                            } else if (otherObjectType.getTypeArguments() == null) {
                                fromType = otherObjectType.createType(fromType.getTypeArguments());
                            } else if (fromType.getTypeArguments().equals(otherObjectType.getTypeArguments())) {
                                fromType = otherObjectType;
                            }
                        }
                    }
                }
            } else if (otherType.isPrimitive()) {
                if (toType == TYPE_UNDEFINED_OBJECT) {
                    dimension = otherType.getDimension();

                    if (dimension > 0) {
                        arrayType = otherType;
                        dimension = otherType.getDimension();
                    }
                }
            }
        }
    }

    @Override
    public void accept(LocalVariableVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ObjectLocalVariable{");

        if (dimension == 0) {
            if (fromType == null) {
                sb.append('?');
            } else if (fromType.getQualifiedName() == null) {
                sb.append(fromType.getName());
            } else {
                sb.append(fromType.getQualifiedName());
            }

            if (!toType.equals(fromType)) {
                sb.append(" ... ");
                sb.append(toType.getQualifiedName()==null ? toType.getName() : toType.getQualifiedName());
            }
        } else if (arrayType.isObject()){
            sb.append(((ObjectType)arrayType).getInternalName());
            sb.append(new String(new char[dimension]).replaceAll("\0", "[]"));
        } else {
            sb.append(((PrimitiveType)arrayType).getName());
            sb.append(new String(new char[dimension]).replaceAll("\0", "[]"));
        }

        sb.append(' ').append(name).append(", index=").append(index);

        if (next != null) {
            sb.append(", next=").append(next);
        }

        return sb.append("}").toString();
    }
}
