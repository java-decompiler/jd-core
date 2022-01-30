/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.classfile.attribute;

// Example: https://github.com/netroby/jdk9-dev/blob/master/jdk/src/java.management/share/classes/module-info.java
public record AttributeModule(String name, int flags, String version, ModuleInfo[] requires, PackageInfo[] exports, PackageInfo[] opens, String[] uses, ServiceInfo[] provides) implements Attribute {
}
