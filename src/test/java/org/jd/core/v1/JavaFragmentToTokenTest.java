/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.fragment.Fragment;
import org.jd.core.v1.model.javafragment.EndMovableJavaBlockFragment;
import org.jd.core.v1.model.javafragment.ImportsFragment;
import org.jd.core.v1.model.javafragment.LineNumberTokensFragment;
import org.jd.core.v1.model.javafragment.StartBodyFragment;
import org.jd.core.v1.model.javafragment.StartMovableJavaBlockFragment;
import org.jd.core.v1.model.javafragment.StartSingleStatementBlockFragment;
import org.jd.core.v1.model.javafragment.StartStatementsBlockFragment;
import org.jd.core.v1.model.javafragment.TokensFragment;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.model.token.DeclarationToken;
import org.jd.core.v1.model.token.EndBlockToken;
import org.jd.core.v1.model.token.KeywordToken;
import org.jd.core.v1.model.token.LineNumberToken;
import org.jd.core.v1.model.token.NewLineToken;
import org.jd.core.v1.model.token.NumericConstantToken;
import org.jd.core.v1.model.token.ReferenceToken;
import org.jd.core.v1.model.token.StartBlockToken;
import org.jd.core.v1.model.token.StringConstantToken;
import org.jd.core.v1.model.token.TextToken;
import org.jd.core.v1.model.token.Token;
import org.jd.core.v1.printer.PlainTextMetaPrinter;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.util.JavaFragmentFactory;
import org.jd.core.v1.service.layouter.LayoutFragmentProcessor;
import org.jd.core.v1.service.writer.WriteTokenProcessor;
import org.jd.core.v1.services.tokenizer.javafragmenttotoken.TestTokenizeJavaFragmentProcessor;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class JavaFragmentToTokenTest extends TestCase {
    public static final KeywordToken BOOLEAN = new KeywordToken("boolean");
    public static final KeywordToken CLASS = new KeywordToken("class");
    public static final KeywordToken CATCH = new KeywordToken("catch");
    public static final KeywordToken EXTENDS = new KeywordToken("extends");
    public static final KeywordToken FALSE = new KeywordToken("false");
    public static final KeywordToken FINALLY = new KeywordToken("finally");
    public static final KeywordToken IF = new KeywordToken("if");
    public static final KeywordToken IMPLEMENTS = new KeywordToken("implements");
    public static final KeywordToken INT = new KeywordToken("int");
    public static final KeywordToken NEW = new KeywordToken("new");
    public static final KeywordToken NULL = new KeywordToken("null");
    public static final KeywordToken PACKAGE = new KeywordToken("package");
    public static final KeywordToken PROTECTED = new KeywordToken("protected");
    public static final KeywordToken PUBLIC = new KeywordToken("public");
    public static final KeywordToken RETURN = new KeywordToken("return");
    public static final KeywordToken STATIC = new KeywordToken("static");
    public static final KeywordToken SUPER = new KeywordToken("super");
    public static final KeywordToken VOID = new KeywordToken("void");

    protected LayoutFragmentProcessor layouter = new LayoutFragmentProcessor();
    protected TestTokenizeJavaFragmentProcessor tokenizer = new TestTokenizeJavaFragmentProcessor();
    protected WriteTokenProcessor writer = new WriteTokenProcessor();

    @Test
    public void testIfReturn_0() throws Exception {
        DecompileContext decompileContext = createMessageToTestIfReturn(0, 0);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();

        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(0);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/*   4:   0 */", "i = 1;")));
    }

    @Test
    public void testIfReturn_1_3() throws Exception {
        DecompileContext decompileContext = createMessageToTestIfReturn(1, 3);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(3);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/* 1: 1 */", "if (args == null)")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 2: 0 */", "return;")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 3 */", "int i = 1;")));
    }

    @Test
    public void testIfReturn_1_4() throws Exception {
        DecompileContext decompileContext = createMessageToTestIfReturn(1, 4);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(4);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/* 1: 1 */", "if (args == null)")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 2: 0 */", "return;")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 4: 4 */", "int i = 1;")));
    }

    @Test
    public void testIfAssignation_0() throws Exception {
        DecompileContext decompileContext = createMessageToTestIfAssignation(0, 0, 0);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();

        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(0);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/*   4:   0 */", "i = 1;")));
    }

    @Test
    public void testIfAssignation_1_2_3() throws Exception {
        DecompileContext decompileContext = createMessageToTestIfAssignation(1, 2, 3);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(3);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/* 1: 1 */", "if (args == null)")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 2: 2 */", "i = 0;")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 3 */", "i = 1;")));
        Assert.assertEquals(-1, source.indexOf('{'));
    }

    @Test
    public void testIfAssignation_1_3_5() throws Exception {
        DecompileContext decompileContext = createMessageToTestIfAssignation(1, 3, 5);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(5);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/* 1: 1 */", "if (args == null)")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 2: 0 */", "{")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 3 */", "i = 0;")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 5: 5 */", "i = 1;")));
    }

    @Test
    public void testClassAndFieldDeclarationWithoutImports_0() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclarationWithoutImports(0);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();

        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(0);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/*   4:   0 */", "protected int a")));
    }

    @Test
    public void testClassAndFieldDeclaration_0() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(0);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();

        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(0);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/*   7:   0 */", "protected int a")));
    }

    @Test
    public void testClassAndFieldDeclaration_1() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(1);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(1);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 1 */ package org.jd.core.v1.service.writer;"));
        Assert.assertNotEquals(-1, source.indexOf("/* 2: 0 */  -->"));
    }

    @Test
    public void testClassAndFieldDeclaration_2() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(2);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(2);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 2: 2 */", "protected int a")));
    }

    @Test
    public void testClassAndFieldDeclaration_3() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(3);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(3);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 2: 0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 3 */", "protected int a")));
        Assert.assertNotEquals(-1, source.indexOf("/* 4: 0 */ } -->"));
    }

    @Test
    public void testClassAndFieldDeclaration_4() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(4);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(4);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 4: 4 */", "protected int a")));
        Assert.assertNotEquals(-1, source.indexOf("/* 5: 0 */ } -->"));
    }

    @Test
    public void testClassAndFieldDeclaration_5() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(5);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(5);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 4: 0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 5: 5 */", "protected int a")));
    }

    @Test
    public void testClassAndFieldDeclaration_6() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(6);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(6);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 5: 0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 6: 6 */", "protected int a")));
    }

    @Test
    public void testClassAndFieldDeclaration_7() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(7);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();

        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(7);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 0 */", "import java.util.ArrayList")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 4: 0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 6: 0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 7: 7 */", "protected int a")));
    }

    @Test
    public void testClassAndFieldDeclaration_8() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(8);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(8);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 0 */", "import java.util.ArrayList")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 4: 0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 6: 0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 8: 8 */", "protected int a")));
    }

    @Test
    public void testClassAndFieldDeclaration_9() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(9);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(9);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 0 */", "import java.util.ArrayList")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 4: 0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 6: 0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 9: 9 */", "protected int a")));
    }

    @Test
    public void testClassAndFieldDeclaration_10() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassDeclaration(10);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(10);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/*  1:  0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  3:  0 */", "import java.util.ArrayList")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  4:  0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  6:  0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 10: 10 */", "protected int a")));
    }

    @Test
    public void testClassAndMethodDeclaration_3() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassAndMethodDeclaration(3);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(3);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 2: 0 */", "public TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 3 */", "super(i);")));
    }

    @Test
    public void testClassAndMethodDeclaration_4() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassAndMethodDeclaration(4);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(4);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 2: 0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 0 */", "public TokenWriterTest", "(int i)")));
    }

    @Test
    public void testClassAndMethodDeclaration_8() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassAndMethodDeclaration(8);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();

        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(8);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 0 */", "import java.util.ArrayList")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 4: 0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 6: 0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 7: 0 */", "public TokenWriterTest", "(int i)")));
    }

    @Test
    public void testClassAndMethodDeclaration_9() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassAndMethodDeclaration(9);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(9);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 1: 0 */ package org.jd.core.v1.service.writer;"));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 3: 0 */", "import java.util.ArrayList")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 4: 0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 6: 0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 8: 0 */", "public TokenWriterTest", "(int i)")));
    }

    @Test
    public void testClassAndMethodDeclaration_10() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassAndMethodDeclaration(10);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(10);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/*  1:  0 */", "package org.jd.core.v1.service.writer;")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  3:  0 */", "import java.util.ArrayList")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  4:  0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  6:  0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  7:  0 */", "extends Test")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  8:  0 */", "implements Serializable")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  9:  0 */", "public TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 10: 10 */", "super")));
    }

    @Test
    public void testClassAndMethodDeclaration_11() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassAndMethodDeclaration(11);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(11);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/*  1:  0 */", "package org.jd.core.v1.service.writer;")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  3:  0 */", "import java.util.ArrayList")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  4:  0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  6:  0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  7:  0 */", "extends Test")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  8:  0 */", "implements Serializable")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 10:  0 */", "public TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 11: 11 */", "super")));
    }

    @Test
    public void testClassAndMethodDeclaration_12() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassAndMethodDeclaration(12);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(12);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/*  1:  0 */", "package org.jd.core.v1.service.writer;")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  3:  0 */", "import java.util.ArrayList")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  4:  0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  7:  0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  8:  0 */", "extends Test")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  9:  0 */", "implements Serializable")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 11:  0 */", "public TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 12: 12 */", "super")));
    }

    @Test
    public void testClassAndMethodDeclaration_14() throws Exception {
        DecompileContext decompileContext = createMessageToTestClassAndMethodDeclaration(14);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(14);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/*  1:  0 */", "package org.jd.core.v1.service.writer;")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  3:  0 */", "import java.util.ArrayList")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  4:  0 */", "import org.junit.Assert")));
        Assert.assertTrue(source.matches(PatternMaker.make("/*  9:  0 */", "public class TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 10:  0 */", "extends Test")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 11:  0 */", "implements Serializable")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 13:  0 */", "public TokenWriterTest")));
        Assert.assertTrue(source.matches(PatternMaker.make("/* 14: 14 */", "super")));
    }

    @Test
    public void testLayout() throws Exception {
        DecompileContext decompileContext = createSimpleMessage(1);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();

        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(22);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/* 22: 22 */", "System", "println", "(i);")));
    }

    @Test
    public void testLayoutWithoutLineNumber() throws Exception {
        DecompileContext decompileContext = createSimpleMessage(0);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();

        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(0);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/*  15:   0 */", "public Object", "nextElement")));
    }

    @Test
    public void testLayoutWithStretchedfFragments_2() throws Exception {
        DecompileContext decompileContext = createSimpleMessage(2);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(44);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 44: 44 */"));
    }

    @Test
    public void testLayoutWithStretchedfFragments_3() throws Exception {
        DecompileContext decompileContext = createSimpleMessage(3);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(66);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertNotEquals(-1, source.indexOf("/* 66: 66 */"));
    }

    @Test
    public void testMoveDown() throws Exception {
        DecompileContext decompileContext = createMessageToTestMoveDown();
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(8);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/* 7: 0 */", "public static void main")));
    }

    @Test
    public void testLinkedBlocks_16() throws Exception {
        DecompileContext decompileContext = createMessageToTestLinkedBlocks(6, 9, 11, 13, 16);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(16);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/* 16: 16 */", "i = 4;")));
    }

    @Test
    public void testLinkedBlocks_22() throws Exception {
        DecompileContext decompileContext = createMessageToTestLinkedBlocks(7, 11, 15, 19, 22);
        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(22);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        Assert.assertTrue(source.matches(PatternMaker.make("/* 22: 22 */", "i = 4;")));
    }

    /**
     * Generate :
     * [  1:  0          ] <-- 0,1,2147483647:4
     * [  2:  lineNumber1]  -->if (args == null)<-- 0,1,2:5
     * [  3:  0          ]    -->return;<-- 0,0,2:5   --><-- 0,1,2147483647:4
     * [  4:  lineNumber2]  -->int i = 1;<-- 0,1,2147483647:4
     * [  5:  0          ]  -->
     *
     * @param lineNumber1
     * @param lineNumber2
     * @return A message
     * @throws Exception
     */
    public DecompileContext createMessageToTestIfReturn(int lineNumber1, int lineNumber2) throws Exception {
        DefaultList<Fragment> fragments = new DefaultList<>();

        JavaFragmentFactory.addSpacerBetweenStatements(fragments);

        List<Token> tokens = Arrays.asList(
                IF,
                TextToken.SPACE,
                TextToken.LEFTROUNDBRACKET,
                new LineNumberToken(lineNumber1),
                new TextToken("args == "),
                NULL,
                TextToken.RIGHTROUNDBRACKET
        );

        if (lineNumber1 == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        StartSingleStatementBlockFragment start = JavaFragmentFactory.addStartSingleStatementBlock(fragments);
        // return;\n
        fragments.add(new TokensFragment(
                RETURN,
                TextToken.SEMICOLON
        ));
        // End of if (args == null)
        JavaFragmentFactory.addEndSingleStatementBlock(fragments, start);

        JavaFragmentFactory.addSpacerBetweenStatements(fragments);

        tokens = Arrays.asList(
                INT,
                new LineNumberToken(lineNumber2),
                new TextToken(" i = 1;")
        );

        if (lineNumber2 == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        JavaFragmentFactory.addSpacerBetweenStatements(fragments);

        DecompileContext decompileContext = new DecompileContext(fragments);
        decompileContext.setBody(fragments);
        decompileContext.setMaxLineNumber(Integer.valueOf(lineNumber2));

        return decompileContext;
    }

    /**
     * Generate :
     * [  1:  0          ] <-- 0,1,2147483647:4
     * [  2:  lineNumber1]  -->if (args == null)<-- 0,1,2:5
     * [  3:  lineNumber2]    -->i = 0;<-- 0,0,2:5   --><-- 0,1,2147483647:4
     * [  4:  lineNumber3]  -->i = 1;<-- 0,1,2147483647:4
     * [  5:  0          ]  -->
     *
     * @param lineNumber1
     * @param lineNumber2
     * @param lineNumber3
     * @return A message
     * @throws Exception
     */
    public DecompileContext createMessageToTestIfAssignation(int lineNumber1, int lineNumber2, int lineNumber3) throws Exception {
        DefaultList<Fragment> fragments = new DefaultList<>();

        JavaFragmentFactory.addSpacerBetweenStatements(fragments);

        List<Token> tokens = Arrays.asList(
                IF,
                TextToken.SPACE,
                TextToken.LEFTROUNDBRACKET,
                new LineNumberToken(lineNumber1),
                new TextToken("args == "),
                NULL,
                TextToken.RIGHTROUNDBRACKET
        );

        if (lineNumber1 == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        StartSingleStatementBlockFragment start = JavaFragmentFactory.addStartSingleStatementBlock(fragments);
        // i = 0;\n
        tokens = Arrays.asList(
                new LineNumberToken(lineNumber2),
                new TextToken("i = 0;")
        );

        if (lineNumber2 == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }
        // End of if (args == null)
        JavaFragmentFactory.addEndSingleStatementBlock(fragments, start);

        JavaFragmentFactory.addSpacerBetweenStatements(fragments);

        tokens = Arrays.asList(
                new LineNumberToken(lineNumber3),
                new TextToken("i = 1;")
        );

        if (lineNumber3 == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        JavaFragmentFactory.addSpacerBetweenStatements(fragments);

        DecompileContext decompileContext = new DecompileContext(fragments);
        decompileContext.setMaxLineNumber(Integer.valueOf(lineNumber3));

        return decompileContext;
    }

    public DecompileContext createMessageToTestClassDeclaration(int lineNumber) throws Exception {
        DefaultList<Fragment> fragments = new DefaultList<>();

        // package org.jd.core.v1.service.writer;\n\n
        fragments.add(new TokensFragment(
                PACKAGE,
                new TextToken(" org.jd.core.v1.service.writer;")
        ));

        JavaFragmentFactory.addSpacerAfterPackage(fragments);

        // import java.util.ArrayList;\n
        // import static org.junit.Assert.*;\n\n
        ImportsFragment imports = JavaFragmentFactory.newImportsFragment();
        imports.addImport("java/util/ArrayList", "java.util.ArrayList");
        imports.addImport("org/junit/Assert/*", "org.junit.Assert.*");
        imports.initLineCounts();
        fragments.add(imports);

        JavaFragmentFactory.addSpacerAfterImports(fragments);
        JavaFragmentFactory.addSpacerBeforeMainDeclaration(fragments);

        // public class TokenWriterTest extends Test implements Serializable, Comparable<Test>, Cloneable {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_TYPE_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                CLASS,
                TextToken.SPACE,
                new DeclarationToken(Printer.TYPE, "org/jd/core/v1/service/test/TokenWriterTest", "TokenWriterTest", null),
                StartBlockToken.START_DECLARATION_OR_STATEMENT_BLOCK
        ));

        JavaFragmentFactory.addSpacerBeforeExtends(fragments);

        fragments.add(new TokensFragment(
                EXTENDS,
                TextToken.SPACE,
                new ReferenceToken(Printer.TYPE, "org/jd/core/v1/service/test/Test", "Test")
        ));

        JavaFragmentFactory.addSpacerBeforeImplements(fragments);

        fragments.add(new TokensFragment(
                IMPLEMENTS,
                TextToken.SPACE,
                new ReferenceToken(Printer.TYPE, "java/io/Serializable", "Serializable"),
                TextToken.COMMA_SPACE,
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_COMPARABLE, "Comparable"),
                TextToken.LEFTANGLEBRACKET,
                new ReferenceToken(Printer.TYPE, "org/jd/core/v1/service/test/Test", "Test"),
                TextToken.RIGHTANGLEBRACKET,
                TextToken.COMMA_SPACE,
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_CLONEABLE, "Cloneable"),
                EndBlockToken.END_DECLARATION_OR_STATEMENT_BLOCK
        ));

        StartBodyFragment classStart = JavaFragmentFactory.addStartTypeBody(fragments);

        // protected int a = 0;
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_FIELD_BLOCK);

        List<Token> tokens = Arrays.asList(
                PROTECTED,
                TextToken.SPACE,
                INT,
                TextToken.SPACE,
                new DeclarationToken(Printer.FIELD, "org/jd/core/v1/service/test/TokenWriterTest", "a", "I"),
                new TextToken(" = "),
                new LineNumberToken(lineNumber),
                new NumericConstantToken("0"),
                TextToken.SEMICOLON
        );

        if (lineNumber == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        JavaFragmentFactory.addEndTypeBody(fragments, classStart);

        DecompileContext decompileContext = new DecompileContext(fragments);
        decompileContext.setMaxLineNumber(Integer.valueOf(lineNumber));

        return decompileContext;
    }

    public DecompileContext createMessageToTestClassDeclarationWithoutImports(int lineNumber) throws Exception {
        DefaultList<Fragment> fragments = new DefaultList<>();

        // package org.jd.core.v1.service.writer;\n\n
        fragments.add(new TokensFragment(
                PACKAGE,
                new TextToken(" org.jd.core.v1.service.writer;")
        ));

        JavaFragmentFactory.addSpacerAfterPackage(fragments);

        // public class TokenWriterTest extends Test implements Serializable, Comparable<Test>, Cloneable {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_TYPE_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                CLASS,
                TextToken.SPACE,
                new DeclarationToken(Printer.TYPE, "org/jd/core/v1/service/test/TokenWriterTest", "TokenWriterTest", null),
                StartBlockToken.START_DECLARATION_OR_STATEMENT_BLOCK
        ));

        JavaFragmentFactory.addSpacerBeforeExtends(fragments);

        fragments.add(new TokensFragment(
                EXTENDS,
                TextToken.SPACE,
                new ReferenceToken(Printer.TYPE, "org/jd/core/v1/service/test/Test", "Test")
        ));

        JavaFragmentFactory.addSpacerBeforeImplements(fragments);

        fragments.add(new TokensFragment(
                IMPLEMENTS,
                TextToken.SPACE,
                new ReferenceToken(Printer.TYPE, "java/io/Serializable", "Serializable"),
                TextToken.COMMA_SPACE,
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_COMPARABLE, "Comparable"),
                TextToken.LEFTANGLEBRACKET,
                new ReferenceToken(Printer.TYPE, "org/jd/core/v1/service/test/Test", "Test"),
                TextToken.RIGHTANGLEBRACKET,
                TextToken.COMMA_SPACE,
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_CLONEABLE, "Cloneable"),
                EndBlockToken.END_DECLARATION_OR_STATEMENT_BLOCK
        ));

        StartBodyFragment classStart = JavaFragmentFactory.addStartTypeBody(fragments);

        // protected int a = 0;
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_FIELD_BLOCK);

        List<Token> tokens = Arrays.asList(
                PROTECTED,
                TextToken.SPACE,
                INT,
                TextToken.SPACE,
                new DeclarationToken(Printer.FIELD, "org/jd/core/v1/service/test/TokenWriterTest", "a", "I"),
                new TextToken(" = "),
                new LineNumberToken(lineNumber),
                new NumericConstantToken("0"),
                TextToken.SEMICOLON
        );

        if (lineNumber == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        JavaFragmentFactory.addEndTypeBody(fragments, classStart);

        DecompileContext decompileContext = new DecompileContext(fragments);
        decompileContext.setMaxLineNumber(Integer.valueOf(lineNumber));

        return decompileContext;
    }

    /*
     *                  package org.jd.core.v1.service.writer;<-- 0,2,1
     *
     * --><-- 0,M,0     import java.util.ArrayList;
     *                  import org.junit.Assert.*;
     * --><-- 0,1,1
     * --><-- 0,++,5 -->public class TokenWriterTest <-- 0,1,4 -->extends Test <-- 0,1,3 -->implements Serializable, Comparable<Test>, Cloneable <-- { 0,2,2
     * -->                public TokenWriterTest(int i) {<-- { 0,2,5
     * -->                  super(i);
     *                    }
     *                  }
     */
    public DecompileContext createMessageToTestClassAndMethodDeclaration(int lineNumber) throws Exception {
        DefaultList<Fragment> fragments = new DefaultList<>();

        // package org.jd.core.v1.service.writer;\n\n
        fragments.add(new TokensFragment(
                PACKAGE,
                new TextToken(" org.jd.core.v1.service.writer;")
        ));

        JavaFragmentFactory.addSpacerAfterPackage(fragments);

        // import java.util.ArrayList;\n
        // import static org.junit.Assert.*;\n\n
        ImportsFragment imports = JavaFragmentFactory.newImportsFragment();
        imports.addImport("java/util/ArrayList", "java.util.ArrayList");
        imports.addImport("org/junit/Assert/*", "org.junit.Assert.*");
        imports.initLineCounts();
        fragments.add(imports);

        JavaFragmentFactory.addSpacerAfterImports(fragments);
        JavaFragmentFactory.addSpacerBeforeMainDeclaration(fragments);

        // public class TokenWriterTest extends Test implements Serializable, Comparable<Test>, Cloneable {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_TYPE_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                CLASS,
                TextToken.SPACE,
                new DeclarationToken(Printer.TYPE, "org/jd/core/v1/service/test/TokenWriterTest", "TokenWriterTest", null),
                StartBlockToken.START_DECLARATION_OR_STATEMENT_BLOCK
        ));

        JavaFragmentFactory.addSpacerBeforeExtends(fragments);

        fragments.add(new TokensFragment(
                EXTENDS,
                TextToken.SPACE,
                new ReferenceToken(Printer.TYPE, "org/jd/core/v1/service/test/Test", "Test")
        ));

        JavaFragmentFactory.addSpacerBeforeImplements(fragments);

        fragments.add(new TokensFragment(
                IMPLEMENTS,
                TextToken.SPACE,
                new ReferenceToken(Printer.TYPE, "java/io/Serializable", "Serializable"),
                TextToken.COMMA_SPACE,
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_COMPARABLE, "Comparable"),
                TextToken.LEFTANGLEBRACKET,
                new ReferenceToken(Printer.TYPE, "org/jd/core/v1/service/test/Test", "Test"),
                TextToken.RIGHTANGLEBRACKET,
                TextToken.COMMA_SPACE,
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_CLONEABLE, "Cloneable"),
                EndBlockToken.END_DECLARATION_OR_STATEMENT_BLOCK
        ));

        StartBodyFragment startMainClass = JavaFragmentFactory.addStartTypeBody(fragments);

        // public TokenWriterTest(int i) {
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_METHOD_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                new DeclarationToken(Printer.CONSTRUCTOR, "org/jd/core/v1/service/test/TokenWriterTest", "TokenWriterTest", "(I)V"),
                TextToken.LEFTROUNDBRACKET,
                INT,
                TextToken.SPACE,
                new TextToken("i"),
                TextToken.RIGHTROUNDBRACKET
        ));

        StartBodyFragment startConstructor = JavaFragmentFactory.addStartMethodBody(fragments);

        fragments.add(new LineNumberTokensFragment(
                new LineNumberToken(lineNumber),
                SUPER,
                TextToken.LEFTROUNDBRACKET,
                new TextToken("i"),
                TextToken.RIGHTROUNDBRACKET,
                TextToken.SEMICOLON
        ));

        JavaFragmentFactory.addEndMethodBody(fragments, startConstructor);
        JavaFragmentFactory.addEndTypeBody(fragments, startMainClass);

        DecompileContext decompileContext = new DecompileContext(fragments);
        decompileContext.setMaxLineNumber(Integer.valueOf(lineNumber));

        return decompileContext;
    }

    /*
     *                  package org.jd.core.v1.service.writer;<-- 0,2,1
     *
     * --><-- 0,M,0     import java.util.ArrayList;
     *                  import org.junit.Assert.*;
     * --><-- 0,1,1
     * --><-- 0,++,5 -->public class TokenWriterTest<-- { 0,2,2
     * -->                public static void main(String[] args) {<-- { 0,2,5
     * -->                  if (args == null);<-- { 0,2,8
     * --><-- 0,++,9 -->      return;<-- 0,++,8 --><--
     *                      } 0,2,8 --><-- 0,++,8
     * -->                  int i = call(\n
     *                        "aaaa",\n
     *                        b,\n
     *                        new Enumeration()<-- { 0,2,2
     * -->                      public boolean hasMoreElements()<-- { 0,2,5
     * -->                        return false; <--
     *                          } 0,2,7 --><-- 0,1,4
     * --><-- 0,++,7
     * -->                      public Object nextElement()<-- { 0,2,5
     * -->                        return null; <--
     *                          } 0,2,7 --><--
     *                        } 0,2,2 -->,<-- 0,++,8
     * -->                    c);<-- 0,++,8
     * -->                  System.out.println(i);<--
     *                    } 0,2,7 --><--
     *                  } 0,2,2 -->
     */
    public DecompileContext createSimpleMessage(int factor) {
        DefaultList<Fragment> fragments = new DefaultList<>();

        // package org.jd.core.v1.service.writer;\n\n
        fragments.add(new TokensFragment(
                PACKAGE,
                new TextToken(" org.jd.core.v1.service.writer;")
        ));

        JavaFragmentFactory.addSpacerAfterPackage(fragments);

        // import java.util.ArrayList;\n
        // import static org.junit.Assert.*;\n\n
        ImportsFragment imports = JavaFragmentFactory.newImportsFragment();
        imports.addImport("java/util/ArrayList", "java.util.ArrayList");
        imports.addImport("org/junit/Assert/*", "org.junit.Assert.*");
        imports.initLineCounts();
        fragments.add(imports);

        JavaFragmentFactory.addSpacerAfterImports(fragments);
        JavaFragmentFactory.addSpacerBeforeMainDeclaration(fragments);

        // public class TokenWriterTest {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_TYPE_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                CLASS,
                TextToken.SPACE,
                new DeclarationToken(Printer.TYPE, "org/jd/core/v1/service/test/TokenWriterTest", "TokenWriterTest", null)
        ));

        StartBodyFragment startMainClass = JavaFragmentFactory.addStartTypeBody(fragments);

        // public static void main(String[] args) {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_METHOD_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                STATIC,
                TextToken.SPACE,
                VOID,
                TextToken.SPACE,
                new DeclarationToken(Printer.METHOD, "org/jd/core/v1/service/test/TokenWriterTest", "main", "([Ljava/lang/String;)V"),
                TextToken.LEFTROUNDBRACKET,
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_STRING, "String", null, "org/jd/core/v1/service/test/TokenWriterTest"),
                new TextToken("/*] args)")
        ));

        StartBodyFragment startMainMethodBody = JavaFragmentFactory.addStartMethodBody(fragments);

        // if (args == null)\n
        List<Token> tokens = Arrays.asList(
                IF,
                TextToken.SPACE,
                TextToken.LEFTROUNDBRACKET,
                new LineNumberToken(8 * factor),
                new TextToken("args == "),
                NULL,
                TextToken.RIGHTROUNDBRACKET
        );

        if (factor == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        StartSingleStatementBlockFragment startIfBlock = JavaFragmentFactory.addStartSingleStatementBlock(fragments);

        // return;\n
        fragments.add(new TokensFragment(
                RETURN,
                TextToken.SEMICOLON
        ));

        // End of if (args == null)
        JavaFragmentFactory.addEndSingleStatementBlock(fragments, startIfBlock);

        JavaFragmentFactory.addSpacerBetweenStatements(fragments);

        // int i = call(\n
        //  "aaaa",\n
        //  b,\n
        //  new Enumeration() {\n
        tokens = Arrays.asList(
                INT,
                new LineNumberToken(10 * factor),
                new TextToken(" i = "),
                new ReferenceToken(Printer.METHOD, "org/jd/core/v1/service/test/TokenWriterTest", "call", "(IILjava/util/Enumeration;I)V", "org/jd/core/v1/service/test/TokenWriterTest"),
                StartBlockToken.START_PARAMETERS_BLOCK,
                new LineNumberToken(11 * factor),
                new StringConstantToken("aaaa", "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.COMMA,
                TextToken.SPACE,
                new LineNumberToken(12 * factor),
                new TextToken("b,"),
                TextToken.SPACE,
                new LineNumberToken(13 * factor),
                NEW,
                TextToken.SPACE,
                new ReferenceToken(Printer.TYPE, "java/util/Enumeration", "java.util.Enumeration", null, "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.LEFTRIGHTROUNDBRACKETS
        );

        if (factor == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        StartBodyFragment startEnumerationClass = JavaFragmentFactory.addStartTypeBody(fragments);

        // public boolean hasMoreElements() {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_METHOD_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                BOOLEAN,
                TextToken.SPACE,
                new DeclarationToken(Printer.METHOD, "org/jd/core/v1/service/test/TokenWriterTest$1", "hasMoreElements", "()Z"),
                TextToken.LEFTRIGHTROUNDBRACKETS
        ));

        StartBodyFragment startHasMoreElementsMethodBody = JavaFragmentFactory.addStartMethodBody(fragments);

        // return false;\n
        tokens = Arrays.asList(
                RETURN,
                TextToken.SPACE,
                new LineNumberToken(15 * factor),
                FALSE,
                TextToken.SEMICOLON
        );

        if (factor == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        // }\n // End of public boolean hasMoreElements()
        JavaFragmentFactory.addEndMethodBody(fragments, startHasMoreElementsMethodBody);

        JavaFragmentFactory.addSpacerBetweenMembers(fragments);

        // public Object nextElement() {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_METHOD_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_OBJECT, "Object", null, "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.SPACE,
                new DeclarationToken(Printer.METHOD, "org/jd/core/v1/service/test/TokenWriterTest$1", "nextElement", "()Ljava/lang/Object;"),
                TextToken.LEFTRIGHTROUNDBRACKETS
        ));

        StartBodyFragment startNextElementMethodBody = JavaFragmentFactory.addStartMethodBody(fragments);

        // return null;\n
        tokens = Arrays.asList(
                RETURN,
                TextToken.SPACE,
                new LineNumberToken(18 * factor),
                NULL,
                TextToken.SEMICOLON
        );

        if (factor == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        // } // End of public Object nextElement()
        JavaFragmentFactory.addEndMethodBody(fragments, startNextElementMethodBody);

        //  \n}, // End of new Enumeration()
        JavaFragmentFactory.addEndSubTypeBodyInParameter(fragments, startEnumerationClass);

        //  c); // End of call(...
        // System.out.println(i);
        tokens = Arrays.asList(
                new LineNumberToken(21 * factor),
                new TextToken("c"),
                EndBlockToken.END_PARAMETERS_BLOCK,
                TextToken.SEMICOLON,
                NewLineToken.NEWLINE_1,
                new LineNumberToken(22 * factor),
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_SYSTEM, "System", null, "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.DOT,
                new ReferenceToken(Printer.FIELD, StringConstants.JAVA_LANG_SYSTEM, "out", "java/io/PrintStream", "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.DOT,
                new ReferenceToken(Printer.METHOD, "java/io/PrintStream", "println", "(I)V", "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.LEFTROUNDBRACKET,
                new TextToken("i"),
                TextToken.RIGHTROUNDBRACKET,
                TextToken.SEMICOLON
        );

        if (factor == 0) {
            fragments.add(new TokensFragment(tokens));
        } else {
            fragments.add(new LineNumberTokensFragment(tokens));
        }

        // \n} // End of public static void main(String[] args)
        JavaFragmentFactory.addEndMethodBody(fragments, startMainMethodBody);
        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        // } // End of public class TokenWriterTest
        JavaFragmentFactory.addEndTypeBody(fragments, startMainClass);
        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        DecompileContext decompileContext = new DecompileContext(fragments);
        if (factor != 0)
            decompileContext.setMaxLineNumber(Integer.valueOf(22 * factor));

        return decompileContext;
    }

    public DecompileContext createMessageToTestMoveDown() {
        DefaultList<Fragment> fragments = new DefaultList<>();

        // package org.jd.core.v1.service.writer;\n\n
        fragments.add(new TokensFragment(
                PACKAGE,
                new TextToken(" org.jd.core.v1.service.writer;")
        ));

        JavaFragmentFactory.addSpacerAfterImports(fragments);
        JavaFragmentFactory.addSpacerBeforeMainDeclaration(fragments);

        // public class TokenWriterTest {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_TYPE_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                CLASS,
                TextToken.SPACE,
                new DeclarationToken(Printer.TYPE, "org/jd/core/v1/service/test/TokenWriterTest", "TokenWriterTest", null)
        ));

        StartBodyFragment startMainClass = JavaFragmentFactory.addStartTypeBody(fragments);

        // public static int TIMESTAMP = System.currentTimeMillis();
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_FIELD_BLOCK);
        fragments.add(new LineNumberTokensFragment(
                PUBLIC,
                TextToken.SPACE,
                STATIC,
                TextToken.SPACE,
                INT,
                TextToken.SPACE,
                new DeclarationToken(Printer.FIELD, "org/jd/core/v1/service/test/TokenWriterTest", "TIMESTAMP", "I"),
                new TextToken(" = "),
                new LineNumberToken(4),
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_SYSTEM, "System", null, "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.DOT,
                new ReferenceToken(Printer.METHOD, StringConstants.JAVA_LANG_SYSTEM, "currentTimeMillis", "()J", "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.LEFTRIGHTROUNDBRACKETS,
                TextToken.SEMICOLON
        ));
        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        JavaFragmentFactory.addSpacerBetweenMembers(fragments);

        // protected int a;
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_FIELD_BLOCK);
        fragments.add(new TokensFragment(
                PROTECTED,
                TextToken.SPACE,
                INT,
                TextToken.SPACE,
                new DeclarationToken(Printer.FIELD, "org/jd/core/v1/service/test/TokenWriterTest", "a", "I"),
                TextToken.SEMICOLON
        ));
        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        JavaFragmentFactory.addSpacerBetweenMembers(fragments);

        // protected int b;
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_FIELD_BLOCK);
        fragments.add(new TokensFragment(
                PROTECTED,
                TextToken.SPACE,
                INT,
                TextToken.SPACE,
                new DeclarationToken(Printer.FIELD, "org/jd/core/v1/service/test/TokenWriterTest", "b", "I"),
                TextToken.SEMICOLON
        ));
        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        JavaFragmentFactory.addSpacerBetweenMembers(fragments);

        // protected int c;
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_FIELD_BLOCK);
        fragments.add(new TokensFragment(
                PROTECTED,
                TextToken.SPACE,
                INT,
                TextToken.SPACE,
                new DeclarationToken(Printer.FIELD, "org/jd/core/v1/service/test/TokenWriterTest", "c", "I"),
                TextToken.SEMICOLON
        ));
        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        JavaFragmentFactory.addSpacerBetweenMembers(fragments);

        // public static void main(String[] args) {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_METHOD_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                STATIC,
                TextToken.SPACE,
                VOID,
                TextToken.SPACE,
                new DeclarationToken(Printer.METHOD, "org/jd/core/v1/service/test/TokenWriterTest", "main", "([Ljava/lang/String;)V"),
                TextToken.LEFTROUNDBRACKET,
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_STRING, "String", null, "org/jd/core/v1/service/test/TokenWriterTest"),
                new TextToken("/*] args)")
        ));

        StartBodyFragment startMainMethodBody = JavaFragmentFactory.addStartMethodBody(fragments);
        //StartBodyFragment startMainMethodBody = JavaFragmentFactory.addStartMethodBody(fragments);

        // System.out.println(TIMESTAMP);
        fragments.add(new LineNumberTokensFragment(
                new LineNumberToken(8),
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_SYSTEM, "System", null, "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.DOT,
                new ReferenceToken(Printer.FIELD, StringConstants.JAVA_LANG_SYSTEM, "out", "java/io/PrintStream", "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.DOT,
                new ReferenceToken(Printer.METHOD, "java/io/PrintStream", "println", "(I)V", "org/jd/core/v1/service/test/TokenWriterTest"),
                TextToken.LEFTROUNDBRACKET,
                new TextToken("TIMESTAMP"),
                TextToken.RIGHTROUNDBRACKET,
                TextToken.SEMICOLON
        ));

        // \n}
        JavaFragmentFactory.addEndMethodBody(fragments, startMainMethodBody);
        //JavaFragmentFactory.addEndMethodBody(fragments, startMainMethodBody);
        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        // }
        JavaFragmentFactory.addEndTypeBody(fragments, startMainClass);
        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        DecompileContext decompileContext = new DecompileContext(fragments);
        decompileContext.setMaxLineNumber(Integer.valueOf(8));
        //message.setStart("containsByteCode", Boolean.TRUE);

        return decompileContext;
    }

    /**
     * Generate:
     * [  1:  0          ] package org.jd.core.v1.service.writer;<-- 0,2,2:9
     * [  2:  0          ]
     * [  3:  0          ]  -->public class TokenWriterTest<META-DECLARATION type='+TYPE' internalName='org/jd/core/v1/service/test/TokenWriterTest' descriptor='null'/><-- 0,1,2:2
     * [  4:  0          ] {
     * [  5:  0          ]    -->public static void main<META-DECLARATION type='+METHOD' internalName='org/jd/core/v1/service/test/TokenWriterTest' descriptor='([Ljava/lang/String;)V'/>(String<META-REFERENCE type='+TYPE' internalName='java/lang/String' descriptor='null' ownerInternalName='org/jd/core/v1/service/test/TokenWriterTest'/>[] args)<-- 0,0,2:11  {
     * [  6:  lineNumber1]      -->int i = 0;<-- 0,1,2147483647:4
     * [  7:  0          ]
     * [  8:  0          ]      -->try<-- 0,1,2:5  {
     * [  9:  lineNumber2]        --><-- 0,0,2147483647:19 -->i = 1;<-- 0,0,2147483647:18 --><-- 0,1,2:5
     * [ 10:  0          ]     } -->catch (RuntimeException<META-REFERENCE type='+TYPE' internalName='java/lang/RuntimeException' descriptor='null' ownerInternalName='org/jd/core/v1/service/test/TokenWriterTest'/> e)<-- 0,1,2:5  {
     * [ 11:  lineNumber3]        --><-- 0,0,2147483647:19 -->i = 2;<-- 0,0,2147483647:18 --><-- 0,1,2:5
     * [ 12:  0          ]     } -->finally<-- 0,1,2:5  {
     * [ 13:  lineNumber4]        --><-- 0,0,2147483647:19 -->i = 3;<-- 0,0,2147483647:18 --><-- 0,1,2:5
     * [ 14:  0          ]     } --><-- 0,1,2147483647:4
     * [ 15:  0          ]
     * [ 16:  lineNumber5]      -->i = 4;<-- 0,1,1:3
     * [ 17:  0          ]   } --><-- 0,1,1:1
     * [ 18:  0          ] } -->
     *
     * @param lineNumber1
     * @param lineNumber2
     * @param lineNumber3
     * @param lineNumber4
     * @param lineNumber5
     * @return
     */
    public DecompileContext createMessageToTestLinkedBlocks(int lineNumber1, int lineNumber2, int lineNumber3, int lineNumber4, int lineNumber5) {
        DefaultList<Fragment> fragments = new DefaultList<>();

        // package org.jd.core.v1.service.writer;\n\n
        fragments.add(new TokensFragment(
                PACKAGE,
                new TextToken(" org.jd.core.v1.service.writer;")
        ));

        JavaFragmentFactory.addSpacerAfterImports(fragments);
        JavaFragmentFactory.addSpacerBeforeMainDeclaration(fragments);

        // public class TokenWriterTest {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_TYPE_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                CLASS,
                TextToken.SPACE,
                new DeclarationToken(Printer.TYPE, "org/jd/core/v1/service/test/TokenWriterTest", "TokenWriterTest", null)
        ));

        StartBodyFragment startMainClass = JavaFragmentFactory.addStartTypeBody(fragments);

        // public static void main(String[] args) {\n
        fragments.add(StartMovableJavaBlockFragment.START_MOVABLE_METHOD_BLOCK);
        fragments.add(new TokensFragment(
                PUBLIC,
                TextToken.SPACE,
                STATIC,
                TextToken.SPACE,
                VOID,
                TextToken.SPACE,
                new DeclarationToken(Printer.METHOD, "org/jd/core/v1/service/test/TokenWriterTest", "main", "([Ljava/lang/String;)V"),
                TextToken.LEFTROUNDBRACKET,
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_STRING, "String", null, "org/jd/core/v1/service/test/TokenWriterTest"),
                new TextToken("/*] args)")
        ));

        StartBodyFragment startMainMethodBody = JavaFragmentFactory.addStartMethodBody(fragments);

        // int i = 0;
        fragments.add(new LineNumberTokensFragment(
                new LineNumberToken(lineNumber1),
                INT,
                new TextToken(" i = 0;")
        ));

        JavaFragmentFactory.addSpacerBetweenStatements(fragments);

        // try {
        StartStatementsBlockFragment.Group group = JavaFragmentFactory.addStartStatementsTryBlock(fragments);

        // i = 1;
        fragments.add(new LineNumberTokensFragment(
                new LineNumberToken(lineNumber2),
                new TextToken("i = 1;")
        ));

        JavaFragmentFactory.addEndStatementsBlock(fragments, group);

        // catch (RuntimeException e)
        fragments.add(new TokensFragment(
                CATCH,
                new TextToken(" ("),
                new ReferenceToken(Printer.TYPE, StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, "RuntimeException", null, "org/jd/core/v1/service/test/TokenWriterTest"),
                new TextToken(" e)")
        ));

        JavaFragmentFactory.addStartStatementsBlock(fragments, group);

        // i = 2;
        fragments.add(new LineNumberTokensFragment(
                new LineNumberToken(lineNumber3),
                new TextToken("i = 2;")
        ));

        JavaFragmentFactory.addEndStatementsBlock(fragments, group);

        // finally
        fragments.add(new TokensFragment(
                FINALLY
        ));

        JavaFragmentFactory.addStartStatementsBlock(fragments, group);

        // i = 3;
        fragments.add(new LineNumberTokensFragment(
                new LineNumberToken(lineNumber4),
                new TextToken("i = 3;")
        ));

        JavaFragmentFactory.addEndStatementsBlock(fragments, group);

        JavaFragmentFactory.addSpacerBetweenStatements(fragments);

        // i = 4;
        fragments.add(new LineNumberTokensFragment(
                new LineNumberToken(lineNumber5),
                new TextToken("i = 4;")
        ));

        // \n}
        JavaFragmentFactory.addEndMethodBody(fragments, startMainMethodBody);
        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        // }
        JavaFragmentFactory.addEndTypeBody(fragments, startMainClass);
        fragments.add(EndMovableJavaBlockFragment.END_MOVABLE_BLOCK);

        DecompileContext decompileContext = new DecompileContext(fragments);
        decompileContext.setMaxLineNumber(Integer.valueOf(8));
        //message.setStart("containsByteCode", Boolean.TRUE);

        return decompileContext;
    }

    protected void printSource(String source) {
        System.out.println("- - - - - - - - ");
        System.out.println(source);
        System.out.println("- - - - - - - - ");
    }
}
