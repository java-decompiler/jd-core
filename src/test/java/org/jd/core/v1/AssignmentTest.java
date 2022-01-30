package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.Assignment;
import org.junit.Test;

import java.io.InputStream;

public class AssignmentTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = Assignment.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/assignment-jdk8u292.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(": 8 */     (r[7]).y = (r[8]).y = (r[6]).y = (r[4]).y + (r[4]).height;")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

}
