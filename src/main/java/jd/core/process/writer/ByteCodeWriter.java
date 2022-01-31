/**
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jd.core.process.writer;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.LineNumber;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.classfile.attribute.CodeException;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.Printer;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;

public final class ByteCodeWriter
{
    public static final String BYTE_CODE = "// Byte code:";

    private ByteCodeWriter() {
    }

    private static final String START_OF_COMMENT = "//   ";
    private static final String CORRUPTED_CONSTANT_POOL =
            "Corrupted_Constant_Pool";

    public static void write(
            Loader loader, Printer printer, ReferenceMap referenceMap,
            ClassFile classFile, Method method)
    {
        // Ecriture du byte code
        byte[] code = method.getCode();

        if (code != null)
        {
            int    length = code.length;
            int    ioperande = 0;
            short  soperande = 0;

            printer.startOfComment();

            ConstantPool constants = classFile.getConstantPool();

            printer.print(BYTE_CODE);

            int offset;
            int opcode;
            for (int index=0; index<length; ++index)
            {
                offset = index;
                opcode = code[index] & 255;

                printer.endOfLine();
                printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
                printer.print(START_OF_COMMENT);
                printer.print(offset);
                printer.print(": ");
                printer.print(
                        Const.getOpcodeName(opcode));

                switch (Const.getNoOfOperands(opcode))
                {
                case 1:
                    printer.print(" ");
                    if (opcode == Const.NEWARRAY) {
                        index++;
                        printer.print(
                                Const.getTypeName(code[index] & 16));
                    } else {
                        index++;
                        printer.print(code[index]);
                    }
                    break;
                case 2:
                    printer.print(" ");
                    switch (opcode)
                    {
                    case Const.IINC:
                        index++;
                        printer.print(code[index]);
                        printer.print(" ");
                        index++;
                        printer.print(code[index]);
                        break;

                    case Const.IFEQ,
                         Const.IFNE,
                         Const.IFLT,
                         Const.IFGE,
                         Const.IFGT,
                         Const.IFLE,
                         Const.IF_ICMPEQ,
                         Const.IF_ICMPNE,
                         Const.IF_ICMPLT,
                         Const.IF_ICMPGE,
                         Const.IF_ICMPGT,
                         Const.IF_ICMPLE,
                         Const.IF_ACMPEQ,
                         Const.IF_ACMPNE,
                         Const.IFNONNULL,
                         Const.IFNULL,

                         Const.GOTO,
                         Const.JSR:
                        soperande = (short)( (code[++index] & 255) << 8 |
                                code[++index] & 255 );
                        if (soperande >= 0) {
                            printer.print('+');
                        }
                        printer.print(soperande);
                        printer.print(" -> ");
                        printer.print(
                                index + soperande - 2);
                        break;

                    case Const.PUTSTATIC,
                         Const.PUTFIELD,
                         Const.GETSTATIC,
                         ByteCodeConstants.OUTERTHIS,
                         Const.GETFIELD:
                        ioperande = (code[++index] & 255) << 8 |
                        code[++index] & 255;
                        printer.print(ioperande);
                        printer.print("\t");
                        String fieldName =
                                getConstantFieldName(constants, ioperande);

                        if (fieldName == null)
                        {
                            printer.startOfError();
                            printer.print(CORRUPTED_CONSTANT_POOL);
                            printer.endOfError();
                        }
                        else
                        {
                            printer.print(fieldName);
                        }
                        break;

                    case Const.INVOKESTATIC,
                         Const.INVOKESPECIAL,
                         Const.INVOKEVIRTUAL:
                        ioperande = (code[++index] & 255) << 8 |
                        code[++index] & 255;
                        printer.print(ioperande);
                        printer.print("\t");
                        String methodName =
                                getConstantMethodName(constants, ioperande);

                        if (methodName == null)
                        {
                            printer.startOfError();
                            printer.print(CORRUPTED_CONSTANT_POOL);
                            printer.endOfError();
                        }
                        else
                        {
                            printer.print(methodName);
                        }
                        break;

                    case Const.NEW,
                         Const.ANEWARRAY,
                         Const.CHECKCAST:
                        ioperande = (code[++index] & 255) << 8 |
                        code[++index] & 255;
                        printer.print(ioperande);
                        printer.print("\t");

                        Constant c = constants.get(ioperande);
                        if (c instanceof ConstantClass)
                        {
                            // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                            ConstantClass cc = (ConstantClass) c;
                            printer.print(
                                    constants.getConstantUtf8(cc.getNameIndex()));
                        }
                        else
                        {
                            printer.print(CORRUPTED_CONSTANT_POOL);
                        }
                        break;

                    default:
                        ioperande = (code[++index] & 255) << 8 |
                        code[++index] & 255;
                        printer.print(ioperande);
                    }
                    break;
                default:
                    switch (opcode)
                    {
                    case Const.MULTIANEWARRAY:
                        printer.print(" ");
                        printer.print(
                                (code[++index] & 255) << 8 | code[++index] & 255);
                        printer.print(" ");
                        index++;
                        printer.print(code[index]);
                        break;
                    case Const.INVOKEINTERFACE:
                        printer.print(" ");
                        printer.print(
                                (code[++index] & 255) << 8 | code[++index] & 255);
                        printer.print(" ");
                        index++;
                        printer.print(code[index]);
                        printer.print(" ");
                        index++;
                        printer.print(code[index]);
                        break;
                    case Const.TABLESWITCH:
                        // Skip padding
                        index = (index+4 & 0xFFFC) - 1;

                        printer.print("\tdefault:+");

                        int jump = (code[++index] & 255) << 24 |
                                (code[++index] & 255) << 16 |
                                (code[++index] & 255) << 8 |
                                code[++index] & 255;

                        printer.print(jump);
                        printer.print("->");
                        printer.print(offset + jump);

                        int low =  (code[++index] & 255) << 24 |
                                (code[++index] & 255) << 16 |
                                (code[++index] & 255) << 8 |
                                code[++index] & 255;
                        int high = (code[++index] & 255) << 24 |
                                (code[++index] & 255) << 16 |
                                (code[++index] & 255) << 8 |
                                code[++index] & 255;

                        for (int value=low; value<=high; value++)
                        {
                            printer.print(", ");
                            printer.print(value);
                            printer.print(":+");

                            jump = (code[++index] & 255) << 24 |
                                    (code[++index] & 255) << 16 |
                                    (code[++index] & 255) << 8 |
                                    code[++index] & 255;

                            printer.print(jump);
                            printer.print("->");
                            printer.print(offset + jump);
                        }
                        break;
                    case Const.LOOKUPSWITCH:
                        // Skip padding
                        index = (index+4 & 0xFFFC) - 1;

                        printer.print("\tdefault:+");

                        jump = (code[++index] & 255) << 24 |
                                (code[++index] & 255) << 16 |
                                (code[++index] & 255) << 8 |
                                code[++index] & 255;

                        printer.print(jump);
                        printer.print("->");
                        printer.print(offset + jump);

                        int npairs = (code[++index] & 255) << 24 |
                                (code[++index] & 255) << 16 |
                                (code[++index] & 255) << 8 |
                                code[++index] & 255;

                        for (int i=0; i<npairs; i++)
                        {
                            printer.print(", ");
                            printer.print(

                                    (code[++index] & 255) << 24 |
                                    (code[++index] & 255) << 16 |
                                    (code[++index] & 255) << 8 |
                                    code[++index] & 255);
                            printer.print(":+");

                            jump = (code[++index] & 255) << 24 |
                                    (code[++index] & 255) << 16 |
                                    (code[++index] & 255) << 8 |
                                    code[++index] & 255;

                            printer.print(jump);
                            printer.print("->");
                            printer.print(offset + jump);
                        }
                        break;
                    case Const.WIDE:
                        index = ByteCodeUtil.nextWideOffset(code, index);
                        break;
                    default:
                        for (int j=Const.getNoOfOperands(opcode); j>0; --j)
                        {
                            printer.print(" ");
                            index++;
                            printer.print(code[index]);
                        }
                    }
                }
            }

            writeAttributeNumberTables(printer, method);
            writeAttributeLocalVariableTables(
                    loader, printer, referenceMap, classFile, method);
            writeCodeExceptions(printer, classFile, method);

            printer.endOfComment();
        }
    }

    private static void writeAttributeNumberTables(
            Printer printer, Method method)
    {
        // Ecriture de la table des numéros de ligne
        LineNumber[] lineNumbers = method.getLineNumbers();
        if (lineNumbers != null)
        {
            printer.endOfLine();
            printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
            printer.print("// Line number table:");

            for (LineNumber lineNumber : lineNumbers) {
                printer.endOfLine();
                printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
                printer.print("//   Java source line #");
                printer.print(lineNumber.getLineNumber());
                printer.print("\t-> byte code offset #");
                printer.print(lineNumber.getStartPC());
            }
        }
    }

    private static void writeAttributeLocalVariableTables(
            Loader loader, Printer printer, ReferenceMap referenceMap,
            ClassFile classFile, Method method)
    {
        // Ecriture de la table des variables locales
        LocalVariables localVariables = method.getLocalVariables();
        if (localVariables != null)
        {
            int length = localVariables.size();

            printer.endOfLine();
            printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
            printer.print("// Local variable table:");
            printer.endOfLine();
            printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
            printer.print("//   start\tlength\tslot\tname\tsignature");

            ConstantPool constants = classFile.getConstantPool();

            LocalVariable lv;
            for (int i=0; i<length; i++)
            {
                lv = localVariables.getLocalVariableAt(i);

                if (lv != null)
                {
                    printer.endOfLine();
                    printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
                    printer.print(START_OF_COMMENT);
                    printer.print(lv.getStartPc());
                    printer.print("\t");
                    printer.print(lv.getLength());
                    printer.print("\t");
                    printer.print(lv.getIndex());
                    printer.print("\t");

                    if (lv.getNameIndex() > 0)
                    {
                        printer.print(constants.getConstantUtf8(lv.getNameIndex()));
                    }
                    else
                    {
                        printer.print("???");
                    }

                    printer.print("\t");

                    if (lv.getSignatureIndex() > 0)
                    {
                        SignatureWriter.writeSignature(
                                loader, printer, referenceMap,
                                classFile, constants.getConstantUtf8(lv.getSignatureIndex()));
                    }
                    else
                    {
                        printer.print("???");
                    }
                }
            }
        }
    }

    private static void writeCodeExceptions(
            Printer printer, ClassFile classFile, Method method)
    {
        // Ecriture de la table des exceptions
        CodeException[] codeExceptions = method.getCodeExceptions();
        if (codeExceptions != null && codeExceptions.length > 0)
        {
            printer.endOfLine();
            printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
            printer.print("// Exception table:");
            printer.endOfLine();
            printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
            printer.print("//   from\tto\ttarget\ttype");

            for (CodeException codeException : codeExceptions) {
                printer.endOfLine();
                printer.startOfLine(Instruction.UNKNOWN_LINE_NUMBER);
                printer.print(START_OF_COMMENT);
                printer.print(codeException.startPc());
                printer.print("\t");
                printer.print(codeException.endPc());
                printer.print("\t");
                printer.print(codeException.handlerPc());
                printer.print("\t");

                if (codeException.catchType() == 0) {
                    printer.print("finally");
                } else {
                    printer.print(

                            classFile.getConstantPool().getConstantClassName(
                                    codeException.catchType()));
                }
            }
        }
    }

    private static String getConstantFieldName(
            ConstantPool constants, int index)
    {
        ConstantFieldref cfr;
        Constant c = constants.get(index);

        if (!(c instanceof ConstantFieldref)) {
            return null;
        }
        cfr = (ConstantFieldref)c;

        ConstantClass cc;
        c = constants.get(cfr.getClassIndex());

        if (!(c instanceof ConstantClass)) {
            return null;
        }
        cc = (ConstantClass)c;

        String classPath = constants.getConstantUtf8(cc.getNameIndex());

        ConstantNameAndType cnat =
                constants.getConstantNameAndType(cfr.getNameAndTypeIndex());

        String fieldName = constants.getConstantUtf8(cnat.getNameIndex());
        String fieldDescriptor =
                constants.getConstantUtf8(cnat.getSignatureIndex());

        return classPath + ':' + fieldName + "\t" + fieldDescriptor;
    }

    private static String getConstantMethodName(
            ConstantPool constants, int index)
    {
        ConstantMethodref cmr;
        Constant c = constants.get(index);

        if (!(c instanceof ConstantMethodref)) {
            return null;
        }
        cmr = (ConstantMethodref)c;

        ConstantClass cc;
        c = constants.get(cmr.getClassIndex());

        if (!(c instanceof ConstantClass)) {
            return null;
        }
        cc = (ConstantClass)c;

        String classPath = constants.getConstantUtf8(cc.getNameIndex());

        ConstantNameAndType cnat =
                constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

        String fieldName = constants.getConstantUtf8(cnat.getNameIndex());
        String fieldDescriptor =
                constants.getConstantUtf8(cnat.getSignatureIndex());

        return classPath + ':' + fieldName + "\t" + fieldDescriptor;
    }
}
