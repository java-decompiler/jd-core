/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import junit.framework.TestCase;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.ClassFileToJavaSyntaxProcessor;
import org.jd.core.v1.service.deserializer.classfile.DeserializeClassFileProcessor;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.JavaSyntaxToJavaFragmentProcessor;
import org.jd.core.v1.service.layouter.LayoutFragmentProcessor;
import org.jd.core.v1.service.tokenizer.javafragmenttotoken.JavaFragmentToTokenProcessor;
import org.jd.core.v1.service.writer.WriteTokenProcessor;
import org.jd.core.v1.services.tokenizer.javafragmenttotoken.TestTokenizeJavaFragmentProcessor;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public class LayoutFragmentProcessorTest extends TestCase {
    protected DeserializeClassFileProcessor deserializer = new DeserializeClassFileProcessor();
    protected ClassFileToJavaSyntaxProcessor converter = new ClassFileToJavaSyntaxProcessor();
    protected JavaSyntaxToJavaFragmentProcessor fragmenter = new JavaSyntaxToJavaFragmentProcessor();
    protected LayoutFragmentProcessor layouter = new LayoutFragmentProcessor();
    //protected TestTokenizeJavaFragmentProcessor tokenizer = new TestTokenizeJavaFragmentProcessor();
    protected JavaFragmentToTokenProcessor tokenizer = new JavaFragmentToTokenProcessor();
    protected WriteTokenProcessor writer = new WriteTokenProcessor();

    @Test
    public void testJdk118Basic() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.1.8.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setHeader("mainInternalTypeName", "org/jd/core/test/Basic");
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);

        deserializer.process(decompileContext);
        converter.process(decompileContext);
        fragmenter.process(decompileContext);
        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        assertTrue(source.indexOf("/* 188: 188 */") != -1);
    }

    @Test
    public void testJdk131TryCatchFinally() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.3.1.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setHeader("mainInternalTypeName", "org/jd/core/test/TryCatchFinally");
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);

        deserializer.process(decompileContext);
        converter.process(decompileContext);
        fragmenter.process(decompileContext);
        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        assertTrue(source.indexOf("/* 902: 902 */") != -1);
    }

    @Test
    public void testTryCatchFinally() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setHeader("mainInternalTypeName", "org/jd/core/test/TryCatchFinally");
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);

        deserializer.process(decompileContext);
        converter.process(decompileContext);
        fragmenter.process(decompileContext);
        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        assertTrue(source.indexOf("/* 902: 902 */") != -1);
    }

    @Test
    public void testAnonymousClass() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setHeader("mainInternalTypeName", "org/jd/core/test/AnonymousClass");
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);

        deserializer.process(decompileContext);
        converter.process(decompileContext);
        fragmenter.process(decompileContext);
        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

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

        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setHeader("mainInternalTypeName", "org/jd/core/test/OuterClass");
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);

        deserializer.process(decompileContext);
        converter.process(decompileContext);
        fragmenter.process(decompileContext);
        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        assertTrue(source.indexOf("/* 182: 182 */") != -1);
    }

    @Test
    public void testEnumClass() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        TestTokenizeJavaFragmentProcessor tokenizer = new TestTokenizeJavaFragmentProcessor();

        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setHeader("mainInternalTypeName", "org/jd/core/test/Enum");
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);

        deserializer.process(decompileContext);
        converter.process(decompileContext);
        fragmenter.process(decompileContext);
        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

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

        TestTokenizeJavaFragmentProcessor tokenizer = new TestTokenizeJavaFragmentProcessor();

        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setHeader("mainInternalTypeName", "org/jd/core/test/annotation/Quality");
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);

        deserializer.process(decompileContext);
        converter.process(decompileContext);
        fragmenter.process(decompileContext);
        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        assertTrue(source.indexOf("/* 9: 0 */   }") != -1);
    }

    @Test
    public void testJdk170Array() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        TestTokenizeJavaFragmentProcessor tokenizer = new TestTokenizeJavaFragmentProcessor();

        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setHeader("mainInternalTypeName", "org/jd/core/test/Array");
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);

        deserializer.process(decompileContext);
        converter.process(decompileContext);
        fragmenter.process(decompileContext);
        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/* 30: 30 */", "int[][] ia", "0, 1, 2")));

        assertTrue(source.indexOf("/* 75: 75 */") != -1);
    }

    protected void printSource(String source) {
        System.out.println("- - - - - - - - ");
        System.out.println(source);
        System.out.println("- - - - - - - - ");
    }
}
