package org.jd.core.v1.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.tools.SimpleJavaFileObject;

public class InMemoryJavaSourceFileObject extends SimpleJavaFileObject {
    private final String sourceCode;
    private final String absClassName;

    public InMemoryJavaSourceFileObject(String absClassName, String sourceCode) {
        super(URI.create("memory:///" + absClassName.replace('.', '/') + ".java"), Kind.SOURCE);
        this.sourceCode = sourceCode;
        this.absClassName = absClassName;
    }

    public String getAbsClassName() {
        return absClassName;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return sourceCode;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(sourceCode.getBytes(StandardCharsets.UTF_8));
    }
}
