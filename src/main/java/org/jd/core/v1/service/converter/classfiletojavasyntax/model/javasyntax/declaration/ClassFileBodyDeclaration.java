/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.declaration.BaseMemberDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.util.DefaultList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassFileBodyDeclaration extends BodyDeclaration implements ClassFileMemberDeclaration {
    private final ClassFile classFile;
    private List<ClassFileFieldDeclaration> fieldDeclarations;
    private List<ClassFileConstructorOrMethodDeclaration> methodDeclarations;
    private List<ClassFileTypeDeclaration> innerTypeDeclarations;
    private Map<String, ClassFileTypeDeclaration> innerTypeMap = Collections.emptyMap();
    private int firstLineNumber;
    private String outerTypeFieldName;
    private DefaultList<String> syntheticInnerFieldNames;
    private final ClassFileBodyDeclaration outerBodyDeclaration;
    private final Map<String, TypeArgument> bindings;
    private final Map<String, BaseType> typeBounds;
    
    public ClassFileBodyDeclaration(ClassFile classFile, Map<String, TypeArgument> bindings, Map<String, BaseType> typeBounds, ClassFileBodyDeclaration outerBodyDeclaration) {
        super(classFile.getInternalTypeName(), null);
        this.classFile = classFile;
        this.bindings = bindings;
        this.typeBounds = typeBounds;
        this.outerBodyDeclaration = outerBodyDeclaration;
    }

    public void setMemberDeclarations(BaseMemberDeclaration memberDeclarations) {
        this.memberDeclarations = memberDeclarations;
    }

    public List<ClassFileFieldDeclaration> getFieldDeclarations() {
        return fieldDeclarations;
    }

    public void setFieldDeclarations(List<ClassFileFieldDeclaration> fieldDeclarations) {
        this.fieldDeclarations = fieldDeclarations;
        updateFirstLineNumber(this.fieldDeclarations);
    }

    public List<ClassFileConstructorOrMethodDeclaration> getMethodDeclarations() {
        return methodDeclarations;
    }

    public void setMethodDeclarations(List<ClassFileConstructorOrMethodDeclaration> methodDeclarations) {
        this.methodDeclarations = methodDeclarations;
        updateFirstLineNumber(this.methodDeclarations);
    }

    public List<ClassFileTypeDeclaration> getInnerTypeDeclarations() {
        return innerTypeDeclarations;
    }

    public boolean hasInnerTypeDeclarations() {
        return innerTypeDeclarations != null;
    }

    public void setInnerTypeDeclarations(List<ClassFileTypeDeclaration> innerTypeDeclarations) {
        if (innerTypeDeclarations != null) {
            this.innerTypeDeclarations = innerTypeDeclarations;
            updateFirstLineNumber(this.innerTypeDeclarations);

            innerTypeMap = new HashMap<>();

            for (ClassFileTypeDeclaration innerType : innerTypeDeclarations) {
                innerTypeMap.put(innerType.getInternalTypeName(), innerType);
            }
        }
    }

    public ClassFileTypeDeclaration getInnerTypeDeclaration(String internalName) {
        ClassFileTypeDeclaration declaration = innerTypeMap.get(internalName);

        if (declaration == null && outerBodyDeclaration != null) {
            return outerBodyDeclaration.getInnerTypeDeclaration(internalName);
        }

        return declaration;
    }

    public ClassFileMemberDeclaration removeInnerType(String internalName) {
        ClassFileMemberDeclaration removed = innerTypeMap.remove(internalName);
        if (innerTypeDeclarations != null) {
            innerTypeDeclarations.remove(removed);
        }
        return removed;
    }

    protected void updateFirstLineNumber(List<? extends ClassFileMemberDeclaration> members) {
        int lineNumber;
        for (ClassFileMemberDeclaration member : members) {
            lineNumber = member.getFirstLineNumber();

            if (lineNumber > 0) {
                if (firstLineNumber == 0 || firstLineNumber > lineNumber) {
                    firstLineNumber = lineNumber;
                }

                break;
            }
        }
    }

    public ClassFile getClassFile() {
        return classFile;
    }

    @Override
    public int getFirstLineNumber() {
        return firstLineNumber;
    }

    public String getOuterTypeFieldName() {
        return outerTypeFieldName;
    }

    public void setOuterTypeFieldName(String outerTypeFieldName) {
        this.outerTypeFieldName = outerTypeFieldName;
    }

    public DefaultList<String> getSyntheticInnerFieldNames() {
        return syntheticInnerFieldNames;
    }

    public void setSyntheticInnerFieldNames(DefaultList<String> syntheticInnerFieldNames) {
        this.syntheticInnerFieldNames = syntheticInnerFieldNames;
    }

    public ClassFileBodyDeclaration getOuterBodyDeclaration() {
        return outerBodyDeclaration;
    }

    public boolean isMainBodyDeclaration() {
        return outerBodyDeclaration == null;
    }
    
    public Map<String, TypeArgument> getBindings() {
        return bindings;
    }

    public Map<String, BaseType> getTypeBounds() {
        return typeBounds;
    }

    @Override
    public String toString() {
        return "ClassFileBodyDeclaration{firstLineNumber=" + firstLineNumber + "}";
    }
}
