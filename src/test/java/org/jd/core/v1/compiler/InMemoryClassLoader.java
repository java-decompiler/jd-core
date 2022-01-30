package org.jd.core.v1.compiler;

import java.util.HashMap;
import java.util.Map;

public class InMemoryClassLoader extends ClassLoader {
    public final Map<String, InMemoryJavaClassFileObject> classes = new HashMap<>();

    public void add(String name, InMemoryJavaClassFileObject fileObject) {
        classes.put(name.replace('/', '.'), fileObject);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        InMemoryJavaClassFileObject fileObject = classes.get(name);
        if (fileObject != null) {
            byte[] byteCode = fileObject.getByteCode();
            return defineClass(name, byteCode, 0, byteCode.length);
        }
        return super.findClass(name);
    }
}