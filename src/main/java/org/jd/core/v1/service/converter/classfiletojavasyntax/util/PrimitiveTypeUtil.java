/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.type.PrimitiveType;

import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.*;

public class PrimitiveTypeUtil {
    protected static final PrimitiveType[] descriptorToPrimitiveType = new PrimitiveType['Z' - 'B' + 1];

    static {
        descriptorToPrimitiveType['B' - 'B'] = TYPE_BYTE;
        descriptorToPrimitiveType['C' - 'B'] = TYPE_CHAR;
        descriptorToPrimitiveType['D' - 'B'] = TYPE_DOUBLE;
        descriptorToPrimitiveType['F' - 'B'] = TYPE_FLOAT;
        descriptorToPrimitiveType['I' - 'B'] = TYPE_INT;
        descriptorToPrimitiveType['J' - 'B'] = TYPE_LONG;
        descriptorToPrimitiveType['S' - 'B'] = TYPE_SHORT;
        descriptorToPrimitiveType['Z' - 'B'] = TYPE_BOOLEAN;
    }

    public static PrimitiveType getPrimitiveTypeFromDescriptor(String descriptor) {
        int dimension = 0;

        while (descriptor.charAt(dimension) == '[') {
            dimension++;
        }

        return descriptorToPrimitiveType[descriptor.charAt(dimension) - 'B'].createType(dimension);
    }

    public static PrimitiveType getPrimitiveTypeFromValue(int value) {
        if (value >= 0) {
            if (value <= 1)
                return MAYBE_BOOLEAN_TYPE;
            if (value <= Byte.MAX_VALUE)
                return MAYBE_BYTE_TYPE;
            if (value <= Short.MAX_VALUE)
                return MAYBE_SHORT_TYPE;
            if (value <= Character.MAX_VALUE)
                return MAYBE_CHAR_TYPE;
        } else {
            if (value >= Byte.MIN_VALUE)
                return MAYBE_NEGATIVE_BYTE_TYPE;
            if (value >= Short.MIN_VALUE)
                return MAYBE_NEGATIVE_SHORT_TYPE;
        }
        return MAYBE_INT_TYPE;
    }

    public static int getStandardPrimitiveTypeFlags(int flags) {
        if ((flags & FLAG_BOOLEAN) != 0)
            return FLAG_BOOLEAN;
        else if ((flags & FLAG_BYTE) != 0)
            return FLAG_BYTE;
        else if ((flags & FLAG_CHAR) != 0)
            return FLAG_CHAR;
        else if ((flags & FLAG_SHORT) != 0)
            return FLAG_SHORT;

        return flags;
    }

    public static PrimitiveType getPrimitiveType(int flags, int dimension) {
        PrimitiveType type = getPrimitiveType(flags);

        if (type == null) {
            type = TYPE_INT;
        }

        return type.createType(dimension);
    }


    public static PrimitiveType getCommonPrimitiveType(PrimitiveType pt1, PrimitiveType pt2) {
        assert (pt1.getDimension() == 0) && (pt2.getDimension() == 0);
        return getPrimitiveType(pt1.getFlags() & pt2.getFlags());
    }

    protected static PrimitiveType getPrimitiveType(int flags) {
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
            default:
                if (flags == (FLAG_CHAR|FLAG_INT))
                    return MAYBE_CHAR_TYPE;
                if (flags == (FLAG_CHAR|FLAG_SHORT|FLAG_INT))
                    return MAYBE_SHORT_TYPE;
                if (flags == (FLAG_BYTE|FLAG_CHAR|FLAG_SHORT|FLAG_INT))
                    return MAYBE_BYTE_TYPE;
                if (flags == (FLAG_BOOLEAN|FLAG_BYTE|FLAG_CHAR|FLAG_SHORT|FLAG_INT))
                    return MAYBE_BOOLEAN_TYPE;
                if (flags == (FLAG_BYTE|FLAG_SHORT|FLAG_INT))
                    return MAYBE_NEGATIVE_BYTE_TYPE;
                if (flags == (FLAG_SHORT|FLAG_INT))
                    return MAYBE_NEGATIVE_SHORT_TYPE;
                if (flags == (FLAG_BOOLEAN|FLAG_BYTE|FLAG_SHORT|FLAG_INT))
                    return MAYBE_NEGATIVE_BOOLEAN_TYPE;
                break;
        }

        return null;
    }
}