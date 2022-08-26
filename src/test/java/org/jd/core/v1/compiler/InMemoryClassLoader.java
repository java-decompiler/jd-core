package org.jd.core.v1.compiler;

import org.jd.core.v1.api.loader.Loader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InMemoryClassLoader extends ClassLoader implements Loader {
    public final Map<String, InMemoryJavaClassFileObject> classes = new HashMap<>();

    public void add(String name, InMemoryJavaClassFileObject fileObject) {
        classes.put(internalNameToFullyQualifiedName(name), fileObject);
    }

    private static String internalNameToFullyQualifiedName(String name) {
        return name.replaceAll("[/$]", ".");
    }

    public byte[] getByteCode(String name) {
        InMemoryJavaClassFileObject fileObject = classes.get(name);
        if (fileObject != null) {
            return fileObject.getByteCode();
        }
        return null;
    }
    
    public Class<?> findClassByInternalName(String internalName) throws ClassNotFoundException {
        return findClass(internalNameToFullyQualifiedName(internalName));
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] byteCode = getByteCode(name);
        if (byteCode != null) {
            return defineClass(name, byteCode, 0, byteCode.length);
        }
        return super.findClass(name);
    }

    @Override
    public boolean canLoad(String internalName) {
        return classes.containsKey(internalNameToFullyQualifiedName(internalName));
    }

    @Override
    public byte[] load(String internalName) throws IOException {
        return getByteCode(internalNameToFullyQualifiedName(internalName));
    }
}