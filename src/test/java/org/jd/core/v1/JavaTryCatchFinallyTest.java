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

public class JavaTryCatchFinallyTest extends AbstractJdTest {
    @Test
    public void testJdk170TryWithResources() throws Exception {
        String internalClassName = "org/jd/core/test/TryWithResources";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  12 */", "try (FileInputStream input = new FileInputStream(path))")));

            assertTrue(source.matches(PatternMaker.make(":  49 */", "try (FileInputStream input = new FileInputStream(path))")));
            assertTrue(source.matches(PatternMaker.make(":  57 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(":  59 */", "System.out.println(\"finally\");")));

            assertTrue(source.matches(PatternMaker.make(": 121 */", "try(FileInputStream input = new FileInputStream(pathIn);")));
            assertTrue(source.matches(PatternMaker.make(": 122 */", "BufferedInputStream bufferedInput = new BufferedInputStream(input);")));
            assertTrue(source.matches(PatternMaker.make(": 123 */", "FileOutputStream output = new FileOutputStream(pathOut);")));
            assertTrue(source.matches(PatternMaker.make(": 124 */", "BufferedOutputStream bufferedOutput = new BufferedOutputStream(output))")));
            assertTrue(source.matches(PatternMaker.make(": 132 */", "if (data == -7)")));
            assertTrue(source.matches(PatternMaker.make(": 133 */", "return 1;")));
            assertTrue(source.matches(PatternMaker.make(": 142 */", "return 2;")));
            assertTrue(source.matches(PatternMaker.make(": 144 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(": 150 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(": 152 */", "System.out.println(\"finally, before loop\");")));
            assertTrue(source.matches(PatternMaker.make(": 156 */", "System.out.println(\"finally, after loop\");")));
            assertTrue(source.matches(PatternMaker.make(": 159 */", "System.out.println(\"finally\");")));
            assertTrue(source.matches(PatternMaker.make(": 162 */", "return 3;")));

            assertNotEquals(-1, source.indexOf("/* 162: 162 */"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk180TryWithResources() throws Exception {
        String internalClassName = "org/jd/core/test/TryWithResources";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.8.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  12 */", "try (FileInputStream input = new FileInputStream(path))")));

            assertTrue(source.matches(PatternMaker.make(":  49 */", "try (FileInputStream input = new FileInputStream(path))")));
            assertTrue(source.matches(PatternMaker.make(":  57 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(":  59 */", "System.out.println(\"finally\");")));

            assertTrue(source.matches(PatternMaker.make(": 121 */", "try(FileInputStream input = new FileInputStream(pathIn);")));
            assertTrue(source.matches(PatternMaker.make(": 122 */", "BufferedInputStream bufferedInput = new BufferedInputStream(input);")));
            assertTrue(source.matches(PatternMaker.make(": 123 */", "FileOutputStream output = new FileOutputStream(pathOut);")));
            assertTrue(source.matches(PatternMaker.make(": 124 */", "BufferedOutputStream bufferedOutput = new BufferedOutputStream(output))")));
            assertTrue(source.matches(PatternMaker.make(": 132 */", "if (data == -7)")));
            assertTrue(source.matches(PatternMaker.make(": 133 */", "return 1;")));
            assertTrue(source.matches(PatternMaker.make(": 142 */", "return 2;")));
            assertTrue(source.matches(PatternMaker.make(": 144 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(": 150 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(": 152 */", "System.out.println(\"finally, before loop\");")));
            assertTrue(source.matches(PatternMaker.make(": 156 */", "System.out.println(\"finally, after loop\");")));
            assertTrue(source.matches(PatternMaker.make(": 159 */", "System.out.println(\"finally\");")));
            assertTrue(source.matches(PatternMaker.make(": 162 */", "return 3;")));

            assertNotEquals(-1, source.indexOf("/* 162: 162 */"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk11TryWithResources() throws Exception {
        String internalClassName = "org/jd/core/test/TryWithResources";
        try (InputStream is = this.getClass().getResourceAsStream("/jar/try-resources-jdk-11.0.12.jar")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);
            
            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  12 */", "try (FileInputStream input = new FileInputStream(path))")));

            assertTrue(source.matches(PatternMaker.make(":  49 */", "try (FileInputStream input = new FileInputStream(path))")));
            assertTrue(source.matches(PatternMaker.make(":  57 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(":  59 */", "System.out.println(\"finally\");")));

            assertTrue(source.matches(PatternMaker.make(": 121 */", "try(FileInputStream input = new FileInputStream(pathIn);")));
            assertTrue(source.matches(PatternMaker.make(": 122 */", "BufferedInputStream bufferedInput = new BufferedInputStream(input);")));
            assertTrue(source.matches(PatternMaker.make(": 123 */", "FileOutputStream output = new FileOutputStream(pathOut);")));
            assertTrue(source.matches(PatternMaker.make(": 124 */", "BufferedOutputStream bufferedOutput = new BufferedOutputStream(output))")));
            assertTrue(source.matches(PatternMaker.make(": 132 */", "if (data == -7)")));
            assertTrue(source.matches(PatternMaker.make(": 133 */", "return 1;")));
            assertTrue(source.matches(PatternMaker.make(": 142 */", "return 2;")));
            assertTrue(source.matches(PatternMaker.make(": 144 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(": 150 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(": 152 */", "System.out.println(\"finally, before loop\");")));
            assertTrue(source.matches(PatternMaker.make(": 156 */", "System.out.println(\"finally, after loop\");")));
            assertTrue(source.matches(PatternMaker.make(": 159 */", "System.out.println(\"finally\");")));
            assertTrue(source.matches(PatternMaker.make(": 162 */", "return 3;")));

            assertNotEquals(-1, source.indexOf("/* 162: 162 */"));
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("11", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
    
    @Test
    public void testJdk17TryWithResources() throws Exception {
        String internalClassName = "org/jd/core/test/TryWithResources";
        try (InputStream is = this.getClass().getResourceAsStream("/jar/try-resources-jdk-17.0.1.jar")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);
            
            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  12 */", "try (FileInputStream input = new FileInputStream(path))")));
            
            assertTrue(source.matches(PatternMaker.make(":  49 */", "try (FileInputStream input = new FileInputStream(path))")));
            assertTrue(source.matches(PatternMaker.make(":  57 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(":  59 */", "System.out.println(\"finally\");")));
            
            assertTrue(source.matches(PatternMaker.make(": 121 */", "try(FileInputStream input = new FileInputStream(pathIn);")));
            assertTrue(source.matches(PatternMaker.make(": 122 */", "BufferedInputStream bufferedInput = new BufferedInputStream(input);")));
            assertTrue(source.matches(PatternMaker.make(": 123 */", "FileOutputStream output = new FileOutputStream(pathOut);")));
            assertTrue(source.matches(PatternMaker.make(": 124 */", "BufferedOutputStream bufferedOutput = new BufferedOutputStream(output))")));
            assertTrue(source.matches(PatternMaker.make(": 132 */", "if (data == -7)")));
            assertTrue(source.matches(PatternMaker.make(": 133 */", "return 1;")));
            assertTrue(source.matches(PatternMaker.make(": 142 */", "return 2;")));
            assertTrue(source.matches(PatternMaker.make(": 144 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(": 150 */", "e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(": 152 */", "System.out.println(\"finally, before loop\");")));
            assertTrue(source.matches(PatternMaker.make(": 156 */", "System.out.println(\"finally, after loop\");")));
            assertTrue(source.matches(PatternMaker.make(": 159 */", "System.out.println(\"finally\");")));
            assertTrue(source.matches(PatternMaker.make(": 162 */", "return 3;")));
            
            assertNotEquals(-1, source.indexOf("/* 162: 162 */"));
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("11", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
    
    @Test
    public void testEclipseJavaCompiler321TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertNotEquals(-1, source.indexOf("catch (RuntimeException runtimeexception)"));
            assertTrue(source.matches(PatternMaker.make("/*  45:  45 */", "inCatch1();")));
            assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

            assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

            // TODO assertTrue(source.matches(PatternMaker.make("/* 217:", "inCatch1();")));

            assertNotEquals(-1, source.indexOf("/* 888: 888 */"));

            assertEquals(-1, source.indexOf("long l = System.currentTimeMillis(); return l;"));
            assertEquals(-1, source.indexOf("catch (RuntimeException null)"));
            assertEquals(-1, source.indexOf("Object object;"));
            assertEquals(-1, source.indexOf("RuntimeException runtimeexception4;"));
            assertEquals(-1, source.indexOf("Exception exception8;"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.5", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
        System.out.println(JavaTryCatchFinallyTest.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        System.out.println(System.getProperty("java.class.path"));
    }

    @Test
    public void testEclipseJavaCompiler370TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertNotEquals(-1, source.indexOf("catch (RuntimeException runtimeException)"));
            assertTrue(source.matches(PatternMaker.make("/*  48:  48 */", "inCatch2();")));
            assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

            assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

            assertTrue(source.matches(PatternMaker.make(": 393 */", "inCatch1();")));
            assertTrue(source.matches(PatternMaker.make(": 395 */", "inCatch2();")));
            assertTrue(source.matches(PatternMaker.make(": 397 */", "inCatch3();")));
            assertTrue(source.matches(PatternMaker.make(": 399 */", "inFinally();")));
            assertEquals(-1, source.indexOf("/* 400:   0]     inFinally();"));

            assertTrue(source.matches(PatternMaker.make(": 424 */", "inTry();")));
            assertTrue(source.matches(PatternMaker.make(": 427 */", "inFinally();")));
            assertTrue(source.matches(PatternMaker.make(": 431 */", "inTryA();")));
            assertTrue(source.matches(PatternMaker.make(": 434 */", "inFinallyA();")));
            assertTrue(source.matches(PatternMaker.make(": 439 */", "inTryC();")));
            assertTrue(source.matches(PatternMaker.make(": 442 */", "inFinallyC();")));
            assertTrue(source.matches(PatternMaker.make(": 445 */", "inFinally();")));

            assertNotEquals(-1, source.indexOf("/* 888: 888 */"));

            assertEquals(-1, source.indexOf("long l = System.currentTimeMillis(); return l;"));
            assertEquals(-1, source.indexOf("catch (RuntimeException null)"));
            assertEquals(-1, source.indexOf("Object object;"));
            assertEquals(-1, source.indexOf("RuntimeException runtimeexception4;"));
            assertEquals(-1, source.indexOf("Exception exception8;"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.5", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testEclipseJavaCompiler3130TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertNotEquals(-1, source.indexOf("catch (RuntimeException runtimeException)"));
            assertTrue(source.matches(PatternMaker.make("/*  48:  48 */", "inCatch2();")));
            assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

            assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

            assertTrue(source.matches(PatternMaker.make(": 393 */", "inCatch1();")));
            assertTrue(source.matches(PatternMaker.make(": 395 */", "inCatch2();")));
            assertTrue(source.matches(PatternMaker.make(": 397 */", "inCatch3();")));
            assertTrue(source.matches(PatternMaker.make(": 399 */", "inFinally();")));

            assertTrue(source.matches(PatternMaker.make(": 424 */", "inTry();")));
            assertTrue(source.matches(PatternMaker.make(": 427 */", "inFinally();")));
            assertTrue(source.matches(PatternMaker.make(": 431 */", "inTryA();")));
            assertTrue(source.matches(PatternMaker.make(": 434 */", "inFinallyA();")));
            assertTrue(source.matches(PatternMaker.make(": 439 */", "inTryC();")));
            assertTrue(source.matches(PatternMaker.make(": 442 */", "inFinallyC();")));
            assertTrue(source.matches(PatternMaker.make(": 445 */", "inFinally();")));

            assertNotEquals(-1, source.indexOf("/* 888: 888 */"));

            assertEquals(-1, source.indexOf("long l = System.currentTimeMillis(); return l;"));
            assertEquals(-1, source.indexOf("catch (RuntimeException null)"));
            assertEquals(-1, source.indexOf("Object object;"));
            assertEquals(-1, source.indexOf("RuntimeException runtimeexception4;"));
            assertEquals(-1, source.indexOf("Exception exception8;"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.5", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk118TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.1.8.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertNotEquals(-1, source.indexOf("catch (RuntimeException runtimeexception)"));
            assertTrue(source.matches(PatternMaker.make("/*  48:  48 */", "inCatch2();")));
            assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

            assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

            assertTrue(source.matches(PatternMaker.make(": 324 */", "return b;")));
            assertTrue(source.matches(PatternMaker.make(": 393 */", "inCatch1();")));
            assertTrue(source.matches(PatternMaker.make(": 395 */", "inCatch2();")));
            assertTrue(source.matches(PatternMaker.make(": 397 */", "inCatch3();")));
            assertTrue(source.matches(PatternMaker.make(": 399 */", "inFinally();")));

            assertTrue(source.matches(PatternMaker.make(": 424 */", "inTry();")));
            assertTrue(source.matches(PatternMaker.make(": 427 */", "inFinally();")));
            assertTrue(source.matches(PatternMaker.make(": 431 */", "inTryA();")));
            assertTrue(source.matches(PatternMaker.make(": 434 */", "inFinallyA();")));
            assertTrue(source.matches(PatternMaker.make(": 439 */", "inTryC();")));
            assertTrue(source.matches(PatternMaker.make(": 442 */", "inFinallyC();")));
            assertTrue(source.matches(PatternMaker.make(": 445 */", "inFinally();")));

            assertNotEquals(-1, source.indexOf("/* 902: 902 */"));

            assertEquals(-1, source.indexOf("long l = System.currentTimeMillis(); return l;"));
            assertEquals(-1, source.indexOf("catch (RuntimeException null)"));
            assertEquals(-1, source.indexOf("Object object;"));
            assertEquals(-1, source.indexOf("RuntimeException runtimeexception4;"));
            assertEquals(-1, source.indexOf("Exception exception8;"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.3", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk131TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.3.1.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertNotEquals(-1, source.indexOf("catch (RuntimeException runtimeexception)"));
            assertTrue(source.matches(PatternMaker.make("/*  48:  48 */", "inCatch2();")));
            assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

            assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

            assertTrue(source.matches(PatternMaker.make(": 393 */", "inCatch1();")));
            assertTrue(source.matches(PatternMaker.make(": 395 */", "inCatch2();")));
            assertTrue(source.matches(PatternMaker.make(": 397 */", "inCatch3();")));
            assertTrue(source.matches(PatternMaker.make(": 399 */", "inFinally();")));

            assertTrue(source.matches(PatternMaker.make(": 424 */", "inTry();")));
            assertTrue(source.matches(PatternMaker.make(": 427 */", "inFinally();")));
            assertTrue(source.matches(PatternMaker.make(": 445 */", "inFinally();")));

            assertNotEquals(-1, source.indexOf("/* 902: 902 */"));

            assertEquals(-1, source.indexOf("long l = System.currentTimeMillis(); return l;"));
            assertEquals(-1, source.indexOf("catch (RuntimeException null)"));
            assertEquals(-1, source.indexOf("Object object;"));
            assertEquals(-1, source.indexOf("RuntimeException runtimeexception4;"));
            assertEquals(-1, source.indexOf("Exception exception8;"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.3", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertNotEquals(-1, source.indexOf("catch (RuntimeException runtimeexception)"));
            assertTrue(source.matches(PatternMaker.make("/*  48:  48 */", "inCatch2();")));
            assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

            assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

            assertTrue(source.matches(PatternMaker.make(": 192 */", "catch (RuntimeException e) {}")));
            assertTrue(source.matches(PatternMaker.make("/* 204:   0 */", "finally {}")));

            assertTrue(source.matches(PatternMaker.make(": 324 */", "return b;")));
            assertTrue(source.matches(PatternMaker.make(": 393 */", "inCatch1();")));
            assertTrue(source.matches(PatternMaker.make(": 395 */", "inCatch2();")));
            assertTrue(source.matches(PatternMaker.make(": 397 */", "inCatch3();")));
            assertTrue(source.matches(PatternMaker.make(": 399 */", "inFinally();")));

            assertTrue(source.matches(PatternMaker.make(": 424 */", "inTry();")));
            assertTrue(source.matches(PatternMaker.make(": 427 */", "inFinally();")));
            assertTrue(source.matches(PatternMaker.make(": 431 */", "inTryA();")));
            assertTrue(source.matches(PatternMaker.make(": 434 */", "inFinallyA();")));
            assertTrue(source.matches(PatternMaker.make(": 439 */", "inTryC();")));
            assertTrue(source.matches(PatternMaker.make(": 442 */", "inFinallyC();")));
            assertTrue(source.matches(PatternMaker.make(": 445 */", "inFinally();")));

            assertNotEquals(-1, source.indexOf("/* 902: 902 */"));

            assertEquals(-1, source.indexOf("long l = System.currentTimeMillis(); return l;"));
            assertEquals(-1, source.indexOf("catch (RuntimeException null)"));
            assertEquals(-1, source.indexOf("Object object;"));
            assertEquals(-1, source.indexOf("RuntimeException runtimeexception4;"));
            assertEquals(-1, source.indexOf("Exception exception8;"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
