package org.jd.core.v1;

import junit.framework.TestCase;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.service.converter.classfiletojavasyntax.ClassFileToJavaSyntaxProcessor;
import org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.JavaSyntaxToJavaFragmentProcessor;
import org.jd.core.v1.service.layouter.LayoutFragmentProcessor;
import org.jd.core.v1.service.tokenizer.javafragmenttotoken.JavaFragmentToTokenProcessor;
import org.jd.core.v1.service.writer.WriteTokenProcessor;

import java.util.Collections;
import java.util.Map;

public abstract class AbstractJdTest extends TestCase {
    protected ClassFileDeserializer deserializer = new ClassFileDeserializer();
    protected ClassFileToJavaSyntaxProcessor converter = new ClassFileToJavaSyntaxProcessor();
    protected JavaSyntaxToJavaFragmentProcessor fragmenter = new JavaSyntaxToJavaFragmentProcessor();
    protected LayoutFragmentProcessor layouter = new LayoutFragmentProcessor();
    protected JavaFragmentToTokenProcessor tokenizer = new JavaFragmentToTokenProcessor();
    protected WriteTokenProcessor writer = new WriteTokenProcessor();

    protected String decompile(Loader loader, Printer printer, String internalTypeName, Map<String, Object> configuration) throws Exception {
        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setLoader(loader);
        decompileContext.setPrinter(printer);
        decompileContext.setMainInternalTypeName(internalTypeName);
        decompileContext.setConfiguration(configuration);

        ClassFile classFile = deserializer.loadClassFile(loader, internalTypeName);
        decompileContext.setClassFile(classFile);

        converter.process(decompileContext);
        fragmenter.process(decompileContext);
        layouter.process(decompileContext);
        tokenizer.process(decompileContext);
        writer.process(decompileContext);

        String source = printer.toString();

        printSource(source);

        return source;
    }

    protected String decompile(Loader loader, Printer printer, String internalTypeName) throws Exception {
        return decompile(loader, printer, internalTypeName, Collections.emptyMap());
    }

    protected String decompileSuccess(Loader loader, Printer printer, String internalTypeName, Map<String, Object> configuration) throws Exception {
        String source = decompile(loader, printer, internalTypeName, configuration);
        assertTrue(source.indexOf("// Byte code:") == -1);
        return source;
    }

    protected String decompileSuccess(Loader loader, Printer printer, String internalTypeName) throws Exception {
        return decompileSuccess(loader, printer, internalTypeName, Collections.emptyMap());
    }

    protected void printSource(String source) {
        System.out.println("- - - - - - - - ");
        System.out.println(source);
        System.out.println("- - - - - - - - ");
    }
}
