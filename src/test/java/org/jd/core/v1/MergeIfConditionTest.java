package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.JavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.MergeIfCondition;
import org.junit.Test;

import java.io.InputStream;

public class MergeIfConditionTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = MergeIfCondition.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/merge-if-condition-jdk8u292.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  9 */     if (Point2D.distance(p1.getX(), p1.getY(), p2.getX(), p2.getY()) == 0.0D && ")));
            assertTrue(source.matches(PatternMaker.make(": 10 */       Point2D.distance(p2.getX(), p2.getY(), p1.getX(), p1.getY()) == 0.0D)")));
            assertTrue(source.matches(PatternMaker.make(": 11 */       return 0.0D; ")));
            assertTrue(source.matches(PatternMaker.make(": 12 */     return p1.distance(p2);")));
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
        }
    }
}
