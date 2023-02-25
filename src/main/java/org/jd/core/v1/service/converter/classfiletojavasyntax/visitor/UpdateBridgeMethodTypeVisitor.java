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
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.StaticInitializerDeclaration;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import static org.apache.bcel.Const.GETSTATIC;
import static org.apache.bcel.Const.INVOKEINTERFACE;
import static org.apache.bcel.Const.INVOKEVIRTUAL;
import static org.apache.bcel.Const.PUTFIELD;

public class UpdateBridgeMethodTypeVisitor extends AbstractJavaSyntaxVisitor {
    private final TypeMaker typeMaker;

    public UpdateBridgeMethodTypeVisitor(final TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

    @Override
    public void visit(final BodyDeclaration declaration) {
        final ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        safeAcceptListDeclaration(bodyDeclaration.getInnerTypeDeclarations());
    }

    @Override
    public void visit(final MethodDeclaration declaration) {
        if (declaration.isStatic() && declaration.getReturnedType().isObjectType() && declaration.getName().startsWith("access$")) {
            final TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(declaration.getReturnedType().getInternalName());

            if (typeTypes != null && typeTypes.getTypeParameters() != null) {
                final ClassFileMethodDeclaration cfmd = (ClassFileMethodDeclaration) declaration;
                final Method method = cfmd.getMethod();
                final byte[] code = method.getCode().getCode();
                int offset = 0;
                int opcode = code[offset] & 255;

                while (21 <= opcode && opcode <= 45 || // ILOAD, LLOAD, FLOAD, DLOAD, ..., ILOAD_0 ... ILOAD_3, ..., ALOAD_1, ..., ALOAD_3
                        89 <= opcode && opcode <= 95) { // DUP, ..., DUP2_X2, SWAP
                    opcode = code[++offset] & 255;
                }

                if (opcode >= GETSTATIC && opcode <= PUTFIELD) {
                    final int index = (code[++offset] & 255) << 8 | code[++offset] & 255;
                    final ConstantPool constants = method.getConstantPool();
                    final ConstantCP constantMemberRef = constants.getConstant(index);
                    final String typeName = constants.getConstantString(constantMemberRef.getClassIndex(), Const.CONSTANT_Class);
                    final ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    final String name = constants.getConstantString(constantNameAndType.getNameIndex(), Const.CONSTANT_Utf8);
                    final String descriptor = constants.getConstantString(constantNameAndType.getSignatureIndex(), Const.CONSTANT_Utf8);
                    final Type type = typeMaker.makeFieldType(typeName, name, descriptor);

                    // Update returned generic type of bridge method
                    typeMaker.setMethodReturnedType(typeName, cfmd.getName(), cfmd.getDescriptor(), type);
                }
                if (opcode >= INVOKEVIRTUAL && opcode <= INVOKEINTERFACE) {
                    final int index = (code[++offset] & 255) << 8 | code[++offset] & 255;
                    final ConstantPool constants = method.getConstantPool();
                    final ConstantCP constantMemberRef = constants.getConstant(index);
                    final String typeName = constants.getConstantString(constantMemberRef.getClassIndex(), Const.CONSTANT_Class);
                    final ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    final String name = constants.getConstantString(constantNameAndType.getNameIndex(), Const.CONSTANT_Utf8);
                    final String descriptor = constants.getConstantString(constantNameAndType.getSignatureIndex(), Const.CONSTANT_Utf8);
                    final TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(typeName, name, descriptor);

                    // Update returned generic type of bridge method
                    typeMaker.setMethodReturnedType(typeName, cfmd.getName(), cfmd.getDescriptor(), methodTypes.getReturnedType());
                }
            }
        }
    }

    @Override public void visit(final ConstructorDeclaration declaration) {}
    @Override public void visit(final StaticInitializerDeclaration declaration) {}

    @Override
    public void visit(final ClassDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(final InterfaceDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override public void visit(final AnnotationDeclaration declaration) {}
    @Override public void visit(final EnumDeclaration declaration) {}
}
