/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.classfile.attribute;

public record ModuleInfo(String name, int flags, String version) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ModuleInfo{name=").append(name);
        sb.append(", flags=").append(flags);

        if (version != null) {
            sb.append(", version=").append(version);
        }

        return sb.append("}").toString();
    }
}
