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
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.LocalVariableTypeTable;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.statement.AssertStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.apache.bcel.Const.ALOAD;
import static org.apache.bcel.Const.ANEWARRAY;
import static org.apache.bcel.Const.ASTORE;
import static org.apache.bcel.Const.BIPUSH;
import static org.apache.bcel.Const.CHECKCAST;
import static org.apache.bcel.Const.DLOAD;
import static org.apache.bcel.Const.DSTORE;
import static org.apache.bcel.Const.FLOAD;
import static org.apache.bcel.Const.FSTORE;
import static org.apache.bcel.Const.GETFIELD;
import static org.apache.bcel.Const.GETSTATIC;
import static org.apache.bcel.Const.GOTO;
import static org.apache.bcel.Const.GOTO_W;
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
import static org.apache.bcel.Const.ISTORE;
import static org.apache.bcel.Const.JSR;
import static org.apache.bcel.Const.JSR_W;
import static org.apache.bcel.Const.LDC;
import static org.apache.bcel.Const.LDC2_W;
import static org.apache.bcel.Const.LDC_W;
import static org.apache.bcel.Const.LLOAD;
import static org.apache.bcel.Const.LOOKUPSWITCH;
import static org.apache.bcel.Const.LSTORE;
import static org.apache.bcel.Const.MULTIANEWARRAY;
import static org.apache.bcel.Const.NEW;
import static org.apache.bcel.Const.NEWARRAY;
import static org.apache.bcel.Const.PUTFIELD;
import static org.apache.bcel.Const.PUTSTATIC;
import static org.apache.bcel.Const.RET;
import static org.apache.bcel.Const.SIPUSH;
import static org.apache.bcel.Const.TABLESWITCH;
import static org.apache.bcel.Const.T_BOOLEAN;
import static org.apache.bcel.Const.T_BYTE;
import static org.apache.bcel.Const.T_CHAR;
import static org.apache.bcel.Const.T_DOUBLE;
import static org.apache.bcel.Const.T_FLOAT;
import static org.apache.bcel.Const.T_INT;
import static org.apache.bcel.Const.T_LONG;
import static org.apache.bcel.Const.T_SHORT;
import static org.apache.bcel.Const.WIDE;

/**
 * Example:
 // Byte code:
 //   0: aconst_null
 //   1: astore_2
 //   2: aconst_null
 //   3: astore_3
 //   4: aload_0
 //   5: aload_1
 //   6: invokeinterface 142 2 0
 //   11: astore_2
 //   12: aload_2
 //   13: ifnull +49 -> 62
 //   16: aload_2
 //   17: invokestatic 146    jd/core/process/deserializer/ClassFileDeserializer:Deserialize    (Ljava/io/DataInput;)Ljd/core/model/classfile/ClassFile;
 //   20: astore_3
 //   21: goto +41 -> 62
 //   24: astore 4
 //   26: aconst_null
 //   27: astore_3
 //   28: aload_2
 //   29: ifnull +46 -> 75
 //   32: aload_2
 //   33: invokevirtual 149    java/io/DataInputStream:close    ()V
 //   36: goto +39 -> 75
 //   39: astore 6
 //   41: goto +34 -> 75
 //   44: astore 5
 //   46: aload_2
 //   47: ifnull +12 -> 59
 //   50: aload_2
 //   51: invokevirtual 149    java/io/DataInputStream:close    ()V
 //   54: goto +5 -> 59
 //   57: astore 6
 //   59: aload 5
 //   61: athrow
 //   62: aload_2
 //   63: ifnull +12 -> 75
 //   66: aload_2
 //   67: invokevirtual 149    java/io/DataInputStream:close    ()V
 //   70: goto +5 -> 75
 //   73: astore 6
 //   75: aload_3
 //   76: areturn
 // Line number table:
 //   #Java source line    -> byte code offset
 //   #112    -> 0
 //   #113    -> 2
 //   #117    -> 4
 //   #118    -> 12
 //   #119    -> 16
 //   #120    -> 21
 //   #121    -> 24
 //   #123    -> 26
 //   #128    -> 28
 //   #129    -> 32
 //   #127    -> 44
 //   #128    -> 46
 //   #129    -> 50
 //   #130    -> 59
 //   #128    -> 62
 //   #129    -> 66
 //   #132    -> 75
 // Local variable table:
 //   start    length    slot    name    signature
 //   0    77    0    loader    Loader
 //   0    77    1    internalClassPath    String
 //   1    66    2    dis    java.io.DataInputStream
 //   3    73    3    classFile    ClassFile
 //   24    3    4    e    IOException
 //   44    16    5    localObject    Object
 //   39    1    6    localIOException1    IOException
 //   57    1    6    localIOException2    IOException
 //   73    1    6    localIOException3    IOException
 // Exception table:
 //   from    to    target    type
 //   4    21    24    java/io/IOException
 //   32    36    39    java/io/IOException
 //   4    28    44    finally
 //   50    54    57    java/io/IOException
 //   66    70    73    java/io/IOException
 */
