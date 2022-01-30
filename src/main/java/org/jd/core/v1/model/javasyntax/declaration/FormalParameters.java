/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.declaration;

import org.jd.core.v1.util.DefaultList;

public class FormalParameters extends DefaultList<FormalParameter> implements BaseFormalParameter {
    private static final long serialVersionUID = 1L;

    public FormalParameters() {}

    public FormalParameters(FormalParameter parameter, FormalParameter... parameters) {
        super(parameter, parameters);
        if (parameters.length <= 0) {
            throw new IllegalArgumentException("Use 'FormalParameter' instead");
        }
    }

    @Override
    public void accept(DeclarationVisitor visitor) {
        visitor.visit(this);
    }
}
