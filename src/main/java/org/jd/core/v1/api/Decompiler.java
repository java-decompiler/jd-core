/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.api;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.api.printer.Printer;

import java.io.IOException;
import java.util.Map;

public interface Decompiler {
    void decompile(Loader loader, Printer printer, String internalName) throws LoaderException, IOException;

    void decompile(Loader loader, Printer printer, String internalName, Map<String, Object> configuration) throws LoaderException, IOException;
}
