/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.processor;

import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.message.Message;
import org.jd.core.v1.model.processor.Processor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ObjectTypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.SignatureParser;
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
    public void process(Message message) throws Exception {
        ObjectTypeMaker maker = message.getHeader("objectTypeMaker");
        SignatureParser parser = message.getHeader("signatureParser");
        CompilationUnit compilationUnit = message.getBody();

        UpdateJavaSyntaxTreeStep1Visitor updateJavaSyntaxTreeStep1Visitor = new UpdateJavaSyntaxTreeStep1Visitor(maker, parser);
        updateJavaSyntaxTreeStep1Visitor.visit(compilationUnit);

        UpdateJavaSyntaxTreeStep2Visitor updateJavaSyntaxTreeStep2Visitor = new UpdateJavaSyntaxTreeStep2Visitor();
        updateJavaSyntaxTreeStep2Visitor.visit(compilationUnit);
    }
}
