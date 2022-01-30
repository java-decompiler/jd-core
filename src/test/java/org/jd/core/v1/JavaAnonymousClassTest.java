/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

public class JavaAnonymousClassTest extends AbstractJdTest {
    @Test
    public void testJdk150AnonymousClass() throws Exception {
        String internalClassName = "org/jd/core/test/AnonymousClass";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.5.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  21 */", "Object object = new Object()")));
            assertTrue(source.matches(PatternMaker.make(":  23 */", "return \"toString() return \" + super.toString() + \" at \" + AnonymousClass.this.time;")));

            assertTrue(source.matches(PatternMaker.make(":  37 */", "final long l1 = System.currentTimeMillis();")));
            assertTrue(source.matches(PatternMaker.make(":  39 */", "Enumeration enumeration = new Enumeration()")));
            assertTrue(source.matches(PatternMaker.make(":  40 */", "Iterator<String> i = AnonymousClass.this.list.iterator();")));
            assertTrue(source.matches(PatternMaker.make(":  44 */", "return (this.i.hasNext() && s1 == s2 && i1 > l1);")));
            assertNotEquals(-1, source.indexOf("return this.i.next();"));
            assertTrue(source.matches(PatternMaker.make(":  52 */", "test(enumeration, \"test\");")));
            assertTrue(source.matches(PatternMaker.make(":  55 */", "System.out.println(\"end\");")));

            assertTrue(source.matches(PatternMaker.make(":  67 */", "if (s1 == s2 && i == 5)")));

            assertTrue(source.matches(PatternMaker.make(":  90 */", "Serializable serializable = new Serializable()")));
            assertTrue(source.matches(PatternMaker.make(":  96 */", "return (abc.equals(param2Object) || def.equals(param2Object) || str1.equals(param2Object) || str2.equals(param2Object));")));
            assertTrue(source.matches(PatternMaker.make(": 104 */", "System.out.println(\"end\");")));

            assertNotEquals(-1, source.indexOf("/* 111: 111 */"));

            assertEquals(-1, source.indexOf("{ ;"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile(
                    "1.5",
                    new InMemoryJavaSourceFileObject(internalClassName, source),
                    new InMemoryJavaSourceFileObject("org/jd/core/test/annotation/Name", "package org.jd.core.test.annotation; public @interface Name {String value();}")
            ));
        }
    }

    @Test
    public void testJdk170AnonymousClass() throws Exception {
        String internalClassName = "org/jd/core/test/AnonymousClass";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  21 */", "Object obj = new Object()")));
            assertTrue(source.matches(PatternMaker.make(":  23 */", "return \"toString() return \" + super.toString() + \" at \" + AnonymousClass.this.time;")));

            assertTrue(source.matches(PatternMaker.make(":  39 */", "Enumeration e = new Enumeration()")));
            assertTrue(source.matches(PatternMaker.make(":  40 */", "Iterator<String> i = AnonymousClass.this.list.iterator();")));
            assertTrue(source.matches(PatternMaker.make(":  44 */", "return (this.i.hasNext() && s1 == s2 && i1 > l1);")));

            assertTrue(source.matches(PatternMaker.make(":  61 */", "final int i = s1.length();")));
            assertTrue(source.matches(PatternMaker.make(":  63 */", "System.out.println(\"2\" + new StringWrapper(123456L)")));
            assertTrue(source.matches(PatternMaker.make(":  67 */", "if (s1 == s2 && i == 5)")));
            assertTrue(source.matches(PatternMaker.make("/*  72:   0 */", "} + \"3\");")));

            assertTrue(source.matches(PatternMaker.make(":  81 */", "final Object abc = \"abc\";")));
            assertTrue(source.matches(PatternMaker.make(":  82 */", "final Object def = \"def\";")));
            assertTrue(source.matches(PatternMaker.make(":  84 */", "Serializable serializable = new Serializable()")));
            assertTrue(source.matches(PatternMaker.make(":  90 */", "Serializable serializable = new Serializable()")));
            assertTrue(source.matches(PatternMaker.make(":  96 */", "return (abc.equals(obj) || def.equals(obj) || ghi.equals(obj) || jkl.equals(obj));")));
            assertTrue(source.matches(PatternMaker.make(": 100 */", "return (abc.equals(obj) || def.equals(obj));")));
            assertTrue(source.matches(PatternMaker.make("/* 102:   0 */", "};")));

            assertTrue(source.matches(PatternMaker.make(": 111 */", "this.l = l & 0x80L;")));

            assertEquals(-1, source.indexOf("{ ;"));
            assertEquals(-1, source.indexOf("} ;"));
            assertEquals(-1, source.indexOf("// Byte code:"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile(
                    "1.7",
                    new InMemoryJavaSourceFileObject(internalClassName, source),
                    new InMemoryJavaSourceFileObject("org/jd/core/test/annotation/Name", "package org.jd.core.test.annotation; public @interface Name {String value();}")
                ));
        }
    }
}
