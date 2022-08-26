/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.token;

import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

public class ReferenceToken extends DeclarationToken {

    private final ObjectType ownerType;

    /**
     * @param type @see org.jd.core.v1.model.token.DeclarationToken
     * @param internalTypeName
     * @param name
     * @param descriptor
     * @param ownerType
     */
    public ReferenceToken(int type, String internalTypeName, String name, String descriptor, ObjectType ownerType) {
        super(type, internalTypeName, name, descriptor);
        this.ownerType = ownerType;
    }

    public ReferenceToken(int type, String internalName, String name, String descriptor, String ownerInternalName) {
        this(type, internalName, name, descriptor, TypeMaker.create(ownerInternalName));
    }

    public ReferenceToken(int type, String internalName, String name) {
        this(type, internalName, name, null, (ObjectType)null);
    }

    public String getOwnerInternalName() {
        return ownerType == null ? null : ownerType.getInternalName();
    }

    @Override
    public String toString() {
        return "ReferenceToken{'" + name + "'}";
    }

    @Override
    public void accept(TokenVisitor visitor) {
        visitor.visit(this);
    }
}
