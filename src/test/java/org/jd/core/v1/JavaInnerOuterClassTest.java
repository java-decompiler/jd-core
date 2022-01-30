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

public class JavaInnerOuterClassTest extends AbstractJdTest {

    @Test
    public void testJdk170InnerOuterClass() throws Exception {
        String internalClassName = "org/jd/core/test/OuterClass";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  10 */", "protected int outerField1 = 0;")));
            assertTrue(source.matches(PatternMaker.make(":  11 */", "protected String[] outerField2 = { \"0\" };")));

            assertNotEquals(-1, source.indexOf("final int localVariable1 = param1;"));
            assertNotEquals(-1, source.indexOf("final String[] localVariable2 = param2;"));

            assertTrue(source.matches(PatternMaker.make(":  21 */", "InnerClass innerClass = new InnerClass(param1, param2);")));
            assertTrue(source.matches(PatternMaker.make(":  22 */", "innerClass.innerMethod(localVariable1, localVariable2);")));
            assertTrue(source.matches(PatternMaker.make(":  24 */", "StaticInnerClass staticInnerClass = new StaticInnerClass(param1, param2);")));
            assertTrue(source.matches(PatternMaker.make(":  25 */", "staticInnerClass.innerMethod(localVariable1, localVariable2);")));

            assertTrue(source.matches(PatternMaker.make(":  27 */", "InnerClass anonymousClass = new InnerClass(param1, param2)")));
            assertNotEquals(-1, source.indexOf("public void innerMethod(int param1, String... param2)"));
            assertTrue(source.matches(PatternMaker.make(":  30 */", "this.innerField2 = param2;")));
            assertTrue(source.matches(PatternMaker.make(":  32 */", "OuterClass.this.outerField1 = param1;")));
            assertTrue(source.matches(PatternMaker.make(":  33 */", "OuterClass.this.outerField2 = param2;")));
            assertTrue(source.matches(PatternMaker.make(":  35 */", "this.innerField1 = localVariable1;")));
            assertTrue(source.matches(PatternMaker.make(":  36 */", "this.innerField2 = localVariable2;")));

            assertTrue(source.matches(PatternMaker.make(":  39 */", "anonymousClass.innerMethod(localVariable1, localVariable2);")));

            assertTrue(source.matches(PatternMaker.make(":  41 */", "StaticInnerClass staticAnonymousClass = new StaticInnerClass(param1, param2)")));
            assertTrue(source.matches(PatternMaker.make(":  44 */", "this.innerField2 = param2;")));
            assertTrue(source.matches(PatternMaker.make(":  46 */", "OuterClass.this.outerField1 = param1;")));
            assertTrue(source.matches(PatternMaker.make(":  47 */", "OuterClass.this.outerField2 = param2;")));
            assertTrue(source.matches(PatternMaker.make(":  49 */", "this.innerField1 = localVariable1;")));
            assertTrue(source.matches(PatternMaker.make(":  50 */", "this.innerField2 = localVariable2;")));

            assertTrue(source.matches(PatternMaker.make(":  53 */", "staticAnonymousClass.innerMethod(localVariable1, localVariable2);")));

            assertTrue(source.matches(PatternMaker.make(":  55 */", "InnerEnum.A.innerMethod(localVariable1, localVariable2);")));

            assertTrue(source.matches(PatternMaker.make("/*  56:   0 */", "class LocalClass")));
            assertTrue(source.matches(PatternMaker.make(":  58 */", "protected int innerField1 = 0;")));
            assertTrue(source.matches(PatternMaker.make(":  59 */", "protected String[] innerField2 = { \"0\" } ;")));
            assertTrue(source.matches(PatternMaker.make(":  69 */", "this.innerField1 = param1;")));
            assertTrue(source.matches(PatternMaker.make(":  70 */", "this.innerField2 = param2;")));
            assertTrue(source.matches(PatternMaker.make(":  72 */", "OuterClass.this.outerField1 = param1;")));
            assertTrue(source.matches(PatternMaker.make(":  73 */", "OuterClass.this.outerField2 = param2;")));
            assertTrue(source.matches(PatternMaker.make(":  75 */", "this.innerField1 = localVariable1;")));
            assertTrue(source.matches(PatternMaker.make(":  76 */", "this.innerField2 = localVariable2;")));
            assertTrue(source.matches(PatternMaker.make(":  94 */", "LocalClass localClass = new LocalClass(param1, param2);")));
            assertTrue(source.matches(PatternMaker.make(":  95 */", "localClass.localMethod(localVariable1, localVariable2);")));

            assertTrue(source.matches(PatternMaker.make(": 114 */", "this(param1, param2);")));
            assertTrue(source.matches(PatternMaker.make(": 144 */", "this(param1, param2);")));

            assertTrue(source.matches(PatternMaker.make(": 158 */", "A,", "B,", "C;")));
            assertNotEquals(-1, source.indexOf("/* 182: 182 */"));

            assertTrue(source.matches(PatternMaker.make("public class InnerInnerClass", "{", "}")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
