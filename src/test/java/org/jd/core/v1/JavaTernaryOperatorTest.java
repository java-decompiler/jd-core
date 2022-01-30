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

public class JavaTernaryOperatorTest extends AbstractJdTest {

    @Test
    public void testJdk118TernaryOperator() throws Exception {
        String internalClassName = "org/jd/core/test/TernaryOperator";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.1.8.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  13 */", "this.str =")));
            assertTrue(source.matches(PatternMaker.make(":  14 */", "(s == null) ?")));
            assertTrue(source.matches(PatternMaker.make(":  15 */", "\"1\"")));
            assertTrue(source.matches(PatternMaker.make(":  16 */", "\"2\";")));
            assertTrue(source.matches(PatternMaker.make(":  24 */", "s = (s == null) ? ((s == null) ? \"1\" : \"2\") : ((s == null) ? \"3\" : \"4\");")));
            assertTrue(source.matches(PatternMaker.make(":  34 */", "return !(s != s || time < time);")));
            assertTrue(source.matches(PatternMaker.make(":  40 */", "if ((s1 == null) ? (s2 == null) : s1.equals(s2))")));
            assertTrue(source.matches(PatternMaker.make(":  60 */", "if ((s1 == null) ? (s2 == null) : s1.equals(s2))")));
            assertTrue(source.matches(PatternMaker.make(":  71 */", "if ((s1 == null) ? false : (s1.length() > 0))")));
            assertTrue(source.matches(PatternMaker.make(":  82 */", "if (s1 != null && s1.length() > 0)")));
            assertTrue(source.matches(PatternMaker.make(": 126 */", "if (s1 == null && false)")));
            assertTrue(source.matches(PatternMaker.make(": 137 */", "if (s1 == s2 && ((s1 == null) ? (s2 == null) : s1.equals(s2)) && s1 == s2)")));
            assertTrue(source.matches(PatternMaker.make(": 148 */", "if (s1 == s2 || ((s1 == null) ? (s2 == null) : s1.equals(s2)) || s1 == s2)")));
            assertTrue(source.matches(PatternMaker.make(": 157 */", "return Short.toString((short)((this == null) ? 1 : 2));")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.3", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170TernaryOperator() throws Exception {
        String internalClassName = "org/jd/core/test/TernaryOperator";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  13 */", "this.str = (s == null) ? \"1\" : \"2\";")));
            assertTrue(source.matches(PatternMaker.make(":  24 */", "s = (s == null) ? ((s == null) ? \"1\" : \"2\") : ((s == null) ? \"3\" : \"4\");")));
            assertTrue(source.matches(PatternMaker.make(":  34 */", "return (s == s && time >= time);")));
            assertTrue(source.matches(PatternMaker.make(":  40 */", "if ((s1 == null) ? (s2 == null) : s1.equals(s2))")));
            assertTrue(source.matches(PatternMaker.make(":  60 */", "if ((s1 == null) ? (s2 == null) : s1.equals(s2))")));
            assertTrue(source.matches(PatternMaker.make(":  71 */", "if (s1 != null && s1.length() > 0)")));
            assertTrue(source.matches(PatternMaker.make(":  82 */", "if (s1 != null && s1.length() > 0)")));
            assertTrue(source.matches(PatternMaker.make(": 126 */", "if (s1 == null);")));
            assertTrue(source.matches(PatternMaker.make(": 137 */", "if (s1 == s2 && ((s1 == null) ? (s2 == null) : s1.equals(s2)) && s1 == s2)")));
            assertTrue(source.matches(PatternMaker.make(": 148 */", "if (s1 == s2 || ((s1 == null) ? (s2 == null) : s1.equals(s2)) || s1 == s2)")));
            assertTrue(source.matches(PatternMaker.make(": 157 */", "return Short.toString((short)((this == null) ? 1 : 2));")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
