/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable;

import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.PrimitiveTypeUtil;

import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.*;

public class PrimitiveLocalVariable extends AbstractLocalVariable {
    protected int flags;

    public PrimitiveLocalVariable(int index, int offset, String descriptor, String name) {
        super(index, offset, name, 0);
        assert descriptor.charAt(0) != '[';
        this.flags = PrimitiveTypeUtil.getPrimitiveTypeFromDescriptor(descriptor).getFlags();
    }

    public PrimitiveLocalVariable(int index, int offset, PrimitiveType type, String name) {
        super(index, offset, name, 0);
        assert type.getDimension() == 0;
        this.flags = type.getFlags();
    }

    public PrimitiveLocalVariable(int index, int offset, PrimitiveType type) {
        super(index, offset, 0);
        assert type.getDimension() == 0;
        this.flags = type.getFlags();
    }

    public PrimitiveLocalVariable(int index, int offset, PrimitiveLocalVariable primitiveLocalVariable) {
        super(index, offset, 0);
        assert primitiveLocalVariable.getDimension() == 0;
        int valueFlags = primitiveLocalVariable.flags;

        if ((valueFlags & FLAG_INT) != 0) {
            this.flags = valueFlags;
        } else if ((valueFlags & FLAG_SHORT) != 0) {
            this.flags = valueFlags | FLAG_INT;
        } else if ((valueFlags & FLAG_CHAR) != 0) {
            this.flags = valueFlags | FLAG_INT | FLAG_SHORT;
        } else if ((valueFlags & FLAG_BYTE) != 0) {
            this.flags = valueFlags | FLAG_INT | FLAG_SHORT;
        } else {
            this.flags = valueFlags;
        }
    }

    @Override
    public Type getType() {
        return PrimitiveTypeUtil.getPrimitiveType(flags, dimension);
    }

    public void setPrimitiveType(PrimitiveType type) {
        assert type.getDimension() == 0;
        this.flags = type.getFlags();
    }

    @Override
    public boolean isAssignable(AbstractLocalVariable other) {
        if ((other.getDimension() == 0) && (other.getClass() == PrimitiveLocalVariable.class)) {
            if (dimension == 0) {
                return (flags & ((PrimitiveLocalVariable)other).flags) != 0;
            } else {
                return flags == ((PrimitiveLocalVariable)other).flags;
            }
        }

        return false;
    }

    @Override
    public boolean isAssignable(Type otherType) {
        if ((otherType.getDimension() == 0) && otherType.isPrimitive()) {
            return (flags & ((PrimitiveType)otherType).getRightFlags()) != 0;
        }

        return false;
    }

    @Override
    public void leftReduce(AbstractLocalVariable other) {
        assert other.getDimension() == 0;

        int otherFlags = ((PrimitiveLocalVariable)other).flags;
        int newFalgs = 0;

        if ((otherFlags & FLAG_BYTE) != 0) {
            newFalgs |= FLAG_BYTE|FLAG_SHORT|FLAG_INT;
        }
        if ((otherFlags & FLAG_CHAR) != 0) {
            newFalgs |= FLAG_CHAR|FLAG_INT;
        }
        if ((otherFlags & FLAG_SHORT) != 0) {
            newFalgs |= FLAG_SHORT|FLAG_INT;
        }
        if ((otherFlags & FLAG_INT) != 0) {
            newFalgs |= FLAG_INT;
        }

        if ((flags & newFalgs) != 0) {
            flags &= newFalgs;
        }
    }

    @Override
    public void rightReduce(AbstractLocalVariable other) {
        assert other.getDimension() == 0;

        int otherFlags = ((PrimitiveLocalVariable)other).flags;
        int newFalgs = 0;

        if ((otherFlags & FLAG_INT) != 0) {
            newFalgs |= FLAG_INT;
        }
        if ((otherFlags & FLAG_SHORT) != 0) {
            newFalgs |= FLAG_SHORT|FLAG_BYTE;
        }
        if ((otherFlags & FLAG_CHAR) != 0) {
            newFalgs |= FLAG_CHAR;
        }
        if ((otherFlags & FLAG_BYTE) != 0) {
            newFalgs |= FLAG_BYTE;
        }

        if ((flags & newFalgs) != 0) {
            flags &= newFalgs;
        }
    }

    @Override
    public void leftReduce(Type otherType) {
        this.flags &= ((PrimitiveType)otherType).getRightFlags();
        assert (otherType.getDimension() == 0) && (this.flags != 0);
    }

    public void rightReduce(Type otherType) {
        this.flags &= ((PrimitiveType)otherType).getLeftFlags();
        assert (otherType.getDimension() == 0) && (this.flags != 0) : "rightReduce : incompatible types";
    }

    @Override
    public void accept(LocalVariableVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("PrimitiveLocalVariable{");

        if ((flags & FLAG_BOOLEAN) != 0)    sb.append("boolean ");
        if ((flags & FLAG_CHAR) != 0)       sb.append("char ");
        if ((flags & FLAG_FLOAT) != 0)      sb.append("float ");
        if ((flags & FLAG_DOUBLE) != 0)     sb.append("double ");
        if ((flags & FLAG_BYTE) != 0)       sb.append("byte ");
        if ((flags & FLAG_SHORT) != 0)      sb.append("short ");
        if ((flags & FLAG_INT) != 0)        sb.append("int ");
        if ((flags & FLAG_LONG) != 0)       sb.append("long ");
        if ((flags & FLAG_VOID) != 0)       sb.append("void ");

        sb.append(name).append(", index=").append(index);

        if (next != null) {
            sb.append(", next=").append(next);
        }

        return sb.append("}").toString();
    }
}
