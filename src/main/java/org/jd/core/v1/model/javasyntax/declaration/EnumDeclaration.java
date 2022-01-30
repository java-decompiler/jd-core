/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.declaration;

import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.type.BaseType;

import java.util.List;

public class EnumDeclaration extends TypeDeclaration {
    private final BaseType interfaces;
    protected List<Constant> constants;

    public EnumDeclaration(int flags, String internalName, String name, List<Constant> constants, BodyDeclaration bodyDeclaration) {
        this(null, flags, internalName, name, null, constants, bodyDeclaration);
    }

    public EnumDeclaration(BaseAnnotationReference annotationReferences, int flags, String internalName, String name, BaseType interfaces, List<Constant> constants, BodyDeclaration bodyDeclaration) {
        super(annotationReferences, flags, internalName, name, bodyDeclaration);
        this.interfaces = interfaces;
        this.constants = constants;
    }

    public BaseType getInterfaces() {
        return interfaces;
    }

    public List<Constant> getConstants() {
        return constants;
    }

    @Override
    public void accept(DeclarationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "EnumDeclaration{" + internalTypeName + "}";
    }

    public static class Constant implements Declaration {
        private final int lineNumber;
        protected final String name;
        private BaseExpression arguments;
        private final BodyDeclaration bodyDeclaration;

        public Constant(String name) {
            this(name, null);
        }

        public Constant(String name, BaseExpression arguments) {
            this(0, name, arguments, null);
        }

        public Constant(int lineNumber, String name, BaseExpression arguments, BodyDeclaration bodyDeclaration) {
            this.lineNumber = lineNumber;
            this.name = name;
            this.arguments = arguments;
            this.bodyDeclaration = bodyDeclaration;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getName() {
            return name;
        }

        public BaseExpression getArguments() {
            return arguments;
        }

        public void setArguments(BaseExpression arguments) {
            this.arguments = arguments;
        }

        public BodyDeclaration getBodyDeclaration() {
            return bodyDeclaration;
        }

        @Override
        public void accept(DeclarationVisitor visitor) {
            visitor.visit(this);
        }
    }
}
