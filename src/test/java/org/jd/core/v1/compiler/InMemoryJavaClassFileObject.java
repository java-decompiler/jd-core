package org.jd.core.v1.compiler;

import org.jd.core.v1.util.StringConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class InMemoryJavaClassFileObject extends SimpleJavaFileObject {
    private ByteArrayOutputStream byteCode;

    public InMemoryJavaClassFileObject(String absClassName) {
        super(URI.create("memory:///" + absClassName.replace('.', '/') + StringConstants.CLASS_FILE_SUFFIX), Kind.CLASS);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        byteCode = new ByteArrayOutputStream();
        return byteCode;
    }

    public byte[] getByteCode() {
        return byteCode.toByteArray();
    }
}