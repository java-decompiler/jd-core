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
import org.jd.core.v1.stub.AutoboxingAndUnboxing;
import org.junit.Test;

import java.util.Collections;

public class JavaAutoboxingTest extends AbstractJdTest {
    @Test
    // https://github.com/java-decompiler/jd-core/issues/14
    public void testAutoboxing() throws Exception {
        String internalClassName = AutoboxingAndUnboxing.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName, Collections.emptyMap());

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(": 5 */", "Integer intObj = 10;")));
        assertTrue(source.matches(PatternMaker.make(": 6 */", "int i = intObj;")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
