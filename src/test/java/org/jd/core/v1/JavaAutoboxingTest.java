/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import junit.framework.TestCase;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.JavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.ClassFileToJavaSyntaxProcessor;
import org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.JavaSyntaxToJavaFragmentProcessor;
import org.jd.core.v1.service.layouter.LayoutFragmentProcessor;
import org.jd.core.v1.service.tokenizer.javafragmenttotoken.JavaFragmentToTokenProcessor;
import org.jd.core.v1.service.writer.WriteTokenProcessor;
import org.jd.core.v1.stub.AutoboxingAndUnboxing;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

public class JavaAutoboxingTest extends TestCase {
    protected ClassFileDeserializer deserializer = new ClassFileDeserializer();
    protected ClassFileToJavaSyntaxProcessor converter = new ClassFileToJavaSyntaxProcessor();
    protected JavaSyntaxToJavaFragmentProcessor fragmenter = new JavaSyntaxToJavaFragmentProcessor();
    protected LayoutFragmentProcessor layouter = new LayoutFragmentProcessor();
    //protected TestTokenizeJavaFragmentProcessor tokenizer = new TestTokenizeJavaFragmentProcessor();
    protected JavaFragmentToTokenProcessor tokenizer = new JavaFragmentToTokenProcessor();
    protected WriteTokenProcessor writer = new WriteTokenProcessor();

    @Test
    // https://github.com/java-decompiler/jd-core/issues/14
    public void testAutoboxing() throws Exception {
        String internalClassName = AutoboxingAndUnboxing.class.getName().replace('.', '/');
        String source = decompile(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(": 5 */", "Integer intObj = 10;")));
        assertTrue(source.matches(PatternMaker.make(": 6 */", "int i = intObj;")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new JavaSourceFileObject(internalClassName, source)));
    }

    protected String decompile(Loader loader, Printer printer, String internalTypeName) throws Exception {
        return decompile(loader, printer, internalTypeName, Collections.emptyMap());
    }

    protected String decompile(Loader loader, Printer printer, String internalTypeName, Map<String, Object> configuration) throws Exception {
        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);
        decompileContext.setMainInternalTypeName(internalTypeName);
        decompileContext.setConfiguration(configuration);

        ClassFile classFile = deserializer.loadClassFile(loader, internalTypeName);
        decompileContext.setBody(classFile);

        converter.process(decompileContext);
        fragmenter.process(decompileContext);
        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        assertTrue(source.indexOf("// Byte code:") == -1);

        return source;
    }

    protected void printSource(String source) {
        System.out.println("- - - - - - - - ");
        System.out.println(source);
        System.out.println("- - - - - - - - ");
    }
}
