/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.message;

import lombok.Getter;
import lombok.Setter;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.fragment.Fragment;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.token.Token;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class DecompileContext {
    protected String mainInternalTypeName;
    protected Map<String, Object> configuration = new HashMap<>();
    protected Loader loader;
    protected Printer printer;

    protected TypeMaker typeMaker;
    protected int majorVersion;
    protected int minorVersion;
    protected int maxLineNumber = Printer.UNKNOWN_LINE_NUMBER;
    protected boolean containsByteCode;
    protected boolean showBridgeAndSynthetic;

    protected ClassFile classFile;
    protected CompilationUnit compilationUnit;
    protected DefaultList<Token> tokens;

    @Deprecated
    protected Object body;

    public DecompileContext() {}

    @Deprecated
    public DecompileContext(DefaultList<Fragment> fragments) {
        this.body = fragments;
    }
    
    @SuppressWarnings("unchecked")
    @Deprecated
    public <T> T getBody() {
        return (T)body;
    }

    @Deprecated
    public void setBody(Object body) {
        this.body = body;
    }
}
