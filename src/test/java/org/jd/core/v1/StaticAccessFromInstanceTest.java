package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.JavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.StaticAccessFromInstance;
import org.junit.Test;

import java.io.InputStream;

public class StaticAccessFromInstanceTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = StaticAccessFromInstance.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/static-access-from-instance-jdk8u292.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(": 14 */     Objects objects = null;")));
            assertTrue(source.matches(PatternMaker.make(": 15 */     if (this.name != null) {")));
            assertTrue(source.matches(PatternMaker.make(": 16 */       objects = this.values.get(this.name);")));
            assertTrue(source.matches(PatternMaker.make(": 17 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 18 */         System.err.println(\"Error !!\"); ")));
            assertTrue(source.matches(PatternMaker.make(": 21 */       objects = this.values.get(this.value);")));
            assertTrue(source.matches(PatternMaker.make(": 22 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 23 */         System.err.println(\"Error !!!\"); ")));
            assertTrue(source.matches(PatternMaker.make(": 26 */     return objects;")));
            assertTrue(source.matches(PatternMaker.make(": 31 */     Objects objects = null;")));
            assertTrue(source.matches(PatternMaker.make(": 32 */     if (this.name != null) {")));
            assertTrue(source.matches(PatternMaker.make(": 33 */       objects = this.values.get(this.name);")));
            assertTrue(source.matches(PatternMaker.make(": 34 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 35 */         System.err.println(\"Error !!\"); ")));
            assertTrue(source.matches(PatternMaker.make(": 38 */       objects = this.values.get(this.value);")));
            assertTrue(source.matches(PatternMaker.make(": 39 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 40 */         System.err.println(\"Error !!!\"); ")));
            assertTrue(source.matches(PatternMaker.make(": 43 */     return objects;")));
            assertTrue(source.matches(PatternMaker.make(": 48 */     Objects objects = null;")));
            assertTrue(source.matches(PatternMaker.make(": 49 */     if (this.name != null) {")));
            assertTrue(source.matches(PatternMaker.make(": 50 */       objects = this.values.get(this.name);")));
            assertTrue(source.matches(PatternMaker.make(": 51 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 52 */         System.err.println(\"Error !!\"); ")));
            assertTrue(source.matches(PatternMaker.make(": 55 */       objects = this.values.get(this.value);")));
            assertTrue(source.matches(PatternMaker.make(": 56 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 57 */         System.err.println(\"Error !!!\"); ")));
            assertTrue(source.matches(PatternMaker.make(": 60 */     return objects;")));
            assertTrue(source.matches(PatternMaker.make(": 65 */     Objects objects = null;")));
            assertTrue(source.matches(PatternMaker.make(": 66 */     if (this.name != null) {")));
            assertTrue(source.matches(PatternMaker.make(": 67 */       objects = this.values.get(this.name);")));
            assertTrue(source.matches(PatternMaker.make(": 68 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 69 */         System.err.println(\"Error !!\"); ")));
            assertTrue(source.matches(PatternMaker.make(": 72 */       objects = this.values.get(this.value);")));
            assertTrue(source.matches(PatternMaker.make(": 73 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 74 */         System.err.println(\"Error !!!\"); ")));
            assertTrue(source.matches(PatternMaker.make(": 77 */     return objects;")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
        }
    }
}
