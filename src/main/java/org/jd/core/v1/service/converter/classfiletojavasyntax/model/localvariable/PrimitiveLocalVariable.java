/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable;

import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.PrimitiveTypeUtil;

import java.util.Map;

import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_BOOLEAN;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_BYTE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_CHAR;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_DOUBLE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_FLOAT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_INT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_LONG;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_SHORT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_VOID;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_BOOLEAN_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_BYTE_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_CHAR_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_NEGATIVE_BOOLEAN_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_NEGATIVE_BYTE_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_NEGATIVE_SHORT_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_SHORT_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BOOLEAN;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BYTE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_CHAR;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_DOUBLE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_FLOAT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_INT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_LONG;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_SHORT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_VOID;

public class PrimitiveLocalVariable extends AbstractLocalVariable {
    private static final String NON_ZERO_DIMENSION = "Non-zero dimension";
    private int flags;

    public PrimitiveLocalVariable(int index, int offset, PrimitiveType type, String name) {
        super(index, offset, name);
        this.flags = type.getFlags();
    }

    public PrimitiveLocalVariable(int index, int offset, PrimitiveLocalVariable primitiveLocalVariable) {
        super(index, offset, null);
        int valueFlags = primitiveLocalVariable.flags;

        if ((valueFlags & FLAG_INT) != 0) {
            this.flags = valueFlags;
        } else if ((valueFlags & FLAG_SHORT) != 0) {
            this.flags = valueFlags | FLAG_INT;
        } else if ((valueFlags & FLAG_CHAR) != 0 || (valueFlags & FLAG_BYTE) != 0) {
            this.flags = valueFlags | FLAG_INT | FLAG_SHORT;
        } else {
            this.flags = valueFlags;
        }
    }

    @Override
    public Type getType() {
        switch (flags) {
            case FLAG_BOOLEAN:
                return TYPE_BOOLEAN;
            case FLAG_CHAR:
                return TYPE_CHAR;
            case FLAG_FLOAT:
                return TYPE_FLOAT;
            case FLAG_DOUBLE:
                return TYPE_DOUBLE;
            case FLAG_BYTE:
                return TYPE_BYTE;
            case FLAG_SHORT:
                return TYPE_SHORT;
            case FLAG_INT:
                return TYPE_INT;
            case FLAG_LONG:
                return TYPE_LONG;
            case FLAG_VOID:
                return TYPE_VOID;
        }

        switch (flags) {
        case FLAG_CHAR|FLAG_INT:
            return MAYBE_CHAR_TYPE;
        case FLAG_CHAR|FLAG_SHORT|FLAG_INT:
            return MAYBE_SHORT_TYPE;
        case FLAG_BYTE|FLAG_CHAR|FLAG_SHORT|FLAG_INT:
            return MAYBE_BYTE_TYPE;
        case FLAG_BOOLEAN|FLAG_BYTE|FLAG_CHAR|FLAG_SHORT|FLAG_INT:
            return MAYBE_BOOLEAN_TYPE;
        case FLAG_BYTE|FLAG_SHORT|FLAG_INT:
            return MAYBE_NEGATIVE_BYTE_TYPE;
        case FLAG_SHORT|FLAG_INT:
            return MAYBE_NEGATIVE_SHORT_TYPE;
        case FLAG_BOOLEAN|FLAG_BYTE|FLAG_SHORT|FLAG_INT:
            return MAYBE_NEGATIVE_BOOLEAN_TYPE;
        default:
            break;
        }

        return TYPE_INT;
    }

    @Override
    public int getDimension() {
        return 0;
    }

    void setType(PrimitiveType type) {
        this.flags = type.getFlags();
    }

