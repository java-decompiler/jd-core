/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.api;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.message.DecompileContext;

import java.io.IOException;
import java.util.Map;

public interface Decompiler {
    DecompileContext decompile(Loader loader, Printer printer, String internalName) throws IOException;

    DecompileContext decompile(Loader loader, Printer printer, String internalName, Map<String, Object> configuration) throws IOException;
}
