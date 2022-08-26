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

import static org.junit.Assert.assertNotEquals;

public class JavaLambdaTest extends AbstractJdTest {

    @Test
    public void testJdk180Lambda() throws Exception {
        String internalClassName = "org/jd/core/test/Lambda";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.8.0.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(": 16 */", "list.forEach(System.out::println);")));
            assertTrue(source.matches(PatternMaker.make(": 20 */", "list.stream().filter(s -> (s != null)).forEach(s -> System.out.println(s));")));
            assertNotEquals(-1, source.indexOf("Predicate<String> filter = s -> (s.length() == length);"));
            assertNotEquals(-1, source.indexOf("Consumer<String> println = s -> System.out.println(s);"));
            assertTrue(source.matches(PatternMaker.make(": 27 */", "list.stream().filter(filter).forEach(println);")));
            assertTrue(source.matches(PatternMaker.make(": 31 */", "((Map)list.stream()")));
            assertTrue(source.matches(PatternMaker.make(": 32 */", ".collect(Collectors.toMap(lambda -> lambda.index, Function.identity())))")));
            assertTrue(source.matches(PatternMaker.make(": 33 */", ".forEach((key, value) ->")));
            assertTrue(source.matches(PatternMaker.make(": 48 */", "Thread thread = new Thread(() -> {")));
            assertTrue(source.matches(PatternMaker.make(": 58 */", "Consumer<String> staticMethodReference = String::valueOf;")));
            assertTrue(source.matches(PatternMaker.make(": 59 */", "BiFunction<String, String, Integer> methodReference = String::compareTo;")));
            assertTrue(source.matches(PatternMaker.make(": 60 */", "Supplier<String> instanceMethodReference = s::toString;")));
            assertTrue(source.matches(PatternMaker.make(": 61 */", "Supplier<String> constructorReference = String::new;")));
            assertTrue(source.matches(PatternMaker.make(": 65 */", "MethodType mtToString = MethodType.methodType(String.class);")));
            assertTrue(source.matches(PatternMaker.make(": 66 */", "MethodType mtSetter = MethodType.methodType(void.class, Object.class);")));
            assertTrue(source.matches(PatternMaker.make(": 67 */", "MethodType mtStringComparator = MethodType.methodType(int[].class, String.class, String.class);")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
