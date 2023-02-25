package org.jd.core.v1;

import org.apache.commons.io.IOUtils;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.printer.StringBuilderPrinter;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

public class ModuleInfoTest extends AbstractJdTest {

    @Test
    public void testJREModules() throws Exception {
        final Enumeration<URL> moduleURLs = getClass().getClassLoader().getResources("module-info.class");
        while (moduleURLs.hasMoreElements()) {
            final URL url = moduleURLs.nextElement();
            if (url.toString().contains("/java.desktop/")) {
                try (InputStream in = url.openStream()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    IOUtils.copy(in, out);
                    Loader loader = new Loader() {

                        @Override
                        public byte[] load(String internalName) throws IOException {
                            return out.toByteArray();
                        }

                        @Override
                        public boolean canLoad(String internalName) {
                            return true;
                        }
                    };
                    assertNotNull(decompileSuccess(loader, new StringBuilderPrinter(), "module-info"));
                }
            }
        }
    }
}
