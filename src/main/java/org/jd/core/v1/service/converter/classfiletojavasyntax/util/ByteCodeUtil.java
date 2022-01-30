/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.ConstantPool;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.AttributeCode;
import org.jd.core.v1.model.classfile.constant.ConstantMemberRef;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;

import static org.apache.bcel.Const.*;

public final class ByteCodeUtil {

    private ByteCodeUtil() {
        super();
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

            offset = computeNextOffset(code, offset, opcode);
        }

        if (offset <= maxOffset) {
            return code[offset] & 255;
        }
        return 0;
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

            offset = computeNextOffset(code, offset, opcode);
        }

        return code[lastOffset] & 255;
    }

    public static void invertLastOpCode(BasicBlock basicBlock) {
        byte[] code = basicBlock.getControlFlowGraph().getMethod().<AttributeCode>getAttribute("Code").getCode();
        int offset = basicBlock.getFromOffset();
        int toOffset = basicBlock.getToOffset();

        if (offset >= toOffset) {
            return;
        }

        int lastOffset = offset;

        for (; offset<toOffset; offset++) {
            int opcode = code[offset] & 255;

            lastOffset = offset;

            offset = computeNextOffset(code, offset, opcode);
        }

        code[lastOffset] = (byte) getOppositeOpCode(code[lastOffset] & 255);
        int delta = basicBlock.getBranch().getFromOffset() - lastOffset;
        // Big Endian
        code[lastOffset+1] = (byte) ((delta >> 8) & 0xFF);
        code[lastOffset+2] = (byte) (delta & 0xFF);
    }
    
    private static int getOppositeOpCode(int opCode) {
        return switch(opCode) {
            case IFNONNULL -> IFNULL;
            case IFNULL -> IFNONNULL;
            case IF_ACMPEQ -> IF_ACMPNE;
            case IF_ACMPNE -> IF_ACMPEQ;
            case IF_ICMPEQ -> IF_ICMPNE;
            case IF_ICMPNE -> IF_ICMPEQ;
            case IF_ICMPGE -> IF_ICMPLT;
            case IF_ICMPLT -> IF_ICMPGE;
            case IF_ICMPLE -> IF_ICMPGT;
            case IF_ICMPGT -> IF_ICMPLE;
            case IFEQ -> IFNE;
            case IFNE -> IFEQ;
            case IFGE -> IFLT;
            case IFLT -> IFGE;
            case IFLE -> IFGT;
            case IFGT -> IFLE;
            default -> throw new IllegalArgumentException("Unexpected opCode " + Const.getOpcodeName(opCode));
        };
    }

    private static int computeNextOffset(byte[] code, int offset, int opcode) {
        switch (opcode) {
            case BIPUSH, LDC,
                 ILOAD, LLOAD, FLOAD, DLOAD, ALOAD,
                 ISTORE, LSTORE, FSTORE, DSTORE, ASTORE,
                 RET,
                 NEWARRAY:
                offset++;
                break;
            case SIPUSH, LDC_W, LDC2_W, IINC, GETSTATIC, PUTSTATIC, NEW, GETFIELD, PUTFIELD, INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, ANEWARRAY, CHECKCAST, INSTANCEOF:
                offset += 2;
                break;
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, IFNULL, IFNONNULL:
                int deltaOffset = (short)((code[++offset] & 255) << 8 | code[++offset] & 255);

                if (deltaOffset > 0) {
                    offset += deltaOffset - 2 - 1;
                }
                break;
            case GOTO_W:
                deltaOffset = (code[++offset] & 255) << 24 | (code[++offset] & 255) << 16 | (code[++offset] & 255) << 8 | code[++offset] & 255;

                if (deltaOffset > 0) {
                    offset += deltaOffset - 4 - 1;
                }
                break;
            case JSR:
                offset += 2;
                break;
            case MULTIANEWARRAY:
                offset += 3;
                break;
            case INVOKEINTERFACE, INVOKEDYNAMIC:
                offset += 4;
                break;
            case JSR_W:
                offset += 4;
                break;
            case TABLESWITCH:
                offset = offset + 4 & 0xFFFC; // Skip padding
                offset += 4; // Skip default offset

                int low = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 | code[offset++] & 255;
                int high = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 | code[offset++] & 255;

                offset += 4 * (high - low + 1) - 1;
                break;
            case LOOKUPSWITCH:
                offset = offset + 4 & 0xFFFC; // Skip padding
                offset += 4; // Skip default offset

                int count = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 | code[offset++] & 255;

                offset += 8 * count - 1;
                break;
            case WIDE:
                opcode = code[++offset] & 255;

                if (opcode == IINC) {
                    offset += 4;
                } else {
                    offset += 2;
                }
                break;
        }
        return offset;
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
                case ACONST_NULL,
                     ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
                     LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1,
                     ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3,
                     LLOAD_0, LLOAD_1, LLOAD_2, LLOAD_3,
                     FLOAD_0, FLOAD_1, FLOAD_2, FLOAD_3,
                     DLOAD_0, DLOAD_1, DLOAD_2, DLOAD_3,
                     ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3,
                     DUP, DUP_X1, DUP_X2:
                    depth++;
                    break;
                case BIPUSH, LDC,
                     ILOAD, LLOAD, FLOAD, DLOAD, ALOAD:
                    offset++;
                    depth++;
                    break;
                case SIPUSH,
                     LDC_W, LDC2_W,
                     JSR,
                     GETSTATIC,
                     NEW:
                    offset += 2;
                    depth++;
                    break;
                case IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD,
                     ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3,
                     LSTORE_0, LSTORE_1, LSTORE_2, LSTORE_3,
                     FSTORE_0, FSTORE_1, FSTORE_2, FSTORE_3,
                     DSTORE_0, DSTORE_1, DSTORE_2, DSTORE_3,
                     ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3,
                     POP,
                     IADD, LADD, FADD, DADD,
                     ISUB, LSUB, FSUB, DSUB,
                     IMUL, LMUL, FMUL, DMUL,
                     IDIV, LDIV, FDIV, DDIV,
                     IREM, LREM, FREM, DREM,
                     ISHL, LSHL,
                     ISHR, LSHR,
                     IUSHR, LUSHR,
                     IAND, LAND,
                     IOR, LOR,
                     IXOR, LXOR,
                     LCMP, FCMPL, FCMPG, DCMPL, DCMPG,
                     IRETURN, LRETURN, FRETURN, DRETURN, ARETURN,
                     MONITORENTER, MONITOREXIT:
                    depth--;
                    break;
                case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE,
                     PUTSTATIC,
                     IFNULL, IFNONNULL:
                    offset += 2;
                    depth--;
                    break;
                case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE:
                    offset++;
                    depth--;
                    break;
                case IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE:
                    depth -= 3;
                    break;
                case DUP2, DUP2_X1, DUP2_X2:
                    depth += 2;
                    break;
                case IINC,
                     GOTO,
                     GETFIELD,
                     ANEWARRAY,
                     CHECKCAST,
                     INSTANCEOF:
                    offset += 2;
                    break;
                case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE,
                     PUTFIELD:
                    offset += 2;
                    depth -= 2;
                    break;
                case POP2:
                    depth -= 2;
                    break;
                case RET,
                     NEWARRAY:
                    offset++;
                    break;
                case TABLESWITCH:
                    offset = offset + 4 & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int low = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 | code[offset++] & 255;
                    int high = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 | code[offset++] & 255;

                    offset += 4 * (high - low + 1) - 1;
                    depth--;
                    break;
                case LOOKUPSWITCH:
                    offset = offset + 4 & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int count = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 | code[offset++] & 255;

                    offset += 8 * count - 1;
                    depth--;
                    break;
                case INVOKEVIRTUAL, INVOKESPECIAL:
                    constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    depth -= 1 + countMethodParameters(descriptor);

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case INVOKESTATIC:
                    constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    depth -= countMethodParameters(descriptor);

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case INVOKEINTERFACE:
                    constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    depth -= 1 + countMethodParameters(descriptor);
                    offset += 2; // Skip 'count' and one byte

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case INVOKEDYNAMIC:
                    constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    depth -= countMethodParameters(descriptor);
                    offset += 2; // Skip 2 bytes

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case WIDE:
                    opcode = code[++offset] & 255;

                    if (opcode == IINC) {
                        offset += 4;
                    } else {
                        offset += 2;

                        switch (opcode) {
                            case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD:
                                depth++;
                                break;
                            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE:
                                depth--;
                                break;
                            case RET:
                                break;
                        }
                    }
                    break;
                case MULTIANEWARRAY:
                    offset += 3;
                    depth += 1 - (code[offset] & 255);
                    break;
                case JSR_W:
                    offset += 4;
                    depth++;
                    // intended fall through
                case GOTO_W:
                    offset += 4;
                    break;
            }
        }

        return depth;
    }

    public static int getMinDepth(BasicBlock bb) {
        Method method = bb.getControlFlowGraph().getMethod();
        ConstantPool constants = method.getConstants();
        AttributeCode attributeCode = method.getAttribute("Code");
        byte[] code = attributeCode.getCode();
        return getMinDepth(constants, code, bb);
    }

    private static int getMinDepth(ConstantPool constants, byte[] code, BasicBlock bb) {
        ConstantMemberRef constantMemberRef;
        ConstantNameAndType constantNameAndType;
        String descriptor;
        int depth = 0;
        int minDepth = 0;

        for (int offset=bb.getFromOffset(), toOffset=bb.getToOffset(); offset<toOffset; offset++) {
            int opcode = code[offset] & 255;

            switch (opcode) {
                case ACONST_NULL,
                     ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
                     LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1,
                     ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3,
                     LLOAD_0, LLOAD_1, LLOAD_2, LLOAD_3,
                     FLOAD_0, FLOAD_1, FLOAD_2, FLOAD_3,
                     DLOAD_0, DLOAD_1, DLOAD_2, DLOAD_3,
                     ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3:
                    depth++;
                    break;
                case DUP:
                    depth--;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    depth += 2;
                    break;
                case DUP_X1:
                    depth -= 2;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    depth += 3;
                    break;
                case DUP_X2:
                    depth -= 3;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    depth += 4;
                    break;
                case BIPUSH, LDC,
                     ILOAD, LLOAD, FLOAD, DLOAD, ALOAD:
                    offset++;
                    depth++;
                    break;
                case SIPUSH,
                     LDC_W, LDC2_W,
                     JSR,
                     GETSTATIC,
                     NEW:
                    offset += 2;
                    depth++;
                    break;
                case IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD,
                     IADD, LADD, FADD, DADD,
                     ISUB, LSUB, FSUB, DSUB,
                     IMUL, LMUL, FMUL, DMUL,
                     IDIV, LDIV, FDIV, DDIV,
                     IREM, LREM, FREM, DREM,
                     ISHL, LSHL,
                     ISHR, LSHR,
                     IUSHR, LUSHR,
                     IAND, LAND,
                     IOR, LOR,
                     IXOR, LXOR,
                     LCMP, FCMPL, FCMPG, DCMPL, DCMPG:
                    depth -= 2;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    depth++;
                    break;
                case ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3,
                     LSTORE_0, LSTORE_1, LSTORE_2, LSTORE_3,
                     FSTORE_0, FSTORE_1, FSTORE_2, FSTORE_3,
                     DSTORE_0, DSTORE_1, DSTORE_2, DSTORE_3,
                     ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3,
                     POP,
                     IRETURN, LRETURN, FRETURN, DRETURN, ARETURN,
                     MONITORENTER, MONITOREXIT:
                    depth--;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    break;
                case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE,
                     PUTSTATIC,
                     IFNULL, IFNONNULL:
                    offset += 2;
                    depth--;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    break;
                case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE:
                    offset++;
                    depth--;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    break;
                case IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE:
                    depth -= 3;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    break;
                case DUP2:
                    depth -= 2;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    depth += 4;
                    break;
                case DUP2_X1:
                    depth -= 3;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    depth += 5;
                    break;
                case DUP2_X2:
                    depth -= 4;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    depth += 6;
                    break;
                case IINC,
                     GOTO:
                    offset += 2;
                    break;
                case GETFIELD,
                     ANEWARRAY,
                     CHECKCAST,
                     INSTANCEOF:
                    offset += 2;
                    depth--;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    depth++;
                    break;
                case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE,
                     PUTFIELD:
                    offset += 2;
                    depth -= 2;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    break;
                case POP2:
                    depth -= 2;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    break;
                case RET:
                    offset++;
                    break;
                case NEWARRAY:
                    offset++;
                    depth--;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    depth++;
                    break;
                case TABLESWITCH:
                    offset = offset + 4 & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int low = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 | code[offset++] & 255;
                    int high = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 | code[offset++] & 255;

                    offset += 4 * (high - low + 1) - 1;
                    depth--;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    break;
                case LOOKUPSWITCH:
                    offset = offset + 4 & 0xFFFC; // Skip padding
                    offset += 4; // Skip default offset

                    int count = (code[offset++] & 255) << 24 | (code[offset++] & 255) << 16 | (code[offset++] & 255) << 8 | code[offset++] & 255;

                    offset += 8 * count - 1;
                    depth--;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    break;
                case INVOKEVIRTUAL, INVOKESPECIAL:
                    constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    depth -= 1 + countMethodParameters(descriptor);
                    if (minDepth > depth) {
                        minDepth = depth;
                    }

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case INVOKESTATIC:
                    constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    depth -= countMethodParameters(descriptor);
                    if (minDepth > depth) {
                        minDepth = depth;
                    }

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case INVOKEINTERFACE:
                    constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    depth -= 1 + countMethodParameters(descriptor);
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    offset += 2; // Skip 'count' and one byte

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case INVOKEDYNAMIC:
                    constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantUtf8(constantNameAndType.getSignatureIndex());
                    depth -= countMethodParameters(descriptor);
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    offset += 2; // Skip 2 bytes

                    if (descriptor.charAt(descriptor.length()-1) != 'V') {
                        depth++;
                    }
                    break;
                case WIDE:
                    opcode = code[++offset] & 255;

                    if (opcode == IINC) {
                        offset += 4;
                    } else {
                        offset += 2;

                        switch (opcode) {
                            case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD:
                                depth++;
                                break;
                            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE:
                                depth--;
                                if (minDepth > depth) {
                                    minDepth = depth;
                                }
                                break;
                            case RET:
                                break;
                        }
                    }
                    break;
                case MULTIANEWARRAY:
                    offset += 3;
                    depth -= code[offset] & 255;
                    if (minDepth > depth) {
                        minDepth = depth;
                    }
                    depth++;
                    break;
                case JSR_W:
                    offset += 4;
                    depth++;
                    // intended fall through
                case GOTO_W:
                    offset += 4;
                    break;
            }
        }

        return minDepth;
    }

    private static int countMethodParameters(String descriptor) {
        int count = 0;
        int i = 2;
        char c = descriptor.charAt(1);

        assert descriptor.length() > 2 && descriptor.charAt(0) == '(';

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

    public static boolean isStaticAccess(byte[] code, int offset) {
        int opCode = code[offset] & 255;
        return opCode == GETSTATIC
            || opCode == INVOKESTATIC
            || opCode == LDC
            || opCode == LDC_W
            || opCode == LDC2_W;
    }

    public static boolean isLoad(byte[] code, int offset) {
        int opCode = code[offset] & 255;
        return opCode >= 18 && opCode <= 53;
    }
}
