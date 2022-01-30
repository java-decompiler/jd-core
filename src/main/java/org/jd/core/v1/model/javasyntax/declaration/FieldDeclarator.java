/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.declaration;

import java.util.Objects;

public class FieldDeclarator implements BaseFieldDeclarator {
    private FieldDeclaration fieldDeclaration;
    private final String name;
    private VariableInitializer variableInitializer;

    public FieldDeclarator(String name) {
        this.name = name;
    }

    public FieldDeclarator(String name, VariableInitializer variableInitializer) {
        this.name = name;
        this.variableInitializer = variableInitializer;
    }

    @Override
    public void setFieldDeclaration(FieldDeclaration fieldDeclaration) {
        this.fieldDeclaration = fieldDeclaration;
    }

    public FieldDeclaration getFieldDeclaration() {
        return fieldDeclaration;
    }

    public String getName() {
        return name;
    }

    public VariableInitializer getVariableInitializer() {
        return variableInitializer;
    }

    public void setVariableInitializer(VariableInitializer variableInitializer) {
        this.variableInitializer = variableInitializer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldDeclarator that = (FieldDeclarator) o;
        return name.equals(that.name) && Objects.equals(variableInitializer, that.variableInitializer);
    }

    @Override
    public int hashCode() {
        int result = 544_278_669 + name.hashCode();
        return 31 * result + Objects.hash(variableInitializer);
    }

    @Override
    public void accept(DeclarationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "FieldDeclarator{" + name + "}";
    }
}
