/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.loader;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.util.StringConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipLoader implements Loader {
    protected Map<String, byte[]> map = new HashMap<>();

    public  ZipLoader(InputStream is) throws IOException {
        byte[] buffer = new byte[1024 * 2];

        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                if (ze.isDirectory() == false) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int read = zis.read(buffer);

                    while (read > 0) {
                        out.write(buffer, 0, read);
                        read = zis.read(buffer);
                    }

                    map.put(ze.getName(), out.toByteArray());
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
        }
    }

    public Map<String, byte[]> getMap() { return map; }

    @Override
    public byte[] load(String internalName) throws IOException {
        return map.get(internalName + StringConstants.CLASS_FILE_SUFFIX);
    }

    @Override
    public boolean canLoad(String internalName) {
        return map.containsKey(internalName + StringConstants.CLASS_FILE_SUFFIX);
    }
}
