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

public class StringMapTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = StringMap.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/string-map-jdk8u331.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, Collections.singletonMap("realignLineNumbers", "true"));

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("private static TriConsumer<String, String, Map<String, String>> PUT_ALL = (key, value, stringStringMap) -> stringStringMap.put(key, value);")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
