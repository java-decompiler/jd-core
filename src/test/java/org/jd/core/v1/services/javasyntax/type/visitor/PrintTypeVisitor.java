/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.services.javasyntax.type.visitor;

import org.jd.core.v1.model.javasyntax.type.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.PrimitiveTypeUtil;

import java.util.List;

import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.*;

public class PrintTypeVisitor implements TypeVisitor {
    protected StringBuilder sb = new StringBuilder();

    public void reset() {
        sb.setLength(0);
    }

    public String toString() {
        return sb.toString();
    }

    @Override
    public void visit(ArrayTypeArguments type) {
        printList(type, ", ");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(Types type) {
        printList(type, ", ");
    }

    @Override
    public void visit(DiamondTypeArgument type) {}

    @Override
    public void visit(WildcardExtendsTypeArgument type) {
        sb.append("? extends ");
        type.getType().accept(this);
    }

    @Override
    public void visit(PrimitiveType type) {
        switch (type.getJavaPrimitiveFlags()) {
            case FLAG_BOOLEAN: sb.append("boolean"); break;
            case FLAG_CHAR: sb.append("char");    break;
            case FLAG_FLOAT: sb.append("float");   break;
            case FLAG_DOUBLE: sb.append("double");  break;
            case FLAG_BYTE: sb.append("byte");    break;
            case FLAG_SHORT: sb.append("short");   break;
            case FLAG_INT: sb.append("int");     break;
            case FLAG_LONG: sb.append("long");    break;
            case FLAG_VOID: sb.append("void");    break;
        }

        printDimension(type.getDimension());
    }

    @Override
    public void visit(ObjectType type) {
        sb.append(type.getQualifiedName());
        printTypeArguments(type);
        printDimension(type.getDimension());
    }

    public void visit(InnerObjectType type) {
        type.getOuterType().accept(this);
        sb.append('.').append(type.getName());
        printTypeArguments(type);
        printDimension(type.getDimension());
    }

    protected void printTypeArguments(ObjectType type) {
        BaseTypeArgument ta = type.getTypeArguments();

        if (ta != null) {
            sb.append('<');
            ta.accept(this);
            sb.append('>');
        }
    }

    @Override
    public void visit(WildcardSuperTypeArgument type) {
        sb.append("? super ");
        type.getType().accept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(TypeBounds type) {
        printList(type, " & ");
    }

    @Override
    public void visit(TypeParameter type) {
        sb.append(type.getIdentifier());
    }

    @Override
    public void visit(TypeParameterWithTypeBounds type) {
        sb.append(type.getIdentifier());
        sb.append(" extends ");
        type.getTypeBounds().accept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(TypeParameters types) {
        printList(types, ", ");
    }

    @Override public void visit(GenericType type) {
        sb.append(type.getName());
        printDimension(type.getDimension());
    }

    @Override
    public void visit(UnknownTypeArgument type) {
        sb.append('?');
    }

    protected <T extends TypeVisitable> void printList(List<T> list, String separator) {
        int size = list.size();

        if (size > 0) {
            list.get(0).accept(this);

            for (int i=1; i<size; i++) {
                sb.append(separator);
                list.get(i).accept(this);
            }
        }
    }

    protected void printDimension(int dimension) {
        switch (dimension) {
            case 0:
                break;
            case 1:
                sb.append("[]");
                break;
            case 2:
                sb.append("[][]");
                break;
            default:
                sb.append(new String(new char[dimension]).replaceAll("\0", "[]"));
        }
    }
}
