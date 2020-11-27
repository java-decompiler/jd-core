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
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

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

    @Deprecated
    protected HashMap<String, Object> headers = new HashMap<>();
    protected Object body;

    public DecompileContext() {}

    public DecompileContext(Object body) {
        this.body = body;
    }

    @SuppressWarnings("unchecked")
    public <T> T getHeader(String name) {
        return (T)headers.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getHeader(String name, T defaultValue) {
        Object value = headers.get(name);

        if (value == null) {
            return defaultValue;
        } else {
            return (T)value;
        }
    }

    public void setHeader(String name, Object value) {
        headers.put(name, value);
    }

    public Object removeHeader(String name) {
        return headers.remove(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBody() {
        return (T)body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
