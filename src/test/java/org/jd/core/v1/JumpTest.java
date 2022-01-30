package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.Jump;
import org.junit.Test;

import java.io.InputStream;

public class JumpTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = Jump.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/jump-jdk8u292.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  0 */ public class Jump {")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   public static boolean computeFlag(String paramObj1, Object paramObj2) {")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     boolean flag;")));
            assertTrue(source.matches(PatternMaker.make(": 11 */     if (paramObj1 == null || Objects.isNull(paramObj2)) {")));
            assertTrue(source.matches(PatternMaker.make(": 12 */       flag = false;")));
            assertTrue(source.matches(PatternMaker.make(": 13 */     } else if (paramObj2.equals(paramObj1.toString())) {")));
            assertTrue(source.matches(PatternMaker.make(": 14 */       flag = true;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } else {")));
            assertTrue(source.matches(PatternMaker.make(": 16 */       flag = false;")));
            assertTrue(source.matches(PatternMaker.make(": 17 */       List<Object> list = Collections.emptyList();")));
            assertTrue(source.matches(PatternMaker.make(": 19 */       for (Object elem : list) {")));
            assertTrue(source.matches(PatternMaker.make(": 20 */         if (\"\".equals(elem.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 21 */           for (String string : elem.toString().split(\"\")) {")));
            assertTrue(source.matches(PatternMaker.make(": 22 */             if (computeFlag(String.valueOf(string), paramObj2)) {")));
            assertTrue(source.matches(PatternMaker.make(": 23 */               flag = true;")));
            assertTrue(source.matches(PatternMaker.make(": 24 */               return flag;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */             } ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */           }  ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       } ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } ")));
            assertTrue(source.matches(PatternMaker.make(": 31 */     return flag;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   public static boolean computeFlag2(String paramObj1, Object paramObj2) {")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     boolean flag;")));
            assertTrue(source.matches(PatternMaker.make(": 37 */     if (paramObj1 == null || Objects.isNull(paramObj2)) {")));
            assertTrue(source.matches(PatternMaker.make(": 38 */       flag = false;")));
            assertTrue(source.matches(PatternMaker.make(": 39 */     } else if (paramObj2.equals(paramObj1.toString())) {")));
            assertTrue(source.matches(PatternMaker.make(": 40 */       flag = true;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } else {")));
            assertTrue(source.matches(PatternMaker.make(": 42 */       flag = false;")));
            assertTrue(source.matches(PatternMaker.make(": 43 */       List<Object> list = Collections.emptyList();")));
            assertTrue(source.matches(PatternMaker.make(": 45 */       label25: for (Object elem : list) {")));
            assertTrue(source.matches(PatternMaker.make(": 46 */         if (\"\".equals(elem.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 47 */           for (String string : elem.toString().split(\"\")) {")));
            assertTrue(source.matches(PatternMaker.make(": 48 */             if (computeFlag(String.valueOf(string), paramObj2)) {")));
            assertTrue(source.matches(PatternMaker.make(": 49 */               flag = true;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */               break label25;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */             } ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */           }  ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       } ")));
            assertTrue(source.matches(PatternMaker.make(": 56 */       System.out.println(\"this is not the return statement yet\");")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } ")));
            assertTrue(source.matches(PatternMaker.make(": 58 */     return flag;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   public static boolean computeFlag3(String paramObj1, Object paramObj2) {")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     boolean flag;")));
            assertTrue(source.matches(PatternMaker.make(": 64 */     if (paramObj1 == null || Objects.isNull(paramObj2)) {")));
            assertTrue(source.matches(PatternMaker.make(": 65 */       flag = false;")));
            assertTrue(source.matches(PatternMaker.make(": 66 */     } else if (paramObj2.equals(paramObj1.toString())) {")));
            assertTrue(source.matches(PatternMaker.make(": 67 */       flag = true;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } else {")));
            assertTrue(source.matches(PatternMaker.make(": 69 */       flag = false;")));
            assertTrue(source.matches(PatternMaker.make(": 70 */       List<Object> list = Collections.emptyList();")));
            assertTrue(source.matches(PatternMaker.make(": 72 */       for (Object elem : list) {")));
            assertTrue(source.matches(PatternMaker.make(": 73 */         if (\"\".equals(elem.toString()))")));
            assertTrue(source.matches(PatternMaker.make(": 74 */           for (String string : elem.toString().split(\"\")) {")));
            assertTrue(source.matches(PatternMaker.make(": 75 */             if (computeFlag(String.valueOf(string), paramObj2)) {")));
            assertTrue(source.matches(PatternMaker.make(": 76 */               flag = true;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */               // goto line number 84")));
            assertTrue(source.matches(PatternMaker.make(":  0 */             } ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */           }  ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */       } ")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } ")));
            assertTrue(source.matches(PatternMaker.make(": 84 */     System.out.println(\"this is not the return statement yet\");")));
            assertTrue(source.matches(PatternMaker.make(": 85 */     return flag;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */   }")));
            assertTrue(source.matches(PatternMaker.make(":  0 */ }")));
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
