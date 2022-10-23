/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.bcel.Const.MAJOR_1_1;
import static org.apache.bcel.Const.MAJOR_1_5;
import static org.apache.bcel.Const.MAJOR_1_8;

import jd.core.ClassUtil;

public class JarFileToJavaSourceTest extends AbstractJdTest {

    private static final Pattern MODULE_INFO_CLASS = Pattern.compile("META-INF/versions/(\\d+)/module-info\\.class");

    @Test
    public void testBCEL() throws Exception {
        test(org.apache.bcel.Const.class);
    }

    @Test
    public void testCommonsCodec() throws Exception {
        test(org.apache.commons.codec.Charsets.class);
    }
    
    @Test
    public void testCommonsCollections4() throws Exception {
        test(org.apache.commons.collections4.CollectionUtils.class);
    }

    @Test
    public void testCommonsImaging() throws Exception {
        test(org.apache.commons.imaging.Imaging.class);
    }

    @Test
    public void testCommonsLang3() throws Exception {
        test(org.apache.commons.lang3.JavaVersion.class);
    }

//    @Test
//    public void testCommonsMath3() throws Exception {
//        test(org.apache.commons.math3.Field.class);
//    }

    @Test
    public void testDiskLruCache() throws Exception {
        test(com.jakewharton.disklrucache.DiskLruCache.class);
    }

    @Test
    public void testJavaPoet() throws Exception {
        test(com.squareup.javapoet.JavaFile.class);
    }

    @Test
    public void testJavaWriter() throws Exception {
        test(com.squareup.javawriter.JavaWriter.class);
    }

//    TODO: in progress
//    @Test
//    public void testJodaTime() throws Exception {
//        test(org.joda.time.DateTime.class);
//    }

    @Test
    public void testJSoup() throws Exception {
        test(org.jsoup.Jsoup.class);
    }

    @Test
    public void testJUnit4() throws Exception {
        test(org.junit.Test.class);
    }

    @Test
    public void testMimecraft() throws Exception {
        test(com.squareup.mimecraft.Part.class);
    }

    @Test
    public void testScribe() throws Exception {
        test(org.scribe.oauth.OAuthService.class);
    }

    @Test
    public void testSparkCore() throws Exception {
        test(spark.Spark.class);
    }

    @Test
    public void testLog4jApi() throws Exception {
        test(org.apache.logging.log4j.Logger.class);
    }

    @Test
    public void testLog4jCore() throws Exception {
        test(org.apache.logging.log4j.core.Logger.class);
    }
    
//    @Test
//    public void testGuava() throws Exception {
//        test(com.google.common.collect.Collections2.class);
//    }

    protected void test(Class<?> clazz) throws Exception {
        File file = Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
        System.out.println("====== Decompiling and recompiling " + file.getName() + " ======");
        try (FileInputStream inputStream = new FileInputStream(file)) {
            test(inputStream);
        }
    }
    
