/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.JavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public class JavaGenericTest extends AbstractJdTest {

    @Test
    public void testJdk170GenericClass() throws Exception {
        String internalClassName = "org/jd/core/test/GenericClass";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
        String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

        // Check decompiled source code
        assertTrue(source.indexOf("public class GenericClass<T1, T2, T3 extends AnnotatedClass, T4 extends Serializable, T5 extends Serializable & Comparable, T6 extends AnnotatedClass & Serializable & Comparable<GenericClass>, T7 extends Map<?, ?>, T8 extends Map<? extends Number, ? super Serializable>, T9 extends T8>") != -1);
        assertTrue(source.indexOf("extends ArrayList<T7>") != -1);
        assertTrue(source.indexOf("implements Serializable, Comparable<T1>") != -1);

        assertTrue(source.matches(PatternMaker.make("/*  26:  26 */", "public List<List<? extends GenericClass>> list1 = new ArrayList<>();")));
        assertTrue(source.indexOf("public List<List<? super GenericClass>> list2;") != -1);
        assertTrue(source.matches(PatternMaker.make("/*  31:  31 */", "list2 = new ArrayList<>();")));

        assertTrue(source.indexOf("public <T> void fromArrayToCollection(T[] a, Collection<T> c)") != -1);
        assertTrue(source.indexOf("public <T> void copy(List<T> dest, List<? extends T> src)") != -1);
        assertTrue(source.indexOf("public <T, S extends T> List<? extends Number> copy2(List<? super T> dest, List<S> src) throws InvalidParameterException, ClassCastException") != -1);
        assertTrue(source.indexOf("public <T1, T2 extends Exception> List<? extends Number> print(List<? super T1> list) throws T2, InvalidParameterException") != -1);

        assertTrue(source.matches(PatternMaker.make(": 100 */", "return call(0);")));
        assertTrue(source.matches(PatternMaker.make(": 104 */", "return (T1)this;")));

        assertTrue(source.indexOf("/* 104: 104 */") != -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile(
                "1.7",
                new JavaSourceFileObject(internalClassName, source),
                new JavaSourceFileObject("org/jd/core/test/AnnotatedClass", "package org.jd.core.test; public class AnnotatedClass {}")
            ));
    }
}
