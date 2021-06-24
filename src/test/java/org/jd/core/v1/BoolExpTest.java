package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.JavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.BoolExp;
import org.junit.Test;

import java.io.InputStream;

public class BoolExpTest extends AbstractJdTest {
    @Test
    public void testBoolExp() throws Exception {
        String internalClassName = BoolExp.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/bool-exp-jdk8u292.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled output
            assertTrue(source.matches(PatternMaker.make(": 25 */     if (this.field1 == null || \"\".equals(this.field1))")));
            assertTrue(source.matches(PatternMaker.make(": 26 */       v.add(new String(\"Field #1 is not valid\")); ")));
            assertTrue(source.matches(PatternMaker.make(": 29 */     if (this.field2 == null || \"\".equals(this.field2))")));
            assertTrue(source.matches(PatternMaker.make(": 30 */       v.add(new String(\"Field #2 is not valid\")); ")));
            assertTrue(source.matches(PatternMaker.make(": 33 */     if (this.field3 == null || \"\".equals(this.field3))")));
            assertTrue(source.matches(PatternMaker.make(": 34 */       v.add(new String(\"Field #3 is not valid\")); ")));
            assertTrue(source.matches(PatternMaker.make(": 37 */     if (this.field4 == null || \"\".equals((this.field4 == null)))")));
            assertTrue(source.matches(PatternMaker.make(": 38 */       v.add(new String(\"Field #4 is not valid\")); ")));
            assertTrue(source.matches(PatternMaker.make(": 41 */     if (this.field5 == null || \"\".equals(this.field5))")));
            assertTrue(source.matches(PatternMaker.make(": 42 */       v.add(new String(\"Field #5 is not valid\")); ")));
            assertTrue(source.matches(PatternMaker.make(": 45 */     if (this.field6 == null || \"\".equals(this.field6))")));
            assertTrue(source.matches(PatternMaker.make(": 46 */       v.add(new String(\"Field #6 is not valid\")); ")));
            assertTrue(source.matches(PatternMaker.make(": 49 */     return v.isEmpty();")));

            assertTrue(source.matches(PatternMaker.make(": 53 */     toolBar.add(button, (Math.random() == 0.0D && toolBar.getComponentCount() > ((Math.random() > 0.5D) ? 1 : 0)));")));

            assertTrue(source.matches(PatternMaker.make(": 58 */     boolean bool = true;")));
            assertTrue(source.matches(PatternMaker.make(": 59 */     if ((Color.BLACK.equals(getColorChoice(s)) && (\"CONST1\"")));
            assertTrue(source.matches(PatternMaker.make(": 60 */       .equals(s) || \"CONST2\".equals(s))) || (")));
            assertTrue(source.matches(PatternMaker.make(": 61 */       Color.WHITE.equals(getColorChoice(s)) && (\"CONST3\"")));
            assertTrue(source.matches(PatternMaker.make(": 62 */       .equals(s) || \"CONST4\".equals(s))))")));
            assertTrue(source.matches(PatternMaker.make(": 64 */       bool = false; ")));
            assertTrue(source.matches(PatternMaker.make(": 66 */     return bool;")));

            assertTrue(source.matches(PatternMaker.make(": 70 */     return \"BLACK\".equals(s) ? Color.BLACK : Color.WHITE;")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
        }
    }

}
