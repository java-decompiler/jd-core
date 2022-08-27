/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodType;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.ConstantPool;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.AttributeBootstrapMethods;
import org.jd.core.v1.model.classfile.attribute.AttributeCode;
import org.jd.core.v1.model.classfile.constant.ConstantMemberRef;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.AbstractNopDeclarationVisitor;
import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameters;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.expression.ArrayExpression;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LengthExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NewArray;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.QualifiedSuperExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.expression.TypeReferenceDotClassExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.LambdaExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeParameterWithTypeBounds;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileClassDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFormalParameter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileTypeDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileCmpExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileMonitorEnterStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileMonitorExitStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker.TypeTypes;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.CreateInstructionsVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.EraseTypeArgumentVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.RenameLocalVariablesVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchFirstLineNumberVisitor;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.DefaultStack;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.bcel.Const.*;
import static org.jd.core.v1.model.javasyntax.statement.ReturnStatement.RETURN;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_CLASS;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_OBJECT;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_UNDEFINED_OBJECT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_BOOLEAN;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_DOUBLE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_FLOAT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_LONG;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_BOOLEAN_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_BYTE_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.MAYBE_NEGATIVE_BYTE_TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BOOLEAN;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BYTE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_CHAR;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_DOUBLE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_FLOAT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_INT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_LONG;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_SHORT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_VOID;

public class ByteCodeParser {
    private static final JsrReturnAddressExpression JSR_RETURN_ADDRESS_EXPRESSION = new JsrReturnAddressExpression();

    private final MemberVisitor memberVisitor = new MemberVisitor();
    private final SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
    private final EraseTypeArgumentVisitor eraseTypeArgumentVisitor = new EraseTypeArgumentVisitor();
    private final LambdaParameterNamesVisitor lambdaParameterNamesVisitor = new LambdaParameterNamesVisitor();
    private final RenameLocalVariablesVisitor renameLocalVariablesVisitor = new RenameLocalVariablesVisitor();

    private final TypeMaker typeMaker;
    private final LocalVariableMaker localVariableMaker;
    private final boolean genericTypesSupported;
    private final String internalTypeName;
    private final AbstractTypeParametersToTypeArgumentsBinder typeParametersToTypeArgumentsBinder;
    private final AttributeBootstrapMethods attributeBootstrapMethods;
    private final ClassFileBodyDeclaration bodyDeclaration;
    private final Map<String, BaseType> typeBounds;
    private Type returnedType;

    public ByteCodeParser(
            TypeMaker typeMaker, LocalVariableMaker localVariableMaker, ClassFile classFile,
            ClassFileBodyDeclaration bodyDeclaration, ClassFileConstructorOrMethodDeclaration comd) {
        this.typeMaker = typeMaker;
        this.localVariableMaker = localVariableMaker;
        this.genericTypesSupported = classFile.getMajorVersion() >= MAJOR_1_5;
        this.internalTypeName = classFile.getInternalTypeName();
        this.attributeBootstrapMethods = classFile.getAttribute("BootstrapMethods");
        this.bodyDeclaration = bodyDeclaration;
        this.returnedType = comd.getReturnedType();
        this.typeBounds = comd.getTypeBounds();

        if (this.genericTypesSupported) {
            this.typeParametersToTypeArgumentsBinder = new Java5TypeParametersToTypeArgumentsBinder(typeMaker, this.internalTypeName, comd);
        } else {
            this.typeParametersToTypeArgumentsBinder = new JavaTypeParametersToTypeArgumentsBinder();
        }
    }

