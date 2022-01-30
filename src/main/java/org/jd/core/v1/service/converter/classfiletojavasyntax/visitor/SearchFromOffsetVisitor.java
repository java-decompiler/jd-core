/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.type.DiamondTypeArgument;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.TypeArguments;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardSuperTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;

public class SearchFromOffsetVisitor extends AbstractJavaSyntaxVisitor {
    private int offset;

    public SearchFromOffsetVisitor() {
        offset = Integer.MAX_VALUE;
    }

    public int getOffset() {
        return this.offset;
    }

    @Override
    public void visit(LocalVariableReferenceExpression expression) {
        int localOffset = ((ClassFileLocalVariableReferenceExpression) expression).getOffset();

        if (this.offset > localOffset) {
            this.offset = localOffset;
        }
    }

    @Override public void visit(IntegerConstantExpression expression) {}

    @Override public void visit(TypeArguments arguments) {}
    @Override public void visit(DiamondTypeArgument argument) {}
    @Override public void visit(WildcardExtendsTypeArgument argument) {}
    @Override public void visit(WildcardSuperTypeArgument argument) {}
    @Override public void visit(WildcardTypeArgument argument) {}
    @Override public void visit(PrimitiveType type) {}
    @Override public void visit(ObjectType type) {}
    @Override public void visit(InnerObjectType type) {}
    @Override public void visit(GenericType type) {}
}
