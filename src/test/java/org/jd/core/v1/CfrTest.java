/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.FloatingPointCasting;
import org.junit.Test;

import java.util.Collections;

public class CfrTest extends AbstractJdTest {

    @Test
    // https://github.com/java-decompiler/jd-core/issues/34
    public void testFloatingPointCasting() throws Exception {
        String internalClassName = FloatingPointCasting.class.getName().replace('.', '/');
        String source = decompile(new ClassPathLoader(), new PlainTextPrinter(), internalClassName, Collections.emptyMap());
        assertEquals(-1, source.indexOf("// Byte code:"));

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(": 12 */", "long b = (long)(double)")));
        assertTrue(source.matches(PatternMaker.make(": 16 */", "long b = Long.MAX_VALUE")));
        assertTrue(source.matches(PatternMaker.make(": 20 */", "long b = (long)(double)")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
