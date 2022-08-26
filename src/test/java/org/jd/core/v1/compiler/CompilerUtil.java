/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.compiler;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class CompilerUtil {
    protected static final File DESTINATION_DIRECTORY = new File("target/test-recompiled");
    protected static final String DESTINATION_DIRECTORY_PATH = DESTINATION_DIRECTORY.getAbsolutePath();

    private CompilerUtil() {
    }

    public static boolean compile(String preferredJavaVersion, InMemoryJavaSourceFileObject... javaFileObjects) throws Exception {
        InMemoryClassLoader classLoader = new InMemoryClassLoader();
        return compile(preferredJavaVersion, classLoader, javaFileObjects);
    }
    
    public static boolean compile(String preferredJavaVersion, InMemoryClassLoader classLoader, InMemoryJavaSourceFileObject... javaFileObjects) throws Exception {
        boolean compilationSuccess = false;
        String javaVersion = getJavaVersion(preferredJavaVersion);

        DESTINATION_DIRECTORY.mkdirs();

        JavaCompiler compiler = new EclipseCompiler();
        StringWriter writer = new StringWriter();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<String> options = Arrays.asList("-g", "-source", javaVersion, "-target", javaVersion, "-d", DESTINATION_DIRECTORY_PATH, "-cp", System.getProperty("java.class.path"));
        List<InMemoryJavaSourceFileObject> compilationUnits = Arrays.asList(javaFileObjects);
        try (StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, Locale.US, StandardCharsets.UTF_8)) {
            try (InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(standardFileManager, compilationUnits, classLoader)) {
                compilationSuccess = compiler.getTask(writer, fileManager, diagnostics, options, null, compilationUnits).call();
    
                if (!diagnostics.getDiagnostics().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    Set<Long> lineNumbers = new HashSet<>();
                    for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                        switch (d.getKind()) {
                            case NOTE:
                            case WARNING:
                            case OTHER:
                                break;
                            default:
                                if (d.getLineNumber() > 0) {
                                    sb.append(String.format("%-7s - line %-4d- %s%n", d.getKind(), d.getLineNumber(), d.getMessage(null)));
                                    if (!lineNumbers.contains(d.getLineNumber())) {
                                        System.out.println(javaFileObjects[0].getCharContent(true).toString().split("\n")[(int) (d.getLineNumber() - 1)]);
                                        lineNumbers.add(d.getLineNumber());
                                    }
                                } else {
                                    sb.append(String.format("%-7s -          - %s%n", d.getKind(), d.getMessage(null)));
                                }
                                break;
                        }
                    }
    
                    if (sb.length() > 0) {
                        System.err.println(compilationUnits.get(0).getName());
                        System.err.print(sb.toString());
                    }
                }
            }
        }
        return compilationSuccess;
    }

    private static String getJavaVersion(String preferredJavaVersion) {
        int numericSystemJavaVersion = parseJavaVersion(System.getProperty("java.version"));

        if (numericSystemJavaVersion <= 8) {
            return preferredJavaVersion;
        }
        int numericPreferredJavaVersion = parseJavaVersion(preferredJavaVersion);

        if (numericPreferredJavaVersion < 6) {
            return "1.6";
        }
        return preferredJavaVersion;
    }

    private static int parseJavaVersion(String javaVersion) {
        if(javaVersion.startsWith("1.")) {
            javaVersion = javaVersion.substring(2, 3);
        } else {
            int index = javaVersion.indexOf(".");

            if(index != -1) {
                javaVersion = javaVersion.substring(0, index);
            }
        }

        return Integer.parseInt(javaVersion);
    }
}
