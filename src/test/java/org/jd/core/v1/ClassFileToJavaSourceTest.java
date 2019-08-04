/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import junit.framework.TestCase;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.JavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.model.message.Message;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.ClassFileToJavaSyntaxProcessor;
import org.jd.core.v1.service.deserializer.classfile.DeserializeClassFileProcessor;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.JavaSyntaxToJavaFragmentProcessor;
import org.jd.core.v1.service.layouter.LayoutFragmentProcessor;
import org.jd.core.v1.service.tokenizer.javafragmenttotoken.JavaFragmentToTokenProcessor;
import org.jd.core.v1.service.writer.WriteTokenProcessor;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ClassFileToJavaSourceTest extends TestCase {
    protected DeserializeClassFileProcessor deserializer = new DeserializeClassFileProcessor();
    protected ClassFileToJavaSyntaxProcessor converter = new ClassFileToJavaSyntaxProcessor();
    protected JavaSyntaxToJavaFragmentProcessor fragmenter = new JavaSyntaxToJavaFragmentProcessor();
    protected LayoutFragmentProcessor layouter = new LayoutFragmentProcessor();
    //protected TestTokenizeJavaFragmentProcessor tokenizer = new TestTokenizeJavaFragmentProcessor();
    protected JavaFragmentToTokenProcessor tokenizer = new JavaFragmentToTokenProcessor();
    protected WriteTokenProcessor writer = new WriteTokenProcessor();

    @Test
    public void testJdk170Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("serialVersionUID = 9506606333927794L;") != -1);
        assertTrue(source.indexOf(".indexOf('B');") != -1);

        assertTrue(source.matches(PatternMaker.make("/*  26:  26 */", "System.out.println(\"hello\");")));

        assertTrue(source.indexOf("String str1 = \"3 == \" + (i + 1) + \" ?\";") != -1);
        assertTrue(source.indexOf("String str2 = String.valueOf(\"abc \\b \\f \\n \\r \\t \\\" \\007 def\");") != -1);
        assertTrue(source.indexOf("char c2 = '€';") != -1);
        assertTrue(source.indexOf("char c3 = '\\'';") != -1);
        assertTrue(source.indexOf("char c4 = c3 = c2 = c1 = Character.toUpperCase('x');") != -1);
        assertTrue(source.indexOf("Class<String> class3 = String.class, class2 = class3, class1 = class2;") != -1);
        assertTrue(source.matches(PatternMaker.make("Class<?> class5 = doSomething(class6 = String.class, args1 = args2 = new String[], class4 = class5;")));
        assertTrue(source.matches(PatternMaker.make("int j = 1, k[] = {1, l[][] = {")));
        assertTrue(source.matches(PatternMaker.make("String stringNull = null;")));

        assertTrue(source.matches(PatternMaker.make(":  60 */", "return new String[] {s, s + '?'};")));

        assertTrue(source.indexOf("if (this instanceof Object)") != -1);

        assertTrue(source.indexOf("int k = 50 / (25 + (i = 789));") != -1);
        assertTrue(source.matches(PatternMaker.make(":  82 */", "k = i += 100;")));
        assertTrue(source.matches(PatternMaker.make(":  87 */", "i = ++this.int78;")));
        assertTrue(source.matches(PatternMaker.make(":  88 */", "i = this.int78++;")));
        assertTrue(source.matches(PatternMaker.make(":  89 */", "i *= 10;")));
        assertTrue(source.matches(PatternMaker.make(":  91 */", "this.int78 = ++i;")));
        assertTrue(source.matches(PatternMaker.make(":  92 */", "this.int78 = i++;")));
        assertTrue(source.matches(PatternMaker.make(":  93 */", "this.int78 *= 10;")));
        assertTrue(source.matches(PatternMaker.make(":  95 */", "long34 = ++long12;")));
        assertTrue(source.matches(PatternMaker.make(":  96 */", "long34 = long12++;")));
        assertTrue(source.matches(PatternMaker.make(":  97 */", "long34 *= 10L;")));
        assertTrue(source.matches(PatternMaker.make(":  99 */", "i = (int)long12 + this.int78;")));
        assertTrue(source.matches(PatternMaker.make(": 101 */", "i = k ^ 0xFF;")));
        assertTrue(source.matches(PatternMaker.make(": 102 */", "i |= 0x7;")));

        assertTrue(source.indexOf("int result;") != -1);
        assertTrue(source.matches(PatternMaker.make(": 114 */", "result = 1;")));
        assertTrue(source.matches(PatternMaker.make(": 116 */", "int k = i;")));
        assertTrue(source.matches(PatternMaker.make(": 117 */", "result = k + 2;")));
        assertTrue(source.matches(PatternMaker.make(": 120 */", "result = this.short56;")));
        assertTrue(source.matches(PatternMaker.make(": 124 */", "return result;")));
        assertTrue(source.matches(PatternMaker.make(": 128 */", "int int78 = getInt78(new Object[] { this }, (short)5);")));
        assertTrue(source.matches(PatternMaker.make(": 130 */", "i = (int)(Basic.long12 + long12) + this.int78 + int78;")));

        assertTrue(source.indexOf("public static native int read();") != -1);

        assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "return str + str;")));
        assertTrue(source.matches(PatternMaker.make("/* 176: 176 */", "return str;")));

        assertTrue(source.matches(PatternMaker.make("/* 185: 185 */", "return ((Basic)objects[index]).int78;")));

        assertTrue(source.matches(PatternMaker.make("/* 188: 188 */", "protected static final Integer INTEGER_255 = new Integer(255);")));

        assertTrue(source.indexOf("<init>()") == -1);
        assertTrue(source.indexOf("NaND") == -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170NoDebugBasic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0-no-debug-info.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("System.out.println(\"hello\");")));

        assertTrue(source.matches(PatternMaker.make("String str1 = \"3 == \" + (paramInt + 1) + \" ?\";")));
        assertTrue(source.indexOf("String str2 = String.valueOf(\"abc \\b \\f \\n \\r \\t \\\" \\007 def\");") != -1);
        assertTrue(source.matches(PatternMaker.make("char c2 = '€';")));
        assertTrue(source.matches(PatternMaker.make("char c4 = c3 = c2 = c1 = Character.toUpperCase('x');")));
        assertTrue(source.matches(PatternMaker.make("Class<String> clazz3 = String.class;")));
        assertTrue(source.matches(PatternMaker.make("Class<String> clazz2 = clazz3;")));
        assertTrue(source.matches(PatternMaker.make("Class<String> clazz1 = clazz2;")));
        assertTrue(source.indexOf("Class<?> clazz5 = doSomething(clazz6 = String.class, arrayOfString1 = arrayOfString2 = new String[]") != -1);

        assertTrue(source.matches(PatternMaker.make("if (this instanceof Object)")));

        assertTrue(source.matches(PatternMaker.make("this.int78 = 50 / (25 + (this.int78 = 789));")));

        assertTrue(source.indexOf("protected static final Integer INTEGER_255 = new Integer(255);") != -1);

        assertTrue(source.indexOf("<init>()") == -1);
        assertTrue(source.indexOf("NaND") == -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170Constructors() throws Exception {
        String internalClassName = "org/jd/core/test/Constructors";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(": 28 */", "this.short123 = 1;")));

        assertTrue(source.matches(PatternMaker.make(": 32 */", "super(short56);")));
        assertTrue(source.matches(PatternMaker.make(": 33 */", "this.short123 = 2;")));

        assertTrue(source.matches(PatternMaker.make(": 37 */", "this(short56);")));
        assertTrue(source.matches(PatternMaker.make(": 38 */", "this.int78 = int78;")));
        assertTrue(source.matches(PatternMaker.make(": 39 */", "this.short123 = 3;")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170IfElse() throws Exception {
        String internalClassName = "org/jd/core/test/IfElse";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("/*  12:  12 */", "if (this == null)")));
        assertTrue(source.matches(PatternMaker.make("/*  22:  22 */", "if (\"abc\".isEmpty() && \"abc\".isEmpty())")));

        assertTrue(source.matches(PatternMaker.make("/*  32:  32 */", "if (this == null)")));
        assertTrue(source.matches(PatternMaker.make("/*  34:   0 */", "} else {")));

        assertTrue(source.matches(PatternMaker.make("/*  44:  44 */", "if (this == null)")));
        assertTrue(source.matches(PatternMaker.make("/*  46:  46 */", "} else if (this == null) {")));
        assertTrue(source.matches(PatternMaker.make("/*  48:   0 */", "} else {")));

        assertTrue(source.matches(PatternMaker.make("/*  58:  58 */", "if (i == 0)")));
        assertTrue(source.matches(PatternMaker.make("/*  60:  60 */", "if (i == 1)")));

        assertTrue(source.matches(PatternMaker.make("/*  71:  71 */", "if (i == System.currentTimeMillis())")));
        assertTrue(source.matches(PatternMaker.make("/*  73:  73 */", "} else if (i != System.currentTimeMillis()) {")));
        assertTrue(source.matches(PatternMaker.make("/*  75:  75 */", "} else if (i > System.currentTimeMillis()) {")));

        assertTrue(source.matches(PatternMaker.make("/* 123: 123 */", "if (i == 4 && i == 5 && i == 6)")));

        assertTrue(source.matches(PatternMaker.make("/* 135: 135 */", "if (i == 3 || i == 5 || i == 6)")));
        assertTrue(source.matches(PatternMaker.make("/* 137: 137 */", "} else if (i != 4 && i > 7 && i > 8) {")));
        assertTrue(source.matches(PatternMaker.make("/* 139:   0 */", "} else {")));

        assertTrue(source.matches(PatternMaker.make("/* 148: 148 */", "if ((i == 1 && i == 2 && i == 3) || (i == 4 && i == 5 && i == 6) || (i == 7 && i == 8 && i == 9))")));
        assertTrue(source.matches(PatternMaker.make("/* 160: 160 */", "if ((i == 1 || i == 2 || i == 3) && (i == 4 || i == 5 || i == 6) && (i == 7 || i == 8 || i == 9))")));

        assertTrue(source.matches(PatternMaker.make("/* 172: 172 */", "if ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4))")));
        assertTrue(source.matches(PatternMaker.make("/* 184: 184 */", "if ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4))")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170Interface() throws Exception {
        String internalClassName = "org/jd/core/test/Interface";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("public interface Interface", "extends Serializable")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170While() throws Exception {
        String internalClassName = "org/jd/core/test/While";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  15 */", "while (i-- > 0)")));
        assertTrue(source.matches(PatternMaker.make(":  23 */", "while (i < 10)")));
        assertTrue(source.matches(PatternMaker.make(":  42 */", "while (i0 > 20)")));
        assertTrue(source.matches(PatternMaker.make("/* 113:   0 */", "continue;")));
        assertTrue(source.matches(PatternMaker.make("/* 128:   0 */", "break;")));
        assertTrue(source.matches(PatternMaker.make("/* 158:   0 */", "while (true)")));
        assertTrue(source.matches(PatternMaker.make(": 232 */", "while (++i < 10)")));
        assertTrue(source.indexOf("while (i == 4 && i == 5 && i == 6)") != -1);
        assertTrue(source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4))") != -1);
        assertTrue(source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4))") != -1);
        assertFalse(source.matches(PatternMaker.make("[ 348:   0 */", "default:")));
        assertFalse(source.matches(PatternMaker.make("[ 350: 348 */", "continue;")));
        assertTrue(source.matches(PatternMaker.make("/* 404: 404 */", "System.out.println(\"a\");")));
        assertTrue(source.matches(PatternMaker.make("/* 431: 431 */", "System.out.println(\"a\");")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk901While() throws Exception {
        String internalClassName = "org/jd/core/test/While";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-9.0.1.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  15 */", "while (i-- > 0)")));
        assertTrue(source.matches(PatternMaker.make(":  23 */", "while (i < 10)")));
        assertTrue(source.matches(PatternMaker.make(":  42 */", "while (i0 > 20)")));
        assertTrue(source.matches(PatternMaker.make("/* 113:   0 */", "continue;")));
        assertTrue(source.matches(PatternMaker.make("/* 128:   0 */", "break;")));
        assertTrue(source.matches(PatternMaker.make("/* 158:   0 */", "while (true)")));
        assertTrue(source.matches(PatternMaker.make(": 232 */", "while (++i < 10)")));
        assertTrue(source.indexOf("while (i == 4 && i == 5 && i == 6)") != -1);
        assertTrue(source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4))") != -1);
        assertTrue(source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4))") != -1);
        assertFalse(source.matches(PatternMaker.make("[ 348:   0 */", "default:")));
        assertFalse(source.matches(PatternMaker.make("[ 350: 348 */", "continue;")));
        assertTrue(source.matches(PatternMaker.make("/* 404: 404 */", "System.out.println(\"a\");")));
        assertTrue(source.matches(PatternMaker.make("/* 431: 431 */", "System.out.println(\"a\");")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk1002While() throws Exception {
        String internalClassName = "org/jd/core/test/While";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-10.0.2.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  15 */", "while (i-- > 0)")));
        assertTrue(source.matches(PatternMaker.make(":  23 */", "while (i < 10)")));
        assertTrue(source.matches(PatternMaker.make(":  42 */", "while (i0 > 20)")));
        assertTrue(source.matches(PatternMaker.make("/* 113:   0 */", "continue;")));
        assertTrue(source.matches(PatternMaker.make("/* 128:   0 */", "break;")));
        assertTrue(source.matches(PatternMaker.make("/* 158:   0 */", "while (true)")));
        assertTrue(source.matches(PatternMaker.make(": 232 */", "while (++i < 10)")));
        assertTrue(source.indexOf("while (i == 4 && i == 5 && i == 6)") != -1);
        assertTrue(source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4))") != -1);
        assertTrue(source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4))") != -1);
        assertFalse(source.matches(PatternMaker.make("[ 348:   0 */", "default:")));
        assertFalse(source.matches(PatternMaker.make("[ 350: 348 */", "continue;")));
        assertTrue(source.matches(PatternMaker.make("/* 404: 404 */", "System.out.println(\"a\");")));
        assertTrue(source.matches(PatternMaker.make("/* 431: 431 */", "System.out.println(\"a\");")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170DoWhile() throws Exception {
        String internalClassName = "org/jd/core/test/DoWhile";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  24 */", "} while (i < 10);")));
        assertTrue(source.matches(PatternMaker.make(":  32 */", "} while (this == null);")));
        assertTrue(source.matches(PatternMaker.make(":  44 */", "++i;")));
        assertTrue(source.matches(PatternMaker.make(":  46 */", "while (i < 10);")));
        assertTrue(source.matches(PatternMaker.make(":  72 */", "while (i0 < 10)")));
        assertTrue(source.matches(PatternMaker.make(":  77 */", "i1--;")));
        assertTrue(source.matches(PatternMaker.make(":  79 */", "while (i1 > 0);")));
        assertTrue(source.matches(PatternMaker.make(":  98 */", "while (--i > 0.0F);")));
        assertTrue(source.matches(PatternMaker.make(": 108 */", "while (i-- > 0.0F);")));
        assertTrue(source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4));") != -1);
        assertTrue(source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4));") != -1);

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk901DoWhile() throws Exception {
        String internalClassName = "org/jd/core/test/DoWhile";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-9.0.1.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  24 */", "} while (i < 10);")));
        assertTrue(source.matches(PatternMaker.make(":  32 */", "} while (this == null);")));
        assertTrue(source.matches(PatternMaker.make(":  44 */", "++i;")));
        assertTrue(source.matches(PatternMaker.make(":  46 */", "while (i < 10);")));
        assertTrue(source.matches(PatternMaker.make(":  72 */", "while (i0 < 10)")));
        assertTrue(source.matches(PatternMaker.make(":  77 */", "i1--;")));
        assertTrue(source.matches(PatternMaker.make(":  79 */", "while (i1 > 0);")));
        assertTrue(source.matches(PatternMaker.make(":  98 */", "while (--i > 0.0F);")));
        assertTrue(source.matches(PatternMaker.make(": 108 */", "while (i-- > 0.0F);")));
        assertTrue(source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4));") != -1);
        assertTrue(source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4));") != -1);

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk1002DoWhile() throws Exception {
        String internalClassName = "org/jd/core/test/DoWhile";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-10.0.2.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  24 */", "} while (i < 10);")));
        assertTrue(source.matches(PatternMaker.make(":  32 */", "} while (this == null);")));
        assertTrue(source.matches(PatternMaker.make(":  44 */", "++i;")));
        assertTrue(source.matches(PatternMaker.make(":  46 */", "while (i < 10);")));
        assertTrue(source.matches(PatternMaker.make(":  72 */", "while (i0 < 10)")));
        assertTrue(source.matches(PatternMaker.make(":  77 */", "i1--;")));
        assertTrue(source.matches(PatternMaker.make(":  79 */", "while (i1 > 0);")));
        assertTrue(source.matches(PatternMaker.make(":  98 */", "while (--i > 0.0F);")));
        assertTrue(source.matches(PatternMaker.make(": 108 */", "while (i-- > 0.0F);")));
        assertTrue(source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4));") != -1);
        assertTrue(source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4));") != -1);

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170BreakContinue() throws Exception {
        String internalClassName = "org/jd/core/test/BreakContinue";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("/*  15:  15 */", "if (i == 1)")));
        assertTrue(source.matches(PatternMaker.make("/*  16:   0 */", "continue;")));
        assertTrue(source.matches(PatternMaker.make("/*  18:  18 */", "if (i == 2)")));
        assertTrue(source.matches(PatternMaker.make("/*  19:   0 */", "continue;")));

        assertTrue(source.matches(PatternMaker.make("/*  31:  31 */", "label18: while (i > 1)")));
        assertTrue(source.matches(PatternMaker.make("/*  37:   0 */", "continue label18;")));
        assertTrue(source.matches(PatternMaker.make("/*  40:   0 */", "break label18;")));

        assertTrue(source.matches(PatternMaker.make("/*  54:  54 */", "label17: while (i > 1)")));
        assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "break;")));
        assertTrue(source.matches(PatternMaker.make("/*  63:   0 */", "continue label17;")));

        assertTrue(source.matches(PatternMaker.make("/*  78:   0 */", "label13:")));
        assertTrue(source.matches(PatternMaker.make("/*  83:   0 */", "break;")));
        assertTrue(source.matches(PatternMaker.make("/*  86:   0 */", "break label13;")));

        assertTrue(source.matches(PatternMaker.make("/* 101:   0 */", "label15:", "do {")));
        assertTrue(source.matches(PatternMaker.make("/* 106:   0 */", "break;")));
        assertTrue(source.matches(PatternMaker.make("/* 109:   0 */", "break label15;")));

        assertTrue(source.matches(PatternMaker.make("/* 123:   0 */", "label24:", "do {")));
        assertTrue(source.matches(PatternMaker.make("/* 133:   0 */", "continue label24;")));
        assertTrue(source.matches(PatternMaker.make("/* 135:   0 */", "break label24;")));
        assertTrue(source.matches(PatternMaker.make("/* 138:   0 */", "break label23;")));

        assertTrue(source.matches(PatternMaker.make("/* 155:   0 */", "label16:", "do {")));
        assertTrue(source.matches(PatternMaker.make("/* 162:   0 */", "break label16;")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170For() throws Exception {
        String internalClassName = "org/jd/core/test/For";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  20 */", "for (int i = 0; i < 10; i++)")));
        assertTrue(source.matches(PatternMaker.make(":  42 */", "int i = 0;")));
        assertTrue(source.matches(PatternMaker.make(":  44 */", "for (; i < 10; i++)")));
        assertTrue(source.matches(PatternMaker.make(":  54 */", "for (; i < 10; i++)")));
        assertTrue(source.matches(PatternMaker.make(":  64 */", "for (int i = 0;; i++)")));
        assertTrue(source.matches(PatternMaker.make(":  72 */", "for (;; i++)")));
        assertTrue(source.matches(PatternMaker.make(":  80 */", "for (int i = 0; i < 10;)")));
        assertTrue(source.matches(PatternMaker.make(":  88 */", "while (i < 10)")));
        assertTrue(source.matches(PatternMaker.make(":  96 */", "int i = 0;")));
        assertTrue(source.matches(PatternMaker.make("/* 104:   0 */", "while (true)")));
        assertTrue(source.matches(PatternMaker.make(": 112 */", "for (int i = 0, j = i, size = 10; i < size; j += ++i)")));
        assertTrue(source.matches(PatternMaker.make(": 122 */", "int i = 0, j = i, size = 10;")));
        assertTrue(source.matches(PatternMaker.make(": 123 */", "for (; i < size;")));
        assertTrue(source.matches(PatternMaker.make(": 124 */", "j += ++i)")));
        assertTrue(source.matches(PatternMaker.make(": 134 */", "int i = 0;")));
        assertTrue(source.matches(PatternMaker.make(": 135 */", "int j = i;")));
        assertTrue(source.matches(PatternMaker.make(": 136 */", "int size = 10;")));
        assertTrue(source.matches(PatternMaker.make(": 137 */", "for (; i < size;")));
        assertTrue(source.matches(PatternMaker.make(": 138 */", "i++,")));
        assertTrue(source.matches(PatternMaker.make(": 139 */", "j += i)")));
        assertTrue(source.matches(PatternMaker.make(": 149 */", "int i = 0;")));
        assertTrue(source.matches(PatternMaker.make(": 151 */", "int j = i;")));
        assertTrue(source.matches(PatternMaker.make(": 153 */", "int size = 10;")));
        assertTrue(source.matches(PatternMaker.make(": 155 */", "for (; i < size;")));
        assertTrue(source.matches(PatternMaker.make(": 157 */", "i++,")));
        assertTrue(source.matches(PatternMaker.make(": 159 */", "j += i)")));
        assertTrue(source.matches(PatternMaker.make(": 169 */", "for (int i = 0; i < 10; i++);")));
        assertTrue(source.matches(PatternMaker.make(": 177 */", "for (; i < 10; i++);")));
        assertTrue(source.matches(PatternMaker.make(": 185 */", "for (int i = 0;; i++);")));
        assertTrue(source.matches(PatternMaker.make("/* 190:   0 */", "while (true)")));
        assertTrue(source.matches(PatternMaker.make(": 191 */", "i++;")));
        assertTrue(source.matches(PatternMaker.make(": 197 */", "for (int i = 0; i < 10;);")));
        assertTrue(source.matches(PatternMaker.make(": 203 */", "for (int[] i = { 0 }; i.length < 10;);")));
        assertTrue(source.matches(PatternMaker.make(": 209 */", "for (int i = 0, j = i, k = i; i < 10;);")));
        assertTrue(source.matches(PatternMaker.make(": 215 */", "for (int[] i = { 0 }, j = i, k = j; i.length < 10;);")));
        assertTrue(source.matches(PatternMaker.make(": 221 */", "for (int i = 0, j[] = { 1 }; i < 10;);")));
        assertTrue(source.matches(PatternMaker.make(": 227 */", "while (i < 10);")));
        assertTrue(source.matches(PatternMaker.make(": 233 */", "int i = 0;")));
        assertTrue(source.matches(PatternMaker.make("/* 234:   0 */", "while (true);")));
        assertTrue(source.matches(PatternMaker.make(": 245 */", "for (int i = 0, j = i, size = 10; i < size; j += ++i);")));
        assertTrue(source.matches(PatternMaker.make("/* 253:   0 */", "while (true) {")));
        assertTrue(source.matches(PatternMaker.make(": 264 */", "for (String s : list)")));
        assertTrue(source.matches(PatternMaker.make(": 310 */", "for (int i : new int[] { 4 })")));
        assertTrue(source.matches(PatternMaker.make(": 389 */", "for (String s : array)")));
        assertTrue(source.matches(PatternMaker.make(": 403 */", "for (String s : list)")));
        assertTrue(source.matches(PatternMaker.make(": 411 */", "Iterator<Class<?>> iterator = Arrays.asList(getClass().getInterfaces()).iterator()")));

        assertTrue(source.indexOf("/* 489: 489 */") != -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170NoDebugFor() throws Exception {
        String internalClassName = "org/jd/core/test/For";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0-no-debug-info.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", "org/jd/core/test/For");
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("for (byte b = 0; b < 10; b++)")));
        assertTrue(source.matches(PatternMaker.make("for (byte b = 0;; b++)")));
        assertTrue(source.matches(PatternMaker.make("for (byte b = 0; b < 10; b++)")));
        assertTrue(source.matches(PatternMaker.make("for (String str : paramList)")));
        assertTrue(source.matches(PatternMaker.make("for (paramInt = 0; paramInt < 10; paramInt++)")));
        assertTrue(source.matches(PatternMaker.make("for (int i : new int[] { 4 })")));
        assertTrue(source.matches(PatternMaker.make("for (String str : paramArrayOfString)")));
        assertTrue(source.matches(PatternMaker.make("for (String str : paramList)")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        //assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk150For() throws Exception {
        String internalClassName = "org/jd/core/test/For";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.5.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  16 */", "for (byte b = 0; b < 10; b++)")));
        assertTrue(source.matches(PatternMaker.make(":  84 */", "while (paramInt < 10)")));
        assertTrue(source.matches(PatternMaker.make(": 269 */", "for (paramInt = 0; paramInt < 10; paramInt++)")));
        assertTrue(source.matches(PatternMaker.make(": 306 */", "for (int i : new int[] { 4 })")));
        assertTrue(source.matches(PatternMaker.make("/* 343:   0 */", "do {")));
        assertTrue(source.matches(PatternMaker.make(": 345 */", "while (b < 10);")));
        assertTrue(source.matches(PatternMaker.make(": 381 */", "for (String str : paramArrayOfString)")));
        assertTrue(source.matches(PatternMaker.make(": 395 */", "for (String str : paramList)")));
        assertTrue(source.matches(PatternMaker.make(": 407 */", "Iterator iterator = Arrays.asList(getClass().getInterfaces()).iterator()")));
        assertTrue(source.matches(PatternMaker.make(": 423 */", "for (byte b = 0; b < 3; b++)")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.5", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk160For() throws Exception {
        String internalClassName = "org/jd/core/test/For";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.6.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  16 */", "for (int i = 0; i < 10; i++)")));
        assertTrue(source.matches(PatternMaker.make(":  84 */", "while (i < 10)")));
        assertTrue(source.matches(PatternMaker.make(": 269 */", "for (i = 0; i < 10; i++)")));
        assertTrue(source.matches(PatternMaker.make(": 306 */", "for (int i : new int[] { 4 })")));
        assertTrue(source.matches(PatternMaker.make("/* 343:   0 */", "do {")));
        assertTrue(source.matches(PatternMaker.make(": 345 */", "while (i < 10);")));
        assertTrue(source.matches(PatternMaker.make(": 381 */", "for (String s : array)")));
        assertTrue(source.matches(PatternMaker.make(": 395 */", "for (String s : list)")));
        assertTrue(source.matches(PatternMaker.make(": 407 */", "Iterator<Class<?>> iterator = Arrays.asList(getClass().getInterfaces()).iterator()")));
        assertTrue(source.matches(PatternMaker.make(": 423 */", "for (int i = 0; i < 3; i++)")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.6", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testIbmJ9For() throws Exception {
        String internalClassName = "org/jd/core/test/For";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-ibm-j9_vm.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  84 */", "while (i < 10)")));
        assertTrue(source.matches(PatternMaker.make(": 269 */", "for (i = 0; i < 10; i++)")));
        assertTrue(source.matches(PatternMaker.make(": 306 */", "for (int i : new int[] { 4 })")));
        assertTrue(source.matches(PatternMaker.make("/* 343:   0 */", "do")));
        assertTrue(source.matches(PatternMaker.make(": 345 */", "while (i < 10);")));
        assertTrue(source.matches(PatternMaker.make(": 381 */", "for (String s : array)")));
        assertTrue(source.matches(PatternMaker.make(": 395 */", "for (String s : list)")));
        assertTrue(source.matches(PatternMaker.make(": 407 */", "Iterator<Class<?>> iterator = Arrays.asList(getClass().getInterfaces()).iterator()")));
        assertTrue(source.matches(PatternMaker.make(": 423 */", "for (int i = 0; i < 3; i++)")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.5", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170Array() throws Exception {
        String internalClassName = "org/jd/core/test/Array";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        HashMap<String, Object> configuration = new HashMap<>();

        configuration.put("realignLineNumbers", Boolean.FALSE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(": 12 */", "int[] i1 = new int[1];")));
        assertTrue(source.matches(PatternMaker.make(": 13 */", "int[][] i2 = new int[1][];")));
        assertTrue(source.matches(PatternMaker.make(": 14 */", "int[][][] i3 = new int[1][][];")));
        assertTrue(source.matches(PatternMaker.make(": 15 */", "int[][][] i4 = new int[1][2][];")));
        assertTrue(source.matches(PatternMaker.make(": 22 */", "String[][][][] s5 = new String[1][2][][];")));

        assertTrue(source.matches(PatternMaker.make(": 26 */", "byte[] b1 = { 1, 2 } ;")));
        assertTrue(source.matches(PatternMaker.make(": 27 */", "byte[][] b2 = { { 1, 2 } } ;")));
        assertTrue(source.matches(PatternMaker.make(": 28 */", "byte[][][][] b3 = { { { 3, 4 } } } ;")));

        assertTrue(source.matches(PatternMaker.make(": 48 */", "testException1(new Exception[]", "{ new Exception(\"1\") } );")));

        assertTrue(source.matches(PatternMaker.make(": 73 */", "testInt2(new int[][]", "{ { 1 } ,")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk150Array() throws Exception {
        String internalClassName = "org/jd/core/test/Array";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.5.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        HashMap<String, Object> configuration = new HashMap<>();

        configuration.put("realignLineNumbers", Boolean.FALSE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(": 13 */", "int[][] arrayOfInt1 = new int[1][];")));
        assertTrue(source.matches(PatternMaker.make(": 30 */", "int[][] arrayOfInt1 = { { 0, 1, 2")));

        assertTrue(source.matches(PatternMaker.make(": 52 */", "testException2(new Exception[][]", "{ { new Exception(\"1\")")));

        assertTrue(source.matches(PatternMaker.make(": 73 */", "testInt2(new int[][] { { 1,")));

        assertTrue(source.matches(PatternMaker.make(": 73 */", "testInt2(new int[][] { { 1,")));
        assertTrue(source.matches(PatternMaker.make(": 75 */", "testInt3(new int[][][] { { { 0, 1")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.5", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170Assert() throws Exception {
        String internalClassName = "org/jd/core/test/Assert";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("/* 16: 16 */", "assert false : \"false\";")));
        assertTrue(source.matches(PatternMaker.make("/* 17: 17 */", "assert i == 0 || i == 1;")));
        assertTrue(source.matches(PatternMaker.make("/* 18: 18 */", "assert i == 2 && i < 3;")));

        assertTrue(source.matches(PatternMaker.make("/* 34: 34 */", "assert new BigDecimal(i) == BigDecimal.ONE;")));

        assertTrue(source.matches(PatternMaker.make("/* 41: 41 */", "assert check() : \"boom\";")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk150Assert() throws Exception {
        String internalClassName = "org/jd/core/test/Assert";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.5.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("/* 16: 16 */", "assert false : \"false\";")));
        assertTrue(source.matches(PatternMaker.make("/* 17: 17 */", "assert paramInt == 0 || paramInt == 1;")));
        assertTrue(source.matches(PatternMaker.make("/* 18: 18 */", "assert paramInt == 2 && paramInt < 3;")));

        assertTrue(source.matches(PatternMaker.make("/* 34: 34 */", "assert new BigDecimal(paramInt) == BigDecimal.ONE;")));

        assertTrue(source.matches(PatternMaker.make("/* 41: 41 */", "assert check() : \"boom\";")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.5", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk150AnonymousClass() throws Exception {
        String internalClassName = "org/jd/core/test/AnonymousClass";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.5.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  21 */", "Object object = new Object()")));
        assertTrue(source.matches(PatternMaker.make(":  23 */", "return \"toString() return \" + super.toString() + \" at \" + AnonymousClass.this.time;")));

        assertTrue(source.matches(PatternMaker.make(":  37 */", "final long l1 = System.currentTimeMillis();")));
        assertTrue(source.matches(PatternMaker.make(":  39 */", "Enumeration enumeration = new Enumeration()")));
        assertTrue(source.matches(PatternMaker.make(":  40 */", "Iterator<String> i = AnonymousClass.this.list.iterator();")));
        assertTrue(source.matches(PatternMaker.make(":  44 */", "return (this.i.hasNext() && s1 == s2 && i1 > l1);")));
        assertTrue(source.indexOf("return this.i.next();") != -1);
        assertTrue(source.matches(PatternMaker.make(":  52 */", "test(enumeration, \"test\");")));
        assertTrue(source.matches(PatternMaker.make(":  55 */", "System.out.println(\"end\");")));

        assertTrue(source.matches(PatternMaker.make(":  67 */", "if (s1 == s2 && i == 5)")));

        assertTrue(source.matches(PatternMaker.make(":  90 */", "Serializable serializable = new Serializable()")));
        assertTrue(source.matches(PatternMaker.make(":  96 */", "return (abc.equals(param2Object) || def.equals(param2Object) || str1.equals(param2Object) || str2.equals(param2Object));")));
        assertTrue(source.matches(PatternMaker.make(": 104 */", "System.out.println(\"end\");")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.5", new JavaSourceFileObject(internalClassName, source),
                                               new JavaSourceFileObject("org/jd/core/test/annotation/Name", "package org.jd.core.test.annotation; public @interface Name {String value();}")));
    }

    @Test
    public void testJdk170Switch() throws Exception {
        String internalClassName = "org/jd/core/test/Switch";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("/*  15:  15 */", "switch (i)")));
        assertTrue(source.matches(PatternMaker.make("/*  16:   0 */", "case 0:")));
        assertTrue(source.matches(PatternMaker.make("/*  17:  17 */", "System.out.println(\"0\");")));
        assertTrue(source.matches(PatternMaker.make("/*  18:   0 */", "break;")));

        assertTrue(source.matches(PatternMaker.make("/*  34:   0 */", "case 0:")));
        assertTrue(source.matches(PatternMaker.make("/*  35:  35 */", "System.out.println(\"0\");")));
        assertTrue(source.matches(PatternMaker.make("/*  36:   0 */", "case 1:")));

        assertTrue(source.matches(PatternMaker.make("/*  56:   0 */", "default:")));

        assertTrue(source.matches(PatternMaker.make("/* 110:   0 */", "break;")));
        assertTrue(source.matches(PatternMaker.make("/* 111:   0 */", "case 1:")));
        assertTrue(source.matches(PatternMaker.make("/* 112: 112 */", "System.out.println(\"1\");")));
        assertTrue(source.matches(PatternMaker.make("/* 113: 113 */", "throw new RuntimeException(\"boom\");")));

        assertTrue(source.matches(PatternMaker.make("/* 134:   0 */", "return;")));

        assertTrue(source.matches(PatternMaker.make("/* 171:   0 */", "case 3:")));
        assertTrue(source.matches(PatternMaker.make("/* 172:   0 */", "case 4:")));
        assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "System.out.println(\"3 or 4\");")));
        assertTrue(source.matches(PatternMaker.make("/* 174:   0 */", "break;")));

        assertTrue(source.matches(PatternMaker.make("/* 265:   0 */", "case 1:")));
        assertTrue(source.matches(PatternMaker.make("/* 266:   0 */", "break;")));
        assertTrue(source.matches(PatternMaker.make("/* 267:   0 */", "default:")));

        assertTrue(source.matches(PatternMaker.make("/* 283:   0 */", "case 1:")));
        assertTrue(source.matches(PatternMaker.make("/* 284:   0 */", "case 2:")));
        assertTrue(source.matches(PatternMaker.make("/* 285:   0 */", "case 3:")));
        assertTrue(source.matches(PatternMaker.make("/* 286:   0 */", "break;")));
        assertTrue(source.matches(PatternMaker.make("/* 288:   0 */", "default:")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170AdvancedSwitch() throws Exception {
        String internalClassName = "org/jd/core/test/AdvancedSwitch";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("/* 13: 13 */", "A,", "B,", "C;")));

        assertTrue(source.matches(PatternMaker.make("/* 19: 19 */", "switch (te)")));
        assertTrue(source.matches(PatternMaker.make("/* 20:  0 */", "case A:")));
        assertTrue(source.matches(PatternMaker.make("/* 22:  0 */", "case B:")));
        assertTrue(source.matches(PatternMaker.make("/* 25:  0 */", "case C:")));

        assertTrue(source.matches(PatternMaker.make("/* 39:  0 */", "case A:")));
        assertTrue(source.matches(PatternMaker.make("/* 40:  0 */", "case B:")));
        assertTrue(source.matches(PatternMaker.make("/* 41: 41 */", "System.out.println(\"A or B\");")));

        assertTrue(source.matches(PatternMaker.make("/* 56: 56 */", "switch (str)")));
        assertTrue(source.matches(PatternMaker.make("/* 57:  0 */", "case \"One\":")));
        assertTrue(source.matches(PatternMaker.make("/* 58: 58 */", "System.out.println(1);")));

        assertTrue(source.matches(PatternMaker.make("/* 78:  0 */", "case \"One\":")));
        assertTrue(source.matches(PatternMaker.make("/* 79:  0 */", "case \"POe\":")));
        assertTrue(source.matches(PatternMaker.make("/* 80: 80 */", "System.out.println(\"'One' or 'POe'\");")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testEclipseJavaCompiler321Switch() throws Exception {
        String internalClassName = "org/jd/core/test/Switch";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-eclipse-java-compiler-3.2.1.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("/* 239: 239 */") != -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.5", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testEclipseJavaCompiler3130Switch() throws Exception {
        String internalClassName = "org/jd/core/test/Switch";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-eclipse-java-compiler-3.13.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("/* 239: 239 */") != -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.5", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk118TernaryOperator() throws Exception {
        String internalClassName = "org/jd/core/test/TernaryOperator";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.1.8.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  13 */", "this.str =")));
        assertTrue(source.matches(PatternMaker.make(":  14 */", "(s == null) ?")));
        assertTrue(source.matches(PatternMaker.make(":  15 */", "\"1\"")));
        assertTrue(source.matches(PatternMaker.make(":  16 */", "\"2\";")));
        assertTrue(source.matches(PatternMaker.make(":  24 */", "s = (s == null) ? ((s == null) ? \"1\" : \"2\") : ((s == null) ? \"3\" : \"4\");")));
        assertTrue(source.matches(PatternMaker.make(":  34 */", "return !(s != s || time < time);")));
        assertTrue(source.matches(PatternMaker.make(":  40 */", "if ((s1 == null) ? (s2 == null) : s1.equals(s2))")));
        assertTrue(source.matches(PatternMaker.make(":  60 */", "if ((s1 == null) ? (s2 == null) : s1.equals(s2))")));
        assertTrue(source.matches(PatternMaker.make(":  71 */", "if ((s1 == null) ? false : (s1.length() > 0))")));
        assertTrue(source.matches(PatternMaker.make(":  82 */", "if (s1 != null && s1.length() > 0)")));
        assertTrue(source.matches(PatternMaker.make(": 126 */", "if (s1 == null && false)")));
        assertTrue(source.matches(PatternMaker.make(": 137 */", "if (s1 == s2 && ((s1 == null) ? (s2 == null) : s1.equals(s2)) && s1 == s2)")));
        assertTrue(source.matches(PatternMaker.make(": 148 */", "if (s1 == s2 || ((s1 == null) ? (s2 == null) : s1.equals(s2)) || s1 == s2)")));
        assertTrue(source.matches(PatternMaker.make(": 157 */", "return Short.toString((short)((this == null) ? 1 : 2));")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.3", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170TernaryOperator() throws Exception {
        String internalClassName = "org/jd/core/test/TernaryOperator";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  13 */", "this.str = (s == null) ? \"1\" : \"2\";")));
        assertTrue(source.matches(PatternMaker.make(":  24 */", "s = (s == null) ? ((s == null) ? \"1\" : \"2\") : ((s == null) ? \"3\" : \"4\");")));
        assertTrue(source.matches(PatternMaker.make(":  34 */", "return (s == s && time >= time);")));
        assertTrue(source.matches(PatternMaker.make(":  40 */", "if ((s1 == null) ? (s2 == null) : s1.equals(s2))")));
        assertTrue(source.matches(PatternMaker.make(":  60 */", "if ((s1 == null) ? (s2 == null) : s1.equals(s2))")));
        assertTrue(source.matches(PatternMaker.make(":  71 */", "if (s1 != null && s1.length() > 0)")));
        assertTrue(source.matches(PatternMaker.make(":  82 */", "if (s1 != null && s1.length() > 0)")));
        assertTrue(source.matches(PatternMaker.make(": 126 */", "if (s1 == null);")));
        assertTrue(source.matches(PatternMaker.make(": 137 */", "if (s1 == s2 && ((s1 == null) ? (s2 == null) : s1.equals(s2)) && s1 == s2)")));
        assertTrue(source.matches(PatternMaker.make(": 148 */", "if (s1 == s2 || ((s1 == null) ? (s2 == null) : s1.equals(s2)) || s1 == s2)")));
        assertTrue(source.matches(PatternMaker.make(": 157 */", "return Short.toString((short)((this == null) ? 1 : 2));")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170TryWithResources() throws Exception {
        String internalClassName = "org/jd/core/test/TryWithResources";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  12 */", "try (FileInputStream input = new FileInputStream(path))")));

        assertTrue(source.matches(PatternMaker.make(":  49 */", "try (FileInputStream input = new FileInputStream(path))")));
        assertTrue(source.matches(PatternMaker.make(":  57 */", "e.printStackTrace();")));
        assertTrue(source.matches(PatternMaker.make(":  59 */", "System.out.println(\"finally\");")));

        assertTrue(source.matches(PatternMaker.make(": 121 */", "try(FileInputStream input = new FileInputStream(pathIn);")));
        assertTrue(source.matches(PatternMaker.make(": 122 */", "BufferedInputStream bufferedInput = new BufferedInputStream(input);")));
        assertTrue(source.matches(PatternMaker.make(": 123 */", "FileOutputStream output = new FileOutputStream(pathOut);")));
        assertTrue(source.matches(PatternMaker.make(": 124 */", "BufferedOutputStream bufferedOutput = new BufferedOutputStream(output))")));
        assertTrue(source.matches(PatternMaker.make(": 132 */", "if (data == -7)")));
        assertTrue(source.matches(PatternMaker.make(": 133 */", "return 1;")));
        assertTrue(source.matches(PatternMaker.make(": 142 */", "return 2;")));
        assertTrue(source.matches(PatternMaker.make(": 144 */", "e.printStackTrace();")));
        assertTrue(source.matches(PatternMaker.make(": 150 */", "e.printStackTrace();")));
        assertTrue(source.matches(PatternMaker.make(": 152 */", "System.out.println(\"finally, before loop\");")));
        assertTrue(source.matches(PatternMaker.make(": 156 */", "System.out.println(\"finally, after loop\");")));
        assertTrue(source.matches(PatternMaker.make(": 159 */", "System.out.println(\"finally\");")));
        assertTrue(source.matches(PatternMaker.make(": 162 */", "return 3;")));

        assertTrue(source.indexOf("/* 162: 162 */") != -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk180TryWithResources() throws Exception {
        String internalClassName = "org/jd/core/test/TryWithResources";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.8.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  12 */", "try (FileInputStream input = new FileInputStream(path))")));

        assertTrue(source.matches(PatternMaker.make(":  49 */", "try (FileInputStream input = new FileInputStream(path))")));
        assertTrue(source.matches(PatternMaker.make(":  57 */", "e.printStackTrace();")));
        assertTrue(source.matches(PatternMaker.make(":  59 */", "System.out.println(\"finally\");")));

        assertTrue(source.matches(PatternMaker.make(": 121 */", "try(FileInputStream input = new FileInputStream(pathIn);")));
        assertTrue(source.matches(PatternMaker.make(": 122 */", "BufferedInputStream bufferedInput = new BufferedInputStream(input);")));
        assertTrue(source.matches(PatternMaker.make(": 123 */", "FileOutputStream output = new FileOutputStream(pathOut);")));
        assertTrue(source.matches(PatternMaker.make(": 124 */", "BufferedOutputStream bufferedOutput = new BufferedOutputStream(output))")));
        assertTrue(source.matches(PatternMaker.make(": 132 */", "if (data == -7)")));
        assertTrue(source.matches(PatternMaker.make(": 133 */", "return 1;")));
        assertTrue(source.matches(PatternMaker.make(": 142 */", "return 2;")));
        assertTrue(source.matches(PatternMaker.make(": 144 */", "e.printStackTrace();")));
        assertTrue(source.matches(PatternMaker.make(": 150 */", "e.printStackTrace();")));
        assertTrue(source.matches(PatternMaker.make(": 152 */", "System.out.println(\"finally, before loop\");")));
        assertTrue(source.matches(PatternMaker.make(": 156 */", "System.out.println(\"finally, after loop\");")));
        assertTrue(source.matches(PatternMaker.make(": 159 */", "System.out.println(\"finally\");")));
        assertTrue(source.matches(PatternMaker.make(": 162 */", "return 3;")));

        assertTrue(source.indexOf("/* 162: 162 */") != -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170Synchronised() throws Exception {
        String internalClassName = "org/jd/core/test/Synchronized";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  11 */", "synchronized (paramStringBuilder)")));
        assertTrue(source.matches(PatternMaker.make(":  13 */", "inSynchronized();")));
        assertTrue(source.matches(PatternMaker.make(":  15 */", "return 2;")));

        assertTrue(source.matches(PatternMaker.make(":  20 */", "synchronized (paramStringBuilder)")));
        assertTrue(source.matches(PatternMaker.make(":  22 */", "inSynchronized();")));
        assertTrue(source.matches(PatternMaker.make(":  23 */", "return 2;")));

        assertTrue(source.matches(PatternMaker.make(":  29 */", "synchronized (paramStringBuilder)")));
        assertTrue(source.matches(PatternMaker.make(":  31 */", "inSynchronized();")));
        assertTrue(source.matches(PatternMaker.make(":   0 */", "return;")));

        assertTrue(source.matches(PatternMaker.make(":  73 */", "synchronized (paramStringBuilder)")));
        assertTrue(source.matches(PatternMaker.make(":  75 */", "inSynchronized();")));
        assertTrue(source.matches(PatternMaker.make(":  76 */", "throw new RuntimeException();")));

        assertTrue(source.matches(PatternMaker.make(":  95 */", "synchronized (s)")));
        assertTrue(source.matches(PatternMaker.make(":  97 */", "return subContentEquals(s);")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testEclipseJavaCompiler321TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-eclipse-java-compiler-3.2.1.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("catch (RuntimeException runtimeexception)") != -1);
        assertTrue(source.matches(PatternMaker.make("/*  45:  45 */", "inCatch1();")));
        assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

        assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

        assertTrue(source.indexOf("/* 888: 888 */") != -1);

        assertTrue(source.indexOf("long l = System.currentTimeMillis(); return l;") == -1);
        assertTrue(source.indexOf("catch (RuntimeException null)") == -1);
        assertTrue(source.indexOf("Object object;") == -1);
        assertTrue(source.indexOf("RuntimeException runtimeexception4;") == -1);
        assertTrue(source.indexOf("Exception exception8;") == -1);

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.5", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testEclipseJavaCompiler370TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-eclipse-java-compiler-3.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("catch (RuntimeException runtimeException)") != -1);
        assertTrue(source.matches(PatternMaker.make("/*  48:  48 */", "inCatch2();")));
        assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

        assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

        assertTrue(source.matches(PatternMaker.make(": 393 */", "inCatch1();")));
        assertTrue(source.matches(PatternMaker.make(": 395 */", "inCatch2();")));
        assertTrue(source.matches(PatternMaker.make(": 397 */", "inCatch3();")));
        assertTrue(source.matches(PatternMaker.make(": 399 */", "inFinally();")));
        assertTrue(source.indexOf("/* 400:   0]     inFinally();") == -1);

        assertTrue(source.matches(PatternMaker.make(": 424 */", "inTry();")));
        assertTrue(source.matches(PatternMaker.make(": 427 */", "inFinally();")));
        assertTrue(source.matches(PatternMaker.make(": 431 */", "inTryA();")));
        assertTrue(source.matches(PatternMaker.make(": 434 */", "inFinallyA();")));
        assertTrue(source.matches(PatternMaker.make(": 439 */", "inTryC();")));
        assertTrue(source.matches(PatternMaker.make(": 442 */", "inFinallyC();")));
        assertTrue(source.matches(PatternMaker.make(": 445 */", "inFinally();")));

        assertTrue(source.indexOf("/* 888: 888 */") != -1);

        assertTrue(source.indexOf("long l = System.currentTimeMillis(); return l;") == -1);
        assertTrue(source.indexOf("catch (RuntimeException null)") == -1);
        assertTrue(source.indexOf("Object object;") == -1);
        assertTrue(source.indexOf("RuntimeException runtimeexception4;") == -1);
        assertTrue(source.indexOf("Exception exception8;") == -1);

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.5", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testEclipseJavaCompiler3130TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-eclipse-java-compiler-3.13.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("catch (RuntimeException runtimeException)") != -1);
        assertTrue(source.matches(PatternMaker.make("/*  48:  48 */", "inCatch2();")));
        assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

        assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

        assertTrue(source.matches(PatternMaker.make(": 393 */", "inCatch1();")));
        assertTrue(source.matches(PatternMaker.make(": 395 */", "inCatch2();")));
        assertTrue(source.matches(PatternMaker.make(": 397 */", "inCatch3();")));
        assertTrue(source.matches(PatternMaker.make(": 399 */", "inFinally();")));

        assertTrue(source.matches(PatternMaker.make(": 424 */", "inTry();")));
        assertTrue(source.matches(PatternMaker.make(": 427 */", "inFinally();")));
        assertTrue(source.matches(PatternMaker.make(": 431 */", "inTryA();")));
        assertTrue(source.matches(PatternMaker.make(": 434 */", "inFinallyA();")));
        assertTrue(source.matches(PatternMaker.make(": 439 */", "inTryC();")));
        assertTrue(source.matches(PatternMaker.make(": 442 */", "inFinallyC();")));
        assertTrue(source.matches(PatternMaker.make(": 445 */", "inFinally();")));

        assertTrue(source.indexOf("/* 888: 888 */") != -1);

        assertTrue(source.indexOf("long l = System.currentTimeMillis(); return l;") == -1);
        assertTrue(source.indexOf("catch (RuntimeException null)") == -1);
        assertTrue(source.indexOf("Object object;") == -1);
        assertTrue(source.indexOf("RuntimeException runtimeexception4;") == -1);
        assertTrue(source.indexOf("Exception exception8;") == -1);

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.5", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk118TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.1.8.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("catch (RuntimeException runtimeexception)") != -1);
        assertTrue(source.matches(PatternMaker.make("/*  48:  48 */", "inCatch2();")));
        assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

        assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

        assertTrue(source.matches(PatternMaker.make(": 393 */", "inCatch1();")));
        assertTrue(source.matches(PatternMaker.make(": 395 */", "inCatch2();")));
        assertTrue(source.matches(PatternMaker.make(": 397 */", "inCatch3();")));
        assertTrue(source.matches(PatternMaker.make(": 399 */", "inFinally();")));

        assertTrue(source.matches(PatternMaker.make(": 424 */", "inTry();")));
        assertTrue(source.matches(PatternMaker.make(": 427 */", "inFinally();")));
        assertTrue(source.matches(PatternMaker.make(": 431 */", "inTryA();")));
        assertTrue(source.matches(PatternMaker.make(": 434 */", "inFinallyA();")));
        assertTrue(source.matches(PatternMaker.make(": 439 */", "inTryC();")));
        assertTrue(source.matches(PatternMaker.make(": 442 */", "inFinallyC();")));
        assertTrue(source.matches(PatternMaker.make(": 445 */", "inFinally();")));

        assertTrue(source.indexOf("/* 902: 902 */") != -1);

        assertTrue(source.indexOf("long l = System.currentTimeMillis(); return l;") == -1);
        assertTrue(source.indexOf("catch (RuntimeException null)") == -1);
        assertTrue(source.indexOf("Object object;") == -1);
        assertTrue(source.indexOf("RuntimeException runtimeexception4;") == -1);
        assertTrue(source.indexOf("Exception exception8;") == -1);

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.3", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk131TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.3.1.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("catch (RuntimeException runtimeexception)") != -1);
        assertTrue(source.matches(PatternMaker.make("/*  48:  48 */", "inCatch2();")));
        assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

        assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

        assertTrue(source.matches(PatternMaker.make(": 393 */", "inCatch1();")));
        assertTrue(source.matches(PatternMaker.make(": 395 */", "inCatch2();")));
        assertTrue(source.matches(PatternMaker.make(": 397 */", "inCatch3();")));
        assertTrue(source.matches(PatternMaker.make(": 399 */", "inFinally();")));

        assertTrue(source.matches(PatternMaker.make(": 424 */", "inTry();")));
        assertTrue(source.matches(PatternMaker.make(": 427 */", "inFinally();")));
        assertTrue(source.matches(PatternMaker.make(": 445 */", "inFinally();")));

        assertTrue(source.indexOf("/* 902: 902 */") != -1);

        assertTrue(source.indexOf("long l = System.currentTimeMillis(); return l;") == -1);
        assertTrue(source.indexOf("catch (RuntimeException null)") == -1);
        assertTrue(source.indexOf("Object object;") == -1);
        assertTrue(source.indexOf("RuntimeException runtimeexception4;") == -1);
        assertTrue(source.indexOf("Exception exception8;") == -1);

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.3", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170TryCatchFinally() throws Exception {
        String internalClassName = "org/jd/core/test/TryCatchFinally";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("catch (RuntimeException runtimeexception)") != -1);
        assertTrue(source.matches(PatternMaker.make("/*  48:  48 */", "inCatch2();")));
        assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "return;")));

        assertTrue(source.matches(PatternMaker.make(": 166 */", "return System.currentTimeMillis();")));

        assertTrue(source.matches(PatternMaker.make(": 192 */", "catch (RuntimeException e) {}")));
        assertTrue(source.matches(PatternMaker.make("/* 204:   0 */", "finally {}")));

        assertTrue(source.matches(PatternMaker.make(": 393 */", "inCatch1();")));
        assertTrue(source.matches(PatternMaker.make(": 395 */", "inCatch2();")));
        assertTrue(source.matches(PatternMaker.make(": 397 */", "inCatch3();")));
        assertTrue(source.matches(PatternMaker.make(": 399 */", "inFinally();")));

        assertTrue(source.matches(PatternMaker.make(": 424 */", "inTry();")));
        assertTrue(source.matches(PatternMaker.make(": 427 */", "inFinally();")));
        assertTrue(source.matches(PatternMaker.make(": 431 */", "inTryA();")));
        assertTrue(source.matches(PatternMaker.make(": 434 */", "inFinallyA();")));
        assertTrue(source.matches(PatternMaker.make(": 439 */", "inTryC();")));
        assertTrue(source.matches(PatternMaker.make(": 442 */", "inFinallyC();")));
        assertTrue(source.matches(PatternMaker.make(": 445 */", "inFinally();")));

        assertTrue(source.indexOf("/* 902: 902 */") != -1);

        assertTrue(source.indexOf("long l = System.currentTimeMillis(); return l;") == -1);
        assertTrue(source.indexOf("catch (RuntimeException null)") == -1);
        assertTrue(source.indexOf("Object object;") == -1);
        assertTrue(source.indexOf("RuntimeException runtimeexception4;") == -1);
        assertTrue(source.indexOf("Exception exception8;") == -1);

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170AnnotatedClass() throws Exception {
        String internalClassName = "org/jd/core/test/AnnotatedClass";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        // PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("@Quality(Quality.Level.HIGH)") != -1);
        assertTrue(source.indexOf("@Author(value = @Name(salutation = \"Mr\", value = \"Donald\", last = \"Duck\"), contributors = {@Name(\"Huey\"), @Name(\"Dewey\"), @Name(\"Louie\")})") != -1);
        assertTrue(source.indexOf("@Value(z = true)") != -1);
        assertTrue(source.indexOf("@Value(b = -15)") != -1);
        assertTrue(source.indexOf("@Value(s = -15)") != -1);
        assertTrue(source.indexOf("@Value(i = 1)") != -1);
        assertTrue(source.indexOf("@Value(l = 1234567890123456789L)") != -1);
        assertTrue(source.indexOf("@Value(f = 123.456F)") != -1);
        assertTrue(source.indexOf("@Value(d = 789.101112D)") != -1);
        assertTrue(source.indexOf("@Value(str = \"str\")") != -1);
        assertTrue(source.indexOf("@Value(str = \"str \u0083 उ ᄉ\")") != -1);
        assertTrue(source.indexOf("@Value(clazz = String.class)") != -1);
        assertTrue(source.indexOf("public void ping(@Deprecated Writer writer, @Deprecated @Value(str = \"localhost\") String host, long timeout)") != -1);

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source),
                new JavaSourceFileObject("org/jd/core/test/annotation/Author", "package org.jd.core.test.annotation; public @interface Author {Name value(); Name[] contributors() default {};}"),
                new JavaSourceFileObject("org/jd/core/test/annotation/Name", "package org.jd.core.test.annotation; public @interface Name {String salutation() default \"\"; String value(); String last() default \"\";}"),
                new JavaSourceFileObject("org/jd/core/test/annotation/Quality", "package org.jd.core.test.annotation; public @interface Quality {enum Level {LOW,MIDDLE,HIGH}; Level value();}"),
                new JavaSourceFileObject("org/jd/core/test/annotation/Value", "package org.jd.core.test.annotation; public @interface Value {boolean z() default true; byte b() default 1; short s() default 1; int i() default 1; long l() default 1L; float f() default 1.0F; double d() default 1.0D; String str() default \"str\"; Class clazz() default Object.class;}")));
    }

    @Test
    public void testJdk170AnonymousClass() throws Exception {
        String internalClassName = "org/jd/core/test/AnonymousClass";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        // PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  21 */", "Object obj = new Object()")));
        assertTrue(source.matches(PatternMaker.make(":  23 */", "return \"toString() return \" + super.toString() + \" at \" + AnonymousClass.this.time;")));

        assertTrue(source.matches(PatternMaker.make(":  39 */", "Enumeration e = new Enumeration()")));
        assertTrue(source.matches(PatternMaker.make(":  40 */", "Iterator<String> i = AnonymousClass.this.list.iterator();")));
        assertTrue(source.matches(PatternMaker.make(":  44 */", "return (this.i.hasNext() && s1 == s2 && i1 > l1);")));

        assertTrue(source.matches(PatternMaker.make(":  61 */", "final int i = s1.length();")));
        assertTrue(source.matches(PatternMaker.make(":  63 */", "System.out.println(\"2\" + new StringWrapper(123456L)")));
        assertTrue(source.matches(PatternMaker.make(":  67 */", "if (s1 == s2 && i == 5)")));
        assertTrue(source.matches(PatternMaker.make("/*  72:   0 */", "} + \"3\");")));

        assertTrue(source.matches(PatternMaker.make(":  81 */", "final Object abc = \"abc\";")));
        assertTrue(source.matches(PatternMaker.make(":  82 */", "final Object def = \"def\";")));
        assertTrue(source.matches(PatternMaker.make(":  84 */", "Serializable serializable = new Serializable()")));
        assertTrue(source.matches(PatternMaker.make(":  90 */", "Serializable serializable = new Serializable()")));
        assertTrue(source.matches(PatternMaker.make(":  96 */", "return (abc.equals(obj) || def.equals(obj) || ghi.equals(obj) || jkl.equals(obj));")));
        assertTrue(source.matches(PatternMaker.make(": 100 */", "return (abc.equals(obj) || def.equals(obj));")));
        assertTrue(source.matches(PatternMaker.make("/* 102:   0 */", "};")));

        assertTrue(source.matches(PatternMaker.make(": 111 */", "this.l = l & 0x80L;")));

        assertTrue(source.indexOf("} ;") == -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source),
                                               new JavaSourceFileObject("org/jd/core/test/annotation/Name", "package org.jd.core.test.annotation; public @interface Name {String value();}")));
    }

    @Test
    public void testJdk170GenericClass() throws Exception {
        String internalClassName = "org/jd/core/test/GenericClass";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.indexOf("public class GenericClass<T1, T2, T3 extends AnnotatedClass, T4 extends Serializable, T5 extends Serializable & Comparable, T6 extends AnnotatedClass & Serializable & Comparable<GenericClass>, T7 extends Map<?, ?>, T8 extends Map<? extends Number, ? super Serializable>, T9 extends T8>") != -1);
        assertTrue(source.indexOf("extends ArrayList<T7>") != -1);
        assertTrue(source.indexOf("implements Serializable, Comparable<T1>") != -1);

        assertTrue(source.matches(PatternMaker.make("/*  26:  26 */", "public List<List<? extends GenericClass>> list1 = new ArrayList();")));
        assertTrue(source.indexOf("public List<List<? super GenericClass>> list2;") != -1);
        assertTrue(source.matches(PatternMaker.make("/*  31:  31 */", "list2 = new ArrayList();")));

        assertTrue(source.indexOf("public <T> void fromArrayToCollection(T[] a, Collection<T> c)") != -1);
        assertTrue(source.indexOf("public <T> void copy(List<T> dest, List<? extends T> src)") != -1);
        assertTrue(source.indexOf("public <T, S extends T> List<? extends Number> copy2(List<? super T> dest, List<S> src) throws InvalidParameterException, ClassCastException") != -1);
        assertTrue(source.indexOf("public <T1, T2 extends Exception> List<? extends Number> print(List<? super T1> list) throws T2, InvalidParameterException") != -1);

        assertTrue(source.matches(PatternMaker.make(": 100 */", "return (T1)call(0);")));
        assertTrue(source.matches(PatternMaker.make(": 104 */", "return (T1)this;")));

        assertTrue(source.indexOf("/* 104: 104 */") != -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source),
                                               new JavaSourceFileObject("org/jd/core/test/AnnotatedClass", "package org.jd.core.test; public class AnnotatedClass {}")));
    }

    @Test
    public void testJdk170AnnotationAuthor() throws Exception {
        String internalClassName = "org/jd/core/test/annotation/Author";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("/*   3:   0 */", "public @interface Author")));
        assertTrue(source.matches(PatternMaker.make("/*   4:   0 */", "Name value();")));
        assertTrue(source.matches(PatternMaker.make("/*   6:   0 */", "Name[] contributors() default {};")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source),
                                               new JavaSourceFileObject("org/jd/core/test/annotation/Name", "package org.jd.core.test.annotation; public @interface Name {String value();}")));
    }

    @Test
    public void testJdk170AnnotationValue() throws Exception {
        String internalClassName = "org/jd/core/test/annotation/Value";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("/*   8:   0 */", "@Retention(RetentionPolicy.RUNTIME)")));
        assertTrue(source.matches(PatternMaker.make("/*   9:   0 */", "@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})")));
        assertTrue(source.matches(PatternMaker.make("/*  10:   0 */", "public @interface Value {")));
        assertTrue(source.matches(PatternMaker.make("/*  11:   0 */", "boolean z() default true;")));
        assertTrue(source.matches(PatternMaker.make("/*  13:   0 */", "byte b() default 1;")));
        assertTrue(source.matches(PatternMaker.make("/*  25:   0 */", "String str() default \"str\";")));
        assertTrue(source.matches(PatternMaker.make("/*  27:   0 */", "Class<?> clazz() default Object.class;")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170OuterClass() throws Exception {
        String internalClassName = "org/jd/core/test/OuterClass";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //ClassPathLoader loader = new ClassPathLoader();
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  10 */", "protected int outerField1 = 0;")));
        assertTrue(source.matches(PatternMaker.make(":  11 */", "protected String[] outerField2 = { \"0\" };")));

        assertTrue(source.indexOf("final int localVariable1 = param1;") != -1);
        assertTrue(source.indexOf("final String[] localVariable2 = param2;") != -1);

        assertTrue(source.matches(PatternMaker.make(":  21 */", "InnerClass innerClass = new InnerClass(param1, param2);")));
        assertTrue(source.matches(PatternMaker.make(":  22 */", "innerClass.innerMethod(localVariable1, localVariable2);")));
        assertTrue(source.matches(PatternMaker.make(":  24 */", "StaticInnerClass staticInnerClass = new StaticInnerClass(param1, param2);")));
        assertTrue(source.matches(PatternMaker.make(":  25 */", "staticInnerClass.innerMethod(localVariable1, localVariable2);")));

        assertTrue(source.matches(PatternMaker.make(":  27 */", "InnerClass anonymousClass = new InnerClass(param1, param2)")));
        assertTrue(source.indexOf("public void innerMethod(int param1, String... param2)") != -1);
        assertTrue(source.matches(PatternMaker.make(":  30 */", "this.innerField2 = param2;")));
        assertTrue(source.matches(PatternMaker.make(":  32 */", "OuterClass.this.outerField1 = param1;")));
        assertTrue(source.matches(PatternMaker.make(":  33 */", "OuterClass.this.outerField2 = param2;")));
        assertTrue(source.matches(PatternMaker.make(":  35 */", "this.innerField1 = localVariable1;")));
        assertTrue(source.matches(PatternMaker.make(":  36 */", "this.innerField2 = localVariable2;")));

        assertTrue(source.matches(PatternMaker.make(":  39 */", "anonymousClass.innerMethod(localVariable1, localVariable2);")));

        assertTrue(source.matches(PatternMaker.make(":  41 */", "StaticInnerClass staticAnonymousClass = new StaticInnerClass(param1, param2)")));
        assertTrue(source.matches(PatternMaker.make(":  44 */", "this.innerField2 = param2;")));
        assertTrue(source.matches(PatternMaker.make(":  46 */", "OuterClass.this.outerField1 = param1;")));
        assertTrue(source.matches(PatternMaker.make(":  47 */", "OuterClass.this.outerField2 = param2;")));
        assertTrue(source.matches(PatternMaker.make(":  49 */", "this.innerField1 = localVariable1;")));
        assertTrue(source.matches(PatternMaker.make(":  50 */", "this.innerField2 = localVariable2;")));

        assertTrue(source.matches(PatternMaker.make(":  53 */", "staticAnonymousClass.innerMethod(localVariable1, localVariable2);")));

        assertTrue(source.matches(PatternMaker.make(":  55 */", "InnerEnum.A.innerMethod(localVariable1, localVariable2);")));

        assertTrue(source.matches(PatternMaker.make("/*  56:   0 */", "class LocalClass")));
        assertTrue(source.matches(PatternMaker.make(":  58 */", "protected int innerField1 = 0;")));
        assertTrue(source.matches(PatternMaker.make(":  59 */", "protected String[] innerField2 = { \"0\" } ;")));
        assertTrue(source.matches(PatternMaker.make(":  69 */", "this.innerField1 = param1;")));
        assertTrue(source.matches(PatternMaker.make(":  70 */", "this.innerField2 = param2;")));
        assertTrue(source.matches(PatternMaker.make(":  72 */", "OuterClass.this.outerField1 = param1;")));
        assertTrue(source.matches(PatternMaker.make(":  73 */", "OuterClass.this.outerField2 = param2;")));
        assertTrue(source.matches(PatternMaker.make(":  75 */", "this.innerField1 = localVariable1;")));
        assertTrue(source.matches(PatternMaker.make(":  76 */", "this.innerField2 = localVariable2;")));
        assertTrue(source.matches(PatternMaker.make(":  94 */", "LocalClass localClass = new LocalClass(param1, param2);")));
        assertTrue(source.matches(PatternMaker.make(":  95 */", "localClass.localMethod(localVariable1, localVariable2);")));

        assertTrue(source.matches(PatternMaker.make(": 114 */", "this(param1, param2);")));
        assertTrue(source.matches(PatternMaker.make(": 144 */", "this(param1, param2);")));

        assertTrue(source.matches(PatternMaker.make(": 158 */", "A,", "B,", "C;")));
        assertTrue(source.indexOf("/* 182: 182 */") != -1);

        assertTrue(source.matches(PatternMaker.make("public class InnerInnerClass", "{", "}")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk170Enum() throws Exception {
        String internalClassName = "org/jd/core/test/Enum";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  5 */", "SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY;")));

        assertTrue(source.matches(PatternMaker.make(":  9 */", "MERCURY(3.303E23D, 2439700.0D),")));
        assertTrue(source.matches(PatternMaker.make(": 17 */", "URANUS(8.686E25D, 2.5559E7D),")));
        assertTrue(source.matches(PatternMaker.make(": 20 */", "NEPTUNE(1.024E26D, 2.4746E7D);")));
        assertTrue(source.indexOf("this.mass = mass;") != -1);
        assertTrue(source.matches(PatternMaker.make(": 27 */", "this.radius = radius;")));
        assertTrue(source.matches(PatternMaker.make(": 37 */", "return 6.673E-11D * this.mass / this.radius * this.radius;")));
        assertTrue(source.matches(PatternMaker.make(": 49 */", "double earthWeight = Double.parseDouble(args[0]);")));
        assertTrue(source.matches(PatternMaker.make(": 50 */", "double mass = earthWeight / EARTH.surfaceGravity();")));
        assertTrue(source.matches(PatternMaker.make(": 51 */", "for (Planet p : values()) {")));
        assertTrue(source.matches(PatternMaker.make(": 52 */", "System.out.printf(\"Your weight on %s is %f%n\", new Object[]", "{ p, Double.valueOf(p.surfaceWeight(mass)) } );")));

        assertTrue(source.matches(PatternMaker.make("enum EmptyEnum {}")));

        assertTrue(source.indexOf("public static final enum") == -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk901Enum() throws Exception {
        String internalClassName = "org/jd/core/test/Enum";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-9.0.1.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  5 */", "SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY;")));

        assertTrue(source.matches(PatternMaker.make(":  9 */", "MERCURY(3.303E23D, 2439700.0D),")));
        assertTrue(source.matches(PatternMaker.make(": 17 */", "URANUS(8.686E25D, 2.5559E7D),")));
        assertTrue(source.matches(PatternMaker.make(": 20 */", "NEPTUNE(1.024E26D, 2.4746E7D);")));
        assertTrue(source.indexOf("this.mass = mass;") != -1);
        assertTrue(source.matches(PatternMaker.make(": 27 */", "this.radius = radius;")));
        assertTrue(source.matches(PatternMaker.make(": 37 */", "return 6.673E-11D * this.mass / this.radius * this.radius;")));
        assertTrue(source.matches(PatternMaker.make(": 49 */", "double earthWeight = Double.parseDouble(args[0]);")));
        assertTrue(source.matches(PatternMaker.make(": 50 */", "double mass = earthWeight / EARTH.surfaceGravity();")));
        assertTrue(source.matches(PatternMaker.make(": 51 */", "for (Planet p : values()) {")));
        assertTrue(source.matches(PatternMaker.make(": 52 */", "System.out.printf(\"Your weight on %s is %f%n\", new Object[]", "{ p, Double.valueOf(p.surfaceWeight(mass)) } );")));

        assertTrue(source.matches(PatternMaker.make("enum EmptyEnum {}")));

        assertTrue(source.indexOf("public static final enum") == -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk1002Enum() throws Exception {
        String internalClassName = "org/jd/core/test/Enum";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-10.0.2.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  5 */", "SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY;")));

        assertTrue(source.matches(PatternMaker.make(":  9 */", "MERCURY(3.303E23D, 2439700.0D),")));
        assertTrue(source.matches(PatternMaker.make(": 17 */", "URANUS(8.686E25D, 2.5559E7D),")));
        assertTrue(source.matches(PatternMaker.make(": 20 */", "NEPTUNE(1.024E26D, 2.4746E7D);")));
        assertTrue(source.indexOf("this.mass = mass;") != -1);
        assertTrue(source.matches(PatternMaker.make(": 27 */", "this.radius = radius;")));
        assertTrue(source.matches(PatternMaker.make(": 37 */", "return 6.673E-11D * this.mass / this.radius * this.radius;")));
        assertTrue(source.matches(PatternMaker.make(": 49 */", "double earthWeight = Double.parseDouble(args[0]);")));
        assertTrue(source.matches(PatternMaker.make(": 50 */", "double mass = earthWeight / EARTH.surfaceGravity();")));
        assertTrue(source.matches(PatternMaker.make(": 51 */", "for (Planet p : values()) {")));
        assertTrue(source.matches(PatternMaker.make(": 52 */", "System.out.printf(\"Your weight on %s is %f%n\", new Object[]", "{ p, Double.valueOf(p.surfaceWeight(mass)) } );")));

        assertTrue(source.matches(PatternMaker.make("enum EmptyEnum {}")));

        assertTrue(source.indexOf("public static final enum") == -1);
        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk118Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.1.8.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  43 */", "Class class3 = String.class, class2 = class3, class1 = class2;")));
        assertTrue(source.matches(PatternMaker.make("String stringNull = null;")));
        assertTrue(source.indexOf("public static native int read();") != -1);
        assertTrue(source.matches(PatternMaker.make(": 128 */", "int int78 = getInt78(new Object[] { this }, (short)5);")));
        assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "return String.valueOf(str) + str;")));
        assertTrue(source.matches(PatternMaker.make("/* 176: 176 */", "return str;")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.3", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk142Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.4.2.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  18 */", "protected short short56 = 56;")));
        assertTrue(source.matches(PatternMaker.make(":  19 */", "protected int int78 = 78;")));
        assertTrue(source.matches(PatternMaker.make(":  43 */", "Class class3 = String.class, class2 = class3, class1 = class2;")));
        assertTrue(source.matches(PatternMaker.make("String stringNull = null;")));
        assertTrue(source.indexOf("public static native int read();") != -1);
        assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "return str + str;")));
        assertTrue(source.matches(PatternMaker.make("/* 176: 176 */", "return str;")));
        assertTrue(source.matches(PatternMaker.make("/* 185: 185 */", "return ((Basic)objects[index]).int78;")));
        assertTrue(source.matches(PatternMaker.make("/* 188: 188 */", "protected static final Integer INTEGER_255 = new Integer(255);")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.4", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk901Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-9.0.1.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  18 */", "protected short short56 = 56;")));
        assertTrue(source.matches(PatternMaker.make(":  19 */", "protected int int78 = 78;")));
        assertTrue(source.matches(PatternMaker.make(":  43 */", "Class<String> class3 = String.class, class2 = class3, class1 = class2;")));
        assertTrue(source.matches(PatternMaker.make("String stringNull = null;")));
        assertTrue(source.indexOf("public static native int read();") != -1);
        assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "return str + str;")));
        assertTrue(source.matches(PatternMaker.make("/* 176: 176 */", "return str;")));
        assertTrue(source.matches(PatternMaker.make("/* 185: 185 */", "return ((Basic)objects[index]).int78;")));
        assertTrue(source.matches(PatternMaker.make("/* 188: 188 */", "protected static final Integer INTEGER_255 = new Integer(255);")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk1002Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-10.0.2.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);
        message.setHeader("configuration", configuration);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  18 */", "protected short short56 = 56;")));
        assertTrue(source.matches(PatternMaker.make(":  19 */", "protected int int78 = 78;")));
        assertTrue(source.matches(PatternMaker.make(":  43 */", "Class<String> class3 = String.class, class2 = class3, class1 = class2;")));
        assertTrue(source.matches(PatternMaker.make("String stringNull = null;")));
        assertTrue(source.indexOf("public static native int read();") != -1);
        assertTrue(source.matches(PatternMaker.make("/* 173: 173 */", "return str + str;")));
        assertTrue(source.matches(PatternMaker.make("/* 176: 176 */", "return str;")));
        assertTrue(source.matches(PatternMaker.make("/* 185: 185 */", "return ((Basic)objects[index]).int78;")));
        assertTrue(source.matches(PatternMaker.make("/* 188: 188 */", "protected static final Integer INTEGER_255 = new Integer(255);")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk180Lambda() throws Exception {
        String internalClassName = "org/jd/core/test/Lambda";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.8.0.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(": 16 */", "list.forEach(System.out::println);")));
        assertTrue(source.matches(PatternMaker.make(": 20 */", "list.stream().filter(s -> (s != null)).forEach(s -> System.out.println(s));")));
        assertTrue(source.indexOf("Predicate<String> filter = s -> (s.length() == length);") != -1);
        assertTrue(source.indexOf("Consumer<String> println = s -> System.out.println(s);") != -1);
        assertTrue(source.matches(PatternMaker.make(": 27 */", "list.stream().filter(filter).forEach(println);")));
        assertTrue(source.matches(PatternMaker.make(": 31 */", "((Map)list.stream()")));
        assertTrue(source.matches(PatternMaker.make(": 32 */", ".collect(Collectors.toMap(lambda -> Integer.valueOf(lambda.index), Function.identity())))")));
        assertTrue(source.matches(PatternMaker.make(": 33 */", ".forEach((key, value) ->")));
        assertTrue(source.matches(PatternMaker.make(": 48 */", "Thread thread = new Thread(() -> {")));
        assertTrue(source.matches(PatternMaker.make(": 58 */", "Consumer<String> staticMethodReference = String::valueOf;")));
        assertTrue(source.matches(PatternMaker.make(": 59 */", "BiFunction<String, String, Integer> methodReference = String::compareTo;")));
        assertTrue(source.matches(PatternMaker.make(": 60 */", "Supplier<String> instanceMethodReference = s::toString;")));
        assertTrue(source.matches(PatternMaker.make(": 61 */", "Supplier<String> constructorReference = String::new;")));
        assertTrue(source.matches(PatternMaker.make(": 65 */", "MethodType mtToString = MethodType.methodType(String.class);")));
        assertTrue(source.matches(PatternMaker.make(": 66 */", "MethodType mtSetter = MethodType.methodType(void.class, Object.class);")));
        assertTrue(source.matches(PatternMaker.make(": 67 */", "MethodType mtStringComparator = MethodType.methodType(int[].class, String.class, new Class[]", "{ String.class")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk901InterfaceWithDefaultMethods() throws Exception {
        String internalClassName = "org/jd/core/test/InterfaceWithDefaultMethods";
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-9.0.1.zip");
        Loader loader = new ZipLoader(is);
        //PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        PlainTextPrinter printer = new PlainTextPrinter();

        Message message = new Message();
        message.setHeader("mainInternalTypeName", internalClassName);
        message.setHeader("loader", loader);
        message.setHeader("printer", printer);

        deserializer.process(message);
        converter.process(message);
        fragmenter.process(message);
        layouter.process(message);
        tokenizer.process(message);
        writer.process(message);

        String source = printer.toString();

        printSource(source);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("public interface InterfaceWithDefaultMethods")));
        assertTrue(source.matches(PatternMaker.make("void setTime(int paramInt1, int paramInt2, int paramInt3);")));
        assertTrue(source.matches(PatternMaker.make("LocalDateTime getLocalDateTime();")));
        assertTrue(source.matches(PatternMaker.make("static ZoneId getZoneId(String zoneString)")));
        assertTrue(source.matches(PatternMaker.make(": 24 */", "return unsafeGetZoneId(zoneString);")));
        assertTrue(source.matches(PatternMaker.make(": 26 */", "System.err.println(\"Invalid time zone: \" + zoneString + \"; using default time zone instead.\");")));
        assertTrue(source.matches(PatternMaker.make(": 27 */", "return ZoneId.systemDefault();")));
        assertTrue(source.matches(PatternMaker.make(": 32 */", "default ZonedDateTime getZonedDateTime(String zoneString) { return getZonedDateTime(getLocalDateTime(), getZoneId(zoneString)); }")));
        assertTrue(source.matches(PatternMaker.make(": 36 */", "private static ZoneId unsafeGetZoneId(String zoneString) { return ZoneId.of(zoneString); }")));
        assertTrue(source.matches(PatternMaker.make(": 40 */", "private ZonedDateTime getZonedDateTime(LocalDateTime localDateTime, ZoneId zoneId) { return ZonedDateTime.of(localDateTime, zoneId); }")));

        assertTrue(source.indexOf("// Byte code:") == -1);

        // Recompile decompiled source code and check errors
        try {
            assertTrue(CompilerUtil.compile("1.9", new JavaSourceFileObject(internalClassName, source)));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("invalid source release: 1.9")) {
                System.err.println("testJdk901InterfaceWithDefaultMethods() need a Java SDK 9+");
            } else {
                assertTrue("Compilation failed: " + e.getMessage(), false);
            }
        }
    }

    protected void printSource(String source) {
        System.out.println("- - - - - - - - ");
        System.out.println(source);
        System.out.println("- - - - - - - - ");
    }
}
