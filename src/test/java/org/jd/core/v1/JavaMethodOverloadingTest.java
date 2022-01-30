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
import org.jd.core.v1.stub.ArrayMethodOverloading;
import org.junit.Test;

public class JavaMethodOverloadingTest extends AbstractJdTest {
    @Test
    // https://github.com/java-decompiler/jd-core/issues/33
    public void testArrayMethodOverloading() throws Exception {
        String internalClassName = ArrayMethodOverloading.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(": 11 */", "use(\"string\");")));
        assertTrue(source.matches(PatternMaker.make(": 15 */", "use((Object)new Object[] { \"\" });")));
        assertTrue(source.matches(PatternMaker.make(": 19 */", "use((Object[])null);")));
        assertTrue(source.matches(PatternMaker.make(": 23 */", "use((Object)null);")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    // TODO: bug should be fix
//    @Test
    // https://github.com/java-decompiler/jd-core/issues/32
//    public void testGenericParameterMethod() throws Exception {
//        class GenericParameterMethod {
//            /* static */ void use(Integer i) {
//                System.out.println("use(Integer)");
//            }
//            /* static */ <T> void use(T t) {
//                System.out.println("use(T)");
//            }
//
//            public /* static */ void main(String... args) {
//                use(1);
//                use((Object) 1); // Calls use(T)
//            }
//        }
//
//        String internalClassName = GenericParameterMethod.class.getName().replace('.', '/');
//        String source = decompile(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
//
//        // Check decompiled source code
//        assertTrue(source.matches(PatternMaker.make(": 85 */", "use(1);")));
//        assertTrue(source.matches(PatternMaker.make(": 86 */", "use((Object)1);")));
//
//        // Recompile decompiled source code and check errors
//        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
//    }

}
