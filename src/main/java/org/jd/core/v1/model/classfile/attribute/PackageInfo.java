/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.classfile.attribute;

import java.util.Arrays;

public record PackageInfo(String internalName, int flags, String[] moduleInfoNames) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("PackageInfo{internalName=").append(internalName);
        sb.append(", flags=").append(flags);

        if (moduleInfoNames != null) {
            sb.append(", moduleInfoNames=").append(Arrays.toString(moduleInfoNames));
        }

        return sb.append("}").toString();
    }
}
