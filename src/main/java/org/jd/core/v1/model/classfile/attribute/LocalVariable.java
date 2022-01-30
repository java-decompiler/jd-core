/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.classfile.attribute;

public record LocalVariable(int startPc, int length, String name, String descriptor, int index) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("LocalVariable{index=").append(index);
        sb.append(", name=").append(name);
        sb.append(", descriptor=").append(descriptor);
        sb.append(", startPc=").append(startPc);
        sb.append(", length=").append(length);

        return sb.append("}").toString();
    }
}
