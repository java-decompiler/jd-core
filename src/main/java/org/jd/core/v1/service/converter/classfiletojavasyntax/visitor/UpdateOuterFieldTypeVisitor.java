/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.StaticInitializerDeclaration;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.TypeArguments;
import org.jd.core.v1.model.javasyntax.type.TypeParameter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import static org.apache.bcel.Const.ALOAD_0;
import static org.apache.bcel.Const.ALOAD_1;
import static org.apache.bcel.Const.PUTFIELD;

public class UpdateOuterFieldTypeVisitor extends AbstractJavaSyntaxVisitor {
    private final TypeMaker typeMaker;
    private final SearchFieldVisitor searchFieldVisitor = new SearchFieldVisitor();

    public UpdateOuterFieldTypeVisitor(final TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

    @Override
    public void visit(final BodyDeclaration declaration) {
        final ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        if (!bodyDeclaration.getClassFile().isStatic()) {
            safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        }

        safeAcceptListDeclaration(bodyDeclaration.getInnerTypeDeclarations());
    }

    @Override
    public void visit(final ConstructorDeclaration declaration) {
        if (!declaration.isStatic()) {
            final ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration) declaration;

            if (cfcd.getClassFile().getOuterClassFile() != null && !declaration.isStatic()) {
                final Method method = cfcd.getMethod();
                final byte[] code = method.getCode().getCode();
                int offset = 0;
                int opcode = code[offset] & 255;

                if (opcode != ALOAD_0) {
                    return;
                }

                opcode = code[++offset] & 255;

                if (opcode != ALOAD_1) {
                    return;
                }

                opcode = code[++offset] & 255;

                if (opcode != PUTFIELD) {
                    return;
                }

                final int index = (code[++offset] & 255) << 8 | code[++offset] & 255;
                final ConstantPool constants = method.getConstantPool();
                final ConstantCP constantMemberRef = constants.getConstant(index);
                final String typeName = constants.getConstantString(constantMemberRef.getClassIndex(), Const.CONSTANT_Class);
                final ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                final String descriptor = constants.getConstantString(constantNameAndType.getSignatureIndex(), Const.CONSTANT_Utf8);
                final TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(descriptor.substring(1, descriptor.length() - 1));

                if (typeTypes != null && typeTypes.getTypeParameters() != null) {
                    final String name = constants.getConstantString(constantNameAndType.getNameIndex(), Const.CONSTANT_Utf8);
                    searchFieldVisitor.init(name);

                    for (final ClassFileFieldDeclaration field : cfcd.getBodyDeclaration().getFieldDeclarations()) {
                        field.getFieldDeclarators().accept(searchFieldVisitor);
                        if (searchFieldVisitor.found()) {
                            BaseTypeArgument typeArguments;

                            if (typeTypes.getTypeParameters().isList()) {
                                final TypeArguments tas = new TypeArguments(typeTypes.getTypeParameters().size());
                                for (final TypeParameter typeParameter : typeTypes.getTypeParameters()) {
                                    tas.add(new GenericType(typeParameter.getIdentifier()));
                                }
                                typeArguments = tas;
                            } else {
                                typeArguments = new GenericType(typeTypes.getTypeParameters().getFirst().getIdentifier());
                            }

                            // Update generic type of outer field reference
                            typeMaker.setFieldType(typeName, name, typeTypes.getThisType().createType(typeArguments));
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visit(final MethodDeclaration declaration) {}
    @Override
    public void visit(final StaticInitializerDeclaration declaration) {}

    @Override
    public void visit(final ClassDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(final InterfaceDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(final AnnotationDeclaration declaration) {}
    @Override
    public void visit(final EnumDeclaration declaration) {}

    protected static class SearchFieldVisitor extends AbstractJavaSyntaxVisitor {
        private String name;
        private boolean found;

        public void init(final String name) {
            this.name = name;
            this.found = false;
        }

        public boolean found() {
            return found;
        }

        @Override
        public void visit(final FieldDeclarator declaration) {
            found |= declaration.getName().equals(name);
        }
    }
}
