/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.SwitchCase;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.util.DefaultList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.bcel.Const.AASTORE;
import static org.apache.bcel.Const.ALOAD;
import static org.apache.bcel.Const.ALOAD_0;
import static org.apache.bcel.Const.ALOAD_1;
import static org.apache.bcel.Const.ALOAD_2;
import static org.apache.bcel.Const.ALOAD_3;
import static org.apache.bcel.Const.ANEWARRAY;
import static org.apache.bcel.Const.ARETURN;
import static org.apache.bcel.Const.ASTORE;
import static org.apache.bcel.Const.ASTORE_0;
import static org.apache.bcel.Const.ASTORE_1;
import static org.apache.bcel.Const.ASTORE_2;
import static org.apache.bcel.Const.ASTORE_3;
import static org.apache.bcel.Const.ATHROW;
import static org.apache.bcel.Const.BASTORE;
import static org.apache.bcel.Const.BIPUSH;
import static org.apache.bcel.Const.CASTORE;
import static org.apache.bcel.Const.CHECKCAST;
import static org.apache.bcel.Const.DASTORE;
import static org.apache.bcel.Const.DLOAD;
import static org.apache.bcel.Const.DRETURN;
import static org.apache.bcel.Const.DSTORE;
import static org.apache.bcel.Const.DSTORE_0;
import static org.apache.bcel.Const.DSTORE_1;
import static org.apache.bcel.Const.DSTORE_2;
import static org.apache.bcel.Const.DSTORE_3;
import static org.apache.bcel.Const.FASTORE;
import static org.apache.bcel.Const.FLOAD;
import static org.apache.bcel.Const.FRETURN;
import static org.apache.bcel.Const.FSTORE;
import static org.apache.bcel.Const.FSTORE_0;
import static org.apache.bcel.Const.FSTORE_1;
import static org.apache.bcel.Const.FSTORE_2;
import static org.apache.bcel.Const.FSTORE_3;
import static org.apache.bcel.Const.GETFIELD;
import static org.apache.bcel.Const.GETSTATIC;
import static org.apache.bcel.Const.GOTO;
import static org.apache.bcel.Const.GOTO_W;
import static org.apache.bcel.Const.IASTORE;
import static org.apache.bcel.Const.IFEQ;
import static org.apache.bcel.Const.IFGE;
import static org.apache.bcel.Const.IFGT;
import static org.apache.bcel.Const.IFLE;
import static org.apache.bcel.Const.IFLT;
import static org.apache.bcel.Const.IFNE;
import static org.apache.bcel.Const.IFNONNULL;
import static org.apache.bcel.Const.IFNULL;
import static org.apache.bcel.Const.IF_ACMPEQ;
import static org.apache.bcel.Const.IF_ACMPNE;
import static org.apache.bcel.Const.IF_ICMPEQ;
import static org.apache.bcel.Const.IF_ICMPGE;
import static org.apache.bcel.Const.IF_ICMPGT;
import static org.apache.bcel.Const.IF_ICMPLE;
import static org.apache.bcel.Const.IF_ICMPLT;
import static org.apache.bcel.Const.IF_ICMPNE;
import static org.apache.bcel.Const.IINC;
import static org.apache.bcel.Const.ILOAD;
import static org.apache.bcel.Const.INSTANCEOF;
import static org.apache.bcel.Const.INVOKEDYNAMIC;
import static org.apache.bcel.Const.INVOKEINTERFACE;
import static org.apache.bcel.Const.INVOKESPECIAL;
import static org.apache.bcel.Const.INVOKESTATIC;
import static org.apache.bcel.Const.INVOKEVIRTUAL;
import static org.apache.bcel.Const.IRETURN;
import static org.apache.bcel.Const.ISTORE;
import static org.apache.bcel.Const.ISTORE_0;
import static org.apache.bcel.Const.ISTORE_1;
import static org.apache.bcel.Const.ISTORE_2;
import static org.apache.bcel.Const.ISTORE_3;
import static org.apache.bcel.Const.JSR;
import static org.apache.bcel.Const.JSR_W;
import static org.apache.bcel.Const.LASTORE;
import static org.apache.bcel.Const.LDC;
import static org.apache.bcel.Const.LDC2_W;
import static org.apache.bcel.Const.LDC_W;
import static org.apache.bcel.Const.LLOAD;
import static org.apache.bcel.Const.LOOKUPSWITCH;
import static org.apache.bcel.Const.LRETURN;
import static org.apache.bcel.Const.LSTORE;
import static org.apache.bcel.Const.LSTORE_0;
import static org.apache.bcel.Const.LSTORE_1;
import static org.apache.bcel.Const.LSTORE_2;
import static org.apache.bcel.Const.LSTORE_3;
import static org.apache.bcel.Const.MONITORENTER;
import static org.apache.bcel.Const.MONITOREXIT;
import static org.apache.bcel.Const.MULTIANEWARRAY;
import static org.apache.bcel.Const.NEW;
import static org.apache.bcel.Const.NEWARRAY;
import static org.apache.bcel.Const.POP;
import static org.apache.bcel.Const.POP2;
import static org.apache.bcel.Const.PUTFIELD;
import static org.apache.bcel.Const.PUTSTATIC;
import static org.apache.bcel.Const.RET;
import static org.apache.bcel.Const.SASTORE;
import static org.apache.bcel.Const.SIPUSH;
import static org.apache.bcel.Const.TABLESWITCH;
import static org.apache.bcel.Const.WIDE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITIONAL_BRANCH;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_DELETED;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_GOTO;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_GOTO_IN_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_JSR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RET;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN_VALUE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_START;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_STATEMENTS;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_SWITCH_DECLARATION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_THROW;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY_DECLARATION;


