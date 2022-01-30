package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
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
            assertTrue(source.matches(PatternMaker.make(": 17 */   public static final String SEP = \"\" + File.separatorChar;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   protected Object getValue(int p1, int p2, int p3) {")));
            assertTrue(source.matches(PatternMaker.make(": 21 */     Objects objects = null;")));
            assertTrue(source.matches(PatternMaker.make(": 22 */     if (this.name != null) {")));
            assertTrue(source.matches(PatternMaker.make(": 23 */       objects = this.values.get(this.name);")));
            assertTrue(source.matches(PatternMaker.make(": 24 */       System.out.println(\"before try\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       try {")));
            assertTrue(source.matches(PatternMaker.make(": 26 */         if (objects == null || Objects.isNull(objects.toString()) || \"icon\".isEmpty() || File.separator.isEmpty())")));
            assertTrue(source.matches(PatternMaker.make(": 27 */           System.err.println(\"Error !!\"); ")));
            assertTrue(source.matches(PatternMaker.make(": 29 */       } catch (Exception e) {")));
            assertTrue(source.matches(PatternMaker.make(": 30 */         e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       } ")));
            assertTrue(source.matches(PatternMaker.make(": 32 */       System.out.println(\"after try\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } else {")));
            assertTrue(source.matches(PatternMaker.make(": 34 */       objects = this.values.get(this.value);")));
            assertTrue(source.matches(PatternMaker.make(": 35 */       System.out.println(\"before try\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       try {")));
            assertTrue(source.matches(PatternMaker.make(": 37 */         if (objects == null || Objects.isNull(objects.toString()) || SEP.isEmpty())")));
            assertTrue(source.matches(PatternMaker.make(": 38 */           System.err.println(\"Error !!!\"); ")));
            assertTrue(source.matches(PatternMaker.make(": 40 */       } catch (Exception e) {")));
            assertTrue(source.matches(PatternMaker.make(": 41 */         e.printStackTrace();")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       } ")));
            assertTrue(source.matches(PatternMaker.make(": 43 */       System.out.println(\"after try\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } ")));
            assertTrue(source.matches(PatternMaker.make(": 45 */     return objects;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   protected Object getValue(int p1, int p2) {")));
            assertTrue(source.matches(PatternMaker.make(": 50 */     Objects objects = null;")));
            assertTrue(source.matches(PatternMaker.make(": 51 */     if (this.name != null) {")));
            assertTrue(source.matches(PatternMaker.make(": 52 */       objects = this.values.get(this.name);")));
            assertTrue(source.matches(PatternMaker.make(": 53 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 54 */         System.err.println(\"Error !!\"); ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } else {")));
            assertTrue(source.matches(PatternMaker.make(": 57 */       objects = this.values.get(this.value);")));
            assertTrue(source.matches(PatternMaker.make(": 58 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 59 */         System.err.println(\"Error !!!\"); ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } ")));
            assertTrue(source.matches(PatternMaker.make(": 62 */     return objects;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   protected Object getValue(int p1) {")));
            assertTrue(source.matches(PatternMaker.make(": 67 */     Objects objects = null;")));
            assertTrue(source.matches(PatternMaker.make(": 68 */     if (this.name != null) {")));
            assertTrue(source.matches(PatternMaker.make(": 69 */       objects = this.values.get(this.name);")));
            assertTrue(source.matches(PatternMaker.make(": 70 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 71 */         System.err.println(\"Error !!\"); ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } else {")));
            assertTrue(source.matches(PatternMaker.make(": 74 */       objects = this.values.get(this.value);")));
            assertTrue(source.matches(PatternMaker.make(": 75 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 76 */         System.err.println(\"Error !!!\"); ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } ")));
            assertTrue(source.matches(PatternMaker.make(": 79 */     return objects;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   protected Object getValue() {")));
            assertTrue(source.matches(PatternMaker.make(": 84 */     Objects objects = null;")));
            assertTrue(source.matches(PatternMaker.make(": 85 */     if (this.name != null) {")));
            assertTrue(source.matches(PatternMaker.make(": 86 */       objects = this.values.get(this.name);")));
            assertTrue(source.matches(PatternMaker.make(": 87 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 88 */         System.err.println(\"Error !!\"); ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } else {")));
            assertTrue(source.matches(PatternMaker.make(": 91 */       objects = this.values.get(this.value);")));
            assertTrue(source.matches(PatternMaker.make(": 92 */       if (objects == null || Objects.isNull(objects.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 93 */         System.err.println(\"Error !!!\"); ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } ")));
            assertTrue(source.matches(PatternMaker.make(": 96 */     return objects;")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
