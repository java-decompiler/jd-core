/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.ConstantPool;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.AttributeBootstrapMethods;
import org.jd.core.v1.model.classfile.attribute.AttributeCode;
import org.jd.core.v1.model.classfile.attribute.BootstrapMethod;
import org.jd.core.v1.model.classfile.constant.*;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.statement.*;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileCmpExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileMonitorEnterStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileMonitorExitStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.GenericLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.PrimitiveLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchFirstLineNumberVisitor;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.DefaultStack;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.jd.core.v1.model.classfile.Constants.ACC_STATIC;
import static org.jd.core.v1.model.javasyntax.declaration.Declaration.FLAG_PRIVATE;
import static org.jd.core.v1.model.javasyntax.declaration.Declaration.FLAG_SYNTHETIC;
import static org.jd.core.v1.model.javasyntax.statement.ReturnStatement.RETURN;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_UNDEFINED_OBJECT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.*;

public class ByteCodeParser {
    private static final JsrReturnAddressExpression JSR_RETURN_ADDRESS_EXPRESSION = new JsrReturnAddressExpression();

    private ObjectTypeMaker objectTypeMaker;
    private SignatureParser signatureParser;
    private LocalVariableMaker localVariableMaker;
    private String internalTypeName;
    private AttributeBootstrapMethods attributeBootstrapMethods;
    private ClassFileBodyDeclaration bodyDeclaration;
    private Type returnedType;
    private MemberVisitor memberVisitor = new MemberVisitor();
    private SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();

    public ByteCodeParser(
            ObjectTypeMaker objectTypeMaker, SignatureParser signatureParser, LocalVariableMaker localVariableMaker,
            String internalTypeName, ClassFile classFile, ClassFileBodyDeclaration bodyDeclaration, Type returnedType) {
        this.objectTypeMaker = objectTypeMaker;
        this.signatureParser = signatureParser;
        this.localVariableMaker = localVariableMaker;
        this.internalTypeName = internalTypeName;
        this.attributeBootstrapMethods = classFile.getAttribute("BootstrapMethods");
        this.bodyDeclaration = bodyDeclaration;
        this.returnedType = returnedType;
    }