    @Override
    public void accept(LocalVariableVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("PrimitiveLocalVariable{");

        if ((flags & FLAG_BOOLEAN) != 0) {
            sb.append("boolean ");
        }
        if ((flags & FLAG_CHAR) != 0) {
            sb.append("char ");
        }
        if ((flags & FLAG_FLOAT) != 0) {
            sb.append("float ");
        }
        if ((flags & FLAG_DOUBLE) != 0) {
            sb.append("double ");
        }
        if ((flags & FLAG_BYTE) != 0) {
            sb.append("byte ");
        }
        if ((flags & FLAG_SHORT) != 0) {
            sb.append("short ");
        }
        if ((flags & FLAG_INT) != 0) {
            sb.append("int ");
        }
        if ((flags & FLAG_LONG) != 0) {
            sb.append("long ");
        }
        if ((flags & FLAG_VOID) != 0) {
            sb.append("void ");
        }

        sb.append(name).append(", index=").append(index);

        if (next != null) {
            sb.append(", next=").append(next);
        }

        return sb.append("}").toString();
    }

    @Override
    public boolean isAssignableFrom(Map<String, BaseType> typeBounds, Type type) {
        return type.getDimension() == 0 && type.isPrimitiveType() && (flags & ((PrimitiveType)type).getRightFlags()) != 0;
    }

    @Override
    public void typeOnRight(Map<String, BaseType> typeBounds, Type type) {
        if (type.isPrimitiveType()) {
            if (type.getDimension() != 0) {
                throw new IllegalArgumentException(NON_ZERO_DIMENSION);
            }

            int f = ((PrimitiveType) type).getRightFlags();

            if ((flags & f) != 0) {
                int old = flags;

                flags &= f;

                if (old != flags) {
                    fireChangeEvent(typeBounds);
                }
            }
        }
    }

    @Override
    public void typeOnLeft(Map<String, BaseType> typeBounds, Type type) {
        if (type.isPrimitiveType()) {
            if (type.getDimension() != 0) {
                throw new IllegalArgumentException(NON_ZERO_DIMENSION);
            }

            int f = ((PrimitiveType) type).getLeftFlags();

            if ((flags & f) != 0) {
                int old = flags;

                flags &= f;

                if (old != flags) {
                    fireChangeEvent(typeBounds);
                }
            }
        }
    }

    @Override
    public boolean isAssignableFrom(Map<String, BaseType> typeBounds, AbstractLocalVariable variable) {
        if (variable.isPrimitiveLocalVariable()) {
            int variableFlags = ((PrimitiveLocalVariable)variable).flags;
            PrimitiveType type = PrimitiveTypeUtil.getPrimitiveTypeFromFlags(variableFlags);

            if (type != null) {
                variableFlags = type.getRightFlags();
            }

            return (flags & variableFlags) != 0;
        }

        return false;
    }

    @Override
    public void variableOnRight(Map<String, BaseType> typeBounds, AbstractLocalVariable variable) {
        if (variable.getDimension() != 0) {
            throw new IllegalArgumentException(NON_ZERO_DIMENSION);
        }

        addVariableOnRight(variable);

        int old = flags;
        int variableFlags = ((PrimitiveLocalVariable)variable).flags;
        PrimitiveType type = PrimitiveTypeUtil.getPrimitiveTypeFromFlags(variableFlags);

        if (type != null) {
            variableFlags = type.getRightFlags();
        }

        flags &= variableFlags;
        if (flags == 0) {
            throw new IllegalArgumentException("PrimitiveLocalVariable.variableOnRight(var) : flags = 0 after type reduction");
        }

        if (old != flags) {
            fireChangeEvent(typeBounds);
        }
    }

    @Override
    public void variableOnLeft(Map<String, BaseType> typeBounds, AbstractLocalVariable variable) {
        if (variable.getDimension() != 0) {
            throw new IllegalArgumentException(NON_ZERO_DIMENSION);
        }

        addVariableOnLeft(variable);

        int old = flags;
        int variableFlags = ((PrimitiveLocalVariable)variable).flags;
        PrimitiveType type = PrimitiveTypeUtil.getPrimitiveTypeFromFlags(variableFlags);

        if (type != null) {
            variableFlags = type.getLeftFlags();
        }

        flags &= variableFlags;
        if (flags == 0) {
            throw new IllegalArgumentException("PrimitiveLocalVariable.variableOnLeft(var) : flags = 0 after type reduction");
        }

        if (old != flags) {
            fireChangeEvent(typeBounds);
        }
    }

    @Override
    public boolean isPrimitiveLocalVariable() { return true; }
}
