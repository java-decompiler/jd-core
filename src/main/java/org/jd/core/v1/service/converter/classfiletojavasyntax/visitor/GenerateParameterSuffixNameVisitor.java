/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.type.AbstractNopTypeArgumentVisitor;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;

import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_BOOLEAN;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_BYTE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_CHAR;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_DOUBLE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_FLOAT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_INT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_LONG;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_SHORT;

public class GenerateParameterSuffixNameVisitor extends AbstractNopTypeArgumentVisitor {
    private String suffix;

    public String getSuffix() {
        return suffix;
    }

    @Override
    public void visit(PrimitiveType type) {
        suffix = switch (type.getJavaPrimitiveFlags()) {
        case FLAG_BYTE -> "Byte";
        case FLAG_CHAR -> "Char";
        case FLAG_DOUBLE -> "Double";
        case FLAG_FLOAT -> "Float";
        case FLAG_INT -> "Int";
        case FLAG_LONG -> "Long";
        case FLAG_SHORT -> "Short";
        case FLAG_BOOLEAN -> "Boolean";
        default -> throw new IllegalStateException();
        };
    }

    @Override
    public void visit(ObjectType type) {
        suffix = type.getName();
    }

    @Override
    public void visit(InnerObjectType type) {
        suffix = type.getName();
    }

    @Override
    public void visit(GenericType type) {
        suffix = type.getName();
    }
}
