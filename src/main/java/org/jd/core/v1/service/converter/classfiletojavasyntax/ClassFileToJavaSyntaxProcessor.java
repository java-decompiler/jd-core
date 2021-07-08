/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.ConvertClassFileProcessor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.UpdateJavaSyntaxTreeProcessor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

/**
 * Convert ClassFile model to Java syntax model.<br><br>
 *
 * Input:  {@link org.jd.core.v1.model.classfile.ClassFile}<br>
 * Output: {@link org.jd.core.v1.model.javasyntax.CompilationUnit}<br>
 *
 * @see ConvertClassFileProcessor
 */
public class ClassFileToJavaSyntaxProcessor {
    protected static final ConvertClassFileProcessor CONVERT_CLASS_FILE_PROCESSOR = new ConvertClassFileProcessor();
    protected static final UpdateJavaSyntaxTreeProcessor UPDATE_JAVA_SYNTAX_TREE_PROCESSOR = new UpdateJavaSyntaxTreeProcessor();

    public CompilationUnit process(DecompileContext decompileContext) {
        Loader loader = decompileContext.getLoader();

        TypeMaker typeMaker = decompileContext.getTypeMaker();
        if (typeMaker == null) {
            typeMaker = new TypeMaker(loader);
            decompileContext.setTypeMaker(typeMaker);
        }

        CompilationUnit compilationUnit = CONVERT_CLASS_FILE_PROCESSOR.process(decompileContext.getClassFile(), typeMaker, decompileContext);
        decompileContext.setCompilationUnit(compilationUnit);

        UPDATE_JAVA_SYNTAX_TREE_PROCESSOR.process(compilationUnit, typeMaker);
        return compilationUnit;
    }
}
