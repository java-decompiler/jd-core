/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.declaration;

import org.jd.core.v1.util.DefaultList;

import java.util.Collection;

public class FormalParameters<T extends FormalParameter> extends DefaultList<T> implements BaseFormalParameter {
    public FormalParameters() {}

    public FormalParameters(Collection<T> collection) {
        super(collection);
        assert (collection != null) && (collection.size() > 1) : "Uses 'FormalParameter' instead";
    }

    @SuppressWarnings("unchecked")
    public FormalParameters(T parameter, T... parameters) {
        super(parameter, parameters);
        assert (parameters != null) && (parameters.length > 0) : "Uses 'FormalParameter' instead";
    }

    @Override
    public void accept(DeclarationVisitor visitor) {
        visitor.visit(this);
    }
}
