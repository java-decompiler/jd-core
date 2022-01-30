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
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Test;

import java.io.InputStream;

public class Java9InterfaceTest extends AbstractJdTest {

    @Test
    public void testJdk901InterfaceWithDefaultMethods() throws Exception {
        String internalClassName = "org/jd/core/test/InterfaceWithDefaultMethods";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-9.0.1.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("public interface InterfaceWithDefaultMethods")));
            assertTrue(source.matches(PatternMaker.make("void setTime(int paramInt1, int paramInt2, int paramInt3);")));
            assertTrue(source.matches(PatternMaker.make("LocalDateTime getLocalDateTime();")));
            assertTrue(source.matches(PatternMaker.make("static ZoneId getZoneId(String zoneString)")));
            assertTrue(source.matches(PatternMaker.make(": 24 */", "return unsafeGetZoneId(zoneString);")));
            assertTrue(source.matches(PatternMaker.make(": 26 */", "System.err.println(\"Invalid time zone: \" + zoneString + \"; using default time zone instead.\");")));
            assertTrue(source.matches(PatternMaker.make(": 27 */", "return ZoneId.systemDefault();")));
            assertTrue(source.matches(PatternMaker.make("default ZonedDateTime getZonedDateTime(String zoneString)")));
            assertTrue(source.matches(PatternMaker.make(": 32 */", "return getZonedDateTime(getLocalDateTime(), getZoneId(zoneString));")));
            assertTrue(source.matches(PatternMaker.make("private static ZoneId unsafeGetZoneId(String zoneString)")));
            assertTrue(source.matches(PatternMaker.make(": 36 */", "return ZoneId.of(zoneString);")));
            assertTrue(source.matches(PatternMaker.make("private ZonedDateTime getZonedDateTime(LocalDateTime localDateTime, ZoneId zoneId)")));
            assertTrue(source.matches(PatternMaker.make(": 40 */", "return ZonedDateTime.of(localDateTime, zoneId);")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.9", new InMemoryJavaSourceFileObject(internalClassName, source)));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("invalid source release: 1.9")) {
                System.err.println("testJdk901InterfaceWithDefaultMethods() need a Java SDK 9+");
            } else {
                junit.framework.TestCase.fail("Compilation failed: " + e.getMessage());
            }
        }
    }
}
