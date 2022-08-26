package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;

import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;

public abstract class AbstractJdTest extends TestCase {
    protected ClassFileToJavaSourceDecompiler classFileToJavaSourceDecompiler = new ClassFileToJavaSourceDecompiler();

    protected String decompile(Loader loader, Printer printer, String internalTypeName, Map<String, Object> configuration) throws Exception {
        classFileToJavaSourceDecompiler.decompile(loader, printer, internalTypeName, configuration);

        String source = printer.toString();

        printSource(source);

        return source;
    }

    protected String decompile(Loader loader, Printer printer, String internalTypeName) throws Exception {
        return decompile(loader, printer, internalTypeName, Collections.emptyMap());
    }

    protected String decompileSuccess(Loader loader, Printer printer, String internalTypeName, Map<String, Object> configuration) throws Exception {
        String source = decompile(loader, printer, internalTypeName, configuration);
        assertEquals(-1, source.indexOf("// Byte code:"));
        assertEquals(-1, source.indexOf("Decompilation failed at line #"));
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
