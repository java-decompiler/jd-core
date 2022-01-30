package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.StringConcatenation;
import org.junit.Test;

import java.io.InputStream;

public class StringConcatenationTest extends AbstractJdTest {
    @Test
    public void testStringConcatenation() throws Exception {
        String internalClassName = StringConcatenation.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/string-concatenation-jdk8u292.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  0 */ package org.jd.core.v1.stub;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */ ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */ public class StringConcatenation {")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   public static final String SEMI_COLON = \";\";")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   public String toString(Object o1, Object o2) {")));
            assertTrue(source.matches(PatternMaker.make(":  9 */     boolean b1 = (o1 != null);")));
            assertTrue(source.matches(PatternMaker.make(": 10 */     boolean b2 = (o2 != null);")));
            assertTrue(source.matches(PatternMaker.make(": 11 */     String insert = \"INSERT INTO EMPLOYEE(first_name,last_name,address,phone\" + (b1 ? \"\" : \",b1\") + (")));
            assertTrue(source.matches(PatternMaker.make(": 12 */       b2 ? \"\" : \",b2\") + \") VALUES(?,?,?,?\" + (b1 ? \"\" : \",?\") + (")));
            assertTrue(source.matches(PatternMaker.make(": 13 */       b2 ? \"\" : \",?\") + \")\" + \";\";")));
            assertTrue(source.matches(PatternMaker.make(": 15 */     return insert;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */ }")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
