/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.javasyntax.statement.ByteCodeStatement;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.*;

import java.util.List;

import static org.jd.core.v1.model.javasyntax.declaration.Declaration.*;


public class CreateInstructionsVisitor extends AbstractJavaSyntaxVisitor {
    protected ObjectTypeMaker objectTypeMaker;
    protected SignatureParser signatureParser;

    public CreateInstructionsVisitor(ObjectTypeMaker objectTypeMaker, SignatureParser signatureParser) {
        this.objectTypeMaker = objectTypeMaker;
        this.signatureParser = signatureParser;
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        // Parse byte code
        List<ClassFileConstructorOrMethodDeclaration> methods = bodyDeclaration.getMethodDeclarations();

        if (methods != null) {
            for (ClassFileConstructorOrMethodDeclaration method : methods) {
                if ((method.getFlags() & (FLAG_SYNTHETIC|FLAG_BRIDGE)) != 0) {
                    method.accept(this);
                } else if ((method.getFlags() & (FLAG_STATIC|FLAG_BRIDGE)) == FLAG_STATIC) {
                    if (method.getMethod().getName().startsWith("access$")) {
                        // Accessor -> bridge method
                        method.setFlags(method.getFlags() | FLAG_BRIDGE);
                        method.accept(this);
                    }
                } else if (method.getParameterTypes() != null) {
                    for (Type type : method.getParameterTypes()) {
                        if (type.isObject() && ((ObjectType)type).getName() == null) {
                            // Synthetic type in parameters -> synthetic method
                            method.setFlags(method.getFlags() | FLAG_SYNTHETIC);
                            method.accept(this);
                            break;
                        }
                    }
                }
            }

            for (ClassFileConstructorOrMethodDeclaration method : methods) {
                if ((method.getFlags() & (FLAG_SYNTHETIC|FLAG_BRIDGE)) == 0) {
                    method.accept(this);
                }
            }
        }
    }

    @Override
    public void visit(FieldDeclaration declaration) {}

    @Override
    public void visit(ConstructorDeclaration declaration) {
        ClassFileConstructorOrMethodDeclaration comdwln = (ClassFileConstructorOrMethodDeclaration)declaration;
        LocalVariableMaker localVariableMaker = new LocalVariableMaker(objectTypeMaker, signatureParser, comdwln, true, comdwln.getParameterTypes());

        createParametersVariablesAndStatements(comdwln, localVariableMaker);
    }

    @Override
    public void visit(MethodDeclaration declaration) {
        ClassFileConstructorOrMethodDeclaration comdwln = (ClassFileConstructorOrMethodDeclaration)declaration;
        LocalVariableMaker localVariableMaker = new LocalVariableMaker(objectTypeMaker, signatureParser, comdwln, false, comdwln.getParameterTypes());

        createParametersVariablesAndStatements(comdwln, localVariableMaker);
    }

    @Override
    public void visit(StaticInitializerDeclaration declaration) {
        ClassFileConstructorOrMethodDeclaration comdwln = (ClassFileConstructorOrMethodDeclaration)declaration;
        LocalVariableMaker localVariableMaker = new LocalVariableMaker(objectTypeMaker, signatureParser, comdwln, false, null);

        createParametersVariablesAndStatements(comdwln, localVariableMaker);
    }

    protected void createParametersVariablesAndStatements(ClassFileConstructorOrMethodDeclaration comdwln, LocalVariableMaker localVariableMaker) {
        ClassFile classFile = comdwln.getClassFile();
        ClassFileBodyDeclaration bodyDeclaration = comdwln.getBodyDeclaration();
        Method method = comdwln.getMethod();
        Type returnedType = comdwln.getReturnedType();
        StatementMaker statementMaker = new StatementMaker(objectTypeMaker, signatureParser, localVariableMaker, classFile, bodyDeclaration, returnedType);

        try {
            ControlFlowGraph cfg = ControlFlowGraphMaker.make(method);

            if (cfg != null) {
                ControlFlowGraphGotoReducer.reduce(cfg);
                ControlFlowGraphLoopReducer.reduce(cfg);

                if (ControlFlowGraphReducer.reduce(cfg)) {
                    comdwln.setStatements(statementMaker.make(cfg));
                } else {
                    comdwln.setStatements(new ByteCodeStatement(ByteCodeWriter.write("// ", method)));
                }
            }
        } catch (Exception e) {
            assert ExceptionUtil.printStackTrace(e);
            comdwln.setStatements(new ByteCodeStatement(ByteCodeWriter.write("// ", method)));
        }

        if ((classFile.getAccessFlags() & FLAG_INTERFACE) != 0) {
            comdwln.setFlags(comdwln.getFlags() & ~(FLAG_PUBLIC|FLAG_ABSTRACT));
        }

        localVariableMaker.make();
        comdwln.setFormalParameters(localVariableMaker.getFormalParameters());
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }
}
