/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.deserializer.classfile;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.message.DecompileContext;

/**
 * Create a ClassFile model from a loader and a internal type name.<br><br>
 *
 * Input:  -<br>
 * Output: {@link org.jd.core.v1.model.classfile.ClassFile}<br>
 */
public class DeserializeClassFileProcessor extends ClassFileDeserializer {

    public void process(DecompileContext decompileContext) throws Exception {
        Loader loader = decompileContext.getLoader();
        String internalTypeName = decompileContext.getMainInternalTypeName();
        ClassFile classFile = loadClassFile(loader, internalTypeName);

        decompileContext.setBody(classFile);
    }
}
