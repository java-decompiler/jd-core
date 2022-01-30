/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.declaration;

import java.util.List;

public class ModuleDeclaration extends TypeDeclaration {
    private final String            version;
    private final List<ModuleInfo>  requires;
    private final List<PackageInfo> exports;
    private final List<PackageInfo> opens;
    private final List<String>      uses;
    private final List<ServiceInfo> provides;

    public ModuleDeclaration(int flags, String internalName, String name, String version, List<ModuleInfo> requires, List<PackageInfo> exports, List<PackageInfo> opens, List<String> uses, List<ServiceInfo> provides) {
        super(null, flags, internalName, name, null);
        this.version = version;
        this.requires = requires;
        this.exports = exports;
        this.opens = opens;
        this.uses = uses;
        this.provides = provides;
    }

    public String getVersion() { return version; }
    public List<ModuleInfo> getRequires() { return requires; }
    public List<PackageInfo> getExports() { return exports; }
    public List<PackageInfo> getOpens() { return opens; }
    public List<String> getUses() { return uses; }
    public List<ServiceInfo> getProvides() { return provides; }

    @Override
    public void accept(DeclarationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "ModuleDeclaration{" + internalTypeName + "}";
    }

    public static record ModuleInfo(String name, int flags, String version) {

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

    public static record PackageInfo(String internalName, int flags, List<String> moduleInfoNames) {

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("PackageInfo{internalName=").append(internalName);
            sb.append(", flags=").append(flags);

            if (moduleInfoNames != null) {
                sb.append(", moduleInfoNames=").append(moduleInfoNames);
            }

            return sb.append("}").toString();
        }
    }

    public static record ServiceInfo(String interfaceTypeName, List<String> implementationTypeNames) {

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("ServiceInfo{interfaceTypeName=").append(interfaceTypeName);

            if (implementationTypeNames != null) {
                sb.append(", implementationTypeNames=").append(implementationTypeNames);
            }

            return sb.append("}").toString();
        }
    }
}
