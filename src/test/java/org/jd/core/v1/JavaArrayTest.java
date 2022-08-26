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
import org.jd.core.v1.stub.InitializedArrayInTernaryOperator;
import org.junit.Test;

import java.io.InputStream;

public class JavaArrayTest extends AbstractJdTest {
    @Test
    public void testJdk150Array() throws Exception {
        String internalClassName = "org/jd/core/test/Array";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.5.0.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(": 13 */", "int[][] arrayOfInt1 = new int[1][];")));
            assertTrue(source.matches(PatternMaker.make(": 30 */", "int[][] arrayOfInt1 = { { 0, 1, 2")));

            assertTrue(source.matches(PatternMaker.make(": 52 */", "testException2(new Exception[][]", "{ { new Exception(\"1\")")));

            assertTrue(source.matches(PatternMaker.make(": 73 */", "testInt2(new int[][] { { 1,")));

            assertTrue(source.matches(PatternMaker.make(": 73 */", "testInt2(new int[][] { { 1,")));
            assertTrue(source.matches(PatternMaker.make(": 75 */", "testInt3(new int[][][] { { { 0, 1")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.5", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170Array() throws Exception {
        String internalClassName = "org/jd/core/test/Array";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(": 12 */", "int[] i1 = new int[1];")));
            assertTrue(source.matches(PatternMaker.make(": 13 */", "int[][] i2 = new int[1][];")));
            assertTrue(source.matches(PatternMaker.make(": 14 */", "int[][][] i3 = new int[1][][];")));
            assertTrue(source.matches(PatternMaker.make(": 15 */", "int[][][] i4 = new int[1][2][];")));
            assertTrue(source.matches(PatternMaker.make(": 22 */", "String[][][][] s5 = new String[1][2][][];")));

            assertTrue(source.matches(PatternMaker.make(": 26 */", "byte[] b1 = { 1, 2 } ;")));
            assertTrue(source.matches(PatternMaker.make(": 27 */", "byte[][] b2 = { { 1, 2 } } ;")));
            assertTrue(source.matches(PatternMaker.make(": 28 */", "byte[][][][] b3 = { { { 3, 4 } } } ;")));

            assertTrue(source.matches(PatternMaker.make(": 48 */", "testException1(new Exception[]", "{ new Exception(\"1\") } );")));

            assertTrue(source.matches(PatternMaker.make(": 73 */", "testInt2(new int[][]", "{ { 1 } ,")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testInitializedArrayInTernaryOperator() throws Exception {
        String internalClassName = InitializedArrayInTernaryOperator.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  5 */", "return (i == 0) ? new Class<?>[] { Object.class } : null;")));
        assertTrue(source.matches(PatternMaker.make(":  8 */", "return (i == 0) ? new Class<?>[] { Object.class, String.class, Number.class } : null;")));
        assertTrue(source.matches(PatternMaker.make(": 11 */", "return (i == 0) ? new Class[][] { { Object.class }, { String.class, Number.class } } : null;")));
        assertTrue(source.matches(PatternMaker.make(": 14 */", "return (i == 0) ? null : new Class<?>[] { Object.class };")));
        assertTrue(source.matches(PatternMaker.make(": 17 */", "return (i == 0) ? null : new Class<?>[] { Object.class, String.class, Number.class };")));
        assertTrue(source.matches(PatternMaker.make(": 20 */", "return (i == 0) ? null : new Class[][] { { Object.class }, { String.class, Number.class} };")));
        assertTrue(source.matches(PatternMaker.make(": 23 */", "return (i == 0) ? new Class<?>[] { Object.class } : new Class<?>[] { String.class, Number.class };")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
