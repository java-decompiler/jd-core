/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.compiler;

import javax.tools.*;
import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;

public class CompilerUtil {
    protected static final File DESTINATION_DIRECTORY = new File("build/test-recompiled");
    protected static final String DESTINATION_DIRECTORY_PATH = DESTINATION_DIRECTORY.getAbsolutePath();

    public static boolean compile(String javaVersion, JavaFileObject... JavaFileObjects) throws Exception {
        boolean compilationSuccess = false;

        DESTINATION_DIRECTORY.mkdirs();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StringWriter writer = new StringWriter();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<String> options = Arrays.asList("-source", javaVersion, "-target", javaVersion, "-d", DESTINATION_DIRECTORY_PATH);
            Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(JavaFileObjects);
            compilationSuccess = compiler.getTask(writer, fileManager, diagnostics, options, null, compilationUnits).call();

            for (Diagnostic d : diagnostics.getDiagnostics()) {
                if (d.getLineNumber() > 0) {
                    System.err.print(String.format("%-7s - line %-4d- %s%n", d.getKind(), d.getLineNumber(), d.getMessage(null)));
                } else {
                    System.err.print(String.format("%-7s -          - %s%n", d.getKind(), d.getMessage(null)));
                }
            }
        }

        return compilationSuccess;
    }
}
