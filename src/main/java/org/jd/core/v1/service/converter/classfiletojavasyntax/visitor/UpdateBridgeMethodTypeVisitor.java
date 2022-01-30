/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.ConstantPool;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.AttributeCode;
import org.jd.core.v1.model.classfile.constant.ConstantMemberRef;
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

    public UpdateBridgeMethodTypeVisitor(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        safeAcceptListDeclaration(bodyDeclaration.getInnerTypeDeclarations());
    }

    @Override
    public void visit(MethodDeclaration declaration) {
        if (declaration.isStatic() && declaration.getReturnedType().isObjectType() && declaration.getName().startsWith("access$")) {
            TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(declaration.getReturnedType().getInternalName());

            if (typeTypes != null && typeTypes.getTypeParameters() != null) {
                ClassFileMethodDeclaration cfmd = (ClassFileMethodDeclaration) declaration;
                Method method = cfmd.getMethod();
                byte[] code = method.<AttributeCode>getAttribute("Code").getCode();
                int offset = 0;
                int opcode = code[offset] & 255;

                while (21 <= opcode && opcode <= 45 || // ILOAD, LLOAD, FLOAD, DLOAD, ..., ILOAD_0 ... ILOAD_3, ..., ALOAD_1, ..., ALOAD_3
                        89 <= opcode && opcode <= 95) { // DUP, ..., DUP2_X2, SWAP
                    opcode = code[++offset] & 255;
                }

                if (opcode >= GETSTATIC && opcode <= PUTFIELD) {
                    int index = (code[++offset] & 255) << 8 | code[++offset] & 255;
                    ConstantPool constants = method.getConstants();
                    ConstantMemberRef constantMemberRef = constants.getConstant(index);
                    String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
                    ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());
                    String descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    Type type = typeMaker.makeFieldType(typeName, name, descriptor);

                    // Update returned generic type of bridge method
                    typeMaker.setMethodReturnedType(typeName, cfmd.getName(), cfmd.getDescriptor(), type);
                }
                if (opcode >= INVOKEVIRTUAL && opcode <= INVOKEINTERFACE) {
                    int index = (code[++offset] & 255) << 8 | code[++offset] & 255;
                    ConstantPool constants = method.getConstants();
                    ConstantMemberRef constantMemberRef = constants.getConstant(index);
                    String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
                    ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());
                    String descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(typeName, name, descriptor);

                    // Update returned generic type of bridge method
                    typeMaker.setMethodReturnedType(typeName, cfmd.getName(), cfmd.getDescriptor(), methodTypes.getReturnedType());
                }
            }
        }
    }

    @Override public void visit(ConstructorDeclaration declaration) {}
    @Override public void visit(StaticInitializerDeclaration declaration) {}

    @Override
    public void visit(ClassDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override public void visit(AnnotationDeclaration declaration) {}
    @Override public void visit(EnumDeclaration declaration) {}
}