public class ByteCodeWriter {

    public static final String DECOMPILATION_FAILED_AT_LINE = "Decompilation failed at line #";

    public static final String ILLEGAL_OPCODE = "<illegal opcode>";

    public String write(String linePrefix, Method method) {
        Code attributeCode = method.getCode();

        if (attributeCode == null) {
            return null;
        }
        ConstantPool constants = method.getConstantPool();
        StringBuilder sb = new StringBuilder(5 * 1024);

        writeByteCode(linePrefix, sb, constants, attributeCode);
        writeLineNumberTable(linePrefix, sb, attributeCode);
        writeLocalVariableTable(linePrefix, sb, attributeCode);
        writeExceptionTable(linePrefix, sb, constants, attributeCode);

        return sb.toString();
    }

    public String write(String linePrefix, Method method, int fromOffset, int toOffset) {
        Code attributeCode = method.getCode();

        if (attributeCode == null) {
            return null;
        }
        ConstantPool constants = method.getConstantPool();
        StringBuilder sb = new StringBuilder(1024);
        byte[] code = attributeCode.getCode();

        writeByteCode(linePrefix, sb, constants, code, fromOffset, toOffset);

        return sb.toString();
    }

    protected void writeByteCode(String linePrefix, StringBuilder sb, ConstantPool constants, Code attributeCode) {
        byte[] code = attributeCode.getCode();
        int length = code.length;

        sb.append(linePrefix).append("Byte code:\n");
        writeByteCode(linePrefix, sb, constants, code, 0, length);
    }

