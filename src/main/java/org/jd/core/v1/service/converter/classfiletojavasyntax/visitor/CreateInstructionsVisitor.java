/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.StaticInitializerDeclaration;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ByteCodeWriter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphReducer;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ExceptionUtil;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.LocalVariableMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.StatementMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import java.util.List;

import static org.apache.bcel.Const.ACC_ABSTRACT;
import static org.apache.bcel.Const.ACC_BRIDGE;
import static org.apache.bcel.Const.ACC_PUBLIC;
import static org.apache.bcel.Const.ACC_STATIC;
import static org.apache.bcel.Const.ACC_SYNTHETIC;

public class CreateInstructionsVisitor extends AbstractJavaSyntaxVisitor {
    private final TypeMaker typeMaker;
    
    public CreateInstructionsVisitor(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
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

        for (ClassFileConstructorOrMethodDeclaration method : methods) {
            if ((method.getFlags() & (ACC_SYNTHETIC|ACC_BRIDGE)) != 0) {
                method.accept(this);
            } else if ((method.getFlags() & (ACC_STATIC|ACC_BRIDGE)) == ACC_STATIC) {
                if (method.getMethod().getName().startsWith("access$")) {
                    // Accessor -> bridge method
                    method.setFlags(method.getFlags() | ACC_BRIDGE);
                    method.accept(this);
                }
            } else if (method.getParameterTypes() != null) {
                if (method.getParameterTypes().isList()) {
                    for (Type type : method.getParameterTypes()) {
                        if (type.isObjectType() && type.getName() == null) {
                            // Synthetic type in parameters -> synthetic method
                            method.setFlags(method.getFlags() | ACC_SYNTHETIC);
                            method.accept(this);
                            break;
                        }
                    }
                } else {
                    Type type = method.getParameterTypes().getFirst();
                    if (type.isObjectType() && type.getName() == null) {
                        // Synthetic type in parameters -> synthetic method
                        method.setFlags(method.getFlags() | ACC_SYNTHETIC);
                        method.accept(this);
                        break;
                    }
                }
            }
        }
        for (ClassFileConstructorOrMethodDeclaration method : methods) {
            if ((method.getFlags() & (ACC_SYNTHETIC|ACC_BRIDGE)) == 0) {
                method.accept(this);
            }
        }
    }

    @Override
    public void visit(FieldDeclaration declaration) {}

    @Override
    public void visit(ConstructorDeclaration declaration) {
        createParametersVariablesAndStatements((ClassFileConstructorOrMethodDeclaration)declaration, true);
    }

    @Override
    public void visit(MethodDeclaration declaration) {
        createParametersVariablesAndStatements((ClassFileConstructorOrMethodDeclaration)declaration, false);
    }

    @Override
    public void visit(StaticInitializerDeclaration declaration) {
        createParametersVariablesAndStatements((ClassFileConstructorOrMethodDeclaration)declaration, false);
    }

    public void createParametersVariablesAndStatements(ClassFileConstructorOrMethodDeclaration comd, boolean constructor) {
        ClassFile classFile = comd.getClassFile();
        Method method = comd.getMethod();
        Code attributeCode = method.getCode();
        LocalVariableMaker localVariableMaker = new LocalVariableMaker(typeMaker, comd, constructor);

        if (attributeCode == null) {
            localVariableMaker.make(false, typeMaker);
        } else {
            StatementMaker statementMaker = new StatementMaker(typeMaker, localVariableMaker, comd);
            boolean containsLineNumber = attributeCode.getLineNumberTable() != null;

            List<ControlFlowGraphReducer> preferredReducers = ControlFlowGraphReducer.getPreferredReducers();

            boolean reduced = false;
            for (ControlFlowGraphReducer controlFlowGraphReducer : preferredReducers) {
                try {
                    if (controlFlowGraphReducer.reduce(method)) {
                        if (comd.getStatements() instanceof Statements) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                            Statements stmts = (Statements) comd.getStatements();
                            if (stmts.isEmpty()) {
                                comd.setStatements(statementMaker.make(controlFlowGraphReducer.getControlFlowGraph(), stmts));
                            }
                        } else {
                            comd.setStatements(statementMaker.make(controlFlowGraphReducer.getControlFlowGraph(), new Statements()));
                        }
                        reduced = true;
                        break;
                    }
                } catch (Exception | StackOverflowError e) {
                    assert ExceptionUtil.printStackTrace(e);
                }
            }
            if (!reduced) {
                System.err.println("Could not reduce control flow graph in method " + method.getName() + method.getSignature() + " from class " + classFile.getInternalTypeName());
                comd.setStatements(new Statements(ByteCodeWriter.getLineNumberTableAsStatements(method)));
            }

            localVariableMaker.make(containsLineNumber, typeMaker);
        }

        comd.setFormalParameters(localVariableMaker.getFormalParameters());

        if (classFile.isInterface()) {
            comd.setFlags(comd.getFlags() & ~(ACC_PUBLIC|ACC_ABSTRACT));
        }
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
