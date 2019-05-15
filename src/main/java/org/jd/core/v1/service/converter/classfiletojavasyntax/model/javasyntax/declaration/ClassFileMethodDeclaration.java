/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.reference.ElementValue;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.Type;

import java.util.List;

public class ClassFileMethodDeclaration extends MethodDeclaration implements ClassFileConstructorOrMethodDeclaration {
    protected ClassFileBodyDeclaration bodyDeclaration;
    protected ClassFile classFile;
    protected Method method;
    protected List<Type> parameterTypes;
    protected int firstLineNumber;

    public ClassFileMethodDeclaration(
            ClassFileBodyDeclaration bodyDeclaration, ClassFile classFile, Method method, String name,
            Type returnedType, List<Type> parameterTypes) {
        super(null, method.getAccessFlags(), name, null, returnedType, null, null, method.getDescriptor(), null, null);
        this.bodyDeclaration = bodyDeclaration;
        this.classFile = classFile;
        this.parameterTypes = parameterTypes;
        this.method = method;
    }

    public ClassFileMethodDeclaration(
            ClassFileBodyDeclaration bodyDeclaration, ClassFile classFile, Method method, String name,
            Type returnedType, List<Type> parameterTypes, int firstLineNumber) {
        super(null, method.getAccessFlags(), name, null, returnedType, null, null, method.getDescriptor(), null, null);
        this.bodyDeclaration = bodyDeclaration;
        this.classFile = classFile;
        this.parameterTypes = parameterTypes;
        this.method = method;
        this.firstLineNumber = firstLineNumber;
    }

    public ClassFileMethodDeclaration(
            ClassFileBodyDeclaration bodyDeclaration, ClassFile classFile, Method method, BaseAnnotationReference annotationReferences,
            String name, BaseTypeParameter typeParameters, Type returnedType, List<Type> parameterTypes, BaseType exceptions,
            ElementValue defaultAnnotationValue, int firstLineNumber) {
        super(annotationReferences, method.getAccessFlags(), name, typeParameters, returnedType, null, exceptions, method.getDescriptor(), null, defaultAnnotationValue);
        this.bodyDeclaration = bodyDeclaration;
        this.classFile = classFile;
        this.parameterTypes = parameterTypes;
        this.method = method;
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
    public List<Type> getParameterTypes() {
        return parameterTypes;
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
