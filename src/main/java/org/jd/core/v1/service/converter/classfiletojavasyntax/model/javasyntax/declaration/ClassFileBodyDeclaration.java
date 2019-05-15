/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration;

import org.jd.core.v1.model.javasyntax.declaration.BaseMemberDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.TypeDeclaration;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.util.DefaultList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassFileBodyDeclaration extends BodyDeclaration implements ClassFileMemberDeclaration {
    protected List<ClassFileFieldDeclaration> fieldDeclarations;
    protected List<ClassFileConstructorOrMethodDeclaration> methodDeclarations;
    protected List<ClassFileMemberDeclaration> innerTypeDeclarations;
    protected Map<String, ClassFileMemberDeclaration> innerTypeMap = Collections.emptyMap();
    protected int firstLineNumber;
    protected ObjectType outerType;
    protected DefaultList<String> outerLocalVariableNames;
    protected ClassFileBodyDeclaration outerBodyDeclaration;

    public ClassFileBodyDeclaration(String internalTypeName) {
        super(internalTypeName, null);
    }

    public ClassFileBodyDeclaration(String internalTypeName, ClassFileBodyDeclaration outerBodyDeclaration) {
        super(internalTypeName, null);
        this.outerBodyDeclaration = outerBodyDeclaration;
    }

    public void setMemberDeclarations(BaseMemberDeclaration memberDeclarations) {
        this.memberDeclarations = memberDeclarations;
    }

    public List<ClassFileFieldDeclaration> getFieldDeclarations() {
        return fieldDeclarations;
    }

    public void setFieldDeclarations(List<ClassFileFieldDeclaration> fieldDeclarations) {
        if (fieldDeclarations != null) {
            updateFirstLineNumber(this.fieldDeclarations = fieldDeclarations);
        }
    }

    public List<ClassFileConstructorOrMethodDeclaration> getMethodDeclarations() {
        return methodDeclarations;
    }

    public void setMethodDeclarations(List<ClassFileConstructorOrMethodDeclaration> methodDeclarations) {
        if (methodDeclarations != null) {
            updateFirstLineNumber(this.methodDeclarations = methodDeclarations);
        }
    }

    public List<ClassFileMemberDeclaration> getInnerTypeDeclarations() {
        return innerTypeDeclarations;
    }

    public void setInnerTypeDeclarations(List<ClassFileMemberDeclaration> innerTypeDeclarations) {
        if (innerTypeDeclarations != null) {
            updateFirstLineNumber(this.innerTypeDeclarations = innerTypeDeclarations);

            innerTypeMap = new HashMap<>();

            for (ClassFileMemberDeclaration innerType : innerTypeDeclarations) {
                TypeDeclaration td = (TypeDeclaration) innerType;
                innerTypeMap.put(td.getInternalName(), innerType);
            }
        }
    }

    public ClassFileMemberDeclaration getInnerTypeDeclaration(String internalName) {
        ClassFileMemberDeclaration declaration = innerTypeMap.get(internalName);

        if ((declaration == null) && (outerBodyDeclaration != null)) {
            return outerBodyDeclaration.getInnerTypeDeclaration(internalName);
        }

        return declaration;
    }

    public ClassFileMemberDeclaration removeInnerType(String internalName) {
        ClassFileMemberDeclaration removed = innerTypeMap.remove(internalName);
        innerTypeDeclarations.remove(removed);
        return removed;
    }

    protected void updateFirstLineNumber(List<? extends ClassFileMemberDeclaration> members) {
        for (ClassFileMemberDeclaration member : members) {
            int lineNumber = member.getFirstLineNumber();

            if (lineNumber > 0) {
                if (firstLineNumber == 0) {
                    firstLineNumber = lineNumber;
                } else if (firstLineNumber > lineNumber) {
                    firstLineNumber = lineNumber;
                }

                break;
            }
        }
    }

    @Override
    public int getFirstLineNumber() {
        return firstLineNumber;
    }

    public ObjectType getOuterType() {
        return outerType;
    }

    public void setOuterType(ObjectType outerType) {
        this.outerType = outerType;
    }

    public DefaultList<String> getOuterLocalVariableNames() {
        return outerLocalVariableNames;
    }

    public void setOuterLocalVariableNames(DefaultList<String> outerLocalVariableNames) {
        this.outerLocalVariableNames = outerLocalVariableNames;
    }

    public ClassFileBodyDeclaration getOuterBodyDeclaration() {
        return outerBodyDeclaration;
    }

    @Override
    public String toString() {
        return "ClassFileBodyDeclaration{firstLineNumber=" + firstLineNumber + "}";
    }
}
