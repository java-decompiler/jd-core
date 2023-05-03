package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.classfile.Method;
import org.apache.commons.io.IOUtils;
import org.jd.core.v1.cfg.MethodUtil;
import org.jd.core.v1.loader.ClassPathLoader;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class ByteCodeWriterTest {

    @Test
    public void testWrite() throws Exception {
        final ByteCodeWriter byteCodeWriter = new ByteCodeWriter();
        final ClassPathLoader classPathLoader = new ClassPathLoader();
        final TypeMaker typeMaker = new TypeMaker(classPathLoader);
        final String internalTypeName = "jd/core/process/analyzer/classfile/reconstructor/PreIncReconstructor";
        final Method method = MethodUtil.searchMethod(classPathLoader, typeMaker, internalTypeName, "Reconstruct", "(Ljava/util/List;)V");
        final String byteCode = byteCodeWriter.write("//", method);
        assertEquals(IOUtils.toString(getClass().getResource("/txt/PreIncReconstructor.txt"), StandardCharsets.UTF_8), byteCode);
    }
}
