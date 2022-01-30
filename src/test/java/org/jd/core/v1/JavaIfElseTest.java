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

public class JavaIfElseTest extends AbstractJdTest {
    @Test
    public void testJdk170IfElse() throws Exception {
        String internalClassName = "org/jd/core/test/IfElse";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("/*  12:  12 */", "if (this == null)")));
            assertTrue(source.matches(PatternMaker.make("/*  22:  22 */", "if (\"abc\".isEmpty() && \"abc\".isEmpty())")));

            assertTrue(source.matches(PatternMaker.make("/*  32:  32 */", "if (this == null)")));
            assertTrue(source.matches(PatternMaker.make("/*  34:   0 */", "} else {")));

            assertTrue(source.matches(PatternMaker.make("/*  44:  44 */", "if (this == null)")));
            assertTrue(source.matches(PatternMaker.make("/*  46:  46 */", "} else if (this == null) {")));
            assertTrue(source.matches(PatternMaker.make("/*  48:   0 */", "} else {")));

            assertTrue(source.matches(PatternMaker.make("/*  58:  58 */", "if (i == 0)")));
            assertTrue(source.matches(PatternMaker.make("/*  60:  60 */", "if (i == 1)")));

            assertTrue(source.matches(PatternMaker.make("/*  71:  71 */", "if (i == System.currentTimeMillis())")));
            assertTrue(source.matches(PatternMaker.make("/*  73:  73 */", "} else if (i != System.currentTimeMillis()) {")));
            assertTrue(source.matches(PatternMaker.make("/*  75:  75 */", "} else if (i > System.currentTimeMillis()) {")));

            assertTrue(source.matches(PatternMaker.make("/* 123: 123 */", "if (i == 4 && i == 5 && i == 6)")));

            assertTrue(source.matches(PatternMaker.make("/* 135: 135 */", "if (i == 3 || i == 5 || i == 6)")));
            assertTrue(source.matches(PatternMaker.make("/* 137: 137 */", "} else if (i != 4 && i > 7 && i > 8) {")));
            assertTrue(source.matches(PatternMaker.make("/* 139:   0 */", "} else {")));

            assertTrue(source.matches(PatternMaker.make("/* 148: 148 */", "if ((i == 1 && i == 2 && i == 3) || (i == 4 && i == 5 && i == 6) || (i == 7 && i == 8 && i == 9))")));
            assertTrue(source.matches(PatternMaker.make("/* 160: 160 */", "if ((i == 1 || i == 2 || i == 3) && (i == 4 || i == 5 || i == 6) && (i == 7 || i == 8 || i == 9))")));

            assertTrue(source.matches(PatternMaker.make("/* 172: 172 */", "if ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4))")));
            assertTrue(source.matches(PatternMaker.make("/* 184: 184 */", "if ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4))")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
