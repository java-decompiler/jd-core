/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.processor;

import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.model.processor.Processor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.UpdateJavaSyntaxTreeStep0Visitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.UpdateJavaSyntaxTreeStep1Visitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.UpdateJavaSyntaxTreeStep2Visitor;

/**
 * Create statements, init fields, merge declarations.<br><br>
 *
 * Input:  {@link CompilationUnit}<br>
 * Output: {@link CompilationUnit}<br>
 */
public class UpdateJavaSyntaxTreeProcessor implements Processor {

    @Override
    public void process(DecompileContext decompileContext) throws Exception {
        TypeMaker typeMaker = decompileContext.getTypeMaker();
        CompilationUnit compilationUnit = decompileContext.getBody();

        new UpdateJavaSyntaxTreeStep0Visitor(typeMaker).visit(compilationUnit);
        new UpdateJavaSyntaxTreeStep1Visitor(typeMaker).visit(compilationUnit);
        new UpdateJavaSyntaxTreeStep2Visitor(typeMaker).visit(compilationUnit);
    }
}
