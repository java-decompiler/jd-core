/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.Decompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.model.token.Token;
import org.jd.core.v1.service.converter.classfiletojavasyntax.ClassFileToJavaSyntaxProcessor;
import org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.JavaSyntaxToJavaFragmentProcessor;
import org.jd.core.v1.service.layouter.LayoutFragmentProcessor;
import org.jd.core.v1.service.tokenizer.javafragmenttotoken.JavaFragmentToTokenProcessor;
import org.jd.core.v1.service.writer.WriteTokenProcessor;
import org.jd.core.v1.util.DefaultList;

import java.util.Map;

public class ClassFileToJavaSourceDecompiler implements Decompiler {
    protected ClassFileDeserializer deserializer = new ClassFileDeserializer();
    protected ClassFileToJavaSyntaxProcessor converter = new ClassFileToJavaSyntaxProcessor();
    protected JavaSyntaxToJavaFragmentProcessor fragmenter = new JavaSyntaxToJavaFragmentProcessor();
    protected LayoutFragmentProcessor layouter = new LayoutFragmentProcessor();
    protected JavaFragmentToTokenProcessor tokenizer = new JavaFragmentToTokenProcessor();
    protected WriteTokenProcessor writer = new WriteTokenProcessor();

    public void decompile(Loader loader, Printer printer, String internalName) throws Exception {
        DecompileContext decompileContext = new DecompileContext();

        decompileContext.setMainInternalTypeName(internalName);
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);

        decompile(decompileContext);
    }

    public void decompile(Loader loader, Printer printer, String internalName, Map<String, Object> configuration) throws Exception {
        DecompileContext decompileContext = new DecompileContext();

        decompileContext.setMainInternalTypeName(internalName);
        decompileContext.setConfiguration(configuration);
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);

        decompile(decompileContext);
    }

    protected void decompile(DecompileContext decompileContext) throws Exception {
        ClassFile classFile = this.deserializer.loadClassFile(decompileContext.getLoader(),
                decompileContext.getMainInternalTypeName());
        decompileContext.setClassFile(classFile);

        CompilationUnit compilationUnit = this.converter.process(decompileContext);
        this.fragmenter.process(compilationUnit, decompileContext);
        this.layouter.process(decompileContext);
        DefaultList<Token> tokens = this.tokenizer.process(decompileContext.getBody());
        decompileContext.setTokens(tokens);
        this.writer.process(decompileContext);
    }
}