    protected void writeByteCode(String linePrefix, StringBuilder sb, ConstantPool constants, byte[] code, int fromOffset, int toOffset) {
        for (int offset=fromOffset; offset<toOffset; offset++) {
            int opcode = code[offset] & 255;

            sb.append(linePrefix).append("  ").append(offset).append(": ").append(OPCODE_NAMES[opcode]);

            switch (opcode) {
                case BIPUSH:
                    sb.append(" #").append((byte) (code[++offset] & 255));
                    break;
                case SIPUSH:
                    sb.append(" #").append((short)((code[++offset] & 255) << 8 | code[++offset] & 255));
                    break;
                case LDC:
                    writeLDC(sb, constants, constants.getConstant(code[++offset] & 255));
                    break;
                case LDC_W, LDC2_W:
                    writeLDC(sb, constants, constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255));
                    break;
                case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD,
                     ISTORE, LSTORE, FSTORE, DSTORE, ASTORE,
                     RET:
                    sb.append(" #").append(code[++offset] & 255);
                    break;
                case IINC:
                    sb.append(" #").append(code[++offset] & 255).append(", ").append((byte)(code[++offset] & 255));
                    break;
                case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE,
                     IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE,
                     GOTO, JSR:
                    sb.append(" -> ").append(offset + (short)((code[++offset] & 255) << 8 | code[++offset] & 255));
                    break;
                case TABLESWITCH:
                    // Skip padding
                    int i = offset + 4 & 0xFFFC;

                    sb.append(" default").append(" -> ").append(offset + ((code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255));

                    int low = (code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255;
                    int high = (code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255;

                    for (int value = low; value <= high; value++) {
                        sb.append(", ").append(value).append(" -> ").append(offset + ((code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255));
                    }

                    offset = i - 1;
                    break;
                case LOOKUPSWITCH:
                    // Skip padding
                    i = offset + 4 & 0xFFFC;

                    sb.append(" default").append(" -> ").append(offset + ((code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255));

                    int npairs = (code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255;

                    for (int k = 0; k < npairs; k++) {
                        sb.append(", ").append((code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255);
                        sb.append(" -> ").append(offset + ((code[i++] & 255) << 24 | (code[i++] & 255) << 16 | (code[i++] & 255) << 8 | code[i++] & 255));
                    }

                    offset = i - 1;
                    break;
                case GETSTATIC, PUTSTATIC:
                    ConstantCP constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    String typeName = constants.getConstantString(constantMemberRef.getClassIndex(), Const.CONSTANT_Class);
                    ConstantNameAndType constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    String name = constants.getConstantString(constantNameAndType.getNameIndex(), Const.CONSTANT_Utf8);
                    String descriptor = constants.getConstantString(constantNameAndType.getSignatureIndex(), Const.CONSTANT_Utf8);

                    sb.append(" ").append(typeName).append('.').append(name).append(" : ").append(descriptor);
                    break;
                case GETFIELD, PUTFIELD, INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC:
                    constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    name = constants.getConstantString(constantNameAndType.getNameIndex(), Const.CONSTANT_Utf8);
                    descriptor = constants.getConstantString(constantNameAndType.getSignatureIndex(), Const.CONSTANT_Utf8);

                    sb.append(" ").append(name).append(" : ").append(descriptor);
                    break;
                case INVOKEINTERFACE, INVOKEDYNAMIC:
                    constantMemberRef = constants.getConstant((code[++offset] & 255) << 8 | code[++offset] & 255);
                    constantNameAndType = constants.getConstant(constantMemberRef.getNameAndTypeIndex());
                    name = constants.getConstantString(constantNameAndType.getNameIndex(), Const.CONSTANT_Utf8);
                    descriptor = constants.getConstantString(constantNameAndType.getSignatureIndex(), Const.CONSTANT_Utf8);

                    sb.append(" ").append(name).append(" : ").append(descriptor);

                    offset += 2; // Skip 2 bytes
                    break;
                case NEW, ANEWARRAY, CHECKCAST, INSTANCEOF:
                    typeName = constants.getConstantString((code[++offset] & 255) << 8 | code[++offset] & 255, Const.CONSTANT_Class);
                    sb.append(" ").append(typeName);
                    break;
                case NEWARRAY:
                    switch (code[++offset] & 255) {
                        case T_BOOLEAN:
                            sb.append(" boolean");
                            break;
                        case T_CHAR:
                            sb.append(" char");
                            break;
                        case T_FLOAT:
                            sb.append(" float");
                            break;
                        case T_DOUBLE:
                            sb.append(" double");
                            break;
                        case T_BYTE:
                            sb.append(" byte");
                            break;
                        case T_SHORT:
                            sb.append(" short");
                            break;
                        case T_INT:
                            sb.append(" int");
                            break;
                        case T_LONG:
                            sb.append(" long");
                            break;
                    }
                    break;
                case WIDE:
                    opcode = code[++offset] & 255;
                    i = (code[++offset] & 255) << 8 | code[++offset] & 255;

                    if (opcode == IINC) {
                        sb.append(" iinc #").append(i).append(' ').append((short)((code[++offset] & 255) << 8 | code[++offset] & 255));
                    } else {
                        switch (opcode) {
                            case ILOAD:
                                sb.append(" iload #").append(i);
                                break;
                            case LLOAD:
                                sb.append(" lload #").append(i);
                                break;
                            case FLOAD:
                                sb.append(" fload #").append(i);
                                break;
                            case DLOAD:
                                sb.append(" dload #").append(i);
                                break;
                            case ALOAD:
                                sb.append(" aload #").append(i);
                                break;
                            case ISTORE:
                                sb.append(" istore #").append(i);
                                break;
                            case LSTORE:
                                sb.append(" lstore #").append(i);
                                break;
                            case FSTORE:
                                sb.append(" fstore #").append(i);
                                break;
                            case DSTORE:
                                sb.append(" dstore #").append(i);
                                break;
                            case ASTORE:
                                sb.append(" astore #").append(i);
                                break;
                            case RET:
                                sb.append(" ret #").append(i);
                                break;
                        }
                    }
                    break;
                case MULTIANEWARRAY:
                    typeName = constants.getConstantString((code[++offset] & 255) << 8 | code[++offset] & 255, Const.CONSTANT_Class);
                    sb.append(typeName).append(' ').append(code[++offset] & 255);
                    break;
                case IFNULL, IFNONNULL:
                    sb.append(" -> ").append(offset + (short)((code[++offset] & 255) << 8 | code[++offset] & 255));
                    break;
                case GOTO_W, JSR_W:
                    sb.append(" -> ").append(offset + ((code[++offset] & 255) << 24 | (code[++offset] & 255) << 16 | (code[++offset] & 255) << 8 | code[++offset] & 255));
                    break;
            }

            sb.append('\n');
        }
    }

    protected void writeLDC(StringBuilder sb, ConstantPool constants, Constant constant) {
        switch (constant.getTag()) {
            case Const.CONSTANT_Integer:
                sb.append(' ').append(((ConstantInteger) constant).getBytes());
                break;
            case Const.CONSTANT_Float:
                sb.append(' ').append(((ConstantFloat) constant).getBytes());
                break;
            case Const.CONSTANT_Class:
                int typeNameIndex = ((ConstantClass) constant).getNameIndex();
                sb.append(' ').append(((ConstantUtf8)constants.getConstant(typeNameIndex)).getBytes());
                break;
            case Const.CONSTANT_Long:
                sb.append(' ').append(((ConstantLong) constant).getBytes());
                break;
            case Const.CONSTANT_Double:
                sb.append(' ').append(((ConstantDouble) constant).getBytes());
                break;
            case Const.CONSTANT_String:
                sb.append(" '");
                int stringIndex = ((ConstantString) constant).getStringIndex();
                String str = constants.getConstantString(stringIndex, Const.CONSTANT_Utf8);

                for (char c : str.toCharArray()) {
                    switch (c) {
                        case '\b':
                            sb.append("\\\\b");
                            break;
                        case '\f':
                            sb.append("\\\\f");
                            break;
                        case '\n':
                            sb.append("\\\\n");
                            break;
                        case '\r':
                            sb.append("\\\\r");
                            break;
                        case '\t':
                            sb.append("\\\\t");
                            break;
                        default:
                            sb.append(c);
                            break;
                    }
                }

                sb.append("'");
                break;
        }
    }

    protected void writeLineNumberTable(String linePrefix, StringBuilder sb, Code attributeCode) {
        LineNumberTable lineNumberTable = attributeCode.getLineNumberTable();

        if (lineNumberTable != null) {
            sb.append(linePrefix).append("Line number table:\n");
            sb.append(linePrefix).append("  Java source line number -> byte code offset\n");

            for (LineNumber lineNumber : lineNumberTable.getLineNumberTable()) {
                sb.append(linePrefix).append("  #");
                sb.append(lineNumber.getLineNumber()).append("\t-> ");
                sb.append(lineNumber.getStartPC()).append('\n');
            }
        }
    }

    public static List<Statement> getLineNumberTableAsStatements(Method method) {

        List<Statement> comments = new ArrayList<>();

        Code attributeCode = method.getCode();

        if (attributeCode == null) {
            return null;
        }

        TreeMap<Integer, List<Integer>> lineNumberToOffsets = new TreeMap<>();

        LineNumberTable lineNumberTable = attributeCode.getLineNumberTable();

        if (lineNumberTable != null) {

            for (LineNumber lineNumber : lineNumberTable.getLineNumberTable()) {
                lineNumberToOffsets.computeIfAbsent(lineNumber.getLineNumber(), k -> new ArrayList<>()).add(lineNumber.getStartPC());
            }
            for (Entry<Integer, List<Integer>> entry : lineNumberToOffsets.entrySet()) {
                int lineNumber = entry.getKey();
                List<Integer> offsets = entry.getValue();
                BooleanExpression condition = new BooleanExpression(entry.getKey(), false);
                StringConstantExpression message = new StringConstantExpression(lineNumber, DECOMPILATION_FAILED_AT_LINE + lineNumber + " -> offsets " + offsets);
                comments.add(new AssertStatement(condition, message));
            }
        }
        return comments;
    }

    protected void writeLocalVariableTable(String linePrefix, StringBuilder sb, Code attributeCode) {
        LocalVariableTable localVariableTable = attributeCode.getLocalVariableTable();

        if (localVariableTable != null) {
            sb.append(linePrefix).append("Local variable table:\n");
            sb.append(linePrefix).append("  start\tlength\tslot\tname\tdescriptor\n");

            for (LocalVariable localVariable : localVariableTable.getLocalVariableTable()) {
                sb.append(linePrefix).append("  ");
                sb.append(localVariable.getStartPC()).append('\t');
                sb.append(localVariable.getLength()).append('\t');
                sb.append(localVariable.getIndex()).append('\t');
                sb.append(localVariable.getName()).append('\t');
                sb.append(localVariable.getSignature()).append('\n');
            }
        }

        LocalVariableTypeTable localVariableTypeTable = (LocalVariableTypeTable) Optional.ofNullable(attributeCode.getAttributes())
                .map(Stream::of).orElseGet(Stream::empty).filter(LocalVariableTypeTable.class::isInstance).findAny().orElse(null);

        if (localVariableTypeTable != null) {
            sb.append(linePrefix).append("Local variable type table:\n");
            sb.append(linePrefix).append("  start\tlength\tslot\tname\tsignature\n");

            for (LocalVariable localVariable : localVariableTypeTable.getLocalVariableTypeTable()) {
                sb.append(linePrefix).append("  ");
                sb.append(localVariable.getStartPC()).append('\t');
                sb.append(localVariable.getLength()).append('\t');
                sb.append(localVariable.getIndex()).append('\t');
                sb.append(localVariable.getName()).append('\t');
                sb.append(localVariable.getSignature()).append('\n');
            }
        }
    }

    protected void writeExceptionTable(String linePrefix, StringBuilder sb, ConstantPool constants, Code attributeCode) {
        CodeException[] codeExceptions = attributeCode.getExceptionTable();

        if (codeExceptions != null) {
            sb.append(linePrefix).append("Exception table:\n");
            sb.append(linePrefix).append("  from\tto\ttarget\ttype\n");

            for (CodeException codeException : codeExceptions) {
                sb.append(linePrefix).append("  ");
                sb.append(codeException.getStartPC()).append('\t');
                sb.append(codeException.getEndPC()).append('\t');
                sb.append(codeException.getHandlerPC()).append('\t');

                if (codeException.getCatchType() == 0) {
                    sb.append("finally");
                } else {
                    sb.append(constants.getConstantString(codeException.getCatchType(), Const.CONSTANT_Class));
                }

                sb.append('\n');
            }
        }
    }

    private static final String[] OPCODE_NAMES = {
        "nop", "aconst_null", "iconst_m1", "iconst_0", "iconst_1",
        "iconst_2", "iconst_3", "iconst_4", "iconst_5", "lconst_0",
        "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0",
        "dconst_1", "bipush", "sipush", "ldc", "ldc_w", "ldc2_w", "iload",
        "lload", "fload", "dload", "aload", "iload_0", "iload_1", "iload_2",
        "iload_3", "lload_0", "lload_1", "lload_2", "lload_3", "fload_0",
        "fload_1", "fload_2", "fload_3", "dload_0", "dload_1", "dload_2",
        "dload_3", "aload_0", "aload_1", "aload_2", "aload_3", "iaload",
        "laload", "faload", "daload", "aaload", "baload", "caload", "saload",
        "istore", "lstore", "fstore", "dstore", "astore", "istore_0",
        "istore_1", "istore_2", "istore_3", "lstore_0", "lstore_1",
        "lstore_2", "lstore_3", "fstore_0", "fstore_1", "fstore_2",
        "fstore_3", "dstore_0", "dstore_1", "dstore_2", "dstore_3",
        "astore_0", "astore_1", "astore_2", "astore_3", "iastore", "lastore",
        "fastore", "dastore", "aastore", "bastore", "castore", "sastore",
        "pop", "pop2", "dup", "dup_x1", "dup_x2", "dup2", "dup2_x1",
        "dup2_x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub",
        "fsub", "dsub", "imul", "lmul", "fmul", "dmul", "idiv", "ldiv",
        "fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg",
        "fneg", "dneg", "ishl", "lshl", "ishr", "lshr", "iushr", "lushr",
        "iand", "land", "ior", "lor", "ixor", "lxor", "iinc", "i2l", "i2f",
        "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l", "d2f",
        "i2b", "i2c", "i2s", "lcmp", "fcmpl", "fcmpg",
        "dcmpl", "dcmpg", "ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle",
        "if_icmpeq", "if_icmpne", "if_icmplt", "if_icmpge", "if_icmpgt",
        "if_icmple", "if_acmpeq", "if_acmpne", "goto", "jsr", "ret",
        "tableswitch", "lookupswitch", "ireturn", "lreturn", "freturn",
        "dreturn", "areturn", "return", "getstatic", "putstatic", "getfield",
        "putfield", "invokevirtual", "invokespecial", "invokestatic",
        "invokeinterface", "invokedynamic", "new", "newarray", "anewarray",
        "arraylength", "athrow", "checkcast", "instanceof", "monitorenter",
        "monitorexit", "wide", "multianewarray", "ifnull", "ifnonnull",
        "goto_w", "jsr_w", "breakpoint", ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
        ILLEGAL_OPCODE, "impdep1", "impdep2"
    };
}
