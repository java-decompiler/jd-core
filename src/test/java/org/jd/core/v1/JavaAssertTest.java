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

public class JavaAssertTest extends AbstractJdTest {

    @Test
    public void testJdk170Assert() throws Exception {
        String internalClassName = "org/jd/core/test/Assert";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("/* 16: 16 */", "assert false : \"false\";")));
            assertTrue(source.matches(PatternMaker.make("/* 17: 17 */", "assert i == 0 || i == 1;")));
            assertTrue(source.matches(PatternMaker.make("/* 18: 18 */", "assert i == 2 && i < 3;")));

            assertTrue(source.matches(PatternMaker.make("/* 34: 34 */", "assert new BigDecimal(i) == BigDecimal.ONE;")));

            assertTrue(source.matches(PatternMaker.make("/* 41: 41 */", "assert check() : \"boom\";")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk150Assert() throws Exception {
        String internalClassName = "org/jd/core/test/Assert";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.5.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("/* 16: 16 */", "assert false : \"false\";")));
            assertTrue(source.matches(PatternMaker.make("/* 17: 17 */", "assert paramInt == 0 || paramInt == 1;")));
            assertTrue(source.matches(PatternMaker.make("/* 18: 18 */", "assert paramInt == 2 && paramInt < 3;")));

            assertTrue(source.matches(PatternMaker.make("/* 34: 34 */", "assert new BigDecimal(paramInt) == BigDecimal.ONE;")));

            assertTrue(source.matches(PatternMaker.make("/* 41: 41 */", "assert check() : \"boom\";")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.5", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
