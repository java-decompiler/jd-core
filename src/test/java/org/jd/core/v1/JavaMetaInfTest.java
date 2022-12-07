/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.ClassFilePrinter;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;

import static jd.core.preferences.Preferences.WRITE_METADATA;
import static jd.core.preferences.Preferences.REALIGN_LINE_NUMBERS;;

public class JavaMetaInfTest extends AbstractJdTest {

    @Test
    public void testJdk170Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, String> configuration = Map.of(WRITE_METADATA, "true", REALIGN_LINE_NUMBERS, "true");
            String source = new ClassFilePrinter().buildDecompiledOutput(configuration, loader, internalClassName + ".class", classFileToJavaSourceDecompiler);

            // Check decompiled source code
            assertTrue(source.contains("Java compiler version: 7 (51.0)"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170NoDebugInfoBasic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0-no-debug-info.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, String> configuration = Map.of(WRITE_METADATA, "true");
            String source = new ClassFilePrinter().buildDecompiledOutput(configuration, loader, internalClassName + ".class", classFileToJavaSourceDecompiler);

            // Check decompiled source code
            assertTrue(source.contains("Java compiler version: 7 (51.0)"));


            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170Constructors() throws Exception {
        String internalClassName = "org/jd/core/test/Constructors";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, String> configuration = Map.of(WRITE_METADATA, "true", REALIGN_LINE_NUMBERS, "true");
            String source = new ClassFilePrinter().buildDecompiledOutput(configuration, loader, internalClassName + ".class", classFileToJavaSourceDecompiler);

            // Check decompiled source code
            assertTrue(source.contains("Java compiler version: 7 (51.0)"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170Interface() throws Exception {
        String internalClassName = "org/jd/core/test/Interface";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, String> configuration = Map.of(WRITE_METADATA, "true", REALIGN_LINE_NUMBERS, "true");
            String source = new ClassFilePrinter().buildDecompiledOutput(configuration, loader, internalClassName + ".class", classFileToJavaSourceDecompiler);

            // Check decompiled source code
            assertTrue(source.contains("Java compiler version: 7 (51.0)"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk118Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.1.8.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, String> configuration = Map.of(WRITE_METADATA, "true", REALIGN_LINE_NUMBERS, "true");
            String source = new ClassFilePrinter().buildDecompiledOutput(configuration, loader, internalClassName + ".class", classFileToJavaSourceDecompiler);

            // Check decompiled source code
            assertTrue(source.contains("Java compiler version: 1.1 (45.3)"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.3", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
    
    @Test
    public void testJdk131Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.3.1.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, String> configuration = Map.of(WRITE_METADATA, "true", REALIGN_LINE_NUMBERS, "true");
            String source = new ClassFilePrinter().buildDecompiledOutput(configuration, loader, internalClassName + ".class", classFileToJavaSourceDecompiler);
            
            // Check decompiled source code
            assertTrue(source.contains("Java compiler version: 1.1 (45.3)"));
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.4", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk142Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.4.2.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, String> configuration = Map.of(WRITE_METADATA, "true", REALIGN_LINE_NUMBERS, "true");
            String source = new ClassFilePrinter().buildDecompiledOutput(configuration, loader, internalClassName + ".class", classFileToJavaSourceDecompiler);

            // Check decompiled source code
            assertTrue(source.contains("Java compiler version: 1.2 (46.0)"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.4", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk901Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-9.0.1.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, String> configuration = Map.of(WRITE_METADATA, "true", REALIGN_LINE_NUMBERS, "true");
            String source = new ClassFilePrinter().buildDecompiledOutput(configuration, loader, internalClassName + ".class", classFileToJavaSourceDecompiler);

            // Check decompiled source code
            assertTrue(source.contains("Java compiler version: 9 (53.0)"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk1002Basic() throws Exception {
        String internalClassName = "org/jd/core/test/Basic";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-10.0.2.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, String> configuration = Map.of(WRITE_METADATA, "true", REALIGN_LINE_NUMBERS, "true");
            String source = new ClassFilePrinter().buildDecompiledOutput(configuration, loader, internalClassName + ".class", classFileToJavaSourceDecompiler);

            // Check decompiled source code
            assertTrue(source.contains("Java compiler version: 10 (54.0)"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
