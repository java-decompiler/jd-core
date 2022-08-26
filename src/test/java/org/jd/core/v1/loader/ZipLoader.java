/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.loader;

import org.jd.core.v1.util.StringConstants;

import java.io.IOException;
import java.io.InputStream;

public class ZipLoader extends org.jd.core.v1.util.ZipLoader {

    public ZipLoader(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected String makeEntryName(String entryName) {
        return entryName;
    }

    @Override
    public byte[] load(String internalName) throws IOException {
        return getMap().get(internalName + StringConstants.CLASS_FILE_SUFFIX);
    }

    @Override
    public boolean canLoad(String internalName) {
        return getMap().containsKey(internalName + StringConstants.CLASS_FILE_SUFFIX);
    }
}
