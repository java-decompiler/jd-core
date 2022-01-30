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
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

public class JavaBasicTest extends AbstractJdTest {

    @Test
    public void testJdk170Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertNotEquals(-1, source.indexOf("serialVersionUID = 9506606333927794L;"));
            assertNotEquals(-1, source.indexOf(".indexOf('B');"));

            assertTrue(source.matches(PatternMaker.make("/*  26:  26 */", "System.out.println(\"hello\");")));

            assertNotEquals(-1, source.indexOf("String str1 = \"3 == \" + (i + 1) + \" ?\";"));
            assertNotEquals(-1, source.indexOf("String str2 = String.valueOf(\"abc \\b \\f \\n \\r \\t \\\" \\007 def\");"));
            assertNotEquals(-1, source.indexOf("char c2 = '€';"));
            assertNotEquals(-1, source.indexOf("char c3 = '\\'';"));
            assertNotEquals(-1, source.indexOf("char c4 = c3 = c2 = c1 = Character.toUpperCase('x');"));
            assertNotEquals(-1, source.indexOf("Class<String> class3 = String.class, class2 = class3, class1 = class2;"));
            assertTrue(source.matches(PatternMaker.make("Class class5 = doSomething(class6 = String.class, args1 = args2 = new String[], class4 = class5;")));
            assertTrue(source.matches(PatternMaker.make("int j = 1, k[] = {1, l[][] = {")));
            assertTrue(source.matches(PatternMaker.make("String stringNull = null;")));

            assertTrue(source.matches(PatternMaker.make(":  60 */", "return new String[] {s, s + '?'};")));

            assertNotEquals(-1, source.indexOf("if (this instanceof Object)"));

            assertNotEquals(-1, source.indexOf("int k = 50 / (25 + (i = 789));"));
            assertTrue(source.matches(PatternMaker.make(":  82 */", "k = i += 100;")));
            assertTrue(source.matches(PatternMaker.make(":  87 */", "i = ++this.int78;")));
            assertTrue(source.matches(PatternMaker.make(":  88 */", "i = this.int78++;")));
            assertTrue(source.matches(PatternMaker.make(":  89 */", "i *= 10;")));
            assertTrue(source.matches(PatternMaker.make(":  91 */", "this.int78 = ++i;")));
            assertTrue(source.matches(PatternMaker.make(":  92 */", "this.int78 = i++;")));
            assertTrue(source.matches(PatternMaker.make(":  93 */", "this.int78 *= 10;")));
            assertTrue(source.matches(PatternMaker.make(":  95 */", "long34 = ++long12;")));
            assertTrue(source.matches(PatternMaker.make(":  96 */", "long34 = long12++;")));
            assertTrue(source.matches(PatternMaker.make(":  97 */", "long34 *= 10L;")));
            assertTrue(source.matches(PatternMaker.make(":  99 */", "i = (int)long12 + this.int78;")));
            assertTrue(source.matches(PatternMaker.make(": 101 */", "i = k ^ 0xFF;")));
            assertTrue(source.matches(PatternMaker.make(": 102 */", "i |= 0x7;")));

            assertNotEquals(-1, source.indexOf("int result;"));
            assertTrue(source.matches(PatternMaker.make(": 114 */", "result = 1;")));
            assertTrue(source.matches(PatternMaker.make(": 116 */", "int k = i;")));
            assertTrue(source.matches(PatternMaker.make(": 117 */", "result = k + 2;")));
            assertTrue(source.matches(PatternMaker.make(": 120 */", "result = this.short56;")));
            assertTrue(source.matches(PatternMaker.make(": 124 */", "return result;")));
            assertTrue(source.matches(PatternMaker.make(": 128 */", "int int78 = getInt78(new Object[] { this }, (short)5);")));
            assertTrue(source.matches(PatternMaker.make(": 130 */", "i = (int)(Basic.long12 + long12) + this.int78 + int78;")));

            assertNotEquals(-1, source.indexOf("public static native int read();"));

            assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "return str + str;")));
            assertTrue(source.matches(PatternMaker.make("/* 176: 176 */", "return str;")));

            assertTrue(source.matches(PatternMaker.make("/* 185: 185 */", "return ((Basic)objects[index]).int78;")));

            assertTrue(source.matches(PatternMaker.make("/* 188: 188 */", "protected static final Integer INTEGER_255 = new Integer(255);")));

            assertEquals(-1, source.indexOf("<init>()"));
            assertEquals(-1, source.indexOf("NaND"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170NoDebugInfoBasic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0-no-debug-info.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("System.out.println(\"hello\");")));

            assertTrue(source.matches(PatternMaker.make("String str1 = \"3 == \" + (paramInt + 1) + \" ?\";")));
            assertNotEquals(-1, source.indexOf("String str2 = String.valueOf(\"abc \\b \\f \\n \\r \\t \\\" \\007 def\");"));
            assertTrue(source.matches(PatternMaker.make("int j = 8364;")));
            assertTrue(source.matches(PatternMaker.make("int m = k = j = i = Character.toUpperCase('x');")));
            assertTrue(source.matches(PatternMaker.make("Class<String> clazz3 = String.class;")));
            assertTrue(source.matches(PatternMaker.make("Class<String> clazz2 = clazz3;")));
            assertTrue(source.matches(PatternMaker.make("Class<String> clazz1 = clazz2;")));
            assertNotEquals(-1, source.indexOf("Class clazz5 = doSomething(clazz6 = String.class, arrayOfString1 = arrayOfString2 = new String[]"));

            assertTrue(source.matches(PatternMaker.make("if (this instanceof Object)")));

            assertTrue(source.matches(PatternMaker.make("this.int78 = 50 / (25 + (this.int78 = 789));")));

            assertNotEquals(-1, source.indexOf("protected static final Integer INTEGER_255 = new Integer(255);"));

            assertEquals(-1, source.indexOf("<init>()"));
            assertEquals(-1, source.indexOf("NaND"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170Constructors() throws Exception {
        String internalClassName = "org/jd/core/test/Constructors";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(": 28 */", "this.short123 = 1;")));

            assertTrue(source.matches(PatternMaker.make(": 32 */", "super(short56);")));
            assertTrue(source.matches(PatternMaker.make(": 33 */", "this.short123 = 2;")));

            assertTrue(source.matches(PatternMaker.make(": 37 */", "this(short56);")));
            assertTrue(source.matches(PatternMaker.make(": 38 */", "this.int78 = int78;")));
            assertTrue(source.matches(PatternMaker.make(": 39 */", "this.short123 = 3;")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170Interface() throws Exception {
        String internalClassName = "org/jd/core/test/Interface";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("public interface Interface", "extends Serializable")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk118Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.1.8.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  43 */", "Class class3 = String.class, class2 = class3, class1 = class2;")));
            assertTrue(source.matches(PatternMaker.make("String stringNull = null;")));
            assertNotEquals(-1, source.indexOf("public static native int read();"));
            assertTrue(source.matches(PatternMaker.make(": 128 */", "int int78 = getInt78(new Object[] { this }, (short)5);")));
            assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "return String.valueOf(str) + str;")));
            assertTrue(source.matches(PatternMaker.make("/* 176: 176 */", "return str;")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.3", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk142Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.4.2.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  18 */", "protected short short56 = 56;")));
            assertTrue(source.matches(PatternMaker.make(":  19 */", "protected int int78 = 78;")));
            assertTrue(source.matches(PatternMaker.make(":  43 */", "Class class3 = String.class, class2 = class3, class1 = class2;")));
            assertTrue(source.matches(PatternMaker.make("String stringNull = null;")));
            assertNotEquals(-1, source.indexOf("public static native int read();"));
            assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "return str + str;")));
            assertTrue(source.matches(PatternMaker.make("/* 176: 176 */", "return str;")));
            assertTrue(source.matches(PatternMaker.make("/* 185: 185 */", "return ((Basic)objects[index]).int78;")));
            assertTrue(source.matches(PatternMaker.make("/* 188: 188 */", "protected static final Integer INTEGER_255 = new Integer(255);")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.4", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk901Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-9.0.1.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  18 */", "protected short short56 = 56;")));
            assertTrue(source.matches(PatternMaker.make(":  19 */", "protected int int78 = 78;")));
            assertTrue(source.matches(PatternMaker.make(":  43 */", "Class<String> class3 = String.class, class2 = class3, class1 = class2;")));
            assertTrue(source.matches(PatternMaker.make("String stringNull = null;")));
            assertNotEquals(-1, source.indexOf("public static native int read();"));
            assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "return str + str;")));
            assertTrue(source.matches(PatternMaker.make("/* 176: 176 */", "return str;")));
            assertTrue(source.matches(PatternMaker.make("/* 185: 185 */", "return ((Basic)objects[index]).int78;")));
            assertTrue(source.matches(PatternMaker.make("/* 188: 188 */", "protected static final Integer INTEGER_255 = new Integer(255);")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk1002Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-10.0.2.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  18 */", "protected short short56 = 56;")));
            assertTrue(source.matches(PatternMaker.make(":  19 */", "protected int int78 = 78;")));
            assertTrue(source.matches(PatternMaker.make(":  43 */", "Class<String> class3 = String.class, class2 = class3, class1 = class2;")));
            assertTrue(source.matches(PatternMaker.make("String stringNull = null;")));
            assertNotEquals(-1, source.indexOf("public static native int read();"));
            assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "return str + str;")));
            assertTrue(source.matches(PatternMaker.make("/* 176: 176 */", "return str;")));
            assertTrue(source.matches(PatternMaker.make("/* 185: 185 */", "return ((Basic)objects[index]).int78;")));
            assertTrue(source.matches(PatternMaker.make("/* 188: 188 */", "protected static final Integer INTEGER_255 = new Integer(255);")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    // Test initializer block
    public void testAnnotationUtils() throws Exception {
        String internalClassName = "org/apache/commons/lang3/AnnotationUtils";
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName, configuration);

        // Check decompiled source code
        assertNotEquals(-1, source.indexOf("setDefaultFullDetail(true);"));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
