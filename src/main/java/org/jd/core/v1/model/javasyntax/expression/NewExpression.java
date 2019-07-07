/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;

public class NewExpression extends AbstractLineNumberExpression {
    protected BaseTypeArgument nonWildcardTypeArguments;
    protected ObjectType type;
    protected String descriptor;
    protected BaseExpression parameters;
    protected BodyDeclaration bodyDeclaration;

    public NewExpression(int lineNumber, ObjectType type) {
        super(lineNumber);
        this.type = type;
    }

    public NewExpression(int lineNumber, ObjectType type, BodyDeclaration bodyDeclaration) {
        super(lineNumber);
        this.type = type;
        this.bodyDeclaration = bodyDeclaration;
    }

    public NewExpression(int lineNumber, BaseTypeArgument nonWildcardTypeArguments, ObjectType type, String descriptor, BaseExpression parameters, BodyDeclaration bodyDeclaration) {
        super(lineNumber);
        this.nonWildcardTypeArguments = nonWildcardTypeArguments;
        this.type = type;
        this.descriptor = descriptor;
        this.parameters = parameters;
        this.bodyDeclaration = bodyDeclaration;
    }

    public BaseTypeArgument getNonWildcardTypeArguments() {
        return nonWildcardTypeArguments;
    }

    public ObjectType getObjectType() {
        return type;
    }

    public void setObjectType(ObjectType type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public BaseExpression getParameters() {
        return parameters;
    }

    public void setParameters(BaseExpression parameters) {
        this.parameters = parameters;
    }

    public void setDescriptorAndParameters(String descriptor, BaseExpression parameters) {
        this.descriptor = descriptor;
        this.parameters = parameters;
    }

    public BodyDeclaration getBodyDeclaration() {
        return bodyDeclaration;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