    public void parse(BasicBlock basicBlock, Statements statements, DefaultStack<Expression> stack, Deque<Expression> enclosingInstances) {
        ControlFlowGraph cfg = basicBlock.getControlFlowGraph();
        int fromOffset = basicBlock.getFromOffset();
        int toOffset = basicBlock.getToOffset();

        Method method = cfg.getMethod();
        ConstantPool constants = method.getConstants();
        byte[] code = method.<AttributeCode>getAttribute("Code").getCode();
        boolean syntheticFlag = (method.getAccessFlags() & ACC_SYNTHETIC) != 0;

        Expression indexRef;
        Expression arrayRef;
        Expression valueRef;
        Expression expression1;
        Expression expression2;
        Expression expression3;
        Type type1;
        Type type2;
        Type type3;
        ConstantMemberRef constantMemberRef;
        ConstantNameAndType constantNameAndType;
        String typeName;
        String name;
        String descriptor;
        ObjectType ot;
        int i;
        int count;
        int value;
        AbstractLocalVariable localVariable;

        int opcode;
        int lineNumber;
        for (int offset=fromOffset; offset<toOffset; offset++) {
            opcode = code[offset] & 255;
            lineNumber = syntheticFlag ? Expression.UNKNOWN_LINE_NUMBER : cfg.getLineNumber(offset);

            switch (opcode) {
                case NOP:
                    break;
                case ACONST_NULL:
                    stack.push(new NullExpression(lineNumber, TYPE_UNDEFINED_OBJECT));
                    break;
                case ICONST_M1:
                    stack.push(new IntegerConstantExpression(lineNumber, MAYBE_NEGATIVE_BYTE_TYPE, -1));
                    break;
                case ICONST_0, ICONST_1:
                    stack.push(new IntegerConstantExpression(lineNumber, MAYBE_BOOLEAN_TYPE, opcode - 3));
                    break;
                case ICONST_2, ICONST_3, ICONST_4, ICONST_5:
                    stack.push(new IntegerConstantExpression(lineNumber, MAYBE_BYTE_TYPE, opcode - 3));
                    break;
                case LCONST_0, LCONST_1:
                    stack.push(new LongConstantExpression(lineNumber, opcode - 9L));
                    break;
                case FCONST_0, FCONST_1, FCONST_2:
                    stack.push(new FloatConstantExpression(lineNumber, opcode - 11F));
                    break;
                case DCONST_0, DCONST_1:
                    stack.push(new DoubleConstantExpression(lineNumber, opcode - 14D));
                    break;
                case BIPUSH:
                    value = (byte)(code[++offset] & 255);
                    stack.push(new IntegerConstantExpression(lineNumber, PrimitiveTypeUtil.getPrimitiveTypeFromValue(value), value));
                    break;
                case SIPUSH:
                    value = (short)((code[++offset] & 255) << 8 | code[++offset] & 255);
                    stack.push(new IntegerConstantExpression(lineNumber, PrimitiveTypeUtil.getPrimitiveTypeFromValue(value), value));
                    break;
                case LDC:
                    parseLDC(stack, constants, lineNumber, constants.getConstant(code[++offset] & 255));
                    break;
                case LDC_W, LDC2_W:
                    parseLDC(stack, constants, lineNumber, constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255));
                    break;
                case ILOAD:
                    localVariable = localVariableMaker.getLocalVariable(code[++offset] & 255, offset);
                    parseILOAD(statements, stack, lineNumber, offset, localVariable);
                    break;
                case LLOAD, FLOAD, DLOAD:
                    localVariable = localVariableMaker.getLocalVariable(code[++offset] & 255, offset);
                    stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable));
                    break;
                case ALOAD:
                    i = code[++offset] & 255;
                    localVariable = localVariableMaker.getLocalVariable(i, offset);
                    if (i == 0 && (method.getAccessFlags() & ACC_STATIC) == 0) {
                        stack.push(new ThisExpression(lineNumber, localVariable.getType()));
                    } else {
                        stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable));
                    }
                    break;
                case ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3:
                    localVariable = localVariableMaker.getLocalVariable(opcode - 26, offset);
                    parseILOAD(statements, stack, lineNumber, offset, localVariable);
                    break;
                case LLOAD_0, LLOAD_1, LLOAD_2, LLOAD_3:
                    localVariable = localVariableMaker.getLocalVariable(opcode - 30, offset);
                    stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable));
                    break;
                case FLOAD_0, FLOAD_1, FLOAD_2, FLOAD_3:
                    localVariable = localVariableMaker.getLocalVariable(opcode - 34, offset);
                    stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable));
                    break;
                case DLOAD_0, DLOAD_1, DLOAD_2, DLOAD_3:
                    localVariable = localVariableMaker.getLocalVariable(opcode - 38, offset);
                    stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable));
                    break;
                case ALOAD_0:
                    localVariable = localVariableMaker.getLocalVariable(0, offset);
                    if ((method.getAccessFlags() & ACC_STATIC) == 0) {
                        stack.push(new ThisExpression(lineNumber, localVariable.getType()));
                    } else {
                        stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable));
                    }
                    break;
                case ALOAD_1, ALOAD_2, ALOAD_3:
                    localVariable = localVariableMaker.getLocalVariable(opcode - 42, offset);
                    stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable));
                    break;
                case IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD:
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    stack.push(new ArrayExpression(lineNumber, arrayRef, indexRef));
                    break;
                case ISTORE, LSTORE, FSTORE, DSTORE:
                    valueRef = stack.pop();
                    localVariable = getLocalVariableInAssignment(code[++offset] & 255, offset + 2, valueRef);
                    parseSTORE(statements, stack, lineNumber, offset, localVariable, valueRef);
                    break;
                case ASTORE:
                    valueRef = stack.pop();
                    localVariable = getLocalVariableInAssignment(code[++offset] & 255, offset + 1, valueRef);
                    parseASTORE(statements, stack, lineNumber, offset, localVariable, valueRef);
                    break;
                case ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3:
                    valueRef = stack.pop();
                    localVariable = getLocalVariableInAssignment(opcode - 59, offset + 1, valueRef);
                    parseSTORE(statements, stack, lineNumber, offset, localVariable, valueRef);
                    break;
                case LSTORE_0, LSTORE_1, LSTORE_2, LSTORE_3:
                    valueRef = stack.pop();
                    localVariable = getLocalVariableInAssignment(opcode - 63, offset + 1, valueRef);
                    parseSTORE(statements, stack, lineNumber, offset, localVariable, valueRef);
                    break;
                case FSTORE_0, FSTORE_1, FSTORE_2, FSTORE_3:
                    valueRef = stack.pop();
                    localVariable = getLocalVariableInAssignment(opcode - 67, offset + 1, valueRef);
                    parseSTORE(statements, stack, lineNumber, offset, localVariable, valueRef);
                    break;
                case DSTORE_0, DSTORE_1, DSTORE_2, DSTORE_3:
                    valueRef = stack.pop();
                    localVariable = getLocalVariableInAssignment(opcode - 71, offset + 1, valueRef);
                    parseSTORE(statements, stack, lineNumber, offset, localVariable, valueRef);
                    break;
                case ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3:
                    valueRef = stack.pop();
                    localVariable = getLocalVariableInAssignment(opcode - 75, offset + 1, valueRef);
                    parseASTORE(statements, stack, lineNumber, offset, localVariable, valueRef);
                    break;
                case IASTORE:
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    type1 = arrayRef.getType();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, type1.createType(type1.getDimension()-1), new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case LASTORE:
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_LONG, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case FASTORE:
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case DASTORE:
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case AASTORE:
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    type1 = arrayRef.getType();
                    type2 = type1.createType(type1.getDimension()>0 ? type1.getDimension()-1 : 0);
                    typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(type2, valueRef);
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, type2, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case BASTORE:
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_BYTE, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case CASTORE:
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_CHAR, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case SASTORE:
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_SHORT, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case POP, POP2:
                    expression1 = stack.pop();
                    if (expression1.isMethodInvocationExpression() && expression1.getExpression().getType().isInnerObjectType() && "getClass".equals(expression1.getName())) {
                        enclosingInstances.push(expression1.getExpression());
                    } else if (!expression1.isLocalVariableReferenceExpression() && !expression1.isFieldReferenceExpression() && !expression1.isThisExpression()) {
                        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(TYPE_OBJECT, expression1);
                        statements.add(new ExpressionStatement(expression1.isCastExpression()? expression1.getExpression() : expression1));
                    }
                    break;
                case DUP: // ..., value => ..., value, value
                    expression1 = stack.pop();
                    stack.push(expression1);
                    stack.push(expression1);
                    break;
                case DUP_X1: // ..., value2, value1 => ..., value1, value2, value1
                    expression1 = stack.pop();
                    expression2 = stack.pop();
                    stack.push(expression1);
                    stack.push(expression2);
                    stack.push(expression1);
                    break;
                case DUP_X2:
                    expression1 = stack.pop();
                    expression2 = stack.pop();

                    type2 = expression2.getType();

                    if (TYPE_LONG.equals(type2) || TYPE_DOUBLE.equals(type2)) {
                        // ..., value2, value1 => ..., value1, value2, value1
                        stack.push(expression1);
                    } else {
                        // ..., value3, value2, value1 => ..., value1, value3, value2, value1
                        expression3 = stack.pop();
                        stack.push(expression1);
                        stack.push(expression3);
                    }
                    stack.push(expression2);
                    stack.push(expression1);
                    break;
                case DUP2:
                    expression1 = stack.pop();

                    type1 = expression1.getType();

                    if (TYPE_LONG.equals(type1) || TYPE_DOUBLE.equals(type1)) {
                        // ..., value => ..., value, value
                        stack.push(expression1);
                    } else {
                        // ..., value2, value1 => ..., value2, value1, value2, value1
                        expression2 = stack.pop();
                        stack.push(expression2);
                        stack.push(expression1);
                        stack.push(expression2);
                    }
                    stack.push(expression1);
                    break;
                case DUP2_X1:
                    expression1 = stack.pop();
                    expression2 = stack.pop();

                    type1 = expression1.getType();

                    if (TYPE_LONG.equals(type1) || TYPE_DOUBLE.equals(type1)) {
                        // ..., value2, value1 => ..., value1, value2, value1
                        stack.push(expression1);
                    } else {
                        // ..., value3, value2, value1 => ..., value2, value1, value3, value2, value1
                        expression3 = stack.pop();
                        stack.push(expression2);
                        stack.push(expression1);
                        stack.push(expression3);
                    }
                    stack.push(expression2);
                    stack.push(expression1);
                    break;
                case DUP2_X2:
                    expression1 = stack.pop();
                    expression2 = stack.pop();

                    type1 = expression1.getType();

                    if (TYPE_LONG.equals(type1) || TYPE_DOUBLE.equals(type1)) {
                        type2 = expression2.getType();

                        if (TYPE_LONG.equals(type2) || TYPE_DOUBLE.equals(type2)) {
                            // ..., value2, value1 => ..., value1, value2, value1
                            stack.push(expression1);
                        } else {
                            // ..., value3, value2, value1 => ..., value1, value3, value2, value1
                            expression3 = stack.pop();
                            stack.push(expression1);
                            stack.push(expression3);
                        }
                    } else {
                        expression3 = stack.pop();
                        type3 = expression3.getType();

                        if (TYPE_LONG.equals(type3) || TYPE_DOUBLE.equals(type3)) {
                            // ..., value3, value2, value1 => ..., value2, value1, value3, value2, value1
                            stack.push(expression2);
                            stack.push(expression1);
                        } else {
                            // ..., value4, value3, value2, value1 => ..., value2, value1, value4, value3, value2, value1
                            Expression expression4 = stack.pop();
                            stack.push(expression2);
                            stack.push(expression1);
                            stack.push(expression4);
                        }
                        stack.push(expression3);
                    }
                    stack.push(expression2);
                    stack.push(expression1);
                    break;
                case SWAP: // ..., value2, value1 => ..., value1, value2
                    expression1 = stack.pop();
                    expression2 = stack.pop();
                    stack.push(expression1);
                    stack.push(expression2);
                    break;
                case IADD:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "+", expression2, 6));
                    break;
                case LADD:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "+", expression2, 6));
                    break;
                case FADD:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, expression1, "+", expression2, 6));
                    break;
                case DADD:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, expression1, "+", expression2, 6));
                    break;
                case ISUB:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "-", expression2, 6));
                    break;
                case LSUB:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "-", expression2, 6));
                    break;
                case FSUB:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, expression1, "-", expression2, 6));
                    break;
                case DSUB:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, expression1, "-", expression2, 6));
                    break;
                case IMUL:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "*", expression2, 5));
                    break;
                case LMUL:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "*", expression2, 5));
                    break;
                case FMUL:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, expression1, "*", expression2, 5));
                    break;
                case DMUL:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, expression1, "*", expression2, 5));
                    break;
                case IDIV:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "/", expression2, 5));
                    break;
                case LDIV:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "/", expression2, 5));
                    break;
                case FDIV:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, expression1, "/", expression2, 5));
                    break;
                case DDIV:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, expression1, "/", expression2, 5));
                    break;
                case IREM:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "%", expression2, 5));
                    break;
                case LREM:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "%", expression2, 5));
                    break;
                case FREM:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, expression1, "%", expression2, 5));
                    break;
                case DREM:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, expression1, "%", expression2, 5));
                    break;
                case INEG, LNEG, FNEG, DNEG:
                    stack.push(newPreArithmeticOperatorExpression(lineNumber, "-", stack.pop()));
                    break;
                case ISHL:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "<<", expression2, 7));
                    break;
                case LSHL:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "<<", expression2, 7));
                    break;
                case ISHR:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_INT, expression1, ">>", expression2, 7));
                    break;
                case LSHR:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, ">>", expression2, 7));
                    break;
                case IUSHR:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, ">>>", expression2, 7));
                    break;
                case LUSHR:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, ">>>", expression2, 7));
                    break;
                case IAND:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerOrBooleanBinaryOperatorExpression(lineNumber, expression1, "&", expression2, 10));
                    break;
                case LAND:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "&", expression2, 10));
                    break;
                case IOR:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerOrBooleanBinaryOperatorExpression(lineNumber, expression1, "|", expression2, 12));
                    break;
                case LOR:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "|", expression2, 12));
                    break;
                case IXOR:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerOrBooleanBinaryOperatorExpression(lineNumber, expression1, "^", expression2, 11));
                    break;
                case LXOR:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "^", expression2, 11));
                    break;
                case IINC:
                    localVariable = localVariableMaker.getLocalVariable(code[++offset] & 255, offset);
                    parseIINC(statements, stack, lineNumber, offset, localVariable, (byte)(code[++offset] & 255));
                    break;
                case I2L:
                    stack.push(new CastExpression(lineNumber, TYPE_LONG, stack.pop(), false));
                    break;
                case I2F:
                    stack.push(new CastExpression(lineNumber, TYPE_FLOAT, stack.pop(), false));
                    break;
                case I2D, L2D, F2D:
                    stack.push(new CastExpression(lineNumber, TYPE_DOUBLE, stack.pop(), false));
                        break;
                case L2I, F2I, D2I:
                    stack.push(new CastExpression(lineNumber, TYPE_INT, forceExplicitCastExpression(stack.pop())));
                        break;
                case L2F, D2F:
                    stack.push(new CastExpression(lineNumber, TYPE_FLOAT, forceExplicitCastExpression(stack.pop())));
                        break;
                case F2L, D2L:
                    stack.push(new CastExpression(lineNumber, TYPE_LONG, forceExplicitCastExpression(stack.pop())));
                        break;
                case I2B:
                    stack.push(new CastExpression(lineNumber, TYPE_BYTE, forceExplicitCastExpression(stack.pop())));
                    break;
                case I2C:
                    stack.push(new CastExpression(lineNumber, TYPE_CHAR, forceExplicitCastExpression(stack.pop())));
                    break;
                case I2S:
                    stack.push(new CastExpression(lineNumber, TYPE_SHORT, forceExplicitCastExpression(stack.pop())));
                    break;
                case LCMP, FCMPL, FCMPG, DCMPL, DCMPG:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new ClassFileCmpExpression(lineNumber, expression1, expression2));
                    break;
                case IFEQ:
                    parseIF(stack, lineNumber, basicBlock, "!=", "==", 8);
                    offset += 2; // Skip branch offset
                    break;
                case IFNE:
                    parseIF(stack, lineNumber, basicBlock, "==", "!=", 8);
                    offset += 2; // Skip branch offset
                    break;
                case IFLT:
                    parseIF(stack, lineNumber, basicBlock, ">=", "<", 7);
                    offset += 2; // Skip branch offset
                    break;
                case IFGE:
                    parseIF(stack, lineNumber, basicBlock, "<", ">=", 7);
                    offset += 2; // Skip branch offset
                    break;
                case IFGT:
                    parseIF(stack, lineNumber, basicBlock, "<=", ">", 7);
                    offset += 2; // Skip branch offset
                    break;
                case IFLE:
                    parseIF(stack, lineNumber, basicBlock, ">", "<=", 7);
                    offset += 2; // Skip branch offset
                    break;
                case IF_ICMPEQ, IF_ACMPEQ:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerOrBooleanComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? "!=" : "==", expression2, 9));
                    offset += 2; // Skip branch offset
                    break;
                case IF_ICMPNE, IF_ACMPNE:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerOrBooleanComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? "==" : "!=", expression2, 9));
                    offset += 2; // Skip branch offset
                    break;
                case IF_ICMPLT:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? ">=" : "<", expression2, 8));
                    offset += 2; // Skip branch offset
                    break;
                case IF_ICMPGE:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? "<" : ">=", expression2, 8));
                    offset += 2; // Skip branch offset
                    break;
                case IF_ICMPGT:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? "<=" : ">", expression2, 8));
                    offset += 2; // Skip branch offset
                    break;
                case IF_ICMPLE:
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? ">" : "<=", expression2, 8));
                    offset += 2; // Skip branch offset
                    break;
                case JSR:
                    stack.push(JSR_RETURN_ADDRESS_EXPRESSION);
                    // intended fall through
                case GOTO:
                    offset += 2; // Skip branch offset
                    break;
                case RET:
                    offset++; // Skip index
                    break;
                case TABLESWITCH:
                    offset = offset+4 & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int low = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 |  code[offset++] & 255;
                    int high = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 |  code[offset++] & 255;

                    offset += 4 * (high - low + 1) - 1;

                    statements.add(new SwitchStatement(stack.pop(), new DefaultList<>(high - low + 2)));
                    break;
                case LOOKUPSWITCH:
                    offset = offset+4 & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    count = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 |  code[offset++] & 255;

                    offset += 8 * count - 1;

                    statements.add(new SwitchStatement(stack.pop(), new DefaultList<>(count+1)));
                    break;
                case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN:
                    parseXRETURN(statements, stack, lineNumber);
                    break;
                case Const.RETURN:
                    if (method.isLambda() && !stack.isEmpty()) {
                        statements.add(new ExpressionStatement(stack.pop()));
                    } else {
                        statements.add(RETURN);
                    }
                    break;
                case GETSTATIC:
                    parseGetStatic(stack, constants, lineNumber, (code[++offset] & 255) << 8 | code[++offset] & 255);
                    break;
                case PUTSTATIC:
                    parsePutStatic(statements, stack, constants, lineNumber, (code[++offset] & 255) << 8 | code[++offset] & 255);
                    break;
                case GETFIELD:
                    parseGetField(stack, constants, lineNumber, (code[++offset] & 255) << 8 | code[++offset] & 255);
                    break;
                case PUTFIELD:
                    parsePutField(statements, stack, constants, lineNumber, (code[++offset] & 255) << 8 | code[++offset] & 255);
                    break;
                case INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE:
                    constantMemberRef = constants.getConstant( (code[++offset] & 255) << 8 | code[++offset] & 255 );
                    typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
                    ot = typeMaker.makeFromDescriptorOrInternalTypeName(typeName);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    name = constants.getConstantUtf8(constantNameAndType.getNameIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    
                    TypeMaker.MethodTypes methodTypes = makeMethodTypes(ot.getInternalName(), name, descriptor);

                    methodTypes.handlePolymorphicSignature(typeName, name);
                    
                    BaseExpression parameters = extractParametersFromStack(statements, stack, methodTypes.getParameterTypes());

                    if (opcode == INVOKESTATIC) {
                        expression1 = typeParametersToTypeArgumentsBinder.newMethodInvocationExpression(lineNumber, new ObjectTypeReferenceExpression(lineNumber, ot), ot, name, descriptor, methodTypes, parameters);
                        if (TYPE_VOID.equals(methodTypes.getReturnedType())) {
                            typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(TYPE_OBJECT, expression1);
                            statements.add(new ExpressionStatement(expression1));
                        } else {
                            stack.push(expression1);
                        }
                    } else {
                        expression1 = stack.pop();
                        if (expression1 instanceof NewExpression && expression1.getType().isInnerObjectType() && !enclosingInstances.isEmpty()) {
                            ((NewExpression)expression1).setQualifier(enclosingInstances.pop());
                        }
                        if (expression1.isLocalVariableReferenceExpression()) {
                            ((ClassFileLocalVariableReferenceExpression)expression1).getLocalVariable().typeOnLeft(typeBounds, ot);
                        }
                        if (opcode == INVOKEINTERFACE) {
                            offset += 2; // Skip 'count' and one byte
                        }
                        if (TYPE_VOID.equals(methodTypes.getReturnedType())) {
                            if (opcode == INVOKESPECIAL &&
                                StringConstants.INSTANCE_CONSTRUCTOR.equals(name)) {
                                if (expression1.isNewExpression()) {
                                    typeParametersToTypeArgumentsBinder.updateNewExpression((ClassFileNewExpression)expression1, descriptor, methodTypes, parameters);
                                } else if (ot.getDescriptor().equals(expression1.getType().getDescriptor())) {
                                    statements.add(new ExpressionStatement(typeParametersToTypeArgumentsBinder.newConstructorInvocationExpression(lineNumber, ot, descriptor, methodTypes, parameters)));
                                } else {
                                    statements.add(new ExpressionStatement(typeParametersToTypeArgumentsBinder.newSuperConstructorInvocationExpression(lineNumber, ot, descriptor, methodTypes, parameters)));
                                }
                            } else {
                                expression1 = typeParametersToTypeArgumentsBinder.newMethodInvocationExpression(
                                    lineNumber, getMethodInstanceReference(expression1, ot,  name, descriptor), ot,  name, descriptor, methodTypes, parameters);
                                typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(TYPE_OBJECT, expression1);
                                statements.add(new ExpressionStatement(expression1));
                            }
                        } else {
                            if (opcode == INVOKEVIRTUAL && "toString".equals(name) && "()Ljava/lang/String;".equals(descriptor)) {
                                typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
                                if (StringConstants.JAVA_LANG_STRING_BUILDER.equals(typeName) || StringConstants.JAVA_LANG_STRING_BUFFER.equals(typeName)) {
                                    stack.push(StringConcatenationUtil.create(expression1, lineNumber, typeName));
                                    break;
                                }
                            }
                            stack.push(typeParametersToTypeArgumentsBinder.newMethodInvocationExpression(
                                lineNumber, getMethodInstanceReference(expression1, ot,  name, descriptor), ot, name, descriptor, methodTypes, parameters));
                        }
                    }
                    break;
                case INVOKEDYNAMIC:
                    parseInvokeDynamic(statements, stack, constants, lineNumber,  (code[++offset] & 255) << 8 | code[++offset] & 255);
                    offset += 2; // Skip 2 bytes
                    break;
                case NEW:
                    typeName = constants.getConstantTypeName( (code[++offset] & 255) << 8 | code[++offset] & 255 );
                    stack.push(newNewExpression(lineNumber, typeName));
                    break;
                case NEWARRAY:
                    type1 = PrimitiveTypeUtil.getPrimitiveTypeFromTag( code[++offset] & 255 ).createType(1);
                    stack.push(new NewArray(lineNumber, type1, stack.pop()));
                    break;
                case ANEWARRAY:
                    typeName = constants.getConstantTypeName( (code[++offset] & 255) << 8 | code[++offset] & 255 );
                    if (typeName.charAt(0) == '[') {
                        type1 = typeMaker.makeFromDescriptor(typeName);
                        type1 = type1.createType(type1.getDimension()+1);
                    } else {
                        type1 = typeMaker.makeFromInternalTypeName(typeName).createType(1);
                    }
                    if (typeName.endsWith(TYPE_CLASS.getInternalName())) {
                        ot = (ObjectType)type1;
                        if (ot.getTypeArguments() == null) {
                            type1 = ot.createType(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                        }
                    }
                    stack.push(new NewArray(lineNumber, type1, stack.pop()));
                    break;
                case ARRAYLENGTH:
                    stack.push(new LengthExpression(lineNumber, stack.pop()));
                    break;
                case ATHROW:
                    statements.add(new ThrowStatement(stack.pop()));
                    break;
                case CHECKCAST:
                    typeName = constants.getConstantTypeName( (code[++offset] & 255) << 8 | code[++offset] & 255 );
                    type1 = typeMaker.makeFromDescriptorOrInternalTypeName(typeName);
                    expression1 = stack.peek();
                    if (!type1.isObjectType() || !expression1.getType().isObjectType() || !typeMaker.isRawTypeAssignable((ObjectType) type1, (ObjectType) expression1.getType())) {
                        if (expression1.isCastExpression()) {
                            // Skip double cast
                            ((CastExpression) expression1).setType(type1);
                        } else {
                            boolean castNeeded = true;
                            if (expression1.getType().isGenericType()) {
                                TypeTypes typeTypes = typeMaker.makeTypeTypes(typeName);
                                if (typeTypes != null && typeTypes.getTypeParameters() instanceof TypeParameterWithTypeBounds) {
                                    TypeParameterWithTypeBounds typeParameterWithTypeBounds = (TypeParameterWithTypeBounds) typeTypes.getTypeParameters();
                                    String identifier = typeParameterWithTypeBounds.getIdentifier();
                                    if (identifier.equals(expression1.getType().getName()) 
                                            && type1 instanceof ObjectType
                                            && typeParameterWithTypeBounds.getTypeBounds() instanceof ObjectType
                                            && typeMaker.isRawTypeAssignable((ObjectType) type1, (ObjectType) typeParameterWithTypeBounds.getTypeBounds())) {
                                        castNeeded = false;
                                    }
                                }
                            }
                            if (castNeeded) {
                                searchFirstLineNumberVisitor.init();
                                expression1.accept(searchFirstLineNumberVisitor);
                                CastExpression castExpression = new CastExpression(searchFirstLineNumberVisitor.getLineNumber(), type1, forceExplicitCastExpression(stack.pop()), true, true);
                                stack.push(castExpression);
                            }
                        }
                    }
                    break;
                case INSTANCEOF:
                    typeName = constants.getConstantTypeName( (code[++offset] & 255) << 8 | code[++offset] & 255 );
                    type1 = typeMaker.makeFromDescriptorOrInternalTypeName(typeName);
                    if (type1 == null) {
                        type1 = PrimitiveTypeUtil.getPrimitiveTypeFromDescriptor(typeName);
                    }
                    stack.push(new InstanceOfExpression(lineNumber, stack.pop(), type1));
                    break;
                case MONITORENTER:
                    statements.add(new ClassFileMonitorEnterStatement(stack.pop()));
                    break;
                case MONITOREXIT:
                    statements.add(new ClassFileMonitorExitStatement(stack.pop()));
                    break;
                case WIDE:
                    opcode = code[++offset] & 255;
                    i = (code[++offset] & 255) << 8 | code[++offset] & 255;

                    if (opcode == IINC) {
                        count = (short)( (code[++offset] & 255) << 8 | code[++offset] & 255 );
                        parseIINC(statements, stack, lineNumber, offset, localVariableMaker.getLocalVariable(i, offset), count);
                    } else {
                        switch (opcode) {
                            case ILOAD:
                                localVariable = localVariableMaker.getLocalVariable(i, offset + 4);
                                parseILOAD(statements, stack, lineNumber, offset, localVariable);
                                break;
                            case LLOAD, FLOAD, DLOAD, ALOAD:
                                stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariableMaker.getLocalVariable(i, offset)));
                                break;
                            case ISTORE:
                                valueRef = stack.pop();
                                localVariable = getLocalVariableInAssignment(i, offset + 4, valueRef);
                                statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, localVariable.getType(), new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable), "=", valueRef, 16)));
                                break;
                            case LSTORE:
                                valueRef = stack.pop();
                                localVariable = getLocalVariableInAssignment(i, offset + 4, valueRef);
                                statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_LONG, new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable), "=", valueRef, 16)));
                                break;
                            case FSTORE:
                                valueRef = stack.pop();
                                localVariable = getLocalVariableInAssignment(i, offset + 4, valueRef);
                                statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable), "=", valueRef, 16)));
                                break;
                            case DSTORE:
                                valueRef = stack.pop();
                                localVariable = getLocalVariableInAssignment(i, offset + 4, valueRef);
                                statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable), "=", valueRef, 16)));
                                break;
                            case ASTORE:
                                valueRef = stack.pop();
                                localVariable = getLocalVariableInAssignment(i, offset + 4, valueRef);
                                parseASTORE(statements, stack, lineNumber, offset, localVariable, valueRef);
                                break;
                            case RET:
                                break;
                        }
                    }
                    break;
                case MULTIANEWARRAY:
                    typeName = constants.getConstantTypeName( (code[++offset] & 255) << 8 | code[++offset] & 255 );
                    type1 = typeMaker.makeFromDescriptor(typeName);
                    i = code[++offset] & 255;

                    Expressions dimensions = new Expressions(i);

                    while (i-- > 0) {
                        dimensions.add(stack.pop());
                    }

                    Collections.reverse(dimensions);
                    stack.push(new NewArray(lineNumber, type1, dimensions));
                    break;
                case IFNULL:
                    expression1 = stack.pop();
                    typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(TYPE_OBJECT, expression1);
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression1, basicBlock.mustInverseCondition() ? "!=" : "==", new NullExpression(expression1.getLineNumber(), expression1.getType()), 9));
                    offset += 2; // Skip branch offset
                    checkStack(stack, code, offset);
                    break;
                case IFNONNULL:
                    expression1 = stack.pop();
                    typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(TYPE_OBJECT, expression1);
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression1, basicBlock.mustInverseCondition() ? "==" : "!=", new NullExpression(expression1.getLineNumber(), expression1.getType()), 9));
                    offset += 2; // Skip branch offset
                    checkStack(stack, code, offset);
                    break;
                case JSR_W:
                    stack.push(JSR_RETURN_ADDRESS_EXPRESSION);
                    // intended fall through
                case GOTO_W:
                    offset += 4; // Skip branch offset
                    break;
            }
        }
    }

    private static BaseExpression extractParametersFromStack(Statements statements, DefaultStack<Expression> stack, BaseType parameterTypes) {
        if (parameterTypes == null) {
            return null;
        }

        switch (parameterTypes.size()) {
            case 0:
                return null;
            case 1:
                Expression parameter = stack.pop();
                if (parameter.isNewArray()) {
                    parameter = NewArrayMaker.make(statements, parameter);
                }
                return checkIfLastStatementIsAMultiAssignment(statements, parameter);
            default:
                Expressions parameters = new Expressions(parameterTypes.size());
                int count = parameterTypes.size() - 1;

                for (int i=count; i>=0; --i) {
                    if (!stack.isEmpty()) {
                        parameter = stack.pop();
                        if (parameter.isNewArray()) {
                            parameter = NewArrayMaker.make(statements, parameter);
                        }
                        parameters.add(checkIfLastStatementIsAMultiAssignment(statements, parameter));
                    }
                }

                Collections.reverse(parameters);
                return parameters;
        }
    }

    private static Expression checkIfLastStatementIsAMultiAssignment(Statements statements, Expression parameter) {
        if (!statements.isEmpty()) {
            Expression expression = statements.getLast().getExpression();

            if (expression.isBinaryOperatorExpression() && getLastRightExpression(expression) == parameter) {
                // Return multi assignment expression
                statements.removeLast();
                return expression;
            }
        }

        return parameter;
    }

    private AbstractLocalVariable getLocalVariableInAssignment(int index, int offset, Expression value) {
        Type valueType = value.getType();

        if (value.isNullExpression()) {
            return localVariableMaker.getLocalVariableInNullAssignment(index, offset, valueType);
        }
        if (value.isLocalVariableReferenceExpression()) {
            AbstractLocalVariable valueLocalVariable = ((ClassFileLocalVariableReferenceExpression)value).getLocalVariable();
            AbstractLocalVariable lv = localVariableMaker.getLocalVariableInAssignment(typeBounds, index, offset, valueLocalVariable);
            valueLocalVariable.variableOnLeft(typeBounds, lv);
            return lv;
        }
        if (value.isMethodInvocationExpression()) {
            if (valueType.isObjectType()) {
                // Remove type arguments
                valueType = ((ObjectType)valueType).createType(null);
            } else if (valueType.isGenericType()) {
                valueType = TYPE_UNDEFINED_OBJECT;
            }
        }
        return localVariableMaker.getLocalVariableInAssignment(typeBounds, index, offset, valueType);
    }

    private void parseLDC(DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, Constant constant) {
        switch (constant.getTag()) {
            case CONSTANT_Integer:
                int i = ((ConstantInteger)constant).getBytes();
                stack.push(new IntegerConstantExpression(lineNumber, PrimitiveTypeUtil.getPrimitiveTypeFromValue(i), i));
                break;
            case CONSTANT_Float:
                float f = ((ConstantFloat)constant).getBytes();

                if (Float.compare(f, Float.MIN_VALUE) == 0) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_FLOAT, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_FLOAT), StringConstants.JAVA_LANG_FLOAT, StringConstants.MIN_VALUE, "F"));
                } else if (Float.compare(f, Float.MAX_VALUE) == 0) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_FLOAT, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_FLOAT), StringConstants.JAVA_LANG_FLOAT, StringConstants.MAX_VALUE, "F"));
                } else if (Float.compare(f, Float.NEGATIVE_INFINITY) == 0) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_FLOAT, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_FLOAT), StringConstants.JAVA_LANG_FLOAT, "NEGATIVE_INFINITY", "F"));
                } else if (Float.compare(f, Float.POSITIVE_INFINITY) == 0) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_FLOAT, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_FLOAT), StringConstants.JAVA_LANG_FLOAT, "POSITIVE_INFINITY", "F"));
                } else if (Float.isNaN(f)) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_FLOAT, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_FLOAT), StringConstants.JAVA_LANG_FLOAT, "NaN", "F"));
                } else {
                    stack.push(new FloatConstantExpression(lineNumber, f));
                }
                break;
            case CONSTANT_Class:
                int typeNameIndex = ((ConstantClass) constant).getNameIndex();
                String typeName = ((ConstantUtf8)constants.getConstant(typeNameIndex)).getBytes();
                Type type = typeMaker.makeFromDescriptorOrInternalTypeName(typeName);
                if (type == null) {
                    type = PrimitiveTypeUtil.getPrimitiveTypeFromDescriptor(typeName);
                }
                stack.push(new TypeReferenceDotClassExpression(lineNumber, type));
                break;
            case CONSTANT_Long:
                long l = ((ConstantLong)constant).getBytes();

                if (l == Long.MIN_VALUE) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_LONG, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_LONG), StringConstants.JAVA_LANG_LONG, StringConstants.MIN_VALUE, "J"));
                } else if (l == Long.MAX_VALUE) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_LONG, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_LONG), StringConstants.JAVA_LANG_LONG, StringConstants.MAX_VALUE, "J"));
                } else {
                    stack.push(new LongConstantExpression(lineNumber, l));
                }
                break;
            case CONSTANT_Double:
                double d = ((ConstantDouble)constant).getBytes();

                if (Double.compare(d, Double.MIN_VALUE) == 0) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_DOUBLE), StringConstants.JAVA_LANG_DOUBLE, StringConstants.MIN_VALUE, "D"));
                } else if (Double.compare(d, Double.MAX_VALUE) == 0) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_DOUBLE), StringConstants.JAVA_LANG_DOUBLE, StringConstants.MAX_VALUE, "D"));
                } else if (Double.compare(d, Double.NEGATIVE_INFINITY) == 0) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_DOUBLE), StringConstants.JAVA_LANG_DOUBLE, "NEGATIVE_INFINITY", "D"));
                } else if (Double.compare(d, Double.POSITIVE_INFINITY) == 0) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_DOUBLE), StringConstants.JAVA_LANG_DOUBLE, "POSITIVE_INFINITY", "D"));
                } else if (Double.isNaN(d)) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_DOUBLE), StringConstants.JAVA_LANG_DOUBLE, "NaN", "D"));
                } else if (Double.compare(d, Math.E) == 0) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_MATH), StringConstants.JAVA_LANG_MATH, "E", "D"));
                } else if (Double.compare(d, Math.PI) == 0) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_MATH), StringConstants.JAVA_LANG_MATH, "PI", "D"));
                } else {
                    stack.push(new DoubleConstantExpression(lineNumber, d));
                }
                break;
            case CONSTANT_String:
                int stringIndex = ((ConstantString)constant).getStringIndex();
                stack.push(new StringConstantExpression(lineNumber, constants.getConstantUtf8(stringIndex)));
                break;
        }
    }

    private static void parseILOAD(Statements statements, DefaultStack<Expression> stack, int lineNumber, int offset, AbstractLocalVariable localVariable) {
        if (! statements.isEmpty()) {
            Expression expression = statements.getLast().getExpression();

            if (expression.getLineNumber() == lineNumber && expression.isPreOperatorExpression() && expression.getExpression().isLocalVariableReferenceExpression()) {
                ClassFileLocalVariableReferenceExpression cflvre = (ClassFileLocalVariableReferenceExpression)expression.getExpression();

                if (cflvre.getLocalVariable() == localVariable) {
                    // IINC pattern found -> Remove last statement and create a pre-incrementation
                    statements.removeLast();
                    stack.push(expression);
                    return;
                }
            }
        }

        stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable));
    }

    private void parseSTORE(Statements statements, DefaultStack<Expression> stack, int lineNumber, int offset, AbstractLocalVariable localVariable, Expression valueRef) {
        ClassFileLocalVariableReferenceExpression vre = new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable);

        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(vre.getType(), valueRef);

        if (valueRef.getLineNumber() == lineNumber && valueRef.isBinaryOperatorExpression() && valueRef.getLeftExpression().isLocalVariableReferenceExpression()) {
            ClassFileLocalVariableReferenceExpression lvr = (ClassFileLocalVariableReferenceExpression) valueRef.getLeftExpression();

            if (lvr.getLocalVariable() == localVariable) {
                BinaryOperatorExpression boe = (BinaryOperatorExpression) valueRef;
                Expression expression;

                switch (boe.getOperator()) {
                    case "*":
                        expression = createAssignment(boe, "*=");
                        break;
                    case "/":
                        expression = createAssignment(boe, "/=");
                        break;
                    case "%":
                        expression = createAssignment(boe, "%=");
                        break;
                    case "<<":
                        expression = createAssignment(boe, "<<=");
                        break;
                    case ">>":
                        expression = createAssignment(boe, ">>=");
                        break;
                    case ">>>":
                        expression = createAssignment(boe, ">>>=");
                        break;
                    case "&":
                        expression = createAssignment(boe, "&=");
                        break;
                    case "^":
                        expression = createAssignment(boe, "^=");
                        break;
                    case "|":
                        expression = createAssignment(boe, "|=");
                        break;
                    case "=":
                        expression = boe;
                        break;
                    case "+":
                        if (isPositiveOne(boe.getRightExpression())) {
                            if (stackContainsLocalVariableReference(stack, localVariable)) {
                                stack.pop();
                                stack.push(valueRef);
                                expression = newPostArithmeticOperatorExpression(boe.getLineNumber(), boe.getLeftExpression(), "++");
                            } else {
                                expression = newPreArithmeticOperatorExpression(boe.getLineNumber(), "++", boe.getLeftExpression());
                            }
                        } else if (isNegativeOne(boe.getRightExpression())) {
                            if (stackContainsLocalVariableReference(stack, localVariable)) {
                                stack.pop();
                                stack.push(valueRef);
                                expression = newPostArithmeticOperatorExpression(boe.getLineNumber(), boe.getLeftExpression(), "--");
                            } else {
                                expression = newPreArithmeticOperatorExpression(boe.getLineNumber(), "--", boe.getLeftExpression());
                            }
                        } else {
                            expression = createAssignment(boe, "+=");
                        }
                        break;
                    case "-":
                        if (isPositiveOne(boe.getRightExpression())) {
                            if (stackContainsLocalVariableReference(stack, localVariable)) {
                                stack.pop();
                                stack.push(valueRef);
                                expression = newPostArithmeticOperatorExpression(boe.getLineNumber(), boe.getLeftExpression(), "--");
                            } else {
                                expression = newPreArithmeticOperatorExpression(boe.getLineNumber(), "--", boe.getLeftExpression());
                            }
                        } else if (isNegativeOne(boe.getRightExpression())) {
                            if (stackContainsLocalVariableReference(stack, localVariable)) {
                                stack.pop();
                                stack.push(valueRef);
                                expression = newPostArithmeticOperatorExpression(boe.getLineNumber(), boe.getLeftExpression(), "++");
                            } else {
                                expression = newPreArithmeticOperatorExpression(boe.getLineNumber(), "++", boe.getLeftExpression());
                            }
                        } else {
                            expression = createAssignment(boe, "-=");
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value expression");
                }

                if (!stack.isEmpty() && stack.peek() == valueRef) {
                    stack.replace(valueRef, expression);
                } else {
                    statements.add(new ExpressionStatement(expression));
                }
                return;
            }
        }

        createAssignment(statements, stack, lineNumber, vre, valueRef);
    }

    private static boolean stackContainsLocalVariableReference(DefaultStack<Expression> stack, AbstractLocalVariable localVariable) {
        if (!stack.isEmpty()) {
            Expression expression = stack.peek();

            if (expression.isLocalVariableReferenceExpression()) {
                ClassFileLocalVariableReferenceExpression lvr = (ClassFileLocalVariableReferenceExpression) expression;
                return lvr.getLocalVariable() == localVariable;
            }
        }

        return false;
    }

    private void parsePUT(Statements statements, DefaultStack<Expression> stack, int lineNumber, FieldReferenceExpression fr, Expression valueRef) {
        if (valueRef.isNewArray()) {
            valueRef = NewArrayMaker.make(statements, valueRef);
        }

        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(fr.getType(), valueRef);

        if (valueRef.getLineNumber() == lineNumber
         && valueRef.isBinaryOperatorExpression()
         && valueRef.getLeftExpression().isFieldReferenceExpression()) {
            FieldReferenceExpression boefr = (FieldReferenceExpression)valueRef.getLeftExpression();

            if (boefr.getName().equals(fr.getName())
             && boefr.getExpression().getType().equals(fr.getExpression().getType())
             && boefr.getExpression().getIndex().getIntegerValue() == fr.getExpression().getIndex().getIntegerValue()) {
                BinaryOperatorExpression boe = (BinaryOperatorExpression)valueRef;
                Expression expression;

                switch (boe.getOperator()) {
                    case "*":
                        expression = createAssignment(boe, "*=");
                        break;
                    case "/":
                        expression = createAssignment(boe, "/=");
                        break;
                    case "%":
                        expression = createAssignment(boe, "%=");
                        break;
                    case "<<":
                        expression = createAssignment(boe, "<<=");
                        break;
                    case ">>":
                        expression = createAssignment(boe, ">>=");
                        break;
                    case ">>>":
                        expression = createAssignment(boe, ">>>=");
                        break;
                    case "&":
                        expression = createAssignment(boe, "&=");
                        break;
                    case "^":
                        expression = createAssignment(boe, "^=");
                        break;
                    case "|":
                        expression = createAssignment(boe, "|=");
                        break;
                    case "=":
                        expression = boe;
                        break;
                    case "+":
                        if (isPositiveOne(boe.getRightExpression())) {
                            if (stackContainsFieldReference(stack, fr)) {
                                stack.pop();
                                stack.push(valueRef);
                                expression = newPostArithmeticOperatorExpression(boe.getLineNumber(), boe.getLeftExpression(), "++");
                            } else {
                                expression = newPreArithmeticOperatorExpression(boe.getLineNumber(), "++", boe.getLeftExpression());
                            }
                        } else if (isNegativeOne(boe.getRightExpression())) {
                            if (stackContainsFieldReference(stack, fr)) {
                                stack.pop();
                                stack.push(valueRef);
                                expression = newPostArithmeticOperatorExpression(boe.getLineNumber(), boe.getLeftExpression(), "--");
                            } else {
                                expression = newPreArithmeticOperatorExpression(boe.getLineNumber(), "--", boe.getLeftExpression());
                            }
                        } else {
                            expression = createAssignment(boe, "+=");
                        }
                        break;
                    case "-":
                        if (isPositiveOne(boe.getRightExpression())) {
                            if (stackContainsFieldReference(stack, fr)) {
                                stack.pop();
                                stack.push(valueRef);
                                expression = newPostArithmeticOperatorExpression(boe.getLineNumber(), boe.getLeftExpression(), "--");
                            } else {
                                expression = newPreArithmeticOperatorExpression(boe.getLineNumber(), "--", boe.getLeftExpression());
                            }
                        } else if (isNegativeOne(boe.getRightExpression())) {
                            if (stackContainsFieldReference(stack, fr)) {
                                stack.pop();
                                stack.push(valueRef);
                                expression = newPostArithmeticOperatorExpression(boe.getLineNumber(), boe.getLeftExpression(), "++");
                            } else {
                                expression = newPreArithmeticOperatorExpression(boe.getLineNumber(), "++", boe.getLeftExpression());
                            }
                        } else {
                            expression = createAssignment(boe, "-=");
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value expression");
                }

                if (!stack.isEmpty() && stack.peek() == valueRef) {
                    stack.replace(valueRef, expression);
                } else {
                    statements.add(new ExpressionStatement(expression));
                }
                return;
            }
        }

        createAssignment(statements, stack, lineNumber, fr, valueRef);
    }

    private void parseInvokeDynamic(Statements statements, DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, int index) {
        // Remove previous 'getClass()' or cast if exists
        if (! statements.isEmpty()) {
            Expression expression = statements.getLast().getExpression();

            if (expression.isMethodInvocationExpression()) {
                MethodInvocationExpression mie = (MethodInvocationExpression)expression;

                if ("getClass".equals(mie.getName()) && "()Ljava/lang/Class;".equals(mie.getDescriptor()) && StringConstants.JAVA_LANG_OBJECT.equals(mie.getInternalTypeName())) {
                    statements.removeLast();
                }
            }
        }

        // Create expression
        ConstantMemberRef constantMemberRef = constants.getConstant(index);

        ConstantNameAndType indyCnat = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String indyMethodName = constants.getConstantUtf8(indyCnat.getNameIndex());
        String indyDescriptor = constants.getConstantUtf8(indyCnat.getSignatureIndex());
        TypeMaker.MethodTypes indyMethodTypes = typeMaker.makeMethodTypes(indyDescriptor);
        
        BaseExpression indyParameters = extractParametersFromStack(statements, stack, indyMethodTypes.getParameterTypes());
        BootstrapMethod bootstrapMethod = attributeBootstrapMethods.getBootstrapMethod(constantMemberRef.getClassIndex());
        int[] bootstrapArguments = bootstrapMethod.getBootstrapArguments();

        if ("makeConcatWithConstants".equals(indyMethodName)) {
            // Create Java 9+ string concatenation
            String recipe = constants.getConstantString(bootstrapArguments[0]);
            stack.push(StringConcatenationUtil.create(recipe, indyParameters));
            return;
        }
        if ("makeConcat".equals(indyMethodName)) {
            // Create Java 9+ string concatenation
            stack.push(StringConcatenationUtil.create(indyParameters));
            return;
        }

        ConstantMethodType cmt0 = constants.getConstant(bootstrapArguments[0]);
        String descriptor0 = constants.getConstantUtf8(cmt0.getDescriptorIndex());
        TypeMaker.MethodTypes methodTypes0 = typeMaker.makeMethodTypes(descriptor0);
        int parameterCount = methodTypes0.getParameterTypes() == null ? 0 : methodTypes0.getParameterTypes().size();
        ConstantMethodHandle constantMethodHandle1 = constants.getConstant(bootstrapArguments[1]);
        ConstantMemberRef cmr1 = constants.getConstant(constantMethodHandle1.getReferenceIndex());
        String typeName = constants.getConstantTypeName(cmr1.getClassIndex());
        ConstantNameAndType cnat1 = constants.getConstant(cmr1.getNameAndTypeIndex());
        String name1 = constants.getConstantUtf8(cnat1.getNameIndex());
        String descriptor1 = constants.getConstantUtf8(cnat1.getSignatureIndex());

        if (typeName.equals(internalTypeName)) {
            for (ClassFileConstructorOrMethodDeclaration methodDeclaration : bodyDeclaration.getMethodDeclarations()) {
                if ((methodDeclaration.getFlags() & (ACC_SYNTHETIC|ACC_PRIVATE)) == (ACC_SYNTHETIC|ACC_PRIVATE) && methodDeclaration.getMethod().getName().equals(name1) && methodDeclaration.getMethod().getDescriptor().equals(descriptor1)) {
                    // Create lambda expression
                    ClassFileMethodDeclaration cfmd = (ClassFileMethodDeclaration)methodDeclaration;
                    if (cfmd.getStatements() == null) {
                        CreateInstructionsVisitor createInstructionsVisitor = new CreateInstructionsVisitor(typeMaker);
                        createInstructionsVisitor.createParametersVariablesAndStatements(cfmd, false);
                    }
                    stack.push(new LambdaIdentifiersExpression(
                            lineNumber, indyMethodTypes.getReturnedType(), indyMethodTypes.getReturnedType(),
                            prepareLambdaParameterNames(cfmd.getFormalParameters(), parameterCount),
                            prepareLambdaStatements(cfmd.getFormalParameters(), indyParameters, cfmd.getStatements())));
                    return;
                }
            }
        }

        if (indyParameters == null) {
            // Create static method reference
            ObjectType ot = typeMaker.makeFromInternalTypeName(typeName);

            if (StringConstants.INSTANCE_CONSTRUCTOR.equals(name1)) {
                stack.push(new ConstructorReferenceExpression(lineNumber, indyMethodTypes.getReturnedType(), ot, descriptor1));
            } else {
                stack.push(new MethodReferenceExpression(lineNumber, indyMethodTypes.getReturnedType(), new ObjectTypeReferenceExpression(lineNumber, ot), typeName, name1, descriptor1));
            }
            return;
        }

        // Create method reference
        stack.push(new MethodReferenceExpression(lineNumber, indyMethodTypes.getReturnedType(), (Expression)indyParameters, typeName, name1, descriptor1));
    }

    private List<String> prepareLambdaParameterNames(BaseFormalParameter formalParameters, int parameterCount) {
        if (formalParameters == null && parameterCount > 0) {
            List<String> ignoredParameters = new ArrayList<>(parameterCount);
            for (int i = 0; i < parameterCount; i++) {
                ignoredParameters.add("ignoredParameter" + (i + 1));
            }
            return ignoredParameters;
        }
        if (formalParameters == null || parameterCount == 0) {
            return null;
        }
        lambdaParameterNamesVisitor.init();
        formalParameters.accept(lambdaParameterNamesVisitor);
        List<String> names = lambdaParameterNamesVisitor.getNames();
        assert names.size() >= parameterCount;
        if (names.size() == parameterCount) {
            return names;
        }
        return names.subList(names.size() - parameterCount, names.size());
    }

    private BaseStatement prepareLambdaStatements(BaseFormalParameter formalParameters, BaseExpression indyParameters, BaseStatement baseStatement) {
        if (baseStatement != null) {
            if (formalParameters != null && indyParameters != null) {
                int size = indyParameters.size();

                if (size > 0 && size <= formalParameters.size()) {
                    Map<String, String> mapping = new HashMap<>();
                    Expression expression = indyParameters.getFirst();

                    if (expression.isLocalVariableReferenceExpression()) {
                        String name = formalParameters.getFirst().getName();
                        String newName = expression.getName();

                        if (!name.equals(newName)) {
                            mapping.put(name, newName);
                        }
                    }

                    if (size > 1) {
                        DefaultList<FormalParameter> formalParameterList = formalParameters.getList();
                        DefaultList<Expression> list = indyParameters.getList();

                        for (int i = 1; i < size; i++) {
                            expression = list.get(i);

                            if (expression.isLocalVariableReferenceExpression()) {
                                FormalParameter formalParameter = formalParameterList.get(i);
                                if (formalParameter instanceof ClassFileFormalParameter) {
                                    ClassFileFormalParameter classFileFormalParameter = (ClassFileFormalParameter) formalParameter;
                                    AbstractLocalVariable localVariable = classFileFormalParameter.getLocalVariable();
                                    if (!localVariable.isAssignableFrom(typeBounds, expression.getType())) {
                                        continue;
                                    }
                                }
                                
                                String name = formalParameter.getName();
                                String newName = expression.getName();

                                if (name.startsWith("param") && !name.equals(newName)) {
                                    mapping.put(name, newName);
                                }
                            }
                        }
                    }
                    if (!mapping.isEmpty()) {
                        renameLocalVariablesVisitor.init(mapping, true);
                        baseStatement.accept(renameLocalVariablesVisitor);
                    }
                }
            }

            if (baseStatement.size() == 1) {
                Statement statement = baseStatement.getFirst();

                if (statement.isReturnExpressionStatement() || statement.isExpressionStatement()) {
                    return new LambdaExpressionStatement(statement.getExpression());
                }
            }
        }

        return baseStatement;
    }

    private static boolean stackContainsFieldReference(DefaultStack<Expression> stack, FieldReferenceExpression fr) {
        if (!stack.isEmpty()) {
            Expression expression = stack.peek();

            if (expression.isFieldReferenceExpression()) {
                return expression.getName().equals(fr.getName()) && expression.getExpression().getType().equals(fr.getExpression().getType());
            }
        }

        return false;
    }

    private static Expression createAssignment(BinaryOperatorExpression boe, String operator) {
        boe.setOperator(operator);
        boe.setPriority(16);
        return boe;
    }

    private static boolean isPositiveOne(Expression expression) {
        return expression.isIntegerConstantExpression() && expression.getIntegerValue() == 1 || expression.isLongConstantExpression() && expression.getLongValue() == 1L || expression.isFloatConstantExpression() && Float.compare(expression.getFloatValue(), 1.0F) == 0 || expression.isDoubleConstantExpression() && Double.compare(expression.getDoubleValue(), 1.0D) == 0;
    }

    private static boolean isNegativeOne(Expression expression) {
        return expression.isIntegerConstantExpression() && expression.getIntegerValue() == -1 || expression.isLongConstantExpression() && expression.getLongValue() == -1L || expression.isFloatConstantExpression() && Float.compare(expression.getFloatValue(), -1.0F) == 0 || expression.isDoubleConstantExpression() && Double.compare(expression.getDoubleValue(), -1.0D) == 0;
    }

    private void parseASTORE(Statements statements, DefaultStack<Expression> stack, int lineNumber, int offset, AbstractLocalVariable localVariable, Expression valueRef) {
        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(localVariable.getType(), valueRef);
        if (!ObjectType.TYPE_OBJECT.equals(localVariable.getType())) {
            localVariable.typeOnRight(typeBounds, valueRef.getType());
        }
        ClassFileLocalVariableReferenceExpression vre = new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable);
        Expression oldValueRef = valueRef;

        if (valueRef.isNewArray()) {
            valueRef = NewArrayMaker.make(statements, valueRef);
        }

        if (oldValueRef != valueRef) {
            stack.replace(oldValueRef, valueRef);
        }

        createAssignment(statements, stack, lineNumber, vre, valueRef);
    }

    private void createAssignment(Statements statements, DefaultStack<Expression> stack, int lineNumber, Expression leftExpression, Expression rightExpression) {
        if (!stack.isEmpty() && stack.peek() == rightExpression) {
            stack.push(new BinaryOperatorExpression(lineNumber, leftExpression.getType(), leftExpression, "=", stack.pop(), 16));
            return;
        }

        if (!statements.isEmpty()) {
            Statement lastStatement = statements.getLast();

            if (lastStatement.isExpressionStatement()) {
                ExpressionStatement lastES = (ExpressionStatement)lastStatement;
                Expression lastExpression = lastStatement.getExpression();

                if (lastExpression.isBinaryOperatorExpression()) {
                    if (getLastRightExpression(lastExpression) == rightExpression) {
                        // Multi assignment
                        lastES.setExpression(new BinaryOperatorExpression(lineNumber, leftExpression.getType(), leftExpression, "=", lastExpression, 16));
                        return;
                    }

                    if (lineNumber > 0 && lastExpression.getLineNumber() == lineNumber && lastExpression.getLeftExpression().getClass() == rightExpression.getClass()) {
                        if (rightExpression.isLocalVariableReferenceExpression()) {
                            ClassFileLocalVariableReferenceExpression lvr1 = (ClassFileLocalVariableReferenceExpression) lastExpression.getLeftExpression();
                            ClassFileLocalVariableReferenceExpression lvr2 = (ClassFileLocalVariableReferenceExpression) rightExpression;

                            if (lvr1.getLocalVariable() == lvr2.getLocalVariable()) {
                                // Multi assignment
                                lastES.setExpression(new BinaryOperatorExpression(lineNumber, leftExpression.getType(), leftExpression, "=", lastExpression, 16));
                                return;
                            }
                        } else if (rightExpression.isFieldReferenceExpression()) {
                            FieldReferenceExpression fr1 = (FieldReferenceExpression) lastExpression.getLeftExpression();
                            FieldReferenceExpression fr2 = (FieldReferenceExpression) rightExpression;

                            if (fr1.getName().equals(fr2.getName()) && fr1.getExpression().getType().equals(fr2.getExpression().getType())) {
                                // Multi assignment
                                lastES.setExpression(new BinaryOperatorExpression(lineNumber, leftExpression.getType(), leftExpression, "=", lastExpression, 16));
                                return;
                            }
                        }
                    }
                } else if (lastExpression.isPreOperatorExpression()) {
                    if (lastExpression.getExpression().getClass() == rightExpression.getClass()) {
                        if (rightExpression.isLocalVariableReferenceExpression()) {
                            ClassFileLocalVariableReferenceExpression lvr1 = (ClassFileLocalVariableReferenceExpression)lastExpression.getExpression();
                            ClassFileLocalVariableReferenceExpression lvr2 = (ClassFileLocalVariableReferenceExpression)rightExpression;

                            if (lvr1.getLocalVariable() == lvr2.getLocalVariable()) {
                                rightExpression = newPreArithmeticOperatorExpression(lastExpression.getLineNumber(), lastExpression.getOperator(), lastExpression.getExpression());
                                statements.removeLast();
                            }
                        } else if (rightExpression.isFieldReferenceExpression()) {
                            FieldReferenceExpression fr1 = (FieldReferenceExpression)lastExpression.getExpression();
                            FieldReferenceExpression fr2 = (FieldReferenceExpression)rightExpression;

                            if (fr1.getName().equals(fr2.getName()) && fr1.getExpression().getType().equals(fr2.getExpression().getType())) {
                                rightExpression = newPreArithmeticOperatorExpression(lastExpression.getLineNumber(), lastExpression.getOperator(), lastExpression.getExpression());
                                statements.removeLast();
                            }
                        }
                    }
                } else if (lastExpression.isPostOperatorExpression()) {
                    PostOperatorExpression poe = (PostOperatorExpression)lastExpression;

                    if (poe.getExpression() == rightExpression) {
                        rightExpression = poe;
                        statements.removeLast();
                    }
                }
            }
        }

        statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, leftExpression.getType(), leftExpression, "=", rightExpression, 16)));
    }

    private void parseIINC(Statements statements, DefaultStack<Expression> stack, int lineNumber, int offset, AbstractLocalVariable localVariable, int count) {
        Expression expression;

        if (!stack.isEmpty()) {
            expression = stack.peek();

            if (expression.getLineNumber() == lineNumber && expression.isLocalVariableReferenceExpression()) {
                ClassFileLocalVariableReferenceExpression exp = (ClassFileLocalVariableReferenceExpression)expression;

                if (exp.getLocalVariable() == localVariable) {
                    // ILOAD found -> Create a post-incrementation
                    stack.pop();

                    if (count == 1) {
                        stack.push(newPostArithmeticOperatorExpression(lineNumber, expression, "++"));
                    } else if (count == -1) {
                        stack.push(newPostArithmeticOperatorExpression(lineNumber, expression, "--"));
                    } else {
                        throw new IllegalStateException();
                    }

                    return;
                }
            }
        }

        expression = new ClassFileLocalVariableReferenceExpression(lineNumber, offset, localVariable);

        if (count == 1) {
            expression = newPreArithmeticOperatorExpression(lineNumber, "++", expression);
        } else if (count == -1) {
            expression = newPreArithmeticOperatorExpression(lineNumber, "--", expression);
        } else if (count >= 0) {
            expression = new BinaryOperatorExpression(lineNumber, expression.getType(), expression, "+=", new IntegerConstantExpression(lineNumber, expression.getType(), count), 16);
        } else {
            expression = new BinaryOperatorExpression(lineNumber, expression.getType(), expression, "-=", new IntegerConstantExpression(lineNumber, expression.getType(), -count), 16);
        }

        statements.add(new ExpressionStatement(expression));
    }

    private void parseIF(DefaultStack<Expression> stack, int lineNumber, BasicBlock basicBlock, String operator1, String operator2, int priority) {
        Expression expression = stack.pop();

        if (expression instanceof ClassFileCmpExpression) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ClassFileCmpExpression cmp = (ClassFileCmpExpression) expression;
            typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(cmp.getLeftExpression().getType(), cmp.getLeftExpression());
            typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(cmp.getRightExpression().getType(), cmp.getRightExpression());

            stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, cmp.getLeftExpression(), basicBlock.mustInverseCondition() ? operator1 : operator2, cmp.getRightExpression(), priority));
        } else if (expression.getType().isPrimitiveType()) {
            PrimitiveType pt = (PrimitiveType)expression.getType();

            typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(pt, expression, true);

            switch (pt.getJavaPrimitiveFlags()) {
                case FLAG_BOOLEAN:
                    if (basicBlock.mustInverseCondition() ^ "==".equals(operator1)) {
                        stack.push(expression);
                    } else {
                        stack.push(new PreOperatorExpression(lineNumber, "!", expression));
                    }
                    break;
                case FLAG_FLOAT:
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression, basicBlock.mustInverseCondition() ? operator1 : operator2, new FloatConstantExpression(lineNumber, 0), 9));
                    break;
                case FLAG_DOUBLE:
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression, basicBlock.mustInverseCondition() ? operator1 : operator2, new DoubleConstantExpression(lineNumber, 0), 9));
                    break;
                case FLAG_LONG:
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression, basicBlock.mustInverseCondition() ? operator1 : operator2, new LongConstantExpression(lineNumber, 0), 9));
                    break;
                default:
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression, basicBlock.mustInverseCondition() ? operator1 : operator2, new IntegerConstantExpression(lineNumber, pt, 0), 9));
                    break;
            }
        } else {
            stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression, basicBlock.mustInverseCondition() ? operator1 : operator2, new NullExpression(lineNumber, expression.getType()), 9));
        }
    }

    private void parseXRETURN(Statements statements, DefaultStack<Expression> stack, int lineNumber) {
        Expression valueRef = stack.pop();

        if (valueRef.isNewArray()) {
            valueRef = NewArrayMaker.make(statements, valueRef);
        }

        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(returnedType, valueRef);

        if (lineNumber > valueRef.getLineNumber()) {
            lineNumber = valueRef.getLineNumber();
        }

        if (!statements.isEmpty() && valueRef.isLocalVariableReferenceExpression()) {
            Statement lastStatement = statements.getLast();

            if (lastStatement.isExpressionStatement()) {
                Expression expression = statements.getLast().getExpression();

                if (lineNumber <= expression.getLineNumber() && expression.isBinaryOperatorExpression() &&
                    "=".equals(expression.getOperator()) && expression.getLeftExpression().isLocalVariableReferenceExpression()) {
                    ClassFileLocalVariableReferenceExpression vre1 = (ClassFileLocalVariableReferenceExpression) expression.getLeftExpression();
                    ClassFileLocalVariableReferenceExpression vre2 = (ClassFileLocalVariableReferenceExpression) valueRef;

                    if (vre1.getLocalVariable() == vre2.getLocalVariable()) {
                        // Remove synthetic local variable
                        localVariableMaker.removeLocalVariable(vre1.getLocalVariable());
                        // Remove assignment statement
                        statements.removeLast();
                        statements.add(new ReturnExpressionStatement(lineNumber, expression.getRightExpression()));
                        return;
                    }
                }
            }
        }

        statements.add(new ReturnExpressionStatement(lineNumber, valueRef));
    }

    private void parseGetStatic(DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, int index) {
        ConstantMemberRef constantMemberRef = constants.getConstant(index);
        String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
        ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());

        if ("TYPE".equals(name) && typeName.startsWith("java/lang/")) {
            switch (typeName) {
                case StringConstants.JAVA_LANG_BOOLEAN:
                    stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_BOOLEAN));
                    return;
                case StringConstants.JAVA_LANG_CHARACTER:
                    stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_CHAR));
                    return;
                case StringConstants.JAVA_LANG_FLOAT:
                    stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_FLOAT));
                    return;
                case StringConstants.JAVA_LANG_DOUBLE:
                    stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_DOUBLE));
                    return;
                case StringConstants.JAVA_LANG_BYTE:
                    stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_BYTE));
                    return;
                case StringConstants.JAVA_LANG_SHORT:
                    stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_SHORT));
                    return;
                case StringConstants.JAVA_LANG_INTEGER:
                    stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_INT));
                    return;
                case StringConstants.JAVA_LANG_LONG:
                    stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_LONG));
                    return;
                case StringConstants.JAVA_LANG_VOID:
                    stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_VOID));
                    return;
            }
        }

        ObjectType ot = typeMaker.makeFromInternalTypeName(typeName);
        String descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
        Type type = makeFieldType(ot.getInternalName(), name, descriptor);
        Expression objectRef = new ObjectTypeReferenceExpression(lineNumber, ot, !internalTypeName.equals(typeName) || localVariableMaker.containsName(name));
        stack.push(typeParametersToTypeArgumentsBinder.newFieldReferenceExpression(lineNumber, type, objectRef, ot, name, descriptor));
    }

    private void parsePutStatic(Statements statements, DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, int index) {
        ConstantMemberRef constantMemberRef = constants.getConstant(index);
        String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
        ObjectType ot = typeMaker.makeFromInternalTypeName(typeName);
        ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());
        String descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
        Type type = makeFieldType(ot.getInternalName(), name, descriptor);
        Expression valueRef = stack.pop();
        Expression objectRef = new ObjectTypeReferenceExpression(lineNumber, ot, !internalTypeName.equals(typeName) || localVariableMaker.containsName(name));
        FieldReferenceExpression fieldRef = typeParametersToTypeArgumentsBinder.newFieldReferenceExpression(lineNumber, type, objectRef, ot, name, descriptor);
        parsePUT(statements, stack, lineNumber, fieldRef, valueRef);
    }

    private void parseGetField(DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, int index) {
        ConstantMemberRef constantMemberRef = constants.getConstant(index);
        String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
        ObjectType ot = typeMaker.makeFromInternalTypeName(typeName);
        ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());
        String descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
        Type type = makeFieldType(ot.getInternalName(), name, descriptor);
        Expression objectRef = stack.pop();
        stack.push(typeParametersToTypeArgumentsBinder.newFieldReferenceExpression(lineNumber, type, getFieldInstanceReference(objectRef, ot,  name), ot, name, descriptor));
    }

    private void parsePutField(Statements statements, DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, int index) {
        ConstantMemberRef constantMemberRef = constants.getConstant(index);
        String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
        ObjectType ot = typeMaker.makeFromInternalTypeName(typeName);
        ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());
        String descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
        Type type = makeFieldType(ot.getInternalName(), name, descriptor);
        Expression valueRef = stack.pop();
        Expression objectRef = stack.pop();
        FieldReferenceExpression fieldRef = typeParametersToTypeArgumentsBinder.newFieldReferenceExpression(lineNumber, type, getFieldInstanceReference(objectRef, ot,  name), ot, name, descriptor);
        parsePUT(statements, stack, lineNumber, fieldRef, valueRef);
    }

    private static Expression getLastRightExpression(Expression boe) {
        do {
            if (! "=".equals(boe.getOperator())) {
                return boe;
            }

            if (! boe.getRightExpression().isBinaryOperatorExpression()) {
                return boe.getRightExpression();
            }

            boe = boe.getRightExpression();
        } while (true);
    }

    private Expression newNewExpression(int lineNumber, String internalTypeName) {
        ObjectType objectType = typeMaker.makeFromInternalTypeName(internalTypeName);

        if (objectType.getQualifiedName() == null && objectType.getName() == null) {
            ClassFileTypeDeclaration typeDeclaration = bodyDeclaration.getInnerTypeDeclaration(internalTypeName);

            if (typeDeclaration == null) {
                return new ClassFileNewExpression(lineNumber, TYPE_OBJECT, false);
            }
            if (typeDeclaration.isClassDeclaration()) {
                ClassFileClassDeclaration declaration = (ClassFileClassDeclaration) typeDeclaration;
                BodyDeclaration localBodyDeclaration;

                if (this.internalTypeName.equals(internalTypeName)) {
                    localBodyDeclaration = null;
                } else {
                    localBodyDeclaration = declaration.getBodyDeclaration();
                }

                if (declaration.getInterfaces() != null) {
                    return new ClassFileNewExpression(lineNumber, (ObjectType) declaration.getInterfaces(), localBodyDeclaration, true, false);
                }
                if (declaration.getSuperType() != null) {
                    return new ClassFileNewExpression(lineNumber, declaration.getSuperType(), localBodyDeclaration, true, false);
                }
                return new ClassFileNewExpression(lineNumber, TYPE_OBJECT, localBodyDeclaration, true, false);
            }
        }

        return new ClassFileNewExpression(lineNumber, objectType, false);
    }

    /**
     * Operators = { "+", "-", "*", "/", "%", "<<", ">>", ">>>" }
     * See "Additive Operators (+ and -) for Numeric Types": https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.18.2
     * See "Shift Operators":                                https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19
     */
    private Expression newIntegerBinaryOperatorExpression(int lineNumber, Expression leftExpression, String operator, Expression rightExpression, int priority) {
        if (leftExpression.isLocalVariableReferenceExpression()) {
            AbstractLocalVariable leftVariable = ((ClassFileLocalVariableReferenceExpression)leftExpression).getLocalVariable();
            leftVariable.typeOnLeft(typeBounds, MAYBE_BYTE_TYPE);
        }

        if (rightExpression.isLocalVariableReferenceExpression()) {
            AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();
            rightVariable.typeOnLeft(typeBounds, MAYBE_BYTE_TYPE);
        }

        return new BinaryOperatorExpression(lineNumber, TYPE_INT, leftExpression, operator, rightExpression, priority);
    }

    /**
     * Operators = { "&", "|", "^" }
     * See "Binary Numeric Promotion": https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.22.1
     */
    private Expression newIntegerOrBooleanBinaryOperatorExpression(int lineNumber, Expression leftExpression, String operator, Expression rightExpression, int priority) {
        Type type = TYPE_INT;

        if (leftExpression.isLocalVariableReferenceExpression()) {
            AbstractLocalVariable leftVariable = ((ClassFileLocalVariableReferenceExpression)leftExpression).getLocalVariable();

            if (rightExpression.isLocalVariableReferenceExpression()) {
                AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();

                if (leftVariable.isAssignableFrom(typeBounds, TYPE_BOOLEAN) || rightVariable.isAssignableFrom(typeBounds, TYPE_BOOLEAN)) {
                    leftVariable.variableOnRight(typeBounds, rightVariable);
                    rightVariable.variableOnLeft(typeBounds, leftVariable);

                    if (leftVariable.getType() == TYPE_BOOLEAN || rightVariable.getType() == TYPE_BOOLEAN) {
                        type = TYPE_BOOLEAN;
                    }
                }
            } else if (rightExpression.getType() == TYPE_BOOLEAN) {
                type = TYPE_BOOLEAN;
                leftVariable.typeOnRight(typeBounds, type);
            }
        } else if (rightExpression.isLocalVariableReferenceExpression() && leftExpression.getType() == TYPE_BOOLEAN) {
            AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();
            type = TYPE_BOOLEAN;
            rightVariable.typeOnRight(typeBounds, type);
        }

        if (type == TYPE_INT && leftExpression.getType().isPrimitiveType() && rightExpression.getType().isPrimitiveType()) {
            int leftFlags = ((PrimitiveType)leftExpression.getType()).getFlags();
            int rightFlags = ((PrimitiveType)rightExpression.getType()).getFlags();
            boolean leftBoolean = (leftFlags & FLAG_BOOLEAN) != 0;
            boolean rightBoolean = (rightFlags & FLAG_BOOLEAN) != 0;
            int commonflags = leftFlags | rightFlags;

            if (!leftBoolean || !rightBoolean) {
                commonflags &= ~FLAG_BOOLEAN;
            }

            type = PrimitiveTypeUtil.getPrimitiveTypeFromFlags(commonflags);

            if (type == null) {
                type = TYPE_INT;
            }
        }

        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(leftExpression.getType(), rightExpression);
        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(rightExpression.getType(), leftExpression);

        return new BinaryOperatorExpression(lineNumber, type, leftExpression, operator, rightExpression, priority);
    }

    /**
     * Operators = { "==", "!=" }
     * See "Numerical Equality Operators == and !=": https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.21.1
     */
    private Expression newIntegerOrBooleanComparisonOperatorExpression(int lineNumber, Expression leftExpression, String operator, Expression rightExpression, int priority) {
        if (leftExpression.isLocalVariableReferenceExpression()) {
            AbstractLocalVariable leftVariable = ((ClassFileLocalVariableReferenceExpression)leftExpression).getLocalVariable();

            if (rightExpression.isLocalVariableReferenceExpression()) {
                AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();

                if (leftVariable.isAssignableFrom(typeBounds, TYPE_BOOLEAN) || rightVariable.isAssignableFrom(typeBounds, TYPE_BOOLEAN)) {
                    leftVariable.variableOnRight(typeBounds, rightVariable);
                    rightVariable.variableOnLeft(typeBounds, leftVariable);
                }
            } else if (rightExpression.getType() == TYPE_BOOLEAN) {
                leftVariable.typeOnRight(typeBounds, TYPE_BOOLEAN);
            }
        } else if (rightExpression.isLocalVariableReferenceExpression() && leftExpression.getType() == TYPE_BOOLEAN) {
            AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();
            rightVariable.typeOnRight(typeBounds, TYPE_BOOLEAN);
        }

        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(leftExpression.getType(), rightExpression);
        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(rightExpression.getType(), leftExpression);

        return new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, leftExpression, operator, rightExpression, priority);
    }

    /**
     * Operators = { "==", "!=" }
     * See "Numerical Equality Operators == and !=": https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.21.1
     */
    private Expression newIntegerComparisonOperatorExpression(int lineNumber, Expression leftExpression, String operator, Expression rightExpression, int priority) {
        if (leftExpression.isLocalVariableReferenceExpression()) {
            AbstractLocalVariable leftVariable = ((ClassFileLocalVariableReferenceExpression)leftExpression).getLocalVariable();
            leftVariable.typeOnLeft(typeBounds, MAYBE_BYTE_TYPE);
        }

        if (rightExpression.isLocalVariableReferenceExpression()) {
            AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();
            rightVariable.typeOnLeft(typeBounds, MAYBE_BYTE_TYPE);
        }

        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(leftExpression.getType(), rightExpression);
        typeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(rightExpression.getType(), leftExpression);

        return new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, leftExpression, operator, rightExpression, priority);
    }

    private Expression newPreArithmeticOperatorExpression(int lineNumber, String operator, Expression expression) {
        reduceIntegerLocalVariableType(expression);
        return new PreOperatorExpression(lineNumber, operator, expression);
    }

    private Expression newPostArithmeticOperatorExpression(int lineNumber, Expression expression, String operator) {
        reduceIntegerLocalVariableType(expression);
        return new PostOperatorExpression(lineNumber, expression, operator);
    }

    private void reduceIntegerLocalVariableType(Expression expression) {
        if (expression.isLocalVariableReferenceExpression()) {
            ClassFileLocalVariableReferenceExpression lvre = (ClassFileLocalVariableReferenceExpression)expression;

            if (lvre.getLocalVariable().isPrimitiveLocalVariable()) {
                AbstractLocalVariable plv = lvre.getLocalVariable();
                if (plv.isAssignableFrom(typeBounds, MAYBE_BOOLEAN_TYPE)) {
                    plv.typeOnRight(typeBounds, MAYBE_BYTE_TYPE);
                }
            }
        }
    }

    /**
     * @return expression, 'this' or 'super'
     */
    private Expression getFieldInstanceReference(Expression expression, ObjectType ot, String name) {
        if (bodyDeclaration.getFieldDeclarations() != null && expression.isThisExpression()) {
            String internalName = expression.getType().getInternalName();

            if (!ot.getInternalName().equals(internalName)) {
                memberVisitor.init(name, null);
                for (ClassFileFieldDeclaration field : bodyDeclaration.getFieldDeclarations()) {
                    field.getFieldDeclarators().accept(memberVisitor);
                    if (memberVisitor.found()) {
                        return new SuperExpression(expression.getLineNumber(), expression.getType());
                    }
                }
            }
        }

        return expression;
    }

    /**
     * @return expression, 'this' or 'super'
     */
    private Expression getMethodInstanceReference(Expression expression, ObjectType ot, String name, String descriptor) {
        if (bodyDeclaration.getMethodDeclarations() != null && expression.isThisExpression()) {
            String internalName = expression.getType().getInternalName();

            if (!ot.getInternalName().equals(internalName)) {
                memberVisitor.init(name, descriptor);

                for (ClassFileConstructorOrMethodDeclaration member : bodyDeclaration.getMethodDeclarations()) {
                    member.accept(memberVisitor);
                    if (memberVisitor.found()) {
                        String[] interfaceTypeNames = member.getClassFile().getInterfaceTypeNames();
                        if (interfaceTypeNames != null && Arrays.asList(interfaceTypeNames).contains(ot.getInternalName())) {
                            return new QualifiedSuperExpression(expression.getLineNumber(), ot);
                        }
                        return new SuperExpression(expression.getLineNumber(), expression.getType());
                    }
                }
            }
        }

        return expression;
    }

    private static void checkStack(DefaultStack<Expression> stack, byte[] code, int offset) {
        if (stack.size() > 1 && offset < code.length) {
            int opcode = code[offset+1] & 255;

            if (opcode == ARETURN || opcode == POP) {
                // Duplicate last expression
                Expression condition = stack.pop();
                stack.push(stack.peek());
                stack.push(condition);
            }
        }
    }

    public static boolean isAssertCondition(String internalTypeName, BasicBlock basicBlock) {
        ControlFlowGraph cfg = basicBlock.getControlFlowGraph();
        int offset = basicBlock.getFromOffset();
        int toOffset = basicBlock.getToOffset();

        if (offset + 3 > toOffset) {
            return false;
        }

        Method method = cfg.getMethod();
        byte[] code = method.<AttributeCode>getAttribute("Code").getCode();
        int opcode = code[offset] & 255;

        if (opcode != GETSTATIC) {
            return false;
        }

        ConstantPool constants = method.getConstants();
        ConstantMemberRef constantMemberRef = constants.getConstant( (code[++offset] & 255) << 8 | code[++offset] & 255 );
        ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());

        if (! "$assertionsDisabled".equals(name)) {
            return false;
        }

        String descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());

        if (! "Z".equals(descriptor)) {
            return false;
        }

        String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());

        return internalTypeName.equals(typeName);
    }

    public static int getExceptionLocalVariableIndex(BasicBlock basicBlock) {
        ControlFlowGraph cfg = basicBlock.getControlFlowGraph();
        int offset = basicBlock.getFromOffset();
        int toOffset = basicBlock.getToOffset();

        if (offset + 1 > toOffset) {
            throw new IllegalStateException();
        }

        Method method = cfg.getMethod();
        byte[] code = method.<AttributeCode>getAttribute("Code").getCode();
        int opcode = code[offset] & 255;

        return switch (opcode) {
            case ASTORE -> code[++offset] & 255;
            case ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3 -> opcode - ASTORE_0;
            case POP, POP2 -> -1;
            default -> throw new IllegalStateException();
        };
    }

    private static class MemberVisitor extends AbstractJavaSyntaxVisitor {
        protected String name;
        protected String descriptor;
        protected boolean found;

        public void init(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
            this.found = false;
        }

        public boolean found() {
            return found;
        }

        @Override
        public void visit(FieldDeclarator declaration) {
            found |= declaration.getName().equals(name);
        }

        @Override
        public void visit(MethodDeclaration declaration) {
            found |= declaration.getName().equals(name) && declaration.getDescriptor().equals(descriptor);
        }
    }

    private static class LambdaParameterNamesVisitor extends AbstractNopDeclarationVisitor {
        protected DefaultList<String> names;

        public void init() {
            names = new DefaultList<>();
        }

        public List<String> getNames() {
            return names;
        }

        @Override
        public void visit(FormalParameter declaration) {
            names.add(declaration.getName());
        }

        @Override
        public void visit(FormalParameters declarations) {
            Iterator<FormalParameter> iterator = declarations.iterator();

            while (iterator.hasNext()) {
                iterator.next().accept(this);
            }
        }
    }

    private static class JsrReturnAddressExpression extends NullExpression {
        public JsrReturnAddressExpression() {
            super(TYPE_VOID);
        }

        @Override
        public String toString() {
            return "JsrReturnAddressExpression{}";
        }
    }

    private Type makeFieldType(String internalTypeName, String fieldName, String descriptor) {
        Type type = typeMaker.makeFieldType(internalTypeName, fieldName, descriptor);

        if (!genericTypesSupported) {
            eraseTypeArgumentVisitor.init();
            type.accept(eraseTypeArgumentVisitor);
            type = (Type)eraseTypeArgumentVisitor.getBaseType();
        }

        return type;
    }

    private TypeMaker.MethodTypes makeMethodTypes(String internalTypeName, String methodName, String descriptor) {
        TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(internalTypeName, methodName, descriptor);

        if (!genericTypesSupported) {
            TypeMaker.MethodTypes mt = new TypeMaker.MethodTypes();

            if (methodTypes.getParameterTypes() != null) {
                eraseTypeArgumentVisitor.init();
                methodTypes.getParameterTypes().accept(eraseTypeArgumentVisitor);
                mt.setParameterTypes(eraseTypeArgumentVisitor.getBaseType());
            }

            eraseTypeArgumentVisitor.init();
            methodTypes.getReturnedType().accept(eraseTypeArgumentVisitor);
            mt.setReturnedType((Type)eraseTypeArgumentVisitor.getBaseType());

            if (methodTypes.getExceptionTypes() != null) {
                eraseTypeArgumentVisitor.init();
                methodTypes.getExceptionTypes().accept(eraseTypeArgumentVisitor);
                mt.setExceptionTypes(eraseTypeArgumentVisitor.getBaseType());
            }

            methodTypes = mt;
        }

        return methodTypes;
    }

    protected static Expression forceExplicitCastExpression(Expression expression) {
        // In case of downcasting, set all cast expression children as explicit to prevent missing castings
        Expression exp = expression;

        CastExpression ce;
        while (exp.isCastExpression()) {
            ce = (CastExpression)exp;
            ce.setExplicit(true);
            exp = ce.getExpression();
        }

        return expression;
    }
}
