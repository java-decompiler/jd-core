package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.SwitchEnum;
import org.junit.Test;

import java.io.InputStream;

public class SwitchEnumTest extends AbstractJdTest {
    @Test
    // https://github.com/java-decompiler/jd-core/issues/42
    public void test() throws Exception {
        String internalClassName = SwitchEnum.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/switch-enum-jdk8u292.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  0 */ package org.jd.core.v1.stub;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */ ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */ public class SwitchEnum {")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   public enum ColourEnum {")));
            assertTrue(source.matches(PatternMaker.make(":  6 */     RED, GREEN, BLUE;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   public enum FruitEnum {")));
            assertTrue(source.matches(PatternMaker.make(": 10 */     BANANA, APPLE, KIWI;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   static class ColourObject {")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     public SwitchEnum.ColourEnum getType() {")));
            assertTrue(source.matches(PatternMaker.make(": 15 */       return SwitchEnum.ColourEnum.BLUE;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   static class FruitObject {")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     public SwitchEnum.FruitEnum getType() {")));
            assertTrue(source.matches(PatternMaker.make(": 21 */       return SwitchEnum.FruitEnum.KIWI;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   public static void main(String[] args) {")));
            assertTrue(source.matches(PatternMaker.make(": 26 */     ColourObject colourObject = new ColourObject();")));
            assertTrue(source.matches(PatternMaker.make(": 27 */     print(colourObject);")));
            assertTrue(source.matches(PatternMaker.make(": 28 */     FruitObject fruitObject = new FruitObject();")));
            assertTrue(source.matches(PatternMaker.make(": 29 */     print(fruitObject);")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   private static void print(FruitObject fruitObject) {")));
            assertTrue(source.matches(PatternMaker.make(": 33 */     switch (fruitObject.getType()) {")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       case APPLE:")));
            assertTrue(source.matches(PatternMaker.make(": 35 */         System.out.println(\"Apple\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */         break;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       case BANANA:")));
            assertTrue(source.matches(PatternMaker.make(": 38 */         System.out.println(\"Banana\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */         break;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       case KIWI:")));
            assertTrue(source.matches(PatternMaker.make(": 41 */         System.out.println(\"Kiwi\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */         break;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       default:")));
            assertTrue(source.matches(PatternMaker.make(": 44 */         System.out.println(\"Default (fruit)\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */         break;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   private static void print(ColourObject colourObject) {")));
            assertTrue(source.matches(PatternMaker.make(": 51 */     switch (colourObject.getType()) {")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       case GREEN:")));
            assertTrue(source.matches(PatternMaker.make(": 53 */         System.out.println(\"Green\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */         break;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       case BLUE:")));
            assertTrue(source.matches(PatternMaker.make(": 56 */         System.out.println(\"Blue\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */         break;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       case RED:")));
            assertTrue(source.matches(PatternMaker.make(": 59 */         System.out.println(\"Red\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */         break;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       default:")));
            assertTrue(source.matches(PatternMaker.make(": 62 */         System.out.println(\"Default (colour)\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */         break;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */ }")));
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
