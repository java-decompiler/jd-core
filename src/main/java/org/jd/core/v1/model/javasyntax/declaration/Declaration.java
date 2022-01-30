/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.declaration;

public interface Declaration {
    // Access flags for Class, Field, Method, Nested class, Module, Module Requires, Module Exports, Module Opens
    int FLAG_ANONYMOUS    = 0x0200;  // .  .  M  .  .  .  .  . // Custom flag
    // Extension
    int FLAG_DEFAULT      = 0x10000; // .  .  M  .  .  .  .  .

    void accept(DeclarationVisitor visitor);
}
