package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.JavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.AutoUnboxingInLoop;
import org.junit.Test;

import java.io.InputStream;

public class AutoUnboxingInLoopTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = AutoUnboxingInLoop.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/auto-unboxing-in-loop-jdk8u292.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(": 13 */     while (hashCode != -1) {")));
            assertTrue(source.matches(PatternMaker.make(": 14 */       List<Object> list = new ArrayList();")));
            assertTrue(source.matches(PatternMaker.make(": 16 */       for (int j = 0; j < paramList.size(); j++) {")));
            assertTrue(source.matches(PatternMaker.make(": 17 */         Object next = paramList.listIterator(j).next();")));
            assertTrue(source.matches(PatternMaker.make(": 18 */         if (next != null) {")));
            assertTrue(source.matches(PatternMaker.make(": 19 */           Object elem = paramList.listIterator(j).next();")));
            assertTrue(source.matches(PatternMaker.make(": 20 */           String str = String.valueOf(elem.hashCode());")));
            assertTrue(source.matches(PatternMaker.make(": 21 */           if (hashCode.equals(Integer.parseInt(str)))")));
            assertTrue(source.matches(PatternMaker.make(": 22 */             list.add(elem); ")));
            assertTrue(source.matches(PatternMaker.make(": 27 */       list2.addAll(list1);")));
            assertTrue(source.matches(PatternMaker.make(": 28 */       Integer integer1 = hashCode, integer2 = hashCode = hashCode - 1;")));
            assertTrue(source.matches(PatternMaker.make(": 30 */     List<Object> l = new ArrayList();")));
            assertTrue(source.matches(PatternMaker.make(": 32 */     for (int i = 0; i < list2.size(); i++) {")));
            assertTrue(source.matches(PatternMaker.make(": 33 */       Object next = list2.listIterator(i).next();")));
            assertTrue(source.matches(PatternMaker.make(": 34 */       if (next != null)")));
            assertTrue(source.matches(PatternMaker.make(": 35 */         l.add(next); ")));

            assertTrue(source.matches(PatternMaker.make(": 41 */     Integer index = (int)Math.random();")));
            assertTrue(source.matches(PatternMaker.make(": 42 */     if (index > 0) {")));
            assertTrue(source.matches(PatternMaker.make(": 43 */       Integer integer1 = index, integer2 = index = index - 1;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     } else {")));
            assertTrue(source.matches(PatternMaker.make(": 45 */       index = null;")));
            assertTrue(source.matches(PatternMaker.make(":  0 */     }")));
            assertTrue(source.matches(PatternMaker.make(": 47 */     return index;")));
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
        }
    }
}