    @SuppressWarnings("unchecked")
    public void parse(BasicBlock basicBlock, Statements<Statement> statements, DefaultStack<Expression> stack) {
        ControlFlowGraph cfg = basicBlock.getControlFlowGraph();
        int fromOffset = basicBlock.getFromOffset();
        int toOffset = basicBlock.getToOffset();

        Method method = cfg.getMethod();
        ConstantPool constants = method.getConstants();
        byte[] code = method.<AttributeCode>getAttribute("Code").getCode();

        Expression indexRef, arrayRef, valueRef, expression1, expression2, expression3;
        Type type1, type2, type3;
        ConstantMemberRef constantMemberRef;
        ConstantNameAndType constantNameAndType;
        String typeName, name, descriptor;
        ObjectType ot;
        Type t;
        int i, count, value;
        AbstractLocalVariable localVariable;

        for (int offset=fromOffset; offset<toOffset; offset++) {
            int opcode = code[offset] & 255;
            int lineNumber = cfg.getLineNumber(offset);

            switch (opcode) {
                case 0: // NOP
                    break;
                case 1: // ACONST_NULL
                    stack.push(new NullExpression(lineNumber, TYPE_UNDEFINED_OBJECT));
                    break;
                case 2: // ICONST_M1
                    stack.push(new IntegerConstantExpression(lineNumber, MAYBE_NEGATIVE_BYTE_TYPE, -1));
                    break;
                case 3: case 4: // ICONST_0, ICONST_1
                    stack.push(new IntegerConstantExpression(lineNumber, MAYBE_BOOLEAN_TYPE, opcode - 3));
                    break;
                case 5: case 6: case 7: case 8: // ICONST_2 ... ICONST_5
                    stack.push(new IntegerConstantExpression(lineNumber, MAYBE_BYTE_TYPE, opcode - 3));
                    break;
                case 9: case 10: // LCONST_0, LCONST_1
                    stack.push(new LongConstantExpression(lineNumber, (long)(opcode - 9)));
                    break;
                case 11: case 12: case 13: // FCONST_0, FCONST_1, FCONST_2
                    stack.push(new FloatConstantExpression(lineNumber, (float)(opcode - 11)));
                    break;
                case 14: case 15: // DCONST_0, DCONST_1
                    stack.push(new DoubleConstantExpression(lineNumber, (double)(opcode - 14)));
                    break;
                case 16: // BIPUSH
                    value = (byte)(code[++offset] & 255);
                    stack.push(new IntegerConstantExpression(lineNumber, PrimitiveTypeUtil.getPrimitiveTypeFromValue(value), value));
                    break;
                case 17: // SIPUSH
                    value = (short)(((code[++offset] & 255) << 8) | (code[++offset] & 255));
                    stack.push(new IntegerConstantExpression(lineNumber, PrimitiveTypeUtil.getPrimitiveTypeFromValue(value), value));
                    break;
                case 18: // LDC
                    parseLDC(stack, constants, lineNumber, constants.getConstant(code[++offset] & 255));
                    break;
                case 19: case 20: // LDC_W, LDC2_W
                    parseLDC(stack, constants, lineNumber, constants.getConstant(((code[++offset] & 255) << 8) | (code[++offset] & 255)));
                    break;
                case 21: // ILOAD
                    localVariable = localVariableMaker.getLocalVariable(code[++offset] & 255, offset);
                    parseILOAD(statements, stack, lineNumber, localVariable);
                    break;
                case 22: case 23: case 24: // LLOAD, FLOAD, DLOAD
                    localVariable = localVariableMaker.getLocalVariable(code[++offset] & 255, offset);
                    stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable));
                    break;
                case 25: // ALOAD
                    i = code[++offset] & 255;
                    localVariable = localVariableMaker.getLocalVariable(i, offset);

                    if ((i == 0) && ((method.getAccessFlags() & ACC_STATIC) == 0)) {
                        stack.push(new ThisExpression(lineNumber, (ObjectType)localVariable.getType()));
                    } else {
                        stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable));
                    }
                    break;
                case 26: case 27: case 28: case 29: // ILOAD_0 ... ILOAD_3
                    localVariable = localVariableMaker.getLocalVariable(opcode - 26, offset);
                    parseILOAD(statements, stack, lineNumber, localVariable);
                    break;
                case 30: case 31: case 32: case 33: // LLOAD_0 ... LLOAD_3
                    localVariable = localVariableMaker.getLocalVariable(opcode - 30, offset);
                    stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable));
                    break;
                case 34: case 35: case 36: case 37: // FLOAD_0 ... FLOAD_3
                    localVariable = localVariableMaker.getLocalVariable(opcode - 34, offset);
                    stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable));
                    break;
                case 38: case 39: case 40: case 41: // DLOAD_0 ... DLOAD_3
                    localVariable = localVariableMaker.getLocalVariable(opcode - 38, offset);
                    stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable));
                    break;
                case 42: // ALOAD_0
                    localVariable = localVariableMaker.getLocalVariable(0, offset);

                    if ((method.getAccessFlags() & ACC_STATIC) == 0) {
                        stack.push(new ThisExpression(lineNumber, (ObjectType)localVariable.getType()));
                    } else {
                        stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable));
                    }
                    break;
                case 43: case 44: case 45: // ALOAD_1 ... ALOAD_3
                    localVariable = localVariableMaker.getLocalVariable(opcode - 42, offset);
                    stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable));
                    break;
                case 46: case 47: case 48: case 49: case 50: case 51: case 52: case 53: // IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    stack.push(new ArrayExpression(lineNumber, arrayRef, indexRef));
                    break;
                case 54: case 55: case 56: case 57: // ISTORE, LSTORE, FSTORE, DSTORE
                    localVariable = getLocalVariableInAssignment(code[++offset] & 255, offset + 2, valueRef = stack.pop());
                    parseSTORE(statements, stack, lineNumber, localVariable, valueRef);
                    break;
                case 58: // ASTORE
                    localVariable = getLocalVariableInAssignment(code[++offset] & 255, offset + 1, valueRef = stack.pop());
                    parseASTORE(statements, stack, lineNumber, localVariable, valueRef);
                    break;
                case 59: case 60: case 61: case 62: // ISTORE_0 ... ISTORE_3
                    localVariable = getLocalVariableInAssignment(opcode - 59, offset + 1, valueRef = stack.pop());
                    parseSTORE(statements, stack, lineNumber, localVariable, valueRef);
                    break;
                case 63: case 64: case 65: case 66: // LSTORE_0 ... LSTORE_3
                    localVariable = getLocalVariableInAssignment(opcode - 63, offset + 1, valueRef = stack.pop());
                    parseSTORE(statements, stack, lineNumber, localVariable, valueRef);
                    break;
                case 67: case 68: case 69: case 70: // FSTORE_0 ... FSTORE_3
                    localVariable = getLocalVariableInAssignment(opcode - 67, offset + 1, valueRef = stack.pop());
                    parseSTORE(statements, stack, lineNumber, localVariable, valueRef);
                    break;
                case 71: case 72: case 73: case 74: // DSTORE_0 ... DSTORE_3
                    localVariable = getLocalVariableInAssignment(opcode - 71, offset + 1, valueRef = stack.pop());
                    parseSTORE(statements, stack, lineNumber, localVariable, valueRef);
                    break;
                case 75: case 76: case 77: case 78: // ASTORE_0 ... ASTORE_3
                    localVariable = getLocalVariableInAssignment(opcode - 75, offset + 1, valueRef = stack.pop());
                    parseASTORE(statements, stack, lineNumber, localVariable, valueRef);
                    break;
                case 79: // IASTORE
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    t = arrayRef.getType();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, t.createType(t.getDimension()-1), new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case 80: // LASTORE
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_LONG, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case 81: // FASTORE
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case 82: // DASTORE
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case 83: // AASTORE
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    t = arrayRef.getType();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, t.createType(t.getDimension()>0 ? t.getDimension()-1 : 0), new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case 84: // BASTORE
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_BYTE, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case 85: // CASTORE
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_CHAR, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case 86: // SASTORE
                    valueRef = stack.pop();
                    indexRef = stack.pop();
                    arrayRef = stack.pop();
                    statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_SHORT, new ArrayExpression(lineNumber, arrayRef, indexRef), "=", valueRef, 16)));
                    break;
                case 87: case 88: // POP, POP2
                    expression1 = stack.pop();
                    Class clazz = expression1.getClass();
                    if ((clazz != ClassFileLocalVariableReferenceExpression.class) && (clazz != FieldReferenceExpression.class)) {
                        statements.add(new ExpressionStatement(expression1));
                    }
                    break;
                case 89: // DUP : ..., value => ..., value, value
                    expression1 = stack.pop();
                    stack.push(expression1);
                    stack.push(expression1);
                    break;
                case 90: // DUP_X1 : ..., value2, value1 => ..., value1, value2, value1
                    expression1 = stack.pop();
                    expression2 = stack.pop();
                    stack.push(expression1);
                    stack.push(expression2);
                    stack.push(expression1);
                    break;
                case 91: // DUP_X2
                    expression1 = stack.pop();
                    expression2 = stack.pop();

                    type2 = expression2.getType();

                    if (TYPE_LONG.equals(type2) || TYPE_DOUBLE.equals(type2)) {
                        // ..., value2, value1 => ..., value1, value2, value1
                        stack.push(expression1);
                        stack.push(expression2);
                        stack.push(expression1);
                    } else {
                        // ..., value3, value2, value1 => ..., value1, value3, value2, value1
                        expression3 = stack.pop();
                        stack.push(expression1);
                        stack.push(expression3);
                        stack.push(expression2);
                        stack.push(expression1);
                    }
                    break;
                case 92: // DUP2
                    expression1 = stack.pop();

                    type1 = expression1.getType();

                    if (TYPE_LONG.equals(type1) || TYPE_DOUBLE.equals(type1)) {
                        // ..., value => ..., value, value
                        stack.push(expression1);
                        stack.push(expression1);
                    } else {
                        // ..., value2, value1 => ..., value2, value1, value2, value1
                        expression2 = stack.pop();
                        stack.push(expression2);
                        stack.push(expression1);
                        stack.push(expression2);
                        stack.push(expression1);
                    }
                    break;
                case 93: // DUP2_X1
                    expression1 = stack.pop();
                    expression2 = stack.pop();

                    type1 = expression1.getType();

                    if (TYPE_LONG.equals(type1) || TYPE_DOUBLE.equals(type1)) {
                        // ..., value2, value1 => ..., value1, value2, value1
                        stack.push(expression1);
                        stack.push(expression2);
                        stack.push(expression1);
                    } else {
                        // ..., value3, value2, value1 => ..., value2, value1, value3, value2, value1
                        expression3 = stack.pop();
                        stack.push(expression2);
                        stack.push(expression1);
                        stack.push(expression3);
                        stack.push(expression2);
                        stack.push(expression1);
                    }
                    break;
                case 94: // DUP2_X2
                    expression1 = stack.pop();
                    expression2 = stack.pop();

                    type1 = expression1.getType();

                    if (TYPE_LONG.equals(type1) || TYPE_DOUBLE.equals(type1)) {
                        type2 = expression2.getType();

                        if (TYPE_LONG.equals(type2) || TYPE_DOUBLE.equals(type2)) {
                            // ..., value2, value1 => ..., value1, value2, value1
                            stack.push(expression1);
                            stack.push(expression2);
                            stack.push(expression1);
                        } else {
                            // ..., value3, value2, value1 => ..., value1, value3, value2, value1
                            expression3 = stack.pop();
                            stack.push(expression1);
                            stack.push(expression3);
                            stack.push(expression2);
                            stack.push(expression1);
                        }
                    } else {
                        expression3 = stack.pop();
                        type3 = expression3.getType();

                        if (TYPE_LONG.equals(type3) || TYPE_DOUBLE.equals(type3)) {
                            // ..., value3, value2, value1 => ..., value2, value1, value3, value2, value1
                            stack.push(expression2);
                            stack.push(expression1);
                            stack.push(expression3);
                            stack.push(expression2);
                            stack.push(expression1);
                        } else {
                            // ..., value4, value3, value2, value1 => ..., value2, value1, value4, value3, value2, value1
                            Expression expression4 = stack.pop();
                            stack.push(expression2);
                            stack.push(expression1);
                            stack.push(expression4);
                            stack.push(expression3);
                            stack.push(expression2);
                            stack.push(expression1);
                        }
                    }
                    break;
                case 95: // SWAP : ..., value2, value1 => ..., value1, value2
                    expression1 = stack.pop();
                    expression2 = stack.pop();
                    stack.push(expression1);
                    stack.push(expression2);
                    break;
                case 96: // IADD
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "+", expression2, 6));
                    break;
                case 97: // LADD
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "+", expression2, 6));
                    break;
                case 98: // FADD
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, expression1, "+", expression2, 6));
                    break;
                case 99: // DADD
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, expression1, "+", expression2, 6));
                    break;
                case 100: // ISUB
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "-", expression2, 6));
                    break;
                case 101: // LSUB
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "-", expression2, 6));
                    break;
                case 102: // FSUB
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, expression1, "-", expression2, 6));
                    break;
                case 103: // DSUB
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, expression1, "-", expression2, 6));
                    break;
                case 104: // IMUL
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "*", expression2, 5));
                    break;
                case 105: // LMUL
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "*", expression2, 5));
                    break;
                case 106: // FMUL
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, expression1, "*", expression2, 5));
                    break;
                case 107: // DMUL
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, expression1, "*", expression2, 5));
                    break;
                case 108: // IDIV
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "/", expression2, 5));
                    break;
                case 109: // LDIV
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "/", expression2, 5));
                    break;
                case 110: // FDIV
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, expression1, "/", expression2, 5));
                    break;
                case 111: // DDIV
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, expression1, "/", expression2, 5));
                    break;
                case 112: // IREM
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "%", expression2, 5));
                    break;
                case 113: // LREM
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "%", expression2, 5));
                    break;
                case 114: // FREM
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, expression1, "%", expression2, 5));
                    break;
                case 115: // DREM
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, expression1, "%", expression2, 5));
                    break;
                case 116: case 117: case 118: case 119: // INEG, LNEG, FNEG, DNEG
                    stack.push(newPreArithmeticOperatorExpression(lineNumber, "-", stack.pop()));
                    break;
                case 120: // ISHL
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, "<<", expression2, 7));
                    break;
                case 121: // LSHL
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "<<", expression2, 7));
                    break;
                case 122: // ISHR
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_INT, expression1, ">>", expression2, 7));
                    break;
                case 123: // LSHR
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, ">>", expression2, 7));
                    break;
                case 124: // IUSHR
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerBinaryOperatorExpression(lineNumber, expression1, ">>>", expression2, 7));
                    break;
                case 125: // LUSHR
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, ">>>", expression2, 7));
                    break;
                case 126: // IAND
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerOrBooleanBinaryOperatorExpression(lineNumber, expression1, "&", expression2, 10));
                    break;
                case 127: // LAND
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "&", expression2, 10));
                    break;
                case 128: // IOR
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerOrBooleanBinaryOperatorExpression(lineNumber, expression1, "|", expression2, 12));
                    break;
                case 129: // LOR
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "|", expression2, 12));
                    break;
                case 130: // IXOR
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerOrBooleanBinaryOperatorExpression(lineNumber, expression1, "^", expression2, 11));
                    break;
                case 131: // LXOR
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_LONG, expression1, "^", expression2, 11));
                    break;
                case 132: // IINC
                    localVariable = localVariableMaker.getLocalVariable(code[++offset] & 255, offset);
                    parseIINC(statements, stack, lineNumber, localVariable, (byte)(code[++offset] & 255));
                    break;
                case 133: // I2L
                    stack.push(new CastExpression(lineNumber, TYPE_LONG, stack.pop(), false));
                    break;
                case 134: // I2F
                    stack.push(new CastExpression(lineNumber, TYPE_FLOAT, stack.pop(), false));
                    break;
                case 135: // I2D
                    stack.push(new CastExpression(lineNumber, TYPE_DOUBLE, stack.pop(), false));
                    break;
                case 136: // L2I
                    stack.push(new CastExpression(lineNumber, TYPE_INT, stack.pop()));
                    break;
                case 137: // L2F
                    stack.push(new CastExpression(lineNumber, TYPE_FLOAT, stack.pop()));
                    break;
                case 138: // L2D
                    stack.push(new CastExpression(lineNumber, TYPE_DOUBLE, stack.pop(), false));
                    break;
                case 139: // F2I
                    stack.push(new CastExpression(lineNumber, TYPE_INT, stack.pop()));
                    break;
                case 140: // F2L
                    stack.push(new CastExpression(lineNumber, TYPE_LONG, stack.pop()));
                    break;
                case 141: // F2D
                    stack.push(new CastExpression(lineNumber, TYPE_DOUBLE, stack.pop(), false));
                    break;
                case 142: // D2I
                    stack.push(new CastExpression(lineNumber, TYPE_INT, stack.pop()));
                    break;
                case 143: // D2L
                    stack.push(new CastExpression(lineNumber, TYPE_LONG, stack.pop()));
                    break;
                case 144: // D2F
                    stack.push(new CastExpression(lineNumber, TYPE_FLOAT, stack.pop()));
                    break;
                case 145: // I2B
                    stack.push(new CastExpression(lineNumber, TYPE_BYTE, stack.pop()));
                    break;
                case 146: // I2C
                    stack.push(new CastExpression(lineNumber, TYPE_CHAR, stack.pop()));
                    break;
                case 147: // I2S
                    stack.push(new CastExpression(lineNumber, TYPE_SHORT, stack.pop()));
                    break;
                case 148: case 149: case 150: case 151: case 152: // LCMP, FCMPL, FCMPG, DCMPL, DCMPG
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(new ClassFileCmpExpression(lineNumber, expression1, expression2));
                    break;
                case 153: // IFEQ
                    parseIF(stack, lineNumber, basicBlock, "!=", "==", 8);
                    offset += 2; // Skip branch offset
                    break;
                case 154: // IFNE
                    parseIF(stack, lineNumber, basicBlock, "==", "!=", 8);
                    offset += 2; // Skip branch offset
                    break;
                case 155: // IFLT
                    parseIF(stack, lineNumber, basicBlock, ">=", "<", 7);
                    offset += 2; // Skip branch offset
                    break;
                case 156: // IFGE
                    parseIF(stack, lineNumber, basicBlock, "<", ">=", 7);
                    offset += 2; // Skip branch offset
                    break;
                case 157: // IFGT
                    parseIF(stack, lineNumber, basicBlock, "<=", ">", 7);
                    offset += 2; // Skip branch offset
                    break;
                case 158: // IFLE
                    parseIF(stack, lineNumber, basicBlock, ">", "<=", 7);
                    offset += 2; // Skip branch offset
                    break;
                case 159: // IF_ICMPEQ
                case 165: // IF_ACMPEQ
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerOrBooleanComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? "!=" : "==", expression2, 9));
                    offset += 2; // Skip branch offset
                    break;
                case 160: // IF_ICMPNE
                case 166: // IF_ACMPNE
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerOrBooleanComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? "==" : "!=", expression2, 9));
                    offset += 2; // Skip branch offset
                    break;
                case 161: // IF_ICMPLT
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? ">=" : "<", expression2, 8));
                    offset += 2; // Skip branch offset
                    break;
                case 162: // IF_ICMPGE
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? "<" : ">=", expression2, 8));
                    offset += 2; // Skip branch offset
                    break;
                case 163: // IF_ICMPGT
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? "<=" : ">", expression2, 8));
                    offset += 2; // Skip branch offset
                    break;
                case 164: // IF_ICMPLE
                    expression2 = stack.pop();
                    expression1 = stack.pop();
                    stack.push(newIntegerComparisonOperatorExpression(lineNumber, expression1, basicBlock.mustInverseCondition() ? ">" : "<=", expression2, 8));
                    offset += 2; // Skip branch offset
                    break;
                case 168: // JSR
                    stack.push(JSR_RETURN_ADDRESS_EXPRESSION);
                case 167: // GOTO
                    offset += 2; // Skip branch offset
                    break;
                case 169: // RET
                    offset++; // Skip index
                    break;
                case 170: // TABLESWITCH
                    offset = (offset+4) & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int low = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8 ) |  (code[offset++] & 255);
                    int high = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8 ) |  (code[offset++] & 255);

                    offset += (4 * (high - low + 1)) - 1;

                    statements.add(new SwitchStatement(stack.pop(), new DefaultList<>(high - low + 2)));
                    break;
                case 171: // LOOKUPSWITCH
                    offset = (offset+4) & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    count = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8 ) |  (code[offset++] & 255);

                    offset += (8 * count) - 1;

                    statements.add(new SwitchStatement(stack.pop(), new DefaultList<>(count+1)));
                    break;
                case 172: case 173: case 174: case 175: case 176: // IRETURN, LRETURN, FRETURN, DRETURN, ARETURN
                    parseXRETURN(statements, stack, lineNumber);
                    break;
                case 177: // RETURN
                    statements.add(RETURN);
                    break;
                case 178: // GETSTATIC
                    parseGetStatic(stack, constants, lineNumber, ((code[++offset] & 255) << 8) | (code[++offset] & 255));
                    break;
                case 179: // PUTSTATIC
                    parsePutStatic(statements, stack, constants, lineNumber, ((code[++offset] & 255) << 8) | (code[++offset] & 255));
                    break;
                case 180: // GETFIELD
                    parseGetField(stack, constants, lineNumber, ((code[++offset] & 255) << 8) | (code[++offset] & 255));
                    break;
                case 181: // PUTFIELD
                    parsePutField(statements, stack, constants, lineNumber, ((code[++offset] & 255) << 8) | (code[++offset] & 255));
                    break;
                case 182: case 183: case 184: case 185: // INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE
                    constantMemberRef = constants.getConstant( ((code[++offset] & 255) << 8) | (code[++offset] & 255) );
                    typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
                    ot = (typeName.charAt(0) == '[') ?
                        objectTypeMaker.makeFromDescriptor(typeName) :
                        objectTypeMaker.makeFromInternalTypeName(typeName);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    name = constants.getConstantUtf8(constantNameAndType.getNameIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getDescriptorIndex());
                    BaseExpression parameters = getParameters(statements, stack, descriptor);
                    Type returnedType = signatureParser.parseReturnedType(descriptor);

                    if (opcode == 184) { // INVOKESTATIC
                        expression1 = new MethodInvocationExpression(lineNumber, returnedType, new ObjectTypeReferenceExpression(lineNumber, ot), typeName, name, descriptor, parameters);
                        if (TYPE_VOID.equals(returnedType)) {
                            statements.add(new ExpressionStatement(expression1));
                        } else {
                            stack.push(expression1);
                        }
                    } else {
                        expression1 = stack.pop();
                        if (expression1.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                            ((ClassFileLocalVariableReferenceExpression)expression1).getLocalVariable().typeOnLeft(ot);
                        }
                        if (opcode == 185) { // INVOKEINTERFACE
                            offset += 2; // Skip 'count' and one byte
                        }
                        if (TYPE_VOID.equals(returnedType)) {
                            if ((opcode == 183) && // INVOKESPECIAL
                                "<init>".equals(name)) {

                                if (expression1.getClass() == NewExpression.class) {
                                    ((NewExpression)expression1).setDescriptorAndParameters(descriptor, parameters);
                                } else if (expression1.getType().equals(ot)) {
                                    statements.add(new ExpressionStatement(new ConstructorInvocationExpression(lineNumber, ot, descriptor, parameters)));
                                } else {
                                    statements.add(new ExpressionStatement(new SuperConstructorInvocationExpression(lineNumber, ot, descriptor, parameters)));
                                }
                            } else {
                                statements.add(new ExpressionStatement(new MethodInvocationExpression(lineNumber, returnedType, getMethodInstanceReference(expression1, ot,  name, descriptor), typeName, name, descriptor, parameters)));
                            }
                        } else {
                            if ((opcode == 182) && // INVOKEVIRTUAL
                                "toString".equals(name) && "()Ljava/lang/String;".equals(descriptor)) {
                                typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
                                if ("java/lang/StringBuilder".equals(typeName) || "java/lang/StringBuffer".equals(typeName)) {
                                    stack.push(StringConcatenationUtil.create(expression1, lineNumber, typeName));
                                    break;
                                }
                            }
                            stack.push(new MethodInvocationExpression(lineNumber, returnedType, getMethodInstanceReference(expression1, ot,  name, descriptor), typeName, name, descriptor, parameters));
                        }
                    }
                    break;
                case 186: // INVOKEDYNAMIC
                    parseInvokeDynamic(statements, stack, constants, lineNumber,  ((code[++offset] & 255) << 8) | (code[++offset] & 255));
                    offset += 2; // Skip 2 bytes
                    break;
                case 187: // NEW
                    typeName = constants.getConstantTypeName( ((code[++offset] & 255) << 8) | (code[++offset] & 255) );
                    stack.push(newNewExpression(lineNumber, typeName));
                    break;
                case 188: // NEWARRAY
                    type1 = PrimitiveTypeUtil.getPrimitiveTypeFromTag( (code[++offset] & 255) ).createType(1);
                    stack.push(new NewArray(lineNumber, type1, stack.pop()));
                    break;
                case 189: // ANEWARRAY
                    typeName = constants.getConstantTypeName( ((code[++offset] & 255) << 8) | (code[++offset] & 255) );
                    if (typeName.charAt(0) == '[') {
                        type1 = objectTypeMaker.makeFromDescriptor(typeName);
                        type1 = type1.createType(type1.getDimension()+1);
                    } else {
                        type1 = objectTypeMaker.makeFromInternalTypeName(typeName).createType(1);
                    }
                    stack.push(new NewArray(lineNumber, type1, stack.pop()));
                    break;
                case 190: // ARRAYLENGTH
                    stack.push(new LengthExpression(lineNumber, stack.pop()));
                    break;
                case 191: // ATHROW
                    statements.add(new ThrowStatement(stack.pop()));
                    break;
                case 192: // CHECKCAST
                    typeName = constants.getConstantTypeName( ((code[++offset] & 255) << 8) | (code[++offset] & 255) );
                    t = (typeName.charAt(0) == '[') ?
                            objectTypeMaker.makeFromDescriptor(typeName) :
                            objectTypeMaker.makeFromInternalTypeName(typeName);
                    expression1 = stack.pop();
                    if (expression1.getClass() == CastExpression.class) {
                        // Skip double cast
                        ((CastExpression)expression1).setType(t);
                    } else {
                        searchFirstLineNumberVisitor.init();
                        expression1.accept(searchFirstLineNumberVisitor);
                        expression1 = new CastExpression(searchFirstLineNumberVisitor.getLineNumber(), t, expression1);
                    }
                    stack.push(expression1);
                    break;
                case 193: // INSTANCEOF
                    typeName = constants.getConstantTypeName( ((code[++offset] & 255) << 8) | (code[++offset] & 255) );
                    t = (typeName.charAt(0) == '[') ?
                            objectTypeMaker.makeFromDescriptor(typeName) :
                            objectTypeMaker.makeFromInternalTypeName(typeName);
                    if (t == null) {
                        t = PrimitiveTypeUtil.getPrimitiveTypeFromDescriptor(typeName);
                    }
                    stack.push(new InstanceOfExpression(lineNumber, stack.pop(), t));
                    break;
                case 194: // MONITORENTER
                    statements.add(new ClassFileMonitorEnterStatement(stack.pop()));
                    break;
                case 195: // MONITOREXIT
                    statements.add(new ClassFileMonitorExitStatement(stack.pop()));
                    break;
                case 196: // WIDE
                    opcode = code[++offset] & 255;
                    i = ((code[++offset] & 255) << 8) | (code[++offset] & 255);

                    if (opcode == 132) { // IINC
                        count = (short)( ((code[++offset] & 255) << 8) | (code[++offset] & 255) );
                        parseIINC(statements, stack, lineNumber, localVariableMaker.getLocalVariable(i, offset), count);
                    } else {
                        switch (opcode) {
                            case 21: // ILOAD
                                localVariable = localVariableMaker.getLocalVariable(i, offset + 4);
                                parseILOAD(statements, stack, lineNumber, localVariable);
                                break;
                            case 22: case 23: case 24: case 25: // LLOAD, FLOAD, DLOAD, ALOAD
                                stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, localVariableMaker.getLocalVariable(i, offset)));
                                break;
                            case 54: // ISTORE
                                localVariable = getLocalVariableInAssignment(i, offset + 4, valueRef = stack.pop());
                                statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, localVariable.getType(), new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable), "=", valueRef, 16)));
                                break;
                            case 55: // LSTORE
                                localVariable = getLocalVariableInAssignment(i, offset + 4, valueRef = stack.pop());
                                statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_LONG, new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable), "=", valueRef, 16)));
                                break;
                            case 56: // FSTORE
                                localVariable = getLocalVariableInAssignment(i, offset + 4, valueRef = stack.pop());
                                statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_FLOAT, new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable), "=", valueRef, 16)));
                                break;
                            case 57: // DSTORE
                                localVariable = getLocalVariableInAssignment(i, offset + 4, valueRef = stack.pop());
                                statements.add(new ExpressionStatement(new BinaryOperatorExpression(lineNumber, TYPE_DOUBLE, new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable), "=", valueRef, 16)));
                                break;
                            case 58: // ASTORE
                                localVariable = getLocalVariableInAssignment(i, offset + 4, valueRef = stack.pop());
                                parseASTORE(statements, stack, lineNumber, localVariable, valueRef);
                                break;
                            case 169: // RET
                                break;
                        }
                    }
                    break;
                case 197: // MULTIANEWARRAY
                    typeName = constants.getConstantTypeName( ((code[++offset] & 255) << 8) | (code[++offset] & 255) );
                    type1 = objectTypeMaker.makeFromDescriptor(typeName);
                    i = code[++offset] & 255;

                    Expressions dimensions = new Expressions(i);

                    while (i-- > 0) {
                        dimensions.add(stack.pop());
                    }

                    Collections.reverse(dimensions);
                    stack.push(new NewArray(lineNumber, type1, dimensions));
                    break;
                case 198: // IFNULL
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression1, basicBlock.mustInverseCondition() ? "!=" : "==", new NullExpression(expression1.getLineNumber(), expression1.getType()), 9));
                    offset += 2; // Skip branch offset
                    checkStack(stack, code, offset);
                    break;
                case 199: // IFNONNULL
                    expression1 = stack.pop();
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression1, basicBlock.mustInverseCondition() ? "==" : "!=", new NullExpression(expression1.getLineNumber(), expression1.getType()), 9));
                    offset += 2; // Skip branch offset
                    checkStack(stack, code, offset);
                    break;
                case 201: // JSR_W
                    stack.push(JSR_RETURN_ADDRESS_EXPRESSION);
                case 200: // GOTO_W
                    offset += 4; // Skip branch offset
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private BaseExpression getParameters(Statements statements, DefaultStack<Expression> stack, String descriptor) {
        List<Type> types = signatureParser.parseParameterTypes(descriptor);

        switch (types.size()) {
            case 0:
                return null;
            case 1:
                Expression parameter = stack.pop();
                if (parameter.getClass() == NewArray.class) {
                    parameter = NewArrayMaker.make(statements, (NewArray)parameter);
                }
                parameter = checkIfLastStatementIsAMultiAssignment(statements, parameter);
                if (parameter.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                    ((ClassFileLocalVariableReferenceExpression)parameter).getLocalVariable().typeOnLeft(types.get(0));
                }
                return parameter;
            default:
                Expressions parameters = new Expressions(types.size());
                int count = types.size() - 1;

                for (int i=count; i>=0; --i) {
                    parameter = stack.pop();
                    if (parameter.getClass() == NewArray.class) {
                        parameter = NewArrayMaker.make(statements, (NewArray)parameter);
                    }
                    parameter = checkIfLastStatementIsAMultiAssignment(statements, parameter);
                    if (parameter.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                        ((ClassFileLocalVariableReferenceExpression)parameter).getLocalVariable().typeOnLeft(types.get(i));
                    }
                    parameters.add(parameter);
                }

                Collections.reverse(parameters);
                return parameters;
        }
    }

    private static Expression checkIfLastStatementIsAMultiAssignment(Statements<Statement> statements, Expression parameter) {
        if (!statements.isEmpty()) {
            Statement lastStatement = statements.getLast();

            if (lastStatement.getClass() == ExpressionStatement.class) {
                Expression expression = ((ExpressionStatement) lastStatement).getExpression();

                if (expression.getClass() == BinaryOperatorExpression.class) {
                    BinaryOperatorExpression boe = (BinaryOperatorExpression) expression;

                    if (getLastRightExpression(boe) == parameter) {
                        // Return multi assignment expression
                        statements.removeLast();
                        return expression;
                    }
                }
            }
        }

        return parameter;
    }

    private AbstractLocalVariable getLocalVariableInAssignment(int index, int offset, Expression value) {
        Class clazz = value.getClass();

        if (clazz == NullExpression.class) {
            return localVariableMaker.getLocalVariableInNullAssignment(index, offset, value.getType());
        } else if (clazz == ClassFileLocalVariableReferenceExpression.class) {
            return localVariableMaker.getLocalVariableInAssignment(index, offset, ((ClassFileLocalVariableReferenceExpression)value).getLocalVariable());
        } else {
            return localVariableMaker.getLocalVariableInAssignment(index, offset, value.getType());
        }
    }

    private void parseLDC(DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, Constant constant) {
        switch (constant.getTag()) {
            case Constant.CONSTANT_Integer:
                int i = ((ConstantInteger)constant).getValue();
                stack.push(new IntegerConstantExpression(lineNumber, PrimitiveTypeUtil.getPrimitiveTypeFromValue(i), i));
                break;
            case Constant.CONSTANT_Float:
                float f = ((ConstantFloat)constant).getValue();

                if (f == Float.MIN_VALUE) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_FLOAT, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_FLOAT), "java/lang/Float", "MIN_VALUE", "F"));
                } else if (f == Float.MAX_VALUE) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_FLOAT, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_FLOAT), "java/lang/Float", "MAX_VALUE", "F"));
                } else if (f == Float.NEGATIVE_INFINITY) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_FLOAT, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_FLOAT), "java/lang/Float", "NEGATIVE_INFINITY", "F"));
                } else if (f == Float.POSITIVE_INFINITY) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_FLOAT, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_FLOAT), "java/lang/Float", "POSITIVE_INFINITY", "F"));
                } else if (f == Float.NaN) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_FLOAT, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_FLOAT), "java/lang/Float", "NaN", "F"));
                } else {
                    stack.push(new FloatConstantExpression(lineNumber, f));
                }
                break;
            case Constant.CONSTANT_Class:
                int typeNameIndex = ((ConstantClass) constant).getNameIndex();
                String typeName = ((ConstantUtf8)constants.getConstant(typeNameIndex)).getValue();
                Type type = (typeName.charAt(0) == '[') ?
                        objectTypeMaker.makeFromDescriptor(typeName) :
                        objectTypeMaker.makeFromInternalTypeName(typeName);
                if (type == null) {
                    type = PrimitiveTypeUtil.getPrimitiveTypeFromDescriptor(typeName);
                }
                stack.push(new TypeReferenceDotClassExpression(lineNumber, type));
                break;
            case Constant.CONSTANT_Long:
                long l = ((ConstantLong)constant).getValue();

                if (l == Long.MIN_VALUE) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_LONG, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_LONG), "java/lang/Long", "MIN_VALUE", "J"));
                } else if (l == Long.MAX_VALUE) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_LONG, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_LONG), "java/lang/Long", "MAX_VALUE", "J"));
                } else {
                    stack.push(new LongConstantExpression(lineNumber, l));
                }
                break;
            case Constant.CONSTANT_Double:
                double d = ((ConstantDouble)constant).getValue();

                if (d == Double.MIN_VALUE) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_DOUBLE), "java/lang/Double", "MIN_VALUE", "D"));
                } else if (d == Double.MAX_VALUE) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_DOUBLE), "java/lang/Double", "MAX_VALUE", "D"));
                } else if (d == Double.NEGATIVE_INFINITY) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_DOUBLE), "java/lang/Double", "NEGATIVE_INFINITY", "D"));
                } else if (d == Double.POSITIVE_INFINITY) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_DOUBLE), "java/lang/Double", "POSITIVE_INFINITY", "D"));
                } else if (d == Double.NaN) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_DOUBLE), "java/lang/Double", "NaN", "D"));
                } else if (d == Math.E) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_MATH), "java/lang/Math", "E", "D"));
                } else if (d == Math.PI) {
                    stack.push(new FieldReferenceExpression(lineNumber, TYPE_DOUBLE, new ObjectTypeReferenceExpression(lineNumber, ObjectType.TYPE_MATH), "java/lang/Math", "PI", "D"));
                } else {
                    stack.push(new DoubleConstantExpression(lineNumber, d));
                }
                break;
            case Constant.CONSTANT_String:
                int stringIndex = ((ConstantString)constant).getStringIndex();
                stack.push(new StringConstantExpression(lineNumber, constants.getConstantUtf8(stringIndex)));
                break;
        }
    }

    private static void parseILOAD(Statements<Statement> statements, DefaultStack<Expression> stack, int lineNumber, AbstractLocalVariable localVariable) {
        if (! statements.isEmpty()) {
            Statement statement = statements.getLast();

            if (statement.getClass() == ExpressionStatement.class) {
                Expression expression = ((ExpressionStatement)statement).getExpression();

                if ((expression.getLineNumber() == lineNumber) && (expression.getClass() == PreOperatorExpression.class)) {
                    PreOperatorExpression poe = (PreOperatorExpression)expression;

                    if (poe.getExpression().getClass() == ClassFileLocalVariableReferenceExpression.class) {
                        ClassFileLocalVariableReferenceExpression cflvre = (ClassFileLocalVariableReferenceExpression)poe.getExpression();

                        if (cflvre.getLocalVariable() == localVariable) {
                            // IINC pattern found -> Remove last statement and create a pre-incrementation
                            statements.removeLast();
                            stack.push(poe);
                            return;
                        }
                    }
                }
            }
        }

        stack.push(new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable));
    }

    @SuppressWarnings("unchecked")
    private static void parseSTORE(Statements statements, DefaultStack<Expression> stack, int lineNumber, AbstractLocalVariable localVariable, Expression valueRef) {
        ClassFileLocalVariableReferenceExpression vre = new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable);

        if ((valueRef.getLineNumber() == lineNumber) && (valueRef.getClass() == BinaryOperatorExpression.class)) {
            BinaryOperatorExpression boe = (BinaryOperatorExpression)valueRef;

            if (boe.getLeftExpression().getClass() == ClassFileLocalVariableReferenceExpression.class) {
                ClassFileLocalVariableReferenceExpression lvr = (ClassFileLocalVariableReferenceExpression)boe.getLeftExpression();

                if (lvr.getLocalVariable() == localVariable) {
                    Expression expression;

                    switch (boe.getOperator()) {
                        case "*": expression = createAssignment(boe, "*="); break;
                        case "/": expression = createAssignment(boe, "/="); break;
                        case "%": expression = createAssignment(boe, "%="); break;
                        case "<<": expression = createAssignment(boe, "<<="); break;
                        case ">>": expression = createAssignment(boe, ">>="); break;
                        case ">>>": expression = createAssignment(boe, ">>>="); break;
                        case "&": expression = createAssignment(boe, "&="); break;
                        case "^": expression = createAssignment(boe, "^="); break;
                        case "|": expression = createAssignment(boe, "|="); break;
                        case "=": expression = boe; break;
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
                        default: throw new RuntimeException("Unexpected value expression");
                    }

                    if (!stack.isEmpty() && (stack.peek() == valueRef)) {
                        stack.replace(valueRef, expression);
                    } else {
                        statements.add(new ExpressionStatement(expression));
                    }
                    return;
                }
            }
        }

        createAssignment(statements, stack, lineNumber, vre, valueRef);
    }

    private static boolean stackContainsLocalVariableReference(DefaultStack<Expression> stack, AbstractLocalVariable localVariable) {
        if (stack.isEmpty())
            return false;

        Expression expression = stack.peek();

        if (expression.getClass() != ClassFileLocalVariableReferenceExpression.class)
            return false;

        ClassFileLocalVariableReferenceExpression lvr = (ClassFileLocalVariableReferenceExpression)expression;

        return lvr.getLocalVariable() == localVariable;
    }

    @SuppressWarnings("unchecked")
    private static void parsePUT(Statements statements, DefaultStack<Expression> stack, int lineNumber, FieldReferenceExpression fr, Expression valueRef) {
        if (valueRef.getClass() == NewArray.class) {
            valueRef = NewArrayMaker.make(statements, (NewArray)valueRef);
        }

        if ((valueRef.getLineNumber() == lineNumber) && (valueRef.getClass() == BinaryOperatorExpression.class)) {
            BinaryOperatorExpression boe = (BinaryOperatorExpression)valueRef;

            if (boe.getLeftExpression().getClass() == FieldReferenceExpression.class) {
                FieldReferenceExpression boefr = (FieldReferenceExpression)boe.getLeftExpression();

                if (boefr.getName().equals(fr.getName()) && boefr.getExpression().getType().equals(fr.getExpression().getType())) {
                    Expression expression;

                    switch (boe.getOperator()) {
                        case "*": expression = createAssignment(boe, "*="); break;
                        case "/": expression = createAssignment(boe, "/="); break;
                        case "%": expression = createAssignment(boe, "%="); break;
                        case "<<": expression = createAssignment(boe, "<<="); break;
                        case ">>": expression = createAssignment(boe, ">>="); break;
                        case ">>>": expression = createAssignment(boe, ">>>="); break;
                        case "&": expression = createAssignment(boe, "&="); break;
                        case "^": expression = createAssignment(boe, "^="); break;
                        case "|": expression = createAssignment(boe, "|="); break;
                        case "=": expression = boe; break;
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
                            } else if (isPositiveOne(boe.getRightExpression())) {
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
                        default: throw new RuntimeException("Unexpected value expression");
                    }

                    if (!stack.isEmpty() && (stack.peek() == valueRef)) {
                        stack.replace(valueRef, expression);
                    } else {
                        statements.add(new ExpressionStatement(expression));
                    }
                    return;
                }
            }
        }

        createAssignment(statements, stack, lineNumber, fr, valueRef);
    }

    private void parseInvokeDynamic(Statements<Statement> statements, DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, int index) {
        // Remove previous 'getClass()' or cast if exists
        if (! statements.isEmpty()) {
            Statement last = statements.getLast();

            if (last.getClass() == ExpressionStatement.class) {
                Expression expression = ((ExpressionStatement)last).getExpression();

                if (expression.getClass() == MethodInvocationExpression.class) {
                    MethodInvocationExpression mie = (MethodInvocationExpression)expression;

                    if (mie.getName().equals("getClass") && mie.getDescriptor().equals("()Ljava/lang/Class;") && mie.getInternalTypeName().equals("java/lang/Object")) {
                        statements.removeLast();
                    }
                }
            }
        }

        // Create expression
        ConstantMemberRef constantMemberRef = constants.getConstant(index);

        ConstantNameAndType indyCnat = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String indyMethodName = constants.getConstantUtf8(indyCnat.getNameIndex());
        String indyDescriptor = constants.getConstantUtf8(indyCnat.getDescriptorIndex());
        BaseExpression indyParameters = getParameters(statements, stack, indyDescriptor);
        Type indyType = signatureParser.parseReturnedType(indyDescriptor);

        BootstrapMethod bootstrapMethod = attributeBootstrapMethods.getBootstrapMethods()[constantMemberRef.getClassIndex()];
        int[] bootstrapArguments = bootstrapMethod.getBootstrapArguments();

        if ("makeConcatWithConstants".equals(indyMethodName)) {
            // Create Java 9+ string concatenation
            String recipe = constants.getConstantString(bootstrapArguments[0]);
            stack.push(StringConcatenationUtil.create(recipe, indyParameters));
            return;
        } else if ("makeConcat".equals(indyMethodName)) {
            // Create Java 9+ string concatenation
            stack.push(StringConcatenationUtil.create(indyParameters));
            return;
        }

        ConstantMethodType cmt0 = constants.getConstant(bootstrapArguments[0]);
        String descriptor0 = constants.getConstantUtf8(cmt0.getDescriptorIndex());
        Type returnedType = signatureParser.parseReturnedType(descriptor0);
        int parameterCount = signatureParser.parseParameterTypes(descriptor0).size();
        ConstantMethodHandle constantMethodHandle1 = constants.getConstant(bootstrapArguments[1]);
        ConstantMemberRef cmr1 = constants.getConstant(constantMethodHandle1.getReferenceIndex());
        String typeName = constants.getConstantTypeName(cmr1.getClassIndex());
        ConstantNameAndType cnat1 = constants.getConstant(cmr1.getNameAndTypeIndex());
        String name1 = constants.getConstantUtf8(cnat1.getNameIndex());
        String descriptor1 = constants.getConstantUtf8(cnat1.getDescriptorIndex());

        if (typeName.equals(internalTypeName)) {
            for (ClassFileConstructorOrMethodDeclaration methodDeclaration : bodyDeclaration.getMethodDeclarations()) {
                if (((methodDeclaration.getFlags() & (FLAG_SYNTHETIC|FLAG_PRIVATE)) == (FLAG_SYNTHETIC|FLAG_PRIVATE)) && methodDeclaration.getMethod().getName().equals(name1) && methodDeclaration.getMethod().getDescriptor().equals(descriptor1)) {
                    // Create lambda expression
                    ClassFileMethodDeclaration cfmd = (ClassFileMethodDeclaration)methodDeclaration;
                    stack.push(new LambdaIdentifiersExpression(
                            lineNumber, indyType, returnedType,
                            prepareLambdaParameters(cfmd.getFormalParameters(), parameterCount),
                            prepareLambdaStatements(cfmd.getStatements())));
                    return;
                }
            }
        }

        if (indyParameters == null) {
            // Create static method reference
            ObjectType ot = objectTypeMaker.makeFromInternalTypeName(typeName);

            if (name1.equals("<init>")) {
                stack.push(new ConstructorReferenceExpression(lineNumber, indyType, ot, descriptor1));
            } else {
                stack.push(new MethodReferenceExpression(lineNumber, indyType, new ObjectTypeReferenceExpression(lineNumber, ot), typeName, name1, descriptor1));
            }
            return;
        }

        // Create method reference
        stack.push(new MethodReferenceExpression(lineNumber, indyType, (Expression)indyParameters, typeName, name1, descriptor1));
    }

    private static List<String> prepareLambdaParameters(BaseFormalParameter formalParameters, int parameterCount) {
        if ((formalParameters == null) || (parameterCount == 0)) {
            return null;
        } else {
            LambdaParameterNamesVisitor lambdaParameterNamesVisitor = new LambdaParameterNamesVisitor();
            formalParameters.accept(lambdaParameterNamesVisitor);
            List<String> names = lambdaParameterNamesVisitor.getNames();

            assert names.size() >= parameterCount;

            if (names.size() == parameterCount) {
                return names;
            } else {
                return names.subList(names.size() - parameterCount, names.size());
            }
        }
    }

    private static BaseStatement prepareLambdaStatements(BaseStatement baseStatement) {
        if ((baseStatement != null) && baseStatement.isList()) {
            DefaultList<Statement> statements = baseStatement.getList();

            if (statements.size() == 1) {
                Statement statement = statements.getFirst();

                if (statement.getClass() == ReturnExpressionStatement.class) {
                    return new LambdaExpressionStatement(((ReturnExpressionStatement)statement).getExpression());
                } else if (statement.getClass() ==  ExpressionStatement.class) {
                    return new LambdaExpressionStatement(((ExpressionStatement)statement).getExpression());
                }
            }
        }

        return baseStatement;
    }

    private static boolean stackContainsFieldReference(DefaultStack<Expression> stack, FieldReferenceExpression fr) {
        if (stack.isEmpty())
            return false;

        Expression expression = stack.peek();

        if (expression.getClass() != FieldReferenceExpression.class)
            return false;

        FieldReferenceExpression stackfr = (FieldReferenceExpression)expression;

        return stackfr.getName().equals(fr.getName()) && stackfr.getExpression().getType().equals(fr.getExpression().getType());
    }

    private static Expression createAssignment(BinaryOperatorExpression boe, String operator) {
        boe.setOperator(operator);
        return boe;
    }

    private static boolean isPositiveOne(Expression expression) {
        if ((expression.getClass() == IntegerConstantExpression.class) && ((IntegerConstantExpression)expression).getValue() == 1)
            return true;
        if ((expression.getClass() == LongConstantExpression.class) && ((LongConstantExpression)expression).getValue() == 1L)
            return true;
        if ((expression.getClass() == FloatConstantExpression.class) && ((FloatConstantExpression)expression).getValue() == 1.0F)
            return true;
        return ((expression.getClass() == DoubleConstantExpression.class) && ((DoubleConstantExpression)expression).getValue() == 1.0D);
    }

    private static boolean isNegativeOne(Expression expression) {
        if ((expression.getClass() == IntegerConstantExpression.class) && ((IntegerConstantExpression)expression).getValue() == -1)
            return true;
        if ((expression.getClass() == LongConstantExpression.class) && ((LongConstantExpression)expression).getValue() == -1L)
            return true;
        if ((expression.getClass() == FloatConstantExpression.class) && ((FloatConstantExpression)expression).getValue() == -1.0F)
            return true;
        return ((expression.getClass() == DoubleConstantExpression.class) && ((DoubleConstantExpression)expression).getValue() == -1.0D);
    }

    private static void parseASTORE(Statements<Statement> statements, DefaultStack<Expression> stack, int lineNumber, AbstractLocalVariable localVariable, Expression valueRef) {
        ClassFileLocalVariableReferenceExpression vre = new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable);
        Expression oldValueRef = valueRef;

        if (valueRef.getClass() == NewArray.class) {
            valueRef = NewArrayMaker.make(statements, (NewArray)valueRef);
        }
        if ((localVariable.getClass() == GenericLocalVariable.class) && !localVariable.getType().equals(valueRef.getType())) {
            valueRef = new CastExpression(valueRef.getLineNumber(), localVariable.getType(), valueRef);
        }
        if (oldValueRef != valueRef) {
            stack.replace(oldValueRef, valueRef);
        }

        createAssignment(statements, stack, lineNumber, vre, valueRef);
    }

    @SuppressWarnings("unchecked")
    private static void createAssignment(Statements<Statement> statements, DefaultStack<Expression> stack, int lineNumber, Expression leftExpression, Expression rightExpression) {
        if (!stack.isEmpty() && (stack.peek() == rightExpression)) {
            stack.push(new BinaryOperatorExpression(lineNumber, leftExpression.getType(), leftExpression, "=", stack.pop(), 16));
            return;
        }

        if (!statements.isEmpty()) {
            Statement lastStatement = statements.getLast();

            if (lastStatement.getClass() == ExpressionStatement.class) {
                ExpressionStatement lastES = (ExpressionStatement) lastStatement;
                Expression lastExpression = lastES.getExpression();
                Class lastExpressionClass = lastExpression.getClass();

                if (lastExpressionClass == BinaryOperatorExpression.class) {
                    BinaryOperatorExpression boe = (BinaryOperatorExpression) lastExpression;

                    if (getLastRightExpression(boe) == rightExpression) {
                        // Multi assignment
                        lastES.setExpression(new BinaryOperatorExpression(lineNumber, leftExpression.getType(), leftExpression, "=", boe, 16));
                        return;
                    }

                    if ((lineNumber > 0) && (boe.getLineNumber() == lineNumber)) {
                        Class leftExpressionClass = boe.getLeftExpression().getClass();

                        if (leftExpressionClass == rightExpression.getClass()) {
                            if (leftExpressionClass == ClassFileLocalVariableReferenceExpression.class) {
                                ClassFileLocalVariableReferenceExpression lvr1 = (ClassFileLocalVariableReferenceExpression) boe.getLeftExpression();
                                ClassFileLocalVariableReferenceExpression lvr2 = (ClassFileLocalVariableReferenceExpression) rightExpression;

                                if (lvr1.getLocalVariable() == lvr2.getLocalVariable()) {
                                    // Multi assignment
                                    lastES.setExpression(new BinaryOperatorExpression(lineNumber, leftExpression.getType(), leftExpression, "=", boe, 16));
                                    return;
                                }
                            } else if (leftExpressionClass == FieldReferenceExpression.class) {
                                FieldReferenceExpression fr1 = (FieldReferenceExpression) boe.getLeftExpression();
                                FieldReferenceExpression fr2 = (FieldReferenceExpression) rightExpression;

                                if (fr1.getName().equals(fr2.getName()) && fr1.getExpression().getType().equals(fr2.getExpression().getType())) {
                                    // Multi assignment
                                    lastES.setExpression(new BinaryOperatorExpression(lineNumber, leftExpression.getType(), leftExpression, "=", boe, 16));
                                    return;
                                }
                            }
                        }
                    }
                } else if (lastExpressionClass == PreOperatorExpression.class) {
                    PreOperatorExpression poe = (PreOperatorExpression)lastExpression;
                    Class clazz = poe.getExpression().getClass();

                    if (clazz == rightExpression.getClass()) {
                        if (clazz == ClassFileLocalVariableReferenceExpression.class) {
                            ClassFileLocalVariableReferenceExpression lvr1 = (ClassFileLocalVariableReferenceExpression)poe.getExpression();
                            ClassFileLocalVariableReferenceExpression lvr2 = (ClassFileLocalVariableReferenceExpression)rightExpression;

                            if (lvr1.getLocalVariable() == lvr2.getLocalVariable()) {
                                rightExpression = newPreArithmeticOperatorExpression(poe.getLineNumber(), poe.getOperator(), poe.getExpression());
                                statements.removeLast();
                            }
                        } else if (clazz == FieldReferenceExpression.class) {
                            FieldReferenceExpression fr1 = (FieldReferenceExpression)poe.getExpression();
                            FieldReferenceExpression fr2 = (FieldReferenceExpression)rightExpression;

                            if (fr1.getName().equals(fr2.getName()) && fr1.getExpression().getType().equals(fr2.getExpression().getType())) {
                                rightExpression = newPreArithmeticOperatorExpression(poe.getLineNumber(), poe.getOperator(), poe.getExpression());
                                statements.removeLast();
                            }
                        }
                    }
                } else if (lastExpressionClass == PostOperatorExpression.class) {
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

    @SuppressWarnings("unchecked")
    private static void parseIINC(Statements statements, DefaultStack<Expression> stack, int lineNumber, AbstractLocalVariable localVariable, int count) {
        Expression expression;

        if (!stack.isEmpty()) {
            expression = stack.peek();

            if ((expression.getLineNumber() == lineNumber) && (expression.getClass() == ClassFileLocalVariableReferenceExpression.class)) {
                ClassFileLocalVariableReferenceExpression exp = (ClassFileLocalVariableReferenceExpression)expression;

                if (exp.getLocalVariable() == localVariable) {
                    // ILOAD found -> Create a post-incrementation
                    stack.pop();

                    if (count == 1) {
                        stack.push(newPostArithmeticOperatorExpression(lineNumber, expression, "++"));
                    } else if (count == -1) {
                        stack.push(newPostArithmeticOperatorExpression(lineNumber, expression, "--"));
                    } else {
                        assert false;
                    }

                    return;
                }
            }
        }

        expression = new ClassFileLocalVariableReferenceExpression(lineNumber, localVariable);

        if (count == 1) {
            expression = newPreArithmeticOperatorExpression(lineNumber, "++", expression);
        } else if (count == -1) {
            expression = newPreArithmeticOperatorExpression(lineNumber, "--", expression);
        } else if (count >= 0) {
            expression = new BinaryOperatorExpression(lineNumber, expression.getType(), expression, "+=", new IntegerConstantExpression(lineNumber, expression.getType(), count), 16);
        } else if (count < 0) {
            expression = new BinaryOperatorExpression(lineNumber, expression.getType(), expression, "-=", new IntegerConstantExpression(lineNumber, expression.getType(), -count), 16);
        } else {
            assert false;
            expression = null;
        }

        statements.add(new ExpressionStatement(expression));
    }

    private static void parseIF(DefaultStack<Expression> stack, int lineNumber, BasicBlock basicBlock, String operator1, String operator2, int priority) {
        Expression expression = stack.pop();

        if (expression.getClass() == ClassFileCmpExpression.class) {
            ClassFileCmpExpression cmp = (ClassFileCmpExpression)expression;
            stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, cmp.getLeftExpression(), (basicBlock.mustInverseCondition() ? operator1 : operator2), cmp.getRightExpression(), priority));
        } else if (expression.getType().isPrimitive()) {
            PrimitiveType pt = (PrimitiveType)expression.getType();

            switch (pt.getJavaPrimitiveFlags()) {
                case FLAG_BOOLEAN:
                    if (basicBlock.mustInverseCondition() ^ "==".equals(operator1))
                        stack.push(expression);
                    else
                        stack.push(new PreOperatorExpression(lineNumber, "!", expression));
                    break;
                case FLAG_FLOAT:
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression, (basicBlock.mustInverseCondition() ? operator1 : operator2), new FloatConstantExpression(lineNumber, 0), 9));
                    break;
                case FLAG_DOUBLE:
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression, (basicBlock.mustInverseCondition() ? operator1 : operator2), new DoubleConstantExpression(lineNumber, 0), 9));
                    break;
                case FLAG_LONG:
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression, (basicBlock.mustInverseCondition() ? operator1 : operator2), new LongConstantExpression(lineNumber, 0), 9));
                    break;
                default:
                    stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression, (basicBlock.mustInverseCondition() ? operator1 : operator2), new IntegerConstantExpression(lineNumber, pt, 0), 9));
                    break;
            }
        } else {
            stack.push(new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, expression, (basicBlock.mustInverseCondition() ? operator1 : operator2), new NullExpression(lineNumber, expression.getType()), 9));
        }
    }

    @SuppressWarnings("unchecked")
    private void parseXRETURN(Statements<Statement> statements, DefaultStack<Expression> stack, int lineNumber) {
        Expression valueRef = stack.pop();

        if (valueRef.getClass() == ClassFileLocalVariableReferenceExpression.class) {
            ((ClassFileLocalVariableReferenceExpression)valueRef).getLocalVariable().typeOnLeft(returnedType);
        }

        if (valueRef.getClass() == NewArray.class) {
            valueRef = NewArrayMaker.make(statements, (NewArray)valueRef);
        }

        if (lineNumber > valueRef.getLineNumber()) {
            lineNumber = valueRef.getLineNumber();
        }

        if (!statements.isEmpty() && (valueRef.getClass() == ClassFileLocalVariableReferenceExpression.class)) {
            Statement lastStatement = statements.getLast();

            if (lastStatement.getClass() == ExpressionStatement.class) {
                Expression expression = ((ExpressionStatement)lastStatement).getExpression();

                if ((lineNumber <= expression.getLineNumber()) && (expression.getClass() == BinaryOperatorExpression.class)) {
                    BinaryOperatorExpression boe = (BinaryOperatorExpression)expression;

                    if ((boe.getOperator().equals("=")) && (boe.getLeftExpression().getClass() == ClassFileLocalVariableReferenceExpression.class)) {
                        ClassFileLocalVariableReferenceExpression vre1 = (ClassFileLocalVariableReferenceExpression) boe.getLeftExpression();
                        ClassFileLocalVariableReferenceExpression vre2 = (ClassFileLocalVariableReferenceExpression) valueRef;

                        if (vre1.getLocalVariable() == vre2.getLocalVariable()) {
                            // Remove synthetic local variable
                            localVariableMaker.removeLocalVariable(vre1.getLocalVariable());
                            // Remove assignment statement
                            statements.removeLast();
                            statements.add(new ReturnExpressionStatement(lineNumber, addGenericCastExpression(boe.getRightExpression())));
                            return;
                        }
                    }
                }
            }
        }

        statements.add(new ReturnExpressionStatement(lineNumber, addGenericCastExpression(valueRef)));
    }

    private Expression addGenericCastExpression(Expression expression) {
        if ((returnedType.getClass() == GenericType.class) && (expression.getClass() != NullExpression.class) && !returnedType.equals(expression.getType())) {
            return new CastExpression(returnedType, expression);
        }

        return expression;
    }

    private void parseGetStatic(DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, int index) {
        ConstantMemberRef constantMemberRef = constants.getConstant(index);
        String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
        ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());

        if (name.equals("TYPE") && typeName.startsWith("java/lang/")) {
            switch (typeName) {
                case "java/lang/Boolean":   stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_BOOLEAN)); return;
                case "java/lang/Character": stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_CHAR));    return;
                case "java/lang/Float":     stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_FLOAT));   return;
                case "java/lang/Double":    stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_DOUBLE));  return;
                case "java/lang/Byte":      stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_BYTE));    return;
                case "java/lang/Short":     stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_SHORT));   return;
                case "java/lang/Integer":   stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_INT));     return;
                case "java/lang/Long":      stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_LONG));    return;
                case "java/lang/Void":      stack.push(new TypeReferenceDotClassExpression(lineNumber, TYPE_VOID));    return;
            }
        }

        String descriptor = constants.getConstantUtf8(constantNameAndType.getDescriptorIndex());
        Type type = signatureParser.parseTypeSignature(descriptor);
        ObjectType ot = objectTypeMaker.makeFromInternalTypeName(typeName);
        Expression objectRef = new ObjectTypeReferenceExpression(lineNumber, ot, !internalTypeName.equals(typeName) || localVariableMaker.containsName(name));
        stack.push(new FieldReferenceExpression(lineNumber, type, objectRef, typeName, name, descriptor));
    }

    private void parsePutStatic(Statements statements, DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, int index) {
        ConstantMemberRef constantMemberRef = constants.getConstant(index);
        String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
        ObjectType ot = objectTypeMaker.makeFromInternalTypeName(typeName);
        ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());
        String descriptor = constants.getConstantUtf8(constantNameAndType.getDescriptorIndex());
        Type type = signatureParser.parseTypeSignature(descriptor);
        Expression valueRef = stack.pop();
        Expression objectRef = new ObjectTypeReferenceExpression(lineNumber, ot, !internalTypeName.equals(typeName) || localVariableMaker.containsName(name));
        FieldReferenceExpression fieldRef = new FieldReferenceExpression(lineNumber, type, objectRef, typeName, name, descriptor);
        parsePUT(statements, stack, lineNumber, fieldRef, valueRef);
    }

    private void parseGetField(DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, int index) {
        ConstantMemberRef constantMemberRef = constants.getConstant(index);
        String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
        ObjectType ot = objectTypeMaker.makeFromInternalTypeName(typeName);
        ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());
        String descriptor = constants.getConstantUtf8(constantNameAndType.getDescriptorIndex());
        Type type = signatureParser.parseTypeSignature(descriptor);
        Expression objectRef = stack.pop();
        stack.push(new FieldReferenceExpression(lineNumber, type, getFieldInstanceReference(objectRef, ot,  name), typeName, name, descriptor));
    }

    private void parsePutField(Statements statements, DefaultStack<Expression> stack, ConstantPool constants, int lineNumber, int index) {
        ConstantMemberRef constantMemberRef = constants.getConstant(index);
        String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());
        ObjectType ot = objectTypeMaker.makeFromInternalTypeName(typeName);
        ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());
        String descriptor = constants.getConstantUtf8(constantNameAndType.getDescriptorIndex());
        Type type = signatureParser.parseTypeSignature(descriptor);
        Expression valueRef = stack.pop();
        Expression objectRef = stack.pop();
        FieldReferenceExpression fieldRef = new FieldReferenceExpression(lineNumber, type, getFieldInstanceReference(objectRef, ot,  name), typeName, name, descriptor);
        parsePUT(statements, stack, lineNumber, fieldRef, valueRef);
    }

    private static Expression getLastRightExpression(BinaryOperatorExpression boe) {
        while (true) {
            if (! boe.getOperator().equals("=")) {
                return boe;
            }

            if (boe.getRightExpression().getClass() != BinaryOperatorExpression.class) {
                return boe.getRightExpression();
            }

            boe = (BinaryOperatorExpression)boe.getRightExpression();
        }
    }

    private Expression newNewExpression(int lineNumber, String internalName) {
        ObjectType objectType = objectTypeMaker.makeFromInternalTypeName(internalName);

        if ((objectType.getQualifiedName() == null) && (objectType.getName() == null)) {
            ClassFileMemberDeclaration memberDeclaration = bodyDeclaration.getInnerTypeDeclaration(internalName);

            if (memberDeclaration == null) {
                return new NewExpression(lineNumber, ObjectType.TYPE_OBJECT);
            } else if (memberDeclaration.getClass() == ClassFileClassDeclaration.class) {
                ClassFileClassDeclaration declaration = (ClassFileClassDeclaration) memberDeclaration;

                if (declaration.getInterfaces() != null) {
                    return new NewExpression(lineNumber, (ObjectType) declaration.getInterfaces(), declaration.getBodyDeclaration());
                } else if (declaration.getSuperType() != null) {
                    return new NewExpression(lineNumber, (ObjectType) declaration.getSuperType(), declaration.getBodyDeclaration());
                } else {
                    return new NewExpression(lineNumber, ObjectType.TYPE_OBJECT, declaration.getBodyDeclaration());
                }
            }
        }

        return new NewExpression(lineNumber, objectType);
    }

    /*
     * Operators = { "+", "-", "*", "/", "%", "<<", ">>", ">>>" }
     * See "Additive Operators (+ and -) for Numeric Types": https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.18.2
     * See "Shift Operators":                                https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19
     */
    private static Expression newIntegerBinaryOperatorExpression(int lineNumber, Expression leftExpression, String operator, Expression rightExpression, int priority) {
        Class leftClass = leftExpression.getClass();
        Class rightClass = rightExpression.getClass();

        if (leftClass == ClassFileLocalVariableReferenceExpression.class) {
            AbstractLocalVariable leftVariable = ((ClassFileLocalVariableReferenceExpression)leftExpression).getLocalVariable();

            leftVariable.typeOnLeft(MAYBE_BYTE_TYPE);
        }

        if (rightClass == ClassFileLocalVariableReferenceExpression.class) {
            AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();

            rightVariable.typeOnLeft(MAYBE_BYTE_TYPE);
        }

        return new BinaryOperatorExpression(lineNumber, TYPE_INT, leftExpression, operator, rightExpression, priority);
    }

    /*
     * Operators = { "&", "|", "^" }
     * See "Binary Numeric Promotion": https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.22.1
     */
    private static Expression newIntegerOrBooleanBinaryOperatorExpression(int lineNumber, Expression leftExpression, String operator, Expression rightExpression, int priority) {
        Class leftClass = leftExpression.getClass();
        Class rightClass = rightExpression.getClass();
        Type type = TYPE_INT;

        if (leftClass == ClassFileLocalVariableReferenceExpression.class) {
            AbstractLocalVariable leftVariable = ((ClassFileLocalVariableReferenceExpression)leftExpression).getLocalVariable();

            if (rightClass == ClassFileLocalVariableReferenceExpression.class) {
                AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();

                if (leftVariable.isAssignableFrom(TYPE_BOOLEAN) || rightVariable.isAssignableFrom(TYPE_BOOLEAN)) {
                    leftVariable.variableOnRight(rightVariable);
                    rightVariable.variableOnLeft(leftVariable);

                    if ((leftVariable.getType() == TYPE_BOOLEAN) || (rightVariable.getType() == TYPE_BOOLEAN)) {
                        type = TYPE_BOOLEAN;
                    }
                }
            } else {
                if (rightExpression.getType() == TYPE_BOOLEAN) {
                    leftVariable.typeOnRight(type = TYPE_BOOLEAN);
                }
            }
        } else {
            if (rightClass == ClassFileLocalVariableReferenceExpression.class) {
                if (leftExpression.getType() == TYPE_BOOLEAN) {
                    AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();

                    rightVariable.typeOnRight(type = TYPE_BOOLEAN);
                }
            }
        }

        return new BinaryOperatorExpression(lineNumber, type, leftExpression, operator, rightExpression, priority);
    }

    /*
     * Operators = { "==", "!=" }
     * See "Numerical Equality Operators == and !=": https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.21.1
     */
    private static Expression newIntegerOrBooleanComparisonOperatorExpression(int lineNumber, Expression leftExpression, String operator, Expression rightExpression, int priority) {
        Class leftClass = leftExpression.getClass();
        Class rightClass = rightExpression.getClass();

        if (leftClass == ClassFileLocalVariableReferenceExpression.class) {
            AbstractLocalVariable leftVariable = ((ClassFileLocalVariableReferenceExpression)leftExpression).getLocalVariable();

            if (rightClass == ClassFileLocalVariableReferenceExpression.class) {
                AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();

                if (leftVariable.isAssignableFrom(TYPE_BOOLEAN) || rightVariable.isAssignableFrom(TYPE_BOOLEAN)) {
                    leftVariable.variableOnRight(rightVariable);
                    rightVariable.variableOnLeft(leftVariable);
                }
            } else {
                if (rightExpression.getType() == TYPE_BOOLEAN) {
                    leftVariable.typeOnRight(TYPE_BOOLEAN);
                }
            }
        } else {
            if (rightClass == ClassFileLocalVariableReferenceExpression.class) {
                if (leftExpression.getType() == TYPE_BOOLEAN) {
                    AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();

                    rightVariable.typeOnRight(TYPE_BOOLEAN);
                }
            }
        }

        return new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, leftExpression, operator, rightExpression, priority);
    }

    /*
     * Operators = { "==", "!=" }
     * See "Numerical Equality Operators == and !=": https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.21.1
     */
    private static Expression newIntegerComparisonOperatorExpression(int lineNumber, Expression leftExpression, String operator, Expression rightExpression, int priority) {
        Class leftClass = leftExpression.getClass();
        Class rightClass = rightExpression.getClass();

        if (leftClass == ClassFileLocalVariableReferenceExpression.class) {
            AbstractLocalVariable leftVariable = ((ClassFileLocalVariableReferenceExpression)leftExpression).getLocalVariable();

            leftVariable.typeOnLeft(MAYBE_BYTE_TYPE);
        }

        if (rightClass == ClassFileLocalVariableReferenceExpression.class) {
            AbstractLocalVariable rightVariable = ((ClassFileLocalVariableReferenceExpression)rightExpression).getLocalVariable();

            rightVariable.typeOnLeft(MAYBE_BYTE_TYPE);
        }

        return new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, leftExpression, operator, rightExpression, priority);
    }

    private static Expression newPreArithmeticOperatorExpression(int lineNumber, String operator, Expression expression) {
        reduceIntegerLocalVariableType(expression);
        return new PreOperatorExpression(lineNumber, operator, expression);
    }

    private static Expression newPostArithmeticOperatorExpression(int lineNumber, Expression expression, String operator) {
        reduceIntegerLocalVariableType(expression);
        return new PostOperatorExpression(lineNumber, expression, operator);
    }

    private static void reduceIntegerLocalVariableType(Expression expression) {
        if (expression.getClass() == ClassFileLocalVariableReferenceExpression.class) {
            ClassFileLocalVariableReferenceExpression lvre = (ClassFileLocalVariableReferenceExpression)expression;

            if (lvre.getLocalVariable().getClass() == PrimitiveLocalVariable.class) {
                PrimitiveLocalVariable plv = (PrimitiveLocalVariable)lvre.getLocalVariable();
                if (plv.isAssignableFrom(MAYBE_BOOLEAN_TYPE)) {
                    plv.typeOnRight(MAYBE_BYTE_TYPE);
                }
            }
        }
    }

    /**
     * @return expression, 'this' or 'super'
     */
    private Expression getFieldInstanceReference(Expression expression, ObjectType ot, String name) {
        if (expression.getClass() == ClassFileLocalVariableReferenceExpression.class) {
            ((ClassFileLocalVariableReferenceExpression)expression).getLocalVariable().typeOnLeft(ot);
        }

        if ((bodyDeclaration.getFieldDeclarations() != null) && (expression.getClass() == ThisExpression.class) && !ot.equals(expression.getType())) {
            memberVisitor.init(name, null);

            for (ClassFileFieldDeclaration field : bodyDeclaration.getFieldDeclarations()) {
                field.getFieldDeclarators().accept(memberVisitor);
                if (memberVisitor.isFound()) {
                    return new SuperExpression(expression.getLineNumber(), ((ThisExpression) expression).getObjectType());
                }
            }
        }

        return expression;
    }

    /**
     * @return expression, 'this' or 'super'
     */
    private Expression getMethodInstanceReference(Expression expression, ObjectType ot, String name, String descriptor) {
        if ((bodyDeclaration.getMethodDeclarations() != null) && (expression.getClass() == ThisExpression.class) && !ot.equals(expression.getType())) {
            memberVisitor.init(name, descriptor);

            for (ClassFileConstructorOrMethodDeclaration member : bodyDeclaration.getMethodDeclarations()) {
                member.accept(memberVisitor);
                if (memberVisitor.isFound()) {
                    return new SuperExpression(expression.getLineNumber(), ((ThisExpression) expression).getObjectType());
                }
            }
        }

        return expression;
    }

    private static void checkStack(DefaultStack<Expression> stack, byte[] code, int offset) {
        if ((stack.size() > 1) && (offset < code.length)) {
            int opcode = code[offset+1] & 255;

            if ((opcode == 87) || (opcode == 176)) { // POP || ARETURN
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

        if (offset + 3 > toOffset)
            return false;

        Method method = cfg.getMethod();
        byte[] code = method.<AttributeCode>getAttribute("Code").getCode();
        int opcode = code[offset] & 255;

        if (opcode != 178) // GETSTATIC
            return false;

        ConstantPool constants = method.getConstants();
        ConstantMemberRef constantMemberRef = constants.getConstant( ((code[++offset] & 255) << 8) | (code[++offset] & 255) );
        ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
        String name = constants.getConstantUtf8(constantNameAndType.getNameIndex());

        if (! "$assertionsDisabled".equals(name))
            return false;

        String descriptor = constants.getConstantUtf8(constantNameAndType.getDescriptorIndex());

        if (! "Z".equals(descriptor))
            return false;

        String typeName = constants.getConstantTypeName(constantMemberRef.getClassIndex());

        return internalTypeName.equals(typeName);
    }

    public static int getExceptionLocalVariableIndex(BasicBlock basicBlock) {
        ControlFlowGraph cfg = basicBlock.getControlFlowGraph();
        int offset = basicBlock.getFromOffset();
        int toOffset = basicBlock.getToOffset();

        if (offset + 1 > toOffset) {
            assert false;
            return -1;
        }

        Method method = cfg.getMethod();
        byte[] code = method.<AttributeCode>getAttribute("Code").getCode();
        int opcode = code[offset] & 255;

        switch (opcode) {
            case 58: // ASTORE
                return code[++offset] & 255;
            case 75: case 76: case 77: case 78: // ASTORE_0 ... ASTORE_3
                return opcode - 75;
            case 87: case 88: // POP, POP2
                return -1;
            default:
                assert false;
                return -1;
        }
    }

    public static int searchNextOpcode(BasicBlock basicBlock, int maxOffset) {
        byte[] code = basicBlock.getControlFlowGraph().getMethod().<AttributeCode>getAttribute("Code").getCode();
        int offset = basicBlock.getFromOffset();
        int toOffset = basicBlock.getToOffset();

        if (toOffset > maxOffset) {
            toOffset = maxOffset;
        }

        for (; offset<toOffset; offset++) {
            int opcode = code[offset] & 255;

            switch (opcode) {
                case 16: case 18: // BIPUSH, LDC
                case 21: case 22: case 23: case 24: case 25: // ILOAD, LLOAD, FLOAD, DLOAD, ALOAD
                case 54: case 55: case 56: case 57: case 58: // ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
                case 169: // RET
                case 188: // NEWARRAY
                    offset++;
                    break;
                case 17: // SIPUSH
                case 19: case 20: // LDC_W, LDC2_W
                case 132: // IINC
                case 178: // GETSTATIC
                case 179: // PUTSTATIC
                case 187: // NEW
                case 180: // GETFIELD
                case 181: // PUTFIELD
                case 182: case 183: // INVOKEVIRTUAL, INVOKESPECIAL
                case 184: // INVOKESTATIC
                case 189: // ANEWARRAY
                case 192: // CHECKCAST
                case 193: // INSTANCEOF
                    offset += 2;
                    break;
                case 153: case 154: case 155: case 156: case 157: case 158: // IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE
                case 159: case 160: case 161: case 162: case 163: case 164: case 165: case 166: // IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE
                case 167: // GOTO
                case 198: case 199: // IFNULL, IFNONNULL
                    int deltaOffset = (short)(((code[++offset] & 255) << 8) | (code[++offset] & 255));

                    if (deltaOffset > 0) {
                        offset += deltaOffset - 2 - 1;
                    }
                    break;
                case 200: // GOTO_W
                    deltaOffset = (((code[++offset] & 255) << 24) | ((code[++offset] & 255) << 16) | ((code[++offset] & 255) << 8) | (code[++offset] & 255));

                    if (deltaOffset > 0) {
                        offset += deltaOffset - 4 - 1;
                    }
                    break;
                case 168: // JSR
                    offset += 2;
                    break;
                case 197: // MULTIANEWARRAY
                    offset += 3;
                    break;
                case 185: // INVOKEINTERFACE
                case 186: // INVOKEDYNAMIC
                    offset += 4;
                    break;
                case 201: // JSR_W
                    offset += 4;
                    break;
                case 170: // TABLESWITCH
                    offset = (offset + 4) & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int low = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8) | (code[offset++] & 255);
                    int high = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8) | (code[offset++] & 255);

                    offset += (4 * (high - low + 1)) - 1;
                    break;
                case 171: // LOOKUPSWITCH
                    offset = (offset + 4) & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int count = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8) | (code[offset++] & 255);

                    offset += (8 * count) - 1;
                    break;
                case 196: // WIDE
                    opcode = code[++offset] & 255;

                    if (opcode == 132) { // IINC
                        offset += 4;
                    } else {
                        offset += 2;
                    }
                    break;
            }
        }

        if (offset <= maxOffset) {
            return code[offset] & 255;
        } else {
            return 0;
        }
    }

    public static int getLastOpcode(BasicBlock basicBlock) {
        byte[] code = basicBlock.getControlFlowGraph().getMethod().<AttributeCode>getAttribute("Code").getCode();
        int offset = basicBlock.getFromOffset();
        int toOffset = basicBlock.getToOffset();

        if (offset >= toOffset) {
            return 0;
        }

        int lastOffset = offset;

        for (; offset<toOffset; offset++) {
            int opcode = code[offset] & 255;

            lastOffset = offset;

            switch (opcode) {
                case 16: case 18: // BIPUSH, LDC
                case 21: case 22: case 23: case 24: case 25: // ILOAD, LLOAD, FLOAD, DLOAD, ALOAD
                case 54: case 55: case 56: case 57: case 58: // ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
                case 169: // RET
                case 188: // NEWARRAY
                    offset++;
                    break;
                case 17: // SIPUSH
                case 19: case 20: // LDC_W, LDC2_W
                case 132: // IINC
                case 178: // GETSTATIC
                case 179: // PUTSTATIC
                case 187: // NEW
                case 180: // GETFIELD
                case 181: // PUTFIELD
                case 182: case 183: // INVOKEVIRTUAL, INVOKESPECIAL
                case 184: // INVOKESTATIC
                case 189: // ANEWARRAY
                case 192: // CHECKCAST
                case 193: // INSTANCEOF
                    offset += 2;
                    break;
                case 153: case 154: case 155: case 156: case 157: case 158: // IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE
                case 159: case 160: case 161: case 162: case 163: case 164: case 165: case 166: // IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE
                case 167: // GOTO
                case 198: case 199: // IFNULL, IFNONNULL
                    int deltaOffset = (short)(((code[++offset] & 255) << 8) | (code[++offset] & 255));

                    if (deltaOffset > 0) {
                        offset += deltaOffset - 2 - 1;
                    }
                    break;
                case 200: // GOTO_W
                    deltaOffset = (((code[++offset] & 255) << 24) | ((code[++offset] & 255) << 16) | ((code[++offset] & 255) << 8) | (code[++offset] & 255));

                    if (deltaOffset > 0) {
                        offset += deltaOffset - 4 - 1;
                    }
                    break;
                case 168: // JSR
                    offset += 2;
                    break;
                case 197: // MULTIANEWARRAY
                    offset += 3;
                    break;
                case 185: // INVOKEINTERFACE
                case 186: // INVOKEDYNAMIC
                    offset += 4;
                    break;
                case 201: // JSR_W
                    offset += 4;
                    break;
                case 170: // TABLESWITCH
                    offset = (offset + 4) & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int low = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8) | (code[offset++] & 255);
                    int high = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8) | (code[offset++] & 255);

                    offset += (4 * (high - low + 1)) - 1;
                    break;
                case 171: // LOOKUPSWITCH
                    offset = (offset + 4) & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int count = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8) | (code[offset++] & 255);

                    offset += (8 * count) - 1;
                    break;
                case 196: // WIDE
                    opcode = code[++offset] & 255;

                    if (opcode == 132) { // IINC
                        offset += 4;
                    } else {
                        offset += 2;
                    }
                    break;
            }
        }

        return code[lastOffset] & 255;
    }

    public static int evalStackDepth(BasicBlock bb) {
        Method method = bb.getControlFlowGraph().getMethod();
        ConstantPool constants = method.getConstants();
        AttributeCode attributeCode = method.getAttribute("Code");
        byte[] code = attributeCode.getCode();
        return evalStackDepth(constants, code, bb);
    }

    public static int evalStackDepth(ConstantPool constants, byte[] code, BasicBlock bb) {
        ConstantMemberRef constantMemberRef;
        ConstantNameAndType constantNameAndType;
        String descriptor;
        int depth = 0;

        for (int offset=bb.getFromOffset(), toOffset=bb.getToOffset(); offset<toOffset; offset++) {
            int opcode = code[offset] & 255;

            switch (opcode) {
                case 1: // ACONST_NULL
                case 2: case 3: case 4: case 5: case 6: case 7: case 8: // ICONST_M1, ICONST_0 ... ICONST_5
                case 9: case 10: case 11: case 12: case 13: case 14: case 15: // LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1
                case 26: case 27: case 28: case 29: // ILOAD_0 ... ILOAD_3
                case 30: case 31: case 32: case 33: // LLOAD_0 ... LLOAD_3
                case 34: case 35: case 36: case 37: // FLOAD_0 ... FLOAD_3
                case 38: case 39: case 40: case 41: // DLOAD_0 ... DLOAD_3
                case 42: case 43: case 44: case 45: // ALOAD_0 ... ALOAD_3
                case 89: case 90: case 91: // DUP, DUP_X1, DUP_X2
                    depth++;
                    break;
                case 16: case 18: // BIPUSH, LDC
                case 21: case 22: case 23: case 24: case 25: // ILOAD, LLOAD, FLOAD, DLOAD, ALOAD
                    offset++;
                    depth++;
                    break;
                case 17: // SIPUSH
                case 19: case 20: // LDC_W, LDC2_W
                case 168: // JSR
                case 178: // GETSTATIC
                case 187: // NEW
                    offset += 2;
                    depth++;
                    break;
                case 46: case 47: case 48: case 49: case 50: case 51: case 52: case 53: // IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD
                case 59: case 60: case 61: case 62: // ISTORE_0 ... ISTORE_3
                case 63: case 64: case 65: case 66: // LSTORE_0 ... LSTORE_3
                case 67: case 68: case 69: case 70: // FSTORE_0 ... FSTORE_3
                case 71: case 72: case 73: case 74: // DSTORE_0 ... DSTORE_3
                case 75: case 76: case 77: case 78: // ASTORE_0 ... ASTORE_3
                case 87: // POP
                case 96: case 97: case 98: case 99:     // IADD, LADD, FADD, DADD
                case 100: case 101: case 102: case 103: // ISUB, LSUB, FSUB, DSUB
                case 104: case 105: case 106: case 107: // IMUL, LMUL, FMUL, DMUL
                case 108: case 109: case 110: case 111: // IDIV, LDIV, FDIV, DDIV
                case 112: case 113: case 114: case 115: // IREM, LREM, FREM, DREM
                case 120: case 121: // ISHL, LSHL
                case 122: case 123: // ISHR, LSHR
                case 124: case 125: // IUSHR, LUSHR
                case 126: case 127: // IAND, LAND
                case 128: case 129: // IOR, LOR
                case 130: case 131: // IXOR, LXOR
                case 148: case 149: case 150: case 151: case 152: // LCMP, FCMPL, FCMPG, DCMPL, DCMPG
                case 172: case 173: case 174: case 175: case 176: // IRETURN, LRETURN, FRETURN, DRETURN, ARETURN
                case 194: case 195: // MONITORENTER, MONITOREXIT
                    depth--;
                    break;
                case 153: case 154: case 155: case 156: case 157: case 158: // IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE
                case 179: // PUTSTATIC
                case 198: case 199: // IFNULL, IFNONNULL
                    offset += 2;
                    depth--;
                    break;
                case 54: case 55: case 56: case 57: case 58: // ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
                    offset++;
                    depth--;
                    break;
                case 79: case 80: case 81: case 82: case 83: case 84: case 85: case 86: // IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE
                    depth -= 3;
                    break;
                case 92: case 93: case 94: // DUP2, DUP2_X1, DUP2_X2
                    depth += 2;
                    break;
                case 132: // IINC
                case 167: // GOTO
                case 180: // GETFIELD
                case 189: // ANEWARRAY
                case 192: // CHECKCAST
                case 193: // INSTANCEOF
                    offset += 2;
                    break;
                case 159: case 160: case 161: case 162: case 163: case 164: case 165: case 166: // IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE
                case 181: // PUTFIELD
                    offset += 2;
                    depth -= 2;
                    break;
                case 88: // POP2
                    depth -= 2;
                    break;
                case 169: // RET
                case 188: // NEWARRAY
                    offset++;
                    break;
                case 170: // TABLESWITCH
                    offset = (offset + 4) & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int low = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8) | (code[offset++] & 255);
                    int high = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8) | (code[offset++] & 255);

                    offset += (4 * (high - low + 1)) - 1;
                    depth--;
                    break;
                case 171: // LOOKUPSWITCH
                    offset = (offset + 4) & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int count = ((code[offset++] & 255) << 24) | ((code[offset++] & 255) << 16) | ((code[offset++] & 255) << 8) | (code[offset++] & 255);

                    offset += (8 * count) - 1;
                    depth--;
                    break;
                case 182: case 183: // INVOKEVIRTUAL, INVOKESPECIAL
                    constantMemberRef = constants.getConstant(((code[++offset] & 255) << 8) | (code[++offset] & 255));
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getDescriptorIndex());
                    depth -= 1 + countMethodParameters(descriptor);

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case 184: // INVOKESTATIC
                    constantMemberRef = constants.getConstant(((code[++offset] & 255) << 8) | (code[++offset] & 255));
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getDescriptorIndex());
                    depth -= countMethodParameters(descriptor);

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case 185: // INVOKEINTERFACE
                    constantMemberRef = constants.getConstant(((code[++offset] & 255) << 8) | (code[++offset] & 255));
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getDescriptorIndex());
                    depth -= 1 + countMethodParameters(descriptor);
                    offset += 2; // Skip 'count' and one byte

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case 186: // INVOKEDYNAMIC
                    constantMemberRef = constants.getConstant(((code[++offset] & 255) << 8) | (code[++offset] & 255));
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getDescriptorIndex());
                    depth -= countMethodParameters(descriptor);
                    offset += 2; // Skip 2 bytes

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case 196: // WIDE
                    opcode = code[++offset] & 255;

                    if (opcode == 132) { // IINC
                        offset += 4;
                    } else {
                        offset += 2;

                        switch (opcode) {
                            case 21: case 22: case 23: case 24: case 25: // ILOAD, LLOAD, FLOAD, DLOAD, ALOAD
                                depth++;
                                break;
                            case 54: case 55: case 56: case 57: case 58: // ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
                                depth--;
                                break;
                            case 169: // RET
                                break;
                        }
                    }
                    break;
                case 197: // MULTIANEWARRAY
                    offset += 3;
                    depth += 1 - (code[offset] & 255);
                    break;
                case 201: // JSR_W
                    offset += 4;
                    depth++;
                case 200: // GOTO_W
                    offset += 4;
                    break;
            }
        }

        return depth;
    }

    private static int countMethodParameters(String descriptor) {
        int count = 0;
        int i = 2;
        char c = descriptor.charAt(1);

        assert (descriptor.length() > 2) && (descriptor.charAt(0) == '(');

        while (c != ')') {
            while (c == '[') {
                c = descriptor.charAt(i++);
            }
            if (c == 'L') {
                do {
                    c = descriptor.charAt(i++);
                } while (c != ';');
            }
            c = descriptor.charAt(i++);
            count++;
        }

        return count;
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

        public boolean isFound() {
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
        protected DefaultList<String> names = new DefaultList<>();

        public void reset() {
            names = new DefaultList<>();
        }

        public List<String> getNames() {
            return names;
        }

        @Override public void visit(FormalParameter declaration) {
            names.add(declaration.getName());
        }

        @Override
        @SuppressWarnings("unchecked")
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
}
