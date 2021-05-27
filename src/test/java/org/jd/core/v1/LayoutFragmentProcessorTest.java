/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.services.tokenizer.javafragmenttotoken.TestTokenizeJavaFragmentProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public class LayoutFragmentProcessorTest extends AbstractJdTest {

    @Test
    public void testJdk118Basic() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.1.8.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        String source = this.decompileSuccess(loader, printer, "org/jd/core/test/Basic", configuration);

        assertTrue(source.indexOf("/* 188: 188 */") != -1);
    }

    @Test
    public void testJdk131TryCatchFinally() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.3.1.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        String source = this.decompileSuccess(loader, printer, "org/jd/core/test/TryCatchFinally", configuration);

        assertTrue(source.indexOf("/* 902: 902 */") != -1);
    }

    @Test
    public void testTryCatchFinally() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
        String source = this.decompileSuccess(loader, printer, "org/jd/core/test/TryCatchFinally", configuration);

        assertTrue(source.indexOf("/* 902: 902 */") != -1);
    }

    @Test
    public void testAnonymousClass() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        String source = this.decompileSuccess(loader, printer, "org/jd/core/test/AnonymousClass", configuration);

        assertTrue(source.indexOf("/* 111: 111 */") != -1);

        assertTrue(source.indexOf("} ;") == -1);
    }

    @Test
    public void testOuterClass() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        String source = this.decompileSuccess(loader, printer, "org/jd/core/test/OuterClass", configuration);

        assertTrue(source.indexOf("/* 182: 182 */") != -1);
    }

    @Test
    public void testEnumClass() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        TestTokenizeJavaFragmentProcessor tokenizer = new TestTokenizeJavaFragmentProcessor();

        String source = this.decompileSuccess(loader, printer, "org/jd/core/test/Enum");

        assertTrue(source.indexOf("NEPTUNE(1.024E26D, 2.4746E7D);") != -1);
        assertTrue(source.indexOf("public static final double G = 6.673E-11D;") != -1);
    }

    @Test
    public void testAnnotationQuality() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
        String source = this.decompileSuccess(loader, printer, "org/jd/core/test/annotation/Quality", configuration);

        assertTrue(source.indexOf("/* 9: 0 */   }") != -1);
    }

    @Test
    public void testJdk170Array() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
        String source = this.decompileSuccess(loader, printer, "org/jd/core/test/Array", configuration);

        Assert.assertTrue(source.matches(PatternMaker.make("/* 30: 30 */", "int[][] ia", "0, 1, 2")));

        assertTrue(source.indexOf("/* 75: 75 */") != -1);
    }
}