public class ControlFlowGraphMaker {
    protected static final BasicBlock MARK = END;

    protected static final CodeExceptionComparator CODE_EXCEPTION_COMPARATOR = new CodeExceptionComparator();

    public ControlFlowGraph make(Method method) {
        Code attributeCode = method.getCode();

        if (attributeCode == null) {
            return null;
        }
        // Parse byte-code
        ConstantPool constants = method.getConstantPool();
        byte[] code = attributeCode.getCode();
        int length = code.length;
        BasicBlock[] map = new BasicBlock[length];
        char[] types = new char[length];                   // 'c' for conditional instruction, 'g' for goto, 't' for throw, 's' for switch, 'r' for return
        int[] nextOffsets = new int[length];               // Next instruction offsets
        int[] branchOffsets = new int[length];             // Branch offsets
        int[][] switchValues = new int[length][];          // Default-value and switch-values
        int[][] switchOffsets = new int[length][];         // Default-case offset and switch-case offsets

        // --- Search leaders --- //
        // The first instruction is a leader
        map[0] = MARK;
        int lastOffset = 0;
        int lastStatementOffset = -1;
        int opcode;
        for (int offset=0; offset<length; offset++) {
            nextOffsets[lastOffset] = offset;
            lastOffset = offset;

            opcode = code[offset] & 255;

            switch (opcode) {
                case BIPUSH,
                     LDC,
                     ILOAD, LLOAD, FLOAD, DLOAD,
                     NEWARRAY:
                    offset++;
                    break;
                case ALOAD, ALOAD_1, ALOAD_2, ALOAD_3:
                    if (opcode == ALOAD) {
                        // ALOAD param to skip
                        offset++;
                    }
                    // identify access to static member from instance by checking if the ALOAD is discarded by a POP
                    // followed by another load instruction
                    if (offset+2 < length
                        && (code[offset+1] & 255) == POP
                        && ByteCodeUtil.isLoad(code, offset+2)) {
                        offset++; // skip pop
                    }
                    break;
                case ALOAD_0:
                    // identify constants prefixed with 'this' qualifier
                    // by matching the pattern 1)aload_0 -> 2)pop -> 3)ldc/getstatic
                    // that we can refactor to a single ldc, ignoring the aload_0 and pop
                    if (offset+2 < length
                        && (code[offset+1] & 255) == POP
                        && ByteCodeUtil.isStaticAccess(code, offset+2)) {
                        offset++; // skip pop
                    }
                    // another pattern 1)aload_0 -> 2)getfield -> 3)pop -> 4)ldc/getstatic
                    if (offset+5 < length
                        && (code[offset+1] & 255) == GETFIELD
                        && (code[offset+4] & 255) == POP
                        && ByteCodeUtil.isStaticAccess(code, offset+5)) {
                        offset+=4; // skip getfield and pop
                    }
                    break;
                case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE:
                    offset++;
                    lastStatementOffset = offset;
                    break;
                case ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3,
                     LSTORE_0, LSTORE_1, LSTORE_2, LSTORE_3,
                     FSTORE_0, FSTORE_1, FSTORE_2, FSTORE_3,
                     DSTORE_0, DSTORE_1, DSTORE_2, DSTORE_3,
                     ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3,
                     IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE,
                     POP, POP2,
                     MONITORENTER, MONITOREXIT:
                    lastStatementOffset = offset;
                    break;
                case RET:
                    offset++;
                    // The instruction that immediately follows a conditional or an unconditional goto/jump instruction is a leader
                    types[offset] = 'R';
                    if (offset + 1 < length) {
                        map[offset + 1] = MARK;
                    }
                    lastStatementOffset = offset;
                    break;
                case PUTSTATIC, PUTFIELD:
                    offset += 2;
                    lastStatementOffset = offset;
                    break;
                case INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC:
                    ConstantCP constantMemberRef = constants.getConstant( (code[++offset] & 255) << 8 | code[++offset] & 255 );
                    ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    String descriptor = constants.getConstantString(constantNameAndType.getSignatureIndex(), Const.CONSTANT_Utf8);
                    if (descriptor.charAt(descriptor.length()-1) == 'V') {
                        lastStatementOffset = offset;
                    }
                    break;
                case INVOKEINTERFACE, INVOKEDYNAMIC:
                    constantMemberRef = constants.getConstant( (code[++offset] & 255) << 8 | code[++offset] & 255 );
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    descriptor = constants.getConstantString(constantNameAndType.getSignatureIndex(), Const.CONSTANT_Utf8);
                    offset += 2; // Skip 2 bytes
                    if (descriptor.charAt(descriptor.length()-1) == 'V') {
                        lastStatementOffset = offset;
                    }
                    break;
                case IINC:
                    offset += 2;
                    if (lastStatementOffset+3 == offset && !checkILOADForIINC(code, offset, code[offset-1] & 255)) {
                        // Last instruction is a 'statement' & the next instruction is not a matching ILOAD -> IINC as a statement
                        lastStatementOffset = offset;
                    }
                    break;
                case SIPUSH,
                     LDC_W, LDC2_W,
                     GETSTATIC, GETFIELD,
                     NEW, ANEWARRAY,
                     CHECKCAST,
                     INSTANCEOF:
                    offset += 2;
                    break;
                case GOTO:
                    char type = lastStatementOffset+1 == offset ? 'g' : 'G';

                    if (lastStatementOffset != -1) {
                        map[lastStatementOffset + 1] = MARK;
                    }
                    // The target of a conditional or an unconditional goto/jump instruction is a leader
                    int branchOffset = offset + (short)((code[++offset] & 255) << 8 | code[++offset] & 255);
                    map[branchOffset] = MARK;
                    types[offset] = type;
                    branchOffsets[offset] = branchOffset;
                    // The instruction that immediately follows a conditional or an unconditional goto/jump instruction is a leader
                    if (offset + 1 < length) {
                        map[offset + 1] = MARK;
                    }
                    lastStatementOffset = offset;
                    break;
                case JSR:
                    if (lastStatementOffset != -1) {
                        map[lastStatementOffset + 1] = MARK;
                    }
                    // The target of a conditional or an unconditional goto/jump instruction is a leader
                    branchOffset = offset + (short)((code[++offset] & 255) << 8 | code[++offset] & 255);
                    map[branchOffset] = MARK;
                    types[offset] = 'j';
                    branchOffsets[offset] = branchOffset;
                    // The instruction that immediately follows a conditional or an unconditional goto/jump instruction is a leader
                    if (offset + 1 < length) {
                        map[offset + 1] = MARK;
                    }
                    lastStatementOffset = offset;
                    break;
                case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE,
                     IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE,
                     IFNULL, IFNONNULL:
                    if (lastStatementOffset != -1) {
                        map[lastStatementOffset + 1] = MARK;
                    }
                    // The target of a conditional or an unconditional goto/jump instruction is a leader
                    branchOffset = offset + (short)((code[++offset] & 255) << 8 | code[++offset] & 255);
                    map[branchOffset] = MARK;
                    types[offset] = 'c';
                    branchOffsets[offset] = branchOffset;
                    // The instruction that immediately follows a conditional or an unconditional goto/jump instruction is a leader
                    if (offset + 1 < length) {
                        map[offset + 1] = MARK;
                    }
                    lastStatementOffset = offset;
                    break;
                case TABLESWITCH:
                    // Skip padding
                    int i = offset + 4 & 0xFFFC;
                    int defaultOffset = offset + ((code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255);

                    map[defaultOffset] = MARK;

                    int low = (code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255;
                    int high = (code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255;
                    int[] values = new int[high - low + 2];
                    int[] offsets = new int[high - low + 2];

                    offsets[0] = defaultOffset;

                    for (int j=1, len=high-low+2; j<len; j++) {
                        values[j] = low + j - 1;
                        branchOffset = offsets[j] = offset + ((code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255);
                        map[branchOffset] = MARK;
                    }

                    offset = i - 1;
                    types[offset] = 's';
                    switchValues[offset] = values;
                    switchOffsets[offset] = offsets;
                    lastStatementOffset = offset;
                    break;
                case LOOKUPSWITCH:
                    // Skip padding
                    i = offset + 4 & 0xFFFC;
                    defaultOffset = offset + ((code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255);

                    map[defaultOffset] = MARK;

                    int npairs = (code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255;

                    values = new int[npairs + 1];
                    offsets = new int[npairs + 1];

                    offsets[0] = defaultOffset;

                    for (int j=1; j<=npairs; j++) {
                        values[j] = (code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255;
                        branchOffset = offsets[j] = offset + ((code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255);
                        map[branchOffset] = MARK;
                    }

                    offset = i - 1;
                    types[offset] = 's';
                    switchValues[offset] = values;
                    switchOffsets[offset] = offsets;
                    lastStatementOffset = offset;
                    break;
                case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN:
                    types[offset] = 'v';
                    if (offset + 1 < length) {
                        map[offset + 1] = MARK;
                    }
                    lastStatementOffset = offset;
                    break;
                case Const.RETURN:
                    if (lastStatementOffset != -1) {
                        map[lastStatementOffset + 1] = MARK;
                    }
                    types[offset] = 'r';
                    if (offset + 1 < length) {
                        map[offset + 1] = MARK;
                    }
                    lastStatementOffset = offset;
                    break;
                case ATHROW:
                    types[offset] = 't';
                    if (offset + 1 < length) {
                        map[offset + 1] = MARK;
                    }
                    lastStatementOffset = offset;
                    break;
                case WIDE:
                    offset++;
                    opcode = code[offset] & 255;

                    switch (opcode) {
                        case IINC:
                            offset += 4;
                            if (lastStatementOffset+6 == offset && !checkILOADForIINC(code, offset, (code[offset-3] & 255) << 8 | code[offset-2] & 255)) {
                                // Last instruction is a 'statement' & the next instruction is not a matching ILOAD -> IINC as a statement
                                lastStatementOffset = offset;
                            }
                            break;
                        case RET:
                            offset += 2;
                            // The instruction that immediately follows a conditional or an unconditional goto/jump instruction is a leader
                            types[offset] = 'R';
                            if (offset + 1 < length) {
                                map[offset + 1] = MARK;
                            }
                            lastStatementOffset = offset;
                            break;
                        case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE:
                            lastStatementOffset = offset+2;
                            // intended fall through
                        default:
                            offset += 2;
                            break;
                    }
                    break;
                case MULTIANEWARRAY:
                    offset += 3;
                    break;
                case GOTO_W:
                    type = lastStatementOffset+1 == offset ? 'g' : 'G';

                    branchOffset = offset + ((code[++offset] & 255) << 24 | (code[++offset] & 255) << 16 | (code[++offset] & 255) << 8 | code[++offset] & 255);
                    map[branchOffset] = MARK;
                    types[offset] = type;
                    branchOffsets[offset] = branchOffset;
                    // The instruction that immediately follows a conditional or an unconditional goto/jump instruction is a leader
                    if (offset + 1 < length) {
                        map[offset + 1] = MARK;
                    }
                    lastStatementOffset = offset;
                    break;
                case JSR_W:
                    if (lastStatementOffset != -1) {
                        map[lastStatementOffset + 1] = MARK;
                    }
                    // The target of a conditional or an unconditional goto/jump instruction is a leader
                    branchOffset = offset + ((code[++offset] & 255) << 24 | (code[++offset] & 255) << 16 | (code[++offset] & 255) << 8 | code[++offset] & 255);
                    map[branchOffset] = MARK;
                    types[offset] = 'j';
                    branchOffsets[offset] = branchOffset;
                    // The instruction that immediately follows a conditional or an unconditional goto/jump instruction is a leader
                    if (offset + 1 < length) {
                        map[offset + 1] = MARK;
                    }
                    lastStatementOffset = offset;
                    break;
            }
        }
        nextOffsets[lastOffset] = length;
        CodeException[] codeExceptions = attributeCode.getExceptionTable();
        for (CodeException codeException : codeExceptions) {
            map[codeException.getStartPC()] = MARK;
            map[codeException.getHandlerPC()] = MARK;
        }
        // --- Create line numbers --- //
        ControlFlowGraph cfg = new ControlFlowGraph(method);
        LineNumberTable attributeLineNumberTable = attributeCode.getLineNumberTable();
        if (attributeLineNumberTable != null) {
            // Parse line numbers
            LineNumber[] lineNumberTable = attributeLineNumberTable.getLineNumberTable();

            int[] offsetToLineNumbers = new int[length];
            int offset = 0;
            int lineNumber = lineNumberTable[0].getLineNumber();

            LineNumber lineNumberEntry;
            int toIndex;
            for (int i=1, len=lineNumberTable.length; i<len; i++) {
                lineNumberEntry = lineNumberTable[i];
                toIndex = lineNumberEntry.getStartPC();

                while (offset < toIndex) {
                    offsetToLineNumbers[offset] = lineNumber;
                    offset++;
                }

                if (lineNumber > lineNumberEntry.getLineNumber()) {
                    map[offset] = MARK;
                }

                lineNumber = lineNumberEntry.getLineNumber();
            }

            while (offset < length) {
                offsetToLineNumbers[offset] = lineNumber;
                offset++;
            }

            cfg.setOffsetToLineNumbers(offsetToLineNumbers);
        }
        // --- Create basic blocks --- //
        lastOffset = 0;
        // Add 'start'
        BasicBlock startBasicBlock = cfg.newBasicBlock(TYPE_START, 0, 0);
        for (int offset=nextOffsets[0]; offset<length; offset=nextOffsets[offset]) {
            if (map[offset] != null) {
                map[lastOffset] = cfg.newBasicBlock(lastOffset, offset);
                lastOffset = offset;
            }
        }
        map[lastOffset] = cfg.newBasicBlock(lastOffset, length);
        // --- Set type, successors and predecessors --- //
        List<BasicBlock> list = cfg.getBasicBlocks();
        List<BasicBlock> basicBlocks = new DefaultList<>(list.size());
        BasicBlock successor = list.get(1);
        startBasicBlock.setNext(successor);
        successor.getPredecessors().add(startBasicBlock);
        BasicBlock basicBlock;
        int lastInstructionOffset;
        for (int i=1, basicBlockLength=list.size(); i<basicBlockLength; i++) {
            basicBlock = list.get(i);
            lastInstructionOffset = basicBlock.getToOffset() - 1;

            switch (types[lastInstructionOffset]) {
                case 'g': // Goto
                    basicBlock.setType(TYPE_GOTO);
                    successor = map[branchOffsets[lastInstructionOffset]];
                    basicBlock.setNext(successor);
                    successor.getPredecessors().add(basicBlock);
                    break;
                case 'G': // Goto in ternary operator
                    basicBlock.setType(TYPE_GOTO_IN_TERNARY_OPERATOR);
                    successor = map[branchOffsets[lastInstructionOffset]];
                    basicBlock.setNext(successor);
                    successor.getPredecessors().add(basicBlock);
                    break;
                case 't': // Throw
                    basicBlock.setType(TYPE_THROW);
                    basicBlock.setNext(END);
                    break;
                case 'r': // Return
                    basicBlock.setType(TYPE_RETURN);
                    basicBlock.setNext(END);
                    break;
                case 'c': // Conditional
                    basicBlock.setType(TYPE_CONDITIONAL_BRANCH);
                    successor = map[basicBlock.getToOffset()];
                    basicBlock.setNext(successor);
                    successor.getPredecessors().add(basicBlock);
                    successor = map[branchOffsets[lastInstructionOffset]];
                    basicBlock.setBranch(successor);
                    successor.getPredecessors().add(basicBlock);
                    break;
                case 's': // Switch
                    basicBlock.setType(TYPE_SWITCH_DECLARATION);
                    int[] values = switchValues[lastInstructionOffset];
                    int[] offsets = switchOffsets[lastInstructionOffset];
                    DefaultList<SwitchCase> switchCases = new DefaultList<>(offsets.length);

                    int defaultOffset = offsets[0];
                    BasicBlock bb = map[defaultOffset];
                    switchCases.add(new SwitchCase(bb));
                    bb.getPredecessors().add(basicBlock);

                    for (int j=1, len=offsets.length; j<len; j++) {
                        int offset = offsets[j];
                        if (offset != defaultOffset) {
                            bb = map[offset];
                            switchCases.add(new SwitchCase(values[j], bb));
                            bb.getPredecessors().add(basicBlock);
                        }
                    }

                    basicBlock.setSwitchCases(switchCases);
                    break;
                case 'j': // Jsr
                    basicBlock.setType(TYPE_JSR);
                    successor = map[basicBlock.getToOffset()];
                    basicBlock.setNext(successor);
                    successor.getPredecessors().add(basicBlock);
                    successor = map[branchOffsets[lastInstructionOffset]];
                    basicBlock.setBranch(successor);
                    successor.getPredecessors().add(basicBlock);
                    break;
                case 'R': // Ret
                    basicBlock.setType(TYPE_RET);
                    basicBlock.setNext(END);
                    break;
                case 'v': // Return value
                    basicBlock.setType(TYPE_RETURN_VALUE);
                    basicBlock.setNext(END);
                    break;
                default:
                    basicBlock.setType(TYPE_STATEMENTS);
                    successor = map[basicBlock.getToOffset()];
                    basicBlock.setNext(successor);
                    successor.getPredecessors().add(basicBlock);
                    basicBlocks.add(basicBlock);
                    break;
            }
        }
        // --- Create try-catch-finally basic blocks --- //
        Map<String, BasicBlock> cache = new HashMap<>();
        ConstantPool constantPool = method.getConstantPool();
        // Reuse arrays
        int[] handlePcToStartPc = branchOffsets;
        char[] handlePcMarks = types;

        Arrays.sort(codeExceptions, CODE_EXCEPTION_COMPARATOR);

        int startPc;
        int handlerPc;
        for (CodeException codeException : codeExceptions) {
            startPc = codeException.getStartPC();
            handlerPc = codeException.getHandlerPC();

            if (startPc != handlerPc && (handlePcMarks[handlerPc] != 'T' || startPc <= map[handlePcToStartPc[handlerPc]].getFromOffset())) {
                int catchType = codeException.getCatchType();
                String key = makeShortKey(codeException);
                BasicBlock tcf = cache.get(key);

                if (tcf == null) {
                    int endPc = codeException.getEndPC();
                    // Check 'endPc'
                    BasicBlock start = map[startPc];

                    // Insert a new 'try-catch-finally' basic block
                    tcf = cfg.newBasicBlock(TYPE_TRY_DECLARATION, startPc, endPc);
                    tcf.setNext(start);

                    // Update predecessors
                    Set<BasicBlock> tcfPredecessors = tcf.getPredecessors();
                    Set<BasicBlock> startPredecessors = start.getPredecessors();
                    Iterator<BasicBlock> iterator = startPredecessors.iterator();

                    BasicBlock predecessor;
                    while (iterator.hasNext()) {
                        predecessor = iterator.next();

                        if (!start.contains(predecessor)) {
                            predecessor.replace(start, tcf);
                            tcfPredecessors.add(predecessor);
                            iterator.remove();
                        }
                    }

                    startPredecessors.add(tcf);

                    // Update map
                    map[startPc] = tcf;

                    // Store to objectTypeCache
                    cache.put(key, tcf);
                }

                String internalThrowableName = catchType == 0 ? null : constantPool.getConstantString(catchType, Const.CONSTANT_Class);
                BasicBlock handlerBB = map[handlerPc];
                tcf.addExceptionHandler(internalThrowableName, handlerBB);
                handlerBB.getPredecessors().add(tcf);
                handlePcToStartPc[handlerPc] = startPc;
                handlePcMarks[handlerPc] = 'T';
            }
        }
        BasicBlock next;
        Set<BasicBlock> predecessors;
        // --- Recheck TYPE_GOTO_IN_TERNARY_OPERATOR --- //
        for (BasicBlock bb : basicBlocks) {
            next = bb.getNext();
            if (bb.getType() == TYPE_STATEMENTS && next.getPredecessors().size() == 1) {
                if (next.getType() == TYPE_GOTO && ByteCodeUtil.evalStackDepth(constants, code, bb) > 0) {
                    // Transform STATEMENTS and GOTO to GOTO_IN_TERNARY_OPERATOR
                    bb.setType(TYPE_GOTO_IN_TERNARY_OPERATOR);
                    bb.setToOffset(next.getToOffset());
                    bb.setNext(next.getNext());
                    predecessors = next.getNext().getPredecessors();
                    predecessors.remove(next);
                    predecessors.add(bb);
                    next.setType(TYPE_DELETED);
                    if (bb.getNext().getNext() == bb) {
                        bb.setType(TYPE_STATEMENTS);
                    }
                } else if (next.getType() == TYPE_CONDITIONAL_BRANCH && ByteCodeUtil.evalStackDepth(constants, code, bb) > 0) {
                    // Merge STATEMENTS and CONDITIONAL_BRANCH
                    bb.setType(TYPE_CONDITIONAL_BRANCH);
                    bb.setToOffset(next.getToOffset());
                    bb.setNext(next.getNext());
                    predecessors = next.getNext().getPredecessors();
                    predecessors.remove(next);
                    predecessors.add(bb);
                    bb.setBranch(next.getBranch());
                    predecessors = next.getBranch().getPredecessors();
                    predecessors.remove(next);
                    predecessors.add(bb);
                    next.setType(TYPE_DELETED);
                }
            }
        }
        return cfg;
    }

    protected static boolean checkILOADForIINC(byte[] code, int offset, int index) {
        if (++offset < code.length) {
            int nextOpcode = code[offset] & 255;

            if (nextOpcode == ILOAD) {
                if (index == (code[offset+1] & 255)) {
                    return true;
                }
            } else if (nextOpcode == 26+index) { // ILOAD_0 ... ILOAD_3
                return true;
            }
        }

        return false;
    }
    
    public static String makeShortKey(CodeException ce) {
        return ce.getStartPC() + "-" + ce.getEndPC();
    }
    
    /** 1) Smaller 'startPc' first 2) Smaller 'endPc' first. */
    public static class CodeExceptionComparator implements java.io.Serializable, Comparator<CodeException> {
        /**
         * Comparators should be Serializable: A non-serializable Comparator can prevent an otherwise-Serializable ordered collection from being serializable.
         */
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(CodeException ce1, CodeException ce2) {
            int comp = ce1.getStartPC() - ce2.getStartPC();
            if (comp == 0) {
                comp = ce1.getEndPC() - ce2.getEndPC();
            }
            return comp;
        }
    }
}
