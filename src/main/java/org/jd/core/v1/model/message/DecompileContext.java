/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.message;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.fragment.Fragment;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.token.Token;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;
import java.util.Map;

public class DecompileContext {
    private String mainInternalTypeName;
    private Map<String, Object> configuration = new HashMap<>();
    private Loader loader;
    private Printer printer;

    private TypeMaker typeMaker;
    private int majorVersion;
    private int minorVersion;
    private int maxLineNumber = Printer.UNKNOWN_LINE_NUMBER;

    private ClassFile classFile;
    private CompilationUnit compilationUnit;
    private DefaultList<Token> tokens;

    private Object body;

    public DecompileContext() {}

    public DecompileContext(DefaultList<Fragment> fragments) {
        this.body = fragments;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBody() {
        return (T)body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public String getMainInternalTypeName() {
        return mainInternalTypeName;
    }

    public void setMainInternalTypeName(String mainInternalTypeName) {
        this.mainInternalTypeName = mainInternalTypeName;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public Loader getLoader() {
        return loader;
    }

    public void setLoader(Loader loader) {
        this.loader = loader;
    }

    public Printer getPrinter() {
        return printer;
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

    public TypeMaker getTypeMaker() {
        return typeMaker;
    }

    public void setTypeMaker(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public int getMaxLineNumber() {
        return maxLineNumber;
    }

    public void setMaxLineNumber(int maxLineNumber) {
        this.maxLineNumber = maxLineNumber;
    }

    public ClassFile getClassFile() {
        return classFile;
    }

    public void setClassFile(ClassFile classFile) {
        this.classFile = classFile;
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    public void setCompilationUnit(CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

    public DefaultList<Token> getTokens() {
        return tokens;
    }

    public void setTokens(DefaultList<Token> tokens) {
        this.tokens = tokens;
    }
}