    protected void test(InputStream inputStream) throws Exception {
        long fileCounter = 0;
        long exceptionCounter = 0;
        long assertFailedCounter = 0;
        long recompilationFailedCounter = 0;

        try (InputStream is = inputStream) {
            ZipLoader loader = new ZipLoader(is);
            CounterPrinter printer = new CounterPrinter();
            Map<String, Integer> statistics = new HashMap<>();
            Map<String, Object> configuration = new HashMap<>();

            configuration.put("realignLineNumbers", Boolean.TRUE);

            long time0 = System.currentTimeMillis();

            for (String path : loader.getMap().keySet()) {
                if (path.endsWith(StringConstants.CLASS_FILE_SUFFIX) && (path.indexOf('$') == -1)) {
                    String internalTypeName = ClassUtil.getInternalName(path);

                    // TODO DEBUG if (!internalTypeName.endsWith("/Debug")) continue;
                    //if (!internalTypeName.endsWith("/MapUtils")) continue;

                    printer.init();

                    fileCounter++;

                    DecompileContext ctx = null;
                    try {
                        // Decompile class
                        ClassFileToJavaSourceDecompiler classFileToJavaSourceDecompiler = new ClassFileToJavaSourceDecompiler();
                        ctx = classFileToJavaSourceDecompiler.decompile(loader, printer, internalTypeName, configuration);
                    } catch (AssertionError e) {
                        String msg = (e.getMessage() == null) ? "<?>" : e.getMessage();
                        statistics.merge(msg, 1, Integer::sum);
                        assertFailedCounter++;
                    } catch (Throwable t) {
                        t.printStackTrace();
                        String msg = t.getMessage() == null ? t.getClass().toString() : t.getMessage();
                        statistics.merge(msg, 1, Integer::sum);
                        exceptionCounter++;
                    }

                    String source = printer.toString();
                    StringBuilder jdkVersion = new StringBuilder();
                    Matcher m = MODULE_INFO_CLASS.matcher(path);
                    if (m.matches()) {
                        continue;
                    }
                    int majorVersion = ctx == null ? MAJOR_1_8 : ctx.getMajorVersion();
                    if (majorVersion >= MAJOR_1_1) {
                        if (majorVersion >= MAJOR_1_5) {
                            jdkVersion.append(majorVersion - (MAJOR_1_5 - 5));
                        } else {
                            jdkVersion.append(majorVersion - (MAJOR_1_1 - 1));
                        }
                    }
                    
                    // Recompile source
                    if (!CompilerUtil.compile(jdkVersion.toString(), new InMemoryJavaSourceFileObject(internalTypeName, source))) {
                        recompilationFailedCounter++;
                    }
                }
            }

            long time9 = System.currentTimeMillis();

            System.out.println("Time: " + (time9-time0) + " ms");

            System.out.println("Counters:");
            System.out.println("  fileCounter             =" + fileCounter);
            System.out.println("  class+innerClassCounter =" + printer.classCounter);
            System.out.println("  methodCounter           =" + printer.methodCounter);
            System.out.println("  exceptionCounter        =" + exceptionCounter);
            System.out.println("  assertFailedCounter     =" + assertFailedCounter);
            System.out.println("  errorInMethodCounter    =" + printer.errorInMethodCounter);
            System.out.println("  accessCounter           =" + printer.accessCounter);
            System.out.println("  recompilationFailed     =" + recompilationFailedCounter);
            System.out.println("Percentages:");
            System.out.println("  % exception             =" + (exceptionCounter * 100F / fileCounter));
            System.out.println("  % assert failed         =" + (assertFailedCounter * 100F / fileCounter));
            System.out.println("  % error in method       =" + (printer.errorInMethodCounter * 100F / printer.methodCounter));
            System.out.println("  % recompilation failed  =" + (recompilationFailedCounter * 100F / fileCounter));

            System.out.println("Errors:");
            DefaultList<String> stats = new DefaultList<>();
            for (Map.Entry<String, Integer> stat : statistics.entrySet()) {
                stats.add("  " + stat.getValue() + " \t: " + stat.getKey());
            }
            stats.sort(Comparator.comparing(this::getCount).reversed());
            for (String stat : stats) {
                System.out.println(stat);
            }

            assertEquals(0, exceptionCounter);
            assertEquals(0, assertFailedCounter);
            assertEquals(0, printer.errorInMethodCounter);
            assertEquals(0, recompilationFailedCounter);
        }
    }

    private int getCount(String stat) {
        return Integer.parseInt(stat.substring(0, 5).trim());
    }

    protected static class CounterPrinter extends PlainTextPrinter {
        public long classCounter = 0;
        public long methodCounter = 0;
        public long errorInMethodCounter = 0;
        public long accessCounter = 0;

        @Override
        public void printText(String text) {
            if (text != null) {
                if ("// Byte code:".equals(text) || text.startsWith("/* monitor enter ") || text.startsWith("/* monitor exit ")) {
                    errorInMethodCounter++;
                }
            }
            super.printText(text);
        }

        @Override
        public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
            if (type == TYPE) classCounter++;
            if ((type == METHOD) || (type == CONSTRUCTOR)) methodCounter++;
            super.printDeclaration(type, internalTypeName, name, descriptor);
        }

        @Override
        public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) {
            if ((name != null) && name.startsWith("access$")) {
                accessCounter++;
            }
            super.printReference(type, internalTypeName, name, descriptor, ownerInternalName);
        }
    }
}
