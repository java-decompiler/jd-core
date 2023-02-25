/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration;

import org.apache.bcel.classfile.Method;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.reference.BaseElementValue;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;

import java.util.Map;

public class ClassFileMethodDeclaration extends MethodDeclaration implements ClassFileConstructorOrMethodDeclaration {
    private final ClassFileBodyDeclaration bodyDeclaration;
    private final ClassFile classFile;
    private final Method method;
    private final BaseType parameterTypes;
    private final Map<String, TypeArgument> bindings;
    private final Map<String, BaseType> typeBounds;
    private final int firstLineNumber;

    public ClassFileMethodDeclaration(
            ClassFileBodyDeclaration bodyDeclaration, ClassFile classFile, Method method, String name,
            Type returnedType, BaseType parameterTypes, Map<String, TypeArgument> bindings,
            Map<String, BaseType> typeBounds, int firstLineNumber) {
        super(null, method.getAccessFlags(), name, null, returnedType, null, null, method.getSignature(), null, null);
        this.bodyDeclaration = bodyDeclaration;
        this.classFile = classFile;
        this.method = method;
        this.parameterTypes = parameterTypes;
        this.bindings = bindings;
        this.typeBounds = typeBounds;
        this.firstLineNumber = firstLineNumber;
    }

    public ClassFileMethodDeclaration(
            ClassFileBodyDeclaration bodyDeclaration, ClassFile classFile, Method method, BaseAnnotationReference annotationReferences,
            String name, BaseTypeParameter typeParameters, Type returnedType, BaseType parameterTypes, BaseType exceptionTypes,
            BaseElementValue defaultAnnotationValue, Map<String, TypeArgument> bindings,
            Map<String, BaseType> typeBounds, int firstLineNumber) {
        super(annotationReferences, method.getAccessFlags(), name, typeParameters, returnedType, null, exceptionTypes, method.getSignature(), null, defaultAnnotationValue);
        this.bodyDeclaration = bodyDeclaration;
        this.classFile = classFile;
        this.method = method;
        this.parameterTypes = parameterTypes;
        this.bindings = bindings;
        this.typeBounds = typeBounds;
        this.firstLineNumber = firstLineNumber;
    }

    @Override
    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Override
    public void setFormalParameters(BaseFormalParameter formalParameters) {
        this.formalParameters = formalParameters;
    }

    @Override
    public void setStatements(BaseStatement statements) {
        this.statements = statements;
    }

    @Override
    public ClassFileBodyDeclaration getBodyDeclaration() {
        return bodyDeclaration;
    }

    @Override
    public ClassFile getClassFile() {
        return classFile;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public BaseType getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public Map<String, TypeArgument> getBindings() {
        return bindings;
    }

    @Override
    public Map<String, BaseType> getTypeBounds() {
        return typeBounds;
    }

    @Override
    public int getFirstLineNumber() {
        return firstLineNumber;
    }

    @Override
    public String toString() {
        return "ClassFileMethodDeclaration{" + name + " " + descriptor + ", firstLineNumber=" + firstLineNumber + "}";
    }
}
