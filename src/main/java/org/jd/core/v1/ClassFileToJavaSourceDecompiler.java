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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class ClassFileToJavaSourceDecompiler implements Decompiler {
    private final ClassFileDeserializer deserializer = new ClassFileDeserializer();
    private final ClassFileToJavaSyntaxProcessor converter = new ClassFileToJavaSyntaxProcessor();
    private final JavaSyntaxToJavaFragmentProcessor fragmenter = new JavaSyntaxToJavaFragmentProcessor();
    private final LayoutFragmentProcessor layouter = new LayoutFragmentProcessor();
    private final JavaFragmentToTokenProcessor tokenizer = new JavaFragmentToTokenProcessor();
    private final WriteTokenProcessor writer = new WriteTokenProcessor();

    @Override
    public DecompileContext decompile(Loader loader, Printer printer, String internalName) throws IOException {
        return decompile(loader, printer, internalName, Collections.emptyMap());
    }

    @Override
    public synchronized DecompileContext decompile(Loader loader, Printer printer, String internalName, Map<String, Object> configuration) throws IOException {
        DecompileContext decompileContext = new DecompileContext();

        decompileContext.setMainInternalTypeName(internalName);
        decompileContext.setConfiguration(configuration);
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);

        decompile(decompileContext);
        return decompileContext;
    }

    protected void decompile(DecompileContext decompileContext) throws IOException {
        ClassFile classFile = this.deserializer.loadClassFile(decompileContext.getLoader(),
                decompileContext.getMainInternalTypeName());
        decompileContext.setClassFile(classFile);
        decompileContext.setMainInternalTypeName(classFile.getInternalTypeName());
        CompilationUnit compilationUnit = converter.process(decompileContext);
        fragmenter.process(compilationUnit, decompileContext);
        layouter.process(decompileContext);
        DefaultList<Token> tokens = tokenizer.process(decompileContext.getBody());
        decompileContext.setTokens(tokens);
        writer.process(decompileContext);
    }
}
